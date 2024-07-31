package com.zephyrs.mybatis.semi.injects.methods;

import com.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import com.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import com.zephyrs.mybatis.semi.metadata.ColumnInfo;
import com.zephyrs.mybatis.semi.metadata.TableInfo;
import org.apache.ibatis.mapping.SqlCommandType;

public class Disable extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "disable";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.UPDATE;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {

        if(!tableInfo.isEnable()) {
            return null;
        }

        ColumnInfo primary = tableInfo.getPkColumn();
        if(primary == null) {
            return null;
        }

        String sqlTmpl = "UPDATE %s SET %s=%s WHERE %s";
        return String.format(sqlTmpl,
                tableInfo.getTableName(),
                tableInfo.getEnableColumn().getColumnName(),
                tableInfo.getDisabledValue(),
                primary.getColumnName() + "=#{id}");

    }
}
