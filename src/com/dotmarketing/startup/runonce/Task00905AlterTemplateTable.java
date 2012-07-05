package com.dotmarketing.startup.runonce;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.WebKeys;

/**
 * Runonce class that alter the Template table for design feature.
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * May 3, 2012 - 10:08:29 AM
 */
public class Task00905AlterTemplateTable implements StartupTask {
    
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		Logger.info(this.getClass(), "Loading sql file for alter template table");
		String dbType = DbConnectionFactory.getDBType();
		String pathToSqlDesignTemplateFile = null;
		if (dbType.equalsIgnoreCase(DbConnectionFactory.POSTGRESQL)) {
			pathToSqlDesignTemplateFile = Config.getStringProperty(WebKeys.FILE_PATH_SQL_TEMPLATE_DESIGN)+File.separator+"postgres.sql";
		}
		if (dbType.equalsIgnoreCase(DbConnectionFactory.MSSQL)) {
			pathToSqlDesignTemplateFile = Config.getStringProperty(WebKeys.FILE_PATH_SQL_TEMPLATE_DESIGN)+File.separator+"mssql.sql";
		}
		if (dbType.equalsIgnoreCase(DbConnectionFactory.MYSQL)) {
			pathToSqlDesignTemplateFile = Config.getStringProperty(WebKeys.FILE_PATH_SQL_TEMPLATE_DESIGN)+File.separator+"mysql.sql";
		}
		if (dbType.equalsIgnoreCase(DbConnectionFactory.ORACLE)) {
			pathToSqlDesignTemplateFile = Config.getStringProperty(WebKeys.FILE_PATH_SQL_TEMPLATE_DESIGN)+File.separator+"oracle.sql";
		}
		StringBuilder schema = new StringBuilder();
		int processedStatementCount = 0;
		
		MaintenanceUtil.flushCache();
		try {

			ClassLoader classLoader = Thread.currentThread()
					.getContextClassLoader();
			URL url = classLoader.getResource(pathToSqlDesignTemplateFile);
			FileInputStream fstream = new FileInputStream(url.getPath());

			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while ((strLine = br.readLine()) != null) {
				if (!(strLine.startsWith("--"))) {
					schema.append(strLine);
					schema.append("\n");
				}
			}

			in.close();
			String schemaString = schema.toString();
			if (dbType.equalsIgnoreCase(DbConnectionFactory.MYSQL)) {
				schemaString = schema.toString().toLowerCase();
			}
			List<String> tokens = SQLUtil.tokenize(schemaString);


			DotConnect dc = new DotConnect();
			java.sql.Connection con = DbConnectionFactory.getConnection();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.executeStatement("SET storage_engine=INNODB", con);
			}
			for (String token : tokens) {
				++processedStatementCount;
				try{
					dc.executeStatement(token, con);
				}catch (Exception e) {
					Logger.fatal(this.getClass(), "Error: " + e.getMessage() + "while trying to execute " + token + " proccessed "
							+ processedStatementCount + " statements", e);
					throw new DotDataException(e.getMessage(),e);
				}
			}
			Logger.info(this, "Alter table done (" + processedStatementCount
					+ " statements executed)");

		} catch (Exception e) {
			Logger.fatal(this.getClass(), "Error: " + e.getMessage() + " proccessed "
					+ processedStatementCount + " statements", e);
		}

	}
	
    public boolean forceRun() {
		Connection conn = null;
		try{
			conn = DbConnectionFactory.getConnection();
			Statement test_st = conn.createStatement();
			ResultSet rs = test_st.executeQuery("select drawed from template");
			rs.next();
			return false;
		}catch(DotRuntimeException dre){
			Logger.fatal(this.getClass(),"Unable to get dotCMS database connection.  Please change your connection properties and restart");
			Logger.fatal(this.getClass(),"Unable to get dotCMS database connection.  Please change your connection properties and restart");
			Logger.fatal(this.getClass(),"Unable to get dotCMS database connection.  Please change your connection properties and restart");
			return false;
		}catch(SQLException e){
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
