package top.zephyrs.mybatis.semi.injects.methods;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.injects.AbstractInjectMethod;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.TableInfo;
import org.apache.ibatis.mapping.SqlCommandType;

public class SelectById extends AbstractInjectMethod {
    @Override
    public String getId() {
        return "selectById";
    }

    @Override
    public SqlCommandType getSqlCommandType() {
        return SqlCommandType.SELECT;
    }

    @Override
    public String buildSqlScript(SemiMybatisConfiguration configuration,
                                 Class<?> beanClass, Class<?> parameterTypeClass,
                                 TableInfo tableInfo) {


        ColumnInfo primary = tableInfo.getPkColumn();
        if(primary == null) {
            return null;
        }

        StringBuilder columnScript = new StringBuilder();
        for (ColumnInfo column : tableInfo.getColumns()) {
            if(column.isSelect()) {
                columnScript.append(column.getColumnName()).append(", ");
            }
        }
        String columns = columnScript.substring(0, columnScript.length()-2);
        String sqlTmpl = "select %s from %s where %s";
        return String.format(sqlTmpl,
                columns,
                tableInfo.getTableName(),
                primary.getColumnName() + "=#{id}");
    }
}
