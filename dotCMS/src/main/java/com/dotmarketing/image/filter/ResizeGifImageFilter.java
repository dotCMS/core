package com.dotmarketing.image.filter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.gif.AnimatedGifEncoder;
import com.dotmarketing.image.gif.GifDecoder;
import com.dotmarketing.util.Logger;
import com.twelvemonkeys.image.ResampleOp;

import io.vavr.control.Try;

public class ResizeGifImageFilter extends ImageFilter {
  public String[] getAcceptedParameters() {
    return new String[] {"w (int) specifies width", "h (int) specifies height, loop=true|false, maxFrames (int)",};
  }

  @Override
  protected String getPrefix() {

    return "resize_";
  }

  public File runFilter(final File file, final Map<String, String[]> parameters) {
    double w = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "w")[0])).getOrElse(0);
    double h = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "h")[0])).getOrElse(0);

    final int loop = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "loop")[0])).getOrElse(0);

    final int maxFrames = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "frames")[0])).getOrElse(Integer.MAX_VALUE);

    final File resultFile = getResultsFile(file, parameters);

    if (!overwrite(resultFile, parameters)) {
      return resultFile;
    }

    try {
      BufferedImage src = StrictImageReader.read(file);
      if (w == 0 && h == 0) {
        return file;
      } else if (w == 0 && h > 0) {
        w = Math.round(h * src.getWidth() / src.getHeight());
      } else if (w > 0 && h == 0) {
        h = Math.round(w * src.getHeight() / src.getWidth());
      }

      final int width = (int) w;
      final int height = (int) h;

      File tempResultFile = stagingFileFor(resultFile);
      try {
        readWriteGIF(file, tempResultFile, maxFrames, loop, width, height);
      } catch (IOException | RuntimeException e) {
        // never leave a partial gif behind in the rendition folder
        tempResultFile.delete();
        throw e;
      }
      publishRendition(tempResultFile, resultFile);
      return resultFile;
    } catch (Exception e) {
        throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
    }

  }

  private void readWriteGIF(File inputFile, File outputFile, final int maxFrames, final int loop, int width, int height)
      throws IOException {
    final BufferedImageOp resampler = new ResampleOp(width, height, ResampleOp.FILTER_TRIANGLE);
    final GifDecoder decoder = new GifDecoder();
    // GifDecoder reports a short or corrupt read through its status and keeps the frames it got,
    // so an unchecked read would encode and cache a GIF with frames missing
    if (decoder.read(inputFile.getAbsolutePath()) != GifDecoder.STATUS_OK) {
      throw new IOException("unable to fully decode gif: " + inputFile);
    }
    if(decoder.getFrameCount()==1) {
      BufferedImage dst = resampler.filter(StrictImageReader.read(inputFile), null);
      ImageIO.write(dst, "png", outputFile);
      dst.flush();
      return;
    }
    
    
    
    
    int frames = Math.min(maxFrames, decoder.getFrameCount());

      // AnimatedGifEncoder reports write failures through its return values, not exceptions
      AnimatedGifEncoder animatedGif = new AnimatedGifEncoder();
      if (!animatedGif.start(outputFile.getAbsolutePath())) {
        throw new IOException("unable to start writing gif: " + outputFile);
      }
      animatedGif.setDelay(decoder.getDelay(0));
      animatedGif.setRepeat(loop);
      animatedGif.setSize(width, height);
      animatedGif.setQuality(20);
      //animatedGif.setTransparent(Color.WHITE, false);
      for (int i = 0; i < frames; i++) {

        BufferedImage frame = decoder.getFrame(i); // frame i

        if (!animatedGif.addFrame(resampler.filter(frame, null))) {
          animatedGif.finish();
          throw new IOException("unable to write gif frame " + i + ": " + outputFile);
        }
      }
      if (!animatedGif.finish()) {
        throw new IOException("unable to finish writing gif: " + outputFile);
      }
    
  }

}
