package com.dotmarketing.business.jgroups;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.CacheTransportException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jonathan Gamba
 *         Date: 8/14/15
 */
public class NullTransport implements CacheTransport {

  private final AtomicBoolean isInitialized = new AtomicBoolean(false);

  @Override
  public void init() throws CacheTransportException {
    isInitialized.set(true);
  }

  @Override
  public boolean isInitialized() {
    return isInitialized.get();
  }

  @Override
  public boolean shouldReinit() {
    return true;
  }

  @Override
  public void send(String message) throws CacheTransportException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void testCluster() throws CacheTransportException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Map<String, Boolean> validateCacheInCluster(String dateInMillis, int numberServers, int maxWaitSeconds)
      throws CacheTransportException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void shutdown() throws CacheTransportException {
    if (isInitialized.get()) {
      isInitialized.set(false);
    }
  }

  @Override
  public CacheTransportInfo getInfo(){
    return new CacheTransportInfo(){
      @Override
      public String getClusterName() {
        return "NullTransport";
      }

      @Override
      public String getAddress() {
        return ("");
      }

      @Override
      public int getPort() {
        return 0;
      }


      @Override
      public boolean isOpen() {
        return false;
      }

      @Override
      public int getNumberOfNodes() {
        return 0;
      }


      @Override
      public long getReceivedBytes() {
        return 0;
      }

      @Override
      public long getReceivedMessages() {
        return 0;
      }

      @Override
      public long getSentBytes() {
        return 0;
      }

      @Override
      public long getSentMessages() {
        return 0;
      }
    };
  }


}