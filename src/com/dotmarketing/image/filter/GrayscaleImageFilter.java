package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;
import com.dotmarketing.jhlabs.image.GrayscaleFilter;

public class GrayscaleImageFilter extends ImageFilter {

	public String[] getAcceptedParameters() {
		return new String[] { "none" };
	}

	public File runFilter(File file,  Map<String, String[]> parameters) {

		File resultFile = getResultsFile(file, parameters);
		GrayscaleFilter filter = new GrayscaleFilter();

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
