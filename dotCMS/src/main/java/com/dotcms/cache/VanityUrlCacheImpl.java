package com.dotcms.cache;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.services.VanityUrlServices;
import com.dotcms.util.VanityUrlUtil;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * This class implements {@link VanityUrlCache} the cache for Vanity URLs.
 * Is used to map the Vanity URLs path to the Vanity URL content
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 24, 2017
 */
public class VanityUrlCacheImpl extends VanityUrlCache {

    private DotCacheAdministrator cache;

    private static final String PRIMARY_GROUP = "VanityURLCache";
    private static final String CACHED_VANITY_URL_GROUP = "cachedVanityUrlGroup";
    private static final String HOSTS_GROUP = "hostsGroup";

    private static final String[] groupNames = {PRIMARY_GROUP, CACHED_VANITY_URL_GROUP};

    public VanityUrlCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    private CachedVanityUrl add(final String key, final CachedVanityUrl vanityUrl) {
        // Add the key to the cache
        cache.put(key, vanityUrl, getPrimaryGroup());
        //Add this site to the list of related sites to the cached VanitiesURLs
        this.addSiteId(vanityUrl.getSiteId());

        return vanityUrl;
    }

    @Override
    public CachedVanityUrl get(final String key) {
        CachedVanityUrl cachedVanityUrl = null;
        try {
            cachedVanityUrl = (CachedVanityUrl) cache.get(key, getPrimaryGroup());
        } catch (DotCacheException e) {
            Logger.debug(this, "Cache Entry not found", e);
        }
        return cachedVanityUrl;
    }

    @Override
    public void clearCache() {
        // clear the cache
        for (String cacheGroup : getGroups()) {
            cache.flushGroup(cacheGroup);
        }
    }

    private void cleanBySecondaryRegion(String key) {

        Set<CachedVanityUrl> secondaryCachedVanities = this
                .getCachedVanityUrls(key);

        if (null != secondaryCachedVanities) {

            /*
            Before to remove this group from cache lets remove all the related Vanities
             */
            for (CachedVanityUrl toRemove : secondaryCachedVanities) {
                //Now remove from the primary group
                this.remove(VanityUrlUtil.sanitizeKey(toRemove.getSiteId(), toRemove.getUrl(),
                        toRemove.getLanguageId()));
            }

            this.removeCachedVanityUrls(key);
        }
    }

    @Override
    public void remove(final Contentlet vanityURL) {

        try {

            String siteId = vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR);

            /*
            First get the records we want to remove from the secondary cache group and
            remove each of those records from the primary cache group, we do this in order to avoid to
            flush completely the primary group.
             */
            cleanBySecondaryRegion(VanityUrlUtil.sanitizeSecondCacheKey(vanityURL));

            /*
            Now we want to clean up the 404 cache records and only touch other sites if we
            are trying to invalidate a SYSTEM_HOST VanityURL, those vanities affects all the sites
             */
            if (Host.SYSTEM_HOST.equals(siteId)) {

                //Get all the sites related to these cached VanityURLs
                Set<String> sites = getSiteIds();
                if (null != sites) {
                    for (String site : sites) {

                        if (!Host.SYSTEM_HOST.equals(site)) {

                            Set<CachedVanityUrl> siteCachedVanityUrl = this
                                    .getCachedVanityUrls(VanityUrlUtil
                                            .sanitizeSecondCacheKey(site,
                                                    vanityURL.getLanguageId()));

                            if (null != siteCachedVanityUrl) {
                                Boolean deleteForHost = Boolean.FALSE;

                                for (CachedVanityUrl toRemove : siteCachedVanityUrl) {

                                    //We care only about 404 records
                                    if (VanityUrlAPI.CACHE_404_VANITY_URL
                                            .equals(toRemove.getVanityUrlId())) {

                                        //Remove from the cache this 404 record
                                        this.remove(VanityUrlUtil
                                                .sanitizeKey(toRemove.getSiteId(),
                                                        toRemove.getUrl(),
                                                        toRemove.getLanguageId()));

                                        //We found a 404 record, we need to remove this site from the cache
                                        deleteForHost = Boolean.TRUE;
                                        break;
                                    }
                                }

                                if (deleteForHost) {
                                    cleanBySecondaryRegion(VanityUrlUtil
                                            .sanitizeSecondCacheKey(site,
                                                    vanityURL.getLanguageId()));
                                }
                            }
                        }
                    }
                }
            }

            this.remove(VanityUrlUtil.sanitizeKey(vanityURL));

        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.debug(VanityUrlServices.class,
                    "Error trying to invalidate Vanity URL with identifier:" + vanityURL
                            .getIdentifier(), e);
        }
    }

    @Override
    public void remove(final String key) {
        try {
            cache.remove(key, getPrimaryGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache not able to be removed", e);
        }
    }

    @Override
    public void update(CachedVanityUrl vanity) {

        try {

            //Update primary Cache
            this.add(VanityUrlUtil
                            .sanitizeKey(vanity.getSiteId(), vanity.getUrl(), vanity.getLanguageId()),
                    vanity);

            //Update Secondary cache
            Set<CachedVanityUrl> siteCachedVanityUrl = this.getCachedVanityUrls(VanityUrlUtil
                    .sanitizeSecondCacheKey(vanity.getSiteId(), vanity.getLanguageId()));

            if (null != siteCachedVanityUrl) {
                siteCachedVanityUrl = ImmutableSet.<CachedVanityUrl>builder()
                        .add(vanity)
                        .addAll(siteCachedVanityUrl)
                        .build();
            } else {
                siteCachedVanityUrl = ImmutableSet.<CachedVanityUrl>builder()
                        .add(vanity)
                        .build();
            }

            this.setCachedVanityUrls(vanity.getSiteId(), vanity.getLanguageId(),
                    siteCachedVanityUrl);
        } catch (DotRuntimeException e) {
            Logger.debug(this, "Error trying to update Vanity URL in cache", e);
        }
    }

    @Override
    public void update(VanityUrl vanity) {
        this.update(new CachedVanityUrl(vanity));
    }

    /**
     * Get the cache groups
     *
     * @return array of cache groups
     */
    public String[] getGroups() {
        return groupNames;
    }

    /**
     * get The cache primary group
     *
     * @return primary group name
     */
    public String getPrimaryGroup() {
        return PRIMARY_GROUP;
    }

    private String getCachedVanityUrlGroup() {
        return CACHED_VANITY_URL_GROUP;
    }

    private String getHostsGroup() {
        return HOSTS_GROUP;
    }

    @Override
    public Set<CachedVanityUrl> getCachedVanityUrls(final String key) {
        Set<CachedVanityUrl> vanityUrlList = null;
        try {
            vanityUrlList = (Set<CachedVanityUrl>) cache.get( key, getCachedVanityUrlGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache Entry not found", e);
        }

        return vanityUrlList;
    }

    @Override
    public void setCachedVanityUrls(final String siteId, Long languageId,
            final Set<CachedVanityUrl> cachedVanityUrlList) {
        cache.put(VanityUrlUtil
                        .sanitizeSecondCacheKey(siteId, languageId),
                ImmutableSet.copyOf(cachedVanityUrlList), getCachedVanityUrlGroup());
    }

    private void removeCachedVanityUrls(String key) {
        try {
            cache.remove(key, getCachedVanityUrlGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache not able to be removed", e);
        }
    }

    private void addSiteId(final String hostId) {

        Set<String> hostIds = getSiteIds();
        if (null != hostIds) {
            hostIds = ImmutableSet.<String>builder()
                    .add(hostId)
                    .addAll(hostIds)
                    .build();
        } else {
            hostIds = ImmutableSet.<String>builder()
                    .add(hostId)
                    .build();
        }

        cache.put("vanitiesHostIds", hostIds, getHostsGroup());
    }

    private Set<String> getSiteIds() {
        Set<String> hostIds = null;
        try {
            hostIds = (Set<String>) cache.get("vanitiesHostIds", getHostsGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache Entry not found", e);
        }
        return hostIds;
    }

}