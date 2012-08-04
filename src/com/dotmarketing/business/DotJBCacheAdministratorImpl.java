/**
 * 
 */
package com.dotmarketing.business;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.Region;
import org.jboss.cache.jmx.CacheJmxWrapper;
import org.jboss.cache.jmx.CacheJmxWrapperMBean;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import com.dotmarketing.business.mbeans.CacheInfo;
import com.dotmarketing.business.mbeans.CacheInfoMBean;
import com.dotmarketing.cache.H2CacheLoader;
import com.dotmarketing.common.business.journal.DistributedJournalAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.velocity.DotResourceCache;
import com.liferay.util.FileUtil;

/**
 * The legacy cache administrator will invalidate cache entries within a cluster
 * on a put where the non legacy one will not.  
 * @author Jason Tesser
 * @version 1.6.5
 *
 */
public class DotJBCacheAdministratorImpl extends ReceiverAdapter implements DotCacheAdministrator {
	
	private Cache<String, Object> cache;
	private DistributedJournalAPI journalAPI;
	private Map<String, Node> regions = new HashMap<String, Node>();
	private JChannel channel;
	private boolean useJgroups = false;
	private TreeSet<String> groups = null;
	private TreeSet<String> keys = null;
		
	public DotJBCacheAdministratorImpl() {
		journalAPI = APILocator.getDistributedJournalAPI();
		String dotCMSSize = Config.getStringProperty("INSTANCE_SIZE");
		File baseConfig = null;
		File sizeConfig = null;
		URL url = null;
		ClassLoader classLoader = null;
		String baseConfigText = null;
		String sizeConfigText = null;
		String evictionPattern = "<!-- EVICTION -->";
		CharSequence sequence = null;
		Pattern pattern = null;
		Matcher matcher = null; 

		if(!dotCMSSize.equalsIgnoreCase("small") && !dotCMSSize.equalsIgnoreCase("med") && !dotCMSSize.equalsIgnoreCase("large"))
			dotCMSSize = "small";
		Logger.info(this,"Initializing dotCMS cache with a dotCMS  " + dotCMSSize + " instance size");
		
		classLoader = Thread.currentThread().getContextClassLoader();
		url = classLoader.getResource("cache-configuration.xml");
		if (url != null) {
			baseConfig = new File(url.getPath());
			try {
				baseConfigText = new String(FileUtil.getBytes(baseConfig));
			} catch (IOException e) {
				Logger.fatal(this,"Cannot initialize dotCMS.  Cache Configs not found.  Shutting Down!!!");
			}
		}else{
			Logger.fatal(this,"Cannot initialize dotCMS.  Cache Configs not found.  Shutting Down!!!");
		}
		
		url = classLoader.getResource("cache-configuration-region-" + dotCMSSize + ".xml");
		if (url != null) {
			sizeConfig = new File(url.getPath());
			try {
				sizeConfigText = new String(FileUtil.getBytes(sizeConfig));
			} catch (IOException e) {
				Logger.fatal(this,"Cannot initialize dotCMS.  Cache Configs not found.  Shutting Down!!!");
			}
		}else{
			Logger.fatal(this,"Cannot initialize dotCMS.  Cache Configs not found.  Shutting Down!!!");
		}
		
		sequence = baseConfigText.subSequence(0, baseConfigText.length());
		pattern = Pattern.compile(evictionPattern);
		matcher = pattern.matcher(sequence);
		baseConfigText = matcher.replaceAll(sizeConfigText);
		
		ByteArrayInputStream configIS = new ByteArrayInputStream(baseConfigText.getBytes());
		
		CacheFactory<String, Object> factory = new DefaultCacheFactory<String, Object>();
		//cache = factory.createCache("cache-configuration.xml");
		cache = factory.createCache(configIS);
		cache.getRoot().setResident(true);
		cache.getRoot().getChildren();
		try {
			Set<String> groups = H2CacheLoader.getGroups();
			for (String group : groups) {
				setUpGroup(group);
			}
		} catch (SQLException e1) {
			Logger.fatal(DotJBCacheAdministratorImpl.class,"Unable to load Groups from H2Cache Loader : " + e1.getMessage(),e1);
		}
		
		if((Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false) == false)&& Config.getBooleanProperty("DIST_INDEXATION_ENABLED", false)){
			Logger.info(this, "Starting JGroups Cluster Setup");
			try {
				String cacheFile = "cache-jgroups-" + Config.getStringProperty("CACHE_PROTOCOL","tcp") + ".xml";
				Logger.info(this, "Going to load JGroups with this Classpath file " + cacheFile);
				String bindAddr = Config.getStringProperty("CACHE_BINDADDRESS",null);
				if(bindAddr != null){
					Logger.info(this, "Using " + bindAddr + " as the bindaddress");
				}else{
					Logger.info(this, "bindaddress is not set");
				}
				if(UtilMethods.isSet(bindAddr)){
					System.setProperty("jgroups.bind_addr", bindAddr);
				}
				String bindPort = Config.getStringProperty("CACHE_BINDPORT", null);
				if(bindPort != null){
					Logger.info(this, "Using " + bindPort + " as the bindport");
				}else{
					Logger.info(this, "bindport is not set");
				}
				if(UtilMethods.isSet(bindPort)){
					System.setProperty("jgroups.bind_port", bindPort);
				}
				String protocol = Config.getStringProperty("CACHE_PROTOCOL","tcp");
				if(protocol.equals("tcp")){
					Logger.info(this, "Setting up TCP Prperties");
					System.setProperty("jgroups.tcpping.initial_hosts", Config.getStringProperty("CACHE_TCP_INITIAL_HOSTS", "localhost[7800]"));
				}else if(protocol.equals("udp")){
					Logger.info(this, "Setting up UDP Prperties");
					System.setProperty("jgroups.udp.mcast_port", Config.getStringProperty("CACHE_MULTICAST_PORT", "45588"));
					System.setProperty("jgroups.udp.mcast_addr", Config.getStringProperty("CACHE_MULTICAST_ADDRESS", "228.10.10.10"));
				}else{
					Logger.info(this, "Not Setting up any Properties as no protocal was found");
				}
				System.setProperty("java.net.preferIPv4Stack", Config.getStringProperty("CACHE_FORCE_IPV4", "true"));
				Logger.info(this, "Setting up JCannel");
				channel = new JChannel(classLoader.getResource(cacheFile));
				channel.setReceiver(this);
				channel.connect("dotCMSCluster");
				channel.setOpt(JChannel.LOCAL, false);
				useJgroups = true;
				Logger.info(this,channel.toString(true));
				Logger.info(this, "Ending JGroups Cluster Setup");
			} catch (Exception e1) {
				Logger.info(this, "Error During JGroups Cluster Setup");
				Logger.fatal(DotJBCacheAdministratorImpl.class,e1.getMessage(),e1);
			}
		}
		
		try {
			CacheJmxWrapperMBean wrapper = new CacheJmxWrapper(cache);
			MBeanServer server = ManagementFactory.getPlatformMBeanServer(); 
			ObjectName on = new ObjectName("jboss.cache:service=TreeCache");
			server.registerMBean(wrapper, on);
			
			on = new ObjectName("org.dotcms:type=CacheInfo");
			CacheInfoMBean infoBean=new CacheInfo(this);
			server.registerMBean(infoBean, on);
			
		} catch (MalformedObjectNameException e) {
			Logger.debug(DotJBCacheAdministratorImpl.class,"MalformedObjectNameException: " + e.getMessage(),e);
		} catch (InstanceAlreadyExistsException e) {
			Logger.debug(DotJBCacheAdministratorImpl.class,"InstanceAlreadyExistsException: " + e.getMessage(),e);
		} catch (MBeanRegistrationException e) {
			Logger.debug(DotJBCacheAdministratorImpl.class,"MBeanRegistrationException: " + e.getMessage(),e);
		} catch (NotCompliantMBeanException e) {
			Logger.debug(DotJBCacheAdministratorImpl.class,"NotCompliantMBeanException: " + e.getMessage(),e);
		} catch (NullPointerException e) {
			Logger.debug(DotJBCacheAdministratorImpl.class,"NullPointerException: " + e.getMessage(),e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#flushAll()
	 */
	public void flushAll() {
		flushAlLocalOnlyl();
		try{
			if(Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)){
				journalAPI.addCacheEntry("0", ROOT_GOUP);
			}else if(useJgroups){
				Message msg = new Message(null,null,"0:" + ROOT_GOUP);
				try {
					channel.send(msg);
				} catch (Exception e) {
					Logger.error(DotJBCacheAdministratorImpl.class,"Unable to send invalidation to cluster : " + e.getMessage(),e);
				}
			}
		}catch(DotDataException e){
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#flushGroup(java.lang.String)
	 */
	public void flushGroup(String group) {
		//cache.removeNode(group);
		group = group.toUpperCase();
		Node groupNode =cache.getRoot().getChild(group);
		if(groupNode != null){
			flushGroupLocalOnly(group);
		}
		try{
			if(Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)){
				journalAPI.addCacheEntry("0", group);
			}else if(useJgroups){
				Message msg = new Message(null,null,"0:" + group);
				try {
					channel.send(msg);
				} catch (Exception e) {
					Logger.error(DotJBCacheAdministratorImpl.class,"Unable to send invalidation to cluster : " + e.getMessage(),e);
				}
			}
		}catch(DotDataException e){
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#flushAll()
	 */
	public void flushAlLocalOnlyl() {
		Set<Node<String,Object>> c = cache.getRoot().getChildren();
		for (Node<String, Object> node : c) {
			cache.removeNode(node.getFqn());
			setUpGroup(node.getFqn().toString().replaceFirst("/",""));
//			cache.evict(node.getFqn(),true);
//			node.clearData();
		}
		regions.clear();
	}
	
	public void flushGroupLocalOnly(String group) {
		group = group.toUpperCase();
		Node grp = cache.getRoot().getChild(group);
		if(grp == null){
			setUpGroup(group);
			cache.getRoot().getChild(group);
		}
		cache.removeNode(grp.getFqn());
		setUpGroup(group);
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#get(java.lang.String)
	 */
	public Object get(String key, String group) throws DotCacheException {
		group = group.toUpperCase();
		Fqn fqn = Fqn.fromElements(new String[]{group,key});
		Object j = cache.get(fqn, key);
		return j;
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#put(java.lang.String, java.lang.Object, java.lang.String[])
	 */
	public void put(String key, Object content, String group) {
		group = group.toUpperCase();
		Fqn fqn = Fqn.fromElements(new String[] { group, key });
		setUpGroup(group);
		cache.put(fqn, key, content);
	}

	private void setUpGroup(String group){
		Node test = regions.get(group);
		Node grp = cache.getRoot().getChild(group);
		if (grp == null || test == null || !grp.isResident() || !grp.equals(test)) { //TEST THIS LINE
			synchronized (group) {
				if(grp == null){
					Fqn fqn = Fqn.fromString(group);
//					cache.getRegion(fqn, true);
//					grp = cache.getNode(fqn);
					grp = cache.getRoot().addChild(fqn);
					grp.getChildren();
				}
				if (grp != null || !grp.isResident() || !grp.equals(test)) {
					grp.setResident(true);
					regions.put(group, grp);
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.dotmarketing.business.DotCacheAdministrator#remove(java.lang.String)
	 */
	public void remove(String key, String group) {
		group = group.toUpperCase();
		Fqn fqn = Fqn.fromElements(new String[]{group,key});
		if(key !=null){
			cache.remove(fqn, key);
		}
		try{
			if(Config.getBooleanProperty("CACHE_CLUSTER_THROUGH_DB", false)){
				journalAPI.addCacheEntry(key, group);
			}else if(useJgroups){
				Message msg = new Message(null,null,key + ":" + group);
				try {
					channel.send(msg);
				} catch (Exception e) {
					Logger.error(DotJBCacheAdministratorImpl.class,"Unable to send invalidation to cluster : " + e.getMessage(),e);
				}
			}
		}catch(DotDataException e){
			Logger.error(this, "Unable to add journal entry for cluster", e);
		}
	}
	
	public void removeLocalOnly(String key, String group) {
		group = group.toUpperCase();
		Fqn fqn = Fqn.fromElements(new String[]{group,key});
		cache.remove(fqn, key);
	}

	public Set<String> getKeys(String group) {
		group = group.toUpperCase();
		Node n= cache.getNode(Fqn.fromString(group));
		Set<String> result = new HashSet<String>();
		
		if (n!=null) {
			Set<Object> s =  n.getChildrenNames();
			for (Object o : s) {
				String r = o.toString();
				if (r.startsWith(group)) {
					result.add(r.substring(group.length(), r.length()));
				} else {
					result.add(r);
				}
			}
		} else {
			result.add("REGION NOT FOUND !!");
		}
		return result;
	}
	
	public long getSize(String group){
		try{
			Node n = cache.getNode(group);
			return n.getChildren().size();
		}catch (Exception e) {
			return 0;
		}
	}
	
	public List<Map<String, Object>> getCacheStatsList(){
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		Set<Object> s1 = cache.getRoot().getChildrenNames();
		
		Set<String> s2 = new HashSet<String>();
		
		for (Object o : s1) {
			s2.add(o.toString());
		}
		try {
			s2.addAll(H2CacheLoader.getGroups());
		} catch (SQLException e) {
			Logger.error(DotJBCacheAdministratorImpl.class,e.getMessage(),e);
		}
//		for(CacheIndex c :CacheLocator.getCacheIndexes()){
//			s2.add(c.toString());
//		}
		
		for (Object group : s2) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("region", group.toString());
			try{
				Node n = cache.getNode(group.toString());
				m.put("resident", n.isResident());
				m.put("memory", n.getChildren().size());
				try{
					m.put("evictionQueueCapacity", cache.getRegion(n.getFqn(),false).getEvictionRegionConfig().getEventQueueSize());
					m.put("evictionAlgorithm", cache.getRegion(n.getFqn(),false).getEvictionRegionConfig().getEvictionAlgorithmConfig());
				}catch (Exception e) {
					m.put("evictionQueueCapacity", "");
					m.put("evictionAlgorithm", "");
				}
				try{
					m.put("evictionQueueSize", ((org.jboss.cache.RegionImpl)cache.getRegion(n.getFqn(),false)).getEvictionEventQueue().size());
				}catch (NullPointerException e) {
					m.put("evictionQueueSize", -1);
				}
			}catch (Exception e) {
				// do nothing
				m.put("memory", -1);
				m.put("resident", false);
				m.put("evictionQueueCapacity", "");
				m.put("evictionAlgorithm", "");
				m.put("evictionQueueSize", -1);
			}
			m.put("disk", H2CacheLoader.getGroupCount(group.toString()));
			list.add(m);
		}
		return list;
	}
	
	public String getCacheStats(){
		String result = "";
		Set<Object> os = ((org.jboss.cache.NodeSPI)cache.getRoot().getChild(cache.getRoot().getFqn())).getChildrenNames();
		List<String> ret=new ArrayList<String>();
		for (Object o : os) {
			try{
				NodeSPI nodeSPI=(org.jboss.cache.NodeSPI)cache.getRoot().getChild(o);
				int s =nodeSPI.getChildren().size();
				ret.add( o.toString() + " " + s  );
			}catch (Exception e) {
				Logger.warn(this, e.getMessage(),e);
			}
		}
		Collections.sort(ret);
		for (String s:ret) {
			result += s+ "\r\n";
		}
		return result;
	}
	
	
	
	public void shutdown() {
		cache.stop();
		cache.destroy();
	}
	
	
	public List<String> getRegionsList(){
		List<String> arraylist = new ArrayList<String>();
		for(String x : regions.keySet()){
			arraylist.add(x);
		}
		return arraylist;
	}
	
	public JChannel getJGroupsChannel() {
		return channel;
	}
	
	@Override
	public void receive(Message msg) {
		if(msg == null){
			return;
		}
		Object v = msg.getObject();
		if(v == null){
			return;
		}

		if(v.toString().equals("TESTINGCLUSTER")){
			Logger.info(this, "Received Message Ping " + new Date());
		}else{
			invalidateCacheFromCluster(v.toString());
		}
	}
	
	public void viewAccepted(View new_view) {
		super.viewAccepted(new_view);
	    Logger.info(this, "Method view: Cluster View is : " + new_view);
	    AdminLogger.log(DotJBCacheAdministratorImpl.class, "viewAccepted", "Cluster View is : " + new_view);
	}
	
	@Override
	public void suspect(Address mbr) {
		super.suspect(mbr);
		Logger.info(this, "Method suspect: There is a suspected member : " + mbr);
	    AdminLogger.log(DotJBCacheAdministratorImpl.class, "suspect", "There is a suspected member : " + mbr);
	}
	
	public void testCluster(){
		Message msg = new Message(null,null,"TESTINGCLUSTER");
		try {
			channel.send(msg);
			Logger.info(this, "Sending Ping to Cluster " + new Date());
		} catch (ChannelNotConnectedException e) {
			Logger.error(DotJBCacheAdministratorImpl.class,e.getMessage(),e);
		} catch (ChannelClosedException e) {
			Logger.error(DotJBCacheAdministratorImpl.class,e.getMessage(),e);
		}
	}
	
	private void invalidateCacheFromCluster(String k){
		boolean flushMenus = false;
		DotResourceCache vc = CacheLocator.getVeloctyResourceCache();
		String menuGroup = vc.getMenuGroup();

		int i = k.lastIndexOf(":");
		if(i > 0){
			String key = k.substring(0, i);
			String group = k.substring(i+1, k.length());
			if(keys != null){
				if(keys.contains(key)){
					Logger.info(this, "Cluster Eviction of Key : " + key + " With Group : " + group + "From Cache");
				}
			}
			if(groups != null){
				if(groups.contains(group)){
					Logger.info(this, "Cluster Eviction of Key : " + key + " With Group : " + group + "From Cache");
				}
			}
			if(key.contains("dynamic")){
				if(group.equals(menuGroup)){
					flushMenus = true;
				}
			}
			if(!flushMenus){
				if(key.equals("0")){
					if(group.equals(DotCacheAdministrator.ROOT_GOUP)){
						CacheLocator.getCacheAdministrator().flushAlLocalOnlyl();
					}else if(group.equalsIgnoreCase(menuGroup)){
						flushMenus = true;
					}else{
						CacheLocator.getCacheAdministrator().flushGroupLocalOnly(group);
					}
				}else{
					CacheLocator.getCacheAdministrator().removeLocalOnly(key, group);
				}
			}
		}else{
			Logger.error(this, "The cache to locally remove key is invalid. The value was " + k);
		}
		if(flushMenus){
			RefreshMenus.deleteMenusOnFileSystemOnly();
			CacheLocator.getCacheAdministrator().flushGroupLocalOnly(menuGroup);
		}
	}

	/**
	 * @return the groups
	 */
	public TreeSet<String> getGroups() {
		return groups;
	}

	/**
	 * @param groups the groups to set
	 */
	public void setGroups(TreeSet<String> groups) {
		this.groups = groups;
	}

	/**
	 * @return the keys
	 */
	public TreeSet<String> getKeys() {
		return keys;
	}

	/**
	 * @param keys the keys to set
	 */
	public void setKeys(TreeSet<String> keys) {
		this.keys = keys;
	}

    @Override
    public Class getImplementationClass() {
        return DotJBCacheAdministratorImpl.class;
    }

    @Override
    public DotCacheAdministrator getImplementationObject() {
        return this;
    }
	
}
