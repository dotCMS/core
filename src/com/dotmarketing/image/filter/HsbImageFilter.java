package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;
import com.dotmarketing.jhlabs.image.HSBAdjustFilter;

public class HsbImageFilter extends ImageFilter {

	public String[] getAcceptedParameters() {
		return new String[] { 
				"h hue (double) between -1.0 and 1.0" ,
				"s saturation (double)  between -1.0 and 1.0" ,
				"b brightness (double)  between -1.0 and 1.0" ,
				
				
		};
	}

	public File runFilter(File file,  Map<String, String[]> parameters) {

		Double h = parameters.get(getPrefix() + "h") != null ? Double.parseDouble(parameters.get(getPrefix()
				+ "h")[0]) : 0.0;
		Double s = parameters.get(getPrefix() + "s") != null ? Double.parseDouble(parameters.get(getPrefix()
				+ "s")[0]) : 0.0;
		Double b = parameters.get(getPrefix() + "b") != null ? Double.parseDouble(parameters.get(getPrefix()
				+ "b")[0]) : 0.0;
		
		
		File resultFile = getResultsFile(file, parameters);
		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}


		HSBAdjustFilter filter = new HSBAdjustFilter();
		filter.setBFactor(b.floatValue());
		filter.setHFactor(h.floatValue());
		filter.setSFactor(s.floatValue());
		
		
		try {
			BufferedImage src = ImageIO.read(file);

			BufferedImage dst = filter.filter(src, null);
			ImageIO.write(dst, "png", resultFile);
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}

		return resultFile;
	}

}
