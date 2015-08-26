/**
 *
 */
package com.dotmarketing.business;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.repackage.com.google.common.cache.Cache;
import com.dotcms.repackage.com.google.common.cache.CacheBuilder;
import com.dotcms.repackage.com.google.common.cache.RemovalListener;
import com.dotcms.repackage.com.google.common.cache.RemovalNotification;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Collections;
import com.dotcms.repackage.org.jboss.cache.Fqn;
import com.dotmarketing.business.cache.CacheTransport;
import com.dotmarketing.business.cache.CacheTransportException;
import com.dotmarketing.cache.H2CacheLoader;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Guava cache administrator uses Google's Guave code
 * under the covers and gets it's startup params from the dotmarketing-config.properties
 * on a put where the non legacy one will not.
 *
 * @author Jason Tesser
 * @version 1.6.5
 *
 */
public class DotGuavaCacheAdministratorImpl implements DotCacheAdministrator {

	CacheTransport cacheTransport;

	private DistributedJournalAPI journalAPI;
	private final ConcurrentHashMap<String, Cache<String, Object>> groups = new ConcurrentHashMap<>();
	private boolean useTransportChannel = false;
	private final ConcurrentHashMap<String, Boolean> cacheToDisk = new ConcurrentHashMap<>();
	private final HashSet<String> availableCaches = new HashSet<>();
	private H2CacheLoader diskCache = null;

	static final String LIVE_CACHE_PREFIX = "livecache";
	static final String WORKING_CACHE_PREFIX = "workingcache";
	static final String DEFAULT_CACHE = "default";
	public static final String TEST_MESSAGE = "HELLO CLUSTER!";
	public static final String TEST_MESSAGE_NODE = "TESTNODE";
	public static final String VALIDATE_CACHE = "validateCacheInCluster-";
	public static final String VALIDATE_CACHE_RESPONSE = "validateCacheInCluster-response-";
	public static final String VALIDATE_SEPARATOR = "_";
	public static final String DUMMY_TEXT_TO_SEND = "DUMMY MSG TO TEST SEND";
	
	private NullCallable nullCallable = new NullCallable();

	private boolean isDiskCache(String group){
		if(group ==null || diskCache==null){
			return false;
		}
		group = group.toLowerCase();
		Boolean ret = cacheToDisk.get(group);
		if(ret == null) {
			if(Config.containsProperty("cache." + group + ".disk")){
				ret = Config.getBooleanProperty("cache." + group + ".disk", false);
			}
			else if(group.startsWith(LIVE_CACHE_PREFIX) && Config.containsProperty("cache." + LIVE_CACHE_PREFIX + ".disk")){
				ret = Config.getBooleanProperty("cache."+LIVE_CACHE_PREFIX+".disk", false);
			}
			else if(group.startsWith(WORKING_CACHE_PREFIX) && Config.containsProperty("cache." + WORKING_CACHE_PREFIX + ".disk")){
				ret = Config.getBooleanProperty("cache." + WORKING_CACHE_PREFIX + ".disk", false);
			}
			/*
			else if (group.startsWith("velocitymenucache")) {
				ret = false;
			}

			else if (group.startsWith("velocitycache")) {
				 ret = false;
			}
			*/
			else{
				ret = Config.getBooleanProperty("cache.default.disk", false);
			}
			cacheToDisk.put(group, ret);
		}

		return ret;

	}

	public CacheTransport getTransport () {
		return cacheTransport;
	}

	public void setTransport ( CacheTransport transport ) {

		if ( getTransport() != null ) {
			getTransport().shutdown();
		}

		this.cacheTransport = transport;
	}

	public DotGuavaCacheAdministratorImpl () {
		this(null);
	}

	public DotGuavaCacheAdministratorImpl ( CacheTransport transport ) {

		if ( transport != null ) {
			useTransportChannel = true;
			this.cacheTransport = transport;
		} else {
			useTransportChannel = false;
		}

		journalAPI = APILocator.getDistributedJournalAPI();

		boolean initDiskCache = false;
		Iterator<String> it = Config.getKeys();
		availableCaches.add(DEFAULT_CACHE);
		while(it.hasNext()){
			String key = it.next();
			if(key ==null){
				continue;
			}
			if(key.startsWith("cache.")){
				String cacheName = key.split("\\.")[1];
				if(key.endsWith(".size")){
					int inMemory = Config.getIntProperty(key, 0);
					availableCaches.add(cacheName.toLowerCase());
					Logger.info(this.getClass(), "***\t Cache Config Memory : " +  cacheName + ": " + inMemory  );
				}
				if(key.endsWith(".disk")){
					boolean useDisk = Config.getBooleanProperty(key, false);
					if(useDisk){
						initDiskCache =true;
						Logger.info(this.getClass(), "***\t Cache Config Disk   : " + cacheName  + ": true");
					}
				}

			}
		}
		if(initDiskCache){
			try{
				diskCache = H2CacheLoader.getInstance();
			}
			catch(Exception e){
				Logger.error(this.getClass(), "***\t Unable to start disk cache: " + e.getMessage(), e);
				cacheToDisk.clear();
			}
		}
	}

	public void setCluster(Server localServer) throws Exception {
		setCluster(null, localServer);
	}

	public void setCluster(Map<String, String> cacheProperties, Server localServer) throws Exception {

			Logger.info(this, "***\t Starting JGroups Cluster Setup");

			journalAPI = APILocator.getDistributedJournalAPI();

			if(cacheProperties==null) {
				cacheProperties = new HashMap<>();
			}
			
			ServerAPI serverAPI = APILocator.getServerAPI();
			
			String cacheProtocol, bindAddr, bindPort, cacheTCPInitialHosts, mCastAddr, mCastPort, preferIPv4;
			if(Config.getBooleanProperty("CLUSTER_AUTOWIRE",true)) {
			    Logger.info(this, "Using automatic port placement as CLUSTER_AUTOWIRE is ON");
			    
			    cacheProtocol = UtilMethods.isSet(cacheProperties.get("CACHE_PROTOCOL"))?cacheProperties.get("CACHE_PROTOCOL")
	                    :Config.getStringProperty("CACHE_PROTOCOL", "tcp");
			    
			    String storedBindAddr = (UtilMethods.isSet(localServer.getHost()) && !localServer.getHost().equals("localhost"))
	                    ?localServer.getHost():localServer.getIpAddress();
	            bindAddr = UtilMethods.isSet(cacheProperties.get("BIND_ADDRESS"))?cacheProperties.get("BIND_ADDRESS")
	                    :Config.getStringProperty("CACHE_BINDADDRESS", storedBindAddr );
	            
	            bindPort = UtilMethods.isSet(cacheProperties.get("CACHE_BINDPORT"))?cacheProperties.get("CACHE_BINDPORT")
	                    :localServer!=null&&UtilMethods.isSet(localServer.getCachePort())?Long.toString(localServer.getCachePort())
	                    :ClusterFactory.getNextAvailablePort(localServer.getServerId(), ServerPort.CACHE_PORT);
	                    
                localServer.setCachePort(Integer.parseInt(bindPort));

                localServer.setHost(Config.getStringProperty("CACHE_BINDADDRESS", null));                

                List<String> myself = new ArrayList<String>();
                myself.add(localServer.getServerId());

                List<Server> aliveServers = serverAPI.getAliveServers(myself);
                aliveServers.add(localServer);

                StringBuilder initialHosts = new StringBuilder();

                int i=0;
                for (Server server : aliveServers) {
                    if(i>0) {
                        initialHosts.append(",");
                    }

                    if(UtilMethods.isSet(server.getHost()) && !server.getHost().equals("localhost")) {
                        initialHosts.append(server.getHost()).append("[").append(server.getCachePort()).append("]");
                    } else {
                        initialHosts.append(server.getIpAddress()).append("[").append(server.getCachePort()).append("]");
                    }
                    i++;
                }

                if(initialHosts.length()==0) {
                    if(bindAddr.equals("localhost")) {
                        initialHosts.append(localServer.getIpAddress()).append("[").append(bindPort).append("]");
                    } else {
                        initialHosts.append(bindAddr).append("[").append(bindPort).append("]");
                    }
                }

                cacheTCPInitialHosts = UtilMethods.isSet(cacheProperties.get("CACHE_TCP_INITIAL_HOSTS"))?cacheProperties.get("CACHE_TCP_INITIAL_HOSTS")
                        :Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", initialHosts.toString());

                mCastAddr = UtilMethods.isSet(cacheProperties.get("CACHE_MULTICAST_ADDRESS"))?cacheProperties.get("CACHE_MULTICAST_ADDRESS")
                        :Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10");
                mCastPort = UtilMethods.isSet(cacheProperties.get("CACHE_MULTICAST_PORT"))?cacheProperties.get("CACHE_MULTICAST_PORT")
                        :Config.getStringProperty("CACHE_MULTICAST_PORT", "45588");
                preferIPv4 = UtilMethods.isSet(cacheProperties.get("CACHE_FORCE_IPV4"))?cacheProperties.get("CACHE_FORCE_IPV4")
                        :Config.getStringProperty("CACHE_FORCE_IPV4", "true");
			}
			else {
			    Logger.info(this, "Using manual port placement as CLUSTER_AUTOWIRE is OFF");
			    
			    cacheProtocol = Config.getStringProperty("CACHE_PROTOCOL", "tcp");
			    bindAddr = Config.getStringProperty("CACHE_BINDADDRESS", null);
			    bindPort = Config.getStringProperty("CACHE_BINDPORT", null);
			    cacheTCPInitialHosts = Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", "localhost[7800]");
			    mCastAddr = Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10");
			    mCastPort = Config.getStringProperty("CACHE_MULTICAST_PORT", "45588");
			    preferIPv4 = Config.getStringProperty("CACHE_FORCE_IPV4", "true");
			}

			if (UtilMethods.isSet(bindAddr)) {
			    Logger.info(this, "***\t Using " + bindAddr + " as the bindaddress");
				System.setProperty("jgroups.bind_addr", bindAddr);
			}
			else {
                Logger.info(this, "***\t bindaddress is not set");
            } 

			if (UtilMethods.isSet(bindPort)) {
			    Logger.info(this, "***\t Using " + bindPort + " as the bindport");
				System.setProperty("jgroups.bind_port", bindPort);
			}
			else {
                Logger.info(this, "***\t bindport is not set");
            }
			
			if (cacheProtocol.equals("tcp")) {
				Logger.info(this, "***\t Setting up TCP initial hosts: "+cacheTCPInitialHosts);
				System.setProperty("jgroups.tcpping.initial_hosts",	cacheTCPInitialHosts);
			} else if (cacheProtocol.equals("udp")) {
				Logger.info(this, "***\t Setting up UDP address and port: "+mCastAddr+":"+mCastPort);
				System.setProperty("jgroups.udp.mcast_port", mCastPort);
				System.setProperty("jgroups.udp.mcast_addr", mCastAddr);
			} else {
				Logger.info(this, "Not Setting up any Properties as no protocal was found");
			}

			Logger.info(this, "***\t Prefer IPv4: "+(preferIPv4.equals("true") ? "enabled" : "disabled"));
			System.setProperty("java.net.preferIPv4Stack", preferIPv4);

		if ( getTransport() != null ) {
			getTransport().init(localServer, cacheProperties);
			useTransportChannel = true;
		} else {
			throw new CacheTransportException("No Cache transport implementation is defined");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.dotmarketing.business.DotCacheAdministrator#flushAll()
	 */
	public void flushAll() {
		flushAlLocalOnlyl();
		try {
			if (Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)) {
				journalAPI.addCacheEntry("0", ROOT_GOUP);
			} else if ( useTransportChannel ) {

				if ( getTransport() != null ) {
					try {
						getTransport().send("0:" + ROOT_GOUP);
					} catch ( Exception e ) {
						Logger.error(DotGuavaCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
					}
				} else {
					throw new CacheTransportException("No Cache transport implementation is defined");
				}

			}
		} catch (DotDataException e) {
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.dotmarketing.business.DotCacheAdministrator#flushGroup(java.lang.
	 * String)
	 */

	public void flushGroup(String group) {

		if(group ==null ){
			return ;
		}
		group = group.toLowerCase();

		groups.remove(group);

		flushGroupLocalOnly(group);

		try {
			if (Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)) {
				journalAPI.addCacheEntry("0", group);
			} else if ( useTransportChannel ) {

				try {
					cacheTransport.send("0:" + group);
				} catch (Exception e) {
					Logger.error(DotGuavaCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
				}

			}
		} catch (DotDataException e) {
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.dotmarketing.business.DotCacheAdministrator#flushAll()
	 */
	public void flushAlLocalOnlyl() {

		Set<String> myGroups = new HashSet<String>();

		myGroups.addAll(groups.keySet());
		groups.clear();
		if(diskCache != null){
			try {
				myGroups.addAll(H2CacheLoader.getGroups());
			} catch (SQLException e) {
				Logger.error(DotGuavaCacheAdministratorImpl.class,e.getMessage(),e);
			}
		}

		for(String group : myGroups){
			flushGroupLocalOnly(group);
		}
		if(diskCache != null){
			diskCache.resetCannotCacheCache();
		}
		cacheToDisk.clear();

	}

	public void flushGroupLocalOnly(String group) {

		if(group ==null ){
			return ;
		}
		group = group.toLowerCase();

		Cache cache = getCache(group);


		if(isDiskCache(group)){
			try {
				diskCache.remove(new Fqn(group));
			} catch (Exception e) {
				Logger.debug(DotGuavaCacheAdministratorImpl.class,e.getMessage(),e);
			}
		}
		cache.invalidateAll();

	}


	private class NullCallable implements Callable{

		public Object call() throws Exception {
			return null;
		}


	}

	public Object getMemory(String key, String group) throws DotCacheException {
		if(key == null || group == null){
			return null;
		}
		key = key.toLowerCase();
		group = group.toLowerCase();
		Cache cache = getCache(group);
		Object j = null;
		try {
			j = cache.get(key, nullCallable);
		} catch (Exception e) {

		}
		return j;
	}

	public Object getDisk(String key, String group) throws DotCacheException {
		if(key == null || group == null){
			return null;
		}

		key = key.toLowerCase();
		group = group.toLowerCase();
		Object j = null;
		if(isDiskCache(group)){
			try {
				Map m=diskCache.get(new Fqn(group, key));
				if(m!=null){
					j = m.get(key);
					if(j != null){
						Cache cache = getCache(group);
						cache.put(key, j);
					}
				}
			} catch (Exception e) {
				Logger.debug(DotGuavaCacheAdministratorImpl.class,e.getMessage(),e);
			}
		}
		return j;

	}
	/**
	 * Gets from Memory, if not in memory, tries disk
	 */
	public Object get(String key, String group) throws DotCacheException {
		if(key == null || group == null){
			return null;
		}
		key = key.toLowerCase();
		group = group.toLowerCase();

		Object j = getMemory( key,  group);
		if(j==null){
			j= getDisk( key,  group);
		}

		return j;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.dotmarketing.business.DotCacheAdministrator#put(java.lang.String,
	 * java.lang.Object, java.lang.String[])
	 */
	public void put(String key, final Object content, String group) {
		if(key == null || group == null){
			return;
		}
		key = key.toLowerCase();
		group = group.toLowerCase();



		Cache cache = getCache(group);
		cache.put(key, content);

		if(isDiskCache(group)){
			try {
				diskCache.put(new Fqn(group, key), key, content);
			} catch (Exception e) {
				Logger.debug(DotGuavaCacheAdministratorImpl.class,e.getMessage(),e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.dotmarketing.business.DotCacheAdministrator#remove(java.lang.String)
	 */
	public void remove(final String key, final String group) {
		if(key == null || group == null){
			return;
		}
		Runnable cacheRemoveRunnable=new Runnable() {
	         public void run() {

				String k = key.toLowerCase();
				String g = group.toLowerCase();
				removeLocalOnly(k, g);

				try {
					if (Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)) {
						journalAPI.addCacheEntry(k, g);
					} else if ( useTransportChannel ) {

						if ( getTransport() != null ) {
							try {
								getTransport().send(k + ":" + g);
							} catch ( Exception e ) {
								Logger.error(DotGuavaCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
							}
						} else {
							throw new CacheTransportException("No Cache transport implementation is defined");
						}

					}
				} catch (DotDataException e) {
					Logger.error(this, "Unable to add journal entry for cluster", e);
				}
	         }
		};
		try {
			if(DbConnectionFactory.inTransaction()){
				HibernateUtil.addCommitListener(cacheRemoveRunnable);
			}
		} catch (Exception e) {
			Logger.error(DotGuavaCacheAdministratorImpl.class,e.getMessage(),e);
		}
		cacheRemoveRunnable.run();
	}

	/*
	 * This method should only be called by Jgroups because it doesn't handle any local transaction as the remove does. 
	 */
	public void removeLocalOnly(final String key, final String group) {
		if(key == null || group == null){
			return;
		}
		Runnable cacheRemoveRunnable=new Runnable() {
	         public void run() {
				String k = key.toLowerCase();
				String g = group.toLowerCase();
				Cache<String, Object>  cache = getCache(g);
				cache.invalidate(k);
				if(isDiskCache(g)){
					try {
						if(!UtilMethods.isSet(key)){
							Logger.error(this.getClass(), "Empty key passed in, clearing group " + group + " by mistake");
						}
						diskCache.remove(new Fqn(g, k), k.toLowerCase());
					} catch (Exception e) {
						Logger.error(DotGuavaCacheAdministratorImpl.class,e.getMessage(),e);
					}
				}
	         }
		};
		cacheRemoveRunnable.run();
	}

	public Set<String> getKeys(String group) {
		if(group ==null ){
			return null;
		}
		Set<String> keys=new HashSet<String>();

		group = group.toLowerCase();
		Cache<String, Object> cache = getCache(group);
		Map<String, Object> m = cache.asMap();

		if (m!=null) {
			keys.addAll(m.keySet());
		}

		if(diskCache!=null && isDiskCache(group)) {
		    try {
		        keys.addAll(diskCache.getKeys(group));
		    }
		    catch(Exception ex) {
		        Logger.error(this, "can't get h2 cache keys on group "+group,ex);
		    }
		}

		return keys;


	}

	public List<Map<String, Object>> getCacheStatsList() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();


		Set<String> myGroups = new HashSet<String>();

		myGroups.addAll(groups.keySet());

		if(diskCache != null){
			try {
				for(String s : H2CacheLoader.getGroups()){
					myGroups.add(s.toLowerCase());
				}
			} catch (SQLException e) {
				Logger.error(DotGuavaCacheAdministratorImpl.class, e.getMessage(), e);
			}
		}

		Cache dCache = getCache(DEFAULT_CACHE);
		for (String group : myGroups) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("cache", getCache(group));
			String region = group.toString();
			m.put("region", region);

			m.put("toDisk", new Boolean(isDiskCache(group)));
			boolean isDefault = false;


			try {
				Cache n = getCache(group);
				m.put("memory", n.size());

				m.put("CacheStats", n.stats());
				isDefault = (!DEFAULT_CACHE.equals(group) && n.equals(dCache));

			} catch (Exception e) {

			}
			m.put("isDefault", isDefault);
			m.put("disk", -1);
			if(diskCache != null){
				if(isDiskCache(group)){
					m.put("disk", H2CacheLoader.getGroupCount(group.toString()));
				}
			}


			int configured = isDefault
				? Config.getIntProperty("cache."+DEFAULT_CACHE+".size" )
    				: (Config.getIntProperty("cache." +region + ".size", -1 ) != -1)
    					? Config.getIntProperty("cache." +region + ".size" )
    						: (region.startsWith(WORKING_CACHE_PREFIX) && Config.getIntProperty("cache."+WORKING_CACHE_PREFIX+".size", -1 ) != -1)
    							? Config.getIntProperty("cache."+WORKING_CACHE_PREFIX+".size" )
   									: (region.startsWith(LIVE_CACHE_PREFIX) && Config.getIntProperty("cache."+LIVE_CACHE_PREFIX+".size", -1 ) != -1)
   										? Config.getIntProperty("cache."+LIVE_CACHE_PREFIX+".size" )
   												: Config.getIntProperty("cache."+DEFAULT_CACHE+".size" );



   			m.put("configuredSize", configured);

			list.add(m);

		}


		Collections.sort(list, new CacheComparator());

		return list;
	}

	private class CacheComparator implements Comparator<Map<String,Object>>{

		public int compare(Map<String,Object> o1, Map<String,Object> o2) {

			if(o1==null && o2!=null) return 1;
			if(o1!=null && o2==null) return -1;
			if(o1==null && o2==null) return 0;

			String group1 = (String) o1.get("region");
			String group2 = (String) o2.get("region");

			if(!UtilMethods.isSet(group1) && !UtilMethods.isSet(group2)) {
				return 0;
			} else if(UtilMethods.isSet(group1) && !UtilMethods.isSet(group2)) {
				return -1;
			} else if(!UtilMethods.isSet(group1) && UtilMethods.isSet(group2)) {
				return 1;
			} else if(group1.equals(group2)) {
				return 0;
			} else if(group1.startsWith(WORKING_CACHE_PREFIX) && group2.startsWith(LIVE_CACHE_PREFIX)) {
				return 1;
			} else if(group1.startsWith(LIVE_CACHE_PREFIX) && group2.startsWith(WORKING_CACHE_PREFIX)) {
				return -1;
			} else if(!group1.startsWith(LIVE_CACHE_PREFIX) && !group1.startsWith(WORKING_CACHE_PREFIX) && (group2.startsWith(LIVE_CACHE_PREFIX) || group2.startsWith(WORKING_CACHE_PREFIX)) ) {
				return -1;
			} else if((group1.startsWith(LIVE_CACHE_PREFIX) || group1.startsWith(WORKING_CACHE_PREFIX)) && !group2.startsWith(LIVE_CACHE_PREFIX) && !group2.startsWith(WORKING_CACHE_PREFIX) ) {
				return 1;
			} else { // neither group1 nor group2 are live or working
				return group1.compareToIgnoreCase(group2);
			}
		}


	}

	public void shutdown() {
		if(diskCache != null){
			diskCache.destroy();
		}
	}

	public void shutdownChannel () {

		if ( getTransport() != null ) {
			getTransport().shutdown();
			useTransportChannel = false;
		} else {
			throw new CacheTransportException("No Cache transport implementation is defined");
		}

	}

	public boolean isClusteringEnabled() {
		return useTransportChannel;
	}

	public void send ( String msg ) {

		if ( getTransport() != null ) {

			try {
				getTransport().send(msg);
			} catch ( Exception e ) {
				Logger.warn(DotGuavaCacheAdministratorImpl.class, "Unable to send message to cluster : " + e.getMessage(), e);
			}

		} else {
			throw new CacheTransportException("No Cache transport implementation is defined");
		}

	}

	/**
	 * Tests the transport channel of a cluster sending and receiving messages for a given number of servers
	 *
	 * @param dateInMillis   String use as Key on out Map of results.
	 * @param numberServers  Number of servers to wait for a response.
	 * @param maxWaitSeconds seconds to wait for a response.
	 * @return Map with DateInMillis, ServerInfo for each cache/live server in Cluster.
	 */
	public Map<String, Boolean> validateCacheInCluster ( String dateInMillis, int numberServers, int maxWaitSeconds ) throws DotCacheException {

		if ( getTransport() != null ) {

			try {
				return getTransport().validateCacheInCluster(dateInMillis, numberServers, maxWaitSeconds);
			} catch ( CacheTransportException e ) {
				Logger.error(DotGuavaCacheAdministratorImpl.class, e.getMessage(), e);
				throw new DotCacheException(e);
			}

		} else {
			throw new CacheTransportException("No Cache transport implementation is defined");
		}
	}

	public void testCluster () {

		if ( getTransport() != null ) {

			try {
				getTransport().testCluster();
			} catch ( Exception e ) {
				Logger.error(DotGuavaCacheAdministratorImpl.class, e.getMessage(), e);
			}

		} else {
			throw new CacheTransportException("No Cache transport implementation is defined");
		}

	}

	private Cache<String, Object> getCache(String cacheName) {

		if (cacheName == null) {
			throw new DotStateException("Null cache region passed in");
		}

		cacheName = cacheName.toLowerCase();
		Cache<String, Object> cache = groups.get(cacheName);

		// init cache if it does not exist
		if (cache == null) {
			synchronized (cacheName.intern()) {
				cache = groups.get(cacheName);
				if (cache == null) {


					boolean separateCache = (availableCaches.contains(cacheName) || DEFAULT_CACHE.equals(cacheName) ||cacheName.startsWith(LIVE_CACHE_PREFIX) || cacheName.startsWith(WORKING_CACHE_PREFIX) );


					if (separateCache) {
						int size = -1;
						boolean toDisk = false;
						if (cacheName.startsWith(LIVE_CACHE_PREFIX)) {
							size = Config.getIntProperty("cache." + cacheName + ".size", -1);
							if(size <0){
								size = Config.getIntProperty("cache."+LIVE_CACHE_PREFIX+".size", -1);
							}
							if(Config.containsProperty("cache." + cacheName + ".disk")){
								toDisk = Config.getBooleanProperty("cache." + cacheName + ".disk", false);
							}
							else{
								toDisk = Config.getBooleanProperty("cache."+LIVE_CACHE_PREFIX+".disk", false);
							}
						}
						else if (cacheName.startsWith(WORKING_CACHE_PREFIX)) {
							size = Config.getIntProperty("cache." + cacheName + ".size", -1);
							if(size <0){
								size = Config.getIntProperty("cache."+WORKING_CACHE_PREFIX+".size", -1);
							}
							if(Config.containsProperty("cache." + cacheName + ".disk")){
								toDisk = Config.getBooleanProperty("cache." + cacheName + ".disk", false);
							}
							else{
								toDisk = Config.getBooleanProperty("cache."+WORKING_CACHE_PREFIX+".disk", false);
							}
						}
						else {
							size = Config.getIntProperty("cache." + cacheName + ".size", -1);
							if(Config.containsProperty("cache." + cacheName + ".disk")){
								toDisk = Config.getBooleanProperty("cache." + cacheName + ".disk", false);
							}
							else{
								toDisk = Config.getBooleanProperty("cache."+DEFAULT_CACHE+".disk", false);
							}
						}

						if (size == -1) {
							size = Config.getIntProperty("cache."+DEFAULT_CACHE+".size", 100);
						}

						Logger.info(this.getClass(), "***\t Building Cache : " + cacheName + ", size:" + size + ", toDisk:" + toDisk + ",Concurrency:" + Config.getIntProperty("cache.concurrencylevel", 32));
						CacheBuilder<Object, Object> cb  = CacheBuilder
								.newBuilder()
								.maximumSize(size)
								.concurrencyLevel(Config.getIntProperty("cache.concurrencylevel", 32));



						cache = cb.build();
						groups.put(cacheName, cache);

					} else {
						Logger.info(this.getClass(), "***\t No Cache for   : " + cacheName + ", using " + DEFAULT_CACHE);
						cache = getCache(DEFAULT_CACHE);
						groups.put(cacheName, cache);
					}
				}
			}
		}

		return cache;

	}

	private class DotRemoval implements RemovalListener {

		public void onRemoval(RemovalNotification removalEvent) {

		}

	}

    @Override
    public Class getImplementationClass() {
        return DotGuavaCacheAdministratorImpl.class;
    }

    @Override
    public DotCacheAdministrator getImplementationObject() {
        return this;
    }

}
