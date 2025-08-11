package com.dotmarketing.business.cache.provider.h22;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

public class H22HikariPool {

	final int dbNumber;
	final String dbRoot;
	final String database;
	final int minPoolSize = Config.getIntProperty("cache.h22.db.poolsize.min", 20);
	final int maxPoolSize = Config.getIntProperty("cache.h22.db.poolsize.max", 500);
	final int idleTimeoutMs = Config.getIntProperty("cache.h22.db.poolsize.idle.timeout", 600000);
	final int connectionTimeout = Config.getIntProperty("cache.h22.db.connection.timeout", 1000);
	final int setLeakDetectionThreshold = Config.getIntProperty("cache.h22.db.leak.detection.timeout", 0);
	final HikariDataSource datasource;
	final String folderName;
	AtomicBoolean running = new AtomicBoolean(false);
	final String extraParms = Config.getStringProperty("cache.h22.db.extra.params", ";DB_CLOSE_ON_EXIT=FALSE"); //;LOCK_MODE=0;DB_CLOSE_ON_EXIT=FALSE;FILE_LOCK=NO
	
	public H22HikariPool(String dbRoot, int dbNumber) {
		this(dbRoot,dbNumber,new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) );
	}
	
	public H22HikariPool(String dbRoot, int dbNumber, String database) {
		this.dbNumber = dbNumber;
		this.dbRoot = dbRoot;
		this.database = database;
		folderName = dbRoot  + File.separator  + dbNumber +File.separator 
				+ database;
		datasource = getDatasource();
		running.set(true);
	}
	

	public H22HikariPool(int dbNumber) {
		this(ConfigUtils.getDynamicContentPath(), dbNumber);
	}

	private String getDbUrl() {
		String params = extraParms;
		new File(folderName).mkdirs();
		String ret = "jdbc:h2:" + folderName + File.separator + "cache" + params;
		return ret;
	}

	private HikariDataSource getDatasource() {

		HikariConfig config = new HikariConfig();
		config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
		config.addDataSourceProperty("URL", getDbUrl());
		config.addDataSourceProperty("user", "sa");
		config.addDataSourceProperty("password", "sa");
		config.setMaximumPoolSize(maxPoolSize);
		config.setConnectionTimeout(connectionTimeout);
		config.setIdleTimeout(idleTimeoutMs); 
		config.setMinimumIdle(minPoolSize);
		config.setRegisterMbeans(Config.getBooleanProperty("hikari.register.mbeans", true));
		Logger.info(this.getClass(), "H22 on disk cache:" + getDbUrl());
		if(setLeakDetectionThreshold>0){
			config.setLeakDetectionThreshold(setLeakDetectionThreshold);
		}
		return new HikariDataSource(config);

	}

	public boolean running() {
		return running.get();
	}
	
    public void stop() {
        running.set(false);
    }
    
    public void start() {
        running.set(true);
    }
    
	public Optional<Connection> connection() throws SQLException {
		if (!running.get()) {
			return Optional.empty();
		}

		return Optional.of(datasource.getConnection());
	}

	public void close() {
	    running.set(false);
		datasource.close();
	}
	
	
	

}