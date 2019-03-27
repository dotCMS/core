package com.dotmarketing.db;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class DotDbConnection implements Connection {

    final Connection _connection;
    int dbCalls=0;
    public DotDbConnection(Connection conn) {
        _connection = conn;
    }
    public int dbCalls(){
        return dbCalls;
    }
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return _connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return _connection.isWrapperFor(iface);
    }

    @Override
    public Statement createStatement() throws SQLException {
        dbCalls++;
        return _connection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        dbCalls++;
        return _connection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        dbCalls++;
        return _connection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        dbCalls++;
        return _connection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        _connection.setAutoCommit(autoCommit);

    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return _connection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        _connection.commit();

    }

    @Override
    public void rollback() throws SQLException {
        _connection.rollback();

    }

    @Override
    public void close() throws SQLException {
        _connection.close();

    }

    @Override
    public boolean isClosed() throws SQLException {
        return _connection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return _connection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        _connection.setReadOnly(readOnly);

    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return _connection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        _connection.setCatalog(catalog);

    }

    @Override
    public String getCatalog() throws SQLException {
        return _connection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        _connection.setTransactionIsolation(level);

    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return _connection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return _connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        _connection.clearWarnings();

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        dbCalls++;
        return _connection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        dbCalls++;
        return _connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        dbCalls++;
        return _connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return _connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        _connection.setTypeMap(map);

    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        _connection.setHoldability(holdability);

    }

    @Override
    public int getHoldability() throws SQLException {
        return _connection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return _connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return _connection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        _connection.rollback(savepoint);

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        _connection.releaseSavepoint(savepoint);

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        dbCalls++;
        return _connection.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        dbCalls++;
        return _connection.prepareStatement(sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        dbCalls++;
        return _connection.prepareCall(sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        dbCalls++;
        return _connection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        dbCalls++;
        return _connection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        dbCalls++;
        return _connection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return _connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return _connection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return _connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return _connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return _connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        _connection.setClientInfo(name, value);

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        _connection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return _connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return _connection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return _connection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return _connection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        _connection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return _connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        _connection.abort(executor);

    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        _connection.setNetworkTimeout(executor, milliseconds);

    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return _connection.getNetworkTimeout();
    }


}
