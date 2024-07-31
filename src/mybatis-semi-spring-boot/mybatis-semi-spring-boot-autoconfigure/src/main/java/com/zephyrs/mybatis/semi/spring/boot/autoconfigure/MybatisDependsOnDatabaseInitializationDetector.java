//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zephyrs.mybatis.semi.spring.boot.autoconfigure;

import java.util.Collections;
import java.util.Set;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;

class MybatisDependsOnDatabaseInitializationDetector extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {
    MybatisDependsOnDatabaseInitializationDetector() {
    }

    protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
        return Collections.singleton(SqlSessionTemplate.class);
    }
}
