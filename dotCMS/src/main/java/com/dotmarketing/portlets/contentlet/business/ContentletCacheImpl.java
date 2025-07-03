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

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class ContentletCacheImpl extends ContentletCache {

	private DotCacheAdministrator cache;

	private String primaryGroup = "ContentletCache";

	private String translatedQueryGroup = "TranslatedQueryCache";
	// region's name for the cache
	private String[] groupNames = {primaryGroup, HostCache.PRIMARY_GROUP, translatedQueryGroup};

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
		if (content.getIdentifier().equals("b9b88559-335c-47cd-a53f-979181d39153") && 1 == content.getLanguageId()) {
			Logger.warn(this, "==========================================================================");
			Logger.warn(this, "==== Expected URL content is being added/updated in cache ====");
			Logger.warn(this, "==========================================================================");
			Logger.warn(this, "-> URL value being set = " + content.getStringProperty("url"));
			Logger.warn(this, "-> Inode = " + content.getInode());
			Logger.warn(this, "-> Site ID = " + content.getHost());
			Logger.warn(this, "-> Mod User = " + content.getModUser());
			Logger.warn(this, "-> Mod Date = " + content.getModDate());
			Thread.dumpStack();
		}
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
}
