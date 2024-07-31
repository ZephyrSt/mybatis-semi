package com.zephyrs.mybatis.semi.injects;

import com.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import com.zephyrs.mybatis.semi.metadata.TableInfo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;

import java.lang.reflect.Method;

/**
 * 通用方法处理器
 */
public interface InjectMethod {

    String getId();

    boolean isDirtySelect();

    SqlCommandType getSqlCommandType();

    SqlSource createSqlSource(SemiMybatisConfiguration configuration,
                              Class<?> mapperClass,
                              Class<?> beanClass,
                              Method method,
                              Class<?> parameterTypeClass,
                              LanguageDriver languageDriver);

    MappedStatement addMappedStatement(TableInfo tableInfo,
                                       MapperBuilderAssistant assistant,
                                       String id, SqlSource sqlSource, StatementType statementType,
                                       SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
                                       String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
                                       boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
                                       LanguageDriver lang, String resultSets, boolean dirtySelect);
}
