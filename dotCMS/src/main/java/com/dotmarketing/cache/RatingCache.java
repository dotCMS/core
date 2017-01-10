package com.dotmarketing.cache;

import com.dotmarketing.beans.Rating;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.factories.RatingsFactory;
import com.dotmarketing.util.Logger;

/**
 * 
 * This class is used to cache user ratings in memory based on the user long lived cookie 
 * @author david
 * @author Jason Tesser
 *
 */
public class RatingCache {
    
    /**
     * To add a new rating to the cache
     * @param clientkey The key is used to identify the submitter of the rating in this case we are using the long lived cookie set in the 
     * browser of every dotcms frontend user
     * @param rating The rating to store in the cache
     */
    public static void addToRatingCache(Rating rating) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	cache.put(getPrimaryGroup() + rating.getLongLiveCookiesId() + "-" + rating.getIdentifier(), rating, getPrimaryGroup());
    }
    
    public static void removeRating(Rating rating) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	cache.remove(getPrimaryGroup() + rating.getLongLiveCookiesId() + "-" + rating.getIdentifier(), getPrimaryGroup());
    }
    
    /**
     * To retrieve a rating from the cache
     * @param identifier
     * @param longLiveCookie
     * @return
     */
    public static Rating getRatingFromCache(String identifier, String longLivedCookie) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Rating rt = null;
    	try{
    		rt = (Rating) cache.get(getPrimaryGroup() + longLivedCookie + "-" + identifier,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(RatingCache.class, "Cache Entry not found", e);
    	}
		if(rt == null) {
			rt = RatingsFactory.getRatingByLongCookieId(identifier, longLivedCookie);
			addToRatingCache(rt);
		}
		return rt;
    }
    public static void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    //clear the cache
	    cache.flushGroup(getPrimaryGroup());
	}
	public static String[] getGroups() {
    	String[] groups = {getPrimaryGroup()};
    	return groups;
    }
    
    public static String getPrimaryGroup() {
    	return "RatingCache";
    }
}