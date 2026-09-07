package com.dotmarketing.servlets;

import com.dotmarketing.image.focalpoint.FocalPoint;
import com.liferay.util.StringPool;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pure, dependency-free expansion of the ShortyServlet image shorthand tokens (e.g. {@code 500w},
 * {@code 300cw}, {@code webp}, {@code avif}, {@code smart}) into the explicit {@code /filter} param
 * path consumed by the image exporter. Extracted from {@link ShortyServlet} so it can be unit tested
 * without bootstrapping the servlet's CDI dependencies.
 */
final class ImageUriPathExpander {

    private ImageUriPathExpander() {}

    private static final Pattern widthPattern      = Pattern.compile("/(\\d+)w\\b");
    private static final Pattern heightPattern     = Pattern.compile("/(\\d+)h\\b");
    private static final Pattern cropWidthPattern  = Pattern.compile("/(\\d+)cw\\b");
    private static final Pattern cropHeightPattern = Pattern.compile("/(\\d+)ch\\b");
    private static final Pattern maxWidthPattern   = Pattern.compile("/(\\d+)maxw\\b");
    private static final Pattern maxHeightPattern  = Pattern.compile("/(\\d+)maxh\\b");
    private static final Pattern minWidthPattern   = Pattern.compile("/(\\d+)minw\\b");
    private static final Pattern minHeightPattern  = Pattern.compile("/(\\d+)minh\\b");
    private static final Pattern resampleOptsPattern = Pattern.compile("/(\\d+)ro\\b");

    /**
     * Appends the expanded {@code /filter}-style params for the given shorthand tokens to
     * {@code pathBuilder}, in token order.
     *
     * @param avif  emit AVIF output ({@code /avif} token)
     * @param smart {@code /smart} token — turns crop tokens into content-aware smartcrop
     */
    static void expand(final int width, final int height, final int maxWidth, final int maxHeight,
            final int minWidth, final int minHeight, final int quality,
            final boolean jpeg, final boolean jpegp, final boolean webp, final boolean avif,
            final boolean smart, final StringBuilder pathBuilder, final Optional<FocalPoint> focalPoint,
            final int cropWidth, final int cropHeight, final int resampleOpt, final String[] filters) {

        for (String token : filters) {
            final String filter = StringPool.FORWARD_SLASH + token;
            if (widthPattern.matcher(filter).find()) {
                pathBuilder.append(width > 0 ? "/resize_w/" + width : StringPool.BLANK);
                continue;
            }
            if (heightPattern.matcher(filter).find()) {
                pathBuilder.append(height > 0 ? "/resize_h/" + height : StringPool.BLANK);
                continue;
            }
            if (maxWidthPattern.matcher(filter).find()) {
                pathBuilder.append("/resize_maxw/" + maxWidth);
                continue;
            }
            if (maxHeightPattern.matcher(filter).find()) {
                pathBuilder.append("/resize_maxh/" + maxHeight);
                continue;
            }
            if (minWidthPattern.matcher(filter).find()) {
                pathBuilder.append("/resize_minw/" + minWidth);
                continue;
            }
            if (minHeightPattern.matcher(filter).find()) {
                pathBuilder.append("/resize_minh/" + minHeight);
                continue;
            }
            if (cropWidthPattern.matcher(filter).find()) {
                // "/smart" turns the crop into a content-aware smartcrop (libvips engine only)
                final String cropWKey = smart ? "/smartcrop_w/" : "/crop_w/";
                pathBuilder.append(cropWidth > 0 ? cropWKey + cropWidth : StringPool.BLANK);
                continue;
            }
            if (cropHeightPattern.matcher(filter).find()) {
                final String cropHKey = smart ? "/smartcrop_h/" : "/crop_h/";
                pathBuilder.append(cropHeight > 0 ? cropHKey + cropHeight : StringPool.BLANK);
                continue;
            }
            if (filter.contains("fp")) {
                pathBuilder.append(focalPoint.isPresent() ? "/fp/" + focalPoint.get() : StringPool.BLANK);
                continue;
            }
            if (resampleOptsPattern.matcher(filter).find()) {
                pathBuilder.append(resampleOpt > 0 ? "/resize_ro/" + resampleOpt : StringPool.BLANK);
            }
        }

        if (quality > 0) {
            pathBuilder.append("/quality_q/" + quality);
        } else {
            pathBuilder.append(jpeg ? "/jpeg_q/75" : StringPool.BLANK);
            pathBuilder.append(webp ? "/webp_q/75" : StringPool.BLANK);
            pathBuilder.append(jpeg && jpegp ? "/jpeg_p/1" : StringPool.BLANK);
        }
        // AVIF output (libvips engine only); honour an explicit /(\d+)q quality, else default 75
        if (avif) {
            pathBuilder.append("/avif_q/" + (quality > 0 ? quality : 75));
        }
    }
}
