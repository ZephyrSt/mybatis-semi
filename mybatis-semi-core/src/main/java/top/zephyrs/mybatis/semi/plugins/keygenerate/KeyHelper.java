package top.zephyrs.mybatis.semi.plugins.keygenerate;

import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.exceptions.KeyGenerateException;
import top.zephyrs.mybatis.semi.metadata.ColumnInfo;
import top.zephyrs.mybatis.semi.metadata.ReflectionUtils;
import top.zephyrs.mybatis.semi.metadata.MetaInfo;

import java.lang.reflect.Field;

public class KeyHelper {

    public static void setKey(SemiMybatisConfiguration configuration, Object parameter, MetaInfo metaInfo) throws IllegalAccessException {
        ColumnInfo column = metaInfo.getPkColumn();
        if (!column.isPK() || column.getIdType() == null || column.getIdType() == IdType.NONE || column.getIdType() == IdType.AUTO) {
            return;
        }
        setKey(configuration, column.getField(), column.getIdType(), parameter);
    }

    private static void setKey(SemiMybatisConfiguration configuration, Field field, IdType idType, Object parameterObj) throws IllegalAccessException {

        ReflectionUtils.makeAccessible(field);
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
        }
    }

}
