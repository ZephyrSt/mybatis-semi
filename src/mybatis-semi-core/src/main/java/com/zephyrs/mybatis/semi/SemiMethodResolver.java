package com.zephyrs.mybatis.semi;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.builder.annotation.MethodResolver;

import java.lang.reflect.Method;

public class SemiMethodResolver extends MethodResolver {
    public SemiMethodResolver(MapperAnnotationBuilder annotationBuilder, Method method) {
        super(annotationBuilder, method);
    }
}
