package com.dotmarketing.image.filter;

import java.util.Map;
import io.vavr.Function0;

public interface ImageFilterAPI {
    Function0<ImageFilterApiImpl> apiInstance =
                    Function0.of(ImageFilterApiImpl::new).memoized();
	

    default ImageFilterAPI getInstance() {
        return apiInstance.apply();
    }



    Map<String, Class> resolveFilters(Map<String, String[]> parameters);
	

}
