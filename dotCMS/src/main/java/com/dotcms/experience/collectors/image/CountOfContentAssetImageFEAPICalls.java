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
 * Represents a Metric to count how many times the Endpoint '/contentAsset/image/' is called from Front End.
 */
public class CountOfContentAssetImageFEAPICalls extends ApiMetricType {
    @Override
    public String getName() {
        return "COUNT_OF_FE_CONTENTASSET_CALLS";
    }

    @Override
    public String getDescription() {
        return "Count of 'contentAsset/image' API calls from Front End";
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

        return currentPageMode == PageMode.LIVE;
    }
}
