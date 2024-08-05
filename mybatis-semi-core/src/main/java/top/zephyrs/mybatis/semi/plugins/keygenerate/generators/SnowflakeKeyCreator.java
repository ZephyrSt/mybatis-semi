package top.zephyrs.mybatis.semi.plugins.keygenerate.generators;

import top.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;

public class SnowflakeKeyCreator implements KeyCreator<Long> {

    private final Snowflake snowflake;

    public SnowflakeKeyCreator(long workId) {
        this.snowflake = new Snowflake(workId);
    }

    @Override
    public Long nextId() {
        return snowflake.nextId();
    }
}
