package com.dotmarketing.image.filter;

import com.dotmarketing.util.Logger;
import com.twelvemonkeys.image.ResampleOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;

public class ResizeImageFilter extends ImageFilter {
	public String[] getAcceptedParameters(){
		return  new String[] {
				"w (int) specifies width",
				"h (int) specifies height",
		};
	}
	public File runFilter(File file,    Map<String, String[]> parameters) {
		double w = parameters.get(getPrefix() +"w") != null?Integer.parseInt(parameters.get(getPrefix() +"w")[0]):0;
		double h = parameters.get(getPrefix() +"h") != null?Integer.parseInt(parameters.get(getPrefix() +"h")[0]):0;
		
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
				w = Math.round(h * src.getWidth() / src.getHeight());
			}
			if(w >0 && h ==0){
				h = Math.round(w * src.getHeight() / src.getWidth());
			}
			
			int width    =      (int) w;    
			int hieght     =     (int) h;


			BufferedImageOp resampler = new ResampleOp(width, hieght, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
			BufferedImage output = resampler.filter(ImageIO.read(file), null);
			ImageIO.write(output, "png", resultFile);
			return resultFile;

		} catch (FileNotFoundException e) {
			Logger.error(this.getClass(), e.getMessage());
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}
		
		return resultFile;
	}

}
