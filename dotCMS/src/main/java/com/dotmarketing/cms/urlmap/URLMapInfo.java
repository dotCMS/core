package com.dotmarketing.cms.urlmap;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * Return value for {@link URLMapAPI#processURLMap(UrlMapContext)}
 */
public class URLMapInfo {
    final Contentlet contentlet;
    final Identifier identifier;
    final String urlMapped;

    URLMapInfo(final Contentlet contentlet, final Identifier identifier, String urlMapped) {
        this.contentlet = contentlet;
        this.identifier = identifier;
        this.urlMapped = urlMapped;
    }

    /**
     * Return the content link with the URI resolve by the
     * {@link URLMapAPI#processURLMap(UrlMapContext)} method
     * @return
     */
    public Contentlet getContentlet() {
        return contentlet;
    }

    /**
     * Return the Page's detail page uri
     * @return
     */
    public String getDetailtPageUri() {
        return identifier.getURI();
    }

    /**
     * Return the content link with the Page's {@link Identifier} resolve by the
     * {@link URLMapAPI#processURLMap(UrlMapContext)} method
     * @return
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Return the URL Mapped, this is the original URL requested
     * @return
     */
    public String getUrlMapped() {
        return urlMapped;
    }
}
