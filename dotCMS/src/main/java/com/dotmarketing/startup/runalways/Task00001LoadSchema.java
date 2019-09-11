package com.dotmarketing.startup.runalways;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * This task verifies if the current dotCMS environment has access to an existing database. If it
 * does, the application will work with the existing data and will continue with the startup
 * process. Otherwise, the OOTB database schema for the system will be generated from scratch,
 * including the Demo Site.
 *
 * @author root
 * @since Mar 22, 2012
 */
public class Task00001LoadSchema implements StartupTask {

	@Override
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
		final StringBuilder schema = new StringBuilder();
		int processedStatementCount = 0;
		
		//flush cache before we do a new import
		MaintenanceUtil.flushCache();
		try {
			// Open the file
			final ClassLoader classLoader = Thread.currentThread()
					.getContextClassLoader();
			final InputStream fstream = classLoader.getResource(schemaFile).openStream();
			// Get the object of DataInputStream
			final DataInputStream in = new DataInputStream(fstream);
			final BufferedReader br = new BufferedReader(new InputStreamReader(in));
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
			final List<String> tokens = SQLUtil.tokenize(schemaString);
			final DotConnect dc = new DotConnect();
			try(Connection con = DbConnectionFactory.getDataSource().getConnection()){
    			if(DbConnectionFactory.isMySql()){
    				dc.executeStatement("SET " + DbConnectionFactory.getMySQLStorageEngine() + "=INNODB", con);
    			}
    			for (final String token : tokens) {
    				++processedStatementCount;
    				try{
    					dc.executeStatement(token, con);
    				}catch (Exception e) {
    					Logger.fatal(this.getClass(), "Error: " + e.getMessage() + "while trying to execute " + token + " processed "
    							+ processedStatementCount + " statements", e);
    					if (br != null) {
    						br.close();
    					}
    					throw new DotDataException(e.getMessage(),e);
    				}
    			}
			}
			Logger.info(this, "Schema created (" + processedStatementCount
					+ " statements executed)");

		} catch (Exception e) {// Catch exception if any
			Logger.fatal(this.getClass(), "Error: " + e.getMessage() + " processed "
					+ processedStatementCount + " statements", e);
		}
		LicenseManager.getInstance().reloadInstance();
	}

	@Override
	public boolean forceRun() {
		
		Connection conn = null;
		
		try{
			conn =DbConnectionFactory.getConnection();
		}
		catch(DotRuntimeException dre){
			Logger.fatal(this.getClass(),"Unable to get the dotCMS database connection. Please " +
					"change your connection properties and restart");
			Logger.fatal(this.getClass(),"Unable to get the dotCMS database connection. Please " +
					"change your connection properties and restart");
			Logger.fatal(this.getClass(),"Unable to get the dotCMS database connection. Please " +
					"change your connection properties and restart");
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
