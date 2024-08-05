package top.zephyrs.mybatis.semi.config;

/**
 * 启用/禁用全局配置
 */
public class EnableConfig {

    /**
     * 全局设置-启用禁用-列名
     */
    private String column;

    /**
     * 全局设置-启用禁用-启用值
     */
    private String enabledValue;

    /**
     * 全局设置-启用禁用-禁用值
     */
    private String disabledValue;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getEnabledValue() {
        return enabledValue;
    }

    public void setEnabledValue(String enabledValue) {
        this.enabledValue = enabledValue;
    }

    public String getDisabledValue() {
        return disabledValue;
    }

    public void setDisabledValue(String disabledValue) {
        this.disabledValue = disabledValue;
    }
}
