package com.dotmarketing.plugin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PluginUtil {

	public static Logger logger = Logger.getLogger(PluginUtil.class);
	
	public static final String SYSTEM_PLUGIN_ORDER_KEY = "pluginOrderKey";

	public static Map<String, Object> parsePluginXML(File pluginXML) throws RuntimeException {
		logger.debug("Starting ParsePluginXML");
		Map<String, Object> result = new HashMap<String, Object>();
		List<String> pluginIds = new ArrayList<String>();
		if (!pluginXML.exists()) {
			return result;
		}
		logger.debug("Building DOM Doc Factory");
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		Document doc = null;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(e.getMessage(), e);
		}
		try {
			doc = docBuilder.parse(pluginXML);
		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		doc.getDocumentElement().normalize();
		logger.debug("Looking for plugin element");
		NodeList listOfPlugins = doc.getElementsByTagName("plugin");
		for (int p = 0; p < listOfPlugins.getLength(); p++) {
			Node plugin = listOfPlugins.item(p);
			if (plugin.getNodeType() == Node.ELEMENT_NODE) {
				Element firstPersonElement = (Element) plugin;
				NodeList pluginIdList = firstPersonElement
						.getElementsByTagName("id");
				if(pluginIdList != null && pluginIdList.getLength()>0){
					Element pluginIdElement = (Element) pluginIdList.item(0);
					NodeList textFNList = pluginIdElement.getChildNodes();
					if(textFNList!=null && textFNList.getLength()>0){
						pluginIds.add(((Node) textFNList.item(0)).getNodeValue().trim());
					}
				}
			}
		}
		result.put(SYSTEM_PLUGIN_ORDER_KEY, pluginIds);
		return result;
	}

	/**
	 * Get the jars in the proper order according to the plugin.xml
	 * 
	 * @return
	 */
	public static List<File> getPluginJars(String rootPath, String pluginPath) {
		logger.debug("Getting JAR list for:" + pluginPath);
		File libDir = new File(pluginPath);
		// Get list of plugins
		List<File> result = new ArrayList<File>();
		if (libDir == null) {
			return result;
		}
		logger.debug("Loading files using filter from filesystem");
		File[] files = libDir.listFiles(new PluginFilter());
		if (files == null) {
			return result;
		}
		logger.debug("Sorting JAR list");
		Arrays.sort(files, new PluginJarComparator(new File(rootPath + File.separator + "WEB-INF" + File.separator + "plugins.xml")));
		result = Arrays.asList(files);
		return result;
	}
	
	public static String getTextData(String fileName, JarFile jar) throws IOException {
		JarEntry entry=jar.getJarEntry(fileName);
		if (entry==null) {
			return null;
		}
		InputStream input=jar.getInputStream(entry);
		StringBuffer buf=new StringBuffer();
		 InputStreamReader isr = 
		      new InputStreamReader(input);
		       BufferedReader reader = new BufferedReader(isr);
		       String line;
		       while ((line = reader.readLine()) != null) {
		         buf.append(line);
		         buf.append("\n");
		       }
		       reader.close();
		       return buf.toString();

	}
	
	public static String[] listFiles(String filterRegEx, JarFile jar) throws IOException {
		Enumeration<JarEntry> entries = jar.entries();
		if (entries==null) {
			return new String[0];
		}
		List<String> entriesRet = new LinkedList<String>();
		while(entries.hasMoreElements()) {
			JarEntry entry  = entries.nextElement();

			if(entry.getName().matches(filterRegEx)) {
				entriesRet.add(entry.getName());
			}
		}
		return entriesRet.toArray(new String[0]);
	}
	
	public static String getPluginNameFromJar(String jarName){
		String name=jarName.substring(7, jarName.length()-4);
		return name;
	}
	
	public static Map<String, String> loadPropertiesFromFile(JarFile jar) throws IOException{
		Map<String, String> result = new HashMap<String, String>();
		Properties props = new Properties();
		JarEntry entry = jar.getJarEntry("conf/plugin.properties");
		if (entry != null ) {
			InputStream in = jar.getInputStream(entry);
			props.load(in);
			Enumeration<?> en = props.propertyNames();
			while (en.hasMoreElements()) {
				String key =  en.nextElement().toString().trim();
				result.put(key, props.getProperty(key, ""));
			}
		}
		return result;
	}
	
	public static Map<String, String> loadContollerPropertiesFromFile(JarFile jar) throws IOException{
		Map<String, String> result = new HashMap<String, String>();
		Properties props = new Properties();
		JarEntry entry = jar.getJarEntry("conf/plugin-controller.properties");
		if (entry != null ) {
			InputStream in = jar.getInputStream(entry);
			props.load(in);
			Enumeration<?> en = props.propertyNames();
			while (en.hasMoreElements()) {
				String key =  en.nextElement().toString().trim();
				result.put(key, props.getProperty(key, ""));
			}
		}
		return result;
	}
	
	public static String escapeForRegex(String s) {
		String ret;
		ret=s.replace("\\","\\\\");
		ret=ret.replace("*", "\\*");
		ret=ret.replace(".","\\." );
		ret=ret.replace("-","\\-" );
		
		return ret;
	}
		
}
