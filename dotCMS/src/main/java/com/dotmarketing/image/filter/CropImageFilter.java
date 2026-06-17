package com.dotmarketing.image.filter;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;

/**
 * Crop a image focusing in a point
 */
public class CropImageFilter extends ImageFilter {

	public static final String X_PARAM_KEY = "x";
	public static final String Y_PARAM_KEY = "y";
	public static final String WIDTH_PARAM_KEY  = "w";
	public static final String HEIGHT_PARAM_KEY = "h";

	public String[] getAcceptedParameters(){
		return  new String[] {
				"x (int) for left of crop",
				"y (int) for top of crop",
				"w (int) for width of crop",
				"h (int) for height of crop",
				"fp (int,int) the focal point of the crop"
		};
	}
	
	
	
	public File runFilter(final File file,  final Map<String, String[]> parameters) {
		int x = parameters.get(getPrefix() + X_PARAM_KEY) != null ? Integer.parseInt(parameters.get(getPrefix() + X_PARAM_KEY)[0]) : 0;
		int y = parameters.get(getPrefix() + Y_PARAM_KEY) != null ? Integer.parseInt(parameters.get(getPrefix() + Y_PARAM_KEY)[0]) : 0;
		final float widthInput  = parameters.get(getPrefix() + WIDTH_PARAM_KEY)  != null ? Float.parseFloat(parameters.get(getPrefix()  + WIDTH_PARAM_KEY)[0]) : 0f;
		final float heightInput = parameters.get(getPrefix() + HEIGHT_PARAM_KEY) != null ? Float.parseFloat(parameters.get(getPrefix() + HEIGHT_PARAM_KEY)[0]) : 0f;
		int width  = 0;
		int height = 0;

		final File resultFile = getResultsFile(file, parameters);
		
		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}
		
		try {

			final BufferedImage src = ImageIO.read(file);
			final Dimension current = new Dimension(src.getWidth(), src.getHeight());

	        if(widthInput ==0 && heightInput >0){
	            height = Math.round(heightInput <=1  ? current.height * heightInput : heightInput);
	            width = Math.round(height * current.width / current.height);
	        }
	        else if(widthInput >0 && heightInput ==0){
	            width = Math.round(widthInput <= 1 ? current.width * widthInput : widthInput);
	            height = Math.round(width * current.height / current.width);
	        }
            else if(widthInput >0 && heightInput >0){
                width = Math.round(widthInput <= 1 ? current.width * widthInput : widthInput);
                height = Math.round(heightInput <= 1 ? current.height * heightInput : heightInput);
            }
	        else{
	            width = current.width;
	            height = current.height;
	        }
	        
	        if(x > current.getWidth() || y > current.getHeight()){
	            return file;   
	        }
	        


			
			if(x + width > current.width){
				width = src.getWidth()-x -1;
			}
			if(y + height > current.height){
				height = src.getHeight()-y-1;
			}

			final BufferedImage out = src.getSubimage(x, y, width, height);
			
            final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() +".tmp");

            
			
			ImageIO.write(out, FILE_EXT, tempResultFile);
			out.flush();
			tempResultFile.renameTo(resultFile);
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}

		
		return resultFile;
	}




}
