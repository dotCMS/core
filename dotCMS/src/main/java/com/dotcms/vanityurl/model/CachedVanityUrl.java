package com.dotcms.vanityurl.model;

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

    /**
     * Generate a cached Vanity URL object
     *
     * @param vanityUrl The vanityurl Url to cache
     */
    public CachedVanityUrl(VanityUrl vanityUrl) {
        //if the VanityUrl URI is not a valid regex
        String regex = VanityUrlUtil.isPatternValid(vanityUrl.getURI()) ? vanityUrl.getURI()
                : StringPool.BLANK;
        this.pattern = Pattern.compile(regex);
        this.vanityUrlId = vanityUrl.getIdentifier();
        this.url = vanityUrl.getURI();
        this.languageId = vanityUrl.getLanguageId();
        this.siteId = vanityUrl.getSite();
        this.forwardTo = vanityUrl.getForwardTo();
        this.response = vanityUrl.getAction();
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

}