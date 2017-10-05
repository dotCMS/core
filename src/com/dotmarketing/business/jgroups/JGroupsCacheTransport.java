package com.dotmarketing.business.jgroups;

import com.dotcms.cluster.bean.Server;
import com.dotcms.repackage.org.jgroups.*;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import java.util.Map;

/**
 * @author Jonathan Gamba
 *         Date: 8/14/15
 */
public class JGroupsCacheTransport extends ReceiverAdapter implements CacheTransport {

    @Override
    public void init ( Server localServer ) throws CacheTransportException {

    }

    @Override
    public void send ( String message ) throws CacheTransportException {

    }

    @Override
    public void testCluster () throws CacheTransportException {

    }

    @Override
    public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds) throws CacheTransportException {
        return null;
    }

    @Override
    public void shutdown () throws CacheTransportException {

    }

    @Override
    public void suspect ( Address mbr ) {

    }

    @Override
    public void viewAccepted ( View new_view ) {

    }

    @Override
    public void receive ( Message msg ) {

    }

}