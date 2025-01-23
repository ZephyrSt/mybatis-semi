package top.zephyrs.mybatis.semi.injects;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;

import java.lang.reflect.Method;

/**
 * 通用方法
 */
public interface InjectMethod {

    String getId();

    String databaseId();

    boolean isDirtySelect();

    SqlCommandType getSqlCommandType();

    SqlSource createSqlSource(SemiMybatisConfiguration configuration,
                              MetaInfo metaInfo,
                              Method method,
                              Class<?> parameterTypeClass,
                              LanguageDriver languageDriver);

    MappedStatement addMappedStatement(MetaInfo metaInfo,
                                       MapperBuilderAssistant assistant,
                                       String id, SqlSource sqlSource, StatementType statementType,
                                       SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
                                       String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
                                       boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
                                       LanguageDriver lang, String resultSets, boolean dirtySelect);
}
