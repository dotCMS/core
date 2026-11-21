package com.dotmarketing.image.filter;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.gif.AnimatedGifEncoder;
import com.dotmarketing.image.gif.GifDecoder;
import com.twelvemonkeys.image.ResampleOp;
import io.vavr.control.Try;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;

public class ResizeGifImageFilter extends ImageFilter {

    @Override
    public String[] getAcceptedParameters() {
        return new String[]{"w (int) specifies width", "h (int) specifies height, loop=true|false, maxFrames (int)"};
    }

    /** The legacy GIF filter accepts the same parameters as resize. */
    @Override
    protected String getPrefix() {
        return "resize_";
    }

    /** Use the shared resize limits for callers of the legacy GIF filter. */
    @Override
    public File runFilter(final File file, final Map<String, String[]> parameters) {
        return new ResizeImageFilter().runFilter(file, parameters);
    }

    /** Render the animation at the dimensions approved by the resize filter. */
    File resize(final File file, final File result, final Dimension target, final String prefix,
            final Map<String, String[]> parameters) {
        final int loop = Try.of(() -> Integer.parseInt(parameters.get(prefix + "loop")[0])).getOrElse(0);
        final int frames = Try.of(() -> Integer.parseInt(parameters.get(prefix + "frames")[0]))
                .getOrElse(Integer.MAX_VALUE);
        final File tmp = new File(result.getAbsolutePath() + "_" + System.nanoTime() + ".tmp");
        final ResampleOp resampler = new ResampleOp(target.width, target.height, ResampleOp.FILTER_TRIANGLE);
        try {
            final GifDecoder decoder = new GifDecoder();
            decoder.read(file.getAbsolutePath());
            if (decoder.getFrameCount() == 1) {
                final BufferedImage frame = resampler.filter(decoder.getFrame(0), null);
                try {
                    ImageIO.write(frame, "gif", tmp);
                } finally {
                    frame.flush();
                }
            } else {
                final AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                encoder.start(tmp.getAbsolutePath());
                encoder.setDelay(decoder.getDelay(0));
                encoder.setRepeat(loop);
                encoder.setSize(target.width, target.height);
                encoder.setQuality(20);
                for (int i = 0; i < Math.min(frames, decoder.getFrameCount()); i++) {
                    encoder.addFrame(resampler.filter(decoder.getFrame(i), null));
                }
                encoder.finish();
            }
            if (!tmp.renameTo(result)) {
                throw new IOException("unable to publish resized GIF");
            }
            return result;
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file: " + file, e);
        } finally {
            tmp.delete();
        }
    }
}
