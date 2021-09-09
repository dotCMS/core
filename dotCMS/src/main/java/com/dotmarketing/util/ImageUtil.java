package com.dotmarketing.util;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public final class ImageUtil {

	private static class UtilHolder {
		public static final ImageUtil INSTANCE = new ImageUtil();
	}

	public static ImageUtil getInstance() {
		return UtilHolder.INSTANCE;
	}

	/**
	 * Returns a java.awt.Dimention that contains the image's
	 * width and height
	 * @param imgFile
	 * @return
	 * @throws IOException
	 */
	public Dimension getDimension(File imgFile) throws IOException {

		if (imgFile == null) {
			throw new IOException("null file passed in");
		}
		int pos = imgFile.getName().lastIndexOf(".");
		if (pos == -1) {
			throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
		}
		String suffix = imgFile.getName().substring(pos + 1);


		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		if (iter.hasNext()) {
			Dimension dimension = getImageDimension(imgFile, iter);
			if (dimension != null) return dimension;
		}
		

		throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
	}


	private Dimension getImageDimension(File imgFile, Iterator<ImageReader> iter) {
		ImageReader reader = iter.next();
		try {
            ImageInputStream stream = new FileImageInputStream(imgFile);
            reader.setInput(stream);
            int width = reader.getWidth(reader.getMinIndex());
            int height = reader.getHeight(reader.getMinIndex());
            return new Dimension(width, height);
        } catch (IOException e) {
            // fall back if we cannot read the image headers
            try{
                BufferedImage src = ImageIO.read(imgFile);
                int width = src.getWidth();
                int height = src.getHeight();
                return new Dimension(width, height);
            } catch (IOException e2) {
                Logger.warn(this, "Error reading: " + imgFile.getAbsolutePath(), e);
            }
        } finally {
            reader.dispose();
        }
		return null;
	}

}