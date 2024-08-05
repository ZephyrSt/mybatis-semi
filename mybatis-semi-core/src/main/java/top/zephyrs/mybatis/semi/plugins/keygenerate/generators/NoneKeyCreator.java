package top.zephyrs.mybatis.semi.plugins.keygenerate.generators;

import top.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;

public class NoneKeyCreator implements KeyCreator<String> {


    @Override
    public String nextId() {
        return null;
    }
}
