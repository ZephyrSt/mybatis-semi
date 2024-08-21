package top.zephyrs.mybatis.semi.plugins.sensitive;

/**
 * 默认的敏感字段实现，存储时不加密，当查询方法  不需要解密敏感字段时，敏感字段返回null值
 */
public class DefaultSensitive implements ISensitive{
    @Override
    public String encrypt(Object bean, String original) {
        return original;
    }

    @Override
    public String decrypt(Object bean, String ciphertext) {
        return ciphertext;
    }

}
