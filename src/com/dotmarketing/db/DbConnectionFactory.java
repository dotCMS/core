package com.dotmarketing.db;import java.sql.Connection;import java.sql.DatabaseMetaData;import java.sql.SQLException;import java.util.ArrayList;import java.util.HashMap;import java.util.Map.Entry;import javax.naming.Binding;import javax.naming.Context;import javax.naming.InitialContext;import javax.naming.NamingEnumeration;import javax.naming.NamingException;import javax.sql.DataSource;import net.sourceforge.jtds.jdbc.ConnectionJDBC2;import com.dotmarketing.exception.DotRuntimeException;import com.dotmarketing.util.Constants;import com.dotmarketing.util.Logger;import com.dotmarketing.util.UtilMethods;import com.liferay.util.JNDIUtil;public class DbConnectionFactory {	public static final String MYSQL = "MySQL";	public static final String POSTGRESQL = "PostgreSQL";	public static final String ORACLE = "Oracle";	public static final String MSSQL = "Microsoft SQL Server";	private static String _dbType = null;	private static final ThreadLocal<HashMap<String, Connection>> connectionsHolder = new ThreadLocal<HashMap<String, Connection>>();	public static DataSource getDataSource() {		try {			InitialContext ctx = new InitialContext();			DataSource ds = (DataSource)JNDIUtil.lookup(ctx, Constants.DATABASE_DEFAULT_DATASOURCE);			return ds;		} catch (Exception e) {			Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error getting dbconnection " + Constants.DATABASE_DEFAULT_DATASOURCE, e);			throw new DotRuntimeException(e.toString());		}	}	/**	 * This is used to get data source to other database != than the default dotCMS one	 * @param dataSource	 * @return	 */	public static DataSource getDataSource(String dataSource) {		try {			InitialContext ctx = new InitialContext();			DataSource ds = (DataSource)JNDIUtil.lookup(ctx, dataSource);			return ds;		} catch (Exception e) {			Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error getting dbconnection ---------------" +dataSource, e);			throw new DotRuntimeException(e.toString());		}	}	/**	 * This method retrieves the default connection to the dotCMS DB	 * @return	 */	public static Connection getConnection() {		try {			HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();			Connection connection = null;			if(connectionsList == null) {				connectionsList = new HashMap<String, Connection>();				connectionsHolder.set(connectionsList);			}				connection = connectionsList.get(Constants.DATABASE_DEFAULT_DATASOURCE);				if (connection == null || connection.isClosed()) {					DataSource db = getDataSource();					connection = db.getConnection();
					connectionsList.put(Constants.DATABASE_DEFAULT_DATASOURCE, connection);
					Logger.debug(DbConnectionFactory.class, "Connection opened for thread " + Thread.currentThread().getId() + "-" +
							Constants.DATABASE_DEFAULT_DATASOURCE);
				}
				// _dbType would only be null until the getDbType was called, then it is static				if(_dbType != null && MSSQL.equals(getDBType())){					connection.setTransactionIsolation(ConnectionJDBC2.TRANSACTION_SNAPSHOT);				}

			return connection;
		} catch (Exception e) {
			Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error : " + e);			Logger.debug(DbConnectionFactory.class, "---------- DBConnectionFactory: error ", e);			
			throw new DotRuntimeException(e.toString());
		}
	}

	/**
	 * Retrieves the list of all valid dataSources setup in the dotCMS
	 * @return
	 * @throws NamingException
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<String> getAllDataSources()throws NamingException{
		ArrayList<String> results = new ArrayList<String>();		Context ctx;			ctx = (Context) new InitialContext().lookup("java:comp/env");		NamingEnumeration ne=null;		try{
			ne = ctx.listBindings("jdbc");		} catch(NamingException e){			ctx = new InitialContext();			ne = ctx.listBindings("jdbc");		}		
		while(ne.hasMore()){
			Binding binding = (Binding)ne.next();
			Connection cn = null;
			try {
				DataSource db = (DataSource) ctx.lookup("jdbc/" + binding.getName());
				cn = db.getConnection();
				results.add("jdbc/" + binding.getName());
			} catch (Exception e) {
				Logger.info(DbConnectionFactory.class, "Unable to add " + binding.getName() + " to list of datasources: " + e.getMessage());
			} finally {
				if(cn != null)
					try {
						cn.close();
					} catch (SQLException e) { }
			}
		}
		return results;
	}

	/**
	 * Retrieves a connection to the given dataSource
	 * @param dataSource
	 * @return
	 */
	public static Connection getConnection(String dataSource) {

		try {
			HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();
			Connection connection = null;

			if(connectionsList == null) {
				connectionsList = new HashMap<String, Connection>();
				connectionsHolder.set(connectionsList);
			}


				connection = connectionsList.get(dataSource);

				if (connection == null || connection.isClosed()) {
					DataSource db = getDataSource(dataSource);
					Logger.debug(DbConnectionFactory.class, "Opening connection for thread " + Thread.currentThread().getId() + "-" +
							dataSource + "\n" + UtilMethods.getDotCMSStackTrace());
					connection = db.getConnection();
					connectionsList.put(dataSource, connection);
					Logger.debug(DbConnectionFactory.class, "Connection opened for thread " + Thread.currentThread().getId() + "-" +
							dataSource);
				}

			return connection;
		} catch (Exception e) {
			Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error getting dbconnection conn named", e);
			throw new DotRuntimeException(e.toString());
		}

	}

	/**
	 * This method closes all the possible opened connections
	 */
	public static void closeConnection() {
		try {
			HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();

			if(connectionsList == null) {
				connectionsList = new HashMap<String, Connection>();
				connectionsHolder.set(connectionsList);
			}


				Logger.debug(DbConnectionFactory.class, "Closing all connections for " + Thread.currentThread().getId() +
						"\n" + UtilMethods.getDotCMSStackTrace());
				for (Entry<String, Connection> entry : connectionsList.entrySet()) {

					String ds = entry.getKey();
					Connection cn = entry.getValue();
					if(cn != null)
						try {
							cn.close();
						} catch (Exception e) {
							Logger.warn(DbConnectionFactory.class, "---------- DBConnectionFactory: error closing the db dbconnection: " + ds + " ---------------", e);
						}
				}

				Logger.debug(DbConnectionFactory.class, "All connections closed for " + Thread.currentThread().getId());
				connectionsList.clear();

		} catch (Exception e) {
			Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error closing the db dbconnection ---------------", e);
			throw new DotRuntimeException(e.toString());
		}

	}

	/**
	 * This method closes a connection to the given datasource
	 */
	public static void closeConnection(String ds) {
		try {
			HashMap<String, Connection> connectionsList = (HashMap<String, Connection>) connectionsHolder.get();

			if(connectionsList == null) {
				connectionsList = new HashMap<String, Connection>();
				connectionsHolder.set(connectionsList);
			}


				Connection cn = connectionsList.get(ds);

				if(cn != null) {
					Logger.debug(DbConnectionFactory.class, "Closing connection for " + Thread.currentThread().getId() + "-" + ds +
							"\n" + UtilMethods.getDotCMSStackTrace());
					cn.close();
					connectionsList.remove(ds);
					Logger.debug(DbConnectionFactory.class, "Connection closed for " + Thread.currentThread().getId() + "-" + ds);
				}

		} catch (Exception e) {
			Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: error closing the db dbconnection: " + ds + " ---------------", e);
			throw new DotRuntimeException(e.toString());
		}

	}

	public static String getDBType() {

		/*
		 * Here is what this out outputs : MySQL PostgreSQL Microsoft SQL Server
		 * Oracle
		 */

		if (_dbType != null) {
			return _dbType;
		}

		Connection conn = getConnection();

		try {
			_dbType = conn.getMetaData().getDatabaseProductName();

		} catch (Exception e) {

		} finally {
			try {
				closeConnection();
			} catch (Exception e) {

			}
		}

		return _dbType;
	}
	public static String getDBDateTimeFunction(){		if(MSSQL.equals(getDBType())){			return "GETDATE()";		}else if(ORACLE.equals(getDBType())){			return "SYSDATE";		}else{			return "now()";		}	}	
	public static String getDBTrue() {
		String x = getDBType();

		if (MYSQL.equals(x)) {
			return "1";
		} else if (POSTGRESQL.equals(x)) {
			return "'true'";
		} else if (MSSQL.equals(x)) {
			return "1";
		} else if (ORACLE.equals(x)) {
			return "1";
		}
		return "true";

	}

	public static String getDBFalse() {
		String x = getDBType();

		if (MYSQL.equals(x)) {
			return "0";
		} else if (POSTGRESQL.equals(x)) {
			return "'false'";
		} else if (MSSQL.equals(x)) {
			return "0";
		} else if (ORACLE.equals(x)) {
			return "0";
		}
		return "false";

	}

	public static boolean isDBTrue(String value) {		String x = getDBType();		if (MYSQL.equals(x)) {			return value.trim().equals("1");		} else if (POSTGRESQL.equals(x)) {			return value.trim().equals("t") || value.trim().equals("true");		} else if (MSSQL.equals(x)) {			return value.trim().equals("1");		} else if (ORACLE.equals(x)) {			return value.trim().equals("1");		}		return false;	}		public static boolean isOracle(){		return ORACLE.equals(getDBType());	}		public static boolean isMsSql(){		return MSSQL.equals(getDBType());	}	public static boolean isPostgres(){		return POSTGRESQL.equals(getDBType());	}	public static boolean isMySql(){		return MYSQL.equals(getDBType());	}		public static int getDbVersion(){	   int version = 0;	    try {	    	 Connection con = getConnection();	    	 DatabaseMetaData meta = con.getMetaData();	         version = meta.getDatabaseMajorVersion();	        } catch (Exception e) {	        	Logger.error(DbConnectionFactory.class, "---------- DBConnectionFactory: Error getting DB version " + "---------------", e);				throw new DotRuntimeException(e.toString());	        }		return version;	}
}
