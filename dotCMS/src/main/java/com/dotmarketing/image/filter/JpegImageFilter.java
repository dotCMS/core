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
import javax.imageio.stream.ImageOutputStream;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

public class JpegImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"q (int) specifies quality",

		};
	}
	public File runFilter(File file,   Map<String, String[]> parameters) {
		int quality = parameters.get(getPrefix() +"q") != null?Integer.parseInt(parameters.get(getPrefix() +"q")[0]):85;
        boolean progressive = (parameters.get(getPrefix() +"p") != null);


		
		Double q = new Double(quality);
		q = q/100;
		
		File resultFile = getResultsFile(file, parameters, "jpg");

		if(!overwrite(resultFile,parameters)){
			return resultFile;
		}
		
		resultFile.delete();

		try {
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
			ImageWriter writer = iter.next();
			ImageWriteParam iwp = writer.getDefaultWriteParam();
			iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			
			iwp.setCompressionQuality(q.floatValue());   
			if(progressive){
			  iwp.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
			}
			BufferedImage src = ImageIO.read(file);
			BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D graphics = dst.createGraphics();  

			graphics.setPaint ( new Color ( 255, 255, 255 ) );

			graphics.fillRect(0, 0, src.getWidth(), src.getHeight());
			graphics.drawImage(src, 0, 0, src.getWidth(), src.getHeight(),null);
			
			
            final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() +".tmp.jpg");

            
			try(ImageOutputStream ios = ImageIO.createImageOutputStream(tempResultFile)){
    			writer.setOutput(ios);
    			writer.write(null,new IIOImage(dst,null,null),iwp);
    			ios.flush();
    			writer.dispose();
			}
			
	        tempResultFile.renameTo(resultFile);


		} catch (Exception e) {
			throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
		}
		
		
		
		
		
		return resultFile;
	}
	

}
