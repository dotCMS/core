package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

public class ResizeImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"w (int) specifies width",
				"h (int) specifies height",
				"i (int) interpolation"
		};
	}
	public File runFilter(final File file,    Map<String, String[]> parameters) {
	    
		int w = parameters.get(getPrefix() +"w") != null?Integer.parseInt(parameters.get(getPrefix() +"w")[0]):0;
		int h = parameters.get(getPrefix() +"h") != null?Integer.parseInt(parameters.get(getPrefix() +"h")[0]):0;
		final int resampleOpts = Try.of(()-> Integer.parseInt(parameters.get(getPrefix() +"ro")[0])).getOrElse(ImageFilterApiImpl.DEFAULT_RESAMPLE_OPT);
		
		if(file.getName().endsWith(".gif")) {
		  return new ResizeGifImageFilter().runFilter(file, parameters);
		}
		
		
        if(w ==0 && h ==0){
            return file;
        }
		
		File resultFile = getResultsFile(file, parameters);
		
		if(!overwrite(resultFile,parameters)){
			return resultFile;
		}
		resultFile.delete();
		
		
		final Dimension originalSize = ImageFilterAPI.apiInstance.get().getWidthHeight(file);
		
        final int width = (int) (w == 0 && h > 0 ? Math.round(h * originalSize.getWidth()) / originalSize.getHeight()
                        : w);

        final int height = (int) (w > 0 && h == 0 ? Math.round(w * originalSize.getHeight() / originalSize.getWidth())
                        : h);
		


        try {
            File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() + ".tmp");
            // resample from stream
            BufferedImage srcImage = ImageFilterAPI.apiInstance.get().intelligentResize(file, width, height,resampleOpts);
            ImageIO.write(srcImage, "png", tempResultFile);
            srcImage.flush();
            srcImage = null;
            tempResultFile.renameTo(resultFile);

            return resultFile;

        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
        }

    }

	


}
