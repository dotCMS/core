package com.dotmarketing.image.filter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import com.dotmarketing.exception.DotRuntimeException;

public class PngImageFilter extends ImageFilter {

	public File runFilter(File file,   Map<String, String[]> parameters) {

		File resultFile = getResultsFile(file, parameters);

		if(!overwrite(resultFile,parameters)){
			return resultFile;
		}
		

		try{
	        final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() + ".tmp.png");

			BufferedImage src = ImageIO.read(file);
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("png");
			ImageWriter writer = iter.next();
			ImageWriteParam iwp = writer.getDefaultWriteParam();
			BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D graphics = dst.createGraphics();  


			graphics.drawImage(src, 0, 0, src.getWidth(), src.getHeight(),null);
			try(ImageOutputStream ios = ImageIO.createImageOutputStream(tempResultFile)){
    			writer.setOutput(ios);
    			writer.write(null,new IIOImage(dst,null,null),iwp);
    			ios.flush();
    			writer.dispose();
			}
            if (!tempResultFile.renameTo(resultFile)) {
                throw new DotRuntimeException("unable to create file:" + resultFile);
            }
            return resultFile;
			
			
	    } catch (Exception e) {
	        throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
	    }
		
	}
	

}
