package com.zephyrs.mybatis.semi.metadata;

import com.zephyrs.mybatis.semi.annotations.*;
import com.zephyrs.mybatis.semi.config.EnableConfig;
import com.zephyrs.mybatis.semi.config.GlobalConfig;
import com.zephyrs.mybatis.semi.config.LogicDeleteConfig;
import com.zephyrs.mybatis.semi.exceptions.MetadataException;
import org.apache.ibatis.type.UnknownTypeHandler;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataHelper {

    /**
     * 储存反射类表信息
     */
    private static final Map<Class<?>, TableInfo> TABLE_INFO_CACHE = new ConcurrentHashMap<>();

    /**
     * 储存表名对应的反射类表信息
     */
    private static final Map<String, TableInfo> TABLE_NAME_INFO_CACHE = new ConcurrentHashMap<>();

    public static TableInfo getTableInfo(Class<?> type) {
        return getTableInfo(null, type, false);
    }

    public static TableInfo getTableInfo(GlobalConfig config, Class<?> type) {
        return getTableInfo(config, type, true);
    }

    public static TableInfo getTableInfo(GlobalConfig config, Class<?> type, boolean loadIfNotExists) {
        TableInfo tableInfo = TABLE_INFO_CACHE.get(type);
        if(tableInfo == null && loadIfNotExists) {
            synchronized (MetadataHelper.class) {
                tableInfo = TABLE_INFO_CACHE.get(type);
                if(tableInfo == null) {
                    tableInfo = parseTableInfo(config, type);
                    TABLE_INFO_CACHE.put(type, tableInfo);
                    TABLE_NAME_INFO_CACHE.put(tableInfo.getTableName(), tableInfo);
                }
            }
        }
        return tableInfo;
    }


    private static TableInfo parseTableInfo(GlobalConfig config, Class<?> type) {
        if (type == null || type.isPrimitive() || type.isInterface()) {
            return null;
        }
        Table table = type.getAnnotation(Table.class);
        TableInfo tableInfo = new TableInfo();
        tableInfo.setType(type);
        if(table != null) {
            tableInfo.setTableName(table.value());
        }else {
            tableInfo.setTableName(humpToLine(type.getSimpleName()));
        }
        List<ColumnInfo> columns = parseField(type);
        tableInfo.setColumns(columns);

        for(ColumnInfo column: columns) {
            if(column.isPK()) {
                tableInfo.setPkColumn(column);
                break;
            }
        }
        LogicDeleteConfig logicCfg = config.getLogic();
        ColumnInfo logicDeleteColumn = columns.stream().filter(columnInfo -> columnInfo.getField().getAnnotation(LogicDelete.class)!= null).findFirst().orElse(null);
        if(logicDeleteColumn != null) {
            LogicDelete logicDelete = logicDeleteColumn.getField().getAnnotation(LogicDelete.class);
            tableInfo.setLogical(true);
            tableInfo.setLogicalColumn(logicDeleteColumn);
            tableInfo.setDeletedValue(logicDelete.deletedValue());
            tableInfo.setNoDeletedValue(logicDelete.existsValue());
        } else if(logicCfg != null && logicCfg.getColumn() != null && !logicCfg.getColumn().isEmpty()) {
            logicDeleteColumn = columns.stream().filter(columnInfo -> columnInfo.getColumnName().equals(logicCfg.getColumn())).findFirst().orElse(null);
            if(logicDeleteColumn != null) {
                tableInfo.setLogical(true);
                tableInfo.setLogicalColumn(logicDeleteColumn);
                tableInfo.setDeletedValue(logicCfg.getDeletedValue());
                tableInfo.setNoDeletedValue(logicCfg.getExistsValue());
            }
        }

        EnableConfig enableCfg = config.getEnable();
        ColumnInfo enableColumn = columns.stream().filter(columnInfo -> columnInfo.getField().getAnnotation(Enable.class)!= null).findFirst().orElse(null);
        if(enableColumn != null) {
            Enable enable = enableColumn.getField().getAnnotation(Enable.class);
            tableInfo.setEnable(true);
            tableInfo.setEnableColumn(enableColumn);
            tableInfo.setEnabledValue(enable.enabledValue());
            tableInfo.setDisabledValue(enable.disabledValue());
        }
        else if(enableCfg != null
                && enableCfg.getColumn()!=null && !enableCfg.getColumn().isEmpty()) {
            enableColumn = columns.stream().filter(columnInfo -> columnInfo.getColumnName().equals(enableCfg.getColumn())).findFirst().orElse(null);
            if(enableColumn != null) {
                tableInfo.setEnable(true);
                tableInfo.setEnableColumn(enableColumn);
                tableInfo.setEnabledValue(enableCfg.getEnabledValue());
                tableInfo.setDisabledValue(enableCfg.getDisabledValue());
            }
        }
        return tableInfo;
    }

    private static List<ColumnInfo> parseField(Class<?> type) {
        List<ColumnInfo> columns = new ArrayList<>();
        for (Field field : getAllFields(type)) {
            ColumnInfo columnInfo = parseField(field);
            columns.add(columnInfo);
        }
        return columns;
    }

    private static ColumnInfo parseField(Field field) {
        Column column = field.getAnnotation(Column.class);
        Primary primary = field.getAnnotation(Primary.class);
        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setField(field);
        columnInfo.setPK(false);
        columnInfo.setFieldName(field.getName());
        if(primary != null) {
            columnInfo.setPK(true);
            columnInfo.setIdType(primary.idType());
        }
        if(column != null) {
            if(column.value() != null && !column.value().isEmpty()) {
                columnInfo.setColumnName(column.value());
            }else {
                columnInfo.setColumnName(humpToLine(field.getName()));
            }
            columnInfo.setExists(column.exists());
            columnInfo.setSelect(column.select());
            columnInfo.setInsert(column.insert());
            columnInfo.setUpdate(column.update());
            columnInfo.setIfNullInsert(column.ifNullInsert());
            columnInfo.setIfNullUpdate(column.ifNullUpdate());
            columnInfo.setTypeHandler(column.typeHandler());
            return columnInfo;
        } else {
            columnInfo.setColumnName(humpToLine(field.getName()));
            columnInfo.setExists(true);
            columnInfo.setSelect(true);
            columnInfo.setInsert(true);
            columnInfo.setUpdate(true);
            columnInfo.setIfNullInsert(false);
            columnInfo.setIfNullUpdate(true);
            columnInfo.setTypeHandler(UnknownTypeHandler.class);
            return columnInfo;
        }
    }


    private static final Pattern humpPattern = Pattern.compile("[A-Z]");
    private static String humpToLine(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = Arrays.asList(type.getDeclaredFields());
        Class<?> superClass = type.getSuperclass();
        if(superClass!= null && !superClass.equals(Object.class)) {
            List<Field> superFields = getAllFields(superClass);
            fields.addAll(superFields);
        }
        return fields;
    }

    public static ColumnInfo getColumnByFieldName(TableInfo tableInfo,String fieldName) {
        for(ColumnInfo column: tableInfo.getColumns()) {
            if(column.getFieldName().equals(fieldName)) {
                return column;
            }
        }
        return null;
    }

    public static Serializable getBeanId(Object bean) {
        if(bean == null) {
            return null;
        }
        TableInfo tableInfo = getTableInfo(bean.getClass());
        if(tableInfo == null) {
            return null;
        }
        ColumnInfo column = tableInfo.getPkColumn();
        if(column == null) {
            return null;
        }
        try {
            Field field = column.getField();
            return (Serializable) field.get(bean);
        } catch (IllegalAccessException e) {
            throw new MetadataException("get bean id failed: "+ e.getMessage(), e);
        }

    }
}
