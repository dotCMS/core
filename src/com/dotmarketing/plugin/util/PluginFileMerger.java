package com.dotmarketing.plugin.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;

public class PluginFileMerger {
	String rootPath;
	private String name;
	private static Logger logger=Logger.getLogger(PluginFileMerger.class);
	boolean deployAppClassLoader = false;
	
	public PluginFileMerger () {
		
	}
	
	public void undeploy(String rootPath, String pluginPath) {
		this.rootPath = rootPath;
		logger.debug("Starting undeploy");
		List<File> plugins = PluginUtil.getPluginJars(rootPath, pluginPath);
		for (File file : plugins) {
			try {
				undeployPlugin(file);
			} catch (IOException e) {
				logger.debug("IOException: "
						+ e.getMessage(), e);
			}

		}
		
		logger.debug("Deleting files from WEB-INF/classes");
		File classesDir = new File(rootPath + File.separator + "WEB-INF" + File.separator + "classes" + File.separator);
		deleteRecursive(classesDir);
	}
	
	private static void deleteRecursive(File dir){
		File[] files = dir.listFiles();
		if (files != null) {
			for(File file : files){
				if(file.exists()&& !file.getName().endsWith(".svn")){
					if(file.isDirectory()){
						deleteRecursive(file);
						file.delete();
					}else{
						file.delete();
					}
				}
			}
		}
	}

	public void mergePlugins(String rootPath, String pluginPath) {
		this.rootPath = rootPath;
		logger.debug("Starting mergePlugins");
		// this.rootPath = rootPath;
		List<File> plugins = PluginUtil.getPluginJars(rootPath, pluginPath);
		// Since order matter, and due to the way we override, we need to
		// iterate backward
		if (plugins != null) {
			logger.debug("Loading " + plugins.size() + " plugins");
			for (int i = plugins.size(); i > 0; i--) {
				File plugin = plugins.get(i - 1);
				logger.debug("Loading plugin at: "
						+ plugin.getAbsolutePath());
				try {
					mergePlugin(plugin);
					logger.debug("Loaded plugin at: "
							+ plugin.getAbsolutePath());
				} catch (IOException e) {
					logger.error("IOException: "
							+ e.getMessage() + " while loading: "
							+ plugin.getAbsolutePath(), e);
					throw new BuildException(e);
				}

			}
		} else {
			logger.debug("No plugins found");
		}

	}

	public boolean merge(File file, String targetCommentBegin,
			String targetCommentEnd, String startComment, String endComment,
			String mergeText) throws IOException {
		if (!file.exists()) {
			logger.error("File doesn't exist: " + file.getAbsolutePath());
			return false;
		}
		if ((mergeText == null) || (mergeText.length() < 1)) {
			logger.info("Nothing to merge into: "
					+ file.getAbsolutePath());
			return true;
		}
		logger.error("Merging file: " + file.getAbsolutePath());
		InputStream input = new FileInputStream(file);
		String buf = merge(input, targetCommentBegin, targetCommentEnd,
				startComment, endComment, mergeText);

		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(buf);
		out.close();
		return true;
	}

	public boolean merge(File file, String targetCommentBegin,
			String targetCommentEnd, String startComment, String endComment,
			String mergeText, Map<String, String> overrides,
			String overrideBegin, String overrideEnd, String override)
			throws IOException {
		if (!file.exists()) {
			logger.error("File doesn't exist: " + file.getAbsolutePath());
			return false;
		}
		if ((mergeText == null) || (mergeText.length() < 1)) {
			logger.info("Nothing to merge into: "
					+ file.getAbsolutePath());
			return true;
		}
		logger.error("Merging file: " + file.getAbsolutePath());
		InputStream input = new FileInputStream(file);
		String buf = mergeByKey(input, targetCommentBegin, targetCommentEnd,
				startComment, endComment, mergeText, overrides, overrideBegin,
				overrideEnd, override);

		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(buf);
		out.close();
		return true;
	}

	public boolean mergeByAttribute(File file, String targetCommentBegin,
			String targetCommentEnd, String startComment, String endComment,
			String mergeText, Map<String, String> overrides,
			String overrideBegin, String overrideEnd, String override)
			throws IOException {
		if (!file.exists()) {
			logger.error("File doesn't exist: " + file.getAbsolutePath());
			return false;
		}
		if ((mergeText == null) || (mergeText.length() < 1)) {
			logger.info("Nothing to merge into: "
					+ file.getAbsolutePath());
			return true;
		}
		logger.error("Merging file: " + file.getAbsolutePath());
		InputStream input = new FileInputStream(file);
		String buf = mergeByAttribute(input, targetCommentBegin,
				targetCommentEnd, startComment, endComment, mergeText,
				overrides, overrideBegin, overrideEnd, override);

		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(buf);
		out.close();
		return true;
	}

	public boolean merge(File file, String targetCommentBegin,
			String targetCommentEnd, String startComment, String endComment,
			String mergeText, String comment, String overrideBegin)
			throws IOException {
		if (!file.exists()) {
			logger.error("File doesn't exist: " + file.getAbsolutePath());
			return false;
		}
		if ((mergeText == null) || (mergeText.length() < 1)) {
			logger.info("Nothing to merge into: "
					+ file.getAbsolutePath());
			return true;
		}
		logger.error("Merging file: " + file.getAbsolutePath());
		InputStream input = new FileInputStream(file);
		String buf = merge(input, targetCommentBegin, targetCommentEnd,
				startComment, endComment, mergeText, comment, overrideBegin);

		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(buf);
		out.close();
		return true;
	}

	public void undeployPlugin(File plugin) throws IOException {
		name = PluginUtil.getPluginNameFromJar(plugin.getName());
		logger.debug("Plugin name: " + name);
		JarFile jar = new JarFile(plugin);

		// undeploy plugin

		removeFragmentXML(new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "toolbox.xml"), "<!-- BEGIN PLUGIN:" + name
				+ " -->", "<!-- END PLUGIN:" + name + " -->",
				"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name + " -->");

		PluginVelocityMerger.removeFragments(rootPath, name);

		removeFragmentXML(new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "dwr.xml"), "<!-- BEGIN PLUGIN:" + name
				+ " -->", "<!-- END PLUGIN:" + name + " -->",
				"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name + " -->");

		List<String> entities = new ArrayList<String>();
		entities.add("servlet");
		entities.add("servlet-mapping");
		entities.add("filter");
		entities.add("filter-mapping");
		entities.add("context-param");
		entities.add("listener");
		entities.add("security-constraint");

		File webXML= new File(rootPath + File.separator	+ "WEB-INF/web.xml");
		for (String entity : entities) {
			String startSection = "<!-- BEGIN PLUGIN-" + entity.toUpperCase()
					+ ":" + name + " -->";
			String endSection = "<!-- END PLUGIN-" + entity.toUpperCase() + ":"
					+ name + " -->";
			removeFragmentXML(webXML, startSection, endSection,
					"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name
							+ " -->");
		}
		removeFragmentXML(webXML, "<!-- BEGIN PLUGIN:"+name+" -->", "<!-- END PLUGIN:"+name+" -->", null, null);

		File strutsCMS = new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "struts-cms.xml");
		File strutsConfig = new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "struts-config-ext.xml");
		entities = new ArrayList<String>();
		entities.add("form-bean");
		entities.add("action");
		entities.add("plug-in");
		for (String entity : entities) {
			String startSection = "<!-- BEGIN PLUGIN-" + entity.toUpperCase()
					+ ":" + name + " -->";
			String endSection = "<!-- END PLUGIN-" + entity.toUpperCase() + ":"
					+ name + " -->";

			new File(rootPath + File.separator + "WEB-INF" + File.separator
					+ "struts-cms.xml");
			new File(rootPath + File.separator + "WEB-INF" + File.separator
					+ "struts-config-ext.xml");

			removeFragmentXML(strutsCMS, startSection, endSection,
					"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name
							+ " -->");
			removeFragmentXML(strutsConfig, startSection, endSection,
					"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name
							+ " -->");
		}

		removePropertiesFragment(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "classes" + File.separator + "portal-ext.properties"), "## BEGIN PLUGIN:"
				+ name, "## END PLUGIN:" + name, "#", "## OVERRIDE:" + name);

		removePropertiesFragment(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "classes" + File.separator + "system-ext.properties"), "## BEGIN PLUGIN:"
				+ name, "## END PLUGIN:" + name, "#", "## OVERRIDE:" + name);

		removeFragmentXML(new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "portlet-ext.xml"), "<!-- BEGIN PLUGIN:"
				+ name + " -->", "<!-- END PLUGIN:" + name + " -->",
				"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name + " -->");
		
		removeFragmentXML(new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "urlrewrite.xml"), "<!-- BEGIN PLUGIN:"
				+ name + " -->", "<!-- END PLUGIN:" + name + " -->",
				"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name + " -->");
		
		removeFragmentXML(new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "liferay-portlet-ext.xml"),
				"<!-- BEGIN PLUGIN:" + name + " -->", "<!-- END PLUGIN:" + name
						+ " -->", "<!-- BEGIN OVERRIDE:" + name,
				" END OVERRIDE:" + name + " -->");

		removeFragmentXML(new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "tiles-defs-ext.xml"), "<!-- BEGIN PLUGIN:"
				+ name + " -->", "<!-- END PLUGIN:" + name + " -->",
				"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name + " -->");

		//removeFragmentXML(new File(rootPath + File.separator + "html"
		//		+ File.separator + "portlet" + File.separator + "ext"
		//		+ File.separator + "contentlet" + File.separator
		//		+ "macro_help.html"), "<!-- BEGIN PLUGIN LI:" + name + " -->",
		//		"<!-- END PLUGIN LI:" + name + " -->", null, null);

		//removeFragmentXML(new File(rootPath + File.separator + "html"
		//		+ File.separator + "portlet" + File.separator + "ext"
		//		+ File.separator + "contentlet" + File.separator
		//		+ "macro_help.html"), "<!-- BEGIN PLUGIN DOC:" + name + " -->",
		//		"<!-- END PLUGIN DOC:" + name + " -->", null, null);

		removePropertiesFragment(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "messages" + File.separator
				+ "Language-ext.properties"), "## BEGIN PLUGIN:" + name,
				"## END PLUGIN:" + name, "#", "## OVERRIDE:" + name);
		removePropertiesFragment(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "classes" + File.separator + "dotmarketing-config.properties"),
				"## BEGIN PLUGIN:" + name, "## END PLUGIN:" + name, "#",
				"## OVERRIDE:" + name);
		
		removeLanguageProperties(jar);
		
		removeFiles();
	}

	private void removeLanguageProperties(JarFile jar) throws IOException {
		
		String[] languageFiles = PluginUtil.listFiles("(conf/Language-ext(.*).properties)", jar);

		Pattern langFilePattern = Pattern.compile("conf/(Language-ext(.*).properties)");
		for(String languageFileEntry : languageFiles) {
			Matcher mt = langFilePattern.matcher(languageFileEntry);
			if(mt.matches()) {
				String fileName = isSet(mt.group(1))?mt.group(1):mt.group(2);
				String languageFilePath = rootPath + File.separator + "WEB-INF" + File.separator
					+ "messages" + File.separator
					+ fileName;
				File languageFile = new File(languageFilePath);
				PrintWriter langFW = null;
				try {
					if(!languageFile.exists()) {
						languageFile.createNewFile();
						langFW = new PrintWriter(languageFile);
						langFW.println("## BEGIN PLUGINS");
						langFW.println("## END PLUGINS");
						langFW.flush();
						langFW.close();
					}

					removePropertiesFragment(languageFile,
							"## BEGIN PLUGIN:" + name, "## END PLUGIN:" + name, "#",
							"## OVERRIDE:" + name);

				} finally {
					if(langFW != null)
						langFW.close();
				}
				
			}
		}		
	}

	public void removeFiles() {
		try{
			String staticDest = rootPath + File.separator + "html" + File.separator
					+ "plugins" + File.separator + name;
			String velocityDest = rootPath + File.separator + "WEB-INF"
					+ File.separator + "velocity" + File.separator + "static"
					+ File.separator + "plugins" + File.separator + name;
			String libDest = rootPath + File.separator + "WEB-INF"
			+ File.separator + "lib";
			String jspDest = rootPath + File.separator + "html" + File.separator + "plugins" + File.separator
			+ name;
			//http://jira.dotmarketing.net/browse/DOTCMS-5634
			String classDest = rootPath + File.separator + "WEB-INF" + File.separator + "classes" + File.separator;
			File plugin = new File(libDest + File.separator + "plugin-"+name+".jar");
			JarFile jar;
			try {
				jar = new JarFile(plugin);
			} catch (IOException e1) {
				logger.info("Jar not found : " + e1.getMessage());
				return;
			}
			if(jar!=null){
				Enumeration<JarEntry> e = jar.entries();
				while (e != null && e.hasMoreElements()) {
					JarEntry entry = e.nextElement();
					if (entry != null && !entry.isDirectory()&& entry.getName().endsWith(".class")) {
				       String path = entry.getName().substring(0, entry.getName().lastIndexOf("/"));
				       String prefix = entry.getName().substring(entry.getName().lastIndexOf("/")+1,entry.getName().indexOf("."));
				       String fullPath = classDest + path;
					   deleteFiles(fullPath,prefix,".class");
					   //to delete empty directories.
					   int size = path.split("/").length;
					   for(int j=size;j>0;j--){
							File dir = new File(fullPath);
							 if (dir.exists() && dir.listFiles().length == 0) 
							   dir.delete();
							fullPath = fullPath.substring(0, fullPath.lastIndexOf("/"));
					   }
			        }
				}	
			}
			
			deleteDirectory(new File(staticDest));
			deleteDirectory(new File(velocityDest));
			deleteDirectory(new File(jspDest));
			deleteFiles(libDest,"pluginlib-"+name+"-",".jar");
			
			//Look for any TinyMCE plugins
			String tinyMCEDest = rootPath + File.separator + "html" + File.separator  + "js" + File.separator  + "tinymce" + File.separator +   "jscripts" + File.separator  + "tiny_mce" + File.separator  +  File.separator
			+ "plugins" + File.separator;
			final String prefix= "plugin_"+name+"_";
			FileFilter filter=new FileFilter() {
	
				public boolean accept(File pathname) {
					if (pathname.getName().startsWith(prefix)) {
						return true;
					}
					return false;
				}
				
			};
			File tinyMCEDir=new File(tinyMCEDest);
			File[] tinyMCEPlugins=tinyMCEDir.listFiles(filter);
			for (File tinyMCEPlugin :tinyMCEPlugins ) {
				deleteDirectory(tinyMCEPlugin);
			}
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private boolean deleteFiles(String directory,String prefix,String postfix) {
		File dir=new File(directory);
		final String fPrefix=prefix;
		//final String fPostfix=postfix;
		String[] list=dir.list(new FilenameFilter(){

			public boolean accept(File arg0, String arg1) {
				if (arg1.startsWith(fPrefix)) {
					return true;
				}
				return false;
			}
			
		});
		if(list!= null){
			for (String item:list) {
				logger.debug("File to delete: " + item);
				File f = new File(directory+File.separator+item);
				if(f.exists()){
					f.delete();
				}
			}
		}
		
		return true;
	}

	private boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	public void mergePlugin(File plugin) throws IOException {

		name = PluginUtil.getPluginNameFromJar(plugin.getName());
		logger.debug("Plugin name: " + name);
		JarFile jar = new JarFile(plugin);

		// Install new plugins

		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "key");
		String toolbox = PluginUtil.getTextData("conf/toolbox-ext.xml", jar);
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "toolbox.xml"), "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", toolbox, overrideMap,
				"<!-- BEGIN OVERRIDE:" + name,
				" END OVERRIDE:" + name + " -->", "<!-- BEGIN OVERRIDE");

		String macro = PluginUtil.getTextData("conf/macros-ext.vm", jar);
		PluginVelocityMerger.mergeVelocityFile(rootPath, macro, name);

		String dwr = PluginUtil.getTextData("conf/dwr-ext.xml", jar);
		overrideMap = new HashMap<String, String>();
		overrideMap.put("create", "javascript");
		mergeByAttribute(new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "dwr.xml"), "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", dwr, overrideMap,
				"<!-- BEGIN OVERRIDE:" + name,
				" END OVERRIDE:" + name + " -->", "<!-- BEGIN OVERRIDE");

		String web = PluginUtil.getTextData("conf/web-ext.xml", jar);
		mergeWebXML(web,
				new File(rootPath + File.separator + "WEB-INF/web.xml"));
		
		String genWeb = PluginUtil.getTextData("conf/generated_web.xml", jar);
		if (genWeb!=null) {
		genWeb=genWeb.replaceAll("<url-pattern>/", "<url-pattern>/plugins/"+name+"/");
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "web.xml"), "<!-- BEGIN JSPS -->",
				"<!-- END JSPS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", genWeb);
		}
	
		String portal = PluginUtil.getTextData("conf/portal-ext.properties",
				jar);
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "classes" + File.separator + "portal-ext.properties"), "## BEGIN PLUGINS",
				"## END PLUGINS", "## BEGIN PLUGIN:" + name, "## END PLUGIN:"
						+ name, portal, "#", "## OVERRIDE:" + name);

		String system = PluginUtil.getTextData("conf/system-ext.properties",
				jar);
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "classes" + File.separator + "system-ext.properties"), "## BEGIN PLUGINS",
				"## END PLUGINS", "## BEGIN PLUGIN:" + name, "## END PLUGIN:"
						+ name, system, "#", "## OVERRIDE:" + name);

		String portlet = PluginUtil.getTextData("conf/portlet-ext.xml", jar);
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "portlet-ext.xml"), "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", portlet);
		
		String urlrewrite = PluginUtil.getTextData("conf/urlrewrite-ext.xml", jar);
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "urlrewrite.xml"), "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", urlrewrite);

		String liferay_portlet = PluginUtil.getTextData(
				"conf/liferay-portlet-ext.xml", jar);
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "liferay-portlet-ext.xml"), "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", liferay_portlet);

		String struts_cms = PluginUtil.getTextData("conf/struts-cms-ext.xml", jar);
		mergeStrutsXML(struts_cms, new File(rootPath + File.separator
				+ "WEB-INF" + File.separator + "struts-cms.xml"));

		String struts_config_ext = PluginUtil.getTextData(
				"conf/struts-config-ext.xml", jar);
		mergeStrutsXML(struts_config_ext, new File(rootPath + File.separator
				+ "WEB-INF" + File.separator + "struts-config-ext.xml"));

		String tiles_defs_ext = PluginUtil.getTextData(
				"conf/tiles-defs-ext.xml", jar);
		overrideMap = new HashMap<String, String>();
		overrideMap.put("definition", "name");
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "tiles-defs-ext.xml"), "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", tiles_defs_ext,
				overrideMap, "<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:"
						+ name + " -->", "<!-- BEGIN OVERRIDE");

		//String macrohelp_li = PluginUtil.getTextData("doc/macro_help_li.html",
		//		jar);
		//merge(new File(rootPath + File.separator + "html" + File.separator
		//		+ "portlet" + File.separator + "ext" + File.separator
		//		+ "contentlet" + File.separator + "macro_help.html"),
		//		"<!-- BEGIN LI PLUGINS -->", "<!-- END LI PLUGINS -->",
		//		"<!-- BEGIN PLUGIN LI:" + name + " -->", "<!-- END PLUGIN LI:"
		//				+ name + " -->", macrohelp_li);

		//String macrohelp_doc = PluginUtil.getTextData(
		//		"doc/macro_help_doc.html", jar);
		//merge(new File(rootPath + File.separator + "html" + File.separator
		//		+ "portlet" + File.separator + "ext" + File.separator
		//		+ "contentlet" + File.separator + "macro_help.html"),
		//		"<!-- BEGIN DOC PLUGINS -->", "<!-- END DOC PLUGINS -->",
		//		"<!-- BEGIN PLUGIN DOC:" + name + " -->",
		//		"<!-- END PLUGIN DOC:" + name + " -->", macrohelp_doc);

		String dotmarketing = PluginUtil.getTextData(
				"conf/dotmarketing-config-ext.properties", jar);
		merge(new File(rootPath + File.separator + "WEB-INF" + File.separator
				+ "classes" + File.separator + "dotmarketing-config.properties"), "## BEGIN PLUGINS",
				"## END PLUGINS", "## BEGIN PLUGIN:" + name, "## END PLUGIN:"
						+ name, dotmarketing, "#", "## OVERRIDE:" + name);

		//Merging plugin language files
		mergeLanguageFiles(jar);
		logger.info("Starting to copy files to file system");
		copyFiles(plugin,name);
		logger.info("Finished to copy files to file system");
		
		//Copy tiny_mce plugins
		
	}

	private void mergeLanguageFiles(JarFile jar) throws IOException {
		
		String[] languageFiles = PluginUtil.listFiles("(conf/Language-ext(.*).properties)", jar);

		Pattern langFilePattern = Pattern.compile("conf/(Language-ext(.*).properties)");
		for(String languageFileEntry : languageFiles) {
			Matcher mt = langFilePattern.matcher(languageFileEntry);
			if(mt.matches()) {
				String fileName = isSet(mt.group(1))?mt.group(1):mt.group(2);
				String languageFilePath = rootPath + File.separator + "WEB-INF" + File.separator
				+ File.separator + "messages" + File.separator
					+ fileName;
				File languageFile = new File(languageFilePath);
				PrintWriter langFW = null;
				try {
					if(!languageFile.exists()) {
						languageFile.createNewFile();
						langFW = new PrintWriter(languageFile);
						langFW.println("## BEGIN PLUGINS");
						langFW.println("## END PLUGINS");
						langFW.flush();
						langFW.close();
					}

					String languageText = PluginUtil.getTextData(languageFileEntry, jar);

					merge(languageFile, "## BEGIN PLUGINS",
							"## END PLUGINS", "## BEGIN PLUGIN:" + name, "## END PLUGIN:"
									+ name, languageText, "#", "## OVERRIDE:" + name);

				} finally {
					if(langFW != null)
						langFW.close();
				}
				
			}
		}
		
	}

	private void copyFiles(File plugin,String name) throws IOException {
		String staticDest = rootPath + File.separator + "html" + File.separator
				+ "plugins" + File.separator + name;
		String velocityDest = rootPath + File.separator + "WEB-INF"
				+ File.separator + "velocity" + File.separator + "static"
				+ File.separator + "plugins" + File.separator + name;
		String libDest;
		JarFile jar = new JarFile(plugin);
		logger.info("The deployAppClassLoader parameter is " + deployAppClassLoader);
		libDest = rootPath + File.separator + "WEB-INF" + File.separator +  "lib";
		logger.info("The lib directry is " + libDest);
		String jspDest = rootPath + File.separator + "html" + File.separator + "plugins" + File.separator
				+ name;
		String tinyMCEDest = rootPath + File.separator + "html" + File.separator  + "js" + File.separator  + "tinymce" + File.separator +   "jscripts" + File.separator  + "tiny_mce" + File.separator  +  File.separator
		+ "plugins" + File.separator;
		String classDest=rootPath + File.separator + "WEB-INF" + File.separator + "classes" + File.separator;
		Enumeration<JarEntry> e = jar.entries();
		while (e.hasMoreElements()) {
			JarEntry entry = e.nextElement();
			if (!entry.isDirectory()) {
				if (entry.getName().startsWith("static_files/")) {
					// Extract it
					dropStaticFile(staticDest, entry, jar, null, true, true);

				}
				if (entry.getName().startsWith("static_velocity/")) {
					// Extract it
					dropStaticFile(velocityDest, entry, jar, null, true, true);

				}
				if (entry.getName().startsWith("lib/")) {
					// Extract it
					if(entry.getName().endsWith(".jar")){
						dropStaticFile(classDest, classDest, entry, jar, "pluginlib-"+name+"-", true, true);
					}else{
					    dropStaticFile(libDest, entry, jar, "pluginlib-"+name+"-", true, true);
					}
					
				}
				
				if (entry.getName().startsWith("jsp/")) {
					// Extract it
					dropStaticFile(jspDest, entry, jar, null, true, true);

				}
				if (entry.getName().startsWith("tiny_mce/")) {
					// Extract it					
					// Use _ instead of -, otherwise js breaks
					dropStaticFile(tinyMCEDest, entry, jar, "plugin_"+name+"_", false, true);

				}
				if (entry.getName().endsWith(".class") ) {
					dropStaticFile(classDest,entry,jar,null,false, false);
				}
				}

		}
	}

	public static void dropStaticFile(String dest, ZipEntry entry, JarFile jar,
			String prefix, boolean prefixFile, boolean stripParentDir) throws IOException {
		if (prefix == null) {
			prefix = "";
		}
		
		String path = entry.getName();
		
		String fileName=path.substring(path.lastIndexOf("/")+1);
		if (stripParentDir) {
			path = path.substring(path.indexOf("/"));
		}
		path = path.substring(0, path.lastIndexOf("/"));
		String	full=null;
		if (prefixFile) {
			full=path+File.separator+prefix+fileName;
		} else {
			//We move the slash to the front.
			if (path.length()<1) {
				//Nothing to do for
				return;
			}
			if (stripParentDir) {
				full="/"+prefix +path.substring(1)+File.separator+fileName;
			} else {
				full="/"+prefix +path.substring(0)+File.separator+fileName;
			}
			
		}
		logger.info("Extracting: " + entry.getName()
				+ " to destination: " + dest + " as " + full);
		File efile = new File(dest, full);
		// Is a directory??
		if (entry.isDirectory()) {
			if (!efile.exists()) {
				efile.mkdirs();
			}
		} else {
			File dir = new File(dest);
			if (!dir.exists()) {
				logger.info("Creating dir: " + dest);
				dir.mkdirs();
			}
			if(!efile.exists()){
				new File(efile.getParent()).mkdirs();
				logger.info("The path for the new file is " + efile.getPath());
				efile.createNewFile();
			}
			InputStream in = new BufferedInputStream(jar.getInputStream(entry));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(
					efile));
			byte[] buffer = new byte[2048];
			for (;;) {
				int nBytes = in.read(buffer);
				if (nBytes <= 0)
					break;
				out.write(buffer, 0, nBytes);
			}
			out.flush();
			out.close();
			in.close();
		}

	}
	
	public static void dropStaticFile(String dest, String dest2, ZipEntry entry, JarFile jar,
			String prefix, boolean prefixFile, boolean stripParentDir) throws IOException {
		if (prefix == null) {
			prefix = "";
		}
			
			String path = entry.getName();

			String fileName=path.substring(path.lastIndexOf("/")+1);
			if (stripParentDir) {
				path = path.substring(path.indexOf("/"));
			}
			path = path.substring(0, path.lastIndexOf("/"));
			String	full=null;
			

			if (prefixFile) {
				full=path+File.separator+prefix+fileName;
			} else {
				//We move the slash to the front.
				if (path.length()<1) {
					//Nothing to do for
					return;
				}
				if (stripParentDir) {
					full="/"+prefix +path.substring(1)+File.separator+fileName;
				} else {
					full="/"+prefix +path.substring(0)+File.separator+fileName;
				}

			}
			logger.info("Extracting: " + entry.getName()
					+ " to destination: " + dest + " as " + full);
			File efile = new File(dest, full);
			// Is a directory??
			if (entry.isDirectory()) {
				if (!efile.exists()) {
					efile.mkdirs();
				}
			} else {
				File dir = new File(dest);
				if (!dir.exists()) {
					logger.info("Creating dir: " + dest);
					dir.mkdirs();
				}
				if(!efile.exists()){
					new File(efile.getParent()).mkdirs();
					logger.info("The path for the new file is " + efile.getPath());
					efile.createNewFile();
				}
				InputStream in = new BufferedInputStream(jar.getInputStream(entry));
				OutputStream out = new BufferedOutputStream(new FileOutputStream(
						efile));
				byte[] buffer = new byte[2048];
				for (;;) {
					int nBytes = in.read(buffer);
					if (nBytes <= 0)
						break;
					out.write(buffer, 0, nBytes);
				}
				out.flush();
				out.close();
				in.close();
			}
			
			if(entry.getName().endsWith(".jar")){
				logger.info("Extracting jar " + efile.getName());
				FileInputStream fis = new FileInputStream(efile);
			    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

				final byte[] buffer = new byte[2048];
				//final File tmpDir = new File(dest2, file.);

				ZipEntry ze;
		        while((ze = zis.getNextEntry()) != null) {
					final ZipEntry zipEntry = ze;
					if(!zipEntry.getName().startsWith("META-INF")){
			            // write the files to the disk
						final File fileToWrite = new File(dest2, zipEntry.getName());
						final File folder = fileToWrite.getParentFile();
						if(!folder.mkdirs() && !folder.exists()) {
							logger.error("Cannot create: " + folder);
							System.exit(0);
						}
				
		        		 if(!zipEntry.isDirectory()) {
				                //No need to use buffered streams since we're doing our own buffering
				                final FileOutputStream fos = new FileOutputStream(fileToWrite);
				                int size;
				                while ((size = zis.read(buffer)) != -1) {
				                    fos.write(buffer, 0, size);
				                }
				                fos.close();
				            }
			        
					}
				}
		        zis.close();
				logger.info("Deleting jar " + efile.getName());
				efile.delete();
			}
	}


	private void mergeWebXML(String fragment, File file) throws IOException {
		if (fragment==null) {
			return;
		}
		List<String> entities = new ArrayList<String>();
		Map<String, Map<String, String>> overrideMaps = new HashMap<String, Map<String, String>>();
		entities.add("servlet");
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("servlet", "servlet-name");
		overrideMaps.put("servlet", map2);

		entities.add("servlet-mapping");
		map2 = new HashMap<String, String>();
		map2.put("servlet-mapping", "url-pattern");
		overrideMaps.put("servlet-mapping", map2);

		entities.add("filter");
		map2 = new HashMap<String, String>();
		map2.put("filter", "filter-name");
		overrideMaps.put("filter", map2);

		entities.add("filter-mapping");

		entities.add("context-param");
		map2 = new HashMap<String, String>();
		map2.put("context-param", "param-name");
		overrideMaps.put("context-param", map2);
		
		entities.add("security-constraint");

		entities.add("listener");

		Map<String, String> map = separateXML(fragment, entities);

		for (String key : map.keySet()) {
			String section = map.get(key);
			if (section.length() > 0) {
				String tag = key.toUpperCase();
				String startTag = "<!-- BEGIN " + tag + "S -->";
				String endTag = "<!-- END " + tag + "S -->";
				String startSection = "<!-- BEGIN PLUGIN-" + tag + ":" + name
						+ " -->";
				String endSection = "<!-- END PLUGIN-" + tag + ":" + name
						+ " -->";
				merge(file, startTag, endTag, startSection, endSection, section);
				Map<String, String> overrideMap = overrideMaps.get(key
						.toLowerCase());
				if (overrideMap != null) {
					merge(file, startTag, endTag, startSection, endSection,
							section, overrideMap,
							"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:"
									+ name + " -->", "<!-- BEGIN OVERRIDE");
				} else {
					merge(file, startTag, endTag, startSection, endSection,
							section);
				}

			}
		}

	}

	private void mergeStrutsXML(String fragment, File file) throws IOException {
		if (fragment==null) {
			return;
		}
		List<String> entities = new ArrayList<String>();
		Map<String, Map<String, String>> overrideMaps = new HashMap<String, Map<String, String>>();

		entities.add("form-bean");
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("form-bean", "name");
		overrideMaps.put("form-bean", map2);

		entities.add("action");
//		map2 = new HashMap<String, String>();
		map2.put("action", "path");
		overrideMaps.put("action", map2);

		entities.add("plug-in");
//		map2 = new HashMap<String, String>();
		map2.put("plug-in", "class");
		overrideMaps.put("plug-in", map2);
		
		Map<String, String> map = separateXML(fragment, entities);

		for (String key : map.keySet()) {
			String section = map.get(key);
			if (section.length() > 0) {
				String tag = key.toUpperCase();
				String startTag = "<!-- BEGIN " + tag + "S -->";
				String endTag = "<!-- END " + tag + "S -->";
				String startSection = "<!-- BEGIN PLUGIN-" + tag + ":" + name
						+ " -->";
				String endSection = "<!-- END PLUGIN-" + tag + ":" + name
						+ " -->";
				merge(file, startTag, endTag, startSection, endSection, section);
				Map<String, String> overrideMap = overrideMaps.get(key
						.toLowerCase());
				if (overrideMap != null) {
					mergeByAttribute(file, startTag, endTag, startSection,
							endSection, section, overrideMap,
							"<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:"
									+ name + " -->", "<!-- BEGIN OVERRIDE");
				} else {
					merge(file, startTag, endTag, startSection, endSection,
							section);
				}

			}
		}

	}

	protected Map<String, String> separateXML(String fragment,
			List<String> entityList) {
		Map<String, String> map = new HashMap<String, String>();
		String patternText = "<(";
		for (String entity : entityList) {
			patternText += entity.toLowerCase() + "|";
		}
		patternText = patternText.substring(0, patternText.length() - 1);
		patternText += ")[^\\\\]*?>.*?</\\1>";
		Pattern p = Pattern.compile(patternText, Pattern.DOTALL);
		Matcher m = p.matcher(fragment);
		while (m.find()) {
			String token = m.group();
			String tag = token.substring(1, token.indexOf(">")).toLowerCase()
					.trim();
			int spaceIndex = tag.indexOf(" ");
			if (spaceIndex > 1) {
				tag = tag.substring(0, spaceIndex);
			}
			String s = map.get(tag);
			if (s == null) {
				s = "";

			} else {
				s += "\n";
			}
			s += token;
			map.put(tag, s);
		}

		return map;
	}

	/**
	 * Merges and overrides properties
	 * 
	 * @param input
	 * @param targetCommentBegin
	 * @param targetCommentEnd
	 * @param startComment
	 * @param endComment
	 * @param mergeText
	 * @param comment
	 * @param overrideBegin
	 * @param overrideEnd
	 * @return
	 * @throws IOException
	 */
	public String merge(InputStream input, String targetCommentBegin,
			String targetCommentEnd, String startComment, String endComment,
			String mergeText, String comment, String overrideBegin)
			throws IOException {
		String s = merge(input, targetCommentBegin, targetCommentEnd,
				startComment, endComment, mergeText);
		List<String> props = new ArrayList<String>();
		String[] result = mergeText.split("\n");
		for (String line : result) {
			String trim = line.trim();
			if ((!trim.startsWith("#")) && trim.length() > 0) {
				int index = trim.indexOf("=");
				if (index > 0) {
					String propName = trim.substring(0, index);
					props.add(propName);
				}
			}
		}

		StringBuilder buf = new StringBuilder();
		result = s.split("\n");
		boolean inPlugins = false;
		for (String line : result) {
			String trim = line.trim();
			if (trim.startsWith(startComment)) {
				inPlugins = true;
			}
			if (trim.startsWith(endComment)) {
				inPlugins = false;
			}
			int index = trim.indexOf("=");
			if ((!inPlugins) && index > 0
					&& props.contains(trim.substring(0, index))) {
				buf.append(overrideBegin);
				buf.append("\n");
				buf.append(comment);
				buf.append(line);
			} else {
				buf.append(line);
			}
			buf.append("\n");

		}

		return buf.toString();
	}

	/**
	 * Merges and overrides XML files.
	 * 
	 * @param input
	 * @param targetCommentBegin
	 * @param targetCommentEnd
	 * @param startComment
	 * @param endComment
	 * @param mergeText
	 * @param overrides
	 * @param overrideBegin
	 * @param overrideEnd
	 * @param override
	 *            
	 * @return
	 * @throws IOException
	 */
	public String mergeByKey(InputStream input, String targetCommentBegin,
			String targetCommentEnd, String startComment, String endComment,
			String mergeText, Map<String, String> overrides,
			String overrideBegin, String overrideEnd, String override)
			throws IOException {
		StringBuffer buf = new StringBuffer();

		List<String> overrideTypes = new ArrayList<String>();
		overrideTypes.addAll(overrides.keySet());
		Map<String, String> overrideTags = separateXML(mergeText, overrideTypes);
		String s = merge(input, targetCommentBegin, targetCommentEnd,
				startComment, endComment, mergeText);
		int startIndex = s.indexOf(startComment);
		int endIndex = s.indexOf(endComment);
		buf.append(s);
		// <(tool)>[^(</tool>)]*?<(key)>(helloWorld2|helloWorld4)</\2>.*?</\1>

		for (String tagName : overrideTags.keySet()) {
			s = buf.toString();
			buf = new StringBuffer();
			// Keyname:
			String keyName = overrides.get(tagName);
			// Get the keys Values
			// Example REGEX to finde the keys:
			// <(tool)>[^(</tool>)]*?<(key)>(.*?)</\2>.*?</\1>
			String keysPattern = "<(" + tagName + ")>.*?<(" + keyName
					+ ")>(.*?)</\\2>.*?</\\1>";
			// <(tool)>[^(</tool>)]*?<(key)>(.*?)</\2>.*?</\1>
			Pattern keyPattern = Pattern.compile(keysPattern, Pattern.DOTALL);
			Matcher m = keyPattern.matcher(mergeText);
			List<String> keyList = new ArrayList<String>();
			while (m.find()) {

				keyList.add(m.group(3));
			}

			String patternText = "<(" + tagName + ")>.*?<(" + keyName + ")>(";
			if (keyList.size() == 0) {
				buf.append(s);
				continue;
			}
			for (String entity : keyList) {
				patternText += PluginUtil.escapeForRegex(entity) + "|";
			}
			patternText = patternText.substring(0, patternText.length() - 1);
			patternText += ")</\\2>.*?</\\1>";

			logger.debug(patternText);
			Pattern p = Pattern.compile(patternText, Pattern.DOTALL);

			String endTag = "</" + tagName + ">";

			int offset = 0;
			String sub = s;
			while (sub.length() > 0) {
				// Do we have another tag??
				int endTagIndex = sub.indexOf(endTag);
				if (endTagIndex > 0) {
					String part = sub.substring(0, endTagIndex
							+ endTag.length());
					m = p.matcher(part);
					if (m.find()) {
						// Should only be one match

						int from = m.start() + offset;
						int to = m.end() + offset;
						buf.append(part.substring(0, m.start()));

						if (!(from > startIndex && (to < endIndex))) {
							// Figure out if it's commented out
							boolean commented = s
									.substring(
											s.substring(0, from).lastIndexOf(
													">"), from).contains(
											override);
							if (commented) {
								buf.append(s.substring(from, to));
							} else {
								String element = s.substring(from, to);
								element = element.replaceAll("<!--", "#!--");
								element = element.replaceAll("-->", "--#");
								buf.append(overrideBegin);
								buf.append("\n");
								buf.append(element);
								buf.append("\n");
								buf.append(overrideEnd);
							}
						} else {
							buf.append(s.substring(from, to));
						}

						// If not inside range, comment out.

					} else {
						buf.append(part);
					}
					offset += part.length();
					sub = sub.substring(part.length());
				} else {
					// Nothing else found, append the last bit, and set sub to
					// nothing to break the while loop
					buf.append(sub);

					sub = "";
				}

			}

		}

		return buf.toString();
	}

	/**
	 * Merges and overrides XML files.
	 * 
	 * @param input
	 * @param targetCommentBegin
	 * @param targetCommentEnd
	 * @param startComment
	 * @param endComment
	 * @param mergeText
	 * @param overrides
	 * @param overrideBegin
	 * @param overrideEnd
	 * @param override
	 *            
	 * @return
	 * @throws IOException
	 */
	public String mergeByAttribute(InputStream input,
			String targetCommentBegin, String targetCommentEnd,
			String startComment, String endComment, String mergeText,
			Map<String, String> overrides, String overrideBegin,
			String overrideEnd, String override) throws IOException {
		StringBuffer buf = new StringBuffer();
		String s = merge(input, targetCommentBegin, targetCommentEnd,
				startComment, endComment, mergeText);
		// Searh my fragment
		// <(form-bean) [^(/)]*?name="([^"]*?)"[^(/)]*?/>
		// <(form-bean)
		// [^(/)]*?name="([^"]*?)"[^(/|>)]*?>[^(/form\-bean)]*?</form-bean>
		buf.append(s);
		int startIndex = s.indexOf(startComment);
		int endIndex = s.indexOf(endComment);

		for (String tagName : overrides.keySet()) {
			s = buf.toString();
			buf = new StringBuffer();
			String keyName = overrides.get(tagName);
			// First search out fragments
			String keyPattern = "<(" + tagName + ") [^(/)]*?" + keyName
					+ "=\"([^\"]*?)\"[^(/)]*?/>";
			Pattern p = Pattern.compile(keyPattern, Pattern.DOTALL);
			Matcher m = p.matcher(mergeText);
			List<String> keyList = new ArrayList<String>();
			while (m.find()) {
				keyList.add(m.group(2));
			}
			// Search the second form
			keyPattern = "(<" + tagName + " )[^(/)]*?" + keyName
					+ "=\"([^\"]*?)\"[^(>)]*?>[^(\\\\1)]*?</" + tagName + ">";
			p = Pattern.compile(keyPattern, Pattern.DOTALL);
			m = p.matcher(mergeText);
			while (m.find()) {
				keyList.add(m.group(2));
			}

			if (keyList.size() == 0) {
				buf.append(s);
				continue;
			}

			// Now comment (twice, since there are two forms)

			// Search what to replace
			// <(form-bean) [^(/)]*?name="(ResumeForm|JobsForm)"
			// [^(/)]*?>[^(/form\-bean)]*?</form-bean>

			String patternText = "(<" + PluginUtil.escapeForRegex(tagName) + " )[^(/)]*?" + PluginUtil.escapeForRegex(keyName)
					+ "=\"(";
			for (String entity : keyList) {
				patternText += PluginUtil.escapeForRegex(entity) + "|";
			}
			patternText = patternText.substring(0, patternText.length() - 1);
			patternText += ")\"[^([/>]|[/"+PluginUtil.escapeForRegex(tagName)+">])]*?>[^\\\\1]*?</" + PluginUtil.escapeForRegex(tagName) + ">";

			p = Pattern.compile(patternText, Pattern.DOTALL);
			m = p.matcher(s);

			int pos = 0;
			while (m.find()) {
				int from = m.start();
				int to = m.end();
				buf.append(s.substring(pos, from));
				pos = from;

				if (!(from > startIndex && (to < endIndex))) {
					// Figure out if it's commented out
					boolean commented = s.substring(
							s.substring(0, from).lastIndexOf(">"), from)
							.contains(override);
					if (commented) {
						buf.append(s.substring(from, to));
					} else {
						String overridenText = s.substring(from, to);
						overridenText = overridenText
								.replaceAll("<!--", "#!--");
						overridenText = overridenText.replaceAll("-->", "--#");
						buf.append(overrideBegin);
						buf.append("\n");
						buf.append(overridenText);
						buf.append("\n");
						buf.append(overrideEnd);
					}
				} else {
					buf.append(s.substring(from, to));
				}
				pos = to;
				// If not inside range, comment out.
			}
			buf.append(s.substring(pos));

			s = buf.toString();
			buf = new StringBuffer();

			// <(form-bean) [^(/)]*?name="(ResumeForm|JobsForm|mapsMapsForm)"
			// [^(/)]*?/>
			patternText = "<(" + tagName + ") [^(/)]*?" + keyName + "=\"(";
			for (String entity : keyList) {
				patternText += entity + "|";
			}
			patternText = patternText.substring(0, patternText.length() - 1);
			patternText += ")\" [^(/)]*?/>";

			p = Pattern.compile(patternText, Pattern.DOTALL);
			m = p.matcher(s);

			pos = 0;
			while (m.find()) {
				int from = m.start();
				int to = m.end();
				buf.append(s.substring(pos, from));
				pos = from;

				if (!(from > startIndex && (to < endIndex))) {
					// Figure out if it's commented out
					boolean commented = s.substring(
							s.substring(0, from).lastIndexOf(">"), from)
							.contains(override);
					if (commented) {
						buf.append(s.substring(from, to));
					} else {
						buf.append(overrideBegin);
						buf.append("\n");
						buf.append(s.substring(from, to));
						buf.append("\n");
						buf.append(overrideEnd);
					}
				} else {
					buf.append(s.substring(from, to));
				}
				pos = to;
				// If not inside range, comment out.
			}
			buf.append(s.substring(pos));

		}
		return buf.toString();
	}

	public String merge(InputStream input, String targetCommentBegin,
			String targetCommentEnd, String startComment, String endComment,
			String mergeText) throws IOException {
		StringBuffer buf = new StringBuffer();
		StringBuffer original = new StringBuffer();
		boolean found = false;
		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader reader = new BufferedReader(isr);
		String line;

		while ((line = reader.readLine()) != null) {
			original.append(line);

			if (line.contains(targetCommentEnd)) {
				String temp = buf.toString();
				// Test to see if our plugin already has a tag
				if (temp.contains(startComment)) {
					if (temp.contains(endComment)) {
						// Already has our tag, remove it
						logger.debug("Removing tag from source: "
								+ endComment);
						buf = new StringBuffer();
						buf.append(temp
								.substring(0, temp.indexOf(startComment)));
						buf
								.append(temp.substring((temp
										.indexOf(endComment) + endComment
										.length())));
					} else {
						logger.fatal(
								"Source has start tag but lack ending tag.");
					}
				} else {
					if (temp.contains(endComment)) {
						logger.fatal(
								"Source  has ending tag but lack start tag.");
					}
				}
				if (line.contains(targetCommentBegin)) {
					// The same line contains both comments, so let's split it
					String firstPart = line.substring(0, (line
							.indexOf(targetCommentBegin) + targetCommentBegin
							.length()));
					buf.append(firstPart);
					buf.append("\n");
					line = line.substring(firstPart.length());
				}
				if (line.contains(endComment)) {
					if (line.contains(startComment)) {
						// Both on same line
						String tempLine = line.substring(0, line
								.indexOf(startComment));

						line = tempLine
								+ line
										.substring((line.indexOf(endComment) + endComment
												.length()));
					} else {
						if (temp.contains(startComment)) {
							temp = temp
									.substring(0, temp.indexOf(startComment));
							line = line.substring(line.indexOf(endComment)
									+ endComment.length());
						} else {
							// Incomplete tag
							logger.fatal("Source  has ending tag but lack start tag.");
						}
					}
				}
				// Remove trailing whitespace
				String trim = buf.toString().trim();
				buf = new StringBuffer();
				buf.append(trim);
				buf.append("\n");

				buf.append(startComment);
				buf.append("\n");
				buf.append(mergeText);
				buf.append("\n");
				buf.append(endComment);
				buf.append("\n");
				found = true;

			}
			buf.append(line);
			buf.append("\n");

		}
		reader.close();
		if (!found) {
			throw new RuntimeException("Tags not found in stream: "
					+ targetCommentBegin);
		}
		if (buf.toString().length() == 0) {
			return original.toString();
		}

		return buf.toString();
	}

	public String removePropertiesFragment(InputStream is, String startComment,
			String endComment, String comment, String beginOverride)
			throws IOException {
		StringBuffer buf = new StringBuffer();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader reader = new BufferedReader(isr);
		String line;
		boolean inPlugin = false;
		boolean inOverride = false;
		while ((line = reader.readLine()) != null) {
			if (beginOverride != null && line.contains(beginOverride)) {
				inOverride = true;
			} else {
				if (line.contains(startComment)) {
					inPlugin = true;
				} else {
					if (line.contains(endComment)) {
						inPlugin = false;
					} else {
						if (inOverride) {
							inOverride = false;
							line = line.trim().substring(comment.length());
						}
						if (!inPlugin && line.length() > 0) {
							buf.append(line);
							buf.append("\n");
						}
					}
				}
			}
		}
		return buf.toString();

	}

	public boolean removeFragmentXML(File file, String startComment,
			String endComment, String beginOverride, String endOverride)
			throws IOException {
		if (!file.exists()) {
			logger.error("File doesn't exist: " + file.getAbsolutePath());
			return false;
		}

		logger.error("Cleaning file: " + file.getAbsolutePath());
		InputStream input = new FileInputStream(file);
		String buf = removeFragmentXML(input, startComment, endComment,
				beginOverride, endOverride);

		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(buf);
		out.close();
		return true;

	}

	public boolean removePropertiesFragment(File file, String startComment,
			String endComment, String beginOverride, String endOverride)
			throws IOException {
		if (!file.exists()) {
			logger.error("File doesn't exist: " + file.getAbsolutePath());
			return false;
		}

		logger.error("Cleaning file: " + file.getAbsolutePath());
		InputStream input = new FileInputStream(file);
		String buf = removePropertiesFragment(input, startComment, endComment,
				beginOverride, endOverride);

		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(buf);
		out.close();
		return true;

	}

	public String removeFragmentXML(InputStream is, String startComment,
			String endComment, String beginOverride, String endOverride)
			throws IOException {

		StringBuffer buf = new StringBuffer();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader reader = new BufferedReader(isr);
		String line;
		boolean inPlugin = false;
		boolean inOverride = false;
		while ((line = reader.readLine()) != null) {
			if (beginOverride != null) {
				if (line.contains(beginOverride)) {
					inOverride = true;
					if (line.contains(endOverride)) {
						// Both in same line handle restoring contentst inside
						// the block
						inOverride = false;
					}
				}
				line = line.replace(beginOverride, "");
			}
			if (endOverride != null) {
				if (line.contains(endOverride)) {
					inOverride = false;
				}
				line = line.replace(endOverride, "");
			}

			if (line.contains(startComment)) {
				buf.append(line.substring(0, line.indexOf(startComment)));
				if (line.contains(endComment)) {
					String overridenText = line.substring(line
							.indexOf(startComment)
							+ endComment.length());
					overridenText = removeXMLComment(overridenText);
					buf.append(overridenText);
				} else {
					inPlugin = true;
				}
			}

			if (!inPlugin && line.length() > 0) {
				if (inOverride) {
					line = removeXMLComment(line);
				}
				buf.append(line);
				buf.append("\n");
			}

			if (line.contains(endComment)) {
				String overridenText = line
						.substring((line.indexOf(endComment) + endComment
								.length()));
				overridenText = removeXMLComment(overridenText);
				buf.append(overridenText);
				inPlugin = false;
			}

		}
		return buf.toString();
	}

	private String removeXMLComment(String s) {
		String line = s;
		line = line.replaceAll("#!--", "<!--");
		line = line.replaceAll("--#", "-->");
		return line;
	}

    private final boolean isSet(String x) {
        if (x == null) {
            return false;
        }

        x = x.toLowerCase();

        if (x.indexOf("null") > -1) {
            x = x.replaceAll("null", "");
        }

        return (x.trim().length() > 0);
    }

    /**
     * @param parameter String value to extract.
     * @param defaultValue boolean fall-through value
     * @return true if parameter is "true", false if parameter is "false", defaultValue otherwise
     */
    private static boolean getBooleanFromString(String parameter, boolean defaultValue) {
      if (parameter == null) {
        return defaultValue;
      } else if (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("t") || parameter.equalsIgnoreCase("1")) {
        return true;
      } else if (parameter.equalsIgnoreCase("false") || parameter.equalsIgnoreCase("f") || parameter.equalsIgnoreCase("0")){
        return false;
      } else {
        return defaultValue;
      }
    }
}
