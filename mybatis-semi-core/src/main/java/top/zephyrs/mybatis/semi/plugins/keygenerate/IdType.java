package top.zephyrs.mybatis.semi.plugins.keygenerate;

/**
 * 主键生成策略
 */
public enum IdType {

    /**
     * 使用默认设置(全局IdType)
     */
    DEFAULT,
    /**
     * 不生成
     */
    NONE,

    AUTO,
    /**
     * UUID
     */
    UUID,

    /**
     * 雪花算法
     */
    SNOWFLAKE,
    /**
     * 自定义
     */
    CUSTOM;
}
