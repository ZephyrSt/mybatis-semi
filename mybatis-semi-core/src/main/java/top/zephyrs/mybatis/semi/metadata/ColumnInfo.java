package top.zephyrs.mybatis.semi.metadata;

import top.zephyrs.mybatis.semi.plugins.keygenerate.IdType;
import top.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Field;

/**
 * 表-Bean映射的列信息
 */
public class ColumnInfo {

    /**
     * 查询时是否查询字段
     */
    private boolean select;
    /**
     * 新增时是否插入
     */
    private boolean insert;
    /**
     * 更新时是否修改
     */
    private boolean update;

    /**
     * 为null值时，新增数据是否设置
     */
    private boolean ifNullInsert;

    /**
     * 为null值时，修改数据是否设置
     */
    private boolean ifNullUpdate;

    /**
     * 是否是字段
     */
    private boolean exists;


    /**
     * 是否是主键
     */
    private boolean isPK = false;

    /**
     * 主键生成策略
     */
    private IdType idType;

    /**
     * 主键生成器
     */
    private KeyCreator<?> keyCreator;

    /**
     * 列名
     */
    private String columnName;


    /**
     * 对应的属性名
     */
    private String fieldName;
    /**
     * 字段
     */
    private Field field;

    /**
     * 字段类型
     */
    private Class<?> fieldType;

    /**
     * 类型处理器
     */
    private Class<? extends TypeHandler<?>> typeHandler;

    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }

    public boolean isInsert() {
        return insert;
    }

    public void setInsert(boolean insert) {
        this.insert = insert;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isIfNullInsert() {
        return ifNullInsert;
    }

    public void setIfNullInsert(boolean ifNullInsert) {
        this.ifNullInsert = ifNullInsert;
    }

    public boolean isIfNullUpdate() {
        return ifNullUpdate;
    }

    public void setIfNullUpdate(boolean ifNullUpdate) {
        this.ifNullUpdate = ifNullUpdate;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public boolean isPK() {
        return isPK;
    }

    public void setPK(boolean PK) {
        isPK = PK;
    }

    public IdType getIdType() {
        return idType;
    }

    public void setIdType(IdType idType) {
        this.idType = idType;
    }

    public KeyCreator<?> getKeyGenerator() {
        return keyCreator;
    }

    public void setKeyGenerator(KeyCreator<?> keyCreator) {
        this.keyCreator = keyCreator;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public void setFieldType(Class<?> fieldType) {
        this.fieldType = fieldType;
    }

    public Class<? extends TypeHandler<?>> getTypeHandler() {
        return typeHandler;
    }

    public void setTypeHandler(Class<? extends TypeHandler<?>> typeHandler) {
        this.typeHandler = typeHandler;
    }
}
