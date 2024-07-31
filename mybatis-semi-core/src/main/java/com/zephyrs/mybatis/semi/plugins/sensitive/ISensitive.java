package com.zephyrs.mybatis.semi.plugins.sensitive;


/**
 * 敏感数据加解密接口
 */
public interface ISensitive {
    String encrypt(Object bean, String original);

    String decrypt(Object bean, String ciphertext);
}
