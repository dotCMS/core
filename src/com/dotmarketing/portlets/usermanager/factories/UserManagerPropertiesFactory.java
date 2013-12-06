package com.dotmarketing.portlets.usermanager.factories;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;

import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;

public class UserManagerPropertiesFactory {

	//begin User Manager Fields Display Configuration
	private static void _checkConfigFile() throws Exception {

		try {
			String filePath = getUserManagerConfigPath() + "user_manager_config.properties";

			boolean copy = false;

			// Create empty file
			File from = new File(filePath);
			if (!from.exists()) {
				Properties properties = new Properties();
				properties.put(Config.getStringProperty("ADDITIONAL_INFO_MIDDLE_NAME_PROPNAME"), Config.getStringProperty("ADDITIONAL_INFO_MIDDLE_NAME_VISIBILITY"));
				properties.put(Config.getStringProperty("ADDITIONAL_INFO_DATE_OF_BIRTH_PROPNAME"), Config.getStringProperty("ADDITIONAL_INFO_DATE_OF_BIRTH_VISIBILITY"));
				properties.put(Config.getStringProperty("ADDITIONAL_INFO_CELL_PROPNAME"), Config.getStringProperty("ADDITIONAL_INFO_CELL_VISIBILITY"));
				properties.put(Config.getStringProperty("ADDITIONAL_INFO_CATEGORIES_PROPNAME"), Config.getStringProperty("ADDITIONAL_INFO_CATEGORIES_VISIBILITY"));

				Company comp = PublicCompanyFactory.getDefaultCompany();

				int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");
				for (int i = 1; i <= numberGenericVariables; i++) {
					properties.put(LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var" + i).replace(" ", "_"), Config.getStringProperty("ADDITIONAL_INFO_DEFAULT_VISIBILITY"));
				}

				try {
					from = new File(getUserManagerConfigPath());
					if (!from.exists())
						from.mkdirs();
					
					from = new File(filePath);
					if (!from.exists())
						from.createNewFile();
					
					properties.store(new java.io.FileOutputStream(filePath), null);
				} catch (Exception e) {
					Logger.error(UserManagerPropertiesFactory.class,e.getMessage(),e);
				}
				from = new File(filePath);
				copy = true;
			}

			String tmpFilePath = UtilMethods.getTemporaryDirPath() + "user_manager_config_properties.tmp";
			File to = new File(tmpFilePath);
			if (!to.exists()) {
				to.createNewFile();
				copy = true;
			}

			if (copy) {
				FileChannel srcChannel = new FileInputStream(from).getChannel();
				// Create channel on the destination
				FileChannel dstChannel = new FileOutputStream(to).getChannel();
				// Copy file contents from source to destination
				dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
				// Close the channels
				srcChannel.close();
				dstChannel.close();
			}

		} catch (IOException e) {
			Logger.error(UserManagerPropertiesFactory.class, "_checkLanguagesFiles:Property File Copy Failed " + e, e);
		}
	}

	private static void _retrieveProperties(RenderRequest req) throws Exception {
		HttpServletRequest servletRequest = ((RenderRequestImpl) req).getHttpServletRequest();
		_retrieveProperties(servletRequest);
	}

	private static void _retrieveProperties(ActionRequest req) throws Exception {    	
		HttpServletRequest servletRequest = ((ActionRequestImpl) req).getHttpServletRequest();
		_retrieveProperties(servletRequest);        
	}

	private static void _retrieveProperties(HttpServletRequest req) throws Exception {

		Properties properties = new Properties();

		try {
			// Loading properties file

			String filePath = UtilMethods.getTemporaryDirPath() + "user_manager_config_properties.tmp";
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(filePath));

			if (is != null) {
				properties.load(is);
			}
			is.close();
		} catch (Exception e) {
			Logger.error(UserManagerPropertiesFactory.class,"Could not load this file = user_manager_config_properties.tmp", e);
		}

		HttpSession sess = req.getSession();
		req.setAttribute(WebKeys.USERMANAGER_PROPERTIES, properties);
		sess.setAttribute(WebKeys.USERMANAGER_PROPERTIES, properties);
	}



	private static String getUserManagerConfigPath() {
		String userManagerConfigPath = Config.getStringProperty("GLOBAL_VARIABLES_PATH");
		if (!UtilMethods.isSet(userManagerConfigPath)) {
			userManagerConfigPath = FileUtil.getRealPath(File.separator + "WEB-INF" + File.separator + "classes" + File.separator);
		}
		if (!userManagerConfigPath.endsWith(File.separator))
			userManagerConfigPath = userManagerConfigPath + File.separator;
		return userManagerConfigPath;
	}


	public static void _getFieldDisplayConfiguration(RenderRequest req) throws Exception {

		// create if necesary and copy from normal files to tmp files
		_checkConfigFile();

		/* load up from tmp files */
		_retrieveProperties(req);
	}

	public static void _getFieldDisplayConfiguration(HttpServletRequest req) throws Exception {

		// create if necesary and copy from normal files to tmp files
		_checkConfigFile();

		/* load up from tmp files */
		_retrieveProperties(req);
	}

	public static void _getFieldDisplayConfiguration(ActionRequest req) throws Exception {

		// create if necesary and copy from normal files to tmp files
		_checkConfigFile();

		/* load up from tmp files */
		_retrieveProperties(req);
	}

	public static void _save(PortletRequest req, PortletResponse res, PortletConfig config, ActionForm form) throws Exception {
		try {
			String filePath = getUserManagerConfigPath() + "user_manager_config.properties";
			String tmpFilePath = UtilMethods.getTemporaryDirPath() + "user_manager_config_properties.tmp";

			// Create empty file
			File from = new java.io.File(tmpFilePath);
			from.createNewFile();

			// Create channel on the source
			File to = new java.io.File(filePath);
			to.createNewFile();

			FileChannel srcChannel = new FileInputStream(from).getChannel();

			// Create channel on the destination
			FileChannel dstChannel = new FileOutputStream(to).getChannel();

			// Copy file contents from source to destination
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

			// Close the channels
			srcChannel.close();
			dstChannel.close();

		} catch (NonWritableChannelException we) {

		} catch (IOException e) {
			Logger.error(UserManagerPropertiesFactory.class, "Property File save Failed " + e, e);
		}

		// Calling a thread to update asynchronous the config.xml file
		// we can't do this in Tomcat, it dies! we can do it in Orion
		// String webConfigFilePath =
		// config.getPortletContext().getRealPath("/") + "WEB-INF/web.xml";
		// new ConfigUpdateThread ("ConfigUpdate", webConfigFilePath).start();

		SessionMessages.add(req, "message", "message.usermanager.display.save");
	}

	public static void _add(RenderRequest req, RenderResponse res, PortletConfig config, ActionForm form) throws Exception {
		String currentFields = req.getParameter("currentFields");
		String availableFields = req.getParameter("availableFields");

		HttpSession sess = ((RenderRequestImpl) req).getHttpServletRequest().getSession();
		Properties properties = (Properties) sess.getAttribute(WebKeys.USERMANAGER_PROPERTIES);

		StringTokenizer currentTokenizer = new StringTokenizer(currentFields, ",");
		if (currentTokenizer.hasMoreTokens()) {
			for (; currentTokenizer.hasMoreTokens();) {
				String currentField = currentTokenizer.nextToken();
				properties.setProperty(currentField, "true");
			}
		}

		StringTokenizer availableTokenizer = new StringTokenizer(availableFields, ",");
		if (availableTokenizer.hasMoreTokens()) {
			for (; availableTokenizer.hasMoreTokens();) {
				String availableField = availableTokenizer.nextToken();
				properties.setProperty(availableField, "false");
			}
		}

		properties.store(new java.io.FileOutputStream(UtilMethods.getTemporaryDirPath() + "user_manager_config_properties.tmp"), null);

		req.setAttribute(WebKeys.USERMANAGER_PROPERTIES, properties);
	}

	public static void _add(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {
		String currentFields = req.getParameter("currentFields");
		String availableFields = req.getParameter("availableFields");

		HttpSession sess = ((RenderRequestImpl) req).getHttpServletRequest().getSession();
		Properties properties = (Properties) sess.getAttribute(WebKeys.USERMANAGER_PROPERTIES);

		StringTokenizer currentTokenizer = new StringTokenizer(currentFields, ",");
		if (currentTokenizer.hasMoreTokens()) {
			for (; currentTokenizer.hasMoreTokens();) {
				String currentField = currentTokenizer.nextToken();
				properties.setProperty(currentField, "true");
			}
		}

		StringTokenizer availableTokenizer = new StringTokenizer(availableFields, ",");
		if (availableTokenizer.hasMoreTokens()) {
			for (; availableTokenizer.hasMoreTokens();) {
				String availableField = availableTokenizer.nextToken();
				properties.setProperty(availableField, "false");
			}
		}

		properties.store(new java.io.FileOutputStream(UtilMethods.getTemporaryDirPath() + "user_manager_config_properties.tmp"), null);

		req.setAttribute(WebKeys.USERMANAGER_PROPERTIES, properties);
	}
	// end User Manager Fields Display Configuration
}
