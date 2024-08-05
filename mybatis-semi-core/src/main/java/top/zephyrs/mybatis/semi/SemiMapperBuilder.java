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

import top.zephyrs.mybatis.semi.base.IMapper;
import top.zephyrs.mybatis.semi.config.SensitiveConfig;
import top.zephyrs.mybatis.semi.annotations.DatabaseId;
import top.zephyrs.mybatis.semi.annotations.SensitiveDecrypt;
import top.zephyrs.mybatis.semi.injects.InjectMethod;
import top.zephyrs.mybatis.semi.metadata.TableInfo;
import top.zephyrs.mybatis.semi.metadata.MetadataHelper;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * 通用Mapper方法构建
 * @author zephyrs
 */
public class SemiMapperBuilder {

    private final SemiMybatisConfiguration configuration;
    private final MapperBuilderAssistant assistant;
    private final Class<?> type;

    public SemiMapperBuilder(SemiMybatisConfiguration configuration, Class<?> type) {
        String resource = type.getName().replace('.', '/') + ".java (best guess)";
        this.assistant = new MapperBuilderAssistant(configuration, resource);
        this.configuration = configuration;
        this.type = type;
    }

    public void parse() {
        assistant.setCurrentNamespace(type.getName());
        for (Method method : type.getMethods()) {
            if (!canHaveStatement(method)) {
                continue;
            }

            String mappedProcessorId = method.getName();
            InjectMethod processor = configuration.getInjectMethod(mappedProcessorId);
            if(processor == null) {
                continue;
            }

            String mappedStatementId = type.getName() + "." + method.getName();
            if(configuration.hasStatement(mappedStatementId, false)) {
                continue;
            }
            // 自定义的结果集解析
            if (method.getAnnotation(ResultMap.class) == null) {
                parseResultMap(method);
            }
            parseStatement(method, processor);
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

    private String parseResultMap(Method method) {
        Class<?> returnType = getReturnType(method, type);
        Arg[] args = method.getAnnotationsByType(Arg.class);
        Result[] results = method.getAnnotationsByType(Result.class);
        TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
        String resultMapId = generateResultMapName(method);
        applyResultMap(resultMapId, returnType, args, results, typeDiscriminator);
        return resultMapId;
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

    private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results,
                                TypeDiscriminator discriminator) {
        if(configuration.hasResultMap(resultMapId)) {
            return;
        }
        List<ResultMapping> resultMappings = new ArrayList<>();
        applyConstructorArgs(args, returnType, resultMappings);
        applyResults(results, returnType, resultMappings);
        Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
        // TODO add AutoMappingBehaviour
        assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
        createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
    }

    private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
        if (discriminator != null) {
            for (Case c : discriminator.cases()) {
                String caseResultMapId = resultMapId + "-" + c.value();
                List<ResultMapping> resultMappings = new ArrayList<>();
                // issue #136
                applyConstructorArgs(c.constructArgs(), resultType, resultMappings);
                applyResults(c.results(), resultType, resultMappings);
                // TODO add AutoMappingBehaviour
                assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
            }
        }
    }

    private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
        if (discriminator != null) {
            String column = discriminator.column();
            Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
            JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
            @SuppressWarnings("unchecked")
            Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (discriminator
                    .typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
            Case[] cases = discriminator.cases();
            Map<String, String> discriminatorMap = new HashMap<>();
            for (Case c : cases) {
                String value = c.value();
                String caseResultMapId = resultMapId + "-" + value;
                discriminatorMap.put(value, caseResultMapId);
            }
            return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
        }
        return null;
    }

    void parseStatement(Method method, InjectMethod processor) {
        Class<?> parameterTypeClass = getParameterType(method);
        LanguageDriver languageDriver = getLanguageDriver(method);
        String mappedStatementId = type.getName() + "." + method.getName();

        //敏感字段加解密功能
        SensitiveConfig sensitiveCfg = configuration.getGlobalConfig().getSensitive();
        if(sensitiveCfg != null && sensitiveCfg.isOpen()) {
            if(!sensitiveCfg.isDefaultDecrypt()) {
                SensitiveDecrypt decrypt = method.getAnnotation(SensitiveDecrypt.class);
                if(decrypt != null) {
                    configuration.addSensitiveMappedStatementIds(mappedStatementId);
                }
            }
        }
        //代理的类
        Class<?> beanClass = getBeanType(type);
        if(beanClass == null) {
            return;
        }

        TableInfo tableInfo = MetadataHelper.getTableInfo(configuration.getGlobalConfig(), beanClass);
        if(tableInfo == null) {
            return;
        }
        SqlSource sqlSource = processor.createSqlSource(configuration, type, beanClass, method, parameterTypeClass, languageDriver);
        if(sqlSource == null) {
            return;
        }

        String databaseId = processor.databaseId();

        Options options = method.getAnnotation(Options.class);
        SqlCommandType sqlCommandType = processor.getSqlCommandType();

        ResultSetType resultSetType = configuration.getDefaultResultSetType();
        boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
        boolean flushCache = !isSelect;
        boolean useCache = isSelect;

        Integer fetchSize =null;
        Integer timeout = null;
        StatementType statementType = StatementType.PREPARED;

        if(options != null){
            databaseId = options.databaseId()== null? "": options.databaseId();
            statementType = options.statementType();
            fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null;
            timeout = options.timeout() > -1 ? options.timeout() : null;
            if (options.resultSetType() != ResultSetType.DEFAULT) {
                resultSetType = options.resultSetType();
            }
            if(!options.useCache()) {
                useCache = false;
            }
            if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
                flushCache = true;
            } else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
                flushCache = false;
            }
        }

        DatabaseId databaseIdAnnotation = method.getAnnotation(DatabaseId.class);
        if(databaseIdAnnotation == null) {
            databaseIdAnnotation = type.getAnnotation(DatabaseId.class);
        }
        if(databaseIdAnnotation != null) {
            databaseId = databaseIdAnnotation.value();
        }

        String resultMapId = null;
        if (isSelect) {
            ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
            if (resultMapAnnotation != null) {
                resultMapId = String.join(",", resultMapAnnotation.value());
            } else {
                resultMapId = generateResultMapName(method);
            }
        }
        processor.addMappedStatement(tableInfo, assistant,
                mappedStatementId, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
                null, parameterTypeClass, resultMapId, getReturnType(method, type), resultSetType, flushCache, useCache,
                false, NoKeyGenerator.INSTANCE, null,  null, databaseId, languageDriver,
                options != null ? nullOrEmpty(options.resultSets()) : null, processor.isDirtySelect());
    }

    private LanguageDriver getLanguageDriver(Method method) {
        Lang lang = method.getAnnotation(Lang.class);
        Class<? extends LanguageDriver> langClass = null;
        if (lang != null) {
            langClass = lang.value();
        }
        return configuration.getLanguageDriver(langClass);
    }

    private Class<?> getParameterType(Method method) {
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

    private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
        for (Result result : results) {
            List<ResultFlag> flags = new ArrayList<>();
            if (result.id()) {
                flags.add(ResultFlag.ID);
            }
            @SuppressWarnings("unchecked")
            Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (result
                    .typeHandler() == UnknownTypeHandler.class ? null : result.typeHandler());
            boolean hasNestedResultMap = hasNestedResultMap(result);
            ResultMapping resultMapping = assistant.buildResultMapping(resultType, nullOrEmpty(result.property()),
                    nullOrEmpty(result.column()), result.javaType() == void.class ? null : result.javaType(),
                    result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
                    hasNestedSelect(result) ? nestedSelectId(result) : null,
                    hasNestedResultMap ? nestedResultMapId(result) : null, null,
                    hasNestedResultMap ? findColumnPrefix(result) : null, typeHandler, flags, null, null, isLazy(result));
            resultMappings.add(resultMapping);
        }
    }

    private String findColumnPrefix(Result result) {
        String columnPrefix = result.one().columnPrefix();
        if (columnPrefix.length() < 1) {
            columnPrefix = result.many().columnPrefix();
        }
        return columnPrefix;
    }

    private String nestedResultMapId(Result result) {
        String resultMapId = result.one().resultMap();
        if (resultMapId.length() < 1) {
            resultMapId = result.many().resultMap();
        }
        if (!resultMapId.contains(".")) {
            resultMapId = type.getName() + "." + resultMapId;
        }
        return resultMapId;
    }

    private boolean hasNestedResultMap(Result result) {
        if (result.one().resultMap().length() > 0 && result.many().resultMap().length() > 0) {
            throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
        }
        return result.one().resultMap().length() > 0 || result.many().resultMap().length() > 0;
    }

    private String nestedSelectId(Result result) {
        String nestedSelect = result.one().select();
        if (nestedSelect.length() < 1) {
            nestedSelect = result.many().select();
        }
        if (!nestedSelect.contains(".")) {
            nestedSelect = type.getName() + "." + nestedSelect;
        }
        return nestedSelect;
    }

    private boolean isLazy(Result result) {
        boolean isLazy = configuration.isLazyLoadingEnabled();
        if (result.one().select().length() > 0 && FetchType.DEFAULT != result.one().fetchType()) {
            isLazy = result.one().fetchType() == FetchType.LAZY;
        } else if (result.many().select().length() > 0 && FetchType.DEFAULT != result.many().fetchType()) {
            isLazy = result.many().fetchType() == FetchType.LAZY;
        }
        return isLazy;
    }

    private boolean hasNestedSelect(Result result) {
        if (result.one().select().length() > 0 && result.many().select().length() > 0) {
            throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
        }
        return result.one().select().length() > 0 || result.many().select().length() > 0;
    }

    private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings) {
        for (Arg arg : args) {
            List<ResultFlag> flags = new ArrayList<>();
            flags.add(ResultFlag.CONSTRUCTOR);
            if (arg.id()) {
                flags.add(ResultFlag.ID);
            }
            @SuppressWarnings("unchecked")
            Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (arg
                    .typeHandler() == UnknownTypeHandler.class ? null : arg.typeHandler());
            ResultMapping resultMapping = assistant.buildResultMapping(resultType, nullOrEmpty(arg.name()),
                    nullOrEmpty(arg.column()), arg.javaType() == void.class ? null : arg.javaType(),
                    arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(), nullOrEmpty(arg.select()),
                    nullOrEmpty(arg.resultMap()), null, nullOrEmpty(arg.columnPrefix()), typeHandler, flags, null, null, false);
            resultMappings.add(resultMapping);
        }
    }

    private String nullOrEmpty(String value) {
        return value == null || value.trim().length() == 0 ? null : value;
    }

    private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId,
                                                   Class<?> parameterTypeClass, LanguageDriver languageDriver) {
        String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
        Class<?> resultTypeClass = selectKeyAnnotation.resultType();
        StatementType statementType = selectKeyAnnotation.statementType();
        String keyProperty = selectKeyAnnotation.keyProperty();
        String keyColumn = selectKeyAnnotation.keyColumn();
        boolean executeBefore = selectKeyAnnotation.before();

        // defaults
        boolean useCache = false;
        KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
        Integer fetchSize = null;
        Integer timeout = null;
        boolean flushCache = false;
        String parameterMap = null;
        String resultMap = null;
        ResultSetType resultSetTypeEnum = null;
        String databaseId = selectKeyAnnotation.databaseId().isEmpty() ? null : selectKeyAnnotation.databaseId();

        SqlSource sqlSource = buildSqlSource(selectKeyAnnotation, parameterTypeClass, languageDriver, null);
        SqlCommandType sqlCommandType = SqlCommandType.SELECT;

        assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
                parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, false, keyGenerator,
                keyProperty, keyColumn, databaseId, languageDriver, null, false);

        id = assistant.applyCurrentNamespace(id, false);

        MappedStatement keyStatement = configuration.getMappedStatement(id, false);
        SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
        configuration.addKeyGenerator(id, answer);
        return answer;
    }

    private SqlSource buildSqlSource(Annotation annotation, Class<?> parameterType, LanguageDriver languageDriver,
                                     Method method) {
        if (annotation instanceof Select) {
            return buildSqlSourceFromStrings(((Select) annotation).value(), parameterType, languageDriver);
        }
        if (annotation instanceof Update) {
            return buildSqlSourceFromStrings(((Update) annotation).value(), parameterType, languageDriver);
        } else if (annotation instanceof Insert) {
            return buildSqlSourceFromStrings(((Insert) annotation).value(), parameterType, languageDriver);
        } else if (annotation instanceof Delete) {
            return buildSqlSourceFromStrings(((Delete) annotation).value(), parameterType, languageDriver);
        } else if (annotation instanceof SelectKey) {
            return buildSqlSourceFromStrings(((SelectKey) annotation).statement(), parameterType, languageDriver);
        }
        return new ProviderSqlSource(assistant.getConfiguration(), annotation, type, method);
    }

    private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass,
                                                LanguageDriver languageDriver) {
        return languageDriver.createSqlSource(configuration, String.join(" ", strings).trim(), parameterTypeClass);
    }

    /**
     * 通过反射,获得定义Class时声明的父类的范型参数的类型.
     * @param mapperClass mapper类型
     * @return mapper支持的类型（泛型）
     */
    protected Class<?> getBeanType(Class<?> mapperClass) {
        if(!IMapper.class.isAssignableFrom(type)) {
            return null;
        }
        for(Type type: mapperClass.getGenericInterfaces()) {
            if(!(type instanceof ParameterizedType)) {
                continue;
            }
            Type[] params = ((ParameterizedType) type).getActualTypeArguments();
            if (params.length == 0 || !(params[0] instanceof Class)) {
                continue;
            }
            return (Class<?>) params[0];
        }
        return Object.class;
    }
}
