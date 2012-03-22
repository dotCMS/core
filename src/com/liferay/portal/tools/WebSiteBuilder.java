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

package com.liferay.portal.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.StringUtil;

/**
 * <a href="WebSiteBuilder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.30 $
 *
 */
public class WebSiteBuilder {

	public static void main(String[] args) {
		if (args.length == 2) {
			new WebSiteBuilder(args[0], args[1]);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public static List getWebSites() throws Exception {
		File file = new File("../web-sites/web-sites.xml");

		SAXReader reader = new SAXReader();

		Document doc = null;

		try {
			doc = reader.read(file);
		}
		catch (DocumentException de) {
			Logger.error(WebSiteBuilder.class,de.getMessage(),de);
		}

		Element root = doc.getRootElement();

		List webSites = new ArrayList();

		Iterator itr = root.elements("web-site").iterator();

		while (itr.hasNext()) {
			Element webSite = (Element)itr.next();

			String id = webSite.attributeValue("id");
			boolean httpEnabled = GetterUtil.getBoolean(
				webSite.attributeValue("http-enabled"), true);
			String keystore = GetterUtil.getString(
				webSite.attributeValue("keystore"));
			String keystorePassword = GetterUtil.getString(
				webSite.attributeValue("keystore-password"));
			String virtualHosts = GetterUtil.getString(
				webSite.attributeValue("virtual-hosts"));
			String forwardURL = GetterUtil.getString(
				webSite.attributeValue("forward-url"), "/c");

			webSites.add(
				new WebSite(
					id, httpEnabled, keystore, keystorePassword, virtualHosts,
					forwardURL));
		}

		return webSites;
	}

	public WebSiteBuilder(String portalExtProperties, String orionConfigDir) {
		try {
			_portalExtProperties = portalExtProperties;
			_orionConfigDir = orionConfigDir;

			List webSites = getWebSites();

			_buildOrionASP(webSites);
			_buildWebSites(webSites);
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	private void _buildOrionASP(List webSites) throws Exception {
		if (_portalExtProperties.startsWith("${") ||
			_orionConfigDir.startsWith("${")) {

			return;
		}

		// portal-ext.properties

		BufferedReader br = new BufferedReader(new FileReader(
			_portalExtProperties));

		StringBuffer sb = new StringBuffer();
		String line = null;

		while ((line = br.readLine()) != null) {
			if (line.startsWith("portal.instances")) {
				sb.append("portal.instances=" + webSites.size());
			}
			else {
				sb.append(line);
			}

			sb.append("\n");
		}

		br.close();

		FileUtil.write(_portalExtProperties, sb.toString());

		// /orion/config/application.xml

		sb = new StringBuffer();

		Iterator itr = webSites.iterator();

		while (itr.hasNext()) {
			WebSite webSite = (WebSite)itr.next();

			if (webSite.isHttpEnabled() || webSite.isHttpsEnabled()) {
				sb.append("\t<web-module id=\"");
				sb.append(webSite.getId());
				sb.append("-web\" ");
				sb.append("path=\"../applications/");
				sb.append(webSite.getId());
				sb.append("-web.war\" />\n");
			}
		}

		File file = new File(_orionConfigDir + "/application.xml");

		String content = FileUtil.read(file);

		int x = content.indexOf("<!-- Begin ASP -->");
		int y = content.indexOf("<!-- End ASP -->");

		content =
			content.substring(0, x  + 20) + sb.toString() +
				content.substring(y - 2, content.length());

		FileUtil.write(file, content);

		// /orion/config/server.xml

		sb = new StringBuffer();

		itr = webSites.iterator();

		while (itr.hasNext()) {
			WebSite webSite = (WebSite)itr.next();

			if (webSite.isHttpEnabled()) {
				sb.append("\t<web-site path=\"./web-sites/");
				sb.append(webSite.getId());
				sb.append("-web.xml\" />\n");
			}

			if (webSite.isHttpsEnabled()) {
				sb.append("\t<web-site path=\"./web-sites/");
				sb.append(webSite.getId());
				sb.append("-web-secure.xml\" />\n");
			}
		}

		file = new File(_orionConfigDir + "/server.xml");

		content = FileUtil.read(file);

		x = content.indexOf("<!-- Begin ASP -->");
		y = content.indexOf("<!-- End ASP -->");

		content =
			content.substring(0, x  + 20) + sb.toString() +
				content.substring(y - 2, content.length());

		FileUtil.write(file, content);

		// /orion/config/web-sites/liferay.com-web.xml

		itr = webSites.iterator();

		while (itr.hasNext()) {
			WebSite webSite = (WebSite)itr.next();

			if (webSite.isHttpEnabled()) {
				_buildOrionASP(webSite, false);
			}

			if (webSite.isHttpsEnabled()) {
				_buildOrionASP(webSite, true);
			}
		}
	}

	private void _buildOrionASP(WebSite webSite, boolean secure)
		throws Exception {

		String xml =
			"<?xml version=\"1.0\"?>\n" +
			"<!DOCTYPE web-site PUBLIC \"Orion Web-site\" " +
				"\"http://www.orionserver.com/dtds/web-site.dtd\">\n" +
			"\n" +
			"<web-site " + (secure ? "secure=\"true\" " : "") +
				"virtual-hosts=\"" + webSite.getVirtualHosts() + "\">\n" +
			"\t<default-web-app application=\"default\" name=\"" +
				webSite.getId() + "-web\" load-on-startup=\"true\" />\n" +
			"\t<web-app application=\"default\" name=\"cms-web\" " +
				"root=\"/cms\" load-on-startup=\"true\" />\n" +
			"\t<web-app application=\"default\" name=\"laszlo-web\" " +
				"root=\"/laszlo\" load-on-startup=\"true\" />\n" +
			"\t<web-app application=\"default\" name=\"portal-web\" " +
				"root=\"/portal\" load-on-startup=\"true\" />\n" +
			"\t<web-app application=\"default\" name=\"tunnel-web\" " +
				"root=\"/tunnel\" load-on-startup=\"true\" />\n" +
			"\t<access-log path=\"../../log/" + webSite.getId() +
				"-web" + (secure ? "-secure" : "") + "-access.log\" />\n";

			if (secure) {
				xml +=
					"\t<ssl-config keystore=\"" + webSite.getKeystore() +
						"\" keystore-password=\"" +
							webSite.getKeystorePassword() + "\" />\n";
			}

			xml += "</web-site>";

		FileUtil.write(
			_orionConfigDir + "/web-sites/" + webSite.getId() + "-web" +
				(secure ? "-secure" : "") + ".xml",
			xml);
	}

	private void _buildWebSites(List webSites) throws Exception {

		// Session timeout

		Properties props = new Properties();
		props.load(
			new FileInputStream("../portal-ejb/classes/portal.properties"));

		String sessionTimeout =
			GetterUtil.get(props.getProperty("session.timeout"), "30");

		// Default NFC

		String nfcConf = StringUtil.replace(
			FileUtil.read(
				"../portal-ejb/src/com/liferay/portal/tools/tmpl/" +
					"nfc.conf.tmpl"),
			new String[] {"[$LISTEN_PORT$]"},
			new String[] {Integer.toString(_nfcListenPort)});

		File nfcConfFile = new File(
			"../portal-web/docroot/WEB-INF/nfc/nfc.conf");

		FileUtil.write(nfcConfFile, nfcConf);

		// web-sites

		Iterator itr = webSites.iterator();

		while (itr.hasNext()) {
			WebSite webSite = (WebSite)itr.next();

			String id = webSite.getId();
			String forwardURL = webSite.getForwardURL();

			// /docroot/index.html

			String indexHTML =
				"<html>\n" +
				"<head>\n" +
				"\t<title></title>\n" +
				"\t<meta content=\"0; url=" + forwardURL +
					"\" http-equiv=\"refresh\">\n" +
				"</head>\n" +
				"\n" +
				"<body onLoad=\"javascript:location.replace('" +
					forwardURL + "')\">\n" +
				"\n" +
				"</body>\n" +
				"\n" +
				"</html>";

			File indexHTMLFile = new File(
				"../web-sites/" + id + "-web/docroot/index.html");

			FileUtil.write(indexHTMLFile, indexHTML);

			// /docroot/WEB-INF/web.xml

			String webXML = StringUtil.replace(
				FileUtil.read(
					"../portal-ejb/src/com/liferay/portal/tools/tmpl/" +
						"web.xml.tmpl"),
				new String[] {"[$COMPANY_ID$]", "[$SESSION_TIMEOUT$]"},
				new String[] {id, sessionTimeout});

			File webXMLFile = new File(
				"../web-sites/" + id + "-web/docroot/WEB-INF/web.xml");

			FileUtil.write(webXMLFile, webXML);

			// /docroot/WEB-INF/lib/util-taglib.jar

			FileUtil.copyFile(
				"../portal-web/docroot/WEB-INF/lib/util-taglib.jar",
				"../web-sites/" + id +
					"-web/docroot/WEB-INF/lib/util-taglib.jar");

			// /docroot/WEB-INF/tld/liferay-portlet.tld

			FileUtil.copyFile(
				"../portal-web/docroot/WEB-INF/tld/liferay-portlet.tld",
				"../web-sites/" + id +
					"-web/docroot/WEB-INF/tld/liferay-portlet.tld");

			// /docroot/WEB-INF/tld/liferay-util.tld

			FileUtil.copyFile(
				"../portal-web/docroot/WEB-INF/tld/liferay-util.tld",
				"../web-sites/" + id +
					"-web/docroot/WEB-INF/tld/liferay-util.tld");

			// /docroot/WEB-INF/jcvs

			File[] jcvsConfArray = new File(
				"../portal-web/docroot/WEB-INF/jcvs/conf").listFiles();

			for (int i = 0; i < jcvsConfArray.length; i++) {
				if (jcvsConfArray[i].isFile() &&
					jcvsConfArray[i].getName().endsWith(".properties")) {

					File webJcvsConf = new File(
						"../web-sites/" + id +
							"-web/docroot/WEB-INF/jcvs/conf/" +
								jcvsConfArray[i].getName());

					if (!webJcvsConf.exists()) {
						FileUtil.copyFile(jcvsConfArray[i], webJcvsConf);
					}
				}
			}

			FileUtil.write(
				"../web-sites/" + id +
					"-web/docroot/WEB-INF/jcvs/temp/deleteme",
				"");

			FileUtil.write(
				"../web-sites/" + id +
					"-web/docroot/WEB-INF/jcvs/work/deleteme",
				"");

			// /docroot/WEB-INF/nfc

			File[] nfcArray = new File(
				"../portal-web/docroot/WEB-INF/nfc").listFiles();

			for (int i = 0; i < nfcArray.length; i++) {
				if (nfcArray[i].isFile() &&
					nfcArray[i].getName().endsWith(".properties")) {

					File webNfc = new File(
						"../web-sites/" + id + "-web/docroot/WEB-INF/nfc/" +
							nfcArray[i].getName());

					if (!webNfc.exists()) {
						FileUtil.copyFile(nfcArray[i], webNfc);
					}
				}
			}

			_nfcListenPort++;

			nfcConf = StringUtil.replace(
				FileUtil.read(
					"../portal-ejb/src/com/liferay/portal/tools/tmpl/" +
						"nfc.conf.tmpl"),
				new String[] {"[$LISTEN_PORT$]"},
				new String[] {Integer.toString(_nfcListenPort)});

			nfcConfFile = new File(
				"../web-sites/" + id + "-web/docroot/WEB-INF/nfc/nfc.conf");

			FileUtil.write(nfcConfFile, nfcConf);
		}
	}

	private String _portalExtProperties = null;
	private String _orionConfigDir = null;
	private int _nfcListenPort = 7777;

}