package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * This View represent a special version of the {@link PageView} that is used to represent an empty
 * page. This is useful when we need to return information on an HTML Page that is mapped to a
 * Vanity URL using a Temporary or Permanent Redirect, or any other scenario in which a request to
 * the {@link com.dotcms.rest.api.v1.page.PageResource} needs to handle a page in a non-conventional
 * way.
 *
 * @author Jose Castro
 * @since May 8th, 2024
 */
@JsonSerialize(using = EmptyPageViewSerializer.class)
public class EmptyPageView extends PageView {

    @JsonProperty("vanityUrl")
    private CachedVanityUrl cachedVanityUrl;

    /**
     * Creates a new instance of the {@link EmptyPageView} object using the Builder.
     *
     * @param builder The {@link Builder} object to use to create the {@link EmptyPageView}.
     */
    private EmptyPageView(final Builder builder) {
        this.cachedVanityUrl = builder.cachedVanityUrl;
    }

    /**
     * Returns the Vanity URL associated with this {@link EmptyPageView}.
     *
     * @return The {@link CachedVanityUrl} object associated with this {@link EmptyPageView}.
     */
    public CachedVanityUrl getCachedVanityUrl() {
        return this.cachedVanityUrl;
    }

    /**
     * Returns a new instance of the {@link Builder} object to create a new {@link EmptyPageView}.
     */
    public static class Builder extends PageView.Builder {

        private CachedVanityUrl cachedVanityUrl;

        public Builder vanityUrl(final CachedVanityUrl cachedVanityUrl) {
            this.cachedVanityUrl = cachedVanityUrl;
            return this;
        }

        @Override
        public EmptyPageView build() {
            return new EmptyPageView(this);
        }

    }

}
