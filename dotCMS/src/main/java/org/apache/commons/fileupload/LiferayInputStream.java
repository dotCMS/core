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

package org.apache.commons.fileupload;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liferay.util.servlet.ServletInputStreamWrapper;

/**
 * <a href="LiferayInputStream.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Kim
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class LiferayInputStream extends ServletInputStreamWrapper {

	public LiferayInputStream(HttpServletRequest req) throws IOException {
		super(req.getInputStream());

		_req = req;
		_ses = req.getSession();
		_totalSize = req.getContentLength();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int bytesRead = super.read(b, off, len);

		if (bytesRead > 0) {
			_totalRead += bytesRead;
		}
		else {
			_totalRead = _totalSize;
		}

		float percent = _totalRead / _totalSize;

		_log.debug(bytesRead + "/" + _totalRead + "=" + percent);

		_ses.setAttribute(LiferayDiskFileUpload.PERCENT, new Float(percent));

		return bytesRead;
	}

	private static final Log _log = LogFactory.getLog(LiferayInputStream.class);

	private HttpServletRequest _req;
	private HttpSession _ses;
	private float _totalRead;
	private int _totalSize;

}