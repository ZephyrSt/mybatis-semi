//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package top.zephyrs.mybatis.semi.spring.boot.autoconfigure;

import org.apache.ibatis.scripting.LanguageDriver;
import org.mybatis.scripting.freemarker.FreeMarkerLanguageDriver;
import org.mybatis.scripting.freemarker.FreeMarkerLanguageDriverConfig;
import org.mybatis.scripting.thymeleaf.SqlGeneratorConfig;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver;
import org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriverConfig;
import org.mybatis.scripting.velocity.Driver;
import org.mybatis.scripting.velocity.VelocityLanguageDriver;
import org.mybatis.scripting.velocity.VelocityLanguageDriverConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({LanguageDriver.class})
public class MybatisLanguageDriverAutoConfiguration {
    private static final String CONFIGURATION_PROPERTY_PREFIX = "mybatis.scripting-language-driver";

    public MybatisLanguageDriverAutoConfiguration() {
    }

    @Configuration
    @ConditionalOnClass({ThymeleafLanguageDriver.class})
    public static class ThymeleafConfiguration {
        public ThymeleafConfiguration() {
        }

        @Bean
        @ConditionalOnMissingBean
        ThymeleafLanguageDriver thymeleafLanguageDriver(ThymeleafLanguageDriverConfig config) {
            return new ThymeleafLanguageDriver(config);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("mybatis.scripting-language-driver.thymeleaf")
        public ThymeleafLanguageDriverConfig thymeleafLanguageDriverConfig() {
            return ThymeleafLanguageDriverConfig.newInstance();
        }

        private static class MetadataThymeleafLanguageDriverConfig extends ThymeleafLanguageDriverConfig {
            private MetadataThymeleafLanguageDriverConfig() {
            }

            @ConfigurationProperties("mybatis.scripting-language-driver.thymeleaf.dialect")
            public SqlGeneratorConfig.DialectConfig getDialect() {
                return super.getDialect();
            }

            @ConfigurationProperties("mybatis.scripting-language-driver.thymeleaf.template-file")
            public ThymeleafLanguageDriverConfig.TemplateFileConfig getTemplateFile() {
                return super.getTemplateFile();
            }
        }
    }

    @Configuration
    @ConditionalOnClass({VelocityLanguageDriver.class, VelocityLanguageDriverConfig.class})
    public static class VelocityConfiguration {
        public VelocityConfiguration() {
        }

        @Bean
        @ConditionalOnMissingBean
        VelocityLanguageDriver velocityLanguageDriver(VelocityLanguageDriverConfig config) {
            return new VelocityLanguageDriver(config);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("mybatis.scripting-language-driver.velocity")
        public VelocityLanguageDriverConfig velocityLanguageDriverConfig() {
            return VelocityLanguageDriverConfig.newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass({Driver.class})
    @ConditionalOnMissingClass({"org.mybatis.scripting.velocity.VelocityLanguageDriverConfig"})
    public static class LegacyVelocityConfiguration {
        public LegacyVelocityConfiguration() {
        }

        @Bean
        @ConditionalOnMissingBean
        Driver velocityLanguageDriver() {
            return new Driver();
        }
    }

    @Configuration
    @ConditionalOnClass({FreeMarkerLanguageDriver.class, FreeMarkerLanguageDriverConfig.class})
    public static class FreeMarkerConfiguration {
        public FreeMarkerConfiguration() {
        }

        @Bean
        @ConditionalOnMissingBean
        FreeMarkerLanguageDriver freeMarkerLanguageDriver(FreeMarkerLanguageDriverConfig config) {
            return new FreeMarkerLanguageDriver(config);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConfigurationProperties("mybatis.scripting-language-driver.freemarker")
        public FreeMarkerLanguageDriverConfig freeMarkerLanguageDriverConfig() {
            return FreeMarkerLanguageDriverConfig.newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass({FreeMarkerLanguageDriver.class})
    @ConditionalOnMissingClass({"org.mybatis.scripting.freemarker.FreeMarkerLanguageDriverConfig"})
    public static class LegacyFreeMarkerConfiguration {
        public LegacyFreeMarkerConfiguration() {
        }

        @Bean
        @ConditionalOnMissingBean
        FreeMarkerLanguageDriver freeMarkerLanguageDriver() {
            return new FreeMarkerLanguageDriver();
        }
    }
}
