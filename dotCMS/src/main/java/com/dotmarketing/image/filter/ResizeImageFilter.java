package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;
import com.dotmarketing.util.Logger;

public class ResizeImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"w (int) specifies width",
				"h (int) specifies height",
		};
	}
	public File runFilter(final File file,    Map<String, String[]> parameters) {
		double w = parameters.get(getPrefix() +"w") != null?Integer.parseInt(parameters.get(getPrefix() +"w")[0]):0;
		double h = parameters.get(getPrefix() +"h") != null?Integer.parseInt(parameters.get(getPrefix() +"h")[0]):0;
		
		
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
			//resample from stream
			BufferedImage srcImage = ImageFilterAPI.apiInstance.get().intelligentResize(file,width,height);


            ImageIO.write(srcImage, "png", resultFile);
            srcImage.flush();
            srcImage=null;
            return resultFile;

		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage());
		}
		
		return resultFile;
	}

	


}
