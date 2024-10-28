package com.dotcms.experience.collectors.image;

import com.dotcms.experience.MetricCategory;
import com.dotcms.experience.MetricFeature;
import com.dotcms.experience.collectors.api.ApiMetricType;
import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotmarketing.util.PageMode;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents a Metric to count how many times the Endpoint '/contentAsset/image/' is called from Back End.
 */
public class CountOfContentAssetImageBEAPICalls extends ApiMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_BE_CONTENTASSET_CALLS";
    }

    @Override
    public String getDescription() {
        return "Count of 'contentAsset/image' API calls from Back End";
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
        return "/contentAsset/image/";
    }

    @Override
    public HTTPMethod getHttpMethod() {
        return HTTPMethod.GET;
    }

    @JsonIgnore
    public boolean shouldCount(final HttpServletRequest request, final HttpServletResponse response){
        final PageMode currentPageMode = PageMode.get(request);

        return currentPageMode != PageMode.LIVE;
    }
}
