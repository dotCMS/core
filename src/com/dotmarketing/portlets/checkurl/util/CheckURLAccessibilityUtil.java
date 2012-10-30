package com.dotmarketing.portlets.checkurl.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.checkurl.bean.CheckURLBean;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * Utility class.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 2, 2012
 */
public class CheckURLAccessibilityUtil {

	//public static variables
//	public static String PLUGIN_ID = "it.eng.dotcms.checkURL";
//	public static String FIELD_LIST_KEY = "fieldList";
//	public static String ERROR_BROKEN_LINKS_KEY = "errorBrokenLinks";
//	public static String EMAIL_BODY_KEY = "emailBody";
//	public static String EMAIL_FROM_KEY = "emailFrom";
//	public static String EMAIL_SUBJECT_KEY = "emailSubject";
//	public static String EMAIL_FULL_NAME_KEY = "emailFromFullName";
	public static String BROKEN_LINKS_PORTLET_LEFT_TITLE = "EXT_BROKEN_LINKS.leftTitle";
	public static String BROKEN_LINKS_PORTLET_LEFT_TEXT = "EXT_BROKEN_LINKS.leftText";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_TITLE = "EXT_BROKEN_LINKS.title";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_FIELD_NAME = "EXT_BROKEN_LINKS.fieldName";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_OWNER = "EXT_BROKEN_LINKS.owner";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_LEU = "EXT_BROKEN_LINKS.lastEditUser";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_STR_NAME = "EXT_BROKEN_LINKS.structureName";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_STR_HOST = "EXT_BROKEN_LINKS.structureHost";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_LMD = "EXT_BROKEN_LINKS.lastModifiedDate";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_LINK = "EXT_BROKEN_LINKS.link";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_ACTION = "EXT_BROKEN_LINKS.action";
	public static String BROKEN_LINKS_PORTLET_DETAIL_TABLE_EMPTY_LIST = "EXT_BROKEN_LINKS.emptyList";
	
	
	//private static variables
	private static int DEFAULT_PROXY_PORT = 8080;	
	
	/**
	 * Used for check URL over an http proxy. Its depends by plugin.properties file.
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public static boolean reloadProxyConfiguration() throws DotDataException{
		boolean result = true;
		if(!ProxyManager.INSTANCE.isLoaded()){
			Logger.info(CheckURLAccessibilityUtil.class, "Load singleton proxy configuration...");
			Connection connection = new Connection();
			Mail mail = new Mail();
			
			//load plugin properties
			boolean proxy = Config.getBooleanProperty("urlcheck.connection.proxy", false);
			String proxyHost = Config.getStringProperty("urlcheck.connection.proxyHost");
			int proxyPort = 0;
			try{
				proxyPort = Config.getIntProperty("urlcheck.connection.proxyPort",8080);
			}catch(NumberFormatException e){
				proxyPort = DEFAULT_PROXY_PORT;
			}
			boolean proxyAuth = Config.getBooleanProperty("urlcheck.connection.proxyRequiredAuth", false);			
			String proxyUsername = Config.getStringProperty("urlcheck.connection.proxyUsername");
			String proxyPassword = Config.getStringProperty("urlcheck.connection.proxyPassword");
			boolean sendMail = Config.getBooleanProperty("urlcheck.mail.send", false);
			connection.setProxy(proxy);
			connection.setProxyHost(proxyHost);
			connection.setProxyPassword(proxyPassword);
			connection.setProxyPort(proxyPort);
			connection.setProxyRequiredAuth(proxyAuth);
			connection.setProxyUsername(proxyUsername);
			mail.setSend(sendMail);
			Logger.info(CheckURLAccessibilityUtil.class, connection.toString());
			ProxyManager.INSTANCE.setConnection(connection);
			ProxyManager.INSTANCE.setMail(mail);
			Logger.info(CheckURLAccessibilityUtil.class, "...singleton proxy configuration loaded successfully");
		}
		return result;
	}
	
	public static List<CheckURLBean> getInternalLinks(List<CheckURLBean> total){
		List<CheckURLBean> result = new ArrayList<CheckURLBean>();
		Iterator<CheckURLBean> it_total = total.iterator();
		while(it_total.hasNext()){
			CheckURLBean c = it_total.next();
			if(c.isInternalLink()){
				result.add(c);
				it_total.remove();
			}
		}
		return result;
	}
	
	public static boolean isValidInternalLink(CheckURLBean internalLink, List<Host> hosts) throws DotDataException, DotSecurityException{
		boolean result = false;
		for(Host h : hosts){
			Identifier id = APILocator.getIdentifierAPI().find(h, internalLink.getUrl());
			if(null!=id.getId() && !"".equals(id.getId()) && !"null".equals(id.getId()))
				result = true;
		}
		return result;
	}
}
