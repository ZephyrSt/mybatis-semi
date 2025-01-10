package top.zephyrs.mybatis.semi;


import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import top.zephyrs.mybatis.semi.config.GlobalConfig;
import top.zephyrs.mybatis.semi.config.KeyGenerateConfig;
import top.zephyrs.mybatis.semi.exceptions.KeyGenerateException;
import top.zephyrs.mybatis.semi.executor.ParameterHandlerWrapper;
import top.zephyrs.mybatis.semi.executor.ResultSetHandlerWrapper;
import top.zephyrs.mybatis.semi.injects.DefaultInjectProcessor;
import top.zephyrs.mybatis.semi.injects.InjectMethod;
import top.zephyrs.mybatis.semi.injects.InjectProcessor;
import top.zephyrs.mybatis.semi.plugins.keygenerate.IdType;
import top.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;
import top.zephyrs.mybatis.semi.plugins.keygenerate.generators.AutoKeyCreator;
import top.zephyrs.mybatis.semi.plugins.keygenerate.generators.NoneKeyCreator;
import top.zephyrs.mybatis.semi.plugins.keygenerate.generators.SnowflakeKeyCreator;
import top.zephyrs.mybatis.semi.plugins.keygenerate.generators.UUIDKeyCreator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 扩展的 org.apache.ibatis.session.Configuration
 * 增加通用功能的配置信息。
 */
public class SemiMybatisConfiguration extends Configuration {

    protected final Log log = LogFactory.getLog(this.getClass());

    /**
     * 代理生成mapper方法 sql 的处理器
     */
    private InjectProcessor injectProcessor = new DefaultInjectProcessor();
    /**
     * 增强配置
     */
    private GlobalConfig globalConfig;

    /**
     * 主键生成方式
     */
    private final Map<IdType, KeyCreator<?>> keyCreatorMap = new HashMap<>();


    /**
     * 敏感数据加解密工具
     */
    private final Set<String> sensitiveMappedStatementIds = new HashSet<>();


    public SemiMybatisConfiguration(Environment environment) {
        super(environment);
    }

    public SemiMybatisConfiguration() {
        super();
    }

    @Override
    public MappedStatement getMappedStatement(String id) {
        return super.getMappedStatement(id);
    }


    @Override
    public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
        return super.getMappedStatement(id, validateIncompleteStatements);
    }

    /* 对ParameterHandler进行增强，增强参数拦截器的可用性*/
    @Override
    public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject,
                                                BoundSql boundSql) {
        ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement,
                parameterObject, boundSql);
        return (ParameterHandler) interceptorChain.pluginAll(new ParameterHandlerWrapper(parameterHandler, mappedStatement));
    }

    /* 对ResultSetHandler进行增强，增强参数拦截器的可用性*/
    @Override
    public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {
        ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler,
                resultHandler, boundSql, rowBounds);

        return (ResultSetHandler) interceptorChain.pluginAll(new ResultSetHandlerWrapper(resultSetHandler, mappedStatement));
    }

    @Override
    protected void buildAllStatements() {
        super.buildAllStatements();
        // 对需要代理的Mapper方法进行解析
        this.buildAllProcessorStatements();
    }

    /**
     * 同步处理解析
     */
    private synchronized void buildAllProcessorStatements() {
        // 加载需要代理的公用方法
        if (!injectProcessor.isLoaded()) {
            injectProcessor.loadMethods();
        }

        for (Class<?> type : mapperRegistry.getMappers()) {
            boolean loadCompleted = false;
            try {
                SemiMapperBuilder parse = new SemiMapperBuilder(this, type);
                parse.parse();
                loadCompleted = true;
            } finally {
                if (!loadCompleted) {
                    log.error("parse mapper failed: " + type.getName());
                }
            }
        }
    }

    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public void setGlobalConfig(GlobalConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

    public InjectMethod getInjectMethod(String id) {
        return this.injectProcessor.getMethod(id);
    }

    public InjectProcessor getInjectProcessor() {
        return injectProcessor;
    }

    public void setInjectProcessor(InjectProcessor injectProcessor) {
        this.injectProcessor = injectProcessor;
    }

    public void setKeyCreators() {
        KeyGenerateConfig cfg = this.globalConfig.getKeyGenerate();
        if (cfg == null) {
            cfg = new KeyGenerateConfig();
        }
        this.setKeyCreator(IdType.UUID, new UUIDKeyCreator());
        this.setKeyCreator(IdType.SNOWFLAKE, new SnowflakeKeyCreator(cfg.getWorkId()));
        this.setKeyCreator(IdType.NONE, new NoneKeyCreator());
        this.setKeyCreator(IdType.AUTO, new AutoKeyCreator());
        try {
            if (cfg.getCustomKeyCreator() != null) {
                this.setKeyCreator(IdType.CUSTOM, cfg.getCustomKeyCreator().getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            throw new KeyGenerateException("Failed to create custom keyCreator. Please check the constructor function. class=" + cfg.getCustomKeyCreator().getName(), e);
        }
        this.setKeyCreator(IdType.DEFAULT, this.getKeyCreator(cfg.getDefaultIdType()));
    }

    public void setKeyCreator(IdType idType, KeyCreator<?> keyCreator) {
        this.keyCreatorMap.put(idType, keyCreator);
    }

    public KeyCreator<?> getKeyCreator(IdType idType) {
        return this.keyCreatorMap.get(idType);
    }

    /**
     * 添加需要解密的MapperStatement，只有在默认不解密的情况下需要设置
     *
     * @param id mappedStatement Id
     */
    public void addSensitiveMappedStatementIds(String id) {
        sensitiveMappedStatementIds.add(id);
    }

    /**
     * 判断是否需要解密
     *
     * @param id mappedStatement Id
     * @return true: 需要解密 false: 不需要解密
     */
    public boolean isSensitiveDecrypt(String id) {
        if (globalConfig.getSensitive() == null || !globalConfig.getSensitive().isOpen()) {
            return false;
        }
        if (globalConfig.getSensitive().isDefaultDecrypt()) {
            return true;
        }
        return this.sensitiveMappedStatementIds.contains(id);
    }

}
