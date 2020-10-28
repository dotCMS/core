package com.dotmarketing.image.filter;

import java.util.Map;
import io.vavr.Function0;

public interface ImageFilterAPI {

    String CROP        = "crop";
    String EXPOSURE    = "exposure";
    String FLIP        = "flip";
    String FOCAL_POINT = "focalpoint";
    String GAMMA       = "gamma";
    String GIF         = "gif";
    String GRAY_SCALE  = "grayscale";
    String HSB         = "hsb";
    String JPEG        = "jpeg";
    String JPG         = "jpg";
    String PDF         = "pdf";
    String PNG         = "png";
    String RESIZE      = "resize";
    String ROTATE      = "rotate";
    String SCALE       = "scale";
    String THUMBNAIL   = "thumbnail";
    String THUMB       = "thumb";
    String WEBP        = "webp";


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
