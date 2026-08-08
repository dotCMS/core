package com.dotcms.rest.api.v1.page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight reference to an HTML page (no rendered content).
 */
public class PageSourceRefView {

    @Schema(description = "Page identifier")
    private final String identifier;

    @Schema(description = "Host-qualified page URI (e.g. //demo.dotcms.com/index)")
    private final String uri;

    @Schema(description = "Language ID under which the page was resolved")
    private final long languageId;

    public PageSourceRefView(final String identifier, final String uri, final long languageId) {
        this.identifier = identifier;
        this.uri        = uri;
        this.languageId = languageId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getUri() {
        return uri;
    }

    public long getLanguageId() {
        return languageId;
    }
}
