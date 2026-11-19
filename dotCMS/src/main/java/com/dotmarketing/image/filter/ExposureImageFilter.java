package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotcms.repackage.com.dotmarketing.jhlabs.image.ExposureFilter;

public class ExposureImageFilter extends ImageFilter {

	public String[] getAcceptedParameters() {
		return new String[] { "expx (double)  between 0 and 5.0" };
	}

	public File runFilter(File file,  Map<String, String[]> parameters) {

		double exp = parameters.get(getPrefix() + "exp") != null ? Double.parseDouble(parameters.get(getPrefix()
				+ "exp")[0]) : 0.0;
		float f = Double.valueOf(exp).floatValue();

		
		
		File resultFile = getResultsFile(file, parameters);



		ExposureFilter ef = new ExposureFilter();
		ef.setExposure(f);

		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}

		try {
			BufferedImage src = StrictImageReader.read(file);

			
            final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() +".tmp.jpg");

			
			BufferedImage dst = ef.filter(src, null);
			ImageIO.write(dst, "png", tempResultFile);
			dst.flush();
			
	        StrictImageReader.assertDecodable(tempResultFile);
	        tempResultFile.renameTo(resultFile);
		} catch (IOException e) {
			throw new DotRuntimeException("unable to convert file:" + file + " : " + e.getMessage(), e);
		}

		return resultFile;
	}

}
