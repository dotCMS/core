package com.dotcms.contenttype.util;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.dotcms.util.marshal.MarshalFactory;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Utility class to handle Key Value field
 *
 * @author Roger
 */
public class KeyValueFieldUtil {

	public static Map<String, Object> JSONValueToHashMap(final String json) {

		LinkedHashMap<String, Object> keyValueMap = new LinkedHashMap<String, Object>();
		if (UtilMethods.isSet(json)) {

			if (json.equals(ContentletCache.CACHED_METADATA)) {

				Logger.error(KeyValueFieldUtil.class,
						"Trying to parse JSON content for cached Metadata, it is required first to search the data into the cache.");
				return keyValueMap;
			}

			// the following code fixes issue 10529
			String replacedJSJson;
			if (json.contains("\\")) {
				replacedJSJson = UtilMethods.replace(json, "\\", "&#92;");
			} else {
				replacedJSJson = json;
			}

			return MarshalFactory.getInstance().getMarshalUtils().unmarshal(replacedJSJson, Map.class);
		}
		return keyValueMap;
	}

}