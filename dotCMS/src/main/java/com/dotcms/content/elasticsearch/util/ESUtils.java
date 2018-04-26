package com.dotcms.content.elasticsearch.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.google.common.base.CharMatcher;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;

public class ESUtils {

	public static final String ES_PATH_HOME = "es.path.home";
	// Query util methods
	@VisibleForTesting
	static final String[] SPECIAL_CHARS = new String[] { "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "?",
			":", "\\" };
	public static final String ES_PATH_HOME_DEFAULT_VALUE = "WEB-INF/elasticsearch";
	private static final String ES_CONFIG_DIR = "config";
	private static final String ES_YML_FILE = "elasticsearch.yml";
	private static final String ES_EXT_YML_FILE = "elasticsearch-override.yml";

	public static String escape(final String text) {

		String escapedText;

		if(CharMatcher.WHITESPACE.matchesAnyOf(text)) {
			escapedText = "\"" +text + "\"";
		} else {
			escapedText = text;
			for (int i = SPECIAL_CHARS.length - 1; i >= 0; i--) {
				escapedText = StringUtils.replace(escapedText, SPECIAL_CHARS[i], "\\" + SPECIAL_CHARS[i]);
			}
		}

		return escapedText;
	}

	public static String getESPathHome() {
		String esPathHome = Config
				.getStringProperty(ESUtils.ES_PATH_HOME, ESUtils.ES_PATH_HOME_DEFAULT_VALUE);

		esPathHome =
				!new File(esPathHome).isAbsolute() ? FileUtil.getRealPath(esPathHome) : esPathHome;

		return esPathHome;
	}

	public static String getYamlConfiguration(){
		final String yamlPath = System.getenv("ES_PATH_CONF");
		if (UtilMethods.isSet(yamlPath)  && FileUtil.exists(yamlPath)){
			return yamlPath;
		}else{
			return getESPathHome() + File.separator + ES_CONFIG_DIR + File.separator + ES_YML_FILE;
		}
	}

	public static Builder getExtSettingsBuilder() throws IOException {

		String yamlPath = System.getenv("ES_PATH_CONF");
		if (!UtilMethods.isSet(yamlPath) || !FileUtil.exists(yamlPath)){
			//Get elasticsearch-override.yml from default location
			yamlPath = getESPathHome() + File.separator + ES_CONFIG_DIR +  File.separator + ES_EXT_YML_FILE;
		} else{
			//Otherwise, get parent directory from the ES_PATH_CONF
			yamlPath = new File(yamlPath).getParent() + File.separator + ES_EXT_YML_FILE;
		}
		final Path settingsPath = Paths.get(yamlPath);
		if (Files.exists(settingsPath)){
			return Settings.builder().loadFromPath(settingsPath);
		}
		return Settings.builder();
	}
	
}