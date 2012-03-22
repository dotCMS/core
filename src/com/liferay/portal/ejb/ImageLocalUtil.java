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

package com.liferay.portal.ejb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;

import EDU.oswego.cs.dl.util.concurrent.SyncMap;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

import com.dotmarketing.util.Logger;
import com.liferay.portal.model.Image;
import com.liferay.portal.util.ImageKey;
import com.liferay.portal.util.PropsUtil;

/**
 * <a href="ImageLocalUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @author  Michael Weisser
 * @version $Revision: 1.13 $
 *
 */
public class ImageLocalUtil {

	public static Image get(String id) {
		ImageLocalUtil imageLocalUtil = _getInstance();

		Image image = imageLocalUtil._get(id);

		if (image == null) {
			try {
				image = ImageManagerUtil.getImage(id);
			}
			catch (Exception e) {
				Logger.error(ImageLocalUtil.class,e.getMessage(),e);
			}

			imageLocalUtil._put(id, image);
		}

		return image;
	}

	public static byte[] getDefaultBytes() {
		return _getInstance()._getDefaultBytes();
	}

	public static void put(String id, byte[] bytes) {
		ImageLocalUtil imageLocalUtil = _getInstance();

		ImageKey.generateNewKey(id);

		Image image = null;

		try {
			image = ImageManagerUtil.updateImage(id, bytes);
		}
		catch (Exception e) {
			Logger.error(ImageLocalUtil.class,e.getMessage(),e);
		}

		imageLocalUtil._put(id, image);
	}

	public static Image remove(String id) {
		ImageLocalUtil imageLocalUtil = _getInstance();

		try {
			ImageManagerUtil.deleteImage(id);
		}
		catch (Exception e) {
		}

		return imageLocalUtil._remove(id);
	}

	private static ImageLocalUtil _getInstance() {
		if (_instance == null) {
			synchronized (ImageLocalUtil.class) {
				if (_instance == null) {
					_instance = new ImageLocalUtil();
				}
			}
		}

		return _instance;
	}

	private ImageLocalUtil() {
		_imagePool = new SyncMap(
			new LRUMap(Image.MAX_SIZE), new WriterPreferenceReadWriteLock());
	}

	private Image _get(String id) {
		return (Image)_imagePool.get(id);
	}

	private byte[] _getDefaultBytes() {
		byte[] defaultBytes = (byte[])_imagePool.get(PropsUtil.IMAGE_DEFAULT);

		if (defaultBytes == null) {
			try {
				InputStream is =
					getClass().getClassLoader().getResourceAsStream(
						PropsUtil.get(PropsUtil.IMAGE_DEFAULT));

				if (is != null) {
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					byte[] bytes = new byte[512];

					for (int i = is.read(bytes, 0, 512); i != -1;
							i = is.read(bytes, 0, 512)) {

						buffer.write(bytes, 0, i);
					}

					defaultBytes = buffer.toByteArray();

					is.close();
					buffer.close();

					_imagePool.put(PropsUtil.IMAGE_DEFAULT, defaultBytes);
				}
			}
			catch (IOException ioe) {
				Logger.error(ImageLocalUtil.class,ioe.getMessage(),ioe);
			}
		}

		return defaultBytes;
	}

	private void _put(String id, Image image) {
		_imagePool.put(id, image);
	}

	private Image _remove(String id) {
		return (Image)_imagePool.remove(id);
	}

	private static ImageLocalUtil _instance;

	private Map _imagePool;

}