package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;

public class TemplateCacheImpl extends TemplateCache {
	
	private DotCacheAdministrator cache;
	
	private static String primaryGroup = "TemplateCache";
    // region's name for the cache
    private static String[] groupNames = {primaryGroup};

	public TemplateCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	protected Template add(String key, Template template) {
		key = primaryGroup + key;

        // Add the key to the cache
        cache.put(key, template, primaryGroup);


		return template;
		
	}
	
	@Override
	protected Template get(String key) {
		key = primaryGroup + key;
		Template template = null;
    	try{
    		template = (Template)cache.get(key,primaryGroup);
    	}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return template;	
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
        cache.flushGroup(primaryGroup);
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    public void remove(String key){
    	key = primaryGroup + key;
    	try{
    		cache.remove(key,primaryGroup);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		} 
    }
    public String[] getGroups() {
    	return groupNames;
    }
    public String getPrimaryGroup() {
    	return primaryGroup;
    }
}
