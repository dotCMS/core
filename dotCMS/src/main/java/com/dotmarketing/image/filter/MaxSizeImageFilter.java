package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;

public class MaxSizeImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"w (int) specifies width",
				"h (int) specifies height",
		};
	}
	
	int maxImageSize() {
	    return Config.getIntProperty("IMAGE_MAX_IMAGE_SIZE", 5000);
	}
	
	
	public File runFilter(final File file,    Map<String, String[]> parameters) {
		int maxW = parameters.get(getPrefix() +"mw") != null?Integer.parseInt(parameters.get(getPrefix() +"mw")[0]):maxImageSize() ;
		int maxH = parameters.get(getPrefix() +"mh") != null?Integer.parseInt(parameters.get(getPrefix() +"mh")[0]):maxImageSize() ;
		
		

		
        if(maxW ==0 && maxH ==0){
            return file;
        }
		
		File resultFile = getResultsFile(file, parameters);
		
		if(!overwrite(resultFile,parameters)){
			return resultFile;
		}
		resultFile.delete();
		
		
		Dimension widthHeight = ImageFilterAPI.apiInstance.get().getWidthHeight(file);
		
        if(widthHeight.height<=maxH && widthHeight.width<=maxW) {
            Try.run(()->FileUtil.copyFile(file, resultFile)); 
            return resultFile;
        }
        
		maxW = Math.min(widthHeight.width, maxW);
		maxH = Math.min(widthHeight.height, maxH);
		
		

        try {
			//subsample from stream
			BufferedImage srcImage = ImageFilterAPI.apiInstance.get().subsampleImage(file,maxW,maxH);


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
