package com.dotmarketing.image.filter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

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
    resultFile.delete();

    try {
      BufferedImage src = ImageIO.read(file);
      if (w == 0 && h == 0) {
        return file;
      } else if (w == 0 && h > 0) {
        w = Math.round(h * src.getWidth() / src.getHeight());
      } else if (w > 0 && h == 0) {
        h = Math.round(w * src.getHeight() / src.getWidth());
      }

      final int width = (int) w;
      final int height = (int) h;

      readWriteGIF(file, resultFile, maxFrames, loop, width, height);
      return resultFile;
    } catch (Exception e) {
      Logger.warnAndDebug(this.getClass(), "error:" + e.getStackTrace()[0].getClassName() + " " + e.getMessage(), e);

    }
    return file;
  }

  private void readWriteGIF(File inputFile, File outputFile, final int maxFrames, final int loop, int width, int height)
      throws IOException {
    final BufferedImageOp resampler = new ResampleOp(width, height, ResampleOp.FILTER_TRIANGLE);
    final GifDecoder decoder = new GifDecoder();
    decoder.read(inputFile.getAbsolutePath());
    if(decoder.getFrameCount()==1) {
      BufferedImage dst = resampler.filter(ImageIO.read(inputFile), null);
      ImageIO.write(dst, "png", outputFile);
      dst.flush();
      return;
    }
    
    
    
    
    int frames = Math.min(maxFrames, decoder.getFrameCount());

      AnimatedGifEncoder animatedGif = new AnimatedGifEncoder();
      animatedGif.start(outputFile.getAbsolutePath());
      animatedGif.setDelay(decoder.getDelay(0));
      animatedGif.setRepeat(loop);
      animatedGif.setSize(width, height);
      animatedGif.setQuality(20);
      //animatedGif.setTransparent(Color.WHITE, false);
      for (int i = 0; i < frames; i++) {

        BufferedImage frame = decoder.getFrame(i); // frame i

        animatedGif.addFrame(resampler.filter(frame, null));
        // animatedGif.addFrame(frame);
      }
      animatedGif.finish();
    
  }

}
