package com.dotmarketing.image.filter;

import com.dotcms.repackage.com.dotmarketing.jhlabs.image.HSBAdjustFilter;
import com.dotcms.repackage.com.dotmarketing.jhlabs.image.QuantizeFilter;
import com.dotmarketing.exception.DotRuntimeException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

public class QuantizeImageFilter extends ImageFilter {

	public String[] getAcceptedParameters() {
		return new String[] { 
				"h hue (double) between -1.0 and 1.0" ,
				"s saturation (double)  between -1.0 and 1.0" ,
				"b brightness (double)  between -1.0 and 1.0" ,
				
				
		};
	}

	public File runFilter(File file,  Map<String, String[]> parameters) {

		int numColors = parameters.get(getPrefix() + "colors") != null ? Integer.parseInt(parameters.get(getPrefix() + "colors")[0]) : 255;

		
		File resultFile = getResultsFile(file, parameters);
		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}


		QuantizeFilter filter = new QuantizeFilter();
		filter.setNumColors(numColors);
		filter.setDither(true);
		
		
		try {
			BufferedImage src = ImageIO.read(file);

			BufferedImage dst = filter.filter(src, null);
			ImageIO.write(dst, "png", resultFile);
			dst.flush();
	    } catch (Exception e) {
	        throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
	    }

		return resultFile;
	}

}
