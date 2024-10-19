package top.zephyrs.mybatis.semi.injects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用方法处理器
 */
public abstract class InjectProcessor {

    protected Map<String, InjectMethod> injectMethodMap = new HashMap<>();

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

    public InjectMethod getMethod(String id) {
        return injectMethodMap.get(id);
    }

    protected void addInject(String id, InjectMethod injectMethod) {
        injectMethodMap.put(id, injectMethod);
    }
    protected void addInject(InjectMethod injectMethod) {
        injectMethodMap.put(injectMethod.getId(), injectMethod);
    }

}
