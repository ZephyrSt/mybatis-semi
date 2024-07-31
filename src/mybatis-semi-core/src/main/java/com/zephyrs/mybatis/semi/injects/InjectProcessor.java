package com.zephyrs.mybatis.semi.injects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class InjectProcessor {

    protected Map<String, InjectMethod> injectMethodMap = new HashMap<>();

    public abstract void loadMethods();

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
