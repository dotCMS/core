/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import com.dotcms.repackage.net.jmge.gif.Gif89Encoder;

import com.dotcms.repackage.com.tjtieto.wap.wapix.WBMPMaster;

/**
 * <a href="ImageUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class ImageUtil {

	public static void encodeGIF(BufferedImage image, OutputStream out)
		throws IOException {

		Gif89Encoder encoder = new Gif89Encoder(image);

		encoder.encode(out);
	}

	public static void encodeWBMP(BufferedImage image, OutputStream out)
		throws InterruptedException, IOException {

		WBMPMaster wbmpMaster = new WBMPMaster();

		int height = image.getHeight();
		int width = image.getWidth();

		int[] pixels = wbmpMaster.grabPixels(image);
		pixels = WBMPMaster.processPixels(
			1, pixels, width, height, 128, Color.white, false);

		WBMPMaster.encodePixels(out, pixels, width, height);
	}

	public static BufferedImage scale(BufferedImage image, double factor) {
		AffineTransformOp op = new AffineTransformOp(
			AffineTransform.getScaleInstance(
				factor, factor), null);

		return op.filter(image, null);
	}

	public static BufferedImage scale(BufferedImage image, int pixels) {
		if ((image.getHeight() <= pixels) && (image.getWidth() <= pixels)) {
			return image;
		}

		double factor = 0.1;

		if (image.getHeight() > image.getWidth()) {
			factor = (double)pixels / image.getHeight();
		}
		else {
			factor = (double)pixels / image.getWidth();
		}

		return scale(image, factor);
	}

}