package com.zephyrs.mybatis.semi.executor;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 *
 * 用于对 ResultSetHandler 进行包装
 */
public class ResultSetHandlerWrapper implements ResultSetHandler {
    private ResultSetHandler resultSetHandler;

    private MappedStatement mappedStatement;

    public ResultSetHandlerWrapper(ResultSetHandler resultSetHandler, MappedStatement mappedStatement) {
        this.resultSetHandler = resultSetHandler;
        this.mappedStatement = mappedStatement;
    }

    @Override
    public <E> List<E> handleResultSets(Statement stmt) throws SQLException {
        return resultSetHandler.handleResultSets(stmt);
    }

    @Override
    public <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException {
        return resultSetHandler.handleCursorResultSets(stmt);
    }

    @Override
    public void handleOutputParameters(CallableStatement cs) throws SQLException {
        resultSetHandler.handleOutputParameters(cs);
    }

    public MappedStatement getMappedStatement() {
        return mappedStatement;
    }
}
