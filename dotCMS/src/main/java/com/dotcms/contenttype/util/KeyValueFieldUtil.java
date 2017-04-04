package com.dotcms.contenttype.util;

import java.util.Map;

/**
 * Utility class to handle Key Value field
 *
 * @author Roger
 * @deprecated Please use the one in the package com.dotmarketing.portlets.structure.model package
 */
public class KeyValueFieldUtil {

	public static Map<String, Object> JSONValueToHashMap(final String json) {
	    return com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(json);
	}

}