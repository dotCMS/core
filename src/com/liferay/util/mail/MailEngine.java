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

import java.io.ByteArrayInputStream;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dotmarketing.util.Logger;
import com.liferay.util.GetterUtil;
import com.liferay.util.JNDIUtil;
import com.liferay.util.Validator;

/**
 * <a href="MailEngine.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class MailEngine {

	public static final String MAIL_SESSION = "java:comp/env/mail/MailSession";

	public static Session getSession() throws NamingException {
		return (Session)JNDIUtil.lookup(new InitialContext(), MAIL_SESSION);
	}

	public static void send(MailMessage mailMessage)
		throws MailEngineException {

		send(mailMessage.getFrom(), mailMessage.getTo(), mailMessage.getCC(),
			 mailMessage.getBCC(), mailMessage.getSubject(),
			 mailMessage.getBody(), mailMessage.isHTMLFormat());
	}

	public static void send(String from, String to, String subject, String body)
		throws MailEngineException {

		try {
			send(new InternetAddress(from), new InternetAddress(to), subject,
				 body);
		}
		catch (AddressException ae) {
			throw new MailEngineException(ae);
		}
	}

	public static void send(InternetAddress from, InternetAddress to,
							String subject, String body)
		throws MailEngineException {

		send(from, new InternetAddress[] {to}, null, null, subject, body,
			 false);
	}

	public static void send(InternetAddress from, InternetAddress to,
							String subject, String body, boolean htmlFormat)
		throws MailEngineException {

		send(from, new InternetAddress[] {to}, null, null, subject, body,
			 htmlFormat);
	}

	public static void send(InternetAddress from, InternetAddress[] to,
							String subject, String body)
		throws MailEngineException {

		send(from, to, null, null, subject, body, false);
	}

	public static void send(InternetAddress from, InternetAddress[] to,
							String subject, String body, boolean htmlFormat)
		throws MailEngineException {

		send(from, to, null, null, subject, body, htmlFormat);
	}

	public static void send(InternetAddress from, InternetAddress[] to,
							InternetAddress[] cc,
							String subject, String body)
		throws MailEngineException {

		send(from, to, cc, null, subject, body, false);
	}

	public static void send(InternetAddress from, InternetAddress[] to,
							InternetAddress[] cc,
							String subject, String body, boolean htmlFormat)
		throws MailEngineException {

		send(from, to, cc, null, subject, body, htmlFormat);
	}

	public static void send(InternetAddress from, InternetAddress[] to,
							InternetAddress[] cc, InternetAddress[] bcc,
							String subject, String body)
		throws MailEngineException {

		send(from, to, cc, bcc, subject, body, false);
	}

	public static void send(InternetAddress from, InternetAddress[] to,
							InternetAddress[] cc, InternetAddress[] bcc,
							String subject, String body, boolean htmlFormat)
		throws MailEngineException {

		long start = System.currentTimeMillis();

		try {
			Session session = getSession();

			Message msg = new MimeMessage(session);

			msg.setFrom(from);
			msg.setRecipients(Message.RecipientType.TO, to);

			if (cc != null) {
				msg.setRecipients(Message.RecipientType.CC, cc);
			}

			if (bcc != null) {
				msg.setRecipients(Message.RecipientType.BCC, bcc);
			}

			msg.setSubject(subject);

			/*BodyPart bodyPart = new MimeBodyPart();

			if (htmlFormat) {
				bodyPart.setContent(body, "text/html");
			}
			else {
				bodyPart.setText(body);
			}

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(bodyPart);

			msg.setContent(multipart);*/

			if (htmlFormat) {
				msg.setContent(body, _TEXT_HTML);
			}
			else {
				msg.setContent(body, _TEXT_PLAIN);
			}

			_sendMessage(session, msg);
		}
		catch (SendFailedException sfe) {
			_log.error("From: " + from);
			_log.error("To: " + to);
			_log.error("Subject: " + subject);
			_log.error("Body: " + body);

			Logger.error(MailEngine.class, sfe.getMessage(),sfe);
		}
		catch (Exception e) {
			throw new MailEngineException(e);
		}

		long end = System.currentTimeMillis();

		_log.debug("Sending mail takes " + (end - start) + " seconds");
	}

	public static void send(byte[] msgByteArray) throws MailEngineException {
		InternetAddress[] from = null;

		try {
			Session session = getSession();

			Message msg = new MimeMessage(
				session, new ByteArrayInputStream(msgByteArray));

			from = (InternetAddress[])msg.getFrom();

			_sendMessage(session, msg);
		}
		catch (SendFailedException sfe) {
		    Logger.error(MailEngine.class, sfe.getMessage(), sfe);
		}
		catch (Exception e) {
			throw new MailEngineException(e);
		}
	}

	private static void _sendMessage(Session session, Message msg)
		throws MessagingException {

		boolean smtpAuth = GetterUtil.getBoolean(
			session.getProperty("mail.smtp.auth"), false);
		String smtpHost = session.getProperty("mail.smtp.host");
		String user = session.getProperty("mail.smtp.user");
		String password = session.getProperty("mail.smtp.password");

		if (smtpAuth && Validator.isNotNull(user) &&
			Validator.isNotNull(password)) {

			Transport tr = session.getTransport("smtp");
			tr.connect(smtpHost, user, password);
			tr.sendMessage(msg, msg.getAllRecipients());
			tr.close();
		}
		else {
			Transport.send(msg);
		}
	}

	private static final Log _log = LogFactory.getLog(MailEngine.class);

	private static final String _TEXT_HTML = "text/html;charset=\"UTF-8\"";

	private static final String _TEXT_PLAIN = "text/plain;charset=\"UTF-8\"";

}