/*
 *  UtilMethods.java
 *
 *  Created on March 4, 2002, 2:56 PM
 */
package com.dotmarketing.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMultipart;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.sun.mail.pop3.POP3SSLStore;

/**
 * @author will
 * @created April 23, 2002
 */
/**
 * @author Carlos Rivas
 *
 */
public class EmailUtils {
   
	@SuppressWarnings({ "unchecked"})

	public static  void  SendContentSubmitEmail (Map <String, String> map, Structure structure,
			List<String> emails) {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.putAll(map);

		List<Field> fields = structure.getFields();
		List paramlist = new ArrayList(map.keySet());
		HashMap<String, String> paramtomail = new HashMap<String, String>();
		StringBuffer Body = new StringBuffer();
		// checks if the parameters belongs to the structure
		int numFields= fields.size();
		int fieldsAdded=0;
		int fieldsIndex=1;
		String keysOrdered="";
		for (Field field : fields) {
			for (Object param : paramlist){		
				String fieldName = field.getFieldName().contains(",")?field.getFieldName().replaceAll(",", "&#44;"):field.getFieldName();
				if ((field.getVelocityVarName().equals(param.toString()) || field.getFieldName().equals(param.toString())) && !APILocator.getFieldAPI().isElementConstant(field) && !field.getFieldType().equals(Field.FieldType.BINARY.toString()) && !field.getFieldType().equals(Field.FieldType.FILE.toString()) && !field.getFieldType().equals(Field.FieldType.IMAGE.toString())) {
					paramtomail.put(fieldName, map.get(param.toString()).toString());
					if(keysOrdered.equals("")){
						keysOrdered += fieldName;
					 }
					else{
						keysOrdered += ","+fieldName;
					}
					parameters.remove(field.getVelocityVarName());
				}
				if ((field.getVelocityVarName().equals(param.toString()) 
						|| field.getFieldName().equals(param.toString())) 
						&&(field.getFieldType().equals(Field.FieldType.BINARY.toString())								
								|| field.getFieldType().equals(Field.FieldType.IMAGE.toString())
								|| field.getFieldType().equals(Field.FieldType.FILE.toString()))) {//DOTCMS-5381
					parameters.remove(field.getVelocityVarName());
				}
			}
		}
		
       String [] keyset =keysOrdered.split(",");
		
       

		Body.append("<table border='0' cellpadding=3 cellspacing=1 bgcolor='#eeeeee'>");
		Body.append("<tr><td><b>New " + structure.getName() +"</b></td><td align='right'>" +new Date().toString() +"</td></tr>");
		for (String key : keyset) {
			String myVal = paramtomail.get(key);
			if(!UtilMethods.isSet(key)){
				continue;
			}
			Body.append("<tr bgcolor='#ffffff'>");
				Body.append("<td>");
					Body.append(key);
				Body.append("</td>");
				Body.append("<td>");
					Body.append(myVal);
				Body.append("</td>");
			Body.append("</tr>");

		}
		if(parameters.size() > 0){
			

			Body.append("<tr><td colspan=2><b>Other Information</td></tr>");
			for (Map.Entry<String, String> entry : parameters.entrySet()){
				Body.append("<tr bgcolor='#ffffff'>");
					Body.append("<td>");
						Body.append(entry.getKey());
					Body.append("</td>");
					Body.append("<td>");
						Body.append(entry.getValue());
					Body.append("</td>");
				Body.append("</tr>");
			}
		}
		
		
		Body.append("</table>");
		for (String email : emails) {

			Mailer m = new Mailer();
			m.setFromEmail("Website");
			m.setFromEmail(CompanyUtils.getDefaultCompany().getEmailAddress());
			m.setToEmail(email);
			m.setSubject("New submittal: " + structure.getName());
			m.setHTMLBody(Body.toString());
			m.setFromEmail(Config.getStringProperty("EMAIL_SYSTEM_ADDRESS"));
			m.sendMessage();
		}

	}
	
	// DOTCMS-6298
	public static List<Map<String,Object>> getEmails(String host,int port,boolean isSSL,String username,String password) throws MessagingException, IOException{
	    
		List<Map<String,Object>> emails = new ArrayList<Map<String,Object>>();
		
        String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        
        //If set to true, failure to create a socket using the specified socket factory class will cause the socket to be created using the java.net.Socket class.
        String fallback = "false";
        if(!isSSL)
        	fallback = "true";

        String portNumber = Integer.toString(port);
        Properties pop3Props = new Properties();

        pop3Props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
        pop3Props.setProperty("mail.pop3.socketFactory.fallback", fallback);
        pop3Props.setProperty("mail.pop3.port",  portNumber);
        pop3Props.setProperty("mail.pop3.socketFactory.port", portNumber);

        URLName url = new URLName("pop3", host, port, "",username, password);

        Session session = Session.getInstance(pop3Props, null);
        Store store = new POP3SSLStore(session, url);
        store.connect();

        Folder folder = store.getFolder("INBOX");

        folder.open(Folder.READ_WRITE);

        Message[] msgs = folder.getMessages();

        if(msgs.length < 1){
        	return emails;
        }

        // Use a suitable FetchProfile
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.CONTENT_INFO);
        folder.fetch(msgs, fp);

        for (int i = 0; i < msgs.length; i++) {
        	Message msg = msgs[i];
        	Map<String, Object> email = new HashMap<String, Object>();
            getEmailContents(msg,email);

            // FROM
            if ((msg.getFrom()) != null) {
            	email.put("From", getDataBetweenAngBrkts(msg.getFrom()[0].toString()));
            }

            // TO
            if ((msg.getRecipients(Message.RecipientType.TO)) != null) {
            	String to = "";
            	for (int j = 0; j < msg.getRecipients(RecipientType.TO).length; j++) {
            		to += getDataBetweenAngBrkts(msg.getRecipients(RecipientType.TO)[j].toString())+",";
				}
            	email.put("To", to.substring(0, to.length()-1));
            }

            // SUBJECT
            email.put("Subject", getCleanSubject(msg.getSubject()));

            // DATE
            Date d = msg.getSentDate();
            if(d != null){
            	SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    			String d2 = sdf2.format(d);
    			email.put("Date", d2.toString());
            }

            // CC
            if(msg.getRecipients(RecipientType.CC) != null){
            	String cc = "";
            	for (int j = 0; j < msg.getRecipients(RecipientType.CC).length; j++) {
					cc += getDataBetweenAngBrkts(msg.getRecipients(RecipientType.CC)[j].toString())+",";
				}
            	email.put("Cc", cc.substring(0, cc.length()-1));
            }
            
            // BCC
            if(msg.getRecipients(RecipientType.BCC) != null){
            	String bcc = "";
            	for (int j = 0; j < msg.getRecipients(RecipientType.BCC).length; j++) {
					bcc += getDataBetweenAngBrkts(msg.getRecipients(RecipientType.BCC)[j].toString())+",";
				}
            	email.put("Bcc", bcc.substring(0, bcc.length()-1));
            }

            // MESSAGE-ID
            //email.put("Message-ID",msg.getHeader("Message-ID")[0]);
            
            @SuppressWarnings("unchecked")
			Enumeration<Header> headers = msg.getAllHeaders();
            while(headers.hasMoreElements()){
            	Header header = headers.nextElement();
            	
            	if(header.getName().equals("Message-ID"))
            			email.put("Message-ID", getDataBetweenAngBrkts(header.getValue()));
            	
            	if(header.getName().equals("In-Reply-To"))
        			email.put("In-Reply-To", getDataBetweenAngBrkts(header.getValue()));
            	
            	if(header.getName().equals("References"))
        			email.put("References", header.getValue());            	
            }

            emails.add(email);
            msg.setFlag(Flag.DELETED, true);
        }

        folder.close(true);

        store.close();

        return emails;
	}
	
	private static String getDataBetweenAngBrkts(String originalString) {		
		char chr;
		StringBuffer strBfr = new StringBuffer();
		for (int i = 0; i < originalString.length(); i++) {
			chr = originalString.charAt(i);
			if(chr == '<'){
				strBfr.delete(0, strBfr.length());
				continue;
			}
			if(chr == '>'){
				return strBfr.toString();				
			}
			strBfr.append(chr);
		}
		return "";
	}


	private static void getEmailContents(Message message, Map<String, Object> email) throws MessagingException, IOException {
		
		int totalAttachments = 0;

		String msgBodyTxt = new String("");

		String contentType = message.getContentType();

		String messageId = message.getHeader("Message-ID")[0];

		String alphanumericMsgId = messageId.replaceAll("[^a-zA-Z0-9]", "");

		File tempEmailDir = new File(Config.CONTEXT.getRealPath(Constants.TEMP_BINARY_PATH) + File.separator + "email" );

		File messageFolder = new File(tempEmailDir.getPath() + File.separator + alphanumericMsgId);
		if(!messageFolder.exists())
			messageFolder.mkdirs();

		if(!contentType.contains("multipart")){//PLAIN/TEXT WITHOUT ATTACHEMENTS

			msgBodyTxt = (String)message.getContent();

		}else{

			MimeMultipart msgContent = (MimeMultipart) message.getContent();
			int msgContentsCount = msgContent.getCount();

			BodyPart msgContentPart;
			String disposition;	//	NULL implies message body, "attachment" implies attachment

			for (int i = 0; i < msgContentsCount; i++) {

				msgContentPart = msgContent.getBodyPart(i);

				disposition = msgContentPart.getDisposition();

				if(disposition == null){	// MESSAGE BODY

					if(msgContentPart.getContentType().contains("multipart")){	// RICH TEXT FORMATTING

						MimeMultipart multiPartMsgBody = (MimeMultipart) msgContentPart.getContent();
						int mulPartMsgBodyCount = multiPartMsgBody.getCount();

						for (int j = 0; j < mulPartMsgBodyCount; j++) {

							BodyPart mulPartContentBodyPart = multiPartMsgBody.getBodyPart(j);

							if(mulPartContentBodyPart.getContentType().contains("multipart")){//RTF WITH EMBEDDED IMAGES/MULTIPART

								MimeMultipart msgBodyRichText = (MimeMultipart) msgContentPart.getContent();

								for (int k = 0; k < msgBodyRichText.getCount(); k++) {

									BodyPart msgBodyRichTextPart = msgBodyRichText.getBodyPart(k);

									if(msgBodyRichTextPart.getContentType().contains("multipart")){
										MimeMultipart actualRTF = (MimeMultipart) msgBodyRichTextPart.getContent();

										for (int m = 0; m < actualRTF.getCount(); m++) {

											BodyPart actualTextPart = actualRTF.getBodyPart(m);

											//rtf PLAIN text
											if(actualTextPart.getContentType().contains("plain")){
												msgBodyTxt = (String)actualTextPart.getContent();
											}

											//RTF html text
											if(actualTextPart.getContentType().contains("html")){
												msgBodyTxt = (String)actualTextPart.getContent();
											}
										}
									}else{
										// RIGHT NOW NOT HANDLING THE EMBEDDED IMAGES OR EMOTIONS INSIDE THE RTF BODY.
									}
								}
							}else{
								//rtf PLAIN text
								if(mulPartContentBodyPart.getContentType().contains("plain")){
									msgBodyTxt = (String)mulPartContentBodyPart.getContent();
								}

								//RTF html text
								if(mulPartContentBodyPart.getContentType().contains("html")){
									msgBodyTxt = (String)mulPartContentBodyPart.getContent();
								}
							}
						}
					}else{
						//plain text with NO attachments
						if(msgContentPart.getContentType().contains("plain")){
							msgBodyTxt = (String)msgContentPart.getContent();
						}
						//html text with NO attachments
						if(msgContentPart.getContentType().contains("html")){
							msgBodyTxt = (String)msgContentPart.getContent();
						}
					}
				}else{	// ATTACHMENTS
					File attachmentDir = new File(messageFolder.getPath() + File.separator + "attachment" + i);
					if(!attachmentDir.exists())
						attachmentDir.mkdirs();

					DataHandler attachmentDataHandler = msgContentPart.getDataHandler();

					File attachment = new File(attachmentDir.getPath() + File.separator + attachmentDataHandler.getName());
					if(!attachment.exists())
						attachment.createNewFile();

					FileOutputStream os = new FileOutputStream(attachment);
					attachmentDataHandler.writeTo(os);
					os.close();

					totalAttachments++;
					email.put("attachment"+totalAttachments, attachment);
				}
			}
		}
		email.put("totalAttachments", totalAttachments);
		email.put("Body", msgBodyTxt);
	}

	public static String getCleanSubject(String subject){
		subject = subject.replace("RE:", "");
		subject = subject.replace("Re:", "");
		subject = subject.replace("Fw:", "");
		subject = subject.replace("FW:", "");
		subject = subject.replace("Fwd:", "");
		while(subject.contains("  ")){
			subject = subject.replace("  ", " ");
		}
		return subject;
	}
}