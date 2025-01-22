package top.zephyrs.mybatis.semi.injects.methods;

import org.apache.ibatis.type.UnknownTypeHandler;
import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.TableInfo;
import org.apache.ibatis.mapping.SqlCommandType;

public class UpdateById extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "updateById";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.UPDATE;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {


        ColumnInfo primary = tableInfo.getPkColumn();
        if(primary == null) {
            return null;
        }

        StringBuilder setScript = new StringBuilder("<set>");
        for (ColumnInfo column : tableInfo.getColumns()) {
            if(!column.isPK() && column.isExists() && column.isUpdate()) {
                if(column.isIfNullUpdate()) {
                    if(column.getTypeHandler() == null || column.getTypeHandler().equals(UnknownTypeHandler.class)) {
                        setScript.append(column.getColumnName()).append("=#{").append(column.getFieldName())
                                .append("}, ");
                    }else {
                        setScript.append(column.getColumnName()).append("=#{").append(column.getFieldName())
                                .append(", typeHandler=").append(column.getTypeHandler().getTypeName())
                                .append("}, ");
                    }
                }else {
                    if(column.getTypeHandler() == null || column.getTypeHandler().equals(UnknownTypeHandler.class)) {
                        setScript.append("<if test=\"").append(column.getFieldName()).append(" != null\">")
                                .append(column.getColumnName()).append("=#{").append(column.getFieldName())
                                .append("}, </if>");
                    }else {
                        setScript.append("<if test=\"").append(column.getFieldName()).append(" != null\">")
                                .append(column.getColumnName()).append("=#{").append(column.getFieldName())
                                .append(", typeHandler=").append(column.getTypeHandler().getTypeName())
                                .append("}, </if>");
                    }
                }
            }
        }
        setScript.append("</set>");
        String sqlTmpl = "<script>update %s %s where %s</script>";
        return String.format(sqlTmpl, tableInfo.getTableName(), setScript,
                primary.getColumnName() + "=#{" + primary.getFieldName() + "}");

    }
}
