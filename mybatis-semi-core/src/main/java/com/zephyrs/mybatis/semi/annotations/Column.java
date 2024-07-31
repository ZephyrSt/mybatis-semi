package com.zephyrs.mybatis.semi.annotations;

import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {

    /**
     * 列名称
     * @return 列名称
     */
    String value() default "";

    /**
     * 是否存在列
     * @return Boolean true: 表中存在此列 false：不是表中的列
     */
    boolean exists() default true;

    /**
     * 是否查询
     * @return Boolean
     */
    boolean select() default true;

    /**
     * 插入时是否插入
     * @return Boolean
     */
    boolean insert() default true;

    /**
     * 修改时是否更新
     * @return Boolean
     */
    boolean update() default true;

    /**
     * 字段为空时是否插入
     * @return Boolean
     */
    boolean ifNullInsert() default false;

    /**
     * 字段为空时是否修改
     * @return Boolean
     */
    boolean ifNullUpdate() default false;

    /**
     * 类型转换
     * @return TypeHandler 类型处理器
     */
    Class<? extends TypeHandler<?>> typeHandler() default UnknownTypeHandler.class;
}
