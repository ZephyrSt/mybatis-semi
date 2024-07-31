package com.zephyrs.mybatis.semi.config;

public class GlobalConfig {

    private LogicDeleteConfig logic;

    private EnableConfig enable;

    private KeyGenerateConfig keyGenerate;

    private SensitiveConfig sensitive;

    /**
     * selectAll 方法的最大查询行数
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
