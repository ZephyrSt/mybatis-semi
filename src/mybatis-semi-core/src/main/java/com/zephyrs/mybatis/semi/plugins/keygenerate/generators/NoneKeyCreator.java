package com.zephyrs.mybatis.semi.plugins.keygenerate.generators;

import com.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;

public class NoneKeyCreator implements KeyCreator<String> {


    @Override
    public String nextId() {
        return null;
    }
}
