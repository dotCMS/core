package com.dotmarketing.image.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import com.dotmarketing.util.Logger;
import com.luciad.imageio.webp.WebPWriteParam;

public class WebPImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"q (int) between 0-100 specifies quality"
		};
	}
	public File runFilter(final File file, final Map<String, String[]> parameters) {

	    final int qualityParam = parameters.get(getPrefix() +"q") != null?Integer.parseInt(parameters.get(getPrefix() +"q")[0]):85;

	    Float quality = new Float(qualityParam);
	    quality = quality/100;

	    final File resultFile = getResultsFile(file, parameters, "webp");

	    if(!overwrite(resultFile,parameters)){
	        return resultFile;
	    }

	    resultFile.delete();

	    try {
	        final ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
	        final WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());

	        if(quality==1) {
	            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	            writeParam.setCompressionType("Lossless");
	        }else {
	            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	            writeParam.setCompressionType("Lossy");
	            writeParam.setCompressionQuality(quality);
	        }

			Logger.info(this.getClass(), "PROBANDO");
			Logger.info(this.getClass(), Arrays.asList(ImageIO.getReaderFormatNames()).toString());

	        writer.setOutput(new FileImageOutputStream(resultFile));
	        writer.write(null, new IIOImage(ImageIO.read(new FileInputStream(file)), null, null), writeParam);
	        writer.dispose();
	    } catch (IOException e) {
	    	e.printStackTrace();
	        Logger.error(this.getClass(), e.getMessage());
	    }

	    return resultFile;
	}
}