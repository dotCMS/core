/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.achecker.dao;import java.io.File;import java.sql.Connection;import java.sql.DatabaseMetaData;import java.sql.SQLException;import org.apache.commons.logging.Log;import org.apache.commons.logging.LogFactory;import com.dotmarketing.util.ConfigUtils;import org.h2.jdbcx.JdbcConnectionPool;public class ACheckerConnectionFactory {	private static final Log LOG = LogFactory.getLog(ACheckerConnectionFactory.class);	private static JdbcConnectionPool pool = null;	private static Connection connection = null;	/**	 * This method retrieves the default connection to the dotCMS DB	 * @return	 * @throws Exception	 */	public static Connection getConnection() throws Exception {		try {			if (connection == null || connection.isClosed()) {				connection = getConn();				LOG.debug(  "Connection opened for thread "  );			}			return connection;		} catch (Exception e) {			LOG.error(  "---------- DBConnectionFactory: error : " + e);			LOG.debug(  "---------- DBConnectionFactory: error ", e);			throw new SQLException(e.toString());		}	}	private static Connection getConn() throws Exception {		if ( pool == null ) {			try {				int x = 1;				String acheckerPath = ConfigUtils.getACheckerPath() + File.separator + "achecker.sql";				String paramLoadScript = "INIT=RUNSCRIPT FROM '" + acheckerPath + "' CHARSET 'utf8'";				String extraParms = ";LOCK_MODE=0;DB_CLOSE_ON_EXIT=TRUE;IGNORECASE=FALSE;FILE_LOCK=NO;MODE=MySQL;" + paramLoadScript;				File dbRoot = new File(ConfigUtils.getDynamicContentPath() + File.separator + "h2db/" + x + "/achecker_db" + x + extraParms);				String dbRootLocation = dbRoot.getAbsolutePath();				String connectURI = "jdbc:h2:split:nio:"+dbRootLocation;				connectURI = connectURI.replace("\\", "/");				pool = JdbcConnectionPool.create(connectURI, "sa", "sa");				pool.setMaxConnections(1000);				pool.setLoginTimeout(3);				Connection connection = pool.getConnection();				return connection;			} catch (Exception e) {				e.printStackTrace();				System.err.println("Unable to start db properly : " + e.getMessage());			}		}		else {			return pool.getConnection();		}		return null;	}	/**	 * This method closes all the possible opened connections	 * @throws SQLException	 */	public static void closeConnection() throws SQLException {		try {			if ( connection!= null  ) {				connection.close();				connection = null;			}		} catch (Exception e) {			LOG.error(  "---------- DBConnectionFactory: error closing the db dbconnection ---------------", e);			throw new SQLException(e.toString());		}	}	public static int getDbVersion() throws Exception{		int version = 0;		try {			Connection con = getConnection();			DatabaseMetaData meta = con.getMetaData();			version = meta.getDatabaseMajorVersion();		} catch (Exception e) {			LOG.error("---------- DBConnectionFactory: Error getting DB version " + "---------------", e);			throw new Exception(e.toString());		}		return version;	}}