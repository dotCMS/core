package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;
import io.vavr.Function0;

public interface ImageFilterAPI {

    String CROP = "crop";
    String EXPOSURE = "exposure";
    String FLIP = "flip";
    String FOCAL_POINT = "focalpoint";
    String GAMMA = "gamma";
    String GIF = "gif";
    String GRAY_SCALE = "grayscale";
    String HSB = "hsb";
    String JPEG = "jpeg";
    String JPG = "jpg";
    String PDF = "pdf";
    String PNG = "png";
    String RESIZE = "resize";
    String ROTATE = "rotate";
    String SCALE = "scale";
    String THUMBNAIL = "thumbnail";
    String THUMB = "thumb";
    String WEBP = "webp";

    Function0<ImageFilterApiImpl> apiInstance = Function0.of(ImageFilterApiImpl::new).memoized();

    default ImageFilterAPI getInstance() {
        return apiInstance.apply();
    }

    /**
     * returns the filters that have been specified in the filter parameter or in the the arguements
     * passed in
     * 
     * @param parameters the parameters passed in
     * @return  a map of the filters that have been specified
     */
    Map<String, Class<? extends ImageFilter>> resolveFilters(Map<String, String[]> parameters);



    /**
     * returns an image dimensions
     * 
     * @param image the image to get the dimensions of
     * @return the dimensions of the image
     */
    Dimension getWidthHeight(File image);

    /**
     * resizing an image is a slower, more memory intensive operation than subsampling but produces
     * better looking thumbnails and results in a scaled image that also maintains the aspect ratio..
     * Resizing should only be done on smaller images (say less than 2000px) as very large images can
     * cause garbage collections and OOM exceptions. This is because the entire image needs to be
     * decompressed into heap memory before the resizing operation can take place.
     * 
     * @param image the image to resize
     * @param width the width to resize to
     * @param height the height to resize to
     * @return the resized image
     */
    BufferedImage resizeImage(File image, int width, int height);

    

    /**
     * resizing an image is a slower, more memory intensive operation than subsampling but produces
     * better looking thumbnails and results in a scaled image that also maintains the aspect ratio..
     * Resizing should only be done on smaller images (say less than 2000px) as very large images can
     * cause garbage collections and OOM exceptions. This is because the entire image needs to be
     * decompressed into heap memory before the resizing operation can take place.
     * 
     * @param srcImage the image to resize
     * @param width the width to resize to
     * @param height the height to resize to
     * @return the resized image
     */
    BufferedImage resizeImage(BufferedImage srcImage, int width, int height);

    
    /**
     * This method allows you to resize an image and specify what resizing filter should be used to 
     * produce the image.  You can see the available filters
     * @param srcImage the image to resize
     * @param width the width to resize to
     * @param height the height to resize to
     * @param resampleOption the resample option to use
     * @return the resized image
     */
    BufferedImage resizeImage(BufferedImage srcImage, int width, int height, int resampleOption);

    
    BufferedImage resizeImage(File imageFile, int width, int height, int resampleOption);

    /**
     * subsampling resizes an image by streaming the larger image and collecting every X pixel resulting
     * in a scaled image that also maintains the aspect ratio. Because it is a stream, subsampling can
     * resize very large images without causing the memory pressures of resizing. If the width and/or
     * height is greater than the original image, the original image will be returned.
     *
     * @param image the image to subsample
     * @param width the width to subsample to
     * @param height the height to subsample to
     * @return the subsampled image
     */
    BufferedImage subsampleImage(File image, int width, int height);

    BufferedImage intelligentResize(File incomingImage, int width, int height, int resampleOption);

    BufferedImage intelligentResize(File incomingImage, int width, int height);

}
