package com.dotmarketing.business.cache.provider.h22;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.dotcms.repackage.com.google.common.cache.CacheStats;
import com.dotcms.repackage.org.apache.commons.collections.map.LRUMap;
import com.dotcms.repackage.org.apache.commons.io.comparator.LastModifiedFileComparator;
import com.dotcms.repackage.org.apache.commons.io.filefilter.DirectoryFileFilter;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.repackage.com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

public class H22Cache extends CacheProvider {



	private Boolean isInitialized = false;

	final static String TABLE_PREFIX = "cach_table_";

	@SuppressWarnings("unchecked")
	private final static Map<Object, Object> DONT_CACHE_ME = Collections.synchronizedMap(new LRUMap(1000));

	// number of different dbs to shard against
	private final int numberOfDbs = Config.getIntProperty("cache.h22.number.of.dbs", 2);

	// number of tables in each db shard
	private final int numberOfTablesPerDb = Config.getIntProperty("cache.h22.number.of.tables.per.db", 9);

	// limit error message to every 5 seconds;
	private final int limitErrorLogMillis = Config.getIntProperty("cache.h22.limit.one.error.log.per.milliseconds", 5000);

	// create a new cache store if our errors are greater that this. Anything <1
	// will disable auto recover
	private final long recoverAfterErrors = Config.getIntProperty("cache.h22.recover.after.errors", 5000);

	// try to recover with h2 if within this time (30m defualt)
	private final long recoverOnRestart = Config.getIntProperty("cache.h22.recover.if.restarted.in.milliseconds", 1000 * 60 * 30);
	private long lastLog = System.currentTimeMillis();
	private long[] errorCounter = new long[numberOfDbs];
	private final H22HikariPool[] pools = new H22HikariPool[numberOfDbs];
	private int failedFlushAlls=0;

	final String dbRoot;
	final private H2GroupStatsList stats = new H2GroupStatsList();

	public H22Cache(final String dbRoot) {
		this.dbRoot = dbRoot;
	}

	public H22Cache() {
		this(ConfigUtils.getDynamicContentPath() + File.separator + "h22cache");
	}
	@Override
	public String getName() {
		return "H22 Cache";
	}

	@Override
	public String getKey() {
		return "H22Cache";
	}

	@Override
	public void init() throws Exception {

		// init the databases
		for (int i = 0; i < numberOfDbs; i++) {
			getPool(i, true);
		}
		isInitialized = true;

	}

	@Override
	public boolean isInitialized() throws Exception {
		return isInitialized;
	}

	@Override
	public void put(String group, String key, Object content) {

		// Building the key
		Fqn fqn = new Fqn(group, key);

		try {
			// Add the given content to the group and for a given key

			doUpsert(fqn, (Serializable) content);

		} catch (ClassCastException e) {
			DONT_CACHE_ME.put(fqn.id, fqn.toString());
			handleError(e, fqn);

		} catch (Exception e) {
			handleError(e, fqn);
		}
	}

	@Override
	public Object get(String group, String key) {


		Object foundObject = null;
		long start = System.nanoTime();
		Fqn fqn = new Fqn(group, key);
		
		try {
			// Get the content from the group and for a given key;
			foundObject = doSelect(fqn);
			stats.group(fqn.group).hitOrMiss(foundObject);
			stats.group(fqn.group).readTime(System.nanoTime() - start);
		} catch (Exception e) {
			foundObject=null;
			handleError(e, fqn);
		}
		
		

		return foundObject;
	}

	@Override
	public void remove(String groupName) {

		Fqn fqn = new Fqn(groupName);

		Logger.info(this.getClass(), "Flushing H22 cache group:" + fqn + " Note: this can be an expensive operation");

		try {
			for (int db = 0; db < numberOfDbs; db++) {
				Optional<Connection> opt = createConnection(true, db);
				if (!opt.isPresent()) {
					continue;
				}
				Connection c = opt.get();
				for (int table = 0; table < numberOfTablesPerDb; table++) {
					PreparedStatement stmt = c.prepareStatement("DELETE from " + TABLE_PREFIX + table + " WHERE cache_group = ?");
					stmt.setString(1, fqn.group);
					stmt.executeUpdate();
				}

				c.close();

			}
		} catch (SQLException e) {

			handleError(e, fqn);
		}
	}

	@Override
	public void remove(String group, String key) {
		Fqn fqn = new Fqn(group, key);
		try {

			if (!UtilMethods.isSet(key)) {
				Logger.warn(this, "Empty key passed in, clearing group " + group + " by mistake");
			}

			// Invalidates from Cache a key from a given group
			doDelete(fqn);
		} catch (Exception e) {
			handleError(e, fqn);
		}
	}

	public void doTruncateTables() throws SQLException {

			for (int db = 0; db < numberOfDbs; db++) {
				Optional<H22HikariPool> poolOpt = getPool(db);
				if(!poolOpt.isPresent())continue;
				H22HikariPool pool = poolOpt.get();
				Optional<Connection> connOpt = pool.connection();
				if(!connOpt.isPresent())continue;
				Connection c = connOpt.get();
				try{
					pool.running=false;
					for (int table = 0; table < numberOfTablesPerDb; table++) {
						Statement stmt = c.createStatement();
						stmt.execute("truncate table " + TABLE_PREFIX + table);
						stmt.close();
					}
				}
				finally{
					pool.running=true;
					c.close();
				}
			}

	}
	

	
	
	@Override
	public void removeAll() {

		Logger.info(this, "Start Full Cache Flush in h22");
		long start = System.nanoTime();
		int failedThreshold = Config.getIntProperty("cache.h22.rebuild.on.removeAll.failure.threshhold", 1);
		failedThreshold = (failedThreshold<1) ? 1: failedThreshold;
		// we either truncate the tables on a full flush or rebuild the tables
		if(Config.getBooleanProperty("cache.h22.rebuild.on.removeAll", false) || failedFlushAlls==failedThreshold){
			dispose(true);
		}
		else{
			try {
				doTruncateTables();
				failedFlushAlls=0;
			} catch (SQLException e) {
				Logger.error(getClass(), e.getMessage());
				failedFlushAlls++;
			}
		}
		
		if(failedFlushAlls==failedThreshold)
		
		stats.clear();
		DONT_CACHE_ME.clear();
		long end = System.nanoTime();
		Logger.info(this, "End Full Cache Flush in h22 : " + TimeUnit.MILLISECONDS.convert(end-start, TimeUnit.NANOSECONDS)+ "ms");

	}

	@Override
	public Set<String> getGroups() {

		Set<String> groups = new HashSet<String>();
		try {
			for (int db = 0; db < numberOfDbs; db++) {
				Optional<Connection> opt = createConnection(true, db);
				if (!opt.isPresent()) {
					continue;
				}
				Connection c = opt.get();
				for (int table = 0; table < numberOfTablesPerDb; table++) {
					Statement stmt = c.createStatement();
					ResultSet rs = stmt.executeQuery("select DISTINCT(cache_group) from " + TABLE_PREFIX + table);
					if (rs != null) {
						while (rs.next()) {
							String groupname = rs.getString(1);
							if (UtilMethods.isSet(groupname)) {
								groups.add(groupname);
							}
						}
						rs.close();
						stmt.close();
					}
				}
				c.close();
			}
		} catch (SQLException e) {
			Logger.warn(this.getClass(), "cannot get groups : " + e.getMessage());
		}

		return groups;
	}

	@Override
	public List<Map<String, Object>> getStats() {

		List<Map<String, Object>> list = new ArrayList<>();

		Set<String> currentGroups = new HashSet<>();
		currentGroups.addAll(getGroups());
		for (String group : currentGroups) {
			H22GroupStats groupStats = stats.group(group);

			CacheStats googleStats = new CacheStats(groupStats.hits, groupStats.misses, groupStats.hits, 0, groupStats.totalTimeReading, 0);

			Map<String, Object> stats = new HashMap<>();
			stats.put("CacheStats", googleStats);
			stats.put("name", getName());
			stats.put("key", getKey());
			stats.put("region", group);
			stats.put("toDisk", true);
			stats.put("entrySize", groupStats.totalSize);
			boolean isDefault = false;
			stats.put("isDefault", isDefault);
			stats.put("memory", -1);

			// we don't limit the size of the disk cache
			stats.put("configuredSize", -1);

			try {
				stats.put("disk", _getGroupCount(group));
			} catch (SQLException e) {
				Logger.warn(this, "can't get h22 group data for: " + group, e);
			}

			list.add(stats);
		}

		return list;
	}

	@Override
	public void shutdown() {
		isInitialized = false;
		// don't trash on shutdown
		dispose(false);
	}

	protected void dispose(boolean trashMe) {
		for (int db = 0; db < numberOfDbs; db++) {
			dispose(db, trashMe);
		}
	}

	protected void dispose(int db, boolean trashMe) {
		try {
			H22HikariPool pool = pools[db];
			pools[db] = null;
			if (pool != null) {
				pool.close();
				if(trashMe){
					new H22CacheCleanupThread(dbRoot, db, pool.database, 20*1000).run();
				}
			}
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
	}


	private Optional<H22HikariPool> getPool(final int dbNum) throws SQLException {
		return getPool(dbNum, false);
	}

	private final Semaphore building = new Semaphore(1, true);

	private Optional<H22HikariPool> getPool(final int dbNum, final boolean startup) throws SQLException {

		H22HikariPool source = pools[dbNum];
		if (source == null) {
			if (building.tryAcquire()) {
				Runnable creater = new Runnable() {
					@Override
					public void run() {
						try {
							Logger.info(H22Cache.class, "Initing H22 cache db:" + dbNum);
							if (startup) {
								pools[dbNum] = recoverLatestPool(dbNum);
							} else {
								pools[dbNum] = createPool(dbNum);
							}
						} catch (SQLException e) {
							Logger.error(H22Cache.class, e.getMessage(), e);
						} finally {
							building.release();
							errorCounter[dbNum] = 0;
						}
					}
				};
				creater.run();
			}
			return Optional.empty();
		}

		return Optional.of(source);

	}

	private H22HikariPool createPool(int dbNum) throws SQLException {
		Logger.info(this, "Building new H22 Cache, db:" + dbNum);
		// create pool
		H22HikariPool source = new H22HikariPool(dbRoot, dbNum);
		// create table
		createTables(source);
		return source;
	}

	private H22HikariPool recoverLatestPool(int dbNum) throws SQLException {
		H22HikariPool source = null;
		File dbs = new File(dbRoot + File.separator + dbNum);
		if (dbs.exists() && dbs.isDirectory()) {
			File[] files = dbs.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
			Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
			if (files.length > 0) {
				File myDb = files[0];
				if (files[0].isDirectory()) {
					files = myDb.listFiles();
					if (files.length > 0) {
						Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
						if (files[0].lastModified() + recoverOnRestart > System.currentTimeMillis()) {
							Logger.info(this, "Recovering H22 Cache, db:" + dbNum + ":" + myDb.getName());
							try {
								source = new H22HikariPool(dbRoot, dbNum, myDb.getName());
								createTables(source);
								pools[dbNum] = source;
							} catch (PoolInitializationException e) {
								Logger.warn(getClass(), "Failed to recover H2 Cache:" + e.getMessage());
							}

						}
					}
				}
			}
		}
		if(source==null){
			source = createPool(dbNum);
		}
		return source;
	}
	
	Optional<Connection> createConnection(boolean autoCommit, int dbnumber) throws SQLException {
		Optional<H22HikariPool> poolOpt = getPool(dbnumber);
		if (poolOpt.isPresent()) {
			Optional<Connection> opt = poolOpt.get().connection();
			if (opt.isPresent()) {
				if (autoCommit == false) {
					opt.get().setAutoCommit(autoCommit);
				}
			}
			return opt;
		}
		return Optional.empty();
	}

	private boolean doUpsert(final Fqn fqn, final Serializable obj) throws Exception {
		long start = System.nanoTime();
		long bytes = 0;
		boolean worked = false;
		if (fqn == null || exclude(fqn)) {
			return worked;
		}

		Optional<Connection> opt = createConnection(true, db(fqn));
		if (!opt.isPresent()) {
			return worked;
		}
		Connection c = opt.get();



		String upsertSQL = "MERGE INTO `" + TABLE_PREFIX + table(fqn) + "` key(cache_id) VALUES (?,?, ?)";

		PreparedStatement upsertStmt = null;
		try{
			upsertStmt = c.prepareStatement(upsertSQL);
			upsertStmt.setString(1, fqn.id);
			upsertStmt.setString(2, fqn.group);
			ObjectOutputStream output = null;
			OutputStream bout;
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			bout = new BufferedOutputStream(os, 8192);

			output = new ObjectOutputStream(bout);
			output.writeObject(obj);
			output.flush();
			byte[] data = os.toByteArray();
			bytes = data.length;
			upsertStmt.setBytes(3, data);

			worked = upsertStmt.execute();
			stats.group(fqn.group).writeSize(bytes * 8);
			stats.group(fqn.group).writeTime(System.nanoTime() - start);
		}
		finally{
			if(upsertStmt!=null)upsertStmt.close();
			c.close();
		}
			
		
		return worked;
	}

	private Object doSelect(Fqn fqn) throws Exception {
		if (fqn == null || exclude(fqn)) {
			return null;
		}

		ObjectInputStream input=null;
		InputStream bin=null;
		InputStream is=null;
		Optional<Connection> opt = createConnection(true, db(fqn));
		if (!opt.isPresent()) {
			return null;
		}
		Connection c = opt.get();

		PreparedStatement stmt = null;
		try {

			stmt = c.prepareStatement("select CACHE_DATA from `" + TABLE_PREFIX + table(fqn) + "` WHERE cache_id = ?");
			stmt.setString(1, fqn.id);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) {
				return null;
			}
			is = new ByteArrayInputStream(rs.getBytes(1));
			bin = new BufferedInputStream(is, 8192);

			input = new ObjectInputStream(bin);
			return input.readObject();

		} finally {

			if (stmt != null) stmt.close();
			c.close();
			if (input != null){
				try {
					input.close();
				} catch (IOException e) {
					Logger.warn(getClass(), "should not be here:" + e.getMessage(),e);
				}
			}
			if (bin != null){
				try {
					bin.close();
				} catch (IOException e) {
					Logger.warn(getClass(), "should not be here:" + e.getMessage(),e);
				}
			}
			
			if (is != null){
				try {
					is.close();
				} catch (IOException e) {
					Logger.warn(getClass(), "should not be here:" + e.getMessage(),e);
				}
			}
		}
	}

	private void doDelete(Fqn fqn) throws SQLException {
		if (fqn == null) {
			return;
		}

		Optional<Connection> opt = createConnection(true, db(fqn));
		if (!opt.isPresent()) {
			return;
		}
		Connection c = opt.get();
		PreparedStatement pstmt = null;
		try{
			String sql = "DELETE from " + TABLE_PREFIX + table(fqn) + " WHERE cache_id = ?";
			pstmt = c.prepareStatement(sql);
			pstmt.setString(1, fqn.id);
			pstmt.execute();
			pstmt.close();
			c.close();
			DONT_CACHE_ME.remove(fqn.id);
		}
		finally{
			pstmt.close();
			c.close();
		}
	}

	private void createTables(H22HikariPool source) throws SQLException {
		Connection c = null;
		int i = 0;
		while (!source.running) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new SQLException("Unable to get connection", e);
			}
			i++;
			if (i == 100) {
				throw new SQLException("Unable to get connection");
			}
		}
		Optional<Connection> opt = source.connection();
		c = opt.get();

		for (int table = 0; table < numberOfTablesPerDb; table++) {

			Statement s = c.createStatement();
			s.execute("CREATE CACHED TABLE IF NOT EXISTS `" + TABLE_PREFIX + table
					+ "` (cache_id bigint PRIMARY KEY,cache_group VARCHAR(255), CACHE_DATA BLOB)");
			s.close();
			s = c.createStatement();
			s.execute("CREATE INDEX IF NOT EXISTS `idx_" + TABLE_PREFIX + table + "_index_` on "
					+ TABLE_PREFIX + table + "(cache_group)");
		}
		c.close();
	}

	@Override
	public Set<String> getKeys(String groupName) {

		Set<String> keys = new HashSet<String>();
		int db = 0;
		Fqn fqn = new Fqn(groupName);
		try {
			for (db = 0; db < numberOfDbs; db++) {
				Optional<Connection> opt = createConnection(true, db);
				if (!opt.isPresent()) {
					continue;
				}
				Connection c = opt.get();
				try{
					for (int table = 0; table < numberOfTablesPerDb; table++) {
						PreparedStatement stmt = c.prepareStatement("select cache_id from " + TABLE_PREFIX + table + " where cache_group = ?");
						stmt.setString(1, fqn.group);
						stmt.setFetchSize(1000);
						ResultSet rs = stmt.executeQuery();
						while (rs.next()) {
							keys.add(rs.getString(1));
						}
						rs.close();
					}
				}
				finally{
					c.close();
				}
			}
		} catch (Exception ex) {
			handleError(ex, fqn);
		}

		return keys;
	}

	private void handleError(final Exception ex, final Fqn fqn) {
		// debug all errors
		Logger.debug(this.getClass(), ex.getMessage() + " on " + fqn, ex);
		int db = db(fqn);
		if (lastLog + limitErrorLogMillis < System.currentTimeMillis()) {
			lastLog = System.currentTimeMillis();
			Logger.warn(this.getClass(), "Error #" + errorCounter[db] + " " + ex.getMessage() + " on " + fqn, ex);
			
		}

		errorCounter[db]++;
		if (errorCounter[db] > recoverAfterErrors && recoverAfterErrors > 0) {
			errorCounter[db] = 0;
			Logger.error(getClass(), "Errors exceeded " + recoverAfterErrors + " rebuilding H22 Cache for db" + db);
			dispose(db, true);
		}

	}

	private String _getGroupCount(String groupName) throws SQLException {
		Fqn fqn = new Fqn(groupName);
		long ret = 0;
		for (int db = 0; db < numberOfDbs; db++) {
			Optional<Connection> opt = createConnection(true, db);
			if (!opt.isPresent()) {
				continue;
			}
			Connection c = opt.get();
			for (int table = 0; table < numberOfTablesPerDb; table++) {
				PreparedStatement stmt = c.prepareStatement("select count(*) from " + TABLE_PREFIX + table + " where cache_group = ?");
				stmt.setString(1, fqn.group);
				ResultSet rs = stmt.executeQuery();
				if (rs != null) {
					while (rs.next()) {
						ret = ret + rs.getInt(1);
					}
					rs.close();
					stmt.close();
				}
			}
			c.close();
		}
		return new Long(ret).toString();
	}

	private int db(Fqn fqn) {
		int hash = Math.abs(fqn.id.hashCode());
		return hash % numberOfDbs;
	}

	private int table(Fqn fqn) {
		int hash = Math.abs(fqn.id.hashCode());
		return hash % numberOfTablesPerDb;
	}

	/**
	 * Method that verifies if must exclude content that can not or must not be
	 * added to this h2 cache based on the given cache group and key
	 *
	 * @param group
	 * @param key
	 * @return
	 */
	private boolean exclude(Fqn fqn) {

		boolean exclude = DONT_CACHE_ME.containsKey(fqn.id);


		return exclude;
	}

}