package com.zephyrs.mybatis.semi.injects;

import com.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import com.zephyrs.mybatis.semi.metadata.TableInfo;
import com.zephyrs.mybatis.semi.metadata.MetadataHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;

import java.lang.reflect.Method;

public abstract class AbstractInjectMethod implements InjectMethod {

    @Override
    public boolean isDirtySelect() {
        return false;
    }

    @Override
    public MappedStatement addMappedStatement(TableInfo tableInfo,
                                              MapperBuilderAssistant assistant,
                                              String id, SqlSource sqlSource, StatementType statementType,
                                              SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout,
                                              String parameterMap, Class<?> parameterType,
                                              String resultMap, Class<?> resultType, ResultSetType resultSetType,
                                              boolean flushCache, boolean useCache, boolean resultOrdered,
                                              KeyGenerator keyGenerator, String keyProperty, String keyColumn,
                                              String databaseId, LanguageDriver lang,
                                              String resultSets, boolean dirtySelect) {
        return assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered,
                keyGenerator, keyProperty, keyColumn,
                databaseId, lang, resultSets, dirtySelect);
    }

    @Override
    public SqlSource createSqlSource(SemiMybatisConfiguration configuration,
                                     Class<?> mapperClass, Class<?> beanClass, Method method,
                                     Class<?> parameterTypeClass,
                                     LanguageDriver languageDriver) {

        TableInfo tableInfo = MetadataHelper.getTableInfo(configuration.getGlobalConfig(), beanClass);
        if (tableInfo == null) {
            return null;
        }
        String sqlScript = this.buildSqlScript(configuration, beanClass, parameterTypeClass, tableInfo);
        if(sqlScript == null || sqlScript.isEmpty()) {
            return null;
        }
        return languageDriver.createSqlSource(configuration, sqlScript, parameterTypeClass);
    }

    /**
     * 构建sql语句
     * @param configuration mybatis配置信息
     * @param beanClass Mapper的泛型的具体类型
     * @param parameterTypeClass 参数类型
     * @param tableInfo 对应的 表信息，包含列，字段，表名等等
     * @return sql语句
     */
    protected abstract String buildSqlScript(SemiMybatisConfiguration configuration,
                                             Class<?> beanClass, Class<?> parameterTypeClass,
                                             TableInfo tableInfo);

}
