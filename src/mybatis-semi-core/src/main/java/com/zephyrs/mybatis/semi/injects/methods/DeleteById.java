package com.zephyrs.mybatis.semi.injects.methods;

import com.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import com.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import com.zephyrs.mybatis.semi.metadata.ColumnInfo;
import com.zephyrs.mybatis.semi.metadata.TableInfo;
import org.apache.ibatis.mapping.SqlCommandType;

public class DeleteById extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "deleteById";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.DELETE;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {

        ColumnInfo primary = tableInfo.getPkColumn();
        if(primary == null) {
            return null;
        }
        String sql;
        if (tableInfo.isLogical()) {
            String sqlTmpl = "UPDATE %s SET %s=%s WHERE %s";
            String deletedColumn = tableInfo.getLogicalColumn().getColumnName();
            sql = String.format(sqlTmpl,
                    tableInfo.getTableName(),
                    deletedColumn,
                    tableInfo.getDeletedValue(),
                    primary.getColumnName()+"=#{id}");
        } else {
            String sqlTmpl = "DELETE FROM %s WHERE %s";
            sql = String.format(sqlTmpl, tableInfo.getTableName(),
                    primary.getColumnName()+"=#{id}");
        }
        return sql;
    }
}
