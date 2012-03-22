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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.dotmarketing.util.Logger;
import com.liferay.portal.util.EntityResolver;
import com.liferay.util.FileUtil;

/**
 * <a href="EJBXMLBuilder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.34 $
 *
 */
public class EJBXMLBuilder {

	public static void main(String[] args) {
		if (args.length == 1) {
			new EJBXMLBuilder(args[0]);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public EJBXMLBuilder(String jarFileName) {
		_jarFileName = jarFileName;

		try {
			_buildBorlandXML();
			_buildJOnASXML();
			_buildJRunXML();
			_buildPramatiXML();
			_buildRexIPXML();
			_buildSunXML();
			_buildWebLogicXML();
			//_buildWebSphereXML();
			_updateEJBXML();
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	private void _buildBorlandXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE ejb-jar PUBLIC \"-//Borland Software Corporation//DTD Enterprise JavaBeans 2.0//EN\" \"http://www.borland.com/devsupport/appserver/dtds/ejb-jar_2_0-borland.dtd\">\n");

		sb.append("\n<ejb-jar>\n");
		sb.append("\t<enterprise-beans>\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			sb.append("\t\t<session>\n");
			sb.append("\t\t\t<ejb-name>").append(entity.elementText("ejb-name")).append("</ejb-name>\n");

			if (entity.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("\t\t\t<bean-local-home-name>ejb/liferay/").append(entity.elementText("display-name")).append("Home</bean-local-home-name>\n");
			}
			else {
				sb.append("\t\t\t<bean-home-name>").append(entity.elementText("ejb-name")).append("</bean-home-name>\n");
			}

			sb.append("\t\t</session>\n");
		}

		sb.append("\t</enterprise-beans>\n");
		sb.append("\t<property>\n");
		sb.append("\t\t<prop-name>ejb.default_transaction_attribute</prop-name>\n");
		sb.append("\t\t<prop-type>String</prop-type>\n");
		sb.append("\t\t<prop-value>Supports</prop-value>\n");
		sb.append("\t</property>\n");
		sb.append("</ejb-jar>");

		File outputFile = new File("classes/META-INF/ejb-borland.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(this,outputFile.toString());
		}
	}

	private void _buildJOnASXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE jonas-ejb-jar PUBLIC \"-//ObjectWeb//DTD JOnAS 3.2//EN\" \"http://www.objectweb.org/jonas/dtds/jonas-ejb-jar_3_2.dtd\">\n");

		sb.append("\n<jonas-ejb-jar>\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			sb.append("\t<jonas-session>\n");
			sb.append("\t\t<ejb-name>").append(entity.elementText("ejb-name")).append("</ejb-name>\n");

			if (entity.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("\t\t<jndi-name>ejb/liferay/").append(entity.elementText("display-name")).append("Home</jndi-name>\n");
			}
			else {
				sb.append("\t\t<jndi-name>").append(entity.elementText("ejb-name")).append("</jndi-name>\n");
			}

			sb.append("\t</jonas-session>\n");
		}

		sb.append("</jonas-ejb-jar>");

		File outputFile = new File("classes/META-INF/jonas-ejb-jar.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(this, outputFile.toString());
		}
	}

	private void _buildJRunXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE jrun-ejb-jar PUBLIC \"-//Macromedia, Inc.//DTD jrun-ejb-jar 4.0//EN\" \"http://jrun.macromedia.com/dtds/jrun-ejb-jar.dtd\">\n");

		sb.append("\n<jrun-ejb-jar>\n");
		sb.append("\t<enterprise-beans>\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			sb.append("\t\t<session>\n");
			sb.append("\t\t\t<ejb-name>").append(entity.elementText("ejb-name")).append("</ejb-name>\n");

			if (entity.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("\t\t\t<local-jndi-name>ejb/liferay/").append(entity.elementText("display-name")).append("Home</local-jndi-name>\n");
			}
			else {
				sb.append("\t\t\t<jndi-name>").append(entity.elementText("ejb-name")).append("</jndi-name>\n");
			}

			sb.append("\t\t\t<cluster-home>false</cluster-home>\n");
			sb.append("\t\t\t<cluster-object>false</cluster-object>\n");
			sb.append("\t\t\t<timeout>3000</timeout>\n");
			sb.append("\t\t</session>\n");
		}

		sb.append("\t</enterprise-beans>\n");
		sb.append("</jrun-ejb-jar>");

		File outputFile = new File("classes/META-INF/jrun-ejb-jar.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(EJBXMLBuilder.class, outputFile.toString());
		}
	}

	private void _buildPramatiXML() throws Exception {
		Map tableNames = new HashMap();

		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE pramati-j2ee-server PUBLIC \"-//Pramati Technologies //DTD Pramati J2ee Server 3.5 SP5//EN\" \"http://www.pramati.com/dtd/pramati-j2ee-server_3_5.dtd\">\n");

		sb.append("\n<pramati-j2ee-server>\n");
		sb.append("\t<vhost-name>default</vhost-name>\n");
		sb.append("\t<auto-start>TRUE</auto-start>\n");
		sb.append("\t<realm-name />\n");
		sb.append("\t<ejb-module>\n");
		sb.append("\t\t<name>").append(_jarFileName).append("</name>\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			sb.append("\t\t<ejb>\n");
			sb.append("\t\t\t<name>").append(entity.elementText("ejb-name")).append("</name>\n");
			sb.append("\t\t\t<max-pool-size>40</max-pool-size>\n");
			sb.append("\t\t\t<min-pool-size>20</min-pool-size>\n");
			sb.append("\t\t\t<enable-freepool>false</enable-freepool>\n");
			sb.append("\t\t\t<pool-waittimeout-millis>60000</pool-waittimeout-millis>\n");

			sb.append("\t\t\t<low-activity-interval>20</low-activity-interval>\n");
			sb.append("\t\t\t<is-secure>false</is-secure>\n");
			sb.append("\t\t\t<is-clustered>true</is-clustered>\n");

			if (entity.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("\t\t\t<jndi-name>ejb/liferay/").append(entity.elementText("display-name")).append("Home</jndi-name>\n");
			}
			else {
				sb.append("\t\t\t<jndi-name>").append(entity.elementText("ejb-name")).append("</jndi-name>\n");
			}

			sb.append("\t\t\t<local-jndi-name>").append(entity.elementText("ejb-name")).append("__PRAMATI_LOCAL").append("</local-jndi-name>\n");

			sb.append(_buildPramatiXMLRefs(entity));

			sb.append("\t\t</ejb>\n");
		}

		sb.append("\t</ejb-module>\n");
		sb.append("</pramati-j2ee-server>");

		File outputFile = new File("classes/pramati-j2ee-server.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(EJBXMLBuilder.class,outputFile.toString());
		}
	}

	private String _buildPramatiXMLRefs(Element entity) {
		StringBuffer sb = new StringBuffer();

		Iterator itr = entity.elements("ejb-local-ref").iterator();

		while (itr.hasNext()) {
			Element ejbRef = (Element)itr.next();

			sb.append("\t\t\t<ejb-local-ref>\n");
			sb.append("\t\t\t\t<ejb-ref-name>").append(ejbRef.elementText("ejb-ref-name")).append("</ejb-ref-name>\n");
			sb.append("\t\t\t\t<ejb-link>").append(ejbRef.elementText("ejb-ref-name")).append("__PRAMATI_LOCAL").append("</ejb-link>\n");
			sb.append("\t\t\t</ejb-local-ref>\n");
		}

		return sb.toString();
	}

	private void _buildRexIPXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");

		sb.append("\n<rexip-ejb-jar>\n");
		sb.append("\t<enterprise-beans>\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			sb.append("\t\t<session>\n");
			sb.append("\t\t\t<ejb-name>").append(entity.elementText("ejb-name")).append("</ejb-name>\n");

			if (entity.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("\t\t\t<local-jndi-name>ejb/liferay/").append(entity.elementText("display-name")).append("Home</local-jndi-name>\n");
			}
			else {
				sb.append("\t\t\t<jndi-name>").append(entity.elementText("ejb-name")).append("</jndi-name>\n");
			}

			sb.append("\t\t\t<clustered>true</clustered>\n");
			sb.append("\t\t\t<pool-size>20</pool-size>\n");
			sb.append("\t\t\t<cache-size>20</cache-size>\n");
			sb.append("\t\t</session>\n");
		}

		sb.append("\t</enterprise-beans>\n");
		sb.append("</rexip-ejb-jar>");

		File outputFile = new File("classes/META-INF/rexip-ejb-jar.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(EJBXMLBuilder.class,outputFile.toString());
		}
	}

	private void _buildSunXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE sun-ejb-jar PUBLIC \"-//Sun Microsystems, Inc.//DTD Sun ONE Application Server 7.0 EJB 2.0//EN\" \"http://www.sun.com/software/sunone/appserver/dtds/sun-ejb-jar_2_0-0.dtd\">\n");

		sb.append("\n<sun-ejb-jar>\n");
		sb.append("\t<enterprise-beans>\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			sb.append("\t\t<ejb>\n");
			sb.append("\t\t\t<ejb-name>").append(entity.elementText("ejb-name")).append("</ejb-name>\n");

			if (entity.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("\t\t\t<jndi-name>ejb/liferay/").append(entity.elementText("display-name")).append("Home</jndi-name>\n");
			}
			else {
				sb.append("\t\t\t<jndi-name>").append(entity.elementText("ejb-name")).append("</jndi-name>\n");
			}

			sb.append("\t\t\t<bean-pool>\n");
			sb.append("\t\t\t\t<steady-pool-size>0</steady-pool-size>\n");
			sb.append("\t\t\t\t<resize-quantity>60</resize-quantity>\n");
			sb.append("\t\t\t\t<max-pool-size>60</max-pool-size>\n");
			sb.append("\t\t\t\t<pool-idle-timeout-in-seconds>900</pool-idle-timeout-in-seconds>\n");
			sb.append("\t\t\t</bean-pool>\n");
			sb.append("\t\t</ejb>\n");
		}

		sb.append("\t</enterprise-beans>\n");
		sb.append("</sun-ejb-jar>");

		File outputFile = new File("classes/META-INF/sun-ejb-jar.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(EJBXMLBuilder.class,outputFile.toString());
		}
	}

	private void _buildWebLogicXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE weblogic-ejb-jar PUBLIC \"-//BEA Systems, Inc.//DTD WebLogic 7.0.0 EJB//EN\" \"http://www.bea.com/servers/wls700/dtd/weblogic-ejb-jar.dtd\">\n");

		sb.append("\n<weblogic-ejb-jar>\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			sb.append("\t<weblogic-enterprise-bean>\n");
			sb.append("\t\t<ejb-name>").append(entity.elementText("ejb-name")).append("</ejb-name>\n");

			if (entity.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("\t\t<local-jndi-name>ejb/liferay/").append(entity.elementText("display-name")).append("Home</local-jndi-name>\n");
			}
			else {
				sb.append("\t\t<jndi-name>").append(entity.elementText("ejb-name")).append("</jndi-name>\n");
			}

			sb.append("\t</weblogic-enterprise-bean>\n");
		}

		sb.append("</weblogic-ejb-jar>");

		File outputFile = new File("classes/META-INF/weblogic-ejb-jar.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(EJBXMLBuilder.class,outputFile.toString());
		}
	}

	private void _buildWebSphereXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");

		sb.append("\n<com.ibm.ejs.models.base.bindings.ejbbnd:EJBJarBinding xmlns:com.ibm.ejs.models.base.bindings.ejbbnd=\"ejbbnd.xmi\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:com.ibm.ejs.models.base.bindings.commonbnd=\"commonbnd.xmi\" xmlns:com.ibm.etools.ejb=\"ejb.xmi\" xmlns:com.ibm.etools.j2ee.common=\"common.xmi\" xmi:version=\"2.0\" xmi:id=\"ejb-jar_ID_Bnd\" currentBackendId=\"CLOUDSCAPE_V50_1\">\n");
		sb.append("\t<ejbJar href=\"META-INF/ejb-jar.xml#ejb-jar_ID\" />\n");

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		int sessionCount = 0;
		int ejbLocalRefCount = 0;

		Iterator itr1 = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr1.hasNext()) {
			Element sessionEl = (Element)itr1.next();

			sb.append("\t<ejbBindings xmi:id=\"Session_").append(++sessionCount).append("_Bnd\" jndiName=\"");

			if (sessionEl.elementText("display-name").endsWith("LocalManagerEJB")) {
				sb.append("ejb/liferay/").append(sessionEl.elementText("display-name")).append("Home");
			}
			else {
				sb.append(sessionEl.elementText("ejb-name"));
			}

			sb.append("\">\n");

			sb.append("\t\t<enterpriseBean xmi:type=\"com.ibm.etools.ejb:Session\" href=\"META-INF/ejb-jar.xml#Session_").append(sessionCount).append("\" />\n");

			Iterator itr2 = sessionEl.elements("ejb-local-ref").iterator();

			while (itr2.hasNext()) {
				Element ejbLocalRefEl = (Element)itr2.next();

				sb.append("\t\t<ejbRefBindings xmi:id=\"EjbRefBinding_").append(++ejbLocalRefCount).append("\" jndiName=\"").append(ejbLocalRefEl.elementText("ejb-ref-name")).append("\">\n");
				sb.append("\t\t\t<bindingEjbRef xmi:type=\"com.ibm.etools.j2ee.common:EJBLocalRef\" href=\"META-INF/ejb-jar.xml#EjbRef_").append(ejbLocalRefCount).append("\" />\n");
				sb.append("\t\t</ejbRefBindings>\n");
			}

			sb.append("\t</ejbBindings>\n");
		}

		sb.append("</com.ibm.ejs.models.base.bindings.ejbbnd:EJBJarBinding>");

		File outputFile = new File("classes/META-INF/ibm-ejb-jar-bnd.xmi");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			Logger.info(EJBXMLBuilder.class,outputFile.toString());
		}
	}

	private void _updateEJBXML() throws Exception {
		File xmlFile = new File("classes/META-INF/ejb-jar.xml");

		StringBuffer methodsSB = new StringBuffer();

		SAXReader reader = new SAXReader();
		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(xmlFile);

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("entity").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			methodsSB.append("\t\t\t<method>\n");
			methodsSB.append("\t\t\t\t<ejb-name>" + entity.elementText("ejb-name") + "</ejb-name>\n");
			methodsSB.append("\t\t\t\t<method-name>*</method-name>\n");
			methodsSB.append("\t\t\t</method>\n");
		}

		itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			methodsSB.append("\t\t\t<method>\n");
			methodsSB.append("\t\t\t\t<ejb-name>" + entity.elementText("ejb-name") + "</ejb-name>\n");
			methodsSB.append("\t\t\t\t<method-name>*</method-name>\n");
			methodsSB.append("\t\t\t</method>\n");
		}

		StringBuffer sb = new StringBuffer();

		sb.append("\t<assembly-descriptor>\n");
		sb.append("\t\t<method-permission>\n");
		sb.append("\t\t\t<unchecked />\n");
		sb.append(methodsSB);
		sb.append("\t\t</method-permission>\n");
		sb.append("\t\t<container-transaction>\n");
		sb.append(methodsSB);
		sb.append("\t\t\t<trans-attribute>Required</trans-attribute>\n");
		sb.append("\t\t</container-transaction>\n");
		sb.append("\t</assembly-descriptor>\n");

		String content = FileUtil.read(xmlFile);

		int x = content.indexOf("<assembly-descriptor>") - 1;
		int y = content.indexOf("</assembly-descriptor>", x) + 23;

		if (x < 0) {
			x = content.indexOf("</ejb-jar>");
			y = x;
		}

		String newContent =
			content.substring(0, x) + sb.toString() +
			content.substring(y, content.length());

		if (!content.equals(newContent)) {
			FileUtil.write(xmlFile, newContent);
		}
	}

	private String _jarFileName;

}