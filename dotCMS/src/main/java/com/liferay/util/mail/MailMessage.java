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

package com.liferay.util.mail;

import java.io.Serializable;

import javax.mail.internet.InternetAddress;

/**
 * <a href="MailMessage.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.5 $
 *
 */
public class MailMessage implements Serializable {

	public MailMessage(InternetAddress from, InternetAddress to,
					   String subject, String body) {

		this(from, new InternetAddress[] {to},  null, null, subject, body,
			 false);
	}

	public MailMessage(InternetAddress from, InternetAddress[] to,
					   InternetAddress[] cc, InternetAddress[] bcc,
					   String subject, String body) {

		this(from, to, cc, bcc, subject, body, false);
	}

	public MailMessage(InternetAddress from, InternetAddress[] to,
					   InternetAddress[] cc, InternetAddress[] bcc,
					   String subject, String body, boolean htmlFormat) {

		_from = from;
		_to = to;
		_cc = cc;
		_bcc = bcc;
		_subject = subject;
		_body = body;
		_htmlFormat = htmlFormat;
	}

	public InternetAddress getFrom() {
		return _from;
	}

	public void setFrom(InternetAddress from) {
		_from = from;
	}

	public InternetAddress[] getTo() {
		return _to;
	}

	public void setTo(InternetAddress[] to) {
		_to = to;
	}

	public InternetAddress[] getCC() {
		return _cc;
	}

	public void setCC(InternetAddress[] cc) {
		_cc = cc;
	}

	public InternetAddress[] getBCC() {
		return _bcc;
	}

	public void setBCC(InternetAddress[] bcc) {
		_bcc = bcc;
	}

	public String getSubject() {
		return _subject;
	}

	public void setSubject(String subject) {
		_subject = subject;
	}

	public String getBody() {
		return _body;
	}

	public void setBody(String body) {
		_body = body;
	}

	public boolean getHTMLFormat() {
		return _htmlFormat;
	}

	public boolean isHTMLFormat() {
		return _htmlFormat;
	}

	public void setHTMLFormat(boolean htmlFormat) {
		_htmlFormat = htmlFormat;
	}

	private InternetAddress _from;
	private InternetAddress[] _to;
	private InternetAddress[] _cc;
	private InternetAddress[] _bcc;
	private String _subject;
	private String _body;
	private boolean _htmlFormat;

}