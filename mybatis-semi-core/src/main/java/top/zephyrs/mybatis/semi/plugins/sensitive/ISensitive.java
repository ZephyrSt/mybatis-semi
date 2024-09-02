package top.zephyrs.mybatis.semi.plugins.sensitive;


/**
 * 敏感数据加解密接口
 */
public interface ISensitive {
    /**
     * 保存时的加密方法
     * @param bean bean
     * @param original 原文
     * @return 加密后的字符串
     */
    String encrypt(Object bean, String original);

    /**
     * 查询时的解密方法
     * @param bean bean
     * @param ciphertext 数据库存储的密文
     * @return 返回查询的方法
     */
    String decrypt(Object bean, String ciphertext);

    /**
     * 不解密时返回的方法, 默认返回null替换密文
     * @param bean bean
     * @param ciphertext 数据库存储的密文
     * @return 返回查询的方法
     */
    default String normal(Object bean, String ciphertext) {
        return null;
    }
}
