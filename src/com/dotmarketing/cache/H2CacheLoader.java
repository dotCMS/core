package com.dotmarketing.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.collections.map.LRUMap;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jboss.cache.CacheException;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.Modification;
import org.jboss.cache.RegionManager;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.loader.CacheLoader;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.ResourceWrapper;
import com.liferay.util.FileUtil;

public class H2CacheLoader implements CacheLoader{

	private static H2CacheLoader instance;
	private static Map cannotCacheCache = Collections.synchronizedMap(new LRUMap(1000));
	private int numberOfSpaces = 9;
	private int dbsPerSpace = Config.getIntProperty("DBS_PER_SPACE", 1);
	protected static int dbsInitialized = 0;
	private Map<Integer, JdbcConnectionPool> conPool = new HashMap<Integer, JdbcConnectionPool>();
	
	private String templateExtension=Config.getStringProperty("VELOCITY_TEMPLATE_EXTENSION");
	private String containerExtension=Config.getStringProperty("VELOCITY_CONTAINER_EXTENSION");
	private String fieldExtension=Config.getStringProperty("VELOCITY_FIELD_EXTENSION");

	private boolean allowConnections = true;
	
	public void commit(Object arg0) throws Exception {
		
	}

	public boolean exists(Fqn arg0) throws Exception {
		return false;
	}

	
	public static H2CacheLoader getInstance() throws Exception{
		if(instance ==null){
			synchronized (H2CacheLoader.class.getCanonicalName()) {
				if(instance== null){
					if (Config.getBooleanProperty("DIST_INDEXATION_ENABLED", true) && Config.getBooleanProperty("CACHE_DISK_SHOULD_DELETE", true)) 
						new H2CacheLoader().moveh2dbDir();
					
					new H2CacheLoader().create();
				}
			}
		}
		return instance;
		
	}
	
	
	public void resetCannotCacheCache(){
		cannotCacheCache = Collections.synchronizedMap(new LRUMap(1000));	
	}
	
	public Map<Object, Object> get(Fqn arg0) throws Exception {
		if (!cacheToDisk(arg0.toString())) {
			return null;
		}
		return loadAttributes(arg0);
	}

	public Set<?> getChildrenNames(Fqn arg0) throws Exception {
		return null;
	}

	public IndividualCacheLoaderConfig getConfig() {
		return null;
	}

	public void loadEntireState(ObjectOutputStream arg0) throws Exception {
				
	}

	public void loadState(Fqn arg0, ObjectOutputStream arg1) throws Exception {
		
	}

	public void prepare(Object arg0, List<Modification> arg1, boolean arg2)
			throws Exception {
		put(arg1);
		
	}

	public void put(List<Modification> arg0) throws Exception {
		for (Modification mod : arg0) {
			put(mod.getFqn(), mod.getData());

		}
		
	}

	public void put(Fqn arg0, Map<Object, Object> arg1) throws Exception {
		doMarshall(arg0, arg1);		
	}

	public Object put(Fqn arg0, Object key, Object value) throws Exception {
		Object retval;
		Map m = new HashMap();
		retval = m.put(key, value);
		put(arg0, m);
		return retval;
	}

	public void remove(Fqn arg0) throws Exception {
		deleteItem(arg0);
		
	}

	public Object remove(Fqn arg0, Object arg1) throws Exception {
		deleteItem(arg0);
		return null;
	}

	public void removeData(Fqn arg0) throws Exception {
		deleteItem(arg0);
		
	}

	public void rollback(Object arg0) {
		
	}

	public void setCache(CacheSPI arg0) {
		
	}

	public void setConfig(IndividualCacheLoaderConfig arg0) {
		
	}

	public void setRegionManager(RegionManager arg0) {
		
	}

	public void storeEntireState(ObjectInputStream arg0) throws Exception {
		
	}

	public void storeState(Fqn arg0, ObjectInputStream arg1) throws Exception {
		
	}

	private class dbInitThread extends Thread {
		private int x; 
		
		public dbInitThread(int x) {
			this.x = x;
		}
		
		@Override
		public void run() {
			Connection conn = null;
			try{
				String extraParms=";LOCK_MODE=0;DB_CLOSE_ON_EXIT=TRUE;FILE_LOCK=NO";
				File dbRoot=new File(ConfigUtils.getDynamicContentPath() + File.separator + "h2db/" + x + "/cache_db" + x + extraParms);
	
				String dbRootLocation=dbRoot.getAbsolutePath();
				String connectURI="jdbc:h2:split:nio:"+dbRootLocation;
				JdbcConnectionPool cp = JdbcConnectionPool.create(connectURI, "sa", "sa");
				cp.setMaxConnections(1000);
				cp.setLoginTimeout(3);
				conPool.put(x, cp);
				//make sure we can connect 
				conn = createConnection(true,x);
			}catch (Exception e) {
				Logger.fatal(this, "Unable to start db properly : " + e.getMessage(),e);
			}finally{
				closeConnection(conn);
			}
			addDbsInited();
		}
	}
	
	private class deleteTrashDir extends Thread {	
		
		@Override
		public void run() {
			File trashDir = new File(ConfigUtils.getDynamicContentPath() + File.separator + "trash");
			FileUtil.deltree(trashDir, false);
		}
	}
	
	private synchronized void addDbsInited(){
		dbsInitialized++;
	}
	
	public void create() throws Exception {
		instance = this;
		int x = 1;
		while(x<=numberOfSpaces*dbsPerSpace){
			new dbInitThread(x).start();
			x++;
		}
		while(dbsInitialized < numberOfSpaces*dbsPerSpace){
			try{
				Thread.sleep(100);
			}catch (Exception e) {
				Logger.debug(this, "Cannot sleep : ", e);
			}
		}
	}
	
	public void moveh2dbDir() throws Exception {
		File h2dbDir=new File(ConfigUtils.getDynamicContentPath() + File.separator + "h2db");
		File trashDir = new File(ConfigUtils.getDynamicContentPath() + File.separator + "trash" + File.separator + "h2db"+ESIndexAPI.timestampFormatter.format(new Date()));
		
		//move the dotsecure/h2db dir to dotsecure/trash/h2db{timestamp} 
		//FileUtil.move(h2dbDir, trashDir);
		FileUtil.copyDirectory(h2dbDir, trashDir);
		FileUtil.deltree(h2dbDir, false);
	
		//fire a separate thread that deletes the contents of the dotsecure/trash directory. 
		new deleteTrashDir().start();
	}

	public void destroy() {
		int x = 1;
		allowConnections = false;
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			Logger.error(H2CacheLoader.class,e1.getMessage(),e1);
		}
		while(x<=numberOfSpaces*dbsPerSpace){
			try {
				try{
					Connection conn = createConnection(true,x,true);
					try{
						conn.createStatement().execute("SHUTDOWN");
					}catch(org.h2.jdbc.JdbcSQLException ac){
						if(ac.getMessage().contains("already closed")){
							//ignore
						}else{
							Logger.warn(this, "Issue shutting down H2 DB : " + ac.getMessage(),ac);
						}
					}
				}catch (Exception e) {
					if(e.getMessage().contains("already closed")){
						//ignore
					}else{
						Logger.warn(this, "Issue shutting down H2 DB : " + e.getMessage(),e);
					}
				}
				try{
					conPool.get(x).dispose();
				}catch (Exception e) {
					Logger.error(this, "Problem closing H2 ConnPool", e);
				}
			} catch (Exception e) {
				Logger.error(H2CacheLoader.class,e.getMessage(),e);
			}
			x++;
		}
	}

	public void start() throws Exception {
		
	}

	public void stop() {
		
	}
	
	private Connection createConnection(boolean autoCommit, int dbnumber) throws SQLException {
		return createConnection(autoCommit, dbnumber, false);
	}
	
	private Connection createConnection(boolean autoCommit, int dbnumber, boolean system) throws SQLException {
		if(!allowConnections && !system){
			return null;
		}
		Connection c = null;
		try{
			c= conPool.get(dbnumber).getConnection();
		}catch (Exception e) {
			Logger.error(this, e.getMessage(),e);
			return null;
		}
		if(autoCommit==false){
			c.setAutoCommit(autoCommit);
		}
		return c;
	}
	
	private void doMarshall(Fqn fqn, Map attrs) throws Exception {
		if(fqn ==null){
			return;
		}
		fqn = Fqn.fromString(fqn.toString().toLowerCase());
		if (!cacheToDisk(fqn.toString())) {
			return;
		}
		if (fqn.toString().length() > 255) {
			return;
		}
		if (!cacheToDisk(fqn,attrs)) {
			return;
		}

		if (cannotCacheCache.get(fqn.toString()) != null) {
			Logger.info(this,
					"returning because object is in cannot cache cache " + fqn.toString());
			return;
		}

		String group= getGroupName(fqn);
		if (RegionLock.getInstance().isLocked(group)) {
			//Bulk delete in progress
			return ;
		}
		
		Connection c=createConnection(true,getDBNumber(fqn));
		if(c==null){
			return;
		}
		PreparedStatement deleteStmt = null;
		PreparedStatement insertStmt = null;
		
		try {
			
			deleteStmt=c.prepareStatement(buildDeleteItemSQL(fqn));
			String key=getKeyName(fqn);
			deleteStmt.setString(1,	key);
			try{
				deleteStmt.execute();
			}catch (Exception e) {
				if(e.getMessage().startsWith("Table") && e.getMessage().contains("not found")){
					Logger.error(this, "NEED TO CREATE TABLE");
					createTable(fqn);
				}
			}
			
			insertStmt=c.prepareStatement(buildInsertItemSQL(fqn));
			insertStmt.setString(1,key);
			ObjectOutputStream output = null;
			OutputStream bout =null ;
			ByteArrayOutputStream os=new ByteArrayOutputStream();
	
			if (Config.getBooleanProperty("USE_CACHE_COMPRESSION", false)) {
				bout = new DeflaterOutputStream(os);
				
			} else {
			
				bout = new BufferedOutputStream(os,
						8192);
			}
			
			output = new ObjectOutputStream(bout);
			output.writeObject(attrs);
			output.flush();
			byte[] data= os.toByteArray();
			insertStmt.setBytes(2, data);
			try{
				insertStmt.execute();
			}catch (Exception e) {
				createTable(fqn);
				try{
					insertStmt.execute();
				}catch (Exception e1) {
					Logger.fatal(this, "HERE");
					Logger.fatal(this, "FQN:" + fqn,e1);
				}
			}
		} catch (StackOverflowError e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (CacheException e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (RuntimeException e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (StreamCorruptedException e) {
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
		} catch (Exception e) {
			try {
				if(!e.getMessage().startsWith("Table") && !e.getMessage().contains("not found")){
					removeData(fqn);
					cannotCacheCache.put(fqn.toString(), fqn.toString());
				}
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			Logger.debug(this, "Unable to serialize object with FQN "
					+ fqn.toString(), e);
		} finally {
			if(deleteStmt != null){
				deleteStmt.close();
			}
			if(insertStmt != null){
				insertStmt.close();
			}
			closeConnection(c);
		}
	}
	
	private Object doUnmarshall(Fqn fqn) throws Exception {
		if(fqn ==null){
			return null;
		}
		fqn = Fqn.fromString(fqn.toString().toLowerCase());
		if (!cacheToDisk(fqn.toString())) {
			return null;
		}
		ObjectInputStream input = null;
		InputStream bin = null;
		InputStream is = null;
		Connection c=null;
		String groupName= getGroupName(fqn);
		if (RegionLock.getInstance().isLocked(groupName)) {
			//Bulk delete in progress
			return null;
		}
		PreparedStatement stmt = null;
		try {
			c=createConnection(true,getDBNumber(fqn));
			if(c==null){
				return null;
			}
			stmt=c.prepareStatement("SELECT CACHE_DATA FROM `" + groupName + "` WHERE CACHE_KEY=?");
			stmt.setString(1, getKeyName(fqn));
			ResultSet rs=stmt.executeQuery();
			if (!rs.next()) {
				return null;
			}
			is=new ByteArrayInputStream(rs.getBytes(1));
			if (Config.getBooleanProperty("USE_CACHE_COMPRESSION", false)) {
				bin = new InflaterInputStream(is);
			} else {
				bin = new BufferedInputStream(is, 8192);
			}
			
			input = new ObjectInputStream(bin);
			Object unmarshalledObj = input.readObject();
			return unmarshalledObj;
		} catch (StackOverflowError e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (CacheException e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (RuntimeException e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (StreamCorruptedException e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			try {
				removeData(fqn);
				cannotCacheCache.put(fqn.toString(), fqn.toString());
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} catch (IOException e) {
			return null;
		} catch (Exception e) {
			Logger.debug(this, "Unable to unserialize object with FQN "
					+ fqn.toString(), e);
			try {
				if(e.getMessage().contains("java.lang.ArrayIndexOutOfBoundsException")){
					return null;
				}else if(!e.getMessage().startsWith("Table") && !e.getMessage().contains("not found")){
					removeData(fqn);
					cannotCacheCache.put(fqn.toString(), fqn.toString());
				}else{
					createTable(fqn);
				}
			} catch (Exception e1) {
				Logger.warn(this, "Unable to delete file", e1);
			}
			return null;
		} finally {
			if(stmt!=null){
				stmt.close();
			}
			closeConnection(c);
			
		}
	}

	private String getGroupName(Fqn fqn) {
		return fqn.get(0).toString().toLowerCase();
	}
	
	private String getKeyName(Fqn fqn) {
		return fqn.toString().toLowerCase();
	}
	
	private void deleteItem(Fqn fqn) {
		if(fqn ==null){
			return;
		}
		fqn = Fqn.fromString(fqn.toString().toLowerCase());
		
		/*
		if(!canSerialize(fqn.toString())){
			return;
		}
		*/
		Statement stmt=null;
		PreparedStatement pstmt= null;
		String lockName=null;
		try {
			int size=fqn.size();
			if (size>=2) {
				Connection c=createConnection(true,getDBNumber(fqn));
				if(c==null){
					return;
				}
				try{
					Logger.debug(this, "Starting flush in h2 for " + fqn);
					pstmt=c.prepareStatement(buildDeleteItemSQL(fqn));
					pstmt.setString(1, getKeyName(fqn));
					pstmt.execute();
					Logger.debug(this, "Finished flush in h2 for " + fqn);
				}catch (SQLException se){
					Logger.debug(this, "Unable to delete item usually not a problem as table will now be created for " + fqn.toString() + ". If you continue to see this error for the same table it might be an issue.");
					createTable(fqn);
				}finally{
					if(pstmt!=null){
						pstmt.close();
					}
					closeConnection(c);
				}
			} else if (size==1) {
			
				Logger.info(this, "Starting Region Cache Flush in h2 for " + fqn);
				lockName=getGroupName(fqn);
				RegionLock.getInstance().lock(lockName);
				//Let's wait a half-second for other operations to finish
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Logger.debug(this, e.getMessage(),e);
				}
				for(int x : getDBNumbers(fqn)){
					Connection c=createConnection(true,x);
					if(c==null){
						return;
					}
					try{
						stmt=c.createStatement();
						stmt.execute(buildDeleteGroupSQL(fqn));
					}catch (SQLException se){
						Logger.debug(this, "Unable to delete group usually not a problem as table will now be created for " + fqn.toString() + ". If you continue to see this error for the same table it might be an issue.");
						createTable(fqn);
					}finally{
						if(stmt!=null){
							stmt.close();
						}
						closeConnection(c);
					}
				}
				Logger.info(this, "Finished Region Cache Flush in h2 for " + fqn);
			} else {
				Logger.info(this, "Starting Full Cache Flush in h2");
				lockName="/";
				RegionLock.getInstance().lock(lockName);
				//Let's wait a second for other operations to finish
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Logger.debug(this, e.getMessage(),e);
				}
				int x = 1;
				while(x<=numberOfSpaces*dbsPerSpace){
					Connection c=createConnection(false,x);
					if(c==null){
						return;
					}
					try{
						stmt=c.createStatement();
						ResultSet rs = stmt.executeQuery("SHOW TABLES");
						if(rs != null){
							Statement stmt1 = null;
							while(rs.next()){
								String table = rs.getString(1);
								if(UtilMethods.isSet(table)){
									try{
										stmt1=c.createStatement();
										stmt1.execute("TRUNCATE TABLE `" + table +"`");
									}catch(Exception e){
										Logger.error(this, "Error while flushing all cache : " + e.getMessage(),e);
									}finally{
										if(stmt1!=null){
											stmt1.close();
										}
									}
								}
							}
						}
						c.commit();
					}catch(Exception e){
						Logger.error(this, "Error while flushing all cache : " + e.getMessage(),e);
						c.rollback();
					}finally{
						if(stmt!=null){
							stmt.close();
						}
						closeConnection(c);
					}
					x++;
				}
				Logger.info(this, "Finished Full Cache Flush in h2");
			}
		} catch (SQLException e) {
			Logger.error(this, "Error deleting cache item : " + e.getMessage(), e);
		}finally {
			if (lockName!=null) {
				RegionLock.getInstance().unlock(lockName);
			}
		}
	}
	
	
	private void closeConnection(Connection c) {
		try {
			if (c!=null) {
				c.close();
			}
		} catch (SQLException e) {
			Logger.error(H2CacheLoader.class,"SQLException: " +e.getMessage(),e);
		}
	}
	
	
	
	private Map loadAttributes(Fqn fqn) throws Exception {
		if (!cacheToDisk(fqn.toString())) {
			return null;
		}

		Map m;
		try {
			m = (Map) doUnmarshall(fqn);
			// m = (Map) regionAwareUnmarshall(fqn, child);
		} catch (FileNotFoundException fnfe) {
			// child no longer exists!
			m = null;
		} catch (Exception e) {

			m = null;
		}
		return m;
	}
	
	private boolean cacheToDisk(String key) {
		if(key.indexOf("/")==key.lastIndexOf("/")){
			return false;
		}
		return canSerialize(key);
	}
	
	private boolean canSerialize(String key){
		if (key.startsWith("/velocitymenucache")) {
			return false;
		}
		if (key.startsWith("/velocitycache")) {
			if (!(key.contains("live") || key.contains("working"))) {
				return false;
			}
		}
		return true;
	}
	
	private String buildDeleteItemSQL(Fqn fqn){
		return "DELETE FROM `" + getGroupName(fqn) + "` WHERE CACHE_KEY=?";		
	}

	private String buildDeleteGroupSQL(Fqn fqn){
		return "TRUNCATE TABLE `" + getGroupName(fqn) + "`";
	}
	
	private String buildCountGroupSQL(String group){
		return "SELECT count(*) as c FROM `"+ group + "`";
	}
	
	private String buildInsertItemSQL(Fqn fqn){
		return "MERGE INTO `" + getGroupName(fqn) + "` key(CACHE_KEY) VALUES (?,?)";
	}
	
	private void createTable(Fqn fqn){
		Connection conn = null;
		try{
			try {
				conn = createConnection(true, getDBNumber(fqn));
			} catch (SQLException e1) {
				Logger.error(H2CacheLoader.class,"Unable to get connection : " + e1.getMessage(),e1);
				return;
			}
			if(conn==null){
				return;
			}
			try {
				Statement s=conn.createStatement();
				s.execute("CREATE CACHED TABLE IF NOT EXISTS `" + getGroupName(fqn) + "` (CACHE_KEY VARCHAR(255) PRIMARY KEY, CACHE_DATA BLOB)" );
				//s.execute("CREATE UNIQUE INDEX IF NOT EXISTS `" + getGroupName(fqn) + "_group_key_idx` ON  `" + getGroupName(fqn) + "` (CACHE_KEY)");
			 } catch (SQLException e) {
				Logger.error(H2CacheLoader.class,"SQLException: " +e.getMessage(),e);
			 }
		}finally{
			closeConnection(conn);
		}
	}
	
	private int getDBNumber(Fqn fqn){
		return (((Math.abs(fqn.hashCode()) % dbsPerSpace)) + 1) + (dbsPerSpace * getSpace(fqn));
	}

	public static String getGroupCount(String group){
		return instance._getGroupCount(group);
	}
	
	public Set<String> _getGroups() throws SQLException{
		Set<String> groups = new HashSet<String>();
		int x = 1;
		while(x<=numberOfSpaces*dbsPerSpace){
			Connection c = createConnection(true, x);
			Statement stmt = null;
			stmt=c.createStatement();
			ResultSet rs = stmt.executeQuery("SHOW TABLES");
			if(rs != null){
				while(rs.next()){
					String table = rs.getString(1);
					if(UtilMethods.isSet(table)){
						groups.add(table);
					}
				}
			}
			x++;
		}
		return groups;
	}
	
	public static Set<String> getGroups() throws SQLException{
		//USE THIS FOR JBCACHELOADER TO SETUP REGION MAP 
		//AND FOR CACHE TABLE STATS
		return instance._getGroups();
	}
	
	private String _getGroupCount(String group){
		long ret = 0;
		Fqn fqn = Fqn.fromElements(new String[]{group});
		Connection conn = null;
		int[] dbs = getDBNumbers(fqn);
		for (int i : dbs) {
			try{
				try {
					conn = createConnection(true, i);
				} catch (SQLException e1) {
					Logger.error(H2CacheLoader.class,"Unable to get connection : " + e1.getMessage(),e1);
				}
				if(conn==null){
					continue;
				}
				try {
					Statement s=conn.createStatement();
					ResultSet rs = s.executeQuery(buildCountGroupSQL(group));
					if(rs != null){
						rs.next();
						ret += rs.getLong(1);
					}
				 } catch (SQLException e) {
					Logger.debug(H2CacheLoader.class,"SQLException: " +e.getMessage(),e);
				 }
			}finally{
				closeConnection(conn);
			}
		}
		return new Long(ret).toString();
	}
	
	private int getSpace(Fqn fqn){
		if(getGroupName(fqn).startsWith("contentletcache")){
			return 1;
		}else if(getGroupName(fqn).startsWith("velocity")){
			return 2;
		}else if (getGroupName(fqn).contains("permission")){
			return 3;			
		}else if (getGroupName(fqn).startsWith("blockdirective")){
			return 4;
		}else if (getGroupName(fqn).startsWith("livecache")){
			return 5;
		}else if (getGroupName(fqn).startsWith("workingcache")){
			return 6;
		}else if (getGroupName(fqn).startsWith("filecache")){
			return 7;
		}else if (getGroupName(fqn).startsWith("category") || getGroupName(fqn).startsWith("category")){
			return 8;
		}else {
			return 0;
		}
	}
	
	private int[] getDBNumbers(Fqn fqn){
		int[] ret = new int[dbsPerSpace];
		int x = getSpace(fqn);
		int c=0;	
		while(c<dbsPerSpace){
			ret[c]=x*dbsPerSpace+c+1;
			c++;
		}
		return ret;
	}
	
	private boolean cacheToDisk(Fqn fqn, Map attrs) {
		String groupName= getGroupName(fqn);
		if (Config.getBooleanProperty("SKIP_MACRO_CACHE",true)) {
			if ("velocitycache".equalsIgnoreCase(groupName)) {
				String key=fqn.toString();
				if ( key.endsWith(containerExtension) || key.endsWith(templateExtension) || key.endsWith(fieldExtension)) {
					ResourceWrapper w=(ResourceWrapper)attrs.values().toArray()[0];
					boolean ret=RegEX.contains(w.getResource().getData().toString(), "\\[#macro\\]");
					return !ret;
				}
			}
		}
		return true;
	}
		
}
