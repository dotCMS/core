package com.dotmarketing.util;

import com.dotmarketing.business.APILocator;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import java.io.File;

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

    private final static String DEFAULT_RELATIVE_ASSET_PATH = "/assets";

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
		if (!UtilMethods.isSet(realPath)) {
			realPath = com.liferay.util.FileUtil.getRealPath("/dotsecure");
		}
		return (realPath.endsWith(File.separator)) ?
		                realPath.substring(0, realPath.length()-1)
		                :realPath;

	}

	public static String getDynamicVelocityPath() {
		return getDynamicContentPath() + File.separator + "velocity"
				+ File.separator + "dynamic";
	}

	public static String getGraphqlPath() {
		return getDynamicContentPath() + File.separator + "graphql";
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

    /**
     * Builds the path using STATIC_PUBLISHING_ROOT_PATH values or default
     * /assets/static_publishing, created the fodler if it doesn't exist.
     *
     * @return {@link} String with the path of Static Publish folder where bundles are going to be
     * stored.
     */
    public static String getStaticPublishPath() {

        final String path = Config.getStringProperty("STATIC_PUBLISHING_ROOT_PATH",
                APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator
                        + "static_publishing");

        final File folder = FileUtil.mkDirsIfNeeded(path);

        return folder.getPath();
    } //getStaticPublishPath.

	public static String getServerId(){
		return APILocator.getServerAPI().readServerId();
	}


    /**
     * This method returns the absolute root path for assets
     *
     * @return the root folder of where assets are stored
     */
    public static String getAbsoluteAssetsRootPath() {
        String realPath = Config.getStringProperty("ASSET_REAL_PATH", null);
        if (UtilMethods.isSet(realPath) && !realPath.endsWith(File.separator)) {
            realPath = realPath + File.separator;
        }
        if (!UtilMethods.isSet(realPath)) {
            final String path = Try
                    .of(() -> Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH))
                    .getOrElse(DEFAULT_RELATIVE_ASSET_PATH);
            return FileUtil.getRealPath(path);
        } else {
            return realPath;
        }
    }
}
