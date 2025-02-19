package top.zephyrs.mybatis.semi.plugins.keygenerate;

import org.apache.ibatis.binding.MapperMethod;
import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.metadata.MetaHelper;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.util.Collection;
import java.util.Collections;

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
            if(parameter == null) {
                return invocation.proceed();
            }
            Class type = parameter.getClass();
            if(parameter instanceof MapperMethod.ParamMap) {
                for(Object params: ((MapperMethod.ParamMap<?>) parameter).values()) {
                    if(params instanceof Collection ) {
                        ((Collection<?>) params).forEach(item ->{
                            MetaInfo metaInfo = MetaHelper.getMetaInfo(item.getClass());
                            if(metaInfo != null) {
                                try {
                                    KeyHelper.setKey(configuration, item, metaInfo);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }else {
                        MetaInfo metaInfo = MetaHelper.getMetaInfo(type);
                        if(metaInfo != null) {
                            KeyHelper.setKey(configuration, params, metaInfo);
                        }
                    }
                }
            }else {
                if(parameter instanceof Collection ) {
                    ((Collection<?>) parameter).forEach(item ->{
                        MetaInfo metaInfo = MetaHelper.getMetaInfo(item.getClass());
                        if(metaInfo != null) {
                            try {
                                KeyHelper.setKey(configuration, item, metaInfo);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }else {
                    MetaInfo metaInfo = MetaHelper.getMetaInfo(type);
                    if(metaInfo != null) {
                        KeyHelper.setKey(configuration, parameter, metaInfo);
                    }
                }
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