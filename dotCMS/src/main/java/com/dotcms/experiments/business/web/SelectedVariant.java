package com.dotcms.experiments.business.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public  class SelectedVariant implements Serializable {
    @JsonProperty("name")
    private String name;
    @JsonProperty("url")
    private String url;

    public SelectedVariant(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String name() {
        return name;
    }

    public String url() {
        return url;
    }
}