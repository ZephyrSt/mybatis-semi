package top.zephyrs.mybatis.semi.injects.methods;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
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
    public String buildSqlScript(SemiMybatisConfiguration configuration, MetaInfo metaInfo) {

        ColumnInfo primary = metaInfo.getPkColumn();
        if(primary == null) {
            return null;
        }
        String sql;
        if (metaInfo.isLogical()) {
            String sqlTmpl = "UPDATE %s SET %s=%s WHERE %s";
            String deletedColumn = metaInfo.getLogicalColumn().getColumnName();
            sql = String.format(sqlTmpl,
                    metaInfo.getTableName(),
                    deletedColumn,
                    metaInfo.getDeletedValue(),
                    primary.getColumnName()+"=#{id}");
        } else {
            String sqlTmpl = "DELETE FROM %s WHERE %s";
            sql = String.format(sqlTmpl, metaInfo.getTableName(),
                    primary.getColumnName()+"=#{id}");
        }
        return sql;
    }
}
