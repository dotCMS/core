package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.TranslatedQuery;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.rendering.velocity.services.PageLoader;
import com.dotcms.rendering.velocity.services.SiteLoader;
import com.dotcms.uuid.shorty.ShortyIdCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class ContentletCacheImpl extends ContentletCache {

	private DotCacheAdministrator cache;

	private static final String primaryGroup = "ContentletCache";

	private static final String translatedQueryGroup = "TranslatedQueryCache";
	// region's name for the cache

	private static final String timeMachineGroup = "TimeMachineCache";

	private static final String[] groupNames = {primaryGroup, HostCache.PRIMARY_GROUP, translatedQueryGroup, timeMachineGroup};

	public ContentletCacheImpl() {
		cache = CacheLocator.getCacheAdministrator();
	}

	@Override
	public void addTranslatedQuery(String key, TranslatedQuery translatedQuery) {
		cache.put(key, translatedQuery, translatedQueryGroup);
	}

	@Override
	public TranslatedQuery getTranslatedQuery(String key) {
		Object o = null;
		try {
			o = cache.get(key, translatedQueryGroup);
		} catch (DotCacheException e) {
			Logger.error(ContentletCacheImpl.class,e.getMessage(),e);
		}
		if(o==null){
			return null;
		}else{
			return (TranslatedQuery)o;
		}
	}

	@Override
	public com.dotmarketing.portlets.contentlet.model.Contentlet add(String key, com.dotmarketing.portlets.contentlet.model.Contentlet content) {

        if(DbConnectionFactory.inTransaction()) {
            return content;
        }

		key = primaryGroup + key;

		// Add the key to the cache
		cache.put(key, content, primaryGroup);

		return content;

	}

	@Override
	public com.dotmarketing.portlets.contentlet.model.Contentlet get(String key) {

        if(DbConnectionFactory.inTransaction()) {
            return null;
        }

		key = primaryGroup + key;
		com.dotmarketing.portlets.contentlet.model.Contentlet content = null;
		try{
			content = (com.dotmarketing.portlets.contentlet.model.Contentlet)cache.get(key,primaryGroup);
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return content;
	}

	/* (non-Javadoc)
     * @see com.dotmarketing.business.PermissionCache#clearCache()
     */
	public void clearCache() {
		// clear the cache
		for(String group : groupNames){
			cache.flushGroup(group);
		}
	}
    public void remove(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet){
        remove(contentlet.getInode());

        if(contentlet.getContentType() instanceof PageContentType ) {
            new PageLoader().invalidate(contentlet);
            
            
        }
        if ("host".equalsIgnoreCase(contentlet.getContentType().variable())) {
            new SiteLoader().invalidate(new Host(contentlet));
        }

        //Invalidating relationship cache
        CacheLocator.getRelationshipCache().removeRelatedContentMap(contentlet.getIdentifier());
    }
	/* (non-Javadoc)
     * @see com.dotmarketing.business.PermissionCache#remove(java.lang.String)
     */
	public void remove(final String key){

		final String myKey = primaryGroup + key;
		com.dotmarketing.portlets.contentlet.model.Contentlet content = null;

		try {

			content = (com.dotmarketing.portlets.contentlet.model.Contentlet)cache.get(myKey,primaryGroup);

			if(content != null && content.isVanityUrl()){
				APILocator.getVanityUrlAPI().invalidateVanityUrl(content);
			}
		
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}

		try {
			cache.remove(myKey,primaryGroup);
		} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}

		final Host host = CacheLocator.getHostCache().getById(key, false);
		if(host != null){
			CacheLocator.getHostCache().remove(host);
		}

		if (content != null && content.getIdentifier() != null)
		    CacheLocator.getHTMLPageCache().remove(content.getIdentifier());

		new ShortyIdCache().remove(key);
		
        //Delete query cache when a new content has been reindexed
        CacheLocator.getESQueryCache().clearCache();
	}

	public String[] getGroups() {
		return groupNames;
	}
	public String getPrimaryGroup() {
		return primaryGroup;
	}

    @Override
    public Contentlet add(Contentlet content) {
        return add(content.getInode(), content);
    }

	@SuppressWarnings("unchecked")
	private Map<String, Contentlet> getTimeMachineByIdentifierCache(final String identifier) {

		Map<String, Map<String, Contentlet>> byIdentifierCache =
				(Map<String, Map<String, Contentlet>>) Try.of(()->cache.get(identifier, timeMachineGroup)).getOrElse(Map.of());

		if (byIdentifierCache == null) {
			byIdentifierCache = new ConcurrentHashMap<>();
			cache.put(identifier, byIdentifierCache, timeMachineGroup);
		}

		return byIdentifierCache.computeIfAbsent(
				identifier, k -> new ConcurrentHashMap<>());
	}

	@Override
	public Contentlet addTimeMachine(final Date timeMachineDate, String identifier, final Contentlet content) {
		final String key = content.getVariantId() + StringPool.COLON + timeMachineDate.getTime();
		getTimeMachineByIdentifierCache(identifier).put(key, content);
		return content;
	}

	@Override
	public Optional<Contentlet> getTimeMachine(final Date timeMachineDate, final String identifier, final String variant) {
		if(DbConnectionFactory.inTransaction()) {
			Logger.debug(this, "In transaction, returning empty");
			return Optional.empty();
		}
		final String key = variant + StringPool.COLON + timeMachineDate.getTime();
		return Optional.ofNullable(getTimeMachineByIdentifierCache(identifier).get(key));
	}

	@Override
	public void invalidateTimeMachine(Contentlet content) {
        cache.remove(content.getIdentifier(), timeMachineGroup);
	}
}
