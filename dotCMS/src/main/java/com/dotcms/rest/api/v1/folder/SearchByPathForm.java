package com.dotcms.rest.api.v1.folder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Search a folders by path
 * @author jsanca
 */
public class SearchByPathForm {

    private final String path;

    @JsonCreator
    public SearchByPathForm(@JsonProperty("path") final String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}