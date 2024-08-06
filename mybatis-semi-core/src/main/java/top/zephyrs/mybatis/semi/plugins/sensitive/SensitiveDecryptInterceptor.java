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
        if (Objects.isNull(resultObject)) {
            return null;
        }
        // 判断是否需要解密
        SemiMybatisConfiguration configuration = (SemiMybatisConfiguration) mappedStatement.getConfiguration();
        boolean needDecrypt = true;
        if (!configuration.isSensitiveDecrypt(mappedStatement.getId())) {
            needDecrypt = false;
        }
        // 不使用Null值替换未解密字段
        if (!needDecrypt && !config.isUseNullOnNotDecrypt()) {
            return resultObject;
        }
        // 查询方法的返回结果都是List
        if(((List<?>)resultObject).isEmpty()) {
            return resultObject;
        }
        SensitiveHelper.SensitiveBean sensitiveBean = SensitiveHelper.getSensitiveBean(config, ((List<?>)resultObject).get(0));
        if(sensitiveBean == null) {
            return resultObject;
        }
        try {
            for (Object result : (List<?>) resultObject) {
                // 返回结果是Map， 不解析
                if (result instanceof Map) {
                    return resultObject;
                }
                if (needDecrypt) {
                    for (Field field : sensitiveBean.getFields()) {
                        ISensitive sensitive = sensitiveBean.getSensitive(field);
                        Object ciphertext = field.get(result);
                        String original = ciphertext == null ? null : sensitive.decrypt(result, String.valueOf(ciphertext));
                        field.set(result, original);
                    }
                } else if (config.isUseNullOnNotDecrypt()) {
                    for (Field field : sensitiveBean.getFields()) {
                        field.set(result, null);
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