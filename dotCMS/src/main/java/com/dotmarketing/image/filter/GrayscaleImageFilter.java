package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotcms.repackage.com.dotmarketing.jhlabs.image.GrayscaleFilter;

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
			BufferedImage src = StrictImageReader.read(file);

			BufferedImage dst = filter.filter(src, null);
			final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() + ".tmp");
			ImageIO.write(dst, "png", tempResultFile);
			dst.flush();
			StrictImageReader.assertDecodable(tempResultFile);
			if (!tempResultFile.renameTo(resultFile)) {
				tempResultFile.delete();
				throw new DotRuntimeException("unable to create file:" + resultFile);
			}
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
        }

		return resultFile;
	}

}
