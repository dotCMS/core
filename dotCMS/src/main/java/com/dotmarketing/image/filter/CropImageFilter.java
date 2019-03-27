package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;

public class CropImageFilter extends ImageFilter {

	public String[] getAcceptedParameters(){
		return  new String[] {
				"x (int) for left of crop",
				"y (int) for top of crop",
				"w (int) for width of crop",
				"h (int) for height of crop"
		};
	}
	
	
	
	public File runFilter(File file,  Map<String, String[]> parameters) {
		int x = parameters.get(getPrefix() + "x") != null ? Integer.parseInt(parameters.get(getPrefix() + "x")[0]) : 0;
		int y = parameters.get(getPrefix() + "y") != null ? Integer.parseInt(parameters.get(getPrefix() + "y")[0]) : 0;
		int w = parameters.get(getPrefix() + "w") != null ? Integer.parseInt(parameters.get(getPrefix() + "w")[0]) : 0;
		int h = parameters.get(getPrefix() + "h") != null ? Integer.parseInt(parameters.get(getPrefix() + "h")[0]) : 0;
		if (w == 0 || h == 0) {
			return file;
		}
		
		File resultFile = getResultsFile(file, parameters);
		
		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}
		
		BufferedImage src;
		try {
			src = ImageIO.read(file);
			if(x > src.getWidth() || y > src.getHeight()){
				return file;
				
			}
			
			if(x + w > src.getWidth()){
				w = src.getWidth()-x -1;
			}
			if(y + h > src.getHeight()){
				h = src.getHeight()-y-1;
			}
			
			
			BufferedImage out = src.getSubimage(x, y, w, h);
			ImageIO.write(out, FILE_EXT, resultFile);
			
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}

		
		return resultFile;
	}

}
