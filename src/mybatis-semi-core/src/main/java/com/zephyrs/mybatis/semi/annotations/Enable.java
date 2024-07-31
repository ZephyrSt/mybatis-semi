package com.zephyrs.mybatis.semi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用/禁用标识
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Enable {

    /**
     * 启用标识
     */
    String enabledValue();

    /**
     * 禁用标识
     */
    String disabledValue();
}
