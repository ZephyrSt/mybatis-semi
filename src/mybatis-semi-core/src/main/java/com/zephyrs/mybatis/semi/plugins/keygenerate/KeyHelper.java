package com.zephyrs.mybatis.semi.plugins.keygenerate;

import com.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import com.zephyrs.mybatis.semi.exceptions.KeyGenerateException;
import com.zephyrs.mybatis.semi.metadata.ColumnInfo;
import com.zephyrs.mybatis.semi.metadata.TableInfo;

import java.lang.reflect.Field;

public class KeyHelper {

    public static void setKey(SemiMybatisConfiguration configuration, Object parameter, TableInfo tableInfo) throws IllegalAccessException {
        ColumnInfo column = tableInfo.getPkColumn();
        if (!column.isPK() || column.getIdType() == null || column.getIdType() == IdType.NONE || column.getIdType() == IdType.AUTO) {
            return;
        }
        setKey(configuration, column.getField(), column.getIdType(), parameter);
    }

    private static void setKey(SemiMybatisConfiguration configuration, Field field, IdType idType, Object parameterObj) throws IllegalAccessException {

        field.setAccessible(true);
        Object existsValue = field.get(parameterObj);
        if (existsValue != null) {
            return;
        }
        KeyCreator<?> keyCreator = configuration.getKeyCreator(idType);
        if(keyCreator == null) {
            throw new KeyGenerateException("generate new key failed! keyCreator not exists! IdType:" + idType);
        }
        try {
            Object nextId = keyCreator.nextId();
            if(nextId == null) {
                return;
            }
            if(String.class.isAssignableFrom(field.getType())) {
                field.set(parameterObj, String.valueOf(nextId));
            }else {
                field.set(parameterObj, nextId);
            }
        } catch (Exception e) {
            throw new KeyGenerateException("generate new key failed! IdType:" + idType + ", field: " + field.getName(), e);
        } finally {
            field.setAccessible(false);
        }
    }

}
