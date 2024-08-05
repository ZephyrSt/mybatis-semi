package top.zephyrs.mybatis.semi.plugins.sensitive;

import top.zephyrs.mybatis.semi.config.SensitiveConfig;
import top.zephyrs.mybatis.semi.exceptions.SensitiveException;
import top.zephyrs.mybatis.semi.executor.ParameterHandlerWrapper;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 敏感数据加解密存储
 */
@Intercepts(@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class}))
public class SensitiveEncryptInterceptor implements Interceptor {

    private final SensitiveConfig config;

    public SensitiveEncryptInterceptor(SensitiveConfig config) {
        this.config = config;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        ParameterHandlerWrapper parameterHandler = (ParameterHandlerWrapper) invocation.getTarget();
        // 获取参数
        Object parameter = parameterHandler.getParameterObject();
        // 判断是否需要加密，空值跳过，未开启功能跳过
        if(Objects.isNull(parameter)|| !config.isOpen()) {
            return invocation.proceed();
        }
        Set<Object> needEncryptObjects = new HashSet<>();
        // ParamMap封装的参数(一般为多个参数或集合)
        if(parameter instanceof MapperMethod.ParamMap) {
            for(Object value: ((MapperMethod.ParamMap<?>) parameter).values()) {
                if(value instanceof Collection) {
                    needEncryptObjects.addAll((Collection<?>)value);
                }else {
                    needEncryptObjects.add(value);
                }
            }
        }
        // Map传参不处理（非 ParamMap）
        else if (Map.class.isAssignableFrom(parameter.getClass())) {
            return invocation.proceed();
        }
        // 普通参数
        else {
            needEncryptObjects.add(parameter);
        }
        try {
            for (Object paramObj : needEncryptObjects) {
                SensitiveHelper.SensitiveBean sensitiveBean = SensitiveHelper.getSensitiveBean(config, paramObj);
                if (sensitiveBean != null) {
                    for (Field field : sensitiveBean.getFields()) {
                        ISensitive sensitive = sensitiveBean.getSensitive(field);
                        Object original = field.get(paramObj);
                        // 只有String 类型的字段才会进行处理
                        if (original instanceof String) {
                            String ciphertext = sensitive.encrypt(paramObj, String.valueOf(original));
                            field.set(paramObj, ciphertext);
                        }
                    }
                }
            }
        }catch (Exception e) {
            throw new SensitiveException("Failed to encrypt sensitive field."+e.getMessage(), e);
        }
        //执行结果
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        //判断是否拦截这个类型对象（根据@Intercepts注解决定），然后决定是返回一个代理对象还是返回原对象。
        //故我们在实现plugin方法时，要判断一下目标类型，如果是插件要拦截的对象时才执行Plugin.wrap方法，否则的话，直接返回目标本身。
        if (target instanceof ParameterHandlerWrapper) {
            return Plugin.wrap(target, this);
        }
        return target;
    }


}