package top.zephyrs.mybatis.semi.plugins.sensitive;

import top.zephyrs.mybatis.semi.annotations.Sensitive;
import top.zephyrs.mybatis.semi.config.SensitiveConfig;
import top.zephyrs.mybatis.semi.metadata.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 敏感字段加解密
 */
public class SensitiveHelper {

    private static final Map<Class<? extends ISensitive>, ISensitive> sensitivesMap = new HashMap<>();

    private static final Map<Class<?>, SensitiveBean> sensitiveBeanMap = new HashMap<>();


    /**
     * 获取类的敏感字段信息
     * @param config 敏感字段加解密配置
     * @param obj 类信息
     * @return 类中的敏感字段配置信息
     * @throws NoSuchMethodException 反射异常
     * @throws InvocationTargetException 反射异常
     * @throws InstantiationException 反射异常
     * @throws IllegalAccessException 反射异常
     */
    public static SensitiveBean getSensitiveBean(SensitiveConfig config, Object obj) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        if(obj == null || obj instanceof String || obj instanceof Long || obj instanceof Integer
                || obj instanceof Short || obj instanceof Boolean || obj instanceof Character
                || obj instanceof Collection || obj instanceof Map) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        SensitiveBean sensitiveBean = sensitiveBeanMap.get(clazz);
        if (sensitiveBean == null) {
            synchronized (SensitiveBean.class) {
                sensitiveBean = sensitiveBeanMap.get(clazz);
                if (sensitiveBean == null) {
                    List<Field> needSensitiveFields = getNeedSensitiveFields(clazz);
                    Map<Field, ISensitive> sensitiveMap = new HashMap<>();
                    Set<Field> ignoreDecryptFieldSet = new HashSet<>();
                    for (Field field : needSensitiveFields) {
                        ReflectionUtils.makeAccessible(field);
                        Sensitive annotation = field.getAnnotation(Sensitive.class);
                        if(annotation == null) {
                            continue;
                        }
                        if(!annotation.needDecrypt()) {
                            ignoreDecryptFieldSet.add(field);
                        }
                        Class<? extends ISensitive> sensitiveClass = annotation.value();
                        if (sensitiveClass == DefaultSensitive.class) {
                            if (config.getDefaultImpl() != null) {
                                sensitiveClass = config.getDefaultImpl();
                            }
                        }
                        ISensitive sensitive = sensitivesMap.get(sensitiveClass);
                        if (sensitive == null) {
                            sensitive = annotation.value().getDeclaredConstructor().newInstance();
                            sensitivesMap.put(annotation.value(), sensitive);
                        }
                        sensitiveMap.put(field, sensitive);
                    }
                    sensitiveBean = new SensitiveBean(clazz, sensitiveMap, ignoreDecryptFieldSet);
                    sensitiveBeanMap.put(clazz, sensitiveBean);
                }
            }
        }
        return sensitiveBean;
    }

    private static List<Field> getNeedSensitiveFields(Class<?> type){
        return ReflectionUtils.getAllFields(type).stream().filter(field -> field.getAnnotation(Sensitive.class) != null).collect(Collectors.toList());
    }

    public static class SensitiveBean {
        private final Class<?> clazz;
        private final Map<Field, ISensitive> sensitiveMap;

        private final Set<Field> ignoreDecryptSet;

        SensitiveBean(Class<?> clazz, Map<Field, ISensitive> sensitiveMap, Set<Field> ignoreDecryptSet) {
            this.clazz = clazz;
            this.sensitiveMap = sensitiveMap;
            this.ignoreDecryptSet = ignoreDecryptSet;
        }

        public Class<?> getSensitiveClass() {
            return clazz;
        }

        Collection<Field> getFields() {
            return sensitiveMap.keySet();
        }

        ISensitive getSensitive(Field field) {
            return sensitiveMap.get(field);
        }

        public boolean isIgnore(Field field) {
            return ignoreDecryptSet.contains(field);
        }
    }


}
