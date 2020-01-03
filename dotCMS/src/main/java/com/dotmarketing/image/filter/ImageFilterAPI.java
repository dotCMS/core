package com.dotmarketing.image.filter;

import java.util.Map;
import io.vavr.Function0;

public interface ImageFilterAPI {
    Function0<ImageFilterApiImpl> apiInstance = Function0.of(ImageFilterApiImpl::new).memoized();


    default ImageFilterAPI getInstance() {
        return apiInstance.apply();
    }

    /**
     * returns the filters that have been specified in the filter
     * parameter or in the the arguements passed in
     * @param parameters
     * @return
     */
    Map<String, Class> resolveFilters(Map<String, String[]> parameters);


}
