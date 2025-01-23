package top.zephyrs.mybatis.semi.injects;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
import top.zephyrs.mybatis.semi.metadata.MetaHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;

import java.lang.reflect.Method;

/**
 * 通用方法
 */
public abstract class AbstractInjectMethod implements InjectMethod {

    public static final String EMPTY_STR = "";

    @Override
    public String databaseId() {
        return "";
    }
    @Override
    public boolean isDirtySelect() {
        return false;
    }

    @Override
    public MappedStatement addMappedStatement(MetaInfo metaInfo,
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
                                     MetaInfo metaInfo,
                                     Method method,
                                     Class<?> parameterTypeClass,
                                     LanguageDriver languageDriver) {

        if (metaInfo == null) {
            return null;
        }
        String sqlScript = this.buildSqlScript(configuration, metaInfo);
        if(sqlScript == null) {
            return null;
        }
        return languageDriver.createSqlSource(configuration, sqlScript, parameterTypeClass);
    }

    /**
     * 构建sql语句
     * @param configuration mybatis配置信息
//     * @param beanClass Mapper的泛型的具体类型
//     * @param parameterTypeClass 参数类型
     * @param metaInfo 对应的 表信息，包含列，字段，表名等等
     * @return sql语句
     */
    protected abstract String buildSqlScript(SemiMybatisConfiguration configuration, MetaInfo metaInfo);

}
