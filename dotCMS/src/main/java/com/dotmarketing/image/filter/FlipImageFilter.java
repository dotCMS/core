package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotcms.repackage.com.dotmarketing.jhlabs.image.ExposureFilter;
import com.dotcms.repackage.com.dotmarketing.jhlabs.image.FlipFilter;

public class FlipImageFilter extends ImageFilter {

	public String[] getAcceptedParameters() {
		return new String[] { "expx (double)  between 0 and 5.0" };
	}

	public File runFilter(File file,  Map<String, String[]> parameters) {

		boolean flip = false;
		try {
			flip = parameters.get(getPrefix() + "flip") != null ? true : false;
		} catch (Exception e) {
		}
	

		File resultFile = getResultsFile(file, parameters);

		FlipFilter filter = new FlipFilter();

		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}

		try {
			if (flip) {
				filter.setOperation(FlipFilter.FLIP_H);
			} 
            final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() +".tmp");

			
			BufferedImage src = StrictImageReader.read(file);
			BufferedImage dst = filter.filter(src, null);
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
