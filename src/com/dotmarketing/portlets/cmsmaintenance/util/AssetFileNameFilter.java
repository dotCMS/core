package com.dotmarketing.portlets.cmsmaintenance.util;

import java.io.File;
import java.io.FileFilter;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

public class AssetFileNameFilter implements FileFilter {

	@Override
	public boolean accept(File dir) {
		if(dir ==null){
			return false;
		}


		if(dir.getAbsolutePath().contains("dotGenerated") ){
			return false;
		}
		String name = dir.getName();
		String osname = System.getProperty("os.name");
		String[] path = null;

		if (osname.startsWith("Windows"))
			path = dir.getAbsolutePath().split("\\\\");
		else
			path = dir.getAbsolutePath().split(File.separator);


		String[] test = new String[0];
		String assetPath=null;
        try {
        	assetPath = Config.getStringProperty("ASSET_REAL_PATH", FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH")));
        	test = new File(assetPath).getAbsolutePath().split(File.separator);
        } catch (Exception e) {
        	Logger.debug(this.getClass(), e.getMessage());
        }
        if(test.length +1 == path.length){

			if( "license".equals(name)){
				return false;
			}
			else if( "bundles".equals(name)){
				return false;
			}
			else if( "tmp_upload".equals(name)){
				return false;
			}
			else if( name.startsWith(".")){
				return false;
	        }
			else if(name.equals("timemachine")) {
			    return false;
			}
        }
		return true;

	}

}
