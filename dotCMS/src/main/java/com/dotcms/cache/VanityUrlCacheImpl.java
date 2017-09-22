package com.dotcms.cache;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.services.VanityUrlServices;
import com.dotcms.vanityurl.business.VanityUrlAPI;
import com.dotcms.vanityurl.model.CacheVanityKey;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotcms.vanityurl.model.SecondaryCacheVanityKey;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.dotcms.util.CollectionsUtils.toImmutableList;

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

    private CachedVanityUrl add(final CacheVanityKey key, final CachedVanityUrl vanityUrl) {
        // Add the key to the cache
        cache.put(key.toString(), vanityUrl, getPrimaryGroup());
        //Add this site to the list of related sites to the cached VanitiesURLs
        this.addSiteId(vanityUrl.getSiteId());

        return vanityUrl;
    }

    @Override
    public CachedVanityUrl get(final CacheVanityKey key) {
        CachedVanityUrl cachedVanityUrl = null;
        try {
            cachedVanityUrl = (CachedVanityUrl) cache.get(key.toString(), getPrimaryGroup());
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

    private void cleanBySecondaryRegion(final SecondaryCacheVanityKey key) {

        final List<CachedVanityUrl> secondaryCachedVanities = this
                .getCachedVanityUrls(key);

        if (null != secondaryCachedVanities) {

            /*
            Before to remove this group from cache lets remove all the related Vanities
             */
            for (CachedVanityUrl toRemove : secondaryCachedVanities) {
                //Now remove from the primary group
                this.remove(new CacheVanityKey(toRemove.getSiteId(),
                        toRemove.getLanguageId(),
                        toRemove.getUrl()).toString());
            }

            this.removeCachedVanityUrls(key.toString());
        }
    }

    @Override
    public void remove(final Contentlet vanityURL) {

        try {

            final String siteId = vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR);

            /*
            First get the records we want to remove from the secondary cache group and
            remove each of those records from the primary cache group, we do this in order to avoid to
            flush completely the primary group.
             */
            cleanBySecondaryRegion(
                    new SecondaryCacheVanityKey(
                      vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                      vanityURL.getLanguageId())
                    );

            /*
            Now we want to clean up the 404 cache records and only touch other sites if we
            are trying to invalidate a SYSTEM_HOST VanityURL, those vanities affects all the sites
             */
            if (Host.SYSTEM_HOST.equals(siteId)) {

                //Get all the sites related to these cached VanityURLs
                final Set<String> sites = getSiteIds();
                if (null != sites) {
                    for (String site : sites) {

                        this.removeSite(vanityURL, site);
                    }
                }
            }

            this.remove(new CacheVanityKey(
                                vanityURL.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR),
                                vanityURL.getLanguageId(),
                                vanityURL.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)
                            ).toString());


        } catch (Exception e) {
            Logger.debug(VanityUrlServices.class,
                    "Error trying to invalidate Vanity URL with identifier:" + vanityURL
                            .getIdentifier(), e);
        }
    }

    private void removeSite(final Contentlet vanityURL, final String site) {

        if (!Host.SYSTEM_HOST.equals(site)) {

            final List<CachedVanityUrl> siteCachedVanityUrl = this
                    .getCachedVanityUrls(new SecondaryCacheVanityKey(site,
                                    vanityURL.getLanguageId()));

            if (null != siteCachedVanityUrl) {
                Boolean deleteForHost = Boolean.FALSE;

                for (CachedVanityUrl toRemove : siteCachedVanityUrl) {

                    //We care only about 404 records
                    if (VanityUrlAPI.CACHE_404_VANITY_URL
                            .equals(toRemove.getVanityUrlId())) {

                        //Remove from the cache this 404 record
                        this.remove(new CacheVanityKey(
                                            toRemove.getSiteId(),
                                            toRemove.getLanguageId(),
                                            toRemove.getUrl()
                                    ).toString());

                        //We found a 404 record, we need to remove this site from the cache
                        deleteForHost = Boolean.TRUE;
                        break;
                    }
                }

                if (deleteForHost) {
                    cleanBySecondaryRegion(
                            new SecondaryCacheVanityKey(
                                    site, vanityURL.getLanguageId()));
                }
            }
        }
    } // removeSite.

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

        final SecondaryCacheVanityKey key =
                new SecondaryCacheVanityKey(vanity.getSiteId(), vanity.getLanguageId());
        try {

            //Update primary Cache
            this.addSingle(vanity);

            //Update Secondary cache
            List<CachedVanityUrl> siteCachedVanityUrl =
                    this.getCachedVanityUrls(key);

            if (null != siteCachedVanityUrl) {
                siteCachedVanityUrl = ImmutableList.<CachedVanityUrl>builder()
                        .add(vanity)
                        .addAll(siteCachedVanityUrl)
                        .build()
                        .stream()
                        .sorted(Comparator.comparing(CachedVanityUrl::getOrder))
                        .collect(toImmutableList());
            } else {
                siteCachedVanityUrl = ImmutableList.<CachedVanityUrl>builder()
                        .add(vanity)
                        .build();
            }

            this.setCachedVanityUrls(key,
                    siteCachedVanityUrl);
        } catch (DotRuntimeException e) {
            Logger.debug(this, "Error trying to update Vanity URL in cache", e);
        }
    }

    @Override
    public void addSingle(final VanityUrl vanity) {

        //Update primary Cache
        this.addSingle(new CachedVanityUrl(vanity));
    }

    @Override
    public void addSingle(final CachedVanityUrl vanity) {

        final CacheVanityKey key =
                new CacheVanityKey(vanity.getSiteId(), vanity.getLanguageId(), vanity.getUrl());

        final CachedVanityUrl currentCachedVanityUrl = this.get(key);
        // we just override if the new vanity has a lower order than the current one.
        final CachedVanityUrl cachedVanityUrlToPut   =
                (null != currentCachedVanityUrl && currentCachedVanityUrl.getOrder() < vanity.getOrder())?
                    currentCachedVanityUrl: vanity;

        //Update primary Cache
        this.add(key, cachedVanityUrlToPut);
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
    public List<CachedVanityUrl> getCachedVanityUrls(final SecondaryCacheVanityKey key) {
        List<CachedVanityUrl> vanityUrlList = null;
        try {
            vanityUrlList = (List<CachedVanityUrl>) cache.get( key.toString(), getCachedVanityUrlGroup());
        } catch (Exception e) {
            Logger.debug(this, "Cache Entry not found", e);
        }

        return vanityUrlList;
    }

    @Override
    public void setCachedVanityUrls(final SecondaryCacheVanityKey secondaryCacheVanityKey,
            final List<CachedVanityUrl> cachedVanityUrlList) {

        cache.put(secondaryCacheVanityKey.toString(),
                            cachedVanityUrlList, getCachedVanityUrlGroup());
    }

    private void removeCachedVanityUrls(final String key) {
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