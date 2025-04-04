package com.dotmarketing.util;
/*
 *  mail class using using javamail API.
 *  Sends an email and writes the email to a file
 */
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.dotmarketing.business.APILocator;

/**
 * 
 * Description of the Class
 * 
 * 
 * 
 * @author will
 * 
 * @created May 8, 2002
 */
public class Mailer {
	MimeMultipart sendingAttachments;
	MimeMultipart sendingText;
	String bcc;
	String cc;
	String errorMessage;
	String fromEmail;
	String fromName;
	String result;
	String subject;
	String toEmail;
	String toName;
	String recipientId;
	String encoding = "UTF-8";
	private Map<String, String> customHeaders = new HashMap<>();

	public Mailer() {
		result = null;
		sendingAttachments = new MimeMultipart();
		sendingText = new MimeMultipart();
		errorMessage = null;
	}
	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	public void setBcc(String x) {
		bcc = x;
	}
	/**
	 * 
	 * Returns the bcc.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getBcc() {
		return bcc;
	}
	public void setCc(String x) {
		cc = x;
	}
	/**
	 * 
	 * Returns the cc.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getCc() {
		return cc;
	}
	/**
	 * 
	 * Sets the errorMessage.
	 * 
	 * 
	 * 
	 * @param errorMessage
	 * 
	 *            The errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		if (errorMessage != null)
			this.errorMessage = errorMessage.trim();
	}
	/**
	 * 
	 * Returns the errorMessage.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	/**
	 * 
	 * Sets the fromEmail.
	 * 
	 * 
	 * 
	 * @param fromEmail
	 * 
	 *            The fromEmail to set
	 */
	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}
	/**
	 * 
	 * Returns the fromEmail.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getFromEmail() {
		return fromEmail;
	}
	public void setFromName(String x) {
		fromName = x;
	}
	/**
	 * 
	 * Returns the fromName.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getFromName() {
		return fromName;
	}
	public void setHTMLAndTextBody(String HTMLBody) {
		try {
			MimeBodyPart bp = new MimeBodyPart();
			String contentEncoding = "text/html;charset=" + encoding;
			bp.setContent(HTMLBody, contentEncoding);
			bp.setHeader("Content-Transfer-Encoding", "base64");
			sendingText.addBodyPart(bp);
			bp = new MimeBodyPart();
			String x = HTMLBody.replaceAll("<[^>]*>", " ");
			while (x.indexOf("  ") > -1) {
				x = x.replaceAll("  ", " ");
			}
			setTextBody(x);
		} catch (Exception f) {
			Logger.error(this, "failed to setHTMLAndTextBody:" + f, f);
		}
	}
	public void setHTMLBody(String HTMLBody) {
		try {
			MimeBodyPart bp = new MimeBodyPart();
			String contentEncoding = "text/html;charset=" + this.getEncoding();
			bp.setContent(HTMLBody, contentEncoding);
			sendingText.addBodyPart(bp);
		} catch (Exception f) {
			Logger.error(this, "failed to setHTMLBody:" + f, f);
		}
	}
	/**
	 * 
	 * Sets the recipientId.
	 * 
	 * 
	 * 
	 * @param recipientId
	 * 
	 *            The recipientId to set
	 */
	public void setRecipientId(String recipientId) {
		this.recipientId = recipientId;
	}
	/**
	 * 
	 * Returns the recipientId.
	 * 
	 * 
	 * 
	 * @return int
	 */
	public String getRecipientId() {
		return recipientId;
	}
	/**
	 * 
	 * Returns the result.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getResult() {
		return result;
	}
	public void setSubject(String x) {
		subject = x;
	}
	/**
	 * 
	 * Returns the subject.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getSubject() {
		return subject;
	}
	public void setTextBody(String TextBody) {
		try {
			MimeBodyPart bp = new MimeBodyPart();
			bp.setContent(TextBody, "text/plain");
			sendingText.addBodyPart(bp, 0);
		} catch (Exception f) {
			Logger.error(this, "failed to setTextBody:" + f, f);
		}
	}
	/**
	 * 
	 * Sets the toEmail.
	 * 
	 * 
	 * 
	 * @param toEmail
	 * 
	 *            The toEmail to set
	 */
	public void setToEmail(String toEmail) {
		this.toEmail = toEmail;
	}
	/**
	 * 
	 * Returns the toEmail.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getToEmail() {
		return toEmail;
	}
	public void setToName(String x) {
		toName = x;
	}
	/**
	 * 
	 * Returns the toName.
	 * 
	 * 
	 * 
	 * @return String
	 */
	public String getToName() {
		return toName;
	}
	public void addAttachment(java.io.File file) {
		if ((file != null) && file.exists() && !file.isDirectory()) {
			try {
				MimeBodyPart mbp = new MimeBodyPart();
				FileDataSource fds = new FileDataSource(file.getAbsolutePath());
				mbp.setDataHandler(new DataHandler(fds));
				mbp.setFileName(file.getName());
				sendingAttachments.addBodyPart(mbp);
			} catch (Exception f) {
				Logger.error(this, "failed to addAttachment:" + f, f);
			}
		}
	}
	public void addAttachment(java.io.File file, String filename) {
		if ((file != null) && file.exists() && !file.isDirectory()) {
			try {
				MimeBodyPart mbp = new MimeBodyPart();
				FileDataSource fds = new FileDataSource(file.getAbsolutePath());
				mbp.setDataHandler(new DataHandler(fds));
				mbp.setFileName(filename);
				sendingAttachments.addBodyPart(mbp);
			} catch (Exception f) {
				Logger.error(this, "failed to addAttachment:" + f, f);
			}
		}
	}
	public void addHeader(String headerName, String headerValue) {
		if (UtilMethods.isSet(headerName) && UtilMethods.isSet(headerValue)) {
			customHeaders.put(headerName, headerValue);
		}
	}
	/**
	 * 
	 * Description of the Method
	 */
	public boolean sendMessage() {
		MimeMultipart mp = new MimeMultipart();
		try {
			// if there is a text and an html section
			if (sendingText.getCount() > 1) {
				sendingText.setSubType("alternative");
			}
			// if we are sending attachments
			if (sendingAttachments.getCount() > 0) {
				MimeBodyPart bp = new MimeBodyPart();
				bp.setContent(sendingAttachments);
				mp.addBodyPart(bp, 0);
				bp = new MimeBodyPart();
				bp.setContent(sendingText);
				mp.addBodyPart(bp, 0);
			} else {
				mp = sendingText;
			}
			Logger.debug(this, "Getting the MailContext.");
			/*
			 * 
			 * Get the mail session from
			 * 
			 * the container Context
			 */
			Session session = APILocator.getMailApi().getMailSession();
			Logger.debug(this, "Delivering mail using: " + session.getProperty("mail.smtp.host") + " as server.");
			MimeMessage message = new MimeMessage(session);

			applyHeaders(message);

			if ((fromEmail != null) && (fromName != null) && (0 < fromEmail.trim().length())) {
				message.setFrom(new InternetAddress(fromEmail, fromName));
			} else if ((fromEmail != null) && (0 < fromEmail.trim().length())) {
				message.setFrom(new InternetAddress(fromEmail));
			}
			if (toName != null) {
				String[] recipients = toEmail.split("[;,]");
				for (String recipient : recipients) {
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient, toName));
				}
			} else {
				String[] recipients = toEmail.split("[;,]");
				for (String recipient : recipients) {
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
				}
			}
			if (UtilMethods.isSet(cc)) {
				String[] recipients = cc.split("[;,]");
				for (String recipient : recipients) {
					message.addRecipient(Message.RecipientType.CC, new InternetAddress(recipient));
				}
			}
			if (UtilMethods.isSet(bcc)) {
				String[] recipients = bcc.split("[;,]");
				for (String recipient : recipients) {
					message.addRecipient(Message.RecipientType.BCC, new InternetAddress(recipient));
				}
			}
			message.setSubject(subject, encoding);
			message.setContent(mp);

			final Transport transport = session.getTransport();
			transport.connect(APILocator.getMailApi().getConnectionHost(),
					APILocator.getMailApi().getConnectionPort(), null, null);
			transport.sendMessage(message, message.getAllRecipients());
			result = "Send Ok";
			return true;
		} catch (javax.mail.SendFailedException f) {
			String error = String.valueOf(f);
			errorMessage = error.substring(
					error.lastIndexOf("javax.mail.SendFailedException:") + "javax.mail.SendFailedException:".length(), (error
					.length() - 1));
			result = "Failed:" + error;
			Logger.error(Mailer.class, f.toString(), f);
			return false;
		} catch (MessagingException f) {
			String error = String.valueOf(f);
			errorMessage = error.substring(error.lastIndexOf("javax.mail.MessagingException:") + "javax.mail.MessagingException:".length(),
					(error
					.length() - 1));
			result = "Failed:" + error;
			Logger.error(Mailer.class, f.toString(), f);
			return false;
		} catch (UnsupportedEncodingException f) {
			String error = String.valueOf(f);
			errorMessage = error.substring(error.lastIndexOf("java.io.UnsupportedEncodingException:")
			+ "java.io.UnsupportedEncodingException:".length(), (error.length() - 1));
			result = "Failed:" + error;
			Logger.error(Mailer.class, f.toString(), f);
			return false;
		}
	}

	/**
	 * Applies email headers to the provided MimeMessage.
	 * <p>
	 * This method determines which headers to apply based on whether any custom headers
	 * have been set. If custom headers are present, they will be used exclusively.
	 * Otherwise, the system-wide default email headers (DEFAULT_EMAIL_HEADERS) will be applied.
	 * Additionally, it always adds the "X-RecipientId" header.
	 * </p>
	 *
	 * @param message the MimeMessage to which the headers should be added
	 * @throws MessagingException if an error occurs while adding headers to the message
	 */
	private void applyHeaders(MimeMessage message) throws MessagingException {
		Map<String, String> headersToApply;
		if (customHeaders.isEmpty()) {
			headersToApply = ConfigUtils.getDefaultEmailHeaders();
		} else {
			headersToApply = customHeaders;
		}
		// Always add the recipient ID header.
		message.addHeader("X-RecipientId", String.valueOf(getRecipientId()));
		// Add all headers.
		for (Map.Entry<String, String> entry : headersToApply.entrySet()) {
			message.addHeader(entry.getKey(), entry.getValue());
		}
	}
}
