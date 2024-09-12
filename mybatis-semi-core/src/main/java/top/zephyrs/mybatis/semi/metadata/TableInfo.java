package top.zephyrs.mybatis.semi.metadata;

import java.util.List;

/**
 * 表-Bean映射的信息
 */
public class TableInfo {


    /**
     * 表名称
     */
    private String tableName;

    /**
     * 是否逻辑删除
     */
    private boolean logical;

    /**
     * 逻辑删除的字段
     */
    private ColumnInfo logicalColumn;
    private String deletedValue;
    private String noDeletedValue;

    private boolean enable;
    private ColumnInfo enableColumn;
    private String enabledValue;
    private String disabledValue;

    /**
     * 对应的类
     */
    private Class<?> type;

    /**
     * 主键列
     */
    private ColumnInfo pkColumn;

    /**
     * 是否使用ResultMap
     */
    private boolean useResultMap;

    /**
     * 包含的列
     */
    private List<ColumnInfo> columns;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public ColumnInfo getPkColumn() {
        return pkColumn;
    }

    public void setPkColumn(ColumnInfo pkColumn) {
        this.pkColumn = pkColumn;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    public boolean isLogical() {
        return logical;
    }

    public void setLogical(boolean logical) {
        this.logical = logical;
    }

    public ColumnInfo getLogicalColumn() {
        return logicalColumn;
    }

    public void setLogicalColumn(ColumnInfo logicalColumn) {
        this.logicalColumn = logicalColumn;
    }

    public String getDeletedValue() {
        return deletedValue;
    }

    public void setDeletedValue(String deletedValue) {
        this.deletedValue = deletedValue;
    }

    public String getNoDeletedValue() {
        return noDeletedValue;
    }

    public void setNoDeletedValue(String noDeletedValue) {
        this.noDeletedValue = noDeletedValue;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public ColumnInfo getEnableColumn() {
        return enableColumn;
    }

    public void setEnableColumn(ColumnInfo enableColumn) {
        this.enableColumn = enableColumn;
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

    public boolean isUseResultMap() {
        return useResultMap;
    }

    public void setUseResultMap(boolean useResultMap) {
        this.useResultMap = useResultMap;
    }
}

