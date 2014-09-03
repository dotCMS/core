package com.dotmarketing.webdav;

import java.util.concurrent.Callable;

import com.dotcms.repackage.com.google.common.cache.Cache;
import com.dotcms.repackage.com.google.common.cache.CacheBuilder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class ResourceCache {

	private Cache<String, Long> cache;

	public ResourceCache() {
		CacheBuilder<Object, Object> cb  = CacheBuilder
				.newBuilder()
				.maximumSize(100)
				.concurrencyLevel(Config.getIntProperty("cache.concurrencylevel", 32));



		cache = cb.build();
	}

	protected Long add(String key, Long timeOfPublishing) {

        // Add the key to the cache
        cache.put(key, timeOfPublishing);


		return timeOfPublishing;

	}

	protected Long get(String key) {
		Long timeOfPublishing = null;
    	try{
    		timeOfPublishing = (Long)cache.get(key, new NullCallable());
    	}catch (Exception e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
        return timeOfPublishing;
	}

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#clearCache()
	 */
    public void clearCache() {
        // clear the cache
//        cache.
    }

    /* (non-Javadoc)
	 * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
	 */
    public void remove(String key){
    	try{
    		cache.invalidate(key);
    	}catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}
    }

    private class NullCallable implements Callable{

		public Object call() throws Exception {
			return null;
		}


	}

}
