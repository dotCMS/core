package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.dotmarketing.util.Logger;
import com.dotmarketing.jhlabs.image.RotateFilter;

public class RotateImageFilter extends ImageFilter {
	public String[] getAcceptedParameters() {
		return new String[] { "a for angle (double) 0.00-359.99 degrees to rotate",

		};
	}

	public File runFilter(File file, Map<String, String[]> parameters) {
		double a = parameters.get(getPrefix() + "a") != null ? Double.parseDouble(parameters.get(getPrefix() + "a")[0])
				: 0.0;
		a = a*-1;
		File resultFile = getResultsFile(file, parameters);

		if (!overwrite(resultFile, parameters)) {
			return resultFile;
		}

		float x = new Double(java.lang.Math.toRadians(a)).floatValue();
		RotateFilter filter = new RotateFilter(x, true);
		filter.setEdgeAction(RotateFilter.ZERO);

		try {
			BufferedImage src = ImageIO.read(file);
			// int w = java.lang.Math.c

			BufferedImage testpass = filter.filter(src, null);

			BufferedImage dst = new BufferedImage(testpass.getWidth(), testpass.getHeight(),
					BufferedImage.TYPE_INT_ARGB);
			dst = filter.filter(src, dst);

			/*
			 * byte alpha =(byte)0; alpha %= 0xff; for (int cx=0;cx<dst.getWidth();cx++) { for (int
			 * cy=0;cy<dst.getHeight();cy++) { int color = dst.getRGB(cx, cy);
			 * 
			 * int mc = (alpha << 24) | 0x00ffffff; int newcolor = color & mc; finalas.setRGB(cx, cy, newcolor);
			 * 
			 * }
			 * 
			 * }
			 */
			ImageIO.write(dst, "png", resultFile);
		} catch (IOException e) {
			Logger.error(this.getClass(), e.getMessage());
		}

		return resultFile;
	}

}
