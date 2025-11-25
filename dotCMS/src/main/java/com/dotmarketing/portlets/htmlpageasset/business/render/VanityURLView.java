package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.vanityurl.model.CachedVanityUrl;

import java.io.Serializable;

/**
 * This View represents the main information of a Vanity URL in dotCMS.
 *
 * @author Jose Castro
 * @since May 8th, 2024
 */
public class VanityURLView implements Serializable {

    private final String id;
    private final String siteId;
    private final String url;
    private final String forwardTo;
    private final int response;

    VanityURLView(final Builder builder) {
        this.id = builder.id;
        this.siteId = builder.siteId;
        this.url = builder.url;
        this.forwardTo = builder.forwardTo;
        this.response = builder.response;
    }

    /**
     * Returns the ID of the Vanity URL.
     *
     * @return The ID of the Vanity URL.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the Site ID where the Vanity URL lives.
     *
     * @return The Site ID where the Vanity URL lives.
     */
    public String getSiteId() {
        return siteId;
    }

    /**
     * Returns the “virtual path” of the Vanity URL.
     *
     * @return The “virtual path” of the Vanity URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the URL that the Vanity URL really points to.
     *
     * @return The forward-to URL.
     */
    public String getForwardTo() {
        return forwardTo;
    }

    /**
     * Returns the HTTP response code that the Vanity URL should return.
     *
     * @return The HTTP response code.
     */
    public int getResponse() {
        return response;
    }

    /**
     * Returns a new instance of the {@link VanityURLView.Builder} object to create a new
     * {@link VanityURLView}.
     */
    public static class Builder {

        private String id;
        private String siteId;
        private String url;
        private String forwardTo;
        private int response;

        /**
         * Creates a new instance of the {@link VanityURLView.Builder} object based on a
         * {@link CachedVanityUrl} object.
         *
         * @param cachedVanityUrl The {@link CachedVanityUrl} object to use to create the
         *                        {@link VanityURLView}.
         *
         * @return The {@link VanityURLView.Builder} object.
         */
        public Builder vanityUrl(final CachedVanityUrl cachedVanityUrl) {
            this.id = cachedVanityUrl.vanityUrlId;
            this.siteId = cachedVanityUrl.siteId;
            this.url = cachedVanityUrl.url;
            this.forwardTo = cachedVanityUrl.forwardTo;
            this.response = cachedVanityUrl.response;
            return this;
        }

        public VanityURLView build() {
            return new VanityURLView(this);
        }

    }

}
