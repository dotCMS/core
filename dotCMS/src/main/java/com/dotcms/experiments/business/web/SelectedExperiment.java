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

    /**
     * Experiment's lookBackWindows'
     */
    @JsonProperty("lookBackWindow")
    private final String lookBackWindow;

    private SelectedExperiment(final String id, final String name, final String pageUrl,
            final SelectedVariant variant, final String lookBackWindow) {
        this.id = id;
        this.name = name;
        this.pageUrl = pageUrl;
        this.variant = variant;
        this.lookBackWindow = lookBackWindow;
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

    public String getLookBackWindow() {
        return lookBackWindow;
    }

    public static class Builder {
        private String name;
        private String id;
        private String pageUrl;
        private SelectedVariant variant;
        private String lookBackWindow;

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder pageUrl(final String pageUrl) {
            this.pageUrl = pageUrl;
            return this;
        }

        public Builder variant(final SelectedVariant selectedVariant) {
            this.variant = selectedVariant;
            return this;
        }

        public Builder lookBackWindow(final String lookBackWindow) {
            this.lookBackWindow = lookBackWindow;
            return this;
        }

        public SelectedExperiment build(){
            return new SelectedExperiment(id, name, pageUrl, variant, lookBackWindow);
        }
    }
}
