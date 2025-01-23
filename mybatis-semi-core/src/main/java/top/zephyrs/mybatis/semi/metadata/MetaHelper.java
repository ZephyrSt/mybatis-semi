package top.zephyrs.mybatis.semi.metadata;

import org.apache.ibatis.type.UnknownTypeHandler;
import top.zephyrs.mybatis.semi.annotations.*;
import top.zephyrs.mybatis.semi.config.EnableConfig;
import top.zephyrs.mybatis.semi.config.GlobalConfig;
import top.zephyrs.mybatis.semi.config.LogicDeleteConfig;
import top.zephyrs.mybatis.semi.exceptions.MetadataException;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaHelper {

    /**
     * 储存反射类表信息
     */
    private static final Map<Class<?>, MetaInfo> TABLE_INFO_CACHE = new ConcurrentHashMap<>();


    public static MetaInfo getMetaInfo(Class<?> type) {
        return getMetaInfo(null, type, false);
    }

    public static MetaInfo getMetaInfo(GlobalConfig config, Class<?> type, boolean loadIfNotExists) {
        MetaInfo metaInfo = TABLE_INFO_CACHE.get(type);
        if(metaInfo == null && loadIfNotExists) {
            synchronized (MetaHelper.class) {
                metaInfo = TABLE_INFO_CACHE.get(type);
                if(metaInfo == null) {
                    metaInfo = parseMetaInfo(config, type);
                    TABLE_INFO_CACHE.put(type, metaInfo);
                }
            }
        }
        return metaInfo;
    }


    private static MetaInfo parseMetaInfo(GlobalConfig config, Class<?> type) {
        if (type == null || type.isPrimitive() || type.isInterface()) {
            return null;
        }
        Table table = type.getAnnotation(Table.class);
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setType(type);
        if(table != null) {
            metaInfo.setTableName(table.value());
        }else {
            metaInfo.setTableName(humpToLine(type.getSimpleName()));
        }
        List<ColumnInfo> columns = parseField(type);
        metaInfo.setColumns(columns);

        for(ColumnInfo column: columns) {
            if(column.isPK()) {
                metaInfo.setPkColumn(column);
                break;
            }
            if(column.getTypeHandler() != null && column.getTypeHandler() != UnknownTypeHandler.class) {
                metaInfo.setUseResultMap(true);
            }
        }
        LogicDeleteConfig logicCfg = config.getLogic();
        ColumnInfo logicDeleteColumn = columns.stream().filter(columnInfo -> columnInfo.getField().getAnnotation(LogicDelete.class)!= null).findFirst().orElse(null);
        if(logicDeleteColumn != null) {
            LogicDelete logicDelete = logicDeleteColumn.getField().getAnnotation(LogicDelete.class);
            metaInfo.setLogical(true);
            metaInfo.setLogicalColumn(logicDeleteColumn);
            metaInfo.setDeletedValue(logicDelete.deletedValue());
            metaInfo.setNoDeletedValue(logicDelete.existsValue());
        } else if(logicCfg != null && logicCfg.getColumn() != null && !logicCfg.getColumn().isEmpty()) {
            logicDeleteColumn = columns.stream().filter(columnInfo -> columnInfo.getColumnName().equals(logicCfg.getColumn())).findFirst().orElse(null);
            if(logicDeleteColumn != null) {
                metaInfo.setLogical(true);
                metaInfo.setLogicalColumn(logicDeleteColumn);
                metaInfo.setDeletedValue(logicCfg.getDeletedValue());
                metaInfo.setNoDeletedValue(logicCfg.getExistsValue());
            }
        }

        EnableConfig enableCfg = config.getEnable();
        ColumnInfo enableColumn = columns.stream().filter(columnInfo -> columnInfo.getField().getAnnotation(Enable.class)!= null).findFirst().orElse(null);
        if(enableColumn != null) {
            Enable enable = enableColumn.getField().getAnnotation(Enable.class);
            metaInfo.setEnable(true);
            metaInfo.setEnableColumn(enableColumn);
            metaInfo.setEnabledValue(enable.enabledValue());
            metaInfo.setDisabledValue(enable.disabledValue());
        }
        else if(enableCfg != null
                && enableCfg.getColumn()!=null && !enableCfg.getColumn().isEmpty()) {
            enableColumn = columns.stream().filter(columnInfo -> columnInfo.getColumnName().equals(enableCfg.getColumn())).findFirst().orElse(null);
            if(enableColumn != null) {
                metaInfo.setEnable(true);
                metaInfo.setEnableColumn(enableColumn);
                metaInfo.setEnabledValue(enableCfg.getEnabledValue());
                metaInfo.setDisabledValue(enableCfg.getDisabledValue());
            }
        }
        return metaInfo;
    }

    private static List<ColumnInfo> parseField(Class<?> type) {
        List<ColumnInfo> columns = new ArrayList<>();
        try {
            for (Field field : ReflectionUtils.getAllFields(type)) {
                ColumnInfo columnInfo = parseField(field);
                columns.add(columnInfo);
            }
        } catch (Exception e) {
            throw new MetadataException("parse Bean property failed:"+ type.getName(), e);
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
        columnInfo.setFieldType(field.getType());
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
            //默认为null则不插入
            columnInfo.setIfNullInsert(false);
            //默认为null则不修改
            columnInfo.setIfNullUpdate(false);
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




    public static ColumnInfo getColumnByFieldName(MetaInfo metaInfo, String fieldName) {
        for(ColumnInfo column: metaInfo.getColumns()) {
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
        MetaInfo metaInfo = getMetaInfo(bean.getClass());
        if(metaInfo == null) {
            return null;
        }
        ColumnInfo column = metaInfo.getPkColumn();
        if(column == null) {
            return null;
        }
        try {
            Field field = column.getField();
            ReflectionUtils.makeAccessible(field);
            return (Serializable) field.get(bean);
        } catch (IllegalAccessException e) {
            throw new MetadataException("get bean id failed: "+ e.getMessage(), e);
        }

    }

}
