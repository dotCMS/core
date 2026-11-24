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
			// a staging file unique to this call: concurrent renders of the same rendition never share one
			final File tempResultFile = java.nio.file.Files.createTempFile(
					resultFile.getAbsoluteFile().getParentFile().toPath(), resultFile.getName() + "_", ".tmp").toFile();
			try {
				ImageIO.write(dst, "png", tempResultFile);
				dst.flush();
				publishRendition(tempResultFile, resultFile);
			} finally {
				// already gone after a successful rename; removes a partial file on any failure
				java.nio.file.Files.deleteIfExists(tempResultFile.toPath());
			}
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
        }

		return resultFile;
	}

}
