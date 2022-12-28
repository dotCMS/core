package com.dotcms.experiments.business.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is a {@link com.dotcms.experiments.model.Experiment} selected to a User, this mean this user
 * is going to be into this {@link com.dotcms.experiments.model.Experiment}.
 *
 * @see ExperimentWebAPI#isUserIncluded(HttpServletRequest, HttpServletResponse, List)
 * @see com.dotcms.experiments.model.Experiment
 */
public class SelectedExperiment implements Serializable {

    /**
     * Experiment's name
     */
    @JsonProperty("name")
    private final String name;

    /**
     * Experiment's ID
     */
    @JsonProperty("id")
    private final String id;

    /**
     * Experiment;s page URL
     */
    @JsonProperty("pageUrl")
    private final String pageUrl;

    /**
     * Selected Variant for the User
     */
    @JsonProperty("variant")
    private final SelectedVariant variant;

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
