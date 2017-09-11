package com.dotmarketing.startup.runalways;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;

public class Task00001LoadSchema implements StartupTask {

	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		Logger.info(this.getClass(), "Loading schema");
		String schemaFile = null;
		if (DbConnectionFactory.isPostgres()) {
			schemaFile = "postgres.sql";
		}
		if (DbConnectionFactory.isMsSql()) {
			schemaFile = "mssql.sql";
		}
		if (DbConnectionFactory.isMySql()) {
			schemaFile = "mysql.sql";
		}
		if (DbConnectionFactory.isOracle()) {
			schemaFile = "oracle.sql";
		}
		if(DbConnectionFactory.isH2()) {
		    schemaFile = "h2.sql";
		}
		StringBuilder schema = new StringBuilder();
		int processedStatementCount = 0;
		
		//flush cache before we do a new import
		MaintenanceUtil.flushCache();
		try {
			// Open the file
			ClassLoader classLoader = Thread.currentThread()
					.getContextClassLoader();
			URL url = classLoader.getResource(schemaFile);
			FileInputStream fstream = new FileInputStream(url.getPath());
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				if (!(strLine.startsWith("--"))) {
					schema.append(strLine);
					schema.append("\n");
				}
			}
			// Close the input stream
			in.close();
			String schemaString = schema.toString();
			if (DbConnectionFactory.isMySql()) {
				schemaString = schema.toString().toLowerCase();
			}
			List<String> tokens = SQLUtil.tokenize(schemaString);


			DotConnect dc = new DotConnect();
			java.sql.Connection con = DbConnectionFactory.getConnection();
			if(DbConnectionFactory.isMySql()){
				dc.executeStatement("SET " + DbConnectionFactory.getMySQLStorageEngine() + "=INNODB", con);
			}
			for (String token : tokens) {
				++processedStatementCount;
				try{
					dc.executeStatement(token, con);
				}catch (Exception e) {
					Logger.fatal(this.getClass(), "Error: " + e.getMessage() + "while trying to execute " + token + " proccessed "
							+ processedStatementCount + " statements", e);
					if (br != null) {
						br.close();
					}
					throw new DotDataException(e.getMessage(),e);
				}
			}
			Logger.info(this, "Schema created (" + processedStatementCount
					+ " statements executed)");

		} catch (Exception e) {// Catch exception if any
			Logger.fatal(this.getClass(), "Error: " + e.getMessage() + " proccessed "
					+ processedStatementCount + " statements", e);
		}

		LicenseManager.getInstance().reloadInstance();

	}

	public boolean forceRun() {
		
		Connection conn = null;
		
		try{
			conn =DbConnectionFactory.getConnection();
		}
		catch(DotRuntimeException dre){
			Logger.fatal(this.getClass(),"Unable to get dotCMS database connection.  Please change your connection properties and restart");
			Logger.fatal(this.getClass(),"Unable to get dotCMS database connection.  Please change your connection properties and restart");
			Logger.fatal(this.getClass(),"Unable to get dotCMS database connection.  Please change your connection properties and restart");
		}
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) as test from inode");
			rs.next();
			return false;
		} catch (SQLException e1) {
			Logger.info(this.getClass(),"-------------------------------------------------------------------------------------");
			Logger.info(this.getClass(),"");
			Logger.info(this.getClass(),"Empty dotCMS database found.  Loading initial dotCMS schema for " + DbConnectionFactory.getDBType());
			Logger.info(this.getClass(),"");
			Logger.info(this.getClass(),"-------------------------------------------------------------------------------------");
			return true;
		}finally{
			try{
				if(conn != null){
					conn.close();
				}
			}
			catch(Exception e){
				Logger.error(this.getClass(),"Unable to close connection... Should not be here.");
			}

		}


		
	}

}

