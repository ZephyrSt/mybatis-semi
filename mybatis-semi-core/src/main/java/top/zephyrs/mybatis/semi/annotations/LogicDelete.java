package top.zephyrs.mybatis.semi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 逻辑删除标识
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface LogicDelete {

    /**
     * @return 已删除标识
     */
    String deletedValue();

    /**
     * @return 未删除标识
     */
    String existsValue();
}
