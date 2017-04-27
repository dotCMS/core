package com.dotcms.cluster.business;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jasontesser on 4/5/17.
 */
public class HazelcastUtil {

	private final static String PROPERTY_HAZELCAST_NETWORK_BIND_ADDRESS = "HAZELCAST_NETWORK_BIND_ADDRESS";
	private final static String PROPERTY_HAZELCAST_NETWORK_BIND_PORT = "HAZELCAST_NETWORK_BIND_PORT";
	private final static String PROPERTY_HAZELCAST_NETWORK_TCP_MEMBERS = "HAZELCAST_NETWORK_TCP_MEMBERS";

	public enum HazelcastInstanceType {
	    EMBEDDED("hazelcast-embedded.xml"),
	    CLIENT("hazelcast-client.xml");

	    private final String path;

	    HazelcastInstanceType(String path) {
	        this.path = path;
	    }

	    public String getPath() {
	        return path;
	    }
	}

    private static Map<HazelcastInstanceType, HazelcastInstance> _memberInstances = new ConcurrentHashMap<>();

    final String syncMe = "hazelSync";

    public HazelcastInstance getHazel(HazelcastInstanceType instanceType){
    	return getHazel(instanceType, false);
    }
    public HazelcastInstance getHazel(HazelcastInstanceType instanceType, boolean reInitialize){
        try{
            initMember(instanceType, reInitialize);
        }catch (Exception e) {
            Logger.error(HazelcastUtil.class, "Could not initialize Hazelcast Member", e);
        }
        return _memberInstances.get(instanceType);
    }

    public void shutdown(HazelcastInstanceType instanceType){
        if (_memberInstances.get(instanceType) != null) {
            synchronized (syncMe) {
                if (_memberInstances.get(instanceType) != null) {
                	_memberInstances.get(instanceType).shutdown();
                	_memberInstances.remove(instanceType);
                }
            }
        }
    }

    public void shutdown() {
        for(HazelcastInstanceType instanceType : HazelcastInstanceType.values()){
        	shutdown(instanceType);        	
        }
    }

    private void initMember(HazelcastInstanceType instanceType, boolean reInitialize) {

        if (_memberInstances.get(instanceType) == null || reInitialize) {
            long start = System.currentTimeMillis();
            synchronized (syncMe) {
                if (_memberInstances.get(instanceType) == null || reInitialize) {
                    Logger.info(this, "Setting Up HazelCast ("+ instanceType +")");

                    HazelcastInstance memberInstance = _memberInstances.remove(instanceType);
                    if (memberInstance != null) {
                    	try {
                    		Thread.sleep(1000);
                    	} catch (InterruptedException e) {
                    		Logger.error(this, "Error waiting instance to become vacant", e);
                    	} finally {
                    		memberInstance.shutdown();
                    	}
                    }

                    if (instanceType == HazelcastInstanceType.EMBEDDED) {

                    	memberInstance = newHazelcastInstanceEmbedded(instanceType.getPath());

                    } else if (instanceType == HazelcastInstanceType.CLIENT) {

                    	memberInstance = newHazelcastInstanceClient(instanceType.getPath());
                    }

                    _memberInstances.put(instanceType, memberInstance);

        		    Logger.info(this, "Initialized Hazelcast member "+ memberInstance);
                }
            }
            System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_HAZEL, String.valueOf(System.currentTimeMillis() - start));
        }
    }

	private HazelcastInstance newHazelcastInstanceEmbedded(String xmlPath) {

		InputStream is = getClass().getClassLoader().getResourceAsStream(xmlPath);

		try {
		    XmlConfigBuilder builder = new XmlConfigBuilder(is);

		    com.hazelcast.config.Config config = builder.build();

		    if (Config.getBooleanProperty("CLUSTER_HAZELCAST_AUTOWIRE", true)) {

		    	Map<String, Object> properties = buildPropertiesEmbedded();

			    if (UtilMethods.isSet( properties.get(PROPERTY_HAZELCAST_NETWORK_BIND_PORT) ) &&
			    	UtilMethods.isSet( properties.get(PROPERTY_HAZELCAST_NETWORK_TCP_MEMBERS) ) )
			    {
				    config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);

				    config.getNetworkConfig().setPort(Integer.parseInt((String) properties.get(PROPERTY_HAZELCAST_NETWORK_BIND_PORT)));

			        for(String member : (String[]) properties.get(PROPERTY_HAZELCAST_NETWORK_TCP_MEMBERS)) {
			        	config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(member);
			        }
			    }
		    }

		    return Hazelcast.newHazelcastInstance(config);
		} finally {

			CloseUtils.closeQuietly(is);
		}
	}

	private HazelcastInstance newHazelcastInstanceClient(String xmlPath) {

		InputStream is = getClass().getClassLoader().getResourceAsStream(xmlPath);

		try {
			XmlClientConfigBuilder builder = new XmlClientConfigBuilder(is);

		    com.hazelcast.client.config.ClientConfig config = builder.build();

		    return HazelcastClient.newHazelcastClient(config);
		} finally {

			CloseUtils.closeQuietly(is);
		}
	}

    private Map<String, Object> buildPropertiesEmbedded(){
        Map<String, Object> properties = new HashMap<>();

        // Bind Address
        String bindAddressProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_BIND_ADDRESS, null);
        if (UtilMethods.isSet(bindAddressProperty)) {
        	properties.put(HazelcastUtil.PROPERTY_HAZELCAST_NETWORK_BIND_ADDRESS, bindAddressProperty);
        }

        // Bind Port
        String bindPortProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_BIND_PORT, null);
        if (UtilMethods.isSet(bindPortProperty)) {
        	properties.put(HazelcastUtil.PROPERTY_HAZELCAST_NETWORK_BIND_PORT, bindPortProperty);
        }

        // Initial Hosts
        String initialHostsProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_TCP_INITIAL_HOSTS, null);
        if (UtilMethods.isSet(initialHostsProperty)) {

        	String[] initialHosts = initialHostsProperty.split(",");

        	for(int i = 0; i < initialHosts.length; i++){
				String initialHost = initialHosts[i].trim();

				initialHosts[i] = initialHost.replaceAll("^(.*)\\[(.*)\\]$", "$1:$2");
			}

        	properties.put(HazelcastUtil.PROPERTY_HAZELCAST_NETWORK_TCP_MEMBERS, initialHosts);
        }

        return properties;
    }
}
