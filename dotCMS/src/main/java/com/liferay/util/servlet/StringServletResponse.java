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

package com.liferay.util.servlet;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * <a href="StringServletResponse.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.9 $
 *
 */
public class StringServletResponse extends HttpServletResponseWrapper {

	public StringServletResponse(HttpServletResponse res) {
		super(res);
	}

	public void setURLEncoder(URLEncoder urlEncoder) {
		_urlEncoder = urlEncoder;
	}

	public String encodeURL(String path) {
		if (_urlEncoder != null) {
			return _urlEncoder.encodeURL(path);
		}
		else {
			return super.encodeURL(path);
		}
	}

	public void setContentType(String content) {
	}

	public void setLocale(Locale locale) {
	}

	public ServletOutputStream getOutputStream() {
		/*if (_callGetWriter) {
			throw new IllegalStateException();
		}*/

		_callGetOutputStream = true;

		return _sos;
	}

	public int getStatus() {
		return _status;
	}

	public void setStatus(int status) {
		_status = status;
	}

	public String getString() throws UnsupportedEncodingException {
		if (_callGetOutputStream) {
			return _baos.toString();
		}
		else if (_callGetWriter) {
			return _sw.toString();
		}
		else {
			return "";
		}
	}

	public PrintWriter getWriter() {
		/*if (_callGetOutputStream) {
			throw new IllegalStateException();
		}*/

		_callGetWriter = true;

		return _pw;
	}

	private URLEncoder _urlEncoder;
	private ByteArrayOutputStream _baos = new ByteArrayOutputStream();
	private ServletOutputStream _sos = new StringServletOutputStream(_baos);
	private int _status = 200;
	private StringWriter _sw = new StringWriter();
	private PrintWriter _pw = new PrintWriter(_sw);
	private boolean _callGetOutputStream;
	private boolean _callGetWriter;

}