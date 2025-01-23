package top.zephyrs.mybatis.semi.plugins.keygenerate;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.metadata.MetaHelper;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

/**
 * 自动生成主键
 */
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class KeyGenerateInterceptor implements Interceptor {

    private final SemiMybatisConfiguration configuration;

    public KeyGenerateInterceptor(SemiMybatisConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement)invocation.getArgs()[0];
        // 获取 SQL
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if(sqlCommandType == SqlCommandType.INSERT) {
            // 获取参数
            Object parameter = invocation.getArgs()[1];
            MetaInfo metaInfo = MetaHelper.getMetaInfo(parameter.getClass());
            if(metaInfo != null) {
                KeyHelper.setKey(configuration, parameter, metaInfo);
            }
        }
        //执行结果
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

}