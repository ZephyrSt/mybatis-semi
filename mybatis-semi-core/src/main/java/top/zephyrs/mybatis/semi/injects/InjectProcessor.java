package top.zephyrs.mybatis.semi.injects;

import top.zephyrs.mybatis.semi.annotations.Search;
import top.zephyrs.mybatis.semi.injects.methods.SelectByQueryAnnotation;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用方法处理器
 */
public abstract class InjectProcessor {

    protected Map<String, InjectMethod> injectMethodMap = new HashMap<>();

    /**
     * 根据Query注解定义SQL, 并根据查询参数拼接查询条件
     */
    protected SelectByQueryAnnotation selectByQueryAnnotation = new SelectByQueryAnnotation();

    /**
     * 加载通用方法
     */
    public abstract void loadMethods();

    public abstract boolean isLoaded();

    /**
     * 查询全部通用方法
     * @return InjectMethods
     */
    public Collection<InjectMethod> getMethods() {
        return injectMethodMap.values();
    }

    public InjectMethod getMethod(String id, Method method) {
        if(method.getAnnotation(Search.class) != null ) {
            return selectByQueryAnnotation;
        }
        return injectMethodMap.get(id);
    }

    protected void addInject(String id, InjectMethod injectMethod) {
        injectMethodMap.put(id, injectMethod);
    }
    protected void addInject(InjectMethod injectMethod) {
        injectMethodMap.put(injectMethod.getId(), injectMethod);
    }

}
