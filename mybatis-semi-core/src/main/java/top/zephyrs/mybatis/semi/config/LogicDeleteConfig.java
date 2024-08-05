package top.zephyrs.mybatis.semi.config;

/**
 * 逻辑删除全局配置
 */
public class LogicDeleteConfig {

    /**
     * 全局设置-逻辑删除-字段名
     */
    private String column;

    /**
     * 全局设置-逻辑删除-删除值
     */
    private String deletedValue;

    /**
     * 全局设置-逻辑删除-未删除值
     */
    private String existsValue;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getDeletedValue() {
        return deletedValue;
    }

    public void setDeletedValue(String deletedValue) {
        this.deletedValue = deletedValue;
    }

    public String getExistsValue() {
        return existsValue;
    }

    public void setExistsValue(String existsValue) {
        this.existsValue = existsValue;
    }
}
