package top.zephyrs.mybatis.semi.plugins.keygenerate;

/**
 * 主键生成器
 */
public interface KeyCreator<T> {

    /**
     * 获取ID
     *
     * @return 主键值
     */
    T nextId();
}
