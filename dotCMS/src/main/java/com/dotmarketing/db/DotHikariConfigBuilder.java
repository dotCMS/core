package com.dotmarketing.db;

import java.util.Properties;

import com.dotcms.repackage.com.zaxxer.hikari.HikariConfig;

public class DotHikariConfigBuilder {
  private final HikariConfig config;


  public DotHikariConfigBuilder setDataSourceClassName(final String dataSourceClassName) {
    this.config.setDataSourceClassName(dataSourceClassName);
    return this;
  }


  public DotHikariConfigBuilder setJdbcUrl(final String jdbcUrl) {
    this.config.setJdbcUrl(jdbcUrl);
    return this;
  }


  public DotHikariConfigBuilder setUsername(final String username) {
    this.config.setUsername(username);
    return this;
  }


  public DotHikariConfigBuilder setPassword(final String password) {
    this.config.setPassword(password);
    return this;
  }


  public DotHikariConfigBuilder setConnectionTestQuery(final String connectionTestQuery) {
    this.config.setConnectionTestQuery(connectionTestQuery);
    return this;
  }


  public DotHikariConfigBuilder setMetricRegistry(final Object metricRegistry) {
    this.config.setMetricRegistry(metricRegistry);
    return this;
  }


  public DotHikariConfigBuilder setHealthCheckRegistry(final Object healthCheckRegistry) {
    this.config.setHealthCheckRegistry(healthCheckRegistry);
    return this;
  }


  public DotHikariConfigBuilder setPoolName(final String poolName) {
    this.config.setPoolName(poolName);
    return this;
  }


  public DotHikariConfigBuilder setCatalog(final String catalog) {
    this.config.setCatalog(catalog);
    return this;
  }


  public DotHikariConfigBuilder setConnectionInitSql(final String connectionInitSql) {
    this.config.setConnectionInitSql(connectionInitSql);
    return this;
  }


  public DotHikariConfigBuilder setDriverClassName(final String driverClassName) {
    this.config.setDriverClassName(driverClassName);
    return this;
  }



  public DotHikariConfigBuilder setTransactionIsolation(final String transactionIsolation) {
    this.config.setTransactionIsolation(transactionIsolation);
    return this;
  }


  public DotHikariConfigBuilder setAutoCommit(boolean autoCommit) {
    this.config.setAutoCommit(autoCommit);
    return this;
  }


  public DotHikariConfigBuilder setIsolateInternalQueries(boolean isolateInternalQueries) {
    this.config.setIsolateInternalQueries(isolateInternalQueries);
    return this;
  }


  public DotHikariConfigBuilder setAllowPoolSuspension(boolean allowPoolSuspension) {
    this.config.setAllowPoolSuspension(allowPoolSuspension);
    return this;
  }


  public DotHikariConfigBuilder setReadOnly(boolean readOnly) {
    this.config.setReadOnly(readOnly);
    return this;
  }


  public DotHikariConfigBuilder setRegisterMbeans(boolean registerMbeans) {
    this.config.setRegisterMbeans(registerMbeans);
    return this;
  }


  public DotHikariConfigBuilder setMaximumPoolSize(int maxPoolSize) {
    this.config.setMaximumPoolSize(maxPoolSize);
    return this;
  }


  public DotHikariConfigBuilder setConnectionTimeout(int connectionTimeout) {
    this.config.setConnectionTimeout(connectionTimeout);
    return this;
  }



  public DotHikariConfigBuilder setIdleTimeout(int idleTimeout) {
    this.config.setIdleTimeout(idleTimeout);
    return this;
  }


  public DotHikariConfigBuilder setMaxLifetime(int maxLifetime) {
    this.config.setMaxLifetime(maxLifetime);
    return this;
  }


  public DotHikariConfigBuilder setMinimumIdle(int minimumIdle) {
    this.config.setMinimumIdle(minimumIdle);
    return this;
  }



  public DotHikariConfigBuilder setInitializationFailFast(boolean failFast) {
    this.config.setInitializationFailFast(failFast);
    return this;
  }


  public DotHikariConfigBuilder setValidationTimeout(int validationTimeout) {
    this.config.setValidationTimeout(validationTimeout);
    return this;
  }


  public DotHikariConfigBuilder setLeakDetectionThreshold(int leakDetectionThreshold) {
    this.config.setLeakDetectionThreshold(leakDetectionThreshold);
    return this;
  }



  public DotHikariConfigBuilder() {
    this.config = new HikariConfig();

  }

  public DotHikariConfigBuilder(final Properties props) {
    this.config = new HikariConfig(props);
  }


  public HikariConfig build() {
    
    return this.config;

  }

}
