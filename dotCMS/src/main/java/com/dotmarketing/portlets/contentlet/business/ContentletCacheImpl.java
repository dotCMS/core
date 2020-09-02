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
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.Map;

/**
 * @author Jason Tesser
 * @since 1.6
 */
public class ContentletCacheImpl extends ContentletCache {

	private DotCacheAdministrator cache;

	private String primaryGroup = "ContentletCache";
	private String metadataGroup = "FileAssetMetadataCache";
	private String translatedQueryGroup = "TranslatedQueryCache";
	// region's name for the cache
	private String[] groupNames = {primaryGroup, HostCache.PRIMARY_GROUP, metadataGroup,translatedQueryGroup};

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

	public void addMetadataMap(final String key, final Map<String, Object> metadataMap) {

		cache.put(META_DATA_MAP_KEY + key, UtilMethods.isSet(metadataMap)?
				metadataMap:EMPTY_METADATA_MAP, metadataGroup);
	}

	public  Map<String, Object> getMetadataMap(final String key) {

		Map<String, Object>  metadata = null;
		try {
			metadata=(Map<String, Object> )cache.get(META_DATA_MAP_KEY + key, metadataGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return metadata;
	}

	@Override
	public void addMetadata(String key, String metadata) {
		key = metadataGroup + key;
		if(!UtilMethods.isSet(metadata))
			metadata=EMPTY_METADATA;
		cache.put(key, metadata, metadataGroup);
	}

	@Override
	public void addMetadata(String key, com.dotmarketing.portlets.contentlet.model.Contentlet content) {
		// http://jira.dotmarketing.net/browse/DOTCMS-7335
		// we need metadata in other cache region
		if("CACHE_404_CONTENTLET".equals(content.getInode())){
			return;
		}
		Structure st=content.getStructure();
		if(st!=null && st.getStructureType()==Structure.STRUCTURE_TYPE_FILEASSET) {
			Field f=st.getFieldVar(FileAssetAPI.META_DATA_FIELD);
			if(f!=null && UtilMethods.isSet(f.getInode())) {
				String metadata=(String)content.get(FileAssetAPI.META_DATA_FIELD);
				addMetadata(key, metadata);
				content.setStringProperty(FileAssetAPI.META_DATA_FIELD, ContentletCache.CACHED_METADATA);
			}
		}
	}

	@Override
	public String getMetadata(String key) {
		key = metadataGroup + key;
		String metadata=null;
		try {
			metadata=(String)cache.get(key, metadataGroup);
		} catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}
		return metadata;
	}

	@Override
	public com.dotmarketing.portlets.contentlet.model.Contentlet add(String key, com.dotmarketing.portlets.contentlet.model.Contentlet content) {

		addMetadata(key, content);

		key = primaryGroup + key;

		// Add the key to the cache
		cache.put(key, content,primaryGroup);


		return content;

	}

	@Override
	public com.dotmarketing.portlets.contentlet.model.Contentlet get(String key) {
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
		final String metadataKey = metadataGroup + key;
		com.dotmarketing.portlets.contentlet.model.Contentlet content = null;

		try {

			content = (com.dotmarketing.portlets.contentlet.model.Contentlet)cache.get(key,primaryGroup);

			if(content != null && content.isVanityUrl()){
				APILocator.getVanityUrlAPI().invalidateVanityUrl(content);
			}
		
		}catch (DotCacheException e) {
			Logger.debug(this, "Cache Entry not found", e);
		}

		try {
			cache.remove(myKey,primaryGroup);
			cache.remove(metadataKey,metadataGroup);
		} catch (Exception e) {
			Logger.debug(this, "Cache not able to be removed", e);
		}

		final Host host = CacheLocator.getHostCache().get(key);
		if(host != null){
			CacheLocator.getHostCache().remove(host);
		}
		CacheLocator.getHTMLPageCache().remove(key);
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
