package com.dotmarketing.factories;

//import com.dotmarketing.threads.DeliverNewsletterThread;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.webforms.model.WebForm;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FormSpamFilter;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * DOCUMENT ME!
 * 
 * @author $author$
 * @version $Revision: 1.10 $
 */
public class EmailFactory {

	private static Long emailTime = new Long(System.currentTimeMillis());

	/**
	 * Rewrites urls to point back to the redirection servlet to track links
	 * and then calls the alterBodyHtmlAbsolutizePaths method
	 */
	public static StringBuffer alterBodyHTML(StringBuffer HTML, String serverName) {
		return new StringBuffer(alterBodyHTML(HTML.toString(), serverName));
	}

	public static String alterBodyHTML(String HTML, String serverName) {
		//This is the new Regular Expression for white spaces ([ .\r\n&&[^>]]*)
		// Replacing links "a href" like tags axcluding the mail to links

		HTML = HTML.replaceAll("(?i)(?s)<a[^>]+href=\"([^/(http://)(https://)(#)(mailto:\")])(.*)\"[^>]*>",
		"<a href=\"http://$1$2\">");

		HTML = HTML.replaceAll(
				//"(?i)(?s)<a[^>]+href=\"([^>]+)\"[^>]*>(.*?)</[^>]*a[^>]*>",
				"(?i)(?s)<a href=\"([^#])([^>]+)\"[^>]*>(.*?)</[^>]*a[^>]*>",
				"<a href=\"http://"
				+ serverName
				//+ "/redirect?r=<rId>&redir=$1\">$2</a>")
				+ "/redirect?r=<rId>&redir=$1$2\">$3</a>")
				.replaceAll("<a href=\"http://" + serverName + "/redirect\\?r=<rId>&redir=(mailto:[^\"]+)\">", "<a href=\"$1\">");
		HTML = alterBodyHtmlAbsolutizePaths(HTML, serverName);
		return HTML;
	}

	/**
	 * Change all the relative paths in the email body to absolute paths
	 */
	public static StringBuffer alterBodyHtmlAbsolutizePaths(StringBuffer HTML, String serverName)
	{
		return new StringBuffer(alterBodyHtmlAbsolutizePaths(HTML.toString(), serverName));
	}

	public static String alterBodyHtmlAbsolutizePaths(String HTML, String serverName)
	{
		String message = HTML;

		//Replacing links "TD background" like tags
		message = message
		.replaceAll(
				"<\\s*td([^>]*)background\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<td$1background=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "TR background" like tags
		message = message
		.replaceAll(
				"<\\s*tr([^>]*)background\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<tr$1background=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "IMG SRC" like tags
		message = message.replaceAll(
				"<\\s*img([^>]*)src\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<img$1src=\"http://" + serverName
				+ "$2\"$3>");
		// Replacing links "A HREF" like tags
		message = message
		.replaceAll(
				"<\\s*link([^>]*)href\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<link$1href=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "SCRIPT" like tags
		message = message
		.replaceAll(
				"<\\s*script([^>]*)src\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<script$1src=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "APPLET" with codebase like tags
		message = message
		.replaceAll(
				"<\\s*applet([^>]*)codebase\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<applet$1codebase=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "APPLET" without codebase like tags
		message = message
		.replaceAll(
				"<\\s*applet(([^>][^(codebase)])*)code\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?(([^>][^(codebase)])*)>",
				"<applet$1code=\"http://"
				+ serverName
				+ "$4\"$5>");
		// Replacing links "IFRAME" src replacement
		message = message
		.replaceAll(
				"<\\s*iframe([^>]*)src\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<iframe$1src=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "IFRAME" longdesc replacement
		message = message
		.replaceAll(
				"<\\s*iframe([^>]*)longdesc\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<iframe$1longdesc=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "FRAME" src replacement
		message = message
		.replaceAll(
				"<\\s*frame([^>]*)src\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<frame$1src=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing links "FRAME" longdesc replacement
		message = message
		.replaceAll(
				"<\\s*frame([^>]*)longdesc\\s*=\\s*[\"\']?([^\'\">]*)[\"\']?([^>]*)>",
				"<frame$1longdesc=\"http://"
				+ serverName
				+ "$2\"$3>");
		// Replacing some style URLs
		message = message
		.replaceAll(
				"<([^>]*)style\\s*=\\s*[\"\']?([^\'\">]*)url\\s*\\(\\s*([^>]*)\\s*\\)([^\'\">]*)[\"\']?([^>]*)>",
				"<$1style=\"$2url(http://"
				+ serverName
				+ "$3)$4\"$5>");
		// Fixing absolute paths
		message = message.replaceAll("http://"
				+ serverName + "\\s*http://",
		"http://");

		return message;
	}

	/**
	 * Sends the forgot password email with the new random generated password
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public static boolean sendForgotPassword(User user, String newPassword, String hostId) throws DotDataException, DotSecurityException {

		HostAPI hostAPI = APILocator.getHostAPI();

		// build a decent default context
		Context context = VelocityUtil.getBasicContext();
		context.put("user", user);
		context.put("UtilMethods", new UtilMethods());
		context.put("language", "1");
		context.put("password", newPassword);

		Host host = hostAPI.find(hostId, user, true);
		context.put("host", host);

		StringWriter writer = new StringWriter();

		String idInode = APILocator.getIdentifierAPI().find(host, Config
				.getStringProperty("PATH_FORGOT_PASSWORD_EMAIL")).getInode();

		try {
			String message = "";
			try {
				Template t = UtilMethods.getVelocityTemplate("live/"+ idInode+ "."+ Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION")); 
				t.merge(context, writer);
				Logger
				.debug(EmailFactory.class, "writer:"
						+ writer.getBuffer());
				message = writer.toString().trim();
			} catch (ResourceNotFoundException ex) {
				message = "<center><b>And error has ocurred loading de message's page<b></center>";
			}

			Mailer m = new Mailer();
			m.setToEmail(user.getEmailAddress());
			m.setSubject("Your " + host.getHostname() + " Password");
			m.setHTMLBody(message);
			m.setFromEmail(Config.getStringProperty("EMAIL_SYSTEM_ADDRESS"));
			return m.sendMessage();
		} catch (Exception e) {
			Logger.warn(EmailFactory.class, e.toString(), e);
			return false;
		}

	}

	public static boolean isSubscribed(MailingList list, User s){

		UserProxy up;
		try {
			up = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(s,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(EmailFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}	
		return MailingListFactory.isSubscribed(list, up);

	}

	/**
	 * 
	 * Send emails based on the parameters given to the method, basically is used
	 * to send emails based on parameters received html forms but it can be used 
	 * to send emails using parameters that come form any source
	 * 
	 * Some parameters are required and need to be passed in order the method to work 
	 * correctly, here it is the description of the predefined parameters and when they are 
	 * required or not:
	 * 
	 * formType/formName: 	The name of the form used to generate the reports and to save the files submitted 
	 * 						as parameters, so if no formType or formName is supplied then no report
	 * 						or files will be saved.
	 * to:					Email address where the email will be sent
	 * from:				Email address will be used as the from address
	 * subject:				Subject of the email
	 * cc:					Carbon copy of the email
	 * bcc:					Hidden carbon copy of the email
	 * html:				Set it to false|0|f if you don't want to send an html kind of email
	 * dispatch:			Not used reserved for struts processing
	 * order:				Order how you want to present the parameters, the valid syntax of this 
	 * 						parameter is like: param1,param2,...
	 * prettyOrder:			This property goes together with the order property to specify 
	 * 						prettiest names to the parameters, these prettiest names are used
	 * 						when building the simple html table e-mail  
	 * ignore:				Parameters you want to exclude from the email sent, the valid syntax of this 
	 * 						parameter is like ;param1;param2;..., this is only used when building the 
	 * 						simple html table email body and not when building the email from a given template
	 * emailTemplate: 		The path to the email template to be used to generate the email
	 * 						it can be a path in the dotCMS tree or a real path in the server file system
	 * 						under /liferay folder, dotCMS paths take precedence over filesystem paths
	 * attachFiles: 		A comma separated list of the file kind of fields you want the method include as
	 * 					    attachments to the email to send
	 * If the following parameters are included an auto reply message will be send
	 * to the from email  
	 * autoReplyFrom:     Address to be used as the from 
	 * autoReplySubject:  Subject of the message
	 * autoReplyText: 	  Message to send
	 * autoReplyTemplate: A path to an html template that can be used to generate this message
	 * 					  it can be a path in the dotCMS tree or a real path in the server file system
	 * 					  under /liferay folder
	 *
	 * @param parameters A map of the submitted fields, any kind of parameter can 
	 * be used and will be passed to the email template to render the email body text used when
	 * sending the email but only string or file kind parameters will be used to generate the database
	 * reports and generate plain table html table like body text when no html template is passed 
	 * in the parameters
	 *   
	 * @param spamValidation List of fields that wants to be checked for possible spam
	 * @param otherIgnoredParams A list of other fields (more than the default ones) that you want to
	 * 							 be excluded from the list of fields to be sent in the email 
	 * @param host Current dotCMS host used to submit the form
	 * @param user User how submits the form (can be null then the user parameters won't be excluded
	 * 				on the template substitution)
	 * @return The saved webform if a formType/formName parameter is specified if not returns null.
	 * @throws 	DotRuntimeException when spam is detected or any other mayor error occurs
	 * 	 		CreditCardDeniedException when the credit card gets denied
	 */
	public static WebForm sendParameterizedEmail(Map<String,Object> parameters, Set<String> spamValidation, 
			Host host, User user) throws  DotRuntimeException
			{

		// check for possible spam
		if(spamValidation != null)
			if (FormSpamFilter.isSpamRequest(parameters, spamValidation)) {
				throw new DotRuntimeException("Spam detected");
			}

		//Variables initialization

		//Default parameters to be ignored when sending the email
		String ignoreString = ":formType:formName:to:from:subject:cc:bcc:html:dispatch:order:" +
		"prettyOrder:autoReplyTo:autoReplyFrom:autoReplyText:autoReplySubject:" +
		"ignore:emailTemplate:autoReplyTemplate:autoReplyHtml:chargeCreditCard:attachFiles:";
		if(UtilMethods.isSet(getMapValue("ignore", parameters))) {
			ignoreString += getMapValue("ignore", parameters).toString().replace(",", ":") + ":";
		}

		// Sort the forms' fields by the given order parameter
		String order = (String)getMapValue("order", parameters);
		Map<String, Object> orderedMap = new LinkedHashMap<String, Object>();

		// Parameter prettyOrder is used to map
		// the pretty names of the variables used in the order field
		// E.G: order = firstName, lastName
		//		prettyOrder = First Name, Last Name
		String prettyOrder = (String)getMapValue("prettyOrder", parameters);
		Map<String, String> prettyVariableNamesMap = new LinkedHashMap<String, String>();

		// Parameter attachFiles is used to specify the file kind of fields you want to attach
		// to the mail is sent by this method
		// E.G: attachFiles = file1, file2, ...
		String attachFiles = (String)getMapValue("attachFiles", parameters);

		//Building the parameters maps from the order and pretty order parameters
		if (order != null) {
			String[] orderArr = order.split("[;,]");
			String[] prettyOrderArr = prettyOrder!=null?prettyOrder.split("[;,]"):new String[0];

			for (int i = 0; i < orderArr.length; i++) {
				String orderParam = orderArr[i].trim();
				Object value = (getMapValue(orderParam, parameters) == null) ? 
						null : getMapValue(orderParam, parameters);

				if(value != null) {
					//if pretty name is passed using it as a key value in the ordered map
					if (prettyOrderArr.length > i) 
						prettyVariableNamesMap.put(orderArr[i].trim(), prettyOrderArr[i].trim());
					else
						prettyVariableNamesMap.put(orderArr[i].trim(), orderArr[i].trim());
					orderedMap.put(orderArr[i].trim(), value);
				}
			}

		}

		for (Entry<String, Object> param : parameters.entrySet()) {
			if(!orderedMap.containsKey(param.getKey())) {
				orderedMap.put(param.getKey(), param.getValue());
				prettyVariableNamesMap.put(param.getKey(), param.getKey());
			}
		}

		StringBuffer filesLinks = new StringBuffer();

		// Saving the form in the database and the submitted file to the dotCMS
		String formType = getMapValue("formType", parameters) != null?
				(String)getMapValue("formType", parameters):(String)getMapValue("formName", parameters);

				WebForm formBean = saveFormBean(parameters, host, formType, ignoreString, filesLinks);


				// Setting up the email
				// Email variables - decrypting crypted email addresses 

				String from = UtilMethods.replace((String)getMapValue("from", parameters), "spamx", "");
				String to = UtilMethods.replace((String)getMapValue("to", parameters), "spamx", "");
				String cc = UtilMethods.replace((String)getMapValue("cc", parameters), "spamx", "");
				String bcc = UtilMethods.replace((String)getMapValue("bcc", parameters), "spamx", "");
				String fromName = UtilMethods.replace((String)getMapValue("fromName", parameters), "spamx", "");
				try { from = PublicEncryptionFactory.decryptString(from); } catch (Exception e) { }
				try { to = PublicEncryptionFactory.decryptString(to); } catch (Exception e) { }
				try { cc = PublicEncryptionFactory.decryptString(cc); } catch (Exception e) { }
				try { bcc = PublicEncryptionFactory.decryptString(bcc); } catch (Exception e) { }
				try { fromName = PublicEncryptionFactory.decryptString(fromName); } catch (Exception e) { }

				String subject = (String)getMapValue("subject", parameters);
				subject = (subject == null) ? "Mail from " + host.getHostname() + "" : subject;

				String emailFolder = (String)getMapValue("emailFolder", parameters);

				boolean html = getMapValue("html", parameters) != null?Parameter.getBooleanFromString((String)getMapValue("html", parameters)):true;

				String templatePath = (String) getMapValue("emailTemplate", parameters);

				// Building email message no template
				Map<String, String> emailBodies = null;

				try {
					emailBodies = buildEmail(templatePath, host, orderedMap, prettyVariableNamesMap, filesLinks.toString(), ignoreString, user);
				} catch (Exception e) {
					Logger.error(EmailFactory.class, "sendForm: Couldn't build the email body text.", e);
					throw new DotRuntimeException("sendForm: Couldn't build the email body text.", e);
				}

				// Saving email backup in a file
				try {
					String filePath = FileUtil.getRealPath(Config.getStringProperty("EMAIL_BACKUPS"));
					new File(filePath).mkdir();

					File file = null;
					synchronized (emailTime) {
						emailTime = new Long(emailTime.longValue() + 1);
						if (UtilMethods.isSet(emailFolder)) {
							new File(filePath + File.separator + emailFolder).mkdir();
							filePath = filePath + File.separator + emailFolder;
						}
						file = new File(filePath + File.separator + emailTime.toString()
								+ ".html");
					}
					if (file != null) {
						java.io.OutputStream os = new java.io.FileOutputStream(file);
						BufferedOutputStream bos = new BufferedOutputStream(os);
						if(emailBodies.get("emailHTMLBody") != null)
							bos.write(emailBodies.get("emailHTMLBody").getBytes());
						else if(emailBodies.get("emailHTMLTableBody") != null) 
							bos.write(emailBodies.get("emailHTMLTableBody").getBytes());
						else
							bos.write(emailBodies.get("emailPlainTextBody").getBytes());
						bos.flush();
						bos.close();
						os.close();
					}
				} catch (Exception e) {
					Logger.warn(EmailFactory.class, "sendForm: Couldn't save the email backup in " + Config.getStringProperty("EMAIL_BACKUPS"));
				}

				// send the mail out;
				Mailer m = new Mailer();
				m.setToEmail(to);
				m.setFromEmail(from);
				m.setFromName(fromName);
				m.setCc(cc);
				m.setBcc(bcc);
				m.setSubject(subject);

				if (html) {
					if(UtilMethods.isSet(emailBodies.get("emailHTMLBody")))
						m.setHTMLBody(emailBodies.get("emailHTMLBody"));
					else
						m.setHTMLBody(emailBodies.get("emailHTMLTableBody"));
				}
				m.setTextBody(emailBodies.get("emailPlainTextBody"));

				//Attaching files requested to be attached to the email
				if(attachFiles != null) {
					attachFiles = "," + attachFiles.replaceAll("\\s", "") + ",";
					for(Entry<String, Object> entry : parameters.entrySet()) {
						if(entry.getValue() instanceof File && attachFiles.indexOf("," + entry.getKey() + ",") > -1) {
							File f = (File)entry.getValue();
							m.addAttachment(f, entry.getKey() + "." + UtilMethods.getFileExtension(f.getName()));
						}
					}
				}

				if (m.sendMessage()) {

					// there is an auto reply, send it on
					if ((UtilMethods.isSet((String)getMapValue("autoReplyTemplate", parameters)) ||
							UtilMethods.isSet((String)getMapValue("autoReplyText", parameters)))
							&& UtilMethods.isSet((String)getMapValue("autoReplySubject", parameters))
							&& UtilMethods.isSet((String)getMapValue("autoReplyFrom", parameters))) {

						templatePath = (String) getMapValue("autoReplyTemplate", parameters);

						if(UtilMethods.isSet(templatePath)) {
							try {
								emailBodies = buildEmail(templatePath, host, orderedMap, prettyVariableNamesMap, filesLinks.toString(), ignoreString, user);
							} catch (Exception e) {
								Logger.error(EmailFactory.class, "sendForm: Couldn't build the auto reply email body text. Sending plain text.", e);
							}
						}

						m = new Mailer();
						String autoReplyTo = (String)(getMapValue("autoReplyTo", parameters) == null?getMapValue("from", parameters):getMapValue("autoReplyTo", parameters));
						m.setToEmail(UtilMethods.replace(autoReplyTo, "spamx", ""));
						m.setFromEmail(UtilMethods.replace((String)getMapValue("autoReplyFrom", parameters), "spamx", ""));
						m.setSubject((String)getMapValue("autoReplySubject", parameters));

						String autoReplyText = (String)getMapValue("autoReplyText", parameters); 
						boolean autoReplyHtml = getMapValue("autoReplyHtml", parameters) != null?Parameter.getBooleanFromString((String)getMapValue("autoReplyHtml", parameters)):html;
						if (autoReplyText != null)
						{
							if(autoReplyHtml)
							{
								m.setHTMLBody((String)getMapValue("autoReplyText", parameters));
							} else {
								m.setTextBody((String)getMapValue("autoReplyText", parameters));
							}
						}
						else
						{
							if (autoReplyHtml) 
							{
								if(UtilMethods.isSet(emailBodies.get("emailHTMLBody")))
									m.setHTMLBody(emailBodies.get("emailHTMLBody"));
								else
									m.setHTMLBody(emailBodies.get("emailHTMLTableBody"));
							}
							m.setTextBody(emailBodies.get("emailPlainTextBody"));
						}
						m.sendMessage();
					}
				} else {
					if(formBean != null){
						try {
							HibernateUtil.delete(formBean);
						} catch (DotHibernateException e) {							
							Logger.error(EmailFactory.class, e.getMessage(), e);
						}
					}
					throw new DotRuntimeException("Unable to send the email");
				}

				return formBean;

			}

	public static Map<String, String> buildEmail(String templatePath, Host host, Map<String,Object> parameters, 
			Map<String, String> prettyParametersNamesMap, String filesLinks, String ignoreString, User user) 
			throws WebAssetException, ResourceNotFoundException, ParseErrorException, MethodInvocationException, IOException, DotDataException, DotSecurityException, PortalException, SystemException
			{
		StringBuffer emailHTMLBody = new StringBuffer();
		StringBuffer emailHTMLTableBody = new StringBuffer();
		StringBuffer emailPlainTextBody = new StringBuffer();

		//Case when a html page template is passed as parameter
		if(UtilMethods.isSet(templatePath))
		{
			String idInode = APILocator.getIdentifierAPI().find(host,templatePath).getInode();

			Template t = null;

			try {
				if(InodeUtils.isSet(idInode)) {
					t = UtilMethods.getVelocityTemplate("live/"+ idInode+ "."+ Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION")); 
				} else {
					t = UtilMethods.getVelocityTemplate(templatePath); 
				}
			} catch (Exception e) {
			}

			if (t != null) {

				HttpServletRequest request = (HttpServletRequest) parameters.get("request"); 
				HttpServletResponse response = (HttpServletResponse) parameters.get("response");
				Context context = null;
				if(InodeUtils.isSet(idInode) && request != null && response != null)
				{
					context = VelocityUtil.getWebContext(request,response);
				}
				else
				{
					context = VelocityUtil.getBasicContext();
				}


				//Copying the parameters to the context 
				for(Entry<String, Object> entry : parameters.entrySet()) {
					Object value = getMapValue(entry.getKey(), parameters);
					if(entry.getKey().equals("ccNumber") && value instanceof String) {
						value = (String)UtilMethods.obfuscateCreditCard((String)value);
					}
					if(entry.getKey().contains("cvv") && value instanceof String) {
						String valueString = (String)value;
						if(valueString.length() > 3){
							value = (String)UtilMethods.obfuscateString(valueString,2);
						}
						else {
							value = (String)UtilMethods.obfuscateString(valueString,1);
						}
					}
					context.put(entry.getKey(), value);
				}
				context.put("utilMethods", new UtilMethods());
				context.put("UtilMethods", new UtilMethods());
				context.put("host", host);
				if(user != null)
					context.put("user", user);

				StringWriter writer = new StringWriter();

				//Rendering the html template with the parameters
				t.merge(context, writer);
				String textVar = writer.toString();
				emailHTMLBody = new StringBuffer(alterBodyHtmlAbsolutizePaths(replaceTextVar(textVar, parameters, user), host.getHostname()));
			}
		}

		String subject = (String)getMapValue("subject", parameters);
		subject = (subject == null) ? "Mail from " + host.getHostname(): subject;

		emailHTMLTableBody.append("<html><style>td{font-family:arial;font-size:10pt;}</style><BODY>");
		emailHTMLTableBody.append("<TABLE bgcolor=eeeeee width=95%>");
		emailHTMLTableBody.append("<TR><TD colspan=2><strong>Information from "
				+ host.getHostname() + ": " + subject
				+ "</strong></TD></TR>");
		emailPlainTextBody.append("Information from " + host.getHostname()
				+ ": \t" + subject + "\n\n");

		// Loop over the request Map or the ordered Map
		Iterator<Entry<String,Object>> it = parameters.entrySet().iterator();

		while (it.hasNext()) {

			Entry<String, Object> e = (Entry<String, Object>) it.next();
			String key = e.getKey();
			Object mapvalue = getMapValue(key, parameters);
			if (mapvalue instanceof String) {
				String value = (String)mapvalue;
				if(key.equals("ccNumber") && value instanceof String) {
					value = (String)UtilMethods.obfuscateCreditCard((String)value);
				}
				if(key.contains("cvv") && value instanceof String) {
					String valueString = (String)value;
					if(valueString.length() > 3){
						value = (String)UtilMethods.obfuscateString(valueString,2);
					}
					else {
						value = (String)UtilMethods.obfuscateString(valueString,1);
					}
				}
				if (ignoreString.indexOf(":" + key + ":") < 0 && UtilMethods.isSet(value)) {
					String prettyKey = prettyParametersNamesMap.get(key);
					String capKey = prettyKey != null?prettyKey:UtilMethods.capitalize(key);
					emailHTMLTableBody.append("<TR><TD bgcolor=white valign=top nowrap>&nbsp;" + capKey + "&nbsp;</TD>");
					emailHTMLTableBody.append("<TD bgcolor=white valign=top width=100%>" + value + "</TD></TR>");
					emailPlainTextBody.append(capKey + ":\t" + value + "\n");
				}
			}
		}

		if (UtilMethods.isSet(filesLinks)) {
			emailHTMLTableBody.append("<TR><TD bgcolor=white valign=top nowrap>&nbsp;Files&nbsp;</TD>");
			emailHTMLTableBody.append("<TD bgcolor=white valign=top width=100%>" + filesLinks + "</TD></TR>");
			emailPlainTextBody.append("Files:\t" + filesLinks + "\n");
		}

		emailHTMLTableBody.append("</TABLE></BODY></HTML>");

		Map<String, String> returnMap = new HashMap<String, String>();
		if(UtilMethods.isSet(emailHTMLBody.toString()))
			returnMap.put("emailHTMLBody", emailHTMLBody.toString());
		if(UtilMethods.isSet(emailHTMLTableBody.toString()))
			returnMap.put("emailHTMLTableBody", emailHTMLTableBody.toString());
		returnMap.put("emailPlainTextBody", emailPlainTextBody.toString());

		return returnMap;
			}

	private static WebForm saveFormBean (Map<String, Object> parameters, Host host, String formType, String ignoreString, StringBuffer filesLinks) {

		//Fields predefined for the form reports
		String predefinedFields = ":prefix:title:firstName:middleInitial:middleName:lastName:fullName:organization:address:address1:address2:city:state:zip:country:phone:email:";

		//Return variable
		WebForm formBean = new WebForm();
		formBean.setFormType(formType);

		// Copy the common fields set in the form
		try {
			for (Entry<String, Object> param : parameters.entrySet()) {
				BeanUtils.setProperty(formBean, param.getKey(), getMapValue(param.getKey(), parameters));
			}
		} catch (Exception e1) {
			Logger.error(EmailFactory.class, "sendForm: Error ocurred trying to copy the form bean parameters", e1);
		}

		try {
			HibernateUtil.save(formBean);
		} catch (DotHibernateException e) {
			Logger.error(EmailFactory.class, e.getMessage(), e);
		}		
		String formId = formBean.getWebFormId();

		// Loop over the request Map or the ordered Map to set the custom
		// fields and also saving the submitted files
		StringBuffer customFields = new StringBuffer();

		Set<Entry<String, Object>> paramSet = parameters.entrySet();

		for (Entry<String, Object> param : paramSet) {

			String key = (String) param.getKey();


			String value = null;

			Object paramValue = getMapValue(key, parameters);
			if (paramValue instanceof File) {

				File f = (File) param.getValue();
				String submittedFileName = f.getName();
				String fileName = key + "." + UtilMethods.getFileExtension(submittedFileName);
				if(getMapValue(fileName.substring(4, key.length()) + "FName", parameters) != null) {
					fileName = getMapValue(fileName.substring(4, key.length()) + "FName", parameters) + 
						"." + UtilMethods.getFileExtension(submittedFileName);
				}

				//Saving the file
				try {
					if(f.exists()) {
						String filesFolder = getMapValue("formFolder", parameters) instanceof String?(String)getMapValue("formFolder", parameters):null;
						
						String fileLink = saveFormFile(formId, formType, fileName, f, host, filesFolder);
						filesLinks.append(filesLinks.toString().equals("")? "http://" + host.getHostname() + fileLink : ",http://" + host.getHostname() + fileLink);
					}
				} catch (Exception e) {
					Logger.error(EmailFactory.class, "sendForm: couldn't saved the submitted file into the cms = " + fileName, e);					
					try {
						HibernateUtil.delete(formBean);
					} catch (DotHibernateException e1) {
						Logger.error(EmailFactory.class, e1.getMessage(), e1);						
					}
					throw new DotRuntimeException("sendForm: couldn't saved the submitted file into the cms = " + fileName, e);
				}

			} else if (paramValue instanceof String)
				value = (String)paramValue;

			List<String> cFields = new ArrayList<String>();
			if (predefinedFields.indexOf(":" + key + ":") < 0
					&& ignoreString.indexOf(":" + key + ":") < 0
					&& UtilMethods.isSet(value)) {
				value = value.replaceAll("\\|", " ").replaceAll("=", " ");
				if(key.equals("ccNumber"))
					value = UtilMethods.obfuscateCreditCard(value);

				String capKey = UtilMethods.capitalize(key);
				int aux = 2;
				String capKeyAux = capKey;
				while (cFields.contains(capKeyAux)) {
					capKeyAux = capKey + aux;
					++aux;
				}
				cFields.add(capKeyAux);
				String cField = capKeyAux + "=" + value;
				customFields.append(cField + "|");
			}
		}

		customFields.append("Files=" + filesLinks);

		//Setting the custom fields and saving them
		formBean.setCustomFields(customFields.toString());
		formBean.setSubmitDate(new Date());

		if(UtilMethods.isSet(formType)){
			try {
				HibernateUtil.saveOrUpdate(formBean);
			} catch (DotHibernateException e) {
				throw new DotRuntimeException("Webform Save Failed");
			}
		}
		else{
			Logger.debug(EmailFactory.class, "The web form doesn't have the required formType field, the form data will not be saved in the database.");
		}


		return formBean;
	}

	

	private static String getFormFileFolderPath (String formType, String formInode) {
		String path = Config.getStringProperty("SAVED_UPLOAD_FILES_PATH")
		+ "/" + formType.replace(" ", "_") + "/"
		+ String.valueOf(formInode).substring(0, 1) + "/" + formInode;
		return path;
	}

	private static String saveFormFile (String formInode, String formType, 
			String fileName, File fileToSave, Host currentHost, String filesFolder) throws Exception {
		FileAPI fileAPI=APILocator.getFileAPI();
		String path;
		if(filesFolder != null)
			path = filesFolder;
		else
			path = getFormFileFolderPath(formType, formInode);
		Folder folder = APILocator.getFolderAPI().createFolders(path, currentHost, APILocator.getUserAPI().getSystemUser(), false);
		String baseFilename = fileName;
		int c = 1;
		while(fileAPI.fileNameExists(folder, fileName)) {
			fileName = UtilMethods.getFileName(baseFilename) + "-" + c + "." + UtilMethods.getFileExtension(baseFilename);
			c++;
		}
		Host host = APILocator.getHostAPI().find(folder.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
		while(APILocator.getFileAssetAPI().fileNameExists(host,folder, fileName, "")) {
			fileName = UtilMethods.getFileName(baseFilename) + "-" + c + "." + UtilMethods.getFileExtension(baseFilename);
			c++;
		}
		
		Contentlet cont = new Contentlet();
		cont.setStructureInode(folder.getDefaultFileType());
		cont.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(fileName));
		cont.setFolder(folder.getInode());
		cont.setHost(host.getIdentifier());
		cont.setBinary(FileAssetAPI.BINARY_FIELD, fileToSave);
		APILocator.getContentletAPI().checkin(cont, APILocator.getUserAPI().getSystemUser(),false);

		return path + "/" + fileName;
	}

	private static String replaceTextVar(String template, Map<String, Object> parameters, User user) 
	{
		String finalMessageStr = template;

		Set<String> keys = parameters.keySet();
		for(String key : keys)
		{
			if(getMapValue(key, parameters) instanceof String) {
				String value = (String)getMapValue(key, parameters);
				value = (value != null ? value : "");
				finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+ key +"(>|(&gt;))", "");
				finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + key + "(\")?( )*/*( )*(>|(&gt;))",value);
			}
		}

		if(UtilMethods.isSet(user))
		{
			Address address = new Address();
			try {
				List<Address> adds = PublicAddressFactory.getAddressesByUserId(user.getUserId());
				if (adds != null && adds.size() > 0) {
					address = (Address) adds.get(0);
				}
			}
			catch(Exception e) {
					Logger.error(EmailFactory.class, "Send To Friend Failed" + e);
			}

			//Variables replacement from user object
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varName(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varName(\")?( )*/*( )*(>|(&gt;))", (user.getFirstName()!=null) ? user.getFirstName() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varEmail(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varEmail(\")?( )*/*( )*(>|(&gt;))", (user.getEmailAddress()!=null) ? user.getEmailAddress() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varMiddleName(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varMiddleName(\")?( )*/*( )*(>|(&gt;))", (user.getMiddleName()!=null) ? user.getMiddleName() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varLastName(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varLastName(\")?( )*/*( )*(>|(&gt;))", (user.getLastName()!=null) ? user.getLastName() : "");

			UserProxy userproxy;
			try {
				userproxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(EmailFactory.class, e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}	
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varLastMessage(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varLastMessage(\")?( )*/*( )*(>|(&gt;))", (userproxy.getLastMessage()!=null) ? userproxy.getLastMessage() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varAddress1(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varAddress1(\")?( )*/*( )*(>|(&gt;))", (address.getStreet1()!=null) ? address.getStreet1() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varAddress2(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varAddress2(\")?( )*/*( )*(>|(&gt;))", (address.getStreet2()!=null) ? address.getStreet2() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varPhone(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varPhone(\")?( )*/*( )*(>|(&gt;))", (address.getPhone()!=null) ? address.getPhone() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varState(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varState(\")?( )*/*( )*(>|(&gt;))", (address.getState()!=null) ? address.getState() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varCity(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varCity(\")?( )*/*( )*(>|(&gt;))", (address.getCity()!=null) ? address.getCity() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varCountry(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varCountry(\")?( )*/*( )*(>|(&gt;))", (address.getCountry()!=null) ? address.getCountry() : "");

			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/varZip(>|(&gt;))", "");
			finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))varZip(\")?( )*/*( )*(>|(&gt;))", (address.getZip()!=null) ? address.getZip() : "");

			//gets default company to get locale
			Company comp = PublicCompanyFactory.getDefaultCompany();

			try {

				int varCounter = 1;

				for (;varCounter < 26;varCounter++) {
					String var = LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var" + varCounter);
					if (var!=null) var = var.replaceAll(" ","_");

					String value = "";
					try {
						value = BeanUtils.getSimpleProperty(userproxy, "var" + varCounter);
					} catch (Exception e) {
						Logger.error(EmailFactory.class, "An error as ocurred trying to access the variable var" + varCounter + " from the user proxy.", e);
					}

					finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/"+var+"(>|(&gt;))", "");
					finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))" + var + "(\")?( )*/*( )*(>|(&gt;))", (value != null) ? value : "");

					finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))/var" + varCounter + "(>|(&gt;))", "");
					finalMessageStr = finalMessageStr.replaceAll("(?i)(<|(&lt;))var" + varCounter + "(\")?( )*/*( )*(>|(&gt;))", (value != null) ? value : "");
				}

			} catch(LanguageException le) {
				Logger.error(EmailFactory.class, le.getMessage());
			}
		}
		return finalMessageStr;
	}

	public static Object getMapValue(String key, Map<String, Object> map) {

		try {
			try
			{
				if(((Object[]) map.get(key)).length > 1)
				{
					String returnValue = "";
					for(Object object : ((Object[]) map.get(key)))
					{
						returnValue += object.toString() + ", ";						
					}
					returnValue = returnValue.substring(0,returnValue.lastIndexOf(","));
					return returnValue;
				}
			}
			catch(Exception ex)
			{}
			return ((Object[]) map.get(key))[0];
			
		} catch (Exception e) {
			try {
				return (Object) map.get(key);
			} catch (Exception ex) {
				return null;
			}

		}

	}	

}
