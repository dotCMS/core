package com.dotmarketing.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import com.dotcms.repackage.com.zaxxer.hikari.HikariConfig;
import com.dotcms.repackage.com.zaxxer.hikari.HikariDataSource;

public class DotHikariPool {



  final HikariDataSource datasource;
  final HikariConfig config;
  volatile boolean running = false;


  public DotHikariPool(final Properties props) {
    this.config = new HikariConfig(props);
    this.datasource = getDatasource();
    this.running = true;
  }

  public DotHikariPool(final HikariConfig config) {
    this.config = config;
    this.datasource = getDatasource();
    this.running = true;
  }

  private HikariDataSource getDatasource() {


    return new HikariDataSource(config);

  }

  public boolean running() {
    return running;
  }

  public Optional<Connection> connection() throws SQLException {
    if (!running) {
      return Optional.empty();
    }

    return Optional.of(datasource.getConnection());
  }

  public void close() {
    running = false;
    datasource.close();
  }



}
