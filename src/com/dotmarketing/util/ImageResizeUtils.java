package com.dotmarketing.util;

/**
 * @author Maria
 * @author David H Torres
 *
 */
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import javax.imageio.ImageIO;

public class ImageResizeUtils {
	
	//Default thumbnail size
	public static final int DEFAULT_HEIGHT = Config.getIntProperty("DEFAULT_HEIGHT");
	public static final int DEFAULT_WIDTH = Config.getIntProperty("DEFAULT_WIDTH");
	public static final Color DEFAULT_BG_COLOR = new Color(Config.getIntProperty("DEFAULT_BG_R_COLOR"), Config.getIntProperty("DEFAULT_BG_G_COLOR"), Config.getIntProperty("DEFAULT_BG_B_COLOR"));

	public static void generateThumbnail(String imagePath, String imageName, String fileExtension, Color bgColor) throws FileNotFoundException, IOException, InterruptedException {
		generateThumbnail(imagePath,imageName,fileExtension,"_thumb",DEFAULT_HEIGHT,DEFAULT_WIDTH, bgColor);
	}
	
	public static void generateThumbnail(String imagePath, String imageName, String fileExtension) throws FileNotFoundException, IOException, InterruptedException {
		generateThumbnail(imagePath,imageName,fileExtension,"_thumb",DEFAULT_HEIGHT,DEFAULT_WIDTH, DEFAULT_BG_COLOR);
	}

	/**
	 * 
	 * Generates an image thumbnail filling the background with the given background color to preserve the original image ratio.
	 * If both width and height are passed as 0 then the defaults (DEFAULT_HEIGHT, DEFAULT_WIDTH) will be used
	 * 
	 * @param imagePath
	 * @param imageName
	 * @param fileExtension
	 * @param fileOutPutSuffix
	 * @param height The desired resulting height, if height <= 0 the system will try to preserve the original image ratio based on the passed width
	 * @param width The desired resulting width, if width <= 0 the system will try to preserve the original image ratio based on the passed height
	 * @param bgColor If null then the default DEFAULT_BG_COLOR is used.
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * 
	 */
	public static void generateThumbnail(String imagePath, String imageName, String fileExtension, String filePrefix, int width, int height, Color bgColor) throws FileNotFoundException, IOException, InterruptedException {
		String fullImagePath = imagePath + imageName + "." + fileExtension;
		String resultImagePath = imagePath +filePrefix +"-"+ imageName +  "." + fileExtension;

		// delete the old thumbnail
		new File(resultImagePath).delete();
		generateThumbnail(new FileInputStream(new File(fullImagePath)), new FileOutputStream(new File(resultImagePath)), fileExtension, width, height, bgColor);
	}
	
	public static void generateThumbnail(InputStream input, OutputStream output, String format, int width, int height, Color bgColor) throws IOException, InterruptedException {

		if (height <= 0 && width <= 0) {
			height = DEFAULT_HEIGHT;
			width = DEFAULT_WIDTH;
		}

		if (bgColor == null)
			bgColor = DEFAULT_BG_COLOR;

		byte[] imageData = new byte[input.available()];
		input.read(imageData);
		Image image = Toolkit.getDefaultToolkit().createImage(imageData);

		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(image, 0);
		mediaTracker.waitForID(0);

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
			thumbWidth = (int) (thumbHeight * imageRatio);
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
		BufferedImage bgImage = new BufferedImage(width, height, getFieldIntValue(Config.getStringProperty("DEFAULT_IMAGE_TYPE")));
		Graphics2D resultGraphics = bgImage.createGraphics();
		resultGraphics.setColor(bgColor);
		resultGraphics.fillRect(0, 0, width, height);

		BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, getFieldIntValue(Config.getStringProperty("DEFAULT_IMAGE_TYPE")));
		Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_INTERPOLATION")));
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_RENDERING")));
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ANTIALIASING")));
		graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_COLOR_RENDERING")));
		graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ALPHA_INTERPOLATION")));
		graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, bgColor, null);
		graphics2D.dispose();

		// compute offsets to center image in its space
		int offsetX = (width - thumbImage.getWidth()) / 2;
		int offsetY = (height - thumbImage.getHeight()) / 2;
		resultGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_INTERPOLATION")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_RENDERING")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ANTIALIASING")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_COLOR_RENDERING")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ALPHA_INTERPOLATION")));
		resultGraphics.drawImage(thumbImage, null, offsetX, offsetY);
		resultGraphics.dispose();

		// save thumbnail image to OUTFILE
		BufferedOutputStream out = new BufferedOutputStream(output);
		ImageIO.write(bgImage, format, out);
		out.close();

		Logger.debug(ImageResizeUtils.class, "Done.");
		
	}
	
	/**
	 * This method resizes the given image, does not preserve the aspect ratio of the image.
	 * If both passed width and height are <= 0 then the defaults will be used
	 * 
	 * @param imagePath   The path where is going to be the image
	 * @param imageName   The image name
	 * @param fileExtension The image Extension
	 * @param fileOutPutName The thumbnail name
	 * @param height  The resulting image height if height < 0 is passed then it will try to preserve the aspect ratio of the image
	 * @param width  The Image Width  if width < 0 is passed then it will try to preserve the aspect ratio of the image
	 * @param bgColor Image BackGround COlor
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void resizeImage(String imagePath, String imageName, String fileExtension, String fileOutPutName, int width, int height) throws FileNotFoundException, IOException, InterruptedException {
		
		String fullImagePath = imagePath + imageName + "." + fileExtension;
		String resultImagePath = imagePath + fileOutPutName + "." + fileExtension;

		// delete the old thumbnail
		new File(resultImagePath).delete();
		resizeImage(new FileInputStream(new File(fullImagePath)), new FileOutputStream(new File(resultImagePath)), fileExtension, width, height);

	}
	
	public static void resizeImage(InputStream input, OutputStream output, String format, int width, int height) throws IOException, InterruptedException {
		
		if (height <= 0 && width <= 0) {
			height = DEFAULT_HEIGHT;
			width = DEFAULT_WIDTH;
		}

		byte[] imageData = new byte[input.available()];
		input.read(imageData);
		Image image = Toolkit.getDefaultToolkit().createImage(imageData);
		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(image, 0);
		mediaTracker.waitForID(0);

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
			thumbWidth = (int) (thumbHeight * imageRatio);
		}

		if (thumbWidth == 0)
			thumbWidth = 1;
		if (thumbHeight == 0)
			thumbHeight = 1;

		if (width <= 0)
			width = (int) Math.ceil(height * imageRatio);
		if (height <= 0)
			height = (int) Math.ceil(width / imageRatio);
		
		
		// if the image is the same size, do not touch it.
		if(imageWidth == thumbWidth && imageHeight == thumbHeight){
			for(byte b : imageData){
				output.write(b);
			}
			output.close();
			return;
		}
		// draw original image to thumbnail image object and
		// scale it to the new size on-the-fly
		// draw original image to thumbnail image object and
		// scale it to the new size on-the-fly
		BufferedImage bgImage = new BufferedImage(width, height, getFieldIntValue(Config.getStringProperty("DEFAULT_IMAGE_TYPE")));
		Graphics2D resultGraphics = bgImage.createGraphics();
		Color transparent = new Color(255,255,255,0);
		resultGraphics.setColor(transparent);
		resultGraphics.fillRect(0, 0, width, height);

		BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, getFieldIntValue(Config.getStringProperty("DEFAULT_IMAGE_TYPE")));
		Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_INTERPOLATION")));
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_RENDERING")));
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ANTIALIASING")));
		graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_COLOR_RENDERING")));
		graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ALPHA_INTERPOLATION")));
		graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, transparent, null);
		graphics2D.dispose();

		// compute offsets to center image in its space
		int offsetX = (width - thumbImage.getWidth()) / 2;
		int offsetY = (height - thumbImage.getHeight()) / 2;
		resultGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_INTERPOLATION")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_RENDERING")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ANTIALIASING")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_COLOR_RENDERING")));
		resultGraphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, getFieldObjectValue(Config.getStringProperty("DEFAULT_KEY_ALPHA_INTERPOLATION")));
		resultGraphics.drawImage(thumbImage, null, offsetX, offsetY);
		resultGraphics.dispose();

		// save thumbnail image to OUTFILE
		BufferedOutputStream out = new BufferedOutputStream(output);
		ImageIO.write(thumbImage, format, out);
		out.close();
		Logger.debug(ImageResizeUtils.class, "Done.");
		
	}
	
	private static int getFieldIntValue(String fieldPath) {
		int fieldValue;
		
		try {
			int separatorIndex = fieldPath.lastIndexOf(".");
			String className = fieldPath.substring(0, separatorIndex);
			String fieldName = fieldPath.substring(separatorIndex + 1);
			Class<?> clazz = Class.forName(className);
			Field field = clazz.getField(fieldName);
			fieldValue = field.getInt(null);
			Logger.debug(ImageResizeUtils.class, clazz.toString());
		} catch (Exception e) {
			fieldValue = -1;
			Logger.debug(ImageResizeUtils.class, e.toString());
		}
		
		return fieldValue;
	}
	
	private static Object getFieldObjectValue(String fieldPath) {
		Object fieldValue;
		
		try {
			int separatorIndex = fieldPath.lastIndexOf(".");
			String className = fieldPath.substring(0, separatorIndex);
			String fieldName = fieldPath.substring(separatorIndex + 1);
			Class<?> clazz = Class.forName(className);
			Field field = clazz.getField(fieldName);
			fieldValue = field.get(null);
			Logger.debug(ImageResizeUtils.class, clazz.toString());
		} catch (Exception e) {
			fieldValue = -1;
			Logger.debug(ImageResizeUtils.class, e.toString());
		}
		
		return fieldValue;
	}
}