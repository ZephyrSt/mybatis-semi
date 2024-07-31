package com.zephyrs.mybatis.semi.annotations;


import com.zephyrs.mybatis.semi.plugins.keygenerate.IdType;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface Primary {

    String value() default "";

    /**
     * 主键生成策略
     * @return 注解生成策略
     */
    IdType idType() default IdType.DEFAULT;

}
