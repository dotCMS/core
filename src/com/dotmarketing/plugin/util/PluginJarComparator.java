/**
 * 
 */
package com.dotmarketing.plugin.util;

import java.io.File;
import java.util.Comparator;
import java.util.List;

class PluginJarComparator implements Comparator<File> {
	
	private File pluginXML;
	
	/**
	 * @return the pluginXML
	 */
	public File getPluginXML() {
		return pluginXML;
	}

	/**
	 * @param pluginXML the pluginXML to set
	 */
	public void setPluginXML(File pluginXML) {
		this.pluginXML = pluginXML;
	}

	public PluginJarComparator(File pluginXML){
		this.pluginXML = pluginXML;
	}
	
	public int compare(File f1, File f2) {
		List<String> pluginIds = (List<String>) PluginUtil.parsePluginXML(pluginXML).get(PluginUtil.SYSTEM_PLUGIN_ORDER_KEY);
		String f1Name = f1.getName();
		String f2Name = f2.getName();
		f1Name = f1Name.replaceFirst("plugin-", "");
		f1Name = f1Name.replace(".jar", "");
		f2Name = f2Name.replaceFirst("plugin-", "");
		f2Name = f2Name.replace(".jar", "");
		for (String pluginId : pluginIds) {
			if (f1Name.equals(pluginId)) {
				return -1;
			} else if (f2Name.equals(pluginId)) {
				return 1;
			}
		}
		return f1Name.compareTo(f2Name);
	}
}