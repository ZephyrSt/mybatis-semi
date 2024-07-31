package com.zephyrs.mybatis.semi.plugins.sensitive;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SensitiveBean {
        private Class<?> clazz;
        private Map<Field, ISensitive> sensitiveMap = new HashMap<>();

        SensitiveBean(Class<?> clazz, Map<Field, ISensitive> sensitiveMap) {
            this.clazz = clazz;
            this.sensitiveMap = sensitiveMap;
        }

        Collection<Field> getFields() {
            return sensitiveMap.keySet();
        }

        ISensitive getSensitive(Field field) {
            return sensitiveMap.get(field);
        }
    }