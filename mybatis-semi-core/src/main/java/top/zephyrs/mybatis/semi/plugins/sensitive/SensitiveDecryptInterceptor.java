package top.zephyrs.mybatis.semi.plugins.sensitive;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.config.SensitiveConfig;
import top.zephyrs.mybatis.semi.exceptions.SensitiveException;
import top.zephyrs.mybatis.semi.executor.ResultSetHandlerWrapper;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.*;

/**
 * 敏感数据加解密存储
 *
 */
@Intercepts(@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class}))
public class SensitiveDecryptInterceptor implements Interceptor {

    private SensitiveConfig config;

    public SensitiveDecryptInterceptor(SensitiveConfig config) {
        this.config = config;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        ResultSetHandlerWrapper resultSetHandler = (ResultSetHandlerWrapper)invocation.getTarget();
        MappedStatement mappedStatement = resultSetHandler.getMappedStatement();
        Object resultObject = invocation.proceed();
        // null值不需要解密
        if (Objects.isNull(resultObject)) {
            return null;
        }
        // 查询方法的返回结果都是List，空集合直接返回
        if(((List<?>)resultObject).isEmpty()) {
            return resultObject;
        }
        // 加密需要的配置信息不存在，直接返回
        SensitiveHelper.SensitiveBean sensitiveBean = SensitiveHelper.getSensitiveBean(config, ((List<?>)resultObject).get(0));
        if(sensitiveBean == null) {
            return resultObject;
        }
        // 判断是否需要解密
        SemiMybatisConfiguration configuration = (SemiMybatisConfiguration) mappedStatement.getConfiguration();
        boolean needDecrypt = true;
        if (!configuration.isSensitiveDecrypt(mappedStatement.getId())) {
            needDecrypt = false;
        }
        try {
            for (Object result : (List<?>) resultObject) {
                // 基于Bean注解配置解析，返回结果是Map， 不解析,
                if (result instanceof Map) {
                    return resultObject;
                }
                for (Field field : sensitiveBean.getFields()) {
                    ISensitive sensitive = sensitiveBean.getSensitive(field);
                    if (sensitive != null) {
                        Object ciphertext = field.get(result);
                        //不需要解密或忽略解密
                        if (!needDecrypt || sensitiveBean.isIgnore(field)) {
                            String original = ciphertext == null ? null : sensitive.normal(result, String.valueOf(ciphertext));
                            field.set(result, original);
                        }else {
                            String original = ciphertext == null ? null : sensitive.decrypt(result, String.valueOf(ciphertext));
                            field.set(result, original);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new SensitiveException("Failed to decrypt result：" + e.getMessage(), e);
        }
        return resultObject;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof ResultSetHandlerWrapper) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

}