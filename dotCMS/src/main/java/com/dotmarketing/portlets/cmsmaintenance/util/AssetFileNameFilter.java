package com.dotmarketing.portlets.cmsmaintenance.util;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileFilter;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import java.util.Set;

public class AssetFileNameFilter implements FileFilter {

    private static final Set<String> EXCLUDE_FOLDERS_LIST = ImmutableSet
            .of("license", "bundles", "tmp_upload",
                    "timemachine", "integrity", "server", "dotGenerated");

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

        return test.length + 1 != path.length || (name.charAt(0) != '.' && !EXCLUDE_FOLDERS_LIST.contains(name));
        
	}

}
