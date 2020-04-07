package com.dotcms.vanityurl.model;

import static com.dotcms.vanityurl.business.VanityUrlAPI.CACHE_404_VANITY_URL;

import com.dotcms.util.VanityUrlUtil;
import com.liferay.util.StringPool;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * This class construct a reduced version of the {@link VanityUrl}
 * object to be saved on cache
 *
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 22, 2017
 */
public class CachedVanityUrl implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Pattern pattern;
    private final String vanityUrlId;
    private final String url;
    private final String siteId;
    private final long languageId;
    private final String forwardTo;
    private final int response;
    private final int order;

    /**
     * Generate a cached Vanity URL object
     *
     * @param vanityUrl The vanityurl Url to cache
     */
    public CachedVanityUrl(final VanityUrl vanityUrl) {
        //if the VanityUrl URI is not a valid regex
        this.vanityUrlId = vanityUrl.getIdentifier();
        final String regex = normalize(vanityUrl.getURI(), CACHE_404_VANITY_URL.equals(this.vanityUrlId));
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.url = vanityUrl.getURI();
        this.languageId = vanityUrl.getLanguageId();
        this.siteId = vanityUrl.getSite();
        this.forwardTo = vanityUrl.getForwardTo();
        this.response = vanityUrl.getAction();
        this.order    = vanityUrl.getOrder();
    }

    /**
     * Generates a CachedVanityUrl from another given CachedVanityUrl
     *
     * @param fromCachedVanityUrl VanityURL to copy
     * @param url url to override in the created copy
     */
    public CachedVanityUrl(CachedVanityUrl fromCachedVanityUrl, String url) {

        this.vanityUrlId = fromCachedVanityUrl.getVanityUrlId();
        //if the VanityUrl URI is not a valid regex
        final String regex = normalize(url, CACHE_404_VANITY_URL.equals(this.vanityUrlId));
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.url = url;
        this.languageId = fromCachedVanityUrl.getLanguageId();
        this.siteId = fromCachedVanityUrl.getSiteId();
        this.forwardTo = fromCachedVanityUrl.getForwardTo();
        this.response = fromCachedVanityUrl.getResponse();
        this.order    = fromCachedVanityUrl.getOrder();
    }

    /**
     * Generates a CachedVanityUrl from another given CachedVanityUrl
     *
     * @param forwardTo replace the forward.
     * @param fromCachedVanityUrl VanityURL to copy
     *
     */
    public CachedVanityUrl(final String forwardTo,
                           final CachedVanityUrl fromCachedVanityUrl) {


        this.pattern     = fromCachedVanityUrl.pattern;
        this.vanityUrlId = fromCachedVanityUrl.getVanityUrlId();
        this.url         = fromCachedVanityUrl.url;
        this.languageId  = fromCachedVanityUrl.getLanguageId();
        this.siteId      = fromCachedVanityUrl.getSiteId();
        this.forwardTo   = forwardTo;
        this.response    = fromCachedVanityUrl.getResponse();
        this.order       = fromCachedVanityUrl.getOrder();
    }

    public int getOrder() {
        return order;
    }

    /**
     * Get the URL from the Cached Vanity URL
     *
     * @return the URL from the Cached Vanity URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the Site Id from the Cached Vanity URL
     *
     * @return the Site ID from the Cached Vanity URL
     */
    public String getSiteId() {
        return siteId;
    }

    /**
     * Get the Language Id from the Cached Vanity URL
     *
     * @return the language Id from the Cached Vanity URL
     */
    public long getLanguageId() {
        return languageId;
    }

    /**
     * Get the Forward to path from the Cached Vanity URL
     *
     * @return the Forward to path from the Cached Vanity URL
     */
    public String getForwardTo() {
        return forwardTo;
    }

    /**
     * Get the Response code from the Cached Vanity URL
     *
     * @return the Response code from the Cached Vanity URL
     */
    public int getResponse() {
        return response;
    }

    /**
     * Get the URI Pattern from the Cached Vanity URL
     *
     * @return the URI Pattern from the Cached Vanity URL
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * get the Vanitu Url Identifier
     *
     * @return The Vanity Url Identifier
     */
    public String getVanityUrlId() {
        return vanityUrlId;
    }

    /**
     * This comes as fix for https://github.com/dotCMS/core/issues/16433
     * If the uri ends with forward slash `/`
     * This method will make that piece optional.
     * @param uri the uri stored in the contentlet.
     * @return the uri regexp with the optional forward slash support added.
     */
    private String addOptionalForwardSlashSupport(final String uri){
        if(uri.endsWith(StringPool.FORWARD_SLASH)){
            String regex = uri;
            regex = regex.substring(0, regex.length() -1 );
            return regex + "(/)*";
        }
        return uri;
    }

    /**
     * This takes the uir that was originally stored in the contentlet adds validates it.
     * @param uri the uri stored in the contentlet.
     * @param cache404VanityUrl whether or not this is a 404 cache entry
     * @return normalized uri.
     */
    private String normalize(final String uri, final boolean cache404VanityUrl){
        final String uriRegEx = cache404VanityUrl ? uri : addOptionalForwardSlashSupport(uri);
        return VanityUrlUtil.isValidRegex(uriRegEx) ? uriRegEx : StringPool.BLANK;
    }


    @Override
    public String toString() {
        return "CachedVanityUrl{" +
                "pattern=" + pattern +
                ", vanityUrlId='" + vanityUrlId + '\'' +
                ", url='" + url + '\'' +
                ", siteId='" + siteId + '\'' +
                ", languageId=" + languageId +
                ", forwardTo='" + forwardTo + '\'' +
                ", response=" + response +
                ", order=" + order +
                '}';
    }
}