package com.dotmarketing.portlets.cmsmaintenance.util;

import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileFilter;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * This {@link FileFilter} implementation provides the actual asset files in the current dotCMS repository. By default,
 * all files and folders that are unnecessary, temporary or application-specific will be excluded.
 *
 * @author Will Ezell
 * @since Dec 4th, 2012
 */
public class AssetFileNameFilter implements FileFilter {

	private static final Set<String> EXCLUDE_FOLDERS_LIST = Set
			.of("license.zip", "license", "bundles", "tmp_upload",
                    "timemachine", "integrity", "server", "dotGenerated");

	private Set<String> excludedFolders;

	/**
	 * Allows you to add the name of a folder that must be excluded from the result of traversing the dotCMS
	 * {@code /assets/}. This folder name will be added to the existing default list of excluded system folders.
	 *
	 * @param folderName The name of the folder that will be excluded.
	 */
	public void addExcludedFolder(final String folderName) {
		this.getExcludedFolders().add(folderName);
	}

	/**
	 * Returns the complete list of folders that will be excluded from the result of traversing the dotCMS
	 * {@code /assets/} folder.
	 *
	 * @return The complete list of excluded folders.
	 */
	public Set<String> getExcludedFolders() {
		if (UtilMethods.isNotSet(this.excludedFolders)) {
			this.excludedFolders = new HashSet<>();
			this.excludedFolders.addAll(EXCLUDE_FOLDERS_LIST);
		}
		return this.excludedFolders;
	}

	@Override
	public boolean accept(File dir) {
		if(dir ==null){
			return false;
		}

		if(dir.getAbsolutePath().contains("dotGenerated") ){
			return false;
		}

		String name = dir.getName();
		final String osName = System.getProperty("os.name");

		String[] path;

		if (osName.startsWith("Windows")) {
			path = dir.getAbsolutePath().split("\\\\");
		}
		else {
			path = dir.getAbsolutePath().split(File.separator);
		}

		String[] test = new String[0];

		String assetPath;

        try {
        	assetPath = Config.getStringProperty("ASSET_REAL_PATH", FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH")));
        	test = new File(assetPath).getAbsolutePath().split(File.separator);
        } catch (Exception e) {
        	Logger.debug(this.getClass(), e.getMessage());
        }

		return test.length + 1 != path.length || (name.charAt(0) != '.' && !this.getExcludedFolders().contains(name));
        
	}

}
