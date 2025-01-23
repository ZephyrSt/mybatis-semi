package top.zephyrs.mybatis.semi.injects.methods;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.annotations.Search;
import top.zephyrs.mybatis.semi.metadata.MetaHelper;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;

import java.lang.reflect.Method;

public class SelectByQueryAnnotation extends SelectByQuery {

    @Override
    public String getId() {
        return "SelectByQueryAnnotation";
    }

    @Override
    public SqlSource createSqlSource(SemiMybatisConfiguration configuration,
                                     MetaInfo metaInfo,
                                     Method method,
                                     Class<?> parameterTypeClass,
                                     LanguageDriver languageDriver) {
        return (Object parameterObject) -> {
            Search Search = method.getAnnotation(Search.class);
            if(Search.value() == null) {
                return null;
            }
            // 查询的条件
            String whereScript = super.getWhereScript(parameterObject, metaInfo);
            String sqlScript;

            String SQL_TMPL = "<script>%s %s</script>";
            sqlScript = String.format(SQL_TMPL, Search.value(), whereScript);
            SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlScript, parameterTypeClass);
            return sqlSource.getBoundSql(parameterObject);
        };
    }

}
