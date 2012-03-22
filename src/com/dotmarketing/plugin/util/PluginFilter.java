/**
 * 
 */
package com.dotmarketing.plugin.util;

import java.io.File;
import java.io.FilenameFilter;

final class PluginFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		if (name.startsWith("plugin-") && name.endsWith(".jar")) {
			return true;
		}
		return false;
	}
}