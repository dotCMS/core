package com.dotmarketing.webdav;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import com.dotcms.repackage.com.google.common.cache.Cache;
import com.dotcms.repackage.com.google.common.cache.CacheBuilder;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class FileResourceCache  {

	private Cache<String, Long> cache;

	public FileResourceCache() {
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

    public void clearExpiredEntries() {
    	ConcurrentMap<String, Long> map = cache.asMap();
    	Set<String> keys = map.keySet();
    	Date currentDate = new Date();
    	long minTimeAllowed = Config.getIntProperty("WEBDAV_MIN_TIME_AFTER_PUBLISH_TO_ALLOW_DELETING_OF_FILES", 5);


    	for (String key : keys) {
			try {
				Long time = cache.get(key);

				if(UtilMethods.isSet(time) && ((currentDate.getTime()-time)/1000>=minTimeAllowed)) {
					cache.invalidate(key);
				}
			} catch (Exception e) {
				Logger.error(getClass(), "Error getting FileResourceCache entry. Key: " + key, e);
			}

		}
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
