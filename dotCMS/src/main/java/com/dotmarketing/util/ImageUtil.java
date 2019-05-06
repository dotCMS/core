package com.dotmarketing.util;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.*;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Iterator;

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

		if (suffix.equalsIgnoreCase("svg")){
			return getSVGImageDimension( imgFile );
		}else{
			Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
			if (iter.hasNext()) {
				Dimension dimension = getImageDimension(imgFile, iter);
				if (dimension != null) return dimension;
			}
		}

		throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
	}

	private Dimension getSVGImageDimension( File imgFile ) {
		try {
			SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(
					XMLResourceDescriptor.getXMLParserClassName());

			final InputStream is = Files.newInputStream(imgFile.toPath());

			UserAgent agent = new UserAgentAdapter();
			DocumentLoader loader = new DocumentLoader(agent);
			BridgeContext context = new BridgeContext(agent, loader);
			context.setDynamic(true);
			GVTBuilder builder = new GVTBuilder();
			GraphicsNode root = builder.build(context, factory.createDocument(
					imgFile.toURI().toURL().toString(), is));

			return new Dimension((int) root.getPrimitiveBounds().getWidth(), (int) root.getPrimitiveBounds().getHeight());
		}catch(IOException e){
			return null;
		}
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