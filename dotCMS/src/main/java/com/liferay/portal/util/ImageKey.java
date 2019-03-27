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

package com.liferay.portal.util;

import java.util.Hashtable;
import java.util.Random;

/**
 * <a href="ImageKey.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class ImageKey {

	public static String generateNewKey(String id) {
		ImageKey imageKey = _getInstance();

		Random random = new Random();
		int i = random.nextInt(1000000);

		String key = Integer.toString(i);

		imageKey._put(id, key);

		return key;
	}

	public static String get(String id) {
		ImageKey imageKey = _getInstance();

		String key = imageKey._get(id);

		if (key == null) {
			key = generateNewKey(id);
		}

		return key;
	}

	private static ImageKey _getInstance() {
		if (_instance == null) {
			synchronized (ImageKey.class) {
				if (_instance == null) {
					_instance = new ImageKey();
				}
			}
		}

		return _instance;
	}

	private ImageKey() {
		_imageKey = new Hashtable();
	}

	private String _get(String id) {
		return (String)_imageKey.get(id);
	}

	private void _put(String id, String key) {
		_imageKey.put(id, key);
	}

	private static ImageKey _instance;

	private Hashtable _imageKey;

}