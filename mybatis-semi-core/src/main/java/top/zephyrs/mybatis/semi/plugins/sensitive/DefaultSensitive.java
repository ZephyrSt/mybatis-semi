package top.zephyrs.mybatis.semi.plugins.sensitive;

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
