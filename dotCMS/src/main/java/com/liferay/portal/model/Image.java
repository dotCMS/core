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

package com.liferay.portal.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import com.liferay.util.Base64;

/**
 * <a href="Image.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.8 $
 *
 */
public class Image extends ImageModel {

	public Image() {
		super();
	}

	public Image(String imageId) {
		super(imageId);
	}

	public Image(String imageId, String text) {
		super(imageId, text);

		setText(text);
	}

	public void setText(String text) {
		_textObj = (byte[])Base64.stringToObject(text);

		_setType();

		super.setText(text);
	}

	public byte[] getTextObj() {
		return _textObj;
	}

	public void setTextObj(byte[] textObj) {
		_textObj = textObj;

		_setType();

		super.setText(Base64.objectToString(textObj));
	}

	public String getType() {
		return _type;
	}

	private void _setType() {
		MemoryCacheImageInputStream mcis =
			new MemoryCacheImageInputStream(new ByteArrayInputStream(_textObj));

		Iterator itr = ImageIO.getImageReaders(mcis);

		_type = null;
		String fm;
		if (itr.hasNext()) {
			ImageReader obj = (ImageReader)itr.next();
			try {
                fm=obj.getFormatName();
            } catch (IOException e) {return;}
			if (fm.equalsIgnoreCase("gif")) {
				_type = "gif";
			}
			else if (fm.equalsIgnoreCase("jpeg") || fm.equalsIgnoreCase("jpg")) {
				_type = "jpeg";
			}
			else if (fm.equalsIgnoreCase("png")) {
				_type = "png";
			}
		}
	}

	private byte[] _textObj;
	private String _type;

}