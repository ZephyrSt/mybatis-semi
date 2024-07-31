package com.zephyrs.mybatis.semi;


import com.zephyrs.mybatis.semi.config.GlobalConfig;
import com.zephyrs.mybatis.semi.config.KeyGenerateConfig;
import com.zephyrs.mybatis.semi.exceptions.KeyGenerateException;
import com.zephyrs.mybatis.semi.executor.ParameterHandlerWrapper;
import com.zephyrs.mybatis.semi.executor.ResultSetHandlerWrapper;
import com.zephyrs.mybatis.semi.injects.DefaultInjectProcessor;
import com.zephyrs.mybatis.semi.injects.InjectMethod;
import com.zephyrs.mybatis.semi.injects.InjectProcessor;
import com.zephyrs.mybatis.semi.plugins.keygenerate.IdType;
import com.zephyrs.mybatis.semi.plugins.keygenerate.KeyCreator;
import com.zephyrs.mybatis.semi.plugins.keygenerate.generators.AutoKeyCreator;
import com.zephyrs.mybatis.semi.plugins.keygenerate.generators.NoneKeyCreator;
import com.zephyrs.mybatis.semi.plugins.keygenerate.generators.SnowflakeKeyCreator;
import com.zephyrs.mybatis.semi.plugins.keygenerate.generators.UUIDKeyCreator;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemiMybatisConfiguration extends Configuration {

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
    private final List<String> sensitiveMappedStatementIds = new ArrayList<>();

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

    private void buildAllProcessorStatements() {
        // 加载需要代理的公用方法
        injectProcessor.loadMethods();

        for(Class<?> type: mapperRegistry.getMappers()) {

            boolean loadCompleted = false;
            try {
                SemiMapperBuilder parse = new SemiMapperBuilder(this, type);
                parse.parse();
                loadCompleted = true;
            } finally {
                if (!loadCompleted) {
                    // todo 这里直接跳过
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
        if(cfg == null) {
            cfg = new KeyGenerateConfig();
        }
        this.setKeyCreator(IdType.UUID, new UUIDKeyCreator());
        this.setKeyCreator(IdType.SNOWFLAKE, new SnowflakeKeyCreator(cfg.getWorkId()));
        this.setKeyCreator(IdType.NONE, new NoneKeyCreator());
        this.setKeyCreator(IdType.AUTO, new AutoKeyCreator());
        try{
            if(cfg.getCustomKeyCreator() != null) {
                this.setKeyCreator(IdType.CUSTOM, cfg.getCustomKeyCreator().getDeclaredConstructor().newInstance());
            }
        }catch (Exception e) {
            throw new KeyGenerateException("Failed to create custom keyCreator. Please check the constructor function. class="+cfg.getCustomKeyCreator().getName(), e);
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
     * @param id
     */
    public void addSensitiveMappedStatementIds(String id) {
        sensitiveMappedStatementIds.add(id);
    }

    /**
     *
     * 判断是否需要解密
     * @param id
     * @return
     */
    public boolean isSensitiveDecrypt(String id) {
        if(globalConfig.getSensitive() == null || !globalConfig.getSensitive().isOpen()) {
            return false;
        }
        if(globalConfig.getSensitive().isDefaultDecrypt()) {
            return true;
        }
        return this.sensitiveMappedStatementIds.contains(id);
    }
}
