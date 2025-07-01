/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package top.zephyrs.mybatis.semi;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;
import top.zephyrs.mybatis.semi.annotations.DatabaseId;
import top.zephyrs.mybatis.semi.annotations.SensitiveDecrypt;
import top.zephyrs.mybatis.semi.base.IMapper;
import top.zephyrs.mybatis.semi.config.SensitiveConfig;
import top.zephyrs.mybatis.semi.injects.InjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetaHelper;
import top.zephyrs.mybatis.semi.metadata.ReflectionUtils;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * 通用Mapper方法构建
 *
 * @author zephyrs
 */
public class SemiMapperBuilder {

    protected final Log log = LogFactory.getLog(this.getClass());
    private final SemiMybatisConfiguration configuration;
    private final MapperBuilderAssistant assistant;
    //Mapper类
    private final Class<?> type;

    public SemiMapperBuilder(SemiMybatisConfiguration configuration, Class<?> type) {
        String resource = type.getName().replace('.', '/') + ".java (semi)";
        this.assistant = new MapperBuilderAssistant(configuration, resource);
        this.configuration = configuration;
        this.type = type;
    }

    public void parse() {
        log.debug("semi parse mapper "+type.getName());
        assistant.setCurrentNamespace(type.getName());
        for (Method method : type.getMethods()) {
            //判断是否需要生成 Statement。（排除默认方法和桥接方法）
            if (!canHaveStatement(method)) {
                continue;
            }

            String mappedStatementId = type.getName() + "." + method.getName();

            //敏感字段加解密功能
            //判断 mappedStatement 是否需要解密，需要则登记到 configuration
            SensitiveConfig sensitiveCfg = configuration.getGlobalConfig().getSensitive();
            if (sensitiveCfg != null && sensitiveCfg.isOpen()) {
                if (!sensitiveCfg.isDefaultDecrypt()) {
                    SensitiveDecrypt decrypt = method.getAnnotation(SensitiveDecrypt.class);
                    if (decrypt != null) {
                        configuration.addSensitiveMappedStatementIds(mappedStatementId);
                    }
                }
            }

            //mappedStatement 已经存在则跳过
            if (configuration.hasStatement(mappedStatementId, false)) {
                //判断是否存在ResultMap映射，不存在则替换为默认方式
                this.injectSemiResultMap(mappedStatementId);
                continue;
            }

            //获取通用的自动代理的方法
            String mappedProcessorId = method.getName();
            InjectMethod processor = configuration.getInjectMethod(mappedProcessorId, method);
            if (processor == null) {
                continue;
            }
            parseStatement(mappedStatementId, method, processor);
        }
        parsePendingMethods();
    }

    private static boolean canHaveStatement(Method method) {
        return !method.isBridge() && !method.isDefault();
    }

    private void parsePendingMethods() {
        Collection<MethodResolver> incompleteMethods = configuration.getIncompleteMethods();
        synchronized (incompleteMethods) {
            Iterator<MethodResolver> iter = incompleteMethods.iterator();
            while (iter.hasNext()) {
                try {
                    iter.next().resolve();
                    iter.remove();
                } catch (IncompleteElementException e) {
                    // This method is still missing a resource
                }
            }
        }
    }

    /**
     * 替换空的返回对象为包含 @Column注解声明的TypeHandler的类型
     */
    private void injectSemiResultMap(String mappedStatementId) {
        MappedStatement ms = configuration.getMappedStatement(mappedStatementId, false);
        if (ms == null) {
            return;
        }
        //已经存在自定义ResultMap则跳过
        org.apache.ibatis.mapping.ResultMap resultMap = ms.getResultMaps().iterator().next();
        if (resultMap.getResultMappings() != null && !resultMap.getResultMappings().isEmpty()) {
            return;
        }
        Class<?> returnType = resultMap.getType();
        if (returnType.isPrimitive()) {
            return;
        }
        org.apache.ibatis.mapping.ResultMap newResultMap = null;
        String newResultMapId = resultMap.getId() + "-semi-meta";
        if (configuration.hasResultMap(newResultMapId)) {
            newResultMap = configuration.getResultMap(newResultMapId);
        } else {
            MetaInfo metaInfo = MetaHelper.getMetaInfo(configuration.getGlobalConfig(), returnType, true);
            if(metaInfo == null) {
                return;
            }
            List<ResultMapping> resultMappings = new ArrayList<>();
            for (ColumnInfo columnInfo : metaInfo.getColumns()) {
                if (columnInfo.getTypeHandler() != null && columnInfo.getTypeHandler() != UnknownTypeHandler.class) {

                    Class<? extends TypeHandler<?>> typeHandlerClazz = columnInfo.getTypeHandler();


                    TypeHandler<?> typeHandler = configuration.getTypeHandlerRegistry().getInstance(columnInfo.getFieldType(), typeHandlerClazz);
                    ResultMapping resultMapping = new ResultMapping.Builder
                            (ms.getConfiguration(), columnInfo.getFieldName(),
                                    columnInfo.getColumnName(), typeHandler).build();
                    resultMappings.add(resultMapping);
                }
            }
            if (!resultMappings.isEmpty()) {
                newResultMap = new org.apache.ibatis.mapping.ResultMap.Builder(ms.getConfiguration(), newResultMapId, returnType, resultMappings).build();
                configuration.addResultMap(newResultMap);
            }

        }
        if (newResultMap != null) {
            Field field = ReflectionUtils.findField(MappedStatement.class, "resultMaps");
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, ms, Collections.singletonList(newResultMap));
        }
    }


    void parseStatement(String mappedStatementId, Method method, InjectMethod processor) {

        Class<?> returnType = getReturnType(method, type);
        Class<?> parameterTypeClass = getParameterType(method);
        SqlCommandType sqlCommandType = processor.getSqlCommandType();
        boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
        //获取对应的对象配置信息
        MetaInfo metaInfo = null;
        if(isSelect) {
            metaInfo = MetaHelper.getMetaInfo(configuration.getGlobalConfig(), returnType, true);
        } else if(!parameterTypeClass.isPrimitive()) {
            if(parameterTypeClass.equals(Object.class) || parameterTypeClass.equals(Serializable.class)) {
                parameterTypeClass = getBeanType(type);
            }
            metaInfo = MetaHelper.getMetaInfo(configuration.getGlobalConfig(), parameterTypeClass, true);
        }

        String resultMapId = null;
        if (isSelect) {
            ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
            if (resultMapAnnotation != null) {
                resultMapId = String.join(",", resultMapAnnotation.value());
            } else {
                resultMapId = generateResultMapName(method);
                // 自定义的结果集解析
                applyResultMap(resultMapId, returnType, metaInfo);
            }
        }

        LanguageDriver languageDriver = getLanguageDriver(method);

        SqlSource sqlSource = processor.createSqlSource(configuration, metaInfo, method, parameterTypeClass, languageDriver);
        if (sqlSource == null) {
            return;
        }

        String databaseId = processor.databaseId();

        Options options = method.getAnnotation(Options.class);

        ResultSetType resultSetType = configuration.getDefaultResultSetType();
        boolean flushCache = !isSelect;
        boolean useCache = isSelect;

        Integer fetchSize = null;
        Integer timeout = null;
        StatementType statementType = StatementType.PREPARED;

        if (options != null) {
            databaseId = options.databaseId() == null ? "" : options.databaseId();
            statementType = options.statementType();
            fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null;
            timeout = options.timeout() > -1 ? options.timeout() : null;
            if (options.resultSetType() != ResultSetType.DEFAULT) {
                resultSetType = options.resultSetType();
            }
            if (!options.useCache()) {
                useCache = false;
            }
            if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
                flushCache = true;
            } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
                flushCache = false;
            }
        }

        DatabaseId databaseIdAnnotation = method.getAnnotation(DatabaseId.class);
        if (databaseIdAnnotation == null) {
            databaseIdAnnotation = type.getAnnotation(DatabaseId.class);
        }
        if (databaseIdAnnotation != null) {
            databaseId = databaseIdAnnotation.value();
        }

        processor.addMappedStatement(metaInfo, assistant,
                mappedStatementId, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
                null, parameterTypeClass, resultMapId, getReturnType(method, type), resultSetType, flushCache, useCache,
                false, NoKeyGenerator.INSTANCE, null, null, databaseId, languageDriver,
                options != null ? nullOrEmpty(options.resultSets()) : null, processor.isDirtySelect());
    }

    private String generateResultMapName(Method method) {
        Results results = method.getAnnotation(Results.class);
        if (results != null && !results.id().isEmpty()) {
            return type.getName() + "." + results.id();
        }
        StringBuilder suffix = new StringBuilder();
        for (Class<?> c : method.getParameterTypes()) {
            suffix.append("-");
            suffix.append(c.getSimpleName());
        }
        if (suffix.length() < 1) {
            suffix.append("-void");
        }
        return type.getName() + "." + method.getName() + suffix;
    }

    private void applyResultMap(String resultMapId, Class<?> returnType, MetaInfo metaInfo) {
        if (configuration.hasResultMap(resultMapId)) {
            return;
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        if (metaInfo != null && returnType.isAssignableFrom(metaInfo.getType())) {

            for (ColumnInfo columnInfo : metaInfo.getColumns()) {
                if (columnInfo.getTypeHandler() != null && columnInfo.getTypeHandler() != UnknownTypeHandler.class) {

                    List<ResultFlag> flags = new ArrayList<>();
                    if (columnInfo.isPK()) {
                        flags.add(ResultFlag.ID);
                    }
                    Class<? extends TypeHandler<?>> typeHandlerClazz = columnInfo.getTypeHandler();

                    ResultMapping resultMapping = assistant.buildResultMapping(returnType, columnInfo.getFieldName(), columnInfo.getColumnName(),
                            columnInfo.getFieldType(), null, null, null, null, null,
                            typeHandlerClazz, flags, null, null, isLazy());
                    resultMappings.add(resultMapping);
                }
            }
        }
        assistant.addResultMap(resultMapId, returnType, null, null, resultMappings, null);
    }

    private LanguageDriver getLanguageDriver(Method method) {
        Lang lang = method.getAnnotation(Lang.class);
        Class<? extends LanguageDriver> langClass = null;
        if (lang != null) {
            langClass = lang.value();
        }
        return configuration.getLanguageDriver(langClass);
    }

    /**
     * 获取参数类型
     * @param method
     * @return
     */
    private static Class<?> getParameterType(Method method) {
        Class<?> parameterType = null;
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> currentParameterType : parameterTypes) {
            if (!RowBounds.class.isAssignableFrom(currentParameterType)
                    && !ResultHandler.class.isAssignableFrom(currentParameterType)) {
                if (parameterType == null) {
                    parameterType = currentParameterType;
                } else {
                    // issue #135
                    parameterType = ParamMap.class;
                }
            }
        }
        return parameterType;
    }

    /**
     * 获取返回值类型
     * @param method
     * @param type
     * @return
     */
    private static Class<?> getReturnType(Method method, Class<?> type) {
        Class<?> returnType = method.getReturnType();
        Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
        if (resolvedReturnType instanceof Class) {
            returnType = (Class<?>) resolvedReturnType;
            if (returnType.isArray()) {
                returnType = returnType.getComponentType();
            }
            // gcode issue #508
            if (void.class.equals(returnType)) {
                ResultType rt = method.getAnnotation(ResultType.class);
                if (rt != null) {
                    returnType = rt.value();
                }
            }
        } else if (resolvedReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 1) {
                    Type returnTypeParameter = actualTypeArguments[0];
                    if (returnTypeParameter instanceof Class<?>) {
                        returnType = (Class<?>) returnTypeParameter;
                    } else if (returnTypeParameter instanceof ParameterizedType) {
                        // (gcode issue #443) actual type can be a also a parameterized type
                        returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
                    } else if (returnTypeParameter instanceof GenericArrayType) {
                        Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
                        // (gcode issue #525) support List<byte[]>
                        returnType = Array.newInstance(componentType, 0).getClass();
                    }
                }
            } else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
                // (gcode issue 504) Do not look into Maps if there is not MapKey annotation
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 2) {
                    Type returnTypeParameter = actualTypeArguments[1];
                    if (returnTypeParameter instanceof Class<?>) {
                        returnType = (Class<?>) returnTypeParameter;
                    } else if (returnTypeParameter instanceof ParameterizedType) {
                        // (gcode issue 443) actual type can be a also a parameterized type
                        returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
                    }
                }
            } else if (Optional.class.equals(rawType)) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                Type returnTypeParameter = actualTypeArguments[0];
                if (returnTypeParameter instanceof Class<?>) {
                    returnType = (Class<?>) returnTypeParameter;
                }
            }
        }

        return returnType;
    }

    private boolean isLazy() {
        return configuration.isLazyLoadingEnabled();
    }


    private String nullOrEmpty(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    /**
     * 获取 IMapper注解的泛型类型
     *
     * @param mapperClass mapper类型
     * @return mapper支持的类型（泛型）
     */
    protected Class<?> getBeanType(Class<?> mapperClass) {
        if (IMapper.class.isAssignableFrom(type)) {
            for (Type type : mapperClass.getGenericInterfaces()) {
                if (!(type instanceof ParameterizedType)) {
                    continue;
                }
                Type[] params = ((ParameterizedType) type).getActualTypeArguments();
                if (params.length == 0 || !(params[0] instanceof Class)) {
                    continue;
                }
                return (Class<?>) params[0];
            }
        }
        return Object.class;
    }
}
