/**
 * 
 */
package com.dotmarketing.business;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jboss.cache.Fqn;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import com.dotmarketing.cache.H2CacheLoader;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * The Guava cache administrator uses Google's Guave code 
 * under the covers and gets it's startup params from the dotmarketing-config.properties
 * on a put where the non legacy one will not.
 * 
 * @author Jason Tesser
 * @version 1.6.5
 * 
 */
public class DotGuavaCacheAdministratorImpl extends ReceiverAdapter implements DotCacheAdministrator {

	private DistributedJournalAPI journalAPI;
	private Map<String, Cache<String, Object>> groups = new HashMap<String, Cache<String, Object>>();
	private JChannel channel;
	private boolean useJgroups = false;
	private HashMap<String, Boolean> cacheToDisk = new HashMap<String,Boolean>();
	private HashSet<String> availableCaches = new HashSet<String>();
	private H2CacheLoader diskCache = null;
	
	static final String LIVE_CACHE_PREFIX = "livecache";
	static final String WORKING_CACHE_PREFIX = "workingcache";
	static final String DEFAULT_CACHE = "default";
	
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
	
	
	
	public DotGuavaCacheAdministratorImpl() {
		journalAPI = APILocator.getDistributedJournalAPI();



		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		
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



		if ((Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false) == false)
				&& Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false)) {
			Logger.info(this, "***\t Starting JGroups Cluster Setup");
			try {
				String cacheFile = "cache-jgroups-" + Config.getStringProperty("CACHE_PROTOCOL", "tcp") + ".xml";
				Logger.info(this, "***\t Going to load JGroups with this Classpath file " + cacheFile);
				String bindAddr = Config.getStringProperty("CACHE_BINDADDRESS", null);
				if (bindAddr != null) {
					Logger.info(this, "***\t Using " + bindAddr + " as the bindaddress");
				} else {
					Logger.info(this, "***\t bindaddress is not set");
				}
				if (UtilMethods.isSet(bindAddr)) {
					System.setProperty("jgroups.bind_addr", bindAddr);
				}
				String bindPort = Config.getStringProperty("CACHE_BINDPORT", null);
				if (bindPort != null) {
					Logger.info(this, "***\t Using " + bindPort + " as the bindport");
				} else {
					Logger.info(this, "***\t bindport is not set");
				}
				if (UtilMethods.isSet(bindPort)) {
					System.setProperty("jgroups.bind_port", bindPort);
				}
				String protocol = Config.getStringProperty("CACHE_PROTOCOL", "tcp");
				if (protocol.equals("tcp")) {
					Logger.info(this, "***\t Setting up TCP Prperties");
					System.setProperty("jgroups.tcpping.initial_hosts",
							Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", "localhost[7800]"));
				} else if (protocol.equals("udp")) {
					Logger.info(this, "***\t Setting up UDP Prperties");
					System.setProperty("jgroups.udp.mcast_port", Config.getStringProperty("CACHE_MULTICAST_PORT", "45588"));
					System.setProperty("jgroups.udp.mcast_addr", Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10"));
				} else {
					Logger.info(this, "Not Setting up any Properties as no protocal was found");
				}
				System.setProperty("java.net.preferIPv4Stack", Config.getStringProperty("CACHE_FORCE_IPV4", "true"));
				Logger.info(this, "***\t Setting up JCannel");
				channel = new JChannel(classLoader.getResource(cacheFile));
				channel.setReceiver(this);
				channel.connect("dotCMSCluster");
				channel.setOpt(JChannel.LOCAL, false);
				useJgroups = true;
				Logger.info(this, "***\t " + channel.toString(true));
				Logger.info(this, "***\t Ending JGroups Cluster Setup");
			} catch (Exception e1) {
				Logger.info(this, "Error During JGroups Cluster Setup");
				Logger.fatal(DotGuavaCacheAdministratorImpl.class, e1.getMessage(), e1);
			}
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
			} else if (useJgroups) {
				Message msg = new Message(null, null, "0:" + ROOT_GOUP);
				try {
					channel.send(msg);
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
			} else if (useJgroups) {
				Message msg = new Message(null, null, "0:" + group);
				try {
					channel.send(msg);
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
		groups = new HashMap<String, Cache<String,Object>>();
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
		cacheToDisk = new HashMap<String,Boolean>();
		
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
	public void remove(String key, String group) {
		if(key == null || group == null){
			return;
		}
		key = key.toLowerCase();
		group = group.toLowerCase();
		removeLocalOnly(key, group);

		try {
			if (Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)) {
				journalAPI.addCacheEntry(key, group);
			} else if (useJgroups) {
				Message msg = new Message(null, null, key + ":" + group);
				try {
					channel.send(msg);
				} catch (Exception e) {
					Logger.error(DotGuavaCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
				}
			}
		} catch (DotDataException e) {
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}

	public void removeLocalOnly(String key, String group) {
		if(key == null || group == null){
			return;
		}
		key = key.toLowerCase();
		group = group.toLowerCase();
		Cache<String, Object>  cache = getCache(group);
		cache.invalidate(key);
		if(isDiskCache(group)){
			try {
				diskCache.remove(new Fqn(group, key), key.toLowerCase());
			} catch (Exception e) {
				Logger.error(DotGuavaCacheAdministratorImpl.class,e.getMessage(),e);
			}
		}
		
	}

	public Set<String> getKeys(String group) {
		if(group ==null ){
			return null;
		}
		group = group.toLowerCase();
		Cache<String, Object> cache = getCache(group);
		Map<String, Object> m = cache.asMap();
		
		if (m!=null) {
			return m.keySet();

		} 
			
		return null;
		

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

	private class CacheComparator implements Comparator<Map<String, Object>>{

		public int compare(Map o1, Map o2) {
			try{
				String group1 = (String) o1.get("region");
				String group2 = (String) o2.get("region");
				if(group1.toLowerCase().startsWith("working")){
					return 1;
				}
				if(group1.toLowerCase().startsWith(LIVE_CACHE_PREFIX)){
					return 1;
				}
				if(group2.toLowerCase().startsWith(WORKING_CACHE_PREFIX)){
					return -1;
				}
				if(group2.toLowerCase().startsWith(LIVE_CACHE_PREFIX)){
					return -1;
				}
				else{
					return group1.compareToIgnoreCase(group2);
				}
			}
			catch(Exception e){
				
			}
			
			return 0;
			
		}
		
		
	}
	
	
	
	
	
	public String getCacheStats() {

		return null;
	}

	public void shutdown() {
		if(diskCache != null){
			diskCache.destroy();
		}
	}



	public JChannel getJGroupsChannel() {
		return channel;
	}

	@Override
	public void receive(Message msg) {
		if (msg == null) {
			return;
		}
		Object v = msg.getObject();
		if (v == null) {
			return;
		}

		if (v.toString().equals("TESTINGCLUSTER")) {
			Logger.info(this, "Received Message Ping " + new Date());
		} else {
			invalidateCacheFromCluster(v.toString());
		}
	}

	public void viewAccepted(View new_view) {
		super.viewAccepted(new_view);
		Logger.info(this, "Method view: Cluster View is : " + new_view);
		AdminLogger.log(DotGuavaCacheAdministratorImpl.class, "viewAccepted", "Cluster View is : " + new_view);
	}

	@Override
	public void suspect(Address mbr) {
		super.suspect(mbr);
		Logger.info(this, "Method suspect: There is a suspected member : " + mbr);
		AdminLogger.log(DotGuavaCacheAdministratorImpl.class, "suspect", "There is a suspected member : " + mbr);
	}

	public void testCluster() {
		Message msg = new Message(null, null, "TESTINGCLUSTER");
		try {
			channel.send(msg);
			Logger.info(this, "Sending Ping to Cluster " + new Date());
		} catch (ChannelNotConnectedException e) {
			Logger.error(DotGuavaCacheAdministratorImpl.class, e.getMessage(), e);
		} catch (ChannelClosedException e) {
			Logger.error(DotGuavaCacheAdministratorImpl.class, e.getMessage(), e);
		}
	}

	private void invalidateCacheFromCluster(String k) {
		boolean flushMenus = false;
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
		String menuGroup = vc.getMenuGroup();

		int i = k.lastIndexOf(":");
		if (i > 0) {
			String key = k.substring(0, i);
			String group = k.substring(i + 1, k.length());
			
			key = key.toLowerCase();
			group = group.toLowerCase();
			if (groups != null) {
				
				if (groups.containsKey(group)) {
					Logger.debug(this, "Cluster Eviction of Key : " + key + " With Group : " + group + " from cache");
				}
			}
			if (key.contains("dynamic")) {
				if (group.equals(menuGroup)) {
					flushMenus = true;
				}
			}
			if (!flushMenus) {
				if (key.equals("0")) {
					if (group.equalsIgnoreCase(DotCacheAdministrator.ROOT_GOUP)) {
						CacheLocator.getCacheAdministrator().flushAlLocalOnlyl();
					} else if (group.equalsIgnoreCase(menuGroup)) {
						flushMenus = true;
					} else {
						CacheLocator.getCacheAdministrator().flushGroupLocalOnly(group);
					}
				} else {
					CacheLocator.getCacheAdministrator().removeLocalOnly(key, group);
				}
			}
		} else {
			Logger.error(this, "The cache to locally remove key is invalid. The value was " + k);
		}
		if (flushMenus) {
			RefreshMenus.deleteMenusOnFileSystemOnly();
			CacheLocator.getCacheAdministrator().flushGroupLocalOnly(menuGroup);
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
			synchronized (cacheName) {
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
