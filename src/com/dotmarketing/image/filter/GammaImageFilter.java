package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;
import com.dotmarketing.jhlabs.image.GammaFilter;

public class GammaImageFilter extends ImageFilter {
	public String[] getAcceptedParameters() {
		return new String[] { "g (double)  between 0 and 3.0" };
	}

	public File runFilter(File file,   Map<String, String[]> parameters) {
		double g = parameters.get(getPrefix() + "g") != null ? Double.parseDouble(parameters.get(getPrefix() + "g")[0])
				: 0.0;
		float f = new Double(g).floatValue();
		
		
		File resultFile = getResultsFile(file, parameters);
		

		GammaFilter filter = new GammaFilter();
		filter.setGamma(f);

		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}

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
