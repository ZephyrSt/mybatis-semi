package top.zephyrs.mybatis.semi.config;

import top.zephyrs.mybatis.semi.plugins.sensitive.NoneSensitive;
import top.zephyrs.mybatis.semi.plugins.sensitive.ISensitive;

/**
 * 敏感字段加/解密全局配置
 */
public class SensitiveConfig {

    /**
     * 是否开启敏感字段加解密功能
     */
    private boolean open = true;

    /**
     * 默认是否解密
     */
    private boolean defaultDecrypt = true;

    /**
     * 当不解密时，是否用Null替换值
     */
    private boolean useNullOnNotDecrypt = false;

    /**
     * 默认的加解密实现
     */
    private Class<? extends ISensitive> defaultImpl = NoneSensitive.class;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Class<? extends ISensitive> getDefaultImpl() {
        return defaultImpl;
    }

    public void setDefaultImpl(Class<? extends ISensitive> defaultImpl) {
        this.defaultImpl = defaultImpl;
    }

    public boolean isDefaultDecrypt() {
        return defaultDecrypt;
    }

    public void setDefaultDecrypt(boolean defaultDecrypt) {
        this.defaultDecrypt = defaultDecrypt;
    }

    public boolean isUseNullOnNotDecrypt() {
        return useNullOnNotDecrypt;
    }

    public void setUseNullOnNotDecrypt(boolean useNullOnNotDecrypt) {
        this.useNullOnNotDecrypt = useNullOnNotDecrypt;
    }
}
