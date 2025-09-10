package com.dotmarketing.util;

import static com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl.DEFAULT_LANGUAGE_CODE;
import static com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl.DEFAULT_LANGUAGE_COUNTRY_CODE;

import com.dotmarketing.business.APILocator;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic class to get return configuration parameters, and any logic required
 * for those parameters. This is different from the Config class, which only
 * reads from the config file.
 *
 * @author andres
 *
 */
public class ConfigUtils {

    private static final String DEFAULT_RELATIVE_ASSET_PATH = "/assets";
    private static final Lazy<Boolean> IS_DEV_MODE = Lazy.of(() -> Config.getBooleanProperty("dotcms.dev.mode", false));

	/**
	 * Returns true if app is running on dev mode.
	 * @return boolean
     */
	public static boolean isDevMode () {

        // by default if the vars does not exists, we assume is not
        // running on dev mode, so it is false.
        return IS_DEV_MODE.get();
	}

	public static String getDynamicContentPath() {

		String realPath = Config.getStringProperty("DYNAMIC_CONTENT_PATH",null);

		if (realPath == null) {
			realPath = Try.of(()-> com.liferay.util.FileUtil.getRealPath("/dotsecure")).getOrElse("." + File.separator + "dotsecure");
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

	/**
	 * Retrieves the configurable path for the backup directory.
	 * Defaults to DYNAMIC_CONTENT_PATH/dotsecure/backup if not overridden.
	 *
	 * @return the backup directory path
	 */
	public static String getBackupPath() {
		return Config.getStringProperty("BACKUP_DIRECTORY_PATH", getDynamicContentPath() + File.separator + "backup");
	}


	public static String getBundlePath() {
		final Path path = Paths.get(String.format("%s%sbundles",getAbsoluteAssetsRootPath(),File.separator)).normalize();
		File pathDir = path.toFile();
		if(!pathDir.exists())
		    pathDir.mkdirs();
		return path.toString();
	}

	public static String getIntegrityPath() {
		String path=getAbsoluteAssetsRootPath();
		path += (path.endsWith(File.separator) ? "" : File.separator) + "integrity";
		File pathDir=new File(path);
		if(!pathDir.exists())
		    pathDir.mkdirs();
		return path;
	}

	public static String getTimeMachinePath(){

		String path = Config.getStringProperty("TIMEMACHINE_PATH", null);

		if(path == null || (path != null && path.equals("null")) ){
			path=getAbsoluteAssetsRootPath() + File.separator + "timemachine";
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
				getAbsoluteAssetsRootPath() + File.separator
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
	private static Lazy<String> assetPath  = Lazy.of(()->{
		String realPath = Config.getStringProperty("ASSET_REAL_PATH", null);
		if (UtilMethods.isSet(realPath) && !realPath.endsWith(File.separator)) {
			return realPath + File.separator;
		}

		final String path = Try
				.of(() -> Config.getStringProperty("ASSET_PATH", DEFAULT_RELATIVE_ASSET_PATH))
				.getOrElse(DEFAULT_RELATIVE_ASSET_PATH);
		return FileUtil.getRealPath(path);

	});

	public static String getAssetPath() {
		return assetPath.get();
	}

	/**
	 * @deprecated Use {@link #getAssetPath()} instead.
	 * @return
	 */
	@Deprecated(forRemoval = true)
    public static String getAbsoluteAssetsRootPath() {
		return assetPath.get();
    }



	private static Lazy<String> assetTempPath  = Lazy.of(()->{
		java.io.File adir=new java.io.File(getAbsoluteAssetsRootPath() + java.io.File.separator + "tmp_upload");
		if(!adir.isDirectory()) {
			adir.mkdirs();
		}

		return adir.getPath();

	});
	public static String getAssetTempPath() {
		return assetTempPath.get();
	}
    


    private static final String LOCAL = "LOCAL";
    public static String getDotGeneratedPath() {
        return dotGeneratedPath.get() + File.separator + "dotGenerated";
    }

    private static Lazy<String> dotGeneratedPath =Lazy.of(() ->
			LOCAL.equalsIgnoreCase(Config.getStringProperty("DOTGENERATED_DEFAULT_PATH", LOCAL))
                    ? ConfigUtils.getDynamicContentPath()
                    : ConfigUtils.getAbsoluteAssetsRootPath());


	public static Tuple2<String,String> getDeclaredDefaultLanguage(){
		final String langCode = Config.getStringProperty(DEFAULT_LANGUAGE_CODE, "en")
				.toLowerCase();
		final String countryCode = Config.getStringProperty(DEFAULT_LANGUAGE_COUNTRY_CODE, "US")
				.toLowerCase();

		return Tuple.of(langCode, countryCode);

	}

	/**
	 * Checks the status of a feature flag.
	 * @param featureFlagName
	 * @return Boolean with the feature flag value. If not set, true is returned by default
	 */
	public static boolean isFeatureFlagOn(final String featureFlagName) {
		return Config.getBooleanProperty(featureFlagName, true);
	}

	/**
	 * Returns the current system-wide default email headers.
	 * <p>
	 * This method dynamically reads and parses the "DEFAULT_EMAIL_HEADERS" configuration property every time it is called.
	 * The expected format is a comma-separated list of header key/value pairs, where each pair is separated by a colon.
	 * For example: "X-Custom-1:Value1, X-Custom-2:Value2".
	 * </p>
	 *
	 * @return a Map containing the default email headers; if the configuration is not set or empty, an empty map is returned.
	 */
	public static Map<String, String> getDefaultEmailHeaders() {
		Map<String, String> headers = new HashMap<>();
		String headerConfig = Config.getStringProperty("DEFAULT_EMAIL_HEADERS", null);
		if (headerConfig != null && !headerConfig.trim().isEmpty()) {
			String[] headerPairs = headerConfig.split(StringPool.COMMA);
			for (String pair : headerPairs) {
				String[] keyValue = pair.split(StringPool.COLON, 2);
				if (keyValue.length == 2) {
					headers.put(keyValue[0].trim(), keyValue[1].trim());
				}
			}
		}
		return headers;
	}
}
