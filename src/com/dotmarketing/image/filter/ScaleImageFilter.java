package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;
import com.dotmarketing.jhlabs.image.ScaleFilter;

public class ScaleImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"w (int) specifies width",
				"h (int) specifies height",
		};
	}
	public File runFilter(File file,    Map<String, String[]> parameters) {
		int w = parameters.get(getPrefix() +"w") != null?Integer.parseInt(parameters.get(getPrefix() +"w")[0]):0;
		int h = parameters.get(getPrefix() +"h") != null?Integer.parseInt(parameters.get(getPrefix() +"h")[0]):0;
		
		File resultFile = getResultsFile(file, parameters);

		
		
		if(!overwrite(resultFile,parameters)){
			return resultFile;
		}
		resultFile.delete();

		
		
		try {
			
			BufferedImage src = ImageIO.read(file);
			if(w ==0 && h ==0){
				return file;
			}
			if(w ==0 && h >0){
				w = h * src.getWidth() / src.getHeight();
			}
			if(w >0 && h ==0){
				h =w * src.getHeight() / src.getWidth();
			}
			
			
			
			
			
			
			
			ScaleFilter filter = new ScaleFilter(w,h);

			BufferedImage dst = new BufferedImage(w, h,
					BufferedImage.TYPE_INT_ARGB);

			 dst = filter.filter(src, dst);
			ImageIO.write(dst, "png", resultFile);
			return resultFile;
			
			
			
			//fos = new FileOutputStream(resultFile);
			//ImageResizeUtils.resizeImage(new FileInputStream(file), fos, FILE_EXT, width, height);
		} catch (FileNotFoundException e) {
			Logger.error(this.getClass(), e.getMessage());
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}
		
		
		
		
		
		return resultFile;
	}
	

}
