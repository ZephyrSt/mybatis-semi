package top.zephyrs.mybatis.semi.injects.methods;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.TableInfo;
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
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {

        if(!tableInfo.isEnable()) {
            return null;
        }

        ColumnInfo primary = tableInfo.getPkColumn();
        if(primary == null) {
            return null;
        }
        ColumnInfo enableColumn = tableInfo.getEnableColumn();
        String sqlTmpl = "UPDATE %s SET %s=CASE %s WHEN %s THEN %s ELSE %s END WHERE %s";
        return String.format(sqlTmpl,
                tableInfo.getTableName(),
                enableColumn.getColumnName(),
                enableColumn.getColumnName(),
                tableInfo.getEnabledValue(),
                tableInfo.getDisabledValue(),
                tableInfo.getEnabledValue(),
                primary.getColumnName() + "=#{id}");

    }
}
