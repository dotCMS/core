package com.dotcms.vanityurl.business;

import com.dotcms.cache.VanityUrlCache;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.vanityurl.model.VanityUrl;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
;
import com.dotcms.services.VanityUrlServices;
import com.dotcms.util.VanityUrlUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation class for the {@link VanityUrlAPI}.
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public class VanityUrlAPIImpl implements VanityUrlAPI {

    final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final VanityUrlCache vanityURLCache = CacheLocator.getVanityURLCache();
    private final VanityUrl cache404VanityUrl = new DefaultVanityUrl();

    public VanityUrlAPIImpl() {
        cache404VanityUrl.setInode(VanityUrlAPI.CACHE_404_VANITY_URL);
    }

    @Override
    public List<VanityUrl> getAllVanityUrls(final User user) {
        ImmutableList.Builder<VanityUrl> results = ImmutableList.builder();
        try {
            List<Contentlet> contentResults = contentletAPI
                    .search("+baseType:" + BaseContentType.VANITY_URL.getType() + " +working:true",
                            0, 0, StringPool.BLANK, user, false);
            contentResults.stream().forEach((Contentlet con) -> {
                results.add(getVanityUrlFromContentlet(con));
            });
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error searching vanityurl URLs", e);
        }
        return results.build();
    }

    @Override
    public VanityUrl getWorkingVanityUrl(final String uri, final Host host, final long languageId,
            final User user) {
        return getVanityUrlByURI(uri, host, languageId, user, false);
    }

    @Override
    public VanityUrl getLiveVanityUrl(final String uri, final Host host, final long languageId,
            final User user) {
        return getVanityUrlByURI(uri, host, languageId, user, true);
    }

    protected VanityUrl getVanityUrlByURI(final String uri, final Host host, final long languageId,
            final User user, final boolean live) {
        VanityUrl result = vanityURLCache.get(VanityUrlUtil.sanitizeKey(
                host != null && InodeUtils.isSet(host.getInode()) ? host.getIdentifier()
                        : Host.SYSTEM_HOST, VanityUrlUtil.fixURI(uri), languageId));
        if (result == null || !InodeUtils.isSet(result.getInode())) {
            List<VanityUrl> results = new ArrayList<>();
            String hostId = (host != null ? host.getIdentifier() : Host.SYSTEM_HOST);
            try {
                List<Contentlet> contentResults = contentletAPI
                        .search("+baseType:" + BaseContentType.VANITY_URL.getType()
                                + " +languageId:" + languageId + " +conHost:" + hostId
                                + " +vanityUrl:" + VanityUrlUtil.fixURI(uri) + (live ? " +live:true"
                                : " +working:true"), 0, 0, StringPool.BLANK, user, false);
                contentResults.stream().forEach((Contentlet con) -> {
                    VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
                    try {
                        if (con.isLive()) {
                            addToVanityURLCache(vanityUrl);
                        } else {
                            VanityUrlServices.invalidateVanityUrl(vanityUrl);
                        }
                    } catch (DotDataException | DotSecurityException e) {
                        Logger.error(this, "Error processing Vanity Url - contentlet Id:" + con
                                .getIdentifier(), e);
                    }
                    results.add(vanityUrl);
                });

                List<ContentType> vanityUrlContentTypes = APILocator.getContentTypeAPI(user)
                        .findByType(BaseContentType.VANITY_URL);

                if (results.size() == 0 && vanityUrlContentTypes.size() > 0
                        && ((VanityUrlContentType) vanityUrlContentTypes.get(0)).fallback()) {
                    long defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage()
                            .getId();
                    contentResults = contentletAPI
                            .search("+baseType:" + BaseContentType.VANITY_URL.getType()
                                            + " +languageId:" + defaultLanguageId + " +conHost:" + hostId
                                            + " +vanityUrl:" + VanityUrlUtil.fixURI(uri) + (live
                                            ? " +live:true" : " +working:true"), 0, 0, StringPool.BLANK,
                                    user, false);
                    contentResults.stream().forEach((Contentlet con) -> {
                        VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
                        try {
                            if (con.isLive()) {
                                addToVanityURLCache(vanityUrl);
                            } else {
                                VanityUrlServices.invalidateVanityUrl(vanityUrl);
                            }
                        } catch (DotDataException | DotSecurityException e) {
                            Logger.error(this, "Error processing Vanity Url - contentlet Id:" + con
                                    .getIdentifier(), e);
                        }
                        results.add(vanityUrl);
                    });
                }

                if (results.size() == 0) {
                    //Add 404 to cache
                    add404URIToCache(hostId, uri, languageId);
                    results.add(cache404VanityUrl);
                }
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Error searching vanityurl URLs", e);
            }

            result = results.size() > 0 ? results.get(0) : null;
        }
        return result;
    }

    @Override
    public List<VanityUrl> getActiveVanityUrls(final User user) {
        ImmutableList.Builder<VanityUrl> results = new ImmutableList.Builder();
        try {
            List<Contentlet> contentResults = contentletAPI
                    .search("+baseType:" + BaseContentType.VANITY_URL.getType()
                            + " +live:true +deleted:false", 0, 0, StringPool.BLANK, user, false);
            contentResults.stream().forEach((Contentlet con) -> {
                VanityUrl vanityUrl = getVanityUrlFromContentlet(con);
                addToVanityURLCache(vanityUrl);
                results.add(vanityUrl);
            });
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, "Error searching vanityurl URLs", e);
        }
        return results.build();
    }

    @Override
    public VanityUrl getVanityUrlFromContentlet(final Contentlet con) {
        if (con != null) {
            try {
                if (!con.isVanityUrl()) {
                    throw new DotStateException(
                            "Contentlet : " + con.getInode() + " is not a Vanity Url");
                }
            } catch (DotDataException | DotSecurityException e) {
                throw new DotStateException(
                        "Contentlet : " + con.getInode() + " is not a Vanity Url", e);
            }
        } else {
            throw new DotStateException("Contentlet is null");
        }

        DefaultVanityUrl vanityUrl;
        try {
            vanityUrl = (DefaultVanityUrl) CacheLocator.getVanityURLCache()
                    .get(VanityUrlUtil.sanitizeKey(con));
        } catch (DotDataException | DotRuntimeException | DotSecurityException e1) {
            throw new DotStateException(e1);
        }
        if (vanityUrl != null && !VanityUrlAPI.CACHE_404_VANITY_URL.equals(vanityUrl.getInode())) {
            return vanityUrl;
        }
        vanityUrl = new DefaultVanityUrl();
        vanityUrl.setStructureInode(con.getContentTypeId());
        try {
            contentletAPI.copyProperties((Contentlet) vanityUrl, con.getMap());
        } catch (Exception e) {
            throw new DotStateException("Vanity Url Copy Failed", e);
        }
        vanityUrl.setHost(con.getHost());
        if (UtilMethods.isSet(con.getFolder())) {
            try {
                Folder folder = APILocator.getFolderAPI()
                        .find(con.getFolder(), APILocator.systemUser(), false);
                vanityUrl.setFolder(folder.getInode());
            } catch (Exception e) {
                vanityUrl = new DefaultVanityUrl();
                Logger.warn(this, "Unable to convert contentlet to Vanity Url " + con, e);
            }
        }

        return vanityUrl;
    }

    /**
     * Add the Vanity URL to the vanityURLCache
     *
     * @param vanityUrl The vanityurl URL object
     */
    private void addToVanityURLCache(VanityUrl vanityUrl) {
        try {
            if (vanityUrl.isLive()) {
                vanityURLCache.add(VanityUrlUtil.sanitizeKey((Contentlet) vanityUrl), vanityUrl);
            } else {
                VanityUrlServices.invalidateVanityUrl(vanityUrl);
            }
        } catch (DotDataException | DotRuntimeException | DotSecurityException e) {
            Logger.error(this,
                    "Error trying to add Vanity URL identifier:" + vanityUrl.getIdentifier()
                            + " to VanityURLCache", e);
        }
    }

    /**
     * Add a 404 Vanity URL to the vanityURLCache
     *
     * @param hostId The current host Id
     * @param uri The current URI
     * @param languageId The current language Id
     */
    private void add404URIToCache(String hostId, String uri, long languageId) {
        try {
            vanityURLCache
                    .add(VanityUrlUtil.sanitizeKey(hostId, uri, languageId), cache404VanityUrl);
        } catch (DotRuntimeException e) {
            Logger.error(this, "Error trying to add 404 Vanity URL to cache", e);
        }
    }


}
