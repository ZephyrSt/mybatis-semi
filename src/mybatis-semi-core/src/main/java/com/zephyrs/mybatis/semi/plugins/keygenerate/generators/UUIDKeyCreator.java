package com.zephyrs.mybatis.semi.plugins.keygenerate.generators;

import com.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;

public class UUIDKeyCreator implements KeyCreator<String> {


    @Override
    public String nextId() {
        return java.util.UUID.randomUUID().toString().replaceAll("-","");
    }
}
