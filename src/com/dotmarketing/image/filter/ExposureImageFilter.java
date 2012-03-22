package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;
import com.dotmarketing.jhlabs.image.ExposureFilter;

public class ExposureImageFilter extends ImageFilter {

	public String[] getAcceptedParameters() {
		return new String[] { "expx (double)  between 0 and 5.0" };
	}

	public File runFilter(File file,  Map<String, String[]> parameters) {

		double exp = parameters.get(getPrefix() + "exp") != null ? Double.parseDouble(parameters.get(getPrefix()
				+ "exp")[0]) : 0.0;
		float f = new Double(exp).floatValue();

		
		
		File resultFile = getResultsFile(file, parameters);



		ExposureFilter ef = new ExposureFilter();
		ef.setExposure(f);

		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}

		try {
			BufferedImage src = ImageIO.read(file);

			BufferedImage dst = ef.filter(src, null);
			ImageIO.write(dst, "png", resultFile);
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}

		return resultFile;
	}

}
