package com.dotcms.rest.api.v1.site;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form to find an host by name
 * @author jsanca
 */
public class SearchSiteByNameForm {

    private final String sitename;

    @JsonCreator
    public SearchSiteByNameForm(@JsonProperty("sitename") final String sitename) {
        this.sitename = sitename;
    }

    public String getSitename() {
        return sitename;
    }
}
