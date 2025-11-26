package com.dotcms.telemetry.collectors.image;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.api.ApiMetricType;
import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotmarketing.util.PageMode;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a Metric to count how many times the Endpoint '/dA' is called from Front End.
 */
public class CountOfDAImageFEAPICalls extends ApiMetricType {

    @Override
    public String getName() {
        return "COUNT_OF_FE_DA_CALLS";
    }

    @Override
    public String getDescription() {
        return "Count of '/dA/' API calls From Front End";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.IMAGE_API;
    }


    @Override
    public String getAPIUrl() {
        return "/dA/";
    }

    @Override
    public HTTPMethod getHttpMethod() {
        return HTTPMethod.GET;
    }

    @Override
    @JsonIgnore
    public boolean shouldCount(final HttpServletRequest request,
                               final HttpServletResponse response) {
        final PageMode currentPageMode = PageMode.get(request);
        return currentPageMode == PageMode.LIVE;
    }

}
