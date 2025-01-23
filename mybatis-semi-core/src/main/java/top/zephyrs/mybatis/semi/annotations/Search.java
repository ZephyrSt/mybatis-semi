package top.zephyrs.mybatis.semi.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 部分查询，使用Query拼接查询条件
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Search {

    /**
     * 查询的语句
     * @return 查询的语句
     */
    String value();
}
