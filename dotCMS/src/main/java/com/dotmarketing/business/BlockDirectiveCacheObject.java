package com.dotmarketing.business;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class BlockDirectiveCacheObject implements Serializable {


    private static final long serialVersionUID = 1L;
    private final long created;
    private final long ttl;
    private final ConcurrentHashMap<String, Serializable> map;


    public long getTtl() {
        return this.ttl;
    }

    public BlockDirectiveCacheObject(Map<String, Serializable> map, int  ttl) {

        this.ttl = ttl;
        this.created = System.currentTimeMillis();
        this.map = (map instanceof ConcurrentHashMap) ? (ConcurrentHashMap) map : new ConcurrentHashMap(map);
    }



    public long getCreated() {
        return this.created;
    }

    public Map<String, Serializable> getMap(){
        return this.map;
    }


}
