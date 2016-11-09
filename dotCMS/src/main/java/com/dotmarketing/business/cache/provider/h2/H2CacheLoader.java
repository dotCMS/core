package com.dotmarketing.business.cache.provider.h2;

import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.repackage.org.apache.commons.collections.map.LRUMap;
import com.dotcms.repackage.org.jboss.cache.*;

import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.util.CacheUtil;
import com.dotmarketing.cache.RegionLock;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class H2CacheLoader extends CacheProvider  {

	private static final long serialVersionUID = 5285667050052706116L;

	private Boolean isInitialized = false;

	static final String DEFAULT_CACHE = CacheProviderAPI.DEFAULT_CACHE;
	static final String LIVE_CACHE_PREFIX = CacheProviderAPI.LIVE_CACHE_PREFIX;
	static final String WORKING_CACHE_PREFIX = CacheProviderAPI.WORKING_CACHE_PREFIX;

	private static Map cannotCacheCache = Collections.synchronizedMap(new LRUMap(1000));
	private int numberOfSpaces = 9;
	private int dbsPerSpace = Config.getIntProperty("DBS_PER_SPACE", 1);
	protected static int dbsInitialized = 0;
	private Map<Integer, JdbcConnectionPool> conPool = new HashMap<>();

	private boolean allowConnections = true;
	


	@Override
	public String getName () {
		return "H2 Cache Provider";
	}

	@Override
	public String getKey () {
		return "LocalH2Disk";
	}

	@Override
	public void init () throws Exception {

		Iterator<String> it = Config.getKeys();
		while ( it.hasNext() ) {

			String key = it.next();
			if ( key == null ) {
				continue;
			}

			if ( key.startsWith("cache.") ) {

				String cacheName = key.split("\\.")[1];
				if ( key.endsWith(".disk") ) {
					boolean useDisk = Config.getBooleanProperty(key, false);
					if ( useDisk ) {
						Logger.info(this.getClass(), "***\t Cache Config Disk   : " + cacheName + ": true");
					}
				}

			}
		}

		if ( Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false) && Config.getBooleanProperty("CACHE_DISK_SHOULD_DELETE", false) ) {
			CacheUtil.Moveh2dbDir();
		}

		create();

		isInitialized = true;
	}

	@Override
	public boolean isInitialized () throws Exception {
		return isInitialized;
	}

	@Override
	public void put ( String group, String key, Object content ) {

		//Building the key
		Fqn fqn = new Fqn(group, key);

		//Check if we must exclude this record from this cache
		if ( exclude(group, key, fqn) ) {
			return;
		}

		try {
			//Add the given content to the group and for a given key
			put(fqn, key, content);
		} catch ( Exception e ) {
			Logger.debug(this, e.getMessage(), e);
		}
	}

	@Override
	public Object get ( String group, String key ) {

		Object foundObject = null;

		try {
			//Get the content from the group and for a given key
			Map m = get(new Fqn(group, key));
			if ( m != null ) {
				foundObject = m.get(key);
			}
		} catch ( Exception e ) {
			Logger.debug(this, e.getMessage(), e);
		}

		return foundObject;
	}

	@Override
	public void remove ( String group ) {

		try {
			//Invalidates the Cache for the given group
			remove(new Fqn(group));
		} catch ( Exception e ) {
			Logger.debug(this, e.getMessage(), e);
		}
	}

	@Override
	public void remove ( String group, String key ) {

		try {

			if ( !UtilMethods.isSet(key) ) {
				Logger.error(this, "Empty key passed in, clearing group " + group + " by mistake");
			}

			//Invalidates from Cache a key from a given group
				remove(new Fqn(group, key), key.toLowerCase());
		} catch ( Exception e ) {
			Logger.error(this, e.getMessage(), e);
		}
	}

	@Override
	public void removeAll () {

		Set<String> currentGroups = new HashSet<>();
		currentGroups.addAll(getGroups());

		for ( String group : currentGroups ) {
			remove(group);
		}

		resetCannotCacheCache();
	}

	@Override
	public Set<String> getGroups () {

		try {
			return _getGroups();
		} catch ( SQLException e ) {
			Logger.error(this, "Error getting list of groups.", e);
		}

		return null;
	}

	@Override
	public Set<String> getKeys ( String group ) {

		Set<String> keys = new HashSet<>();

		try {
			keys = getGroupKeys(group);
		} catch ( Exception ex ) {
			Logger.error(this, "can't get h2 cache keys on group " + group, ex);
		}

		return keys;
	}

	@Override
	public List<Map<String, Object>> getStats () {

		List<Map<String, Object>> list = new ArrayList<>();

		Set<String> currentGroups = new HashSet<>();
		currentGroups.addAll(getGroups());

		for ( String group : currentGroups ) {

			Map<String, Object> stats = new HashMap<>();
			stats.put("name", getName());
			stats.put("key", getKey());
			stats.put("region", group);
			stats.put("toDisk", true);

			boolean isDefault = false;
			stats.put("isDefault", isDefault);
			stats.put("memory", -1);
			stats.put("disk", getGroupCount(group));
			// we don't limit the size of the disk cache
			stats.put("configuredSize", -1);

			list.add(stats);
		}

		return list;
	}

	@Override
	public void shutdown () {
		destroy();
		isInitialized = false;
	}
	
	public void resetCannotCacheCache(){
		cannotCacheCache = Collections.synchronizedMap(new LRUMap(1000));	
	}
	
	public Map<Object, Object> get(Fqn arg0) throws Exception {
		return loadAttributes(arg0);
	}



	public void put(List<Modification> arg0) throws Exception {
		for (Modification mod : arg0) {
			put(mod.getFqn(), mod.getData());
		}
	}

	public void put(Fqn arg0, Map<Object, Object> arg1) throws Exception {
		doMarshall(arg0, arg1);		
	}

	public Object put ( Fqn arg0, Object key, Object value ) throws Exception {

		Map m = new HashMap();
		Object retval = m.put(key, value);
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
				addConPoolToPoolMap(x, cp);
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

	private synchronized void addConPoolToPoolMap(int dbNumber,JdbcConnectionPool cp){
		conPool.put(dbNumber, cp);
	}
	
	private synchronized void addDbsInited(){
		dbsInitialized++;
	}

	public void create () throws Exception {

		Logger.info(this, "Starting Disk Cache");
		int x = 1;
		while ( x <= numberOfSpaces * dbsPerSpace ) {
			new dbInitThread(x).start();
			x++;
		}
		while ( dbsInitialized < numberOfSpaces * dbsPerSpace ) {
			try {
				Thread.sleep(100);
			} catch ( Exception e ) {
				Logger.debug(this, "Cannot sleep : ", e);
			}
		}
		Logger.info(this, "Disk Cache Started");
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
						conn.createStatement().execute(Config.getBooleanProperty("H2_SHUTDOWN_IMEDIATELY",true) ? "SHUTDOWN IMMEDIATELY" : "SHUTDOWN");
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


	private Connection createConnection ( boolean autoCommit, int dbnumber ) throws SQLException {
		return createConnection(autoCommit, dbnumber, false);
	}
	
	private Connection createConnection(boolean autoCommit, int dbnumber, boolean system) throws SQLException {
		if(!allowConnections && !system){
			return null;
		}
		Connection c;
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
		if (fqn.toString().length() > 255) {
			Logger.warn(this.getClass(), "Key exceeded 255 characters [" + fqn.toString() + "]");
			return;
		}

		if (cannotCacheCache.get(fqn.toString()) != null) {
			Logger.info(this, "returning because object is in cannot cache cache " + fqn.toString());
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
			} catch ( Exception e1 ) {
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

		ObjectInputStream input;
		InputStream bin;
		InputStream is;
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

		Map m;
		try {
			m = (Map) doUnmarshall(fqn);
			// m = (Map) regionAwareUnmarshall(fqn, child);
		} catch (FileNotFoundException fnfe) {
			// child no longer exists!
			m = null;
		} catch (Exception e) {
			Logger.error(this.getClass(), "Error unmarshalling object.", e);
			m = null;
		}
		return m;
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
				s.close();
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

	private Set<String> getGroupKeys ( String group ) throws Exception {

	    Set<String> keys=new HashSet<String>();
	    Connection conn = null;
	    PreparedStatement smt = null;
	    
	    for(int db : getDBNumbers(new Fqn(group))) {
	        try {
	            conn=createConnection(true,db);
	            smt=conn.prepareStatement("SELECT CACHE_KEY FROM `"+group+"`");
	            smt.setFetchSize(1000);
	            ResultSet rs=smt.executeQuery();
	            while(rs.next()) {
	                Fqn fqn=Fqn.fromString(rs.getString(1));
	                keys.add(fqn.getLastElementAsString());
	            }
	            rs.close();
	        }
	        catch(Exception ex) { 
	            throw new Exception("couldn't get keys on group "+group+" db number"+db,ex);
	        }
	        finally {
	            if(smt!=null) smt.close();
	            closeConnection(conn);
	        }
		}

		return keys;
	}

	public String getGroupCount ( String group ) {
		return _getGroupCount(group);
	}
	
	public Set<String> _getGroups() throws SQLException{
		Set<String> groups = new HashSet<String>();
		int x = 1;
		while(x<=numberOfSpaces*dbsPerSpace){
			Connection c = createConnection(true, x);
			Statement stmt = null;
			try {
    			stmt=c.createStatement();
    			ResultSet rs = stmt.executeQuery("SHOW TABLES");
    			if(rs != null){
    				while(rs.next()){
    					String table = rs.getString(1);
    					if(UtilMethods.isSet(table)){
    						groups.add(table);
    					}
    				}
    				rs.close();
    			}
    			x++;
			}
			finally {
			    stmt.close();
			    c.close(); 
			}
		}
		return groups;
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
					rs.close(); s.close();
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

	/**
	 * Method that verifies if must exclude content that can not or must not be added to this h2 cache
	 * based on the given cache group and key
	 *
	 * @param group
	 * @param key
	 * @return
	 */
	private boolean exclude ( String group, String key, Fqn fqn ) {

		Boolean exclude = false;

		if ( group.equals(ONLY_MEMORY_GROUP) ) {
			exclude = true;
		}

		if ( exclude ) {
			cannotCacheCache.put(fqn.toString(), fqn.toString());
		}

		return exclude;
	}

}