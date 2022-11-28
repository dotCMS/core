package com.dotcms.experiments.business.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is a {@link com.dotcms.variant.model.Variant selected to a User, this mean this user
 * is going to be into the {@link com.dotcms.variant.model.Variant}'s {@link com.dotcms.experiments.model.Experiment}.
 *
 * @see ExperimentWebAPI#isUserIncluded(HttpServletRequest, HttpServletResponse, List)
 * @see com.dotcms.variant.model.Variant
 */
public  class SelectedVariant implements Serializable {

    /**
     * {@link com.dotcms.variant.model.Variant}'s name
     */
    @JsonProperty("name")
    private final String name;

    /**
     * URL to render the {@link com.dotcms.experiments.model.Experiment}'s page in this {@link com.dotcms.variant.model.Variant}
     */
    @JsonProperty("url")
    private final String url;

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