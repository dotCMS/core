package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsCompassDirection;
import app.photofox.vipsffm.enums.VipsExtend;
import com.dotmarketing.util.Config;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * libvips equivalent of {@link com.dotmarketing.image.filter.ThumbnailImageFilter}: fit the image
 * inside a {@code w x h} box maintaining aspect ratio, then centre it on a solid background-coloured
 * canvas of exactly {@code w x h}.
 */
public class VipsThumbnailImageFilter extends VipsImageFilter {

    private static final int DEFAULT_HEIGHT = Config.getIntProperty("DEFAULT_HEIGHT", 100);
    private static final int DEFAULT_WIDTH = Config.getIntProperty("DEFAULT_WIDTH", 100);

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "w (int) specifies width",
                "h (int) specifies height",
                "bg (int) must be 9 digits of rgb (000000000=black, 255255255=white) for background color"
        };
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        int width = intParam(parameters, "w", 0);
        int height = intParam(parameters, "h", 0);
        if (height <= 0 && width <= 0) {
            height = DEFAULT_HEIGHT;
            width = DEFAULT_WIDTH;
        }
        final String[] bgParam = parameters.get(getPrefix() + "bg");
        // bg must be exactly 9 digits (rrrgggbbb); fall back to white on bad input.
        final String rgb = (bgParam != null && bgParam[0] != null && bgParam[0].matches("\\d{9}"))
                ? bgParam[0] : "255255255";
        final double r = Integer.parseInt(rgb.substring(0, 3));
        final double g = Integer.parseInt(rgb.substring(3, 6));
        final double b = Integer.parseInt(rgb.substring(6));

        final int boxW = width;
        final int boxH = height;
        VipsManager.run(arena -> {
            // Fit within the box, maintaining aspect ratio (no FORCE).
            VImage thumb = VImage.thumbnail(arena, in.getAbsolutePath(), boxW,
                    VipsOption.Int("height", boxH));
            // Drop any alpha onto the requested background so the canvas colour shows through.
            if (thumb.hasAlpha()) {
                thumb = thumb.flatten(VipsOption.ArrayDouble("background", List.of(r, g, b)));
            }
            // Centre on an exact boxW x boxH canvas, padding with the background colour.
            final VImage canvas = thumb.gravity(VipsCompassDirection.COMPASS_DIRECTION_CENTRE, boxW, boxH,
                    VipsOption.Enum("extend", VipsExtend.EXTEND_BACKGROUND),
                    VipsOption.ArrayDouble("background", List.of(r, g, b)));
            canvas.writeToFile(out.getAbsolutePath());
        });
    }
}
