package com.dotmarketing.plugin.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.plugin.model.Plugin;
import com.dotmarketing.plugin.model.PluginProperty;
import com.dotmarketing.util.Logger;

/**
 * @author Jason Tesser
 * @since 1.6.5c
 */
public class PluginCacheImpl extends PluginCache {
	
	private DotCacheAdministrator cache;
	
	private String pluginGroup = "PluginCache";
	private String propertyGroup = "PropertyCache";
    // region's name for the cache
    private String[] groupNames = {pluginGroup, propertyGroup};

	public PluginCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected com.dotmarketing.plugin.model.Plugin add(com.dotmarketing.plugin.model.Plugin plugin) {
		String key = pluginGroup + plugin.getId();
        // Add the key to the cache
        cache.put(key, plugin,pluginGroup);
        try {
			return (Plugin)cache.get(key,pluginGroup);
		} catch (DotCacheException e) {
			Logger.warn(this,"Cache Entry not found after adding", e);
			return plugin;
		}
	}
	
	@Override
	protected com.dotmarketing.plugin.model.Plugin get(String pluginId) {
		String key = pluginGroup + pluginId;
    	Plugin plugin = null;
    	try{
    		plugin = (Plugin)cache.get(key,pluginGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return plugin;	
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
	public void clearCache() {
        // clear the cache
        cache.flushGroup(pluginGroup);
        cache.flushGroup(propertyGroup);
    }

    public String[] getGroups() {
    	return groupNames;
    }

	@Override
	protected PluginProperty addProperty(PluginProperty pluginProperty) {
		String key = propertyGroup + pluginProperty.getPluginId() + ":" + pluginProperty.getPropkey();
        // Add the key to the cache
        cache.put(key, pluginProperty,propertyGroup);

        try {
			return (PluginProperty)cache.get(key,propertyGroup);
		} catch (DotCacheException e) {
			Logger.warn(this,"Cache Entry not found after adding", e);
			return pluginProperty;
		}
	}

	@Override
	protected void clearPluginCache() {
		cache.flushGroup(pluginGroup);
	}

	@Override
	protected void clearPropertyCache() {
		cache.flushGroup(propertyGroup);
	}

	@Override
	protected PluginProperty getProperty(String pluginId, String propertyKey) {
		String key = propertyGroup + pluginId + ":" + propertyKey;
		PluginProperty value = null;
    	try{
    		value = (PluginProperty)cache.get(key,propertyGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return value;	
	}

	@Override
	protected void removePlugin(String pluginId) {
		String key = pluginGroup + pluginId;
    	try{
    		cache.remove(key,pluginGroup);
    	}catch (Exception e) {
			Logger.debug(this, e.getMessage(), e);
		} 
	}

	@Override
	protected void removePluginProperty(PluginProperty pluginProperty) {
		String key = propertyGroup + pluginProperty.getPluginId() + ":" + pluginProperty.getPropkey();
    	try{
    		cache.remove(key,propertyGroup);
    	}catch (Exception e) {
			Logger.debug(this, e.getMessage(), e);
		} 
	}

	public String getPrimaryGroup() {
		return pluginGroup;
	}
}
