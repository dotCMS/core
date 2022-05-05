package com.dotcms.vanityurl.cache;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
import java.util.Optional;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

/**
 * This class implements {@link VanityUrlCache} the cache for Vanity URLs. Is used to map the Vanity
 * URLs path to the Vanity URL content
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 24, 2017
 */
public class VanityUrlCacheImpl extends VanityUrlCache {

    private DotCacheAdministrator cache;

    private static final String VANITY_URL_SITE_GROUP = "VanityURLSiteCache";
    private static final String VANITY_URL_DIRECT_GROUP = "VanityURLDirectCache";



    public VanityUrlCacheImpl() {
        cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public String getPrimaryGroup() {

        return VANITY_URL_SITE_GROUP;
    }

    @Override
    public String[] getGroups() {

        return new String[] {VANITY_URL_SITE_GROUP, VANITY_URL_DIRECT_GROUP};
    }

    @Override
    public void clearCache() {
        cache.flushGroup(VANITY_URL_SITE_GROUP);
        cache.flushGroup(VANITY_URL_DIRECT_GROUP);

    }

    @Override
    public void remove(final Contentlet vanityURL) {
        if (vanityURL == null || !vanityURL.isVanityUrl()) {
            return;
        }
        Host host = Try.of(() -> APILocator.getHostAPI().find(vanityURL.getHost(), APILocator.systemUser(), false)).getOrNull();
        Language lang = Try.of(() -> APILocator.getLanguageAPI().getLanguage(vanityURL.getLanguageId())).getOrNull();
        if (host == null || lang == null) {
            return;
        }
        remove(host, lang);
    }


    @Override
    public void remove(final Host vanityHost) {
        if (vanityHost == null) {
            return;
        }

        final List<Language> langs = APILocator.getLanguageAPI().getLanguages();

        for (Language lang : langs) {
            remove(vanityHost, lang);
        }

    }


    @Override
    public void remove(final Host vanityHost, final Language lang) {
        if (vanityHost == null || lang == null) {
            return;
        }

        remove(vanityHost.getIdentifier(), lang.getId());
    }


    @Override
    public void remove(final String hostId, final long langId) {
        if (hostId == null) {
            return;
        }

        cache.remove(key(hostId, langId), VANITY_URL_SITE_GROUP);
        cache.flushGroup(VANITY_URL_DIRECT_GROUP);

    }


    @Override
    public void putSiteMappings(final Host host, final Language lang, final List<CachedVanityUrl> vanityURLs) {
        if (host == null || host.getIdentifier() == null || lang == null || vanityURLs == null) {
            return;
        }
        cache.put(key(host, lang), vanityURLs, VANITY_URL_SITE_GROUP);
    }

    @Override
    public List<CachedVanityUrl> getSiteMappings(final Host host, final Language lang) {
        if (host == null || lang == null || host.getIdentifier() == null) {
            return null;
        }
        final String key = key(host, lang);

        return (List<CachedVanityUrl>) cache.getNoThrow(key, VANITY_URL_SITE_GROUP);

    }

    @Override
    public Optional<CachedVanityUrl> getDirectMapping(final String url, final Host host, final Language lang) {

        final CachedVanityUrl cachedVanityUrl = (CachedVanityUrl) cache.getNoThrow(key(host, lang, url), VANITY_URL_DIRECT_GROUP);

        if (null == cachedVanityUrl) {
            return null;
        }

        return cachedVanityUrl == NOT_FOUND404 || NOT_FOUND404.vanityUrlId.equals(cachedVanityUrl.vanityUrlId)?
                Optional.empty(): Optional.ofNullable(cachedVanityUrl);
    }
    



    @Override
    public void putDirectMapping(final Host host, final Language lang, final String url, final Optional<CachedVanityUrl> vanityUrl) {
        if(vanityUrl==null) {
            throw new DotRuntimeException("Unable to put null value in cache:" + key(host, lang, url));
        }
        cache.put(key(host, lang, url), vanityUrl.isPresent()?vanityUrl.get():NOT_FOUND404, VANITY_URL_DIRECT_GROUP);

    }


    String key(final Host host, final Language lang) {
        return key(host, lang, null);
    }

    String key(final Host host, final Language lang, final String url) {
        if(host==null || lang==null) {
            throw new DotRuntimeException("Host or language are null - host:" + host  + " lang:" + lang);
        }

        return key(host.getIdentifier(), lang.getId(), url);
    }

    private String key(final String hostId, final long langId) {
        return key(hostId, langId, null);
    }

    private String key(final String hostId, final long langId, final String url) {
        if(hostId == null) {
            throw new DotRuntimeException("Host or language are null - host:" + hostId  + " lang:" + langId);
        }
        return hostId + StringPool.UNDERLINE + langId + StringPool.UNDERLINE + (url != null ? url : "");
    }
}
