package com.dotmarketing.business.cache.provider.h22;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;

import com.dotcms.repackage.com.google.common.cache.CacheStats;
import com.dotcms.repackage.org.apache.commons.collections.map.LRUMap;
import com.dotcms.repackage.org.jboss.cache.util.concurrent.ConcurrentHashSet;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class H22Cache extends CacheProvider {

	private static final long serialVersionUID = 4485667050052706116L;

	private Boolean isInitialized = false;

	final static String TABLE_PREFIX = "cach_table_";

	@SuppressWarnings("unchecked")
	private final static Map<Object, Object> DONT_CACHE_ME = Collections.synchronizedMap(new LRUMap(1000));

	// number of different dbs to shard against
	private final int numberOfDbs = Config.getIntProperty("cache.h22.number.of.dbs", 2);
	
	//number of tables in each db shard
	private final int numberOfTablesPerDb = Config.getIntProperty("cache.h22.number.of.tables.per.db", 5);

	// limit error message to every 5 seconds;
	private final int limitErrorLogMillis = Config.getIntProperty("cache.h22.limit.error.logging.per.milliseconds", 5000);
	
	// create a new cache store if our errors are greater that this.  Anything <0 will disable auto
	private final long recoverAfterErrors= Config.getIntProperty("cache.h22.recover.after.errors", 5000);
	
	private long lastLog = System.currentTimeMillis();
	private long errorCounter = 0;

	private final H22HikariPool[] pools = new H22HikariPool[numberOfDbs];
	private final Set<String> allowCacheRegions = new ConcurrentHashSet<String>();
	private final boolean allRegions;
	final String dbRoot;
	final private H2GroupStatsList stats = new H2GroupStatsList();


	public H22Cache(final String dbRoot, boolean allRegions) {
		this.dbRoot = dbRoot;
		this.allRegions = allRegions;
	}

	public H22Cache(final String dbRoot) {
		this(ConfigUtils.getDynamicContentPath(), Config.getBooleanProperty("cache.default.h22", false));
	}

	public H22Cache() {
		this(ConfigUtils.getDynamicContentPath());
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

		Iterator<String> it = Config.getKeys();
		while (it.hasNext()) {
			String key = it.next();
			if (key == null || !key.startsWith("cache.") || !key.endsWith(".h22")) {
				continue;
			}
			boolean useDisk = Config.getBooleanProperty(key, allRegions);

			if (useDisk) {
				String cacheName = key.split("\\.")[1];
				allowCacheRegions.add(cacheName);
				Logger.info(this.getClass(), "H22 Cache for region: " + cacheName);
			}
		}

		for (int i = 0; i < numberOfDbs; i++) {
			getPool(i);
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

		} catch (Exception e) {
			handleError(e, fqn);
		}
		stats.group(fqn.group).hitOrMiss(foundObject);
		stats.group(fqn.group).readTime(System.nanoTime() - start);

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
		Fqn fqn =new Fqn(group, key);
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

	@Override
	public void removeAll() {

		Logger.info(this, "Starting Full Cache Flush in h22");
		dispose();
		stats.clear();
		DONT_CACHE_ME.clear();

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
			groupStats.compute();
			CacheStats googleStats = new CacheStats(groupStats.hits, groupStats.misses, groupStats.reads, 0, groupStats.avgReadTime, 0);

			Map<String, Object> stats = new HashMap<>();
			stats.put("CacheStats", googleStats);
			stats.put("name", getName());
			stats.put("key", getKey());
			stats.put("region", group);
			stats.put("toDisk", true);
			stats.put("entrySize", groupStats.avgEntrySize);
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
		dispose();

	}

	protected void dispose() {
		for (int x = 0; x < numberOfDbs; x++) {

			try {
				H22HikariPool pool = pools[x];
				pools[x] = null;
				if (pool != null) {
					pool.dispose();
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), e.getMessage(), e);
			}
		}
	}
	private final Semaphore available = new Semaphore(1, true);
	private Optional<H22HikariPool> getPool(final int dbNum) throws SQLException {

		H22HikariPool source = pools[dbNum];

		
		if (source == null) {
			if(available.tryAcquire()){
				Runnable creater = new Runnable() {
					Exception e = null;
					@Override
					public void run() {
						try {
							pools[dbNum] = createPool(dbNum);
						} catch (SQLException e) {
							Logger.error(H22Cache.class, e.getMessage(),e);
						}
						finally{
							available.release();
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
		Logger.info(this, "Starting H22 Cache, db:" + dbNum);
		// create pool
		H22HikariPool source = new H22HikariPool(dbRoot, dbNum);
		// create table
		createTables(source);
		
		pools[dbNum] = source;
		return pools[dbNum];

	}

	Optional<Connection> createConnection(boolean autoCommit, int dbnumber) throws SQLException {
		
		Optional<H22HikariPool> poolOpt = getPool(dbnumber);
		if(poolOpt.isPresent()){
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
		if (fqn == null || exclude(fqn)) {
			return false;
		}

		Optional<Connection> opt = createConnection(true, db(fqn));
		if (!opt.isPresent()) {
			return false;
		}
		Connection c = opt.get();
		PreparedStatement upsertStmt = null;
		try {

			String upsertSQL = "MERGE INTO `" + TABLE_PREFIX + table(fqn) + "` key(cache_id) VALUES (?,?, ?)";

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

			boolean test = upsertStmt.execute();

		} catch (StreamCorruptedException | RuntimeException e) {
			handleError(e, fqn);
			try {

				DONT_CACHE_ME.put(fqn.id, fqn.toString());
			} catch (Exception e1) {
				handleError(e1, fqn);
			}
			return false;
		} finally {
			if (upsertStmt != null) {
				upsertStmt.close();
			}
			c.close();
			stats.group(fqn.group).writeSize(bytes * 8);
			stats.group(fqn.group).writeTime(System.nanoTime() - start);
		}
		return true;
	}

	private Object doSelect(Fqn fqn) throws SQLException {
		if (fqn == null || exclude(fqn)) {
			return null;
		}

		ObjectInputStream input;
		InputStream bin;
		InputStream is;
		Optional<Connection> opt = createConnection(true, db(fqn));
		if (!opt.isPresent()) {
			return null;
		}
		Connection c = opt.get();

		int tableNumber = table(fqn);
		PreparedStatement stmt = null;
		try {

			stmt = c.prepareStatement("select CACHE_DATA from `" + TABLE_PREFIX + tableNumber + "` WHERE cache_id = ?");
			stmt.setString(1, fqn.id);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) {
				return null;
			}
			is = new ByteArrayInputStream(rs.getBytes(1));
			bin = new BufferedInputStream(is, 8192);

			input = new ObjectInputStream(bin);
			return input.readObject();

		} catch (RuntimeException | IOException | ClassNotFoundException e) {
			handleError(e, fqn);
			try {
				doDelete(fqn);
				DONT_CACHE_ME.put(fqn.id, fqn.toString());
			} catch (Exception e1) {
				handleError(e1, fqn);
			}
			return null;

		} finally {
			if (stmt != null) {
				stmt.close();
			}
			c.close();

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
		String sql = "DELETE from " + TABLE_PREFIX + table(fqn) + " WHERE cache_id = ?";
		PreparedStatement pstmt = c.prepareStatement(sql);
		pstmt.setString(1, fqn.id);
		pstmt.execute();
		pstmt.close();
		c.close();
		DONT_CACHE_ME.remove(fqn.id);
	}

	private void setCompression(int db) throws SQLException {
		Connection conn;
		if (Config.getBooleanProperty("USE_CACHE_COMPRESSION", false)) {
			Optional<Connection> opt = createConnection(true, db);
			if (!opt.isPresent()) {
				return;
			}
			conn = opt.get();
			Statement s = conn.createStatement();
			s.execute("SET COMPRESS_LOB LZF");
			s.close();
			conn.close();
		}
	}

	private void createTables(H22HikariPool source) throws SQLException {
		Connection conn = null;
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
		conn = opt.get();

		for (int table = 0; table < numberOfTablesPerDb; table++) {

			Statement s = conn.createStatement();
			s.execute("CREATE CACHED TABLE IF NOT EXISTS `" + TABLE_PREFIX + table
					+ "` (cache_id bigint PRIMARY KEY,cache_group VARCHAR(255), CACHE_DATA BLOB)");
			s.close();
			s = conn.createStatement();
			s.execute("CREATE INDEX `idx_" + TABLE_PREFIX + table + "_index_" + System.currentTimeMillis() + "` on " + TABLE_PREFIX + table
					+ "(cache_group)");

		}
		conn.close();
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
				c.close();
			}
		} catch (Exception ex) {
			handleError(ex, fqn);
		}

		return keys;
	}

	private void handleError(final Exception ex, final Fqn fqn) {
		// debug all errors
		Logger.debug(this.getClass(), ex.getMessage() + " on " + fqn , ex);
		
		if(lastLog + limitErrorLogMillis <  System.currentTimeMillis()){
			lastLog=System.currentTimeMillis();
			Logger.warn(this.getClass(), ex.getMessage() + " on " + fqn , ex);
		}
		errorCounter++;
		if(errorCounter>recoverAfterErrors&&recoverAfterErrors>0){
			Logger.error(getClass(), "Errors exceeded " + recoverAfterErrors + " rebuilding H22 Cache");
			dispose();
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
				stmt.setString(1,fqn.group);
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

		boolean exclude = false;

		if (!allowCacheRegions.contains(fqn.group) && !allRegions) {
			exclude = true;
		}
		if (!exclude) {
			exclude = DONT_CACHE_ME.containsKey(fqn.id);

		}

		return exclude;
	}

}