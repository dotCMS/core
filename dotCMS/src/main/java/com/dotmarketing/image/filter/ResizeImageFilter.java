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
				"i (int) interpolation",
				"maxw (int) specifies maxWidth",
				"maxh (int) specifies maxHeight",
                "minw (int) specifies minWidth",
                "minh (int) specifies minHeight"
		};
	}
	public File runFilter(final File file,    Map<String, String[]> parameters) {

        final int w = Try.of(()-> Integer.parseInt(parameters.getOrDefault(getPrefix() + "w", new String[]{"0"})[0])).getOrElse(0);
		final int h = Try.of(()-> Integer.parseInt(parameters.getOrDefault(getPrefix() + "h", new String[]{"0"})[0])).getOrElse(0);
		final int resampleOpts = Try.of(()-> Integer.parseInt(parameters.get(getPrefix() +"ro")[0])).getOrElse(ImageFilterApiImpl.DEFAULT_RESAMPLE_OPT);
		final int mxw = Try.of(()-> Integer.parseInt(parameters.getOrDefault(getPrefix() + "maxw", new String[]{"0"})[0])).getOrElse(0);
		final int mxh = Try.of(()-> Integer.parseInt(parameters.getOrDefault(getPrefix() + "maxh", new String[]{"0"})[0])).getOrElse(0);
		final int mnw = Try.of(()-> Integer.parseInt(parameters.getOrDefault(getPrefix() + "minw", new String[]{"0"})[0])).getOrElse(0);
		final int mnh = Try.of(()-> Integer.parseInt(parameters.getOrDefault(getPrefix() + "minh", new String[]{"0"})[0])).getOrElse(0);
        
        
        
		if(file.getName().endsWith(".gif")) {
		  return new ResizeGifImageFilter().runFilter(file, parameters);
		}

		
        if (w == 0 && h == 0 && mxh == 0 && mxw == 0&& mnh == 0 && mnw == 0) {
            return file;
        }
		
		File resultFile = getResultsFile(file, parameters);
		
		if(!overwrite(resultFile,parameters)){
			return resultFile;
		}
		resultFile.delete();
		
		
		final Dimension originalSize = ImageFilterAPI.apiInstance.get().getWidthHeight(file);

		final Dimension newSize = new ResizeCalc.Builder(originalSize)
		                .desiredHeight(h)
		                .desiredWidth(w)
		                .maxWidth(mxw)
		                .maxHeight(mxh)
		                .minHeight(mnh)
		                .minWidth(mnw)
		                .build()
		                .getDim();
		
		
		if(originalSize.equals(newSize)) {
		    return file;
		}
		

        try {
            File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() + ".tmp");
            // resample from stream
            BufferedImage srcImage = ImageFilterAPI.apiInstance.get().intelligentResize(file, newSize.width, newSize.height,resampleOpts);
            ImageIO.write(srcImage, "png", tempResultFile);
            srcImage.flush();
            srcImage = null;
            if(tempResultFile.renameTo(resultFile)) {
                return resultFile;
            }
			throw new DotRuntimeException("unable to create tmp file :" + resultFile);
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
        }

    }

	
	
	
	
	


}
