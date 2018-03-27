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

	// Query util methods
	@VisibleForTesting
	static final String[] SPECIAL_CHARS = new String[] { "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "?",
			":", "\\" };

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

	public static String getYamlConfiguration(String esPathHome){
		String yamlPath = System.getenv("ES_PATH_CONF");
		if (UtilMethods.isSet(yamlPath)  && FileUtil.exists(yamlPath)){
			return yamlPath;
		}else{
			return esPathHome + File.separator + "config" + File.separator + "elasticsearch.yml";
		}
	}

	public static Builder getExtSettingsBuilder() throws IOException {

		String esPathHome = Config.getStringProperty(ESClient.HOME_PATH, "WEB-INF/elasticsearch");
		String yamlPath = System.getenv("ES_PATH_CONF");
		if (!UtilMethods.isSet(yamlPath) || !FileUtil.exists(yamlPath)){
			//Get elasticsearch-ext.yml from default location
			yamlPath = esPathHome + File.separator + "config" +  File.separator + "elasticsearch-ext.yml";
		} else{
			//Otherwise, get parent directory from the ES_PATH_CONF
			yamlPath = new File(yamlPath).getParent() + File.separator + "elasticsearch-ext.yml";
		}
		Path settingsPath = Paths.get(yamlPath);
		if (Files.exists(settingsPath)){
			return Settings.builder().loadFromPath(settingsPath);
		}
		return Settings.builder();
	}
	
}