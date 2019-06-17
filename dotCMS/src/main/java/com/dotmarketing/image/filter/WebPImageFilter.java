package com.dotmarketing.image.filter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
				"q (int) specifies quality",
				"p (String) if set specifies progressive"
		};
	}
	public File runFilter(File file,   Map<String, String[]> parameters) {
    int quality = parameters.get(getPrefix() +"q") != null?Integer.parseInt(parameters.get(getPrefix() +"q")[0]):85;
    boolean progressive = (parameters.get(getPrefix() +"p") != null && Boolean.parseBoolean(parameters.get(getPrefix() +"p")[0]));

    
    Float q = new Float(quality);
    q = q/100;
    
    File resultFile = getResultsFile(file, parameters, "webp");

    if(!overwrite(resultFile,parameters)){
      return resultFile;
    }
    
    resultFile.delete();

    try {
      ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
      WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
      
      if(q==1) {
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType("Lossless");

      }else {
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType("Lossy");
        writeParam.setCompressionQuality(q);
      }
      
      if(progressive) {
        writeParam.setProgressiveMode(WebPWriteParam.MODE_DEFAULT);
        
      }
      
      
      

      writer.setOutput(new FileImageOutputStream(resultFile));
      writer.write(null, new IIOImage(ImageIO.read(file), null, null), writeParam);
      writer.dispose();
    } catch (FileNotFoundException e) {
      Logger.error(this.getClass(), e.getMessage());
    } catch (IOException e) {
      Logger.error(this.getClass(), e.getMessage());
    }
    
    
    
		
		
		return resultFile;
	}
	

}
