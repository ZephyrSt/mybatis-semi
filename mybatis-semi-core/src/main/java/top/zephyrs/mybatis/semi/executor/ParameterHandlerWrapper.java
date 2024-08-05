package top.zephyrs.mybatis.semi.executor;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 用于对 ParameterHandler 进行包装
 */
public class ParameterHandlerWrapper implements ParameterHandler {

    private final ParameterHandler parameterHandler;

    private final MappedStatement mappedStatement;

    public ParameterHandlerWrapper(ParameterHandler parameterHandler, MappedStatement mappedStatement) {
        this.parameterHandler = parameterHandler;
        this.mappedStatement = mappedStatement;
    }

    @Override
    public Object getParameterObject() {
        return parameterHandler.getParameterObject();
    }

    @Override
    public void setParameters(PreparedStatement ps) throws SQLException {
        parameterHandler.setParameters(ps);
    }

    public MappedStatement getMappedStatement() {
        return mappedStatement;
    }
}
