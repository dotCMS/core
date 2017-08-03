package com.dotmarketing.util;

import com.dotmarketing.business.APILocator;

import java.io.File;
import java.util.UUID;

/**
 * Generic class to get return configuration parameters, and any logic required
 * for those paramenters. This is different from the Config class, which only
 * reads from the config file.
 *
 * @author andres
 *
 */
public class ConfigUtils {


    /*
     * This property determine if the app is running on dev mode.
     */
    public static final String DEV_MODE_KEY = "dotcms.dev.mode";

	/**
	 * Returns true if app is running on dev mode.
	 * @return boolean
     */
	public static boolean isDevMode () {

        // by default if the vars does not exists, we assume is not
        // running on dev mode, so it is false.
        return Config.getBooleanProperty(DEV_MODE_KEY, false);
	}

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

	public static String getLucenePath() {
		return getDynamicContentPath() + File.separator + "dotlucene";
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

	public static String getServerId(){
		String serverId;
		if (Config.getStringProperty("DIST_INDEXATION_SERVER_ID")==null || Config.getStringProperty("DIST_INDEXATION_SERVER_ID").equalsIgnoreCase("")) {
			serverId = APILocator.getServerAPI().readServerId();

			if(!UtilMethods.isSet(serverId)) {
				serverId = UUID.randomUUID().toString();

			}

			Config.setProperty("DIST_INDEXATION_SERVER_ID", serverId);
		} else {
			serverId= Config.getStringProperty("DIST_INDEXATION_SERVER_ID");
		}
		return serverId;
	}


}
