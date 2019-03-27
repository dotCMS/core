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

package com.liferay.util.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.liferay.util.lang.FastStringBuffer;

/**
 * <a href="ZipCollection.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class ZipCollection implements Serializable {

	public ZipCollection() {
		_baos = new ByteArrayOutputStream();
		_zos = new ZipOutputStream(new BufferedOutputStream(_baos));
	}

	public void addEntry(String name, FastStringBuffer sb) throws IOException {
		addEntry(name, sb.toString());
	}

	public void addEntry(String name, StringBuffer sb) throws IOException {
		addEntry(name, sb.toString());
	}

	public void addEntry(String name, String s) throws IOException {
		addEntry(name, s.getBytes());
	}

	public void addEntry(String name, byte[] byteArray) throws IOException {
		ZipEntry entry = new ZipEntry(name);

		_zos.putNextEntry(entry);

		BufferedInputStream bis = new BufferedInputStream(
			new ByteArrayInputStream(byteArray), _BUFFER);

		int count;

		while ((count = bis.read(_data, 0, _BUFFER)) != -1) {
			_zos.write(_data, 0, count);
		}

		bis.close();
	}

	public byte[] finish() throws IOException {
		_zos.close();

		return _baos.toByteArray();
	}

	private static final int _BUFFER = 2048;

	private ByteArrayOutputStream _baos;
	private ZipOutputStream _zos;
	private byte[] _data = new byte[_BUFFER];

}