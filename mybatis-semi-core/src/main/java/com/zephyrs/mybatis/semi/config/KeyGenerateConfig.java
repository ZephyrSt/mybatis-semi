package com.zephyrs.mybatis.semi.config;

import com.zephyrs.mybatis.semi.plugins.keygenerate.IdType;
import com.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;

public class KeyGenerateConfig {


    /**
     * 主键生成时使用的机器编号(雪花算法)
     */
    private long workId = 0L;

    /**
     * 默认的主键生成策略
     */
    private IdType defaultIdType = IdType.SNOWFLAKE;

    /**
     * 自定义的主键生成策略的实现类
     */
    private Class<? extends KeyCreator<?>> customKeyCreator;

    public long getWorkId() {
        return workId;
    }

    public void setWorkId(long workId) {
        this.workId = workId;
    }

    public IdType getDefaultIdType() {
        return defaultIdType;
    }

    public void setDefaultIdType(IdType defaultIdType) {
        this.defaultIdType = defaultIdType;
    }

    public Class<? extends KeyCreator<?>> getCustomKeyCreator() {
        return customKeyCreator;
    }

    public void setCustomKeyCreator(Class<? extends KeyCreator<?>> customKeyCreator) {
        this.customKeyCreator = customKeyCreator;
    }
}
