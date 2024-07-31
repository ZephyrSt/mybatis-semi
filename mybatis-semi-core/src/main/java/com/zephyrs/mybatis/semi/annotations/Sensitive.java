package com.zephyrs.mybatis.semi.annotations;

import com.zephyrs.mybatis.semi.plugins.sensitive.DefaultSensitive;
import com.zephyrs.mybatis.semi.plugins.sensitive.NoneSensitive;
import com.zephyrs.mybatis.semi.plugins.sensitive.ISensitive;

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

    Class<? extends ISensitive> value() default DefaultSensitive.class;
}
