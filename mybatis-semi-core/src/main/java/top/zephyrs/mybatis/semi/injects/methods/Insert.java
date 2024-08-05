package top.zephyrs.mybatis.semi.injects.methods;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.TableInfo;
import top.zephyrs.mybatis.semi.plugins.keygenerate.IdType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;

public class Insert extends AbstractInjectMethod {

    @Override
    public String getId() {
        return "insert";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.INSERT;
    }

    @Override
    public MappedStatement addMappedStatement(TableInfo tableInfo,
                                              MapperBuilderAssistant assistant,
                                              String id, SqlSource sqlSource, StatementType statementType, SqlCommandType sqlCommandType,
                                              Integer fetchSize, Integer timeout,
                                              String parameterMap, Class<?> parameterType,
                                              String resultMap, Class<?> resultType, ResultSetType resultSetType,
                                              boolean flushCache, boolean useCache, boolean resultOrdered,
                                              KeyGenerator keyGenerator, String keyProperty, String keyColumn,
                                              String databaseId, LanguageDriver lang, String resultSets, boolean dirtySelect) {

        // 没有设置主键列则不代理
        ColumnInfo primary = tableInfo.getPkColumn();
        if(primary == null) {
            return null;
        }
        if(primary.getIdType() == IdType.AUTO) {
            keyGenerator = Jdbc3KeyGenerator.INSTANCE;
            keyProperty = primary.getFieldName();
            keyColumn = primary.getColumnName();
        }else if(primary.getIdType() == IdType.NONE) {
            keyGenerator = NoKeyGenerator.INSTANCE;
        }

        return super.addMappedStatement(tableInfo, assistant,
                id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, lang, resultSets, dirtySelect);
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {

        StringBuilder columnScript = new StringBuilder("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
        StringBuilder parameterScript = new StringBuilder("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
        for (ColumnInfo column : tableInfo.getColumns()) {
            if(column.isPK() && column.getIdType()== IdType.AUTO) {
                continue;
            }
            if (column.isExists() && column.isInsert()) {
                if(column.isIfNullInsert()) {
                    columnScript.append(column.getColumnName());
                    parameterScript.append(column.getFieldName());
                } else {
                    columnScript.append("<if test=\"").append(column.getFieldName()).append(" != null\">`").append(column.getColumnName()).append("`,</if>");
                    parameterScript.append("<if test=\"").append(column.getFieldName()).append(" != null\">#{").append(column.getFieldName()).append("},</if>");
                }
            }
        }
        columnScript.append("</trim>");
        parameterScript.append("</trim>");
        String sqlTmpl = "<script>INSERT INTO %s %s VALUES %s</script>";
        return String.format(sqlTmpl, tableInfo.getTableName(), columnScript, parameterScript);
    }

}
