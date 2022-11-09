package com.dotcms.experiments.business.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class SelectedExperiment implements Serializable {
    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("pageUrl")
    private String pageUrl;
    @JsonProperty("variant")
    private SelectedVariant variant;

    public SelectedExperiment(final String id, final String name, final String pageUrl,
            final SelectedVariant variant) {
        this.id = id;
        this.name = name;
        this.pageUrl = pageUrl;
        this.variant = variant;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String pageUrl() {
        return pageUrl;
    }

    public SelectedVariant variant() {
        return variant;
    }
}
