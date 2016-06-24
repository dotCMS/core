/**
 *
 */
package com.dotmarketing.business;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.bean.ServerPort;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.enterprise.cache.provider.CacheProviderAPI;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.repackage.com.google.common.cache.RemovalListener;
import com.dotcms.repackage.com.google.common.cache.RemovalNotification;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Cache administrator that uses the CacheProviders infrastructure (Cache chains)
 *
 * @author Jason Tesser
 * @version 1.6.5
 *
 */
public class ChainableCacheAdministratorImpl implements DotCacheAdministrator {

	CacheTransport cacheTransport;

	private DistributedJournalAPI journalAPI;
	private CacheProviderAPI cacheProviderAPI;
	private boolean useTransportChannel = false;

	public static final String TEST_MESSAGE = "HELLO CLUSTER!";
	public static final String TEST_MESSAGE_NODE = "TESTNODE";
	public static final String VALIDATE_CACHE = "validateCacheInCluster-";
	public static final String VALIDATE_CACHE_RESPONSE = "validateCacheInCluster-response-";
	public static final String VALIDATE_SEPARATOR = "_";
	public static final String DUMMY_TEXT_TO_SEND = "DUMMY MSG TO TEST SEND";

	public CacheTransport getTransport () {
		return cacheTransport;
	}

	public void setTransport ( CacheTransport transport ) {

		if ( getTransport() != null ) {
			getTransport().shutdown();
		}

		this.cacheTransport = transport;
	}

	public ChainableCacheAdministratorImpl () {
		this(null);
	}

	public ChainableCacheAdministratorImpl ( CacheTransport transport ) {

		if ( transport != null ) {
			useTransportChannel = true;
			this.cacheTransport = transport;
		} else {
			useTransportChannel = false;
		}

		journalAPI = APILocator.getDistributedJournalAPI();
	}

	public void initProviders () {

		try {
			//Initializing all the Cache providers
			cacheProviderAPI = APILocator.getCacheProviderAPI();
			cacheProviderAPI.init();
		} catch ( Exception e ) {
			throw new DotRuntimeException("Error initializing Cache providers", e);
		}

	}

	public void setCluster(Server localServer) throws Exception {
			Logger.info(this, "***\t Starting JGroups Cluster Setup");

			journalAPI = APILocator.getDistributedJournalAPI();
			ServerAPI serverAPI = APILocator.getServerAPI();
			
			String cacheProtocol, bindAddr, bindPort, cacheTCPInitialHosts, mCastAddr, mCastPort, preferIPv4;

			if(Config.getBooleanProperty("CLUSTER_AUTOWIRE",true)) {
			    Logger.info(this, "Using automatic port placement as CLUSTER_AUTOWIRE is ON");

				String bindAddressFromProperty = Config.getStringProperty("CACHE_BINDADDRESS", null, false);

				if(UtilMethods.isSet(bindAddressFromProperty)) {
					try {
						InetAddress addr = InetAddress.getByName(bindAddressFromProperty);
						if(ClusterFactory.isValidIP(bindAddressFromProperty)){
							bindAddressFromProperty = addr.getHostAddress();
						}else{
							Logger.info(ClusterFactory.class, "Address provided in CACHE_BINDADDRESS property is not "
								+ "valid: " + bindAddressFromProperty);
							bindAddressFromProperty = null;
						}
					} catch(UnknownHostException e) {
						Logger.info(ClusterFactory.class, "Address provided in CACHE_BINDADDRESS property is not "
							+ " valid: " + bindAddressFromProperty);
						bindAddressFromProperty = null;
					}
				}
			    
			    cacheProtocol = Config.getStringProperty("CACHE_PROTOCOL", "tcp");

	            bindAddr = bindAddressFromProperty!=null ? bindAddressFromProperty : localServer.getIpAddress();

				if(UtilMethods.isSet(localServer.getCachePort())){
					bindPort = Long.toString(localServer.getCachePort());
				} else {
					bindPort = ClusterFactory.getNextAvailablePort(localServer.getServerId(), ServerPort.CACHE_PORT);
				}

                localServer.setCachePort(Integer.parseInt(bindPort));

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

                cacheTCPInitialHosts = Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", initialHosts.toString());

                mCastAddr = Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10");
                mCastPort = Config.getStringProperty("CACHE_MULTICAST_PORT", "45588");
                preferIPv4 = Config.getStringProperty("CACHE_FORCE_IPV4", "true");
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
			getTransport().init(localServer);
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

		flushAlLocalOnly();

		try {
			if (Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)) {
				journalAPI.addCacheEntry("0", ROOT_GOUP);
			} else if ( useTransportChannel ) {

				if ( getTransport() != null ) {
					try {
						getTransport().send("0:" + ROOT_GOUP);
					} catch ( Exception e ) {
						Logger.error(ChainableCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
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

		flushGroupLocalOnly(group);

		try {
			if (Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)) {
				journalAPI.addCacheEntry("0", group);
			} else if ( useTransportChannel ) {

				try {
					cacheTransport.send("0:" + group);
				} catch (Exception e) {
					Logger.error(ChainableCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
				}

			}
		} catch (DotDataException e) {
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}

	public void flushAlLocalOnly () {

		//Invalidates all the Cache
		cacheProviderAPI.removeAll();
	}

	public void flushGroupLocalOnly ( String group ) {

		if ( group == null ) {
			return;
		}

		group = group.toLowerCase();

		//Invalidates the Cache for the given group
		cacheProviderAPI.remove(group);
	}

	public Object get ( String key, String group ) throws DotCacheException {

		if ( key == null || group == null ) {
			return null;
		}

		key = key.toLowerCase();
		group = group.toLowerCase();

		//Find the Object for a given key in a given group
		return cacheProviderAPI.get(group, key);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.dotmarketing.business.DotCacheAdministrator#put(java.lang.String,
	 * java.lang.Object, java.lang.String[])
	 */
	public void put ( String key, final Object content, String group ) {

		if ( key == null || group == null ) {
			return;
		}

		key = key.toLowerCase();
		group = group.toLowerCase();

		//Adds a given object gor a given group to a given key
		cacheProviderAPI.put(group, key, content);
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

		FlushCacheRunnable cacheRemoveRunnable=new FlushCacheRunnable() {
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
								Logger.error(ChainableCacheAdministratorImpl.class, "Unable to send invalidation to cluster : " + e.getMessage(), e);
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
			Logger.error(ChainableCacheAdministratorImpl.class,e.getMessage(),e);
		}

		cacheRemoveRunnable.run();
	}

	public void removeLocalOnly ( final String key, final String group ) {

		if ( key == null || group == null ) {
			return;
		}

		Runnable cacheRemoveRunnable = new Runnable() {
			public void run () {
				//Invalidates from Cache a key from a given group
				cacheProviderAPI.remove(group, key);
			}
		};
		cacheRemoveRunnable.run();
	}

	public Set<String> getGroups () {
		//Returns all groups in the cache
		return cacheProviderAPI.getGroups();
	}

	public List<Map<String, Object>> getCacheStatsList () {
		//Returns the stats for all the cache providers
		return cacheProviderAPI.getStats();
	}

	public void shutdown () {
		cacheProviderAPI.shutdown();
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
				Logger.warn(ChainableCacheAdministratorImpl.class, "Unable to send message to cluster : " + e.getMessage(), e);
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
				Logger.error(ChainableCacheAdministratorImpl.class, e.getMessage(), e);
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
				Logger.error(ChainableCacheAdministratorImpl.class, e.getMessage(), e);
			}

		} else {
			throw new CacheTransportException("No Cache transport implementation is defined");
		}

	}

	private class DotRemoval implements RemovalListener {

		public void onRemoval(RemovalNotification removalEvent) {

		}

	}

    @Override
    public Class getImplementationClass() {
        return ChainableCacheAdministratorImpl.class;
    }

    @Override
    public DotCacheAdministrator getImplementationObject() {
        return this;
    }

}
