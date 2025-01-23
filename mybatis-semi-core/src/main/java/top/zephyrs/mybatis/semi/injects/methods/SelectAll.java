package top.zephyrs.mybatis.semi.injects.methods;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
import org.apache.ibatis.mapping.SqlCommandType;

public class SelectAll extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "selectAll";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.SELECT;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration, MetaInfo metaInfo) {

        StringBuilder columnScript = new StringBuilder();
        for (ColumnInfo column : metaInfo.getColumns()) {
            if(column.isSelect()) {
                columnScript.append(column.getColumnName()).append(", ");
            }
        }
        String columns = columnScript.substring(0, columnScript.length()-2);
        //逻辑删除的不查询
        if(metaInfo.isLogical()) {
            String sqlTemplate = "SELECT %s FROM %s WHERE %s=%s";
            return String.format(sqlTemplate,
                    columns,
                    metaInfo.getTableName(),
                    metaInfo.getLogicalColumn().getColumnName(),
                    metaInfo.getNoDeletedValue());
        }else {
            String sqlTemplate = "SELECT %s FROM %s";
            return String.format(sqlTemplate, columns, metaInfo.getTableName());
        }
    }
}
