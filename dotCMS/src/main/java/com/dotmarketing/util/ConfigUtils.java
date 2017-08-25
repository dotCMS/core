package com.dotmarketing.util;

import java.io.File;

import com.dotmarketing.business.APILocator;

/**
 * Generic class to get return configuration parameters, and any logic required
 * for those paramenters. This is different from the Config class, which only
 * reads from the config file.
 *
 * @author andres
 *
 */
public class ConfigUtils {



	public static String getDynamicContentPath() {
		String realPath = Config.getStringProperty("DYNAMIC_CONTENT_PATH");
		if (UtilMethods.isSet(realPath)) {
			if (!realPath.endsWith(java.io.File.separator)) {
				realPath = realPath + java.io.File.separator;
			}
		} else {
			realPath = com.liferay.util.FileUtil.getRealPath("/dotsecure");
		}
		return realPath;

	}

	public static String getDynamicVelocityPath() {
		return getDynamicContentPath() + File.separator + "velocity"
				+ File.separator + "dynamic";
	}

	public static String getACheckerPath() {
		return com.liferay.util.FileUtil.getRealPath("/WEB-INF/achecker_sql");
	}


	public static String getBackupPath() {
		return getDynamicContentPath() + File.separator + "backup";
	}

	public static String getBundlePath() {
		String path=APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "bundles";
		File pathDir=new File(path);
		if(!pathDir.exists())
		    pathDir.mkdirs();
		return path;
	}

	public static String getIntegrityPath() {
		String path=APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "integrity";
		File pathDir=new File(path);
		if(!pathDir.exists())
		    pathDir.mkdirs();
		return path;
	}

	public static String getTimeMachinePath(){

		String path = Config.getStringProperty("TIMEMACHINE_PATH", null);

		if(path == null || (path != null && path.equals("null")) ){
			path=APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "timemachine";
			File pathDir=new File(path);
			if(!pathDir.exists())
			    pathDir.mkdirs();
		}

		return path;
	}
	/**
	 *
	 * @deprecated use {@link ServerAPI.readServerId()} instead.  
	 */
	@Deprecated
	public static String getServerId(){
		return APILocator.getServerAPI().readServerId();

	}


}
