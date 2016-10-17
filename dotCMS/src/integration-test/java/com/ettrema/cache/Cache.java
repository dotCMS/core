package com.ettrema.cache;

/**
*
* @author brad
*/
public interface Cache<K, T> {

   String getName();

   void flush();

   T get( K key );

   void put( K key, T val );

   void remove( K key );

   long getHits();

   long getMisses();

   /**
    * Number of objects in cache if known, otherwise null
    *
    * @return
    */
   Long getSize();
}