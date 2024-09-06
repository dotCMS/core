package com.dotcms.experiments.business.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @JsonProperty("runningId")
    private final String runningId;

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
    private final LookBackWindow lookBackWindow;

    @JsonProperty("regexs")
    private Map<String, String> regexs = new HashMap();

    private SelectedExperiment(final Builder builder) {

        this.id = builder.id;
        this.name = builder.name;
        this.pageUrl = builder.pageUrl;
        this.variant = builder.variant;
        this.lookBackWindow = new LookBackWindow(builder.lookBackWindow, builder.expireTime);
        this.regexs = builder.regexs();
        this.runningId = builder.runningId;
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

    public LookBackWindow getLookBackWindow() {
        return lookBackWindow;
    }

    public String getRunningId() {
        return runningId;
    }

    public Map<String, String> regexs() {
        return regexs;
    }

    public static class Builder {
        private String name;
        private String id;
        private String pageUrl;
        private SelectedVariant variant;
        private String lookBackWindow;
        private long expireTime;

        private String redirectPattern;
        private String targetPagePattern;
        private String runningId;

        public Builder runningId(final String runningId) {
            this.runningId = runningId;
            return this;
        }
        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder experimentPagePattern(final String redirectPattern) {
            this.redirectPattern = redirectPattern;
            return this;
        }

        public Builder targetPagePattern(final String targetPagePattern) {
            this.targetPagePattern = targetPagePattern;
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

        public Builder expireTime(final long expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public SelectedExperiment build(){
            return new SelectedExperiment(this);
        }

        public Map<String, String> regexs() {

            final Map<String, String> regexs = new HashMap();

            regexs.put("isExperimentPage", redirectPattern);
            regexs.put("isTargetPage", targetPagePattern);

            return regexs;
        }
    }


    public static class LookBackWindow {
        final String value;
        final long expireMillis;

        public LookBackWindow(final String value, final long expireMillis) {
            this.value = value;
            this.expireMillis = expireMillis;
        }

        public String getValue() {
            return value;
        }

        public long getExpireMillis() {
            return expireMillis;
        }
    }
}
