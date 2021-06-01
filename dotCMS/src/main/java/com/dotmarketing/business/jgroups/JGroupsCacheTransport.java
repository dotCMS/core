package com.dotmarketing.business.jgroups;

import com.dotcms.cluster.bean.Server;
import com.dotcms.cluster.business.ServerAPI;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.struts.MultiMessageResources;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.collections.map.LRUMap;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

/**
 * @author Jonathan Gamba
 *         Date: 8/14/15
 */
public class JGroupsCacheTransport extends ReceiverAdapter implements CacheTransport {

    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private Map<String, Map<String, Boolean>> cacheStatus;
    private JChannel channel;

    @Override
    public void init ( Server localServer ) throws CacheTransportException {

        Logger.info(this, "***\t Setting up JChannel");

        setProperties();

        try {
            ServerAPI serverAPI = APILocator.getServerAPI();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            cacheStatus = new LRUMap(100);

            String cacheProtocol = Config.getStringProperty("CACHE_PROTOCOL", "tcp");

            String cacheFile = "cache-jgroups-" + cacheProtocol + ".xml";
            Logger.info(this, "***\t Going to load JGroups with this Classpath file " + cacheFile);

            //If already initialized set it again
            if ( channel != null ) {
                channel.disconnect();
            }

            //Create a new channel and configure
            channel = new JChannel(classLoader.getResource(cacheFile));
            channel.setReceiver(this);
            channel.connect(Config.getStringProperty("CACHE_JGROUPS_GROUP_NAME", "dotCMSCluster"));
            channel.setDiscardOwnMessages(true);
            channel.send(new Message(null, null, ChainableCacheAdministratorImpl.TEST_MESSAGE));

            Address channelAddress = channel.getAddress();
            PhysicalAddress physicalAddr = (PhysicalAddress) channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, channelAddress));
            String[] addrParts = physicalAddr.toString().split(":");
            String usedPort = addrParts[addrParts.length - 1];

            
            //Update the cluster_server table with the current server info
            localServer = Server.builder(localServer).withCachePort(Integer.parseInt(usedPort)).build();
            serverAPI.updateServer(localServer);

            Logger.info(this, "***\t " + channel.toString(true));
            Logger.info(this, "***\t Ending JGroups Cluster Setup");

            isInitialized.set(true);

        } catch ( Exception e ) {
            Logger.error(JGroupsCacheTransport.class, "Error initializing jgroups channel: " + e.getMessage(), e);
            throw new CacheTransportException("Error initializing jgroups channel", e);
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    public boolean shouldReinit() {
        return true;
    }

    private void setProperties(){
        // Bind Address
        String bindAddressProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_BIND_ADDRESS, null);
        if (UtilMethods.isSet(bindAddressProperty)) {
        	System.setProperty("jgroups.bind_addr", bindAddressProperty);
        }

        // Bind Port
        String bindPortProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_BIND_PORT, null);
        if (UtilMethods.isSet(bindPortProperty)) {
        	System.setProperty("jgroups.bind_port", bindPortProperty);
        }

        // Initial Hosts
        String initialHostsProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_TCP_INITIAL_HOSTS, null);
        if (UtilMethods.isSet(initialHostsProperty)) {
        	System.setProperty("jgroups.tcpping.initial_hosts",	initialHostsProperty);
        }

        // Multicast Address
        String multiCastAddressProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_UDP_MCAST_ADDRESS, null);
        if (UtilMethods.isSet(multiCastAddressProperty)) {
        	System.setProperty("jgroups.udp.mcast_addr", multiCastAddressProperty);
        }

        // Multicast Port
        String multiCastPortProperty = Config.getStringProperty(WebKeys.DOTCMS_CACHE_TRANSPORT_UDP_MCAST_PORT, null);
        if (UtilMethods.isSet(multiCastPortProperty)) {
        	System.setProperty("jgroups.udp.mcast_port", multiCastPortProperty);
        }
    }
    
    @Override
    public void send ( String message ) throws CacheTransportException {

        Message msg = new Message(null, null, message);
        try {
            channel.send(msg);
        } catch ( Exception e ) {
            Logger.error(JGroupsCacheTransport.class, "Unable to send message: " + e.getMessage(), e);
            throw new CacheTransportException("Unable to send message", e);
        }
    }

    @Override
    public void testCluster () throws CacheTransportException {

        try {
            send(ChainableCacheAdministratorImpl.TEST_MESSAGE);
            Logger.info(this, "Sending Ping to Cluster " + new Date());
        } catch ( Exception e ) {
            Logger.error(JGroupsCacheTransport.class, e.getMessage(), e);
            throw new CacheTransportException("Error testing cluster", e);
        }
    }

    @Override
    public void shutdown () throws CacheTransportException {

        synchronized (this) {
            try {
                if ( channel != null ) {
                    channel.disconnect();
                    channel.close();
                    channel = null;
                }
            } catch ( Exception e ) {
                throw new CacheTransportException(e);
            }

            if (isInitialized.get()) {
                isInitialized.set(false);
            }
        }
    }

    @Override
    public void suspect ( Address mbr ) {
        super.suspect(mbr);
        Logger.info(this, "Method suspect: There is a suspected member : " + mbr);
        Logger.info(JGroupsCacheTransport.class, "suspect + There is a suspected member : " + mbr);
    }

    @Override
    public void viewAccepted ( View new_view ) {
        super.viewAccepted(new_view);
        Logger.info(this, "Method view: Cluster View is : " + new_view);
        Logger.info(ChainableCacheAdministratorImpl.class, "viewAccepted + Cluster View is : " + new_view);
    }

    @Override
    public void receive ( Message msg ) {

        if ( msg == null ) {
            return;
        }

        Object v = msg.getObject();
        if ( v == null ) {
            return;
        }

        if ( v.toString().equals(ChainableCacheAdministratorImpl.TEST_MESSAGE) ) {

            Logger.info(this, "Received Message Ping " + new Date());
            try {
                channel.send(null, "ACK");
            } catch ( Exception e ) {
                Logger.error(JGroupsCacheTransport.class, e.getMessage(), e);
            }

            //Handle when other server is responding to ping.
        } else if ( v.toString().startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE) ) {

            String message = v.toString();
            //Deletes the first part of the message, no longer needed.
            message = message.replace(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE, "");

            //Gets the part of the message that has the Data in Milli.
            String dateInMillis = message.substring(0, message.indexOf(ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR));
            //Gets the last part of the message that has the Server ID.
            String serverID = message.substring(message.lastIndexOf(ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR) + 1);

            synchronized (this) {
                //Creates or updates the Map inside the Map.
                Map<String, Boolean> localMap = cacheStatus.get(dateInMillis);

                if ( localMap == null ) {
                    localMap = new HashMap<String, Boolean>();
                }

                localMap.put(serverID, Boolean.TRUE);

                //Add the Info with the Date in Millis and the Map with Server Info.
                cacheStatus.put(dateInMillis, localMap);
            }

            Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + " SERVER_ID: " + serverID + " DATE_MILLIS: " + dateInMillis);

            //Handle when other server is trying to ping local server.
        } else if ( v.toString().startsWith(ChainableCacheAdministratorImpl.VALIDATE_CACHE) ) {

            String message = v.toString();
            //Deletes the first part of the message, no longer needed.
            message = message.replace(ChainableCacheAdministratorImpl.VALIDATE_CACHE, "");

            //Gets the part of the message that has the Data in Milli.
            String dateInMillis = message;
            //Sends the message back in order to alert the server we are alive.
            try {
                send(ChainableCacheAdministratorImpl.VALIDATE_CACHE_RESPONSE + dateInMillis + ChainableCacheAdministratorImpl.VALIDATE_SEPARATOR + APILocator.getServerAPI().readServerId());
            } catch ( CacheTransportException e ) {
                Logger.error(this.getClass(), "Error sending message", e);
                throw new DotRuntimeException("Error sending message", e);
            }

            Logger.debug(this, ChainableCacheAdministratorImpl.VALIDATE_CACHE + " DATE_MILLIS: " + dateInMillis);

        } else if ( v.toString().equals("ACK") ) {
            Logger.info(this, "ACK Received " + new Date());
        } else if ( v.toString().equals("MultiMessageResources.reload") ) {
            MultiMessageResources messages = (MultiMessageResources) Config.CONTEXT.getAttribute(
                    Globals.MESSAGES_KEY);
            messages.reloadLocally();
        } else if ( v.toString().equals(ChainableCacheAdministratorImpl.DUMMY_TEXT_TO_SEND) ) {
            //Don't do anything is we are only checking sending.
        } else {
            CacheLocator.getCacheAdministrator().invalidateCacheMesageFromCluster(v.toString());
        }
    }



    public Map<String, Boolean> validateCacheInCluster ( String dateInMillis, int numberServers, int maxWaitSeconds ) throws CacheTransportException {

        cacheStatus = new HashMap<>();

        //If we are already in Cluster.
        if ( numberServers > 0 ) {
            //Sends the message to the other servers.
            send(ChainableCacheAdministratorImpl.VALIDATE_CACHE + dateInMillis);

            //Waits for 2 seconds in order all the servers respond.
            int maxWaitTime = maxWaitSeconds * 1000;
            int passedWaitTime = 0;

            //Trying to NOT wait whole 2 seconds for returning the info.
            while ( passedWaitTime <= maxWaitTime ) {
                try {
                    Thread.sleep(10);
                    passedWaitTime += 10;

                    Map<String, Boolean> ourMap = cacheStatus.get(dateInMillis);

                    //No need to wait if we have all server results.
                    if ( ourMap != null && ourMap.size() == numberServers ) {
                        passedWaitTime = maxWaitTime + 1;
                    }

                } catch ( InterruptedException ex ) {
                    Thread.currentThread().interrupt();
                    passedWaitTime = maxWaitTime + 1;
                }
            }
        }

        //Returns the Map with all the info stored by receive() method.
        Map<String, Boolean> mapToReturn = new HashMap<String, Boolean>();

        if ( cacheStatus.get(dateInMillis) != null ) {
            mapToReturn = cacheStatus.get(dateInMillis);
        }

        return mapToReturn;
    }

    public View getView () {
        if ( channel != null ) {
            return channel.getView();
        } else {
            return null;
        }
    }

    public JChannel getChannel () {
        return channel;
    }

    @Override
    public CacheTransportInfo getInfo() {
    	View view = getView();

    	return (view == null) ? null : new CacheTransportInfo(){
    		@Override
    		public String getClusterName() {
    			return channel.getClusterName();
    		}

    		@Override
        	public String getAddress() {
    			return channel.getAddressAsString();
    		}

    		@Override
    		public int getPort() {
                Address channelAddress = channel.getAddress();
                PhysicalAddress physicalAddr = (PhysicalAddress) channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, channelAddress));
                String[] addrParts = physicalAddr.toString().split(":");
                String usedPort = addrParts[addrParts.length - 1];

    			return Integer.parseInt(usedPort);
    		}


    		@Override
    		public boolean isOpen() {
    			return channel.isOpen();
    		}

    		@Override
    		public int getNumberOfNodes() {
    			return view.getMembers().size();
        	}


    		@Override
    		public long getReceivedBytes() {
    			return channel.getReceivedBytes();
    		}

    		@Override
    		public long getReceivedMessages() {
    			return channel.getReceivedMessages();
    		}

    		@Override
    		public long getSentBytes() {
    			return channel.getSentBytes();
    		}

    		@Override
    		public long getSentMessages() {
    			return channel.getSentMessages();
    		}
    	};
    }
}