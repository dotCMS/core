package com.dotmarketing.business.cache.provider.guava;

import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jonathan Gamba
 *         Date: 9/2/15
 */
public class TestCacheProvider extends CacheProvider {

    private static final long serialVersionUID = -388836120327525960L;

    private Boolean isInitialized = false;

    @Override
    public String getName () {
        return "Test Cache Provider";
    }

    @Override
    public String getKey () {
        return "testCacheProvider";
    }

    @Override
    public void init () {
        isInitialized = true;
        Logger.info(this.getClass(), "===== Initializing [" + getName() + "].");
    }

    @Override
    public boolean isInitialized () throws Exception {
        return isInitialized;
    }

    @Override
    public void put ( String group, String key, Object content ) {
        Logger.info(this.getClass(), "===== Calling put for [" + getName() + "] - With: Group [" + group + "] - Key [" + key + "].");
    }

    @Override
    public Object get ( String group, String key ) {
        Logger.info(this.getClass(), "===== Calling get for [" + getName() + "] - With: Group [" + group + "] - Key [" + key + "].");
        return null;
    }

    @Override
    public void remove ( String group, String key ) {
        Logger.info(this.getClass(), "===== Calling remove for [" + getName() + "] - With: Group [" + group + "] - Key [" + key + "].");
    }

    @Override
    public void remove ( String group ) {
        Logger.info(this.getClass(), "===== Calling remove for [" + getName() + "] - With: Group [" + group + "].");
    }

    @Override
    public void removeAll () {
        Logger.info(this.getClass(), "===== Calling removeAll [" + getName() + "].");
    }

    @Override
    public Set<String> getKeys ( String group ) {
        Logger.info(this.getClass(), "===== Calling getKeys [" + getName() + "].");
        return null;
    }

    @Override
    public Set<String> getGroups () {
        Logger.info(this.getClass(), "===== Calling getGroups [" + getName() + "].");
        return null;
    }

    @Override
    public List<Map<String, Object>> getStats () {
        Logger.info(this.getClass(), "===== Calling getCacheStats [" + getName() + "].");
        return null;
    }

    @Override
    public void shutdown () {
        isInitialized = false;
        Logger.info(this.getClass(), "===== Calling shutdown [" + getName() + "].");
    }

}