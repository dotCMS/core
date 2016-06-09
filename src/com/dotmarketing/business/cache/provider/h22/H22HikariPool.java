package com.dotmarketing.business.cache.provider.h22;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class H22HikariPool {

	final int db;
	final String dbRoot;
	final int maxPoolSize = Config.getIntProperty("cache.h22.db.poolsize.max", 500);
	final HikariDataSource datasource;
	final String folderName;
	boolean running = false;
	final String extraParms = Config.getStringProperty("cache.h22.db.extra.params", ";DB_CLOSE_ON_EXIT=FALSE"); //;LOCK_MODE=0;DB_CLOSE_ON_EXIT=FALSE;FILE_LOCK=NO
	
	public H22HikariPool(String dbRoot, int dbNumber) {
		this.db = dbNumber;
		this.dbRoot = dbRoot;
		folderName = dbRoot + File.separator + "h22cache" + File.separator + "db-" + db + "_"
				+ new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(new Date());
		datasource = getDatasource();
		running = true;
	}

	public H22HikariPool(int dbNumber) {
		this(ConfigUtils.getDynamicContentPath(), dbNumber);
	}

	private String getDbUrl() {
		String params = extraParms;
		new File(folderName).mkdirs();
		String ret = "jdbc:h2:" + folderName + File.separator + "cache_db_" + db + params;
		return ret;
	}

	private HikariDataSource getDatasource() {

		HikariConfig config = new HikariConfig();
		config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
		config.setConnectionTestQuery("VALUES 1");
		config.addDataSourceProperty("URL", getDbUrl());
		config.addDataSourceProperty("user", "sa");
		config.addDataSourceProperty("password", "sa");
		config.setMaximumPoolSize(maxPoolSize);
		Logger.info(this.getClass(), "H22 on disk cache:" + getDbUrl());
		return new HikariDataSource(config);

	}

	public boolean running() {
		return running;
	}

	public Optional<Connection> connection() throws SQLException {
		Optional<Connection> opt;
		if (!running) {
			return Optional.empty();
		}

		return Optional.of(datasource.getConnection());
	}

	public void dispose() {
		running = false;

		datasource.close();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {

			e1.printStackTrace();
		}
		File folder = new File(folderName);

		File trashFolder = new File(dbRoot + File.separator + "trash" + File.separator + folder.getName());
		trashFolder.mkdirs();
		trashFolder.delete();

		try {
			FileUtil.move(folder, trashFolder);
		} catch (Exception e) {
			Logger.warn(getClass(), "Failed to move: " + folderName, e);

		}

	}

}