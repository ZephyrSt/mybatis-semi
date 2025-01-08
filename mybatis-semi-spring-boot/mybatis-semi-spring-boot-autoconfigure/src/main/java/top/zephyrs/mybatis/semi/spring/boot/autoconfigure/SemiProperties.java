/*
 *    Copyright 2015-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package top.zephyrs.mybatis.semi.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import top.zephyrs.mybatis.semi.SemiMybatisConfiguration;
import top.zephyrs.mybatis.semi.config.EnableConfig;
import top.zephyrs.mybatis.semi.config.KeyGenerateConfig;
import top.zephyrs.mybatis.semi.config.LogicDeleteConfig;
import top.zephyrs.mybatis.semi.config.SensitiveConfig;
import top.zephyrs.mybatis.semi.exceptions.MappedProcessorException;
import top.zephyrs.mybatis.semi.injects.DefaultInjectProcessor;
import top.zephyrs.mybatis.semi.injects.InjectProcessor;
import top.zephyrs.mybatis.semi.plugins.keygenerate.KeyGenerateInterceptor;
import top.zephyrs.mybatis.semi.plugins.sensitive.SensitiveDecryptInterceptor;
import top.zephyrs.mybatis.semi.plugins.sensitive.SensitiveEncryptInterceptor;

/**
 * Configuration properties for MyBatis.
 *
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 */
@ConfigurationProperties(prefix = SemiProperties.MYBATIS_SEMI_PREFIX)
public class SemiProperties {

    public static final String MYBATIS_SEMI_PREFIX = "mybatis-semi";
    @NestedConfigurationProperty
    private GlobalConfig globalConfig;

    private Class<? extends InjectProcessor> injectProcessor;

    public void applyTo(SemiMybatisConfiguration target) {
        if (this.injectProcessor == null) {
            this.injectProcessor = DefaultInjectProcessor.class;
            target.setInjectProcessor(new DefaultInjectProcessor());
        } else {
            try {
                target.setInjectProcessor(injectProcessor.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new MappedProcessorException("create injectProcessor instance error:" + e.getMessage(), e);
            }
        }
        if (this.globalConfig == null) {
            this.globalConfig = new GlobalConfig();

        }
        this.globalConfig.applyTo(target);
        target.setKeyCreators();
        target.addInterceptor(new KeyGenerateInterceptor(target));
        SensitiveConfig sensitiveConfig = this.globalConfig.getSensitive();
        if (sensitiveConfig != null && sensitiveConfig.isOpen()) {
            target.addInterceptor(new SensitiveEncryptInterceptor(sensitiveConfig));
            target.addInterceptor(new SensitiveDecryptInterceptor(sensitiveConfig));
        }
    }

    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

    public Class<? extends InjectProcessor> getInjectProcessor() {
        return injectProcessor;
    }

    public void setInjectProcessor(Class<? extends InjectProcessor> injectProcessor) {
        this.injectProcessor = injectProcessor;
    }


    public static class GlobalConfig extends top.zephyrs.mybatis.semi.config.GlobalConfig {

        /**
         * 全局设置-逻辑删除
         */
        @NestedConfigurationProperty
        private LogicDeleteConfig logicDelete;

        /**
         * 全局设置-启用禁用
         */
        @NestedConfigurationProperty
        private EnableConfig enable;

        /**
         * selectAll 方法的最大查询行数
         */
        private Integer selectAllMaxRow = 3000;

        /**
         * 全局设置-主键生成配置
         */
        @NestedConfigurationProperty
        private KeyGenerateConfig keyGenerate;
        @NestedConfigurationProperty
        private SensitiveConfig sensitive;

        public void applyTo(SemiMybatisConfiguration target) {
            PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
            top.zephyrs.mybatis.semi.config.GlobalConfig config = new top.zephyrs.mybatis.semi.config.GlobalConfig();
            mapper.from(getSelectAllMaxRow()).to(config::setSelectAllMaxRow);
            mapper.from(getKeyGenerate()).to(config::setKeyGenerate);
            mapper.from(getLogicDelete()).to(config::setLogic);
            mapper.from(getEnable()).to(config::setEnable);
            mapper.from(getSensitive()).to(config::setSensitive);
            target.setGlobalConfig(config);
        }

        public LogicDeleteConfig getLogicDelete() {
            return logicDelete;
        }

        public void setLogicDelete(LogicDeleteConfig logicDelete) {
            this.logicDelete = logicDelete;
        }

        @Override
        public EnableConfig getEnable() {
            return enable;
        }

        @Override
        public void setEnable(EnableConfig enable) {
            this.enable = enable;
        }

        @Override
        public Integer getSelectAllMaxRow() {
            return selectAllMaxRow;
        }

        @Override
        public void setSelectAllMaxRow(Integer selectAllMaxRow) {
            this.selectAllMaxRow = selectAllMaxRow;
        }

        public KeyGenerateConfig getKeyGenerate() {
            return keyGenerate;
        }

        public void setKeyGenerate(KeyGenerateConfig keyGenerate) {
            this.keyGenerate = keyGenerate;
        }

        @Override
        public SensitiveConfig getSensitive() {
            return sensitive;
        }

        @Override
        public void setSensitive(SensitiveConfig sensitive) {
            this.sensitive = sensitive;
        }
    }

}
