package com.zephyrs.mybatis.semi.plugins.keygenerate;

import com.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import com.zephyrs.mybatis.semi.metadata.MetadataHelper;
import com.zephyrs.mybatis.semi.metadata.TableInfo;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

/**
 * 自动生成主键
 * @link Primary 标识的字段,在为null时，会根据指定生成器生成值
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
            TableInfo tableInfo = MetadataHelper.getTableInfo(parameter.getClass());
            if(tableInfo != null) {
                KeyHelper.setKey(configuration, parameter, tableInfo);
            }
        }
        //执行结果
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        //判断是否拦截这个类型对象（根据@Intercepts注解决定），然后决定是返回一个代理对象还是返回原对象。
        //故我们在实现plugin方法时，要判断一下目标类型，如果是插件要拦截的对象时才执行Plugin.wrap方法，否则的话，直接返回目标本身。
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

}