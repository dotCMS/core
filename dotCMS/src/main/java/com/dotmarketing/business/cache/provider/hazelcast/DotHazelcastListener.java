package com.dotmarketing.business.cache.provider.hazelcast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.MapEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.map.listener.MapClearedListener;
import com.hazelcast.map.listener.MapEvictedListener;

public class DotHazelcastListener implements EntryAddedListener<String, String>, 
                                            EntryRemovedListener<String, String>, 
                                            EntryUpdatedListener<String, String>, 
                                            EntryEvictedListener<String, String> , 
                                            MapEvictedListener, 
                                            MapClearedListener   {
      @Override
      public void entryAdded( EntryEvent<String, String> event ) {
        System.out.println( "Entry Added:" + event );
      }

      @Override
      public void entryRemoved( EntryEvent<String, String> event ) {
        System.out.println( "Entry Removed:" + event );
      }

      @Override
      public void entryUpdated( EntryEvent<String, String> event ) {
        System.out.println( "Entry Updated:" + event );
      }

      @Override
      public void entryEvicted( EntryEvent<String, String> event ) {
        System.out.println( "Entry Evicted:" + event );
      }

      @Override
      public void mapEvicted( MapEvent event ) {
        System.out.println( "Map Evicted:" + event );
      }

      @Override
      public void mapCleared( MapEvent event ) {
        System.out.println( "Map Cleared:" + event );
      }

    
  

}
