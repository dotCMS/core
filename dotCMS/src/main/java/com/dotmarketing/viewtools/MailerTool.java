package com.dotmarketing.viewtools;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.Logger;
import com.liferay.util.JNDIUtil;

/**
 * Simple Email Sender ViewTool for DotCMS
 * 
 * @author Christopher Falzone <cfalzone@edinboro.edu>
 * @author Jason Tesser
 * @version 1.9.1
 */
public class MailerTool implements ViewTool {

	/**
	 * Sends an email
	 * 
	 * Example:  #set($error = $mailer.sendMail(
	 *                   'them@theirdomain.com',
	 *                   'you@yourdomain.com',
	 *                   'The Subject',
	 *                   'The Message',
	 *                   false))
	 *           #if($UtilMethods.isSet($error))
	 *             ## Custom Error Handling 
	 *           #else
	 *             <p> Your message was sent </p>
	 *           #end
	 * 
	 * @param to		email address to send to
	 * @param from		email address to send from
	 * @param subject	subject of the email
	 * @param message	the message to send
	 * @param html		Whether or not to send it in HTML 
	 * @return			Empty if Successful, or the error message otherwise
	 */
	public String sendEmail(String to, String from, String subject,
			String message, Boolean html) {
		Session s = null;
		try {
			Context ctx = (Context) new InitialContext();
			s = (javax.mail.Session) JNDIUtil.lookup(ctx, "mail/MailSession");
		} catch (NamingException e1) {
			Logger.error(this,e1.getMessage(),e1);
		}

		if(s ==null){
			Logger.debug(this, "No Mail Session Available.");
			return "";
		}
		String smtpServer = s.getProperty("mail.smtp.host");

		/* Attach to the session */
		Properties props = System.getProperties();
		props.put("mail.host", smtpServer);
		props.setProperty("mail.transport.protocol", "smtp");
		Session session = Session.getDefaultInstance(props, null);

		/* Create a new message */
		Message msg = new MimeMessage(session);

		/* Set To and From Address */
		try {
			msg.setFrom(new InternetAddress(from));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(
					to, false));
		} catch (Exception e) {
			Logger.error(this, "Error Assigning To and From Addresses", e);
			return "Invalid To and/or From Address: " + e.getMessage();
		}
		try {
			msg.setSubject(subject);
		} catch (Exception e) {
			Logger.error(this, "Error Assigning Subject", e);
			return "Invalid Subject: " + e.getMessage();
		}

		/* See if using HTML or not */
		if (html) {
			/* Set the HTML Message */
			try {
				msg.setContent(message, "text/html");
			} catch (Exception e) {
				Logger.error(this, "Error Setting Content", e);
				return "Invalid Message: " + e.getMessage();
			}
		} else {
			/* Set the TEXT message */
			try {
				msg.setText(message);
			} catch (Exception e) {
				Logger.error(this, "Error Assigning Text", e);
				return "Invalid Message: " + e.getMessage();
			}
		}

		/* Send the email */
		try {
			/* Some Headers */
			msg.setHeader("X-Mailer", "DotCMSSimpleMailer");
			msg.setSentDate(new Date());

			/* Open the transport and send the message */
			Transport transport = session.getTransport();
			transport.connect();
			transport.sendMessage(msg, msg
					.getRecipients(Message.RecipientType.TO));
			transport.close();
		} catch (Exception e) {
			Logger.error(this, "Error Sending Message", e);
			return "Unable to Send Message: " + e.getMessage();
		}

		/*
		 * At this point if there are no exceptions 
		 * I am assuming the email was sent
		 */
		return "";
	}

	/**
	 * Init Method for the viewtool
	 */
	public void init(Object obj) {

	}
}
