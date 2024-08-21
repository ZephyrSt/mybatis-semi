package top.zephyrs.mybatis.semi.annotations;

import top.zephyrs.mybatis.semi.plugins.sensitive.DefaultSensitive;
import top.zephyrs.mybatis.semi.plugins.sensitive.ISensitive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据加密/解密
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Sensitive {

    /**
     * 敏感数据加解密实现
     * @return 默认：{@link top.zephyrs.mybatis.semi.plugins.sensitive.DefaultSensitive}
     */
    Class<? extends ISensitive> value() default DefaultSensitive.class;

    /**
     * 解密时字段是否需要解密，默认是
     * @return 默认 true: 查询结果集需要解密时，解密， false: 查询结果集需要解密时，跳过此字段
     */
    boolean needDecrypt() default true;
}
