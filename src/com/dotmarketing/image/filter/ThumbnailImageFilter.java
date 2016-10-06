package com.dotmarketing.image.filter;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.ImageResizeUtils;
import com.dotmarketing.util.Logger;
import com.twelvemonkeys.image.ResampleOp;

public class ThumbnailImageFilter extends ImageFilter {
	public String[] getAcceptedParameters() {
		return new String[] { "w (int) specifies width", "h (int) specifies height",
				"bg (int) must be 9 digits of rgb (000000000=black, 255255255=white) for background color"

		};
	}
    public static final int DEFAULT_HEIGHT = Config.getIntProperty("DEFAULT_HEIGHT",100);
    public static final int DEFAULT_WIDTH = Config.getIntProperty("DEFAULT_WIDTH",100);
    public static final Color DEFAULT_BG_COLOR = new Color(Config.getIntProperty("DEFAULT_BG_R_COLOR"), Config.getIntProperty("DEFAULT_BG_G_COLOR"), Config.getIntProperty("DEFAULT_BG_B_COLOR"));

	public File runFilter(File file,  Map<String, String[]> parameters) {

		int height = parameters.get(getPrefix() + "h") != null ? Integer.parseInt(parameters.get(getPrefix() + "h")[0])
				: 0;
		int width = parameters.get(getPrefix() + "w") != null ? Integer.parseInt(parameters.get(getPrefix() + "w")[0])
				: 0;
		String rgb = parameters.get(getPrefix() + "bg") != null ? parameters.get(getPrefix() + "bg")[0] : "255255255";
		Color color = new Color(Integer.parseInt(rgb.substring(0, 3)), Integer.parseInt(rgb.substring(3, 6)),
				Integer.parseInt(rgb.substring(6)));

		File resultFile = getResultsFile(file, parameters);

		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}

		FileOutputStream fos = null;
		try {
			resultFile.delete();
	        if (height <= 0 && width <= 0) {
	            height = DEFAULT_HEIGHT;
	            width = DEFAULT_WIDTH;
	        }

	        if (color == null){
	            color = DEFAULT_BG_COLOR;
	        }
	        

	        Image image = ImageIO.read(file);



	        // determine thumbnail size from WIDTH and HEIGHT
	        int imageWidth = image.getWidth(null);
	        int imageHeight = image.getHeight(null);
	        double imageRatio = (double) imageWidth / (double) imageHeight;

	        int thumbWidth = width;
	        int thumbHeight = height;
	        if (thumbWidth <= 0)
	            thumbWidth = (int) (thumbHeight * imageRatio);
	        if (thumbHeight <= 0)
	            thumbHeight = (int) (thumbWidth / imageRatio);
	        double thumbRatio = (double) thumbWidth / (double) thumbHeight;

	        if (thumbRatio < imageRatio) {
	            thumbHeight = (int) Math.ceil((thumbWidth / imageRatio));
	        } else {
	            thumbWidth = (int) Math.ceil((thumbHeight * imageRatio));
	        }

	        if (thumbWidth == 0)
	            thumbWidth = 1;
	        if (thumbHeight == 0)
	            thumbHeight = 1;

	        if (width <= 0)
	            width = (int) Math.ceil(height * imageRatio);
	        if (height <= 0)
	            height = (int) Math.ceil(width / imageRatio);

	        // draw original image to thumbnail image object and
	        // scale it to the new size on-the-fly
	        BufferedImage bgImage = new BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
	        Graphics2D resultGraphics = bgImage.createGraphics();
	        resultGraphics.setColor(color);
	        resultGraphics.fillRect(0, 0, width, height);

	        
	        
	        BufferedImageOp resampler = new ResampleOp(thumbWidth, thumbHeight, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
	        BufferedImage thumbImage = resampler.filter(ImageIO.read(file), null);


	        // compute offsets to center image in its space
	        int offsetX = (width - thumbImage.getWidth()) / 2;
	        int offsetY = (height - thumbImage.getHeight()) / 2;

	        resultGraphics.drawImage(thumbImage, null, offsetX, offsetY);
	        resultGraphics.dispose();

	        // save thumbnail image to OUTFILE
	        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(resultFile));
	        ImageIO.write(bgImage, "png", out);
	        out.close();

	        Logger.debug(ImageResizeUtils.class, "Done.");
		} catch (FileNotFoundException e) {
			Logger.error(this.getClass(), e.getMessage());
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					Logger.error(this.getClass(), "should not be here");
				}
			}
		}

		return resultFile;

	}

}
