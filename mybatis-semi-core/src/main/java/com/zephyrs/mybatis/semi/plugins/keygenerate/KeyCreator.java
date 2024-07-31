package com.zephyrs.mybatis.semi.plugins.keygenerate;

public interface KeyCreator<T> {

    /**
     * 获取ID
     */
    T nextId();
}
