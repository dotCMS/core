package com.dotmarketing.image.filter;

import java.io.File;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import com.dotmarketing.exception.DotRuntimeException;
import com.luciad.imageio.webp.WebPWriteParam;

public class WebPImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"q (int) between 0-100 specifies quality"
		};
	}
	public File runFilter(final File file, final Map<String, String[]> parameters) {
        
	    final int qualityParam = parameters.get(getPrefix() +"q") != null?Integer.parseInt(parameters.get(getPrefix() +"q")[0]):85;

	    Float quality = Float.valueOf(qualityParam);
	    quality = quality/100;

	    final File resultFile = getResultsFile(file, parameters);

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

            final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() +".tmp");



	        writer.setOutput(new FileImageOutputStream(tempResultFile));
	        writer.write(null, new IIOImage(ImageIO.read(file), null, null), writeParam);
	        writer.dispose();
            tempResultFile.renameTo(resultFile);
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
        }

	    return resultFile;
	}
	
    @Override
    public File getResultsFile(File file, Map<String, String[]> parameters) {
        try {
            return super.getResultsFile(file, parameters, "webp");
        }
        catch(Exception e) {
            return new File(System.getProperty("java.io.tmpdir") + file.separator + System.currentTimeMillis() + "." + "webp");
        }

    }
	
	
	
}
