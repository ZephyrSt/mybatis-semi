package com.zephyrs.mybatis.semi.injects.methods;

import com.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import com.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import com.zephyrs.mybatis.semi.metadata.ColumnInfo;
import com.zephyrs.mybatis.semi.metadata.TableInfo;
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
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {

        StringBuilder columnScript = new StringBuilder();
        for (ColumnInfo column : tableInfo.getColumns()) {
            if(column.isSelect()) {
                columnScript.append(column.getColumnName()).append(", ");
            }
        }
        Integer maxRow = configuration.getGlobalConfig().getSelectAllMaxRow();
        String columns = columnScript.substring(0, columnScript.length()-2);
        //逻辑删除的不查询
        if(tableInfo.isLogical()) {
            String sqlTemplate = "SELECT %s FROM %s WHERE %s=%s LIMIT 0, "+ maxRow;
            return String.format(sqlTemplate,
                    columns,
                    tableInfo.getTableName(),
                    tableInfo.getLogicalColumn(),
                    tableInfo.getNoDeletedValue(),
                    maxRow);
        }else {
            String sqlTemplate = "SELECT %s FROM %s LIMIT 0, "+maxRow;
            return String.format(sqlTemplate, columns, tableInfo.getTableName(), maxRow);
        }
    }
}
