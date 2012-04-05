package com.eng.achecker.dao;import java.sql.Connection;import java.sql.DatabaseMetaData;import java.sql.SQLException;import org.apache.commons.logging.Log;import org.apache.commons.logging.LogFactory;import com.dotmarketing.db.DbConnectionFactory;import com.eng.achecker.utility.PropertyLoader;public class ACheckerConnectionFactory {	private static final Log LOG = LogFactory.getLog(ACheckerConnectionFactory.class);	private static Connection connection = null;	//	public static DataSource getDataSource() throws Exception {//		try {//			InitialContext ctx = new InitialContext();//			DataSource ds = (DataSource) JNDIUtil.lookup(ctx, PropertyLoader.getValue(DBConstants.CONNECTION_DS));//			// DataSource ds = (DataSource)ctx.lookup( PropertyLoader.getValue(  DBConstants.CONNECTION_DS ));//			return ds;////		} catch (Exception e) {//			LOG.error(  "error getting dbconnection " +  DBConstants.CONNECTION_DS, e);//			throw new Exception(e.toString());//		}//	}	/**	 * This method retrieves the default connection to the dotCMS DB	 * @return	 * @throws Exception 	 */	public static Connection getConnection() throws Exception {		try {			if (connection == null || connection.isClosed()) {				connection = getConn();
				LOG.debug(  "Connection opened for thread "  );
			}
			return connection;
		} catch (Exception e) {
			LOG.error(  "---------- DBConnectionFactory: error : " + e);			LOG.debug(  "---------- DBConnectionFactory: error ", e);
			throw new SQLException(e.toString());
		}
	}

	private static Connection getConn() throws Exception {		String jdbcName = PropertyLoader.getValue(DBConstants.CONNECTION_DS);		connection = DbConnectionFactory.getConnection(jdbcName);		return connection;	}

	/**
	 * This method closes all the possible opened connections	 * @throws SQLException 
	 */
	public static void closeConnection() throws SQLException {
		try {
			if ( connection!= null  ) {				connection.close();				connection = null;			}		} catch (Exception e) {
			LOG.error(  "---------- DBConnectionFactory: error closing the db dbconnection ---------------", e);
			throw new SQLException(e.toString());
		}

	}
 	public static int getDbVersion() throws Exception{		int version = 0;		try {			Connection con = getConnection();			DatabaseMetaData meta = con.getMetaData();			version = meta.getDatabaseMajorVersion();		} catch (Exception e) {			LOG.error("---------- DBConnectionFactory: Error getting DB version " + "---------------", e);			throw new Exception(e.toString());		}		return version;	}
}
