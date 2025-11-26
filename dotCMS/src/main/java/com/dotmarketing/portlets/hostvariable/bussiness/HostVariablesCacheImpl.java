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

	private static final String ALL_SITE_VARIABLES_KEY = "ALL_SITE_VARIABLES";

    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public HostVariablesCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected void putVariablesForSite(final String siteId, final List<HostVariable> list) {
		String key = primaryGroup + siteId;
		cache.put(key, list, getPrimaryGroup());
	}

	@Override
	protected List<HostVariable> getVariablesForSite(final String siteId) {

		String key = primaryGroup + siteId;

		List<HostVariable> variables = null;

		try {
			variables = (List<HostVariable>) cache.get(key, primaryGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Error retrieving cache entry", e);
		}

		return variables;
	}

	@Override
	protected void clearVariablesForSite(final String siteId) {
		String key = primaryGroup + siteId;
		cache.remove(key, primaryGroup);
		cache.remove(ALL_SITE_VARIABLES_KEY, primaryGroup);
	}

	@Override
	protected List<HostVariable> put(List<HostVariable> list) {

		// Add the key to the cache
		cache.put(ALL_SITE_VARIABLES_KEY, list, primaryGroup);


		return list;
		
	}
	
	@Override
	protected List<HostVariable> getAll() {

		List<HostVariable> l = null;
    	try{
			l = (List<HostVariable>) cache.get(ALL_SITE_VARIABLES_KEY, primaryGroup);
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
