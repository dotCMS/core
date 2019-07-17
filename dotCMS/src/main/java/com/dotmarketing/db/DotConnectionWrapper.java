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

public class DotConnectionWrapper implements Connection {

  final Connection internalConnection;

  public DotConnectionWrapper(final Connection conn) {
    
    this.internalConnection = conn;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {

    return internalConnection.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {

    return internalConnection.isWrapperFor(iface);
  }

  @Override
  public Statement createStatement() throws SQLException {

    return internalConnection.createStatement();
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {

    return internalConnection.prepareStatement(sql);
  }

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {

    return internalConnection.prepareCall(sql);
  }

  @Override
  public String nativeSQL(String sql) throws SQLException {

    return internalConnection.nativeSQL(sql);
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {

    internalConnection.setAutoCommit(autoCommit);

  }

  @Override
  public boolean getAutoCommit() throws SQLException {

    return internalConnection.getAutoCommit();
  }

  @Override
  public void commit() throws SQLException {

    internalConnection.commit();
  }

  @Override
  public void rollback() throws SQLException {
    internalConnection.rollback();

  }

  @Override
  public void close() throws SQLException {

    internalConnection.close();
  }

  @Override
  public boolean isClosed() throws SQLException {

    return internalConnection.isClosed();
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {

    return internalConnection.getMetaData();
  }

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    internalConnection.setReadOnly(readOnly);

  }

  @Override
  public boolean isReadOnly() throws SQLException {

    return internalConnection.isReadOnly();
  }

  @Override
  public void setCatalog(String catalog) throws SQLException {

    internalConnection.setCatalog(catalog);
  }

  @Override
  public String getCatalog() throws SQLException {

    return internalConnection.getCatalog();
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {

    internalConnection.setTransactionIsolation(level);
  }

  @Override
  public int getTransactionIsolation() throws SQLException {

    return internalConnection.getTransactionIsolation();
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {

    return internalConnection.getWarnings();
  }

  @Override
  public void clearWarnings() throws SQLException {

    internalConnection.clearWarnings();
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {

    return internalConnection.createStatement(resultSetType, resultSetConcurrency);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {

    return internalConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {

    return internalConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
  }

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {

    return internalConnection.getTypeMap();
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

    internalConnection.setTypeMap(map);
  }

  @Override
  public void setHoldability(int holdability) throws SQLException {

    internalConnection.setHoldability(holdability);
  }

  @Override
  public int getHoldability() throws SQLException {

    return internalConnection.getHoldability();
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {

    return internalConnection.setSavepoint();
  }

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {

    return internalConnection.setSavepoint(name);
  }

  @Override
  public void rollback(Savepoint savepoint) throws SQLException {

    internalConnection.rollback(savepoint);
  }

  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {

    internalConnection.releaseSavepoint(savepoint);
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {

    return internalConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {

    return internalConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {

    return internalConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {

    return internalConnection.prepareStatement(sql, autoGeneratedKeys);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {

    return internalConnection.prepareStatement(sql, columnIndexes);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {

    return internalConnection.prepareStatement(sql, columnNames);
  }

  @Override
  public Clob createClob() throws SQLException {

    return internalConnection.createClob();
  }

  @Override
  public Blob createBlob() throws SQLException {

    return internalConnection.createBlob();
  }

  @Override
  public NClob createNClob() throws SQLException {

    return internalConnection.createNClob();
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {

    return internalConnection.createSQLXML();
  }

  @Override
  public boolean isValid(int timeout) throws SQLException {

    return internalConnection.isValid(timeout);
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {

    internalConnection.setClientInfo(name, value);
  }

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {

    internalConnection.setClientInfo(properties);
  }

  @Override
  public String getClientInfo(String name) throws SQLException {

    return internalConnection.getClientInfo(name);
  }

  @Override
  public Properties getClientInfo() throws SQLException {

    return internalConnection.getClientInfo();
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {

    return internalConnection.createArrayOf(typeName, elements);
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {

    return internalConnection.createStruct(typeName, attributes);
  }

  @Override
  public void setSchema(String schema) throws SQLException {
    internalConnection.setSchema(schema);

  }

  @Override
  public String getSchema() throws SQLException {

    return internalConnection.getSchema();
  }

  @Override
  public void abort(Executor executor) throws SQLException {
    internalConnection.abort(executor);

  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

    internalConnection.setNetworkTimeout(executor, milliseconds);
  }

  @Override
  public int getNetworkTimeout() throws SQLException {

    return internalConnection.getNetworkTimeout();
  }

  @Override
  public boolean equals(Object obj) {
    Object conn = obj;
    if (obj instanceof DotConnectionWrapper) {
      return internalConnection.equals(((DotConnectionWrapper) conn).internalConnection);
    }
    return super.equals(obj);
  }

  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return internalConnection.toString();
  }

  @Override
  public int hashCode() {
    return internalConnection.hashCode();
  }

}
