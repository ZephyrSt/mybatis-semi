package top.zephyrs.mybatis.semi.injects.methods;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
import org.apache.ibatis.mapping.SqlCommandType;

public class ToggleEnable extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "toggleEnable";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.UPDATE;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 MetaInfo metaInfo) {

        if(!metaInfo.isEnable()) {
            return EMPTY_STR;
        }

        ColumnInfo primary = metaInfo.getPkColumn();
        if(primary == null) {
            return EMPTY_STR;
        }
        ColumnInfo enableColumn = metaInfo.getEnableColumn();
        String sqlTmpl = "UPDATE %s SET %s=CASE %s WHEN %s THEN %s ELSE %s END WHERE %s";
        return String.format(sqlTmpl,
                metaInfo.getTableName(),
                enableColumn.getColumnName(),
                enableColumn.getColumnName(),
                metaInfo.getEnabledValue(),
                metaInfo.getDisabledValue(),
                metaInfo.getEnabledValue(),
                primary.getColumnName() + "=#{id}");

    }
}
