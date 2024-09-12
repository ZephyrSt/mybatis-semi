package top.zephyrs.mybatis.semi.metadata;

import top.zephyrs.mybatis.semi.base.IMapper;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtils {

    /**
     * 获取Bean的所有非静态字段（包含父类字段）
     * @param type 类型
     * @return Bean的字段集合
     */
    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())).collect(Collectors.toList());

        Class<?> superClass = type.getSuperclass();
        if(superClass!= null && !superClass.equals(Object.class)) {
            List<Field> superFields = getAllFields(superClass);
            fields.addAll(superFields);
        }
        return fields;
    }

    /**
     * 获取所有Bean的属性对应的字段（仅限字段与属性名称一致的情况）
     * @param type 类型
     * @return Bean的属性字段集合
     * @throws Exception 反射执行异常
     */
    public static List<Field> getProperty(Class<?> type) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
        if (beanInfo == null) {
            return Collections.emptyList();
        }
        Set<String> propertyNameSet = new HashSet<>();
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for(PropertyDescriptor descriptor: propertyDescriptors) {
            propertyNameSet.add(descriptor.getName());
        }
        List<Field> fields = new ArrayList<>();
        for(Field field: getAllFields(type)) {
            fields.add(field);
            if(propertyNameSet.contains(field.getName())) {
                fields.add(field);
            }
        }
        return fields;

    }
    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }

    }


}
