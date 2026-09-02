package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.CropImageFilter}.
 *
 * <p>The crop-rectangle math — including percentage inputs, aspect-fill when only one dimension is
 * given, and focal-point centering — is ported verbatim from the legacy filter so the selected
 * region is identical. Only the pixel extraction switches to libvips {@code extractArea}.</p>
 */
public class VipsCropImageFilter extends VipsImageFilter {

    public static final String X = "x";
    public static final String Y = "y";
    public static final String W = "w";
    public static final String H = "h";

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "x (int) for left of crop",
                "y (int) for top of crop",
                "w (int) for width of crop",
                "h (int) for height of crop",
                "fp (int,int) the focal point of the crop"
        };
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int x0 = intParam(parameters, X, 0);
        final int y0 = intParam(parameters, Y, 0);
        final float widthInput = (float) doubleParam(parameters, W, 0d);
        final float heightInput = (float) doubleParam(parameters, H, 0d);

        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            final Dimension current = new Dimension(src.getWidth(), src.getHeight());

            int width;
            int height;
            if (widthInput == 0 && heightInput > 0) {
                height = Math.round(heightInput <= 1 ? current.height * heightInput : heightInput);
                width = Math.round((float) height * current.width / current.height);
            } else if (widthInput > 0 && heightInput == 0) {
                width = Math.round(widthInput <= 1 ? current.width * widthInput : widthInput);
                height = Math.round((float) width * current.height / current.width);
            } else if (widthInput > 0 && heightInput > 0) {
                width = Math.round(widthInput <= 1 ? current.width * widthInput : widthInput);
                height = Math.round(heightInput <= 1 ? current.height * heightInput : heightInput);
            } else {
                width = current.width;
                height = current.height;
            }

            if (x0 > current.getWidth() || y0 > current.getHeight()) {
                src.writeToFile(out.getAbsolutePath());
                return;
            }

            int x = x0;
            int y = y0;
            final Optional<Point> centerOpt = (x0 == 0 && y0 == 0)
                    ? calcFocalPoint(current, parameters) : Optional.empty();
            if (centerOpt.isPresent()) {
                final int halfWidth = Math.floorDiv(width, 2);
                final int halfHeight = Math.floorDiv(height, 2);
                final Point p = centerOpt.get();
                x = Math.max(p.x - halfWidth, 0);
                y = Math.max(p.y - halfHeight, 0);
                width = x + halfWidth > current.width ? current.width - x : width;
                height = y + halfHeight > current.height ? current.height - y : height;
            }

            if (x + width > current.width) {
                width = current.width - x - 1;
            }
            if (y + height > current.height) {
                height = current.height - y - 1;
            }

            final VImage cropped = src.extractArea(x, y, width, height);
            cropped.writeToFile(out.getAbsolutePath());
        });
    }

    /** Resolve the focal point (from params or stored field metadata) to pixel coordinates. */
    protected Optional<Point> calcFocalPoint(final Dimension current, final Map<String, String[]> parameters) {
        final FocalPointAPIImpl api = new FocalPointAPIImpl();
        Optional<FocalPoint> optPoint = api.parseFocalPointFromParams(parameters);
        if (optPoint.isEmpty() && parameters.get("assetInodeOrIdentifier") != null
                && parameters.get("fieldVarName") != null) {
            final String inode = parameters.get("assetInodeOrIdentifier")[0];
            final String fieldVar = parameters.get("fieldVarName")[0];
            optPoint = api.readFocalPoint(inode, fieldVar);
        }
        return optPoint.map(fp -> new Point(
                Math.round(current.width * fp.x),
                Math.round(current.height * fp.y)));
    }
}
