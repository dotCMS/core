package com.dotcms.cluster.business;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.io.InputStream;
import java.util.Map;

/**
 * Created by jasontesser on 4/5/17.
 */
public class HazelcastUtil {

	public final static String PROPERTY_HAZELCAST_NETWORK_BIND_ADDRESS = "HAZELCAST_NETWORK_BIND_ADDRESS";
	public final static String PROPERTY_HAZELCAST_NETWORK_BIND_PORT = "HAZELCAST_NETWORK_BIND_PORT";
	public final static String PROPERTY_HAZELCAST_NETWORK_TCP_MEMBERS = "HAZELCAST_NETWORK_TCP_MEMBERS";

    private static HazelcastInstance _memberInstance;
    final String syncMe = "hazelSync";

    public HazelcastInstance getHazel(){
        return getHazel(null);
    }

    public HazelcastInstance getHazel(Map<String, Object> properties){
        return getHazel("hazelcast-embedded.xml", properties);
    }

    public HazelcastInstance getHazel(String xmlPath, Map<String, Object> properties) {
        try{
            initMember(xmlPath, properties);
        }catch (Exception e) {
            Logger.error(HazelcastUtil.class, "Could not initialize Hazelcast Member", e);
        }
        return _memberInstance;
    }

    public void shutdown(){
        if (_memberInstance != null) {
            synchronized (syncMe) {
                if ( _memberInstance != null) {
                    _memberInstance.shutdown();
                    _memberInstance = null;
                }
            }
        }
    }

    private void initMember(String xmlPath, Map<String, Object> properties) {

        if (_memberInstance == null) {
            long start = System.currentTimeMillis();
            synchronized (syncMe) {
                if ( _memberInstance == null) {
                    Logger.info(this, "Setting Up HazelCast");

                    Config config = buildConfig(xmlPath, properties);

        		    _memberInstance = Hazelcast.newHazelcastInstance(config);
                }
            }
            System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_HAZEL, String.valueOf(System.currentTimeMillis() - start));
        }
    }

	private Config buildConfig(String xmlPath, Map<String, Object> properties) {

		InputStream is = getClass().getClassLoader().getResourceAsStream(xmlPath);

		try {
		    XmlConfigBuilder builder = new XmlConfigBuilder(is);

		    Config config = builder.build();

		    if (properties != null) {

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

		    return config;
		} finally {
	        try {
	            is.close();
	        }catch (Exception e){
	            Logger.error(Hazelcast.class, "Unable to close inputstream for Hazel xml", e);
	        }
		}
	}
}
