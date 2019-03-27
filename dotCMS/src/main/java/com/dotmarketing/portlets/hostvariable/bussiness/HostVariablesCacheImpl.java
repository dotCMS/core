package com.dotmarketing.portlets.hostvariable.bussiness;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.Logger;

public class HostVariablesCacheImpl extends HostVariablesCache {
	
	private DotCacheAdministrator cache;
	
	private static String primaryGroup = "HostVariablesCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public HostVariablesCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected List<HostVariable> put(List<HostVariable> list) {
		String key = primaryGroup + "all";
		
        // Add the key to the cache
        cache.put(key, list, primaryGroup);


		return list;
		
	}
	
	@Override
	protected List<HostVariable> getAll() {
		String key = primaryGroup + "all";
		List<HostVariable> l = null;
    	try{
    		l = (List<HostVariable>)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return l;	
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(primaryGroup);
    }

    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
}
