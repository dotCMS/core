package com.dotmarketing.image.filter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import com.dotmarketing.util.Logger;

public class GifImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"q (int) specifies quality",

		};
	}
	public File runFilter(File file,   Map<String, String[]> parameters) {

		File resultFile = getResultsFile(file, parameters, "gif");

		if(!overwrite(resultFile,parameters)){
			return resultFile;
		}
		
		resultFile.delete();

		try {
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("gif");
			ImageWriter writer = iter.next();
			ImageWriteParam iwp = writer.getDefaultWriteParam();

			BufferedImage src = ImageIO.read(file);
			BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D graphics = dst.createGraphics();  

			graphics.setPaint ( new Color ( 255, 255, 255 ) );

			graphics.fillRect(0, 0, src.getWidth(), src.getHeight());
			graphics.drawImage(src, 0, 0, src.getWidth(), src.getHeight(),null);
			ImageOutputStream ios = ImageIO.createImageOutputStream(resultFile);
			writer.setOutput(ios);
			writer.write(null,new IIOImage(dst,null,null),iwp);
			ios.flush();
			writer.dispose();
			ios.close();
			//writer.setOutput(output);

		//	IIOImage image = new IIOImage(src, null, null);
		//	writer.write(null, image, iwp);
		//	writer.dispose();
			

		} catch (FileNotFoundException e) {
			Logger.error(this.getClass(), e.getMessage());
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}
		
		
		
		
		
		return resultFile;
	}
	

}
