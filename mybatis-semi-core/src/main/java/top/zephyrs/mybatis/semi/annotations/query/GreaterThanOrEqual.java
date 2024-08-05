package top.zephyrs.mybatis.semi.annotations.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询条件： &gt;=
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface GreaterThanOrEqual {

    /**
     * 查询的列对应Bean的属性（Column的字段名称）
     * @return Bean字段名称
     */
    String value() default "";
}
