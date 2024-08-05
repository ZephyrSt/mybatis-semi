package top.zephyrs.mybatis.semi.config;

/**
 * 通用功能全局配置信息
 */
public class GlobalConfig {

    /**
     * 逻辑删除配置
     */
    private LogicDeleteConfig logic;

    /**
     * 启用/禁用配置
     */
    private EnableConfig enable;

    /**
     * 主键配置
     */
    private KeyGenerateConfig keyGenerate;

    /**
     * 敏感字段加解密配置
     */
    private SensitiveConfig sensitive;

    /**
     * selectAll 方法的最大查询行数，默认1000
     */
    private Integer selectAllMaxRow = 1000;

    public LogicDeleteConfig getLogic() {
        return logic;
    }

    public void setLogic(LogicDeleteConfig logic) {
        this.logic = logic;
    }

    public EnableConfig getEnable() {
        return enable;
    }

    public void setEnable(EnableConfig enable) {
        this.enable = enable;
    }

    public KeyGenerateConfig getKeyGenerate() {
        return keyGenerate;
    }

    public void setKeyGenerate(KeyGenerateConfig keyGenerate) {
        this.keyGenerate = keyGenerate;
    }

    public Integer getSelectAllMaxRow() {
        return selectAllMaxRow;
    }

    public void setSelectAllMaxRow(Integer selectAllMaxRow) {
        this.selectAllMaxRow = selectAllMaxRow;
    }

    public SensitiveConfig getSensitive() {
        return sensitive;
    }

    public void setSensitive(SensitiveConfig sensitive) {
        this.sensitive = sensitive;
    }
}
