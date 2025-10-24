package com.dotmarketing.business.cache.provider.h22;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dotcms.cache.CacheValue;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

public class H22Cache extends CacheProvider {


    private static final long serialVersionUID = 1L;
    final int numberOfAsyncThreads=Config.getIntProperty("cache_h22_async_threads", 5);
    final int asyncTaskQueueSize = Config.getIntProperty("cache_h22_async_task_queue", 10000);
	final float threadAllocationTolerance = Config.getFloatProperty("cache_h22_async_tolerance",0.98F);
    final boolean shouldAsync=Config.getBooleanProperty("cache_h22_async", true);


    final ThreadFactory namedThreadFactory =  new ThreadFactoryBuilder().setDaemon(true).setNameFormat("H22-ASYNC-COMMIT-%d").build();
    final private LinkedBlockingQueue<Runnable> asyncTaskQueue = new LinkedBlockingQueue<>();
    final private ExecutorService executorService = new ThreadPoolExecutor(numberOfAsyncThreads, numberOfAsyncThreads, 10, TimeUnit.SECONDS, asyncTaskQueue ,namedThreadFactory);


	private AtomicBoolean isInitialized = new AtomicBoolean(false);

	final static String TABLE_PREFIX = "cach_table_";


	private final static Cache<String, String> DONT_CACHE_ME = Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(20, TimeUnit.SECONDS)
                    .build();

	// number of different dbs to shard against
	private final int numberOfDbs = Config.getIntProperty("cache.h22.number.of.dbs", 2);

	// number of tables in each db shard
	private final int numberOfTablesPerDb = Config.getIntProperty("cache.h22.number.of.tables.per.db", 9);

	// limit error message to every 5 seconds;
	private final int limitErrorLogMillis = Config.getIntProperty("cache.h22.limit.one.error.log.per.milliseconds", 5000);

	// create a new cache store if our errors are greater that this. Anything <1
	// will disable auto recover
	private final long recoverAfterErrors = Config.getIntProperty("cache.h22.recover.after.errors", 5000);

	// try to recover with h2 if within this time (30m default)
	private final long recoverOnRestart = Config.getIntProperty("cache.h22.recover.if.restarted.in.milliseconds", 0);
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
    public boolean isDistributed() {
    	return false;
    }

	@Override
	public void init() throws Exception {

		if (isInitialized.compareAndSet(false, true)) {
			// init the databases
			for (int i = 0; i < numberOfDbs; i++) {
				getPool(i, true);
			}
		}

	}

	@Override
	public boolean isInitialized() throws Exception {
		return isInitialized.get();
	}

	@Override
	public void put(final String group, final String key, final Object content) {
		// Don't accept new cache operations during shutdown
		if (!isInitialized.get() || com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
			return;
		}
		// Building the key
		final Fqn fqn = new Fqn(group, key);
        if(exclude(fqn)) {
            return;
        }
		DONT_CACHE_ME.put(fqn.id, fqn.toString());
		if(shouldAsync()) {
		    putAsync(fqn, content);
		    return;
		}
		
		
		try {
			// Add the given content to the group and for a given key
		    doUpsert(fqn, (Serializable) content);
		} catch (Exception e) {
			handleError(e, fqn);
		}

	}


    void putAsync(final Fqn fqn, final Object content) {

        executorService.submit(()-> {
            try {
                // Add the given content to the group and for a given key
                doUpsert(fqn, (Serializable) content);
            } catch (Exception e) {
                handleError(e, fqn);
            }
         });
    }
	
	
	
	
	@Override
	public Object get(String group, String key) {
		// Don't accept new cache operations during shutdown
		if (!isInitialized.get() || com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
			return null;
		}


		Object foundObject = null;
		long start = System.nanoTime();
		final Fqn fqn = new Fqn(group, key);
		
		try {
			// Get the content from the group and for a given key;
			foundObject = doSelect(fqn);

			if (foundObject instanceof CacheValue && ((CacheValue) foundObject).isExpired()) {
				removeAsync(fqn);
				foundObject = null;
			}

			stats.group(fqn.group).hitOrMiss(foundObject);
			stats.group(fqn.group).readTime(System.nanoTime() - start);
		} catch (Exception e) {
			foundObject=null;
			handleError(e, fqn);
		}
		
		

		return foundObject;
	}

	@Override
	public void remove(final String groupName) {

		final Fqn fqn = new Fqn(groupName);

		Logger.info(this.getClass(), "Flushing H22 cache group:" + fqn + " Note: this can be an expensive operation");

		try {
			for (int db = 0; db < numberOfDbs; db++) {
				
				for (int table = 0; table < numberOfTablesPerDb; table++) {

					 final Optional<Connection> opt = createConnection(true, db); 
					 
					if (opt.isEmpty()) {
					    throw new SQLException("Unable to get connection when trying to remove groups " + groupName + " in H22Cache");
					}

					try (Connection connection = opt.get();PreparedStatement stmt = connection.prepareStatement("DELETE from " + TABLE_PREFIX + table + " WHERE cache_group = ?")){
					    Logger.debug(this, "connection.getAutoCommit():" + connection.getAutoCommit());
					    stmt.setString(1, fqn.group);
					    stmt.executeUpdate();
					} 
				}
			}
		} catch (SQLException e) {

			handleError(e, fqn);
		}
	}

	@Override
	public void remove(final String group, final String key) {
        if (!UtilMethods.isSet(key)) {
            Logger.warn(this, "Empty key passed in, clearing group " + group + " by mistake");
        }
        
		final Fqn fqn = new Fqn(group, key);
        DONT_CACHE_ME.put(fqn.id, fqn.toString());
        if(shouldAsync()) {
            removeAsync(fqn);
            return;
        }
        
        
        try {
            // Invalidates from Cache a key from a given group
            doDelete(fqn);
        } catch (Exception e) {
            handleError(e, fqn);
        }
        
		
	}

	/**
	 * Calculates the thread allocation % for a given queue size.
	 * Then determines if that allocation % exceeds or not a tolerance.
	 * @return
	 */
	boolean isAllocationWithinTolerance() {
		final int size = asyncTaskQueue.size();
		final float allocation = (float) size / (float) asyncTaskQueueSize;
		Logger.debug(H22Cache.class,
				() -> " size is " + size + ", allocation is " + allocation + ", tolerance is :"
						+ threadAllocationTolerance);
		return allocation < threadAllocationTolerance;
	}

    /**
     * returns true if async set to true and the task queue is < than a given tolerance % full.
     *
     * @return
     */
    boolean shouldAsync() {
		   return shouldAsync && isAllocationWithinTolerance();
	}

    void removeAsync(final Fqn fqn) {

        executorService.submit(()-> {
            try {
                // Invalidates from Cache a key from a given group
                doDelete(fqn);
            } catch (Exception e) {
                handleError(e, fqn);
            }
        });
    }
	
	
	
	public void doTruncateTables() throws SQLException {

			for (int db = 0; db < numberOfDbs; db++) {
				Optional<H22HikariPool> poolOpt = getPool(db);
				if(poolOpt.isEmpty())continue;
				H22HikariPool pool = poolOpt.get();
				Optional<Connection> connOpt = pool.connection();
				if(connOpt.isEmpty())continue;
				
				try(Connection c = connOpt.get()){
					pool.stop();
					for (int table = 0; table < numberOfTablesPerDb; table++) {
						Statement stmt = c.createStatement();
						stmt.execute("truncate table " + TABLE_PREFIX + table);
						stmt.close();
					}
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
		if(Config.getBooleanProperty("cache.h22.rebuild.on.removeAll", true) || failedFlushAlls==failedThreshold){
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
		

		stats.clear();
		DONT_CACHE_ME.invalidateAll();
		long end = System.nanoTime();
		Logger.info(this, "End Full Cache Flush in h22 : " + TimeUnit.MILLISECONDS.convert(end-start, TimeUnit.NANOSECONDS)+ "ms");

	}

	@Override
	public Set<String> getGroups() {

		Set<String> groups = new HashSet<>();
		try {
			for (int db = 0; db < numberOfDbs; db++) {
				Optional<Connection> opt = createConnection(true, db);
				if (opt.isEmpty()) {
					continue;
				}
				try(Connection c = opt.get()){
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
				}
			}
		} catch (SQLException e) {
			Logger.warn(this.getClass(), "cannot get groups : " + e.getMessage());
		}

		return groups;
	}

    @Override
    public CacheProviderStats getStats() {
        CacheStats providerStats = new CacheStats();
        CacheProviderStats ret = new CacheProviderStats(providerStats,getName());
        Set<String> currentGroups = new HashSet<>();
        currentGroups.addAll(getGroups());

        
        for (String group : currentGroups) {
            H22GroupStats groupStats = stats.group(group);
            long perObject = (groupStats.writes==0) ? 0 : groupStats.totalSize/groupStats.writes;
            CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, group);
            stats.addStat(CacheStats.REGION_MEM_TOTAL_PRETTY, UtilMethods.prettyByteify(groupStats.totalSize ));
            stats.addStat(CacheStats.REGION_MEM_PER_OBJECT, UtilMethods.prettyByteify(perObject ));
            
            try {
              stats.addStat(CacheStats.REGION_SIZE,  _getGroupCount(group));
            } catch (SQLException e) {
                Logger.warn(this, "can't get h22 group data for: " + group, e);
            }
            ret.addStatRecord(stats);
        }
        return ret;
    }

	@Override
	public void shutdown() {
		isInitialized.set(false);
		// don't trash on shutdown - just close existing pools without creating new ones
		shutdownPools();
	}

	/**
	 * Shutdown existing pools without creating new ones (for clean shutdown)
	 */
	private void shutdownPools() {
		for (int db = 0; db < numberOfDbs; db++) {
			try {
				final H22HikariPool pool = pools[db];
				if (pool != null) {
					pool.close();
					pools[db] = null;
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), "Error closing H22 cache pool " + db + ": " + e.getMessage(), e);
			}
		}
		
		// Shutdown the async executor service
		try {
			executorService.shutdown();
			if (!executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	protected void dispose(boolean trashMe) {
		for (int db = 0; db < numberOfDbs; db++) {
			dispose(db, trashMe);
		}
	}

	protected void dispose(int db, boolean trashMe) {
		try {
			final H22HikariPool oldPool = pools[db];
			pools[db] = createPool(db);
			if (oldPool != null) {
			    oldPool.close();
				if(trashMe){
					new H22CacheCleanupThread(dbRoot, db, oldPool.database, 20*1000).start();
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
		// Don't create new pools during shutdown
		if (!isInitialized.get() && com.dotcms.shutdown.ShutdownCoordinator.isShutdownStarted()) {
			return Optional.empty();
		}

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
		if (fqn == null) {
			return worked;
		}

		Optional<Connection> opt = createConnection(true, db(fqn));
		if (opt.isEmpty()) {
			return worked;
		}
		
		try(Connection c = opt.get();
		                ByteArrayOutputStream os = new ByteArrayOutputStream();
		                ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(os, 8192)); ){

		    String upsertSQL = "MERGE INTO `" + TABLE_PREFIX + table(fqn) + "` key(cache_id) VALUES (?,?, ?)";

		    try(PreparedStatement upsertStmt = c.prepareStatement(upsertSQL)){
    			upsertStmt.setString(1, fqn.id);
    			upsertStmt.setString(2, fqn.group);
    
    			output.writeObject(obj);
    			output.flush();
    			byte[] data = os.toByteArray();
    			bytes = data.length;
    			upsertStmt.setBytes(3, data);
    
    			worked = upsertStmt.execute();
		    }
			stats.group(fqn.group).writes++;
			stats.group(fqn.group).writeSize(bytes * 8);
			stats.group(fqn.group).writeTime(System.nanoTime() - start);
			DONT_CACHE_ME.invalidate(fqn.id);
		}
		
			
		
		return worked;
	}

	private Object doSelect(Fqn fqn) throws Exception {
		if (fqn == null || exclude(fqn)) {
			return null;
		}


		Optional<Connection> opt = createConnection(true, db(fqn));
		if (opt.isEmpty()) {
			return null;
		}
        try(Connection c = opt.get();
            PreparedStatement stmt = c.prepareStatement("select CACHE_DATA from `" + TABLE_PREFIX + table(fqn) + "` WHERE cache_id = ?");){
			stmt.setString(1, fqn.id);
			try(ResultSet rs = stmt.executeQuery()){
    			if (!rs.next()) {
    				return null;
    			}
    			
    			try  (final InputStream bin=new BufferedInputStream(new ByteArrayInputStream(rs.getBytes(1)), 8192);
                       final ObjectInputStream input=new ObjectInputStream(bin);){
    
    			    return input.readObject();
    			}
			}


		}
	}

	private void doDelete(Fqn fqn) throws SQLException {
		if (fqn == null) {
			return;
		}
		String sql = "DELETE from " + TABLE_PREFIX + table(fqn) + " WHERE cache_id = ?";
		Optional<Connection> opt = createConnection(true, db(fqn));
		if (opt.isEmpty()) {
			return;
		}
		try(Connection c = opt.get(); PreparedStatement pstmt = c.prepareStatement(sql)){
			pstmt.setString(1, fqn.id);
			pstmt.execute();
			DONT_CACHE_ME.invalidate(fqn.id);
		}
		
	}

	private void createTables(H22HikariPool source) throws SQLException {

		int i = 0;
		while (!source.running()) {
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
		try(Connection c = opt.get()){

    		for (int table = 0; table < numberOfTablesPerDb; table++) {
    
    			Statement s = c.createStatement();
    			s.execute("CREATE CACHED TABLE IF NOT EXISTS `" + TABLE_PREFIX + table
    					+ "` (cache_id bigint PRIMARY KEY,cache_group VARCHAR(255), CACHE_DATA BLOB)");
    			s.close();
    			s = c.createStatement();
    			s.execute("CREATE INDEX IF NOT EXISTS `idx_" + TABLE_PREFIX + table + "_index_` on "
    					+ TABLE_PREFIX + table + "(cache_group)");
    			s.close();
    		}
		}
	}

	@Override
	public Set<String> getKeys(String groupName) {

		Set<String> keys = new HashSet<>();
		int db = 0;
		Fqn fqn = new Fqn(groupName);
		try {
			for (db = 0; db < numberOfDbs; db++) {
				Optional<Connection> opt = createConnection(true, db);
				if (opt.isEmpty()) {
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
	    DONT_CACHE_ME.put(fqn.id, fqn.toString());
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
			if (opt.isEmpty()) {
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
		return Long.valueOf(ret).toString();
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
	 * @return
	 */
	private boolean exclude(Fqn fqn) {

	    return DONT_CACHE_ME.getIfPresent(fqn.id)!=null;

	}

}
