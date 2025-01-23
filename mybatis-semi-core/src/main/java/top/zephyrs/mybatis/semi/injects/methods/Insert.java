package top.zephyrs.mybatis.semi.injects.methods;

import org.apache.ibatis.type.UnknownTypeHandler;
import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
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
    public MappedStatement addMappedStatement(MetaInfo metaInfo,
                                              MapperBuilderAssistant assistant,
                                              String id, SqlSource sqlSource, StatementType statementType, SqlCommandType sqlCommandType,
                                              Integer fetchSize, Integer timeout,
                                              String parameterMap, Class<?> parameterType,
                                              String resultMap, Class<?> resultType, ResultSetType resultSetType,
                                              boolean flushCache, boolean useCache, boolean resultOrdered,
                                              KeyGenerator keyGenerator, String keyProperty, String keyColumn,
                                              String databaseId, LanguageDriver lang, String resultSets, boolean dirtySelect) {
        if(metaInfo == null) {
            return null;
        }
        // 没有设置主键列则不代理
        ColumnInfo primary = metaInfo.getPkColumn();
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

        return super.addMappedStatement(metaInfo, assistant,
                id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, lang, resultSets, dirtySelect);
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration, MetaInfo metaInfo) {

        StringBuilder columnScript = new StringBuilder("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
        StringBuilder paramScript = new StringBuilder("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">");
        for (ColumnInfo column : metaInfo.getColumns()) {
            if(column.isPK() && column.getIdType()== IdType.AUTO) {
                continue;
            }
            if (column.isExists() && column.isInsert()) {
                if(column.isIfNullInsert()) {
                    columnScript.append(column.getColumnName()).append(",");
                    if(column.getTypeHandler() == null || column.getTypeHandler().equals(UnknownTypeHandler.class)) {
                        paramScript.append("#{"+column.getFieldName()+"},");
                    }else {
                        paramScript.append("#{"+column.getFieldName()+", typeHandler=" + column.getTypeHandler().getTypeName()+"},");
                    }
                } else {
                    columnScript.append("<if test=\""+column.getFieldName()+"!=null\">"+column.getColumnName()+",</if>");
                    if(column.getTypeHandler() == null || column.getTypeHandler().equals(UnknownTypeHandler.class)) {
                        paramScript.append("<if test=\""+column.getFieldName()+"!=null\">#{"+column.getFieldName()+"},</if>");
                    }else {
                        paramScript.append("<if test=\""+column.getFieldName()+"!=null\">#{"+column.getFieldName()+", typeHandler=" + column.getTypeHandler().getTypeName()+"},</if>");
                    }
                }
            }
        }
        columnScript.append("</trim>");
        paramScript.append("</trim>");
        String sqlTmpl = "<script>INSERT INTO %s %s VALUES %s</script>";
        return String.format(sqlTmpl,
                metaInfo.getTableName(),
                columnScript,
                paramScript);
    }

}
