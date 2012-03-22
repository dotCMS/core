package com.dotmarketing.image.filter;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import com.dotmarketing.util.ImageResizeUtils;
import com.dotmarketing.util.Logger;

public class ThumbnailImageFilter extends ImageFilter {
	public String[] getAcceptedParameters() {
		return new String[] { "w (int) specifies width", "h (int) specifies height",
				"bg (int) must be 9 digits of rgb (000000000=black, 255255255=white) for background color"

		};
	}

	public File runFilter(File file,  Map<String, String[]> parameters) {

		int height = parameters.get(getPrefix() + "h") != null ? Integer.parseInt(parameters.get(getPrefix() + "h")[0])
				: 0;
		int width = parameters.get(getPrefix() + "w") != null ? Integer.parseInt(parameters.get(getPrefix() + "w")[0])
				: 0;
		String rgb = parameters.get(getPrefix() + "bg") != null ? parameters.get(getPrefix() + "bg")[0] : "255255255";
		Color color = new Color(Integer.parseInt(rgb.substring(0, 3)), Integer.parseInt(rgb.substring(3, 6)),
				Integer.parseInt(rgb.substring(6)));

		File resultFile = getResultsFile(file, parameters);

		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}

		FileOutputStream fos = null;
		try {
			resultFile.delete();
			fos = new FileOutputStream(resultFile);
			ImageResizeUtils.generateThumbnail(new FileInputStream(file), fos, FILE_EXT, width, height, color);
		} catch (FileNotFoundException e) {
			Logger.error(this.getClass(), e.getMessage());
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		} catch (InterruptedException e) {
			Logger.error(this.getClass(), e.getMessage());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					Logger.error(this.getClass(), "should not be here");
				}
			}
		}

		return resultFile;

	}

}
