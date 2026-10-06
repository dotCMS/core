package com.dotmarketing.image.vips;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsInteresting;
import java.io.File;
import java.util.Map;

/**
 * Content-aware smart crop — a capability the legacy AWT engine does not have.
 *
 * <p>Crops to an exact {@code w x h} box centred on the most salient region of the image rather than
 * the geometric centre, using libvips' attention model (saliency + skin-tone + edge energy). Ideal
 * for automatically generating well-composed thumbnails of arbitrary content.</p>
 *
 * <p>URL contract: {@code filter=smartcrop&smartcrop_w=400&smartcrop_h=400} with an optional
 * {@code smartcrop_mode} of {@code attention} (default), {@code entropy} or {@code centre}.</p>
 */
public class VipsSmartCropImageFilter extends VipsImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[] {
                "w (int) target width",
                "h (int) target height",
                "mode (attention|entropy|centre) saliency strategy, default attention"
        };
    }

    @Override
    protected void transform(final File in, final File out, final Map<String, String[]> parameters) {
        final int width = intParam(parameters, "w", 0);
        final int height = intParam(parameters, "h", 0);
        final String[] modeParam = parameters.get(getPrefix() + "mode");
        final VipsInteresting interesting = resolveMode(modeParam != null ? modeParam[0] : "attention");

        VipsManager.run(arena -> {
            final VImage src = VipsManager.load(arena, in);
            // libvips smartcrop cannot extract a region larger than the source, so clamp the target
            // to the image bounds — an oversized request returns the largest valid crop rather than
            // failing with "bad extract area" (smartcrop has no legacy fallback).
            final int targetW = Math.min(width > 0 ? width : src.getWidth(), src.getWidth());
            final int targetH = Math.min(height > 0 ? height : src.getHeight(), src.getHeight());
            final VImage cropped = src.smartcrop(targetW, targetH,
                    VipsOption.Enum("interesting", interesting));
            cropped.writeToFile(out.getAbsolutePath());
        });
    }

    private VipsInteresting resolveMode(final String mode) {
        switch (mode == null ? "" : mode.toLowerCase()) {
            case "entropy":
                return VipsInteresting.INTERESTING_ENTROPY;
            case "centre":
            case "center":
                return VipsInteresting.INTERESTING_CENTRE;
            case "attention":
            default:
                return VipsInteresting.INTERESTING_ATTENTION;
        }
    }
}
