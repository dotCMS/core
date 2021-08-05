package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form to find an site by name
 * @author jsanca
 */
public class SearchSiteByNameForm {

    private final String siteName;

    @JsonCreator
    public SearchSiteByNameForm(@JsonProperty("siteName") final String siteName) {
        this.siteName = siteName;
    }

    public String getSiteName() {
        return siteName;
    }
}
