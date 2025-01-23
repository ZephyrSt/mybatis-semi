package top.zephyrs.mybatis.semi.injects.methods;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
import org.apache.ibatis.mapping.SqlCommandType;

public class Enable extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "enable";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.UPDATE;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration, MetaInfo metaInfo) {

        if(!metaInfo.isEnable()) {
            return EMPTY_STR;
        }

        ColumnInfo primary = metaInfo.getPkColumn();
        if(primary == null) {
            return EMPTY_STR;
        }

        String sqlTmpl = "UPDATE %s SET %s=%s WHERE %s";
        return String.format(sqlTmpl,
                metaInfo.getTableName(),
                metaInfo.getEnableColumn().getColumnName(),
                metaInfo.getEnabledValue(),
                primary.getColumnName() + "=#{id}");
    }
}
