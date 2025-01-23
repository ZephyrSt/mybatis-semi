package top.zephyrs.mybatis.semi.annotations.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询条件：in
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface In {
    /**
     * 查询的列对应Bean的属性（类属性名称）
     * @return Bean字段名称
     */
    String value() default "";

    /**
     * 查询语句中的列名称
     * @return 列的名称
     */
    String column() default "";
}
