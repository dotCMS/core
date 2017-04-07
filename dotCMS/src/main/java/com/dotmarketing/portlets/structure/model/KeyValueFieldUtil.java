package com.dotmarketing.portlets.structure.model;

import java.util.Map;

/**
 * Utility class to handle Key Value field
 *
 * @author Roger
 * @deprecated Please use the one in the com.dotcms.contenttype.util package
 */
public class KeyValueFieldUtil {

    public static Map<String, Object> JSONValueToHashMap ( final String json ) {
        return com.dotcms.contenttype.util.KeyValueFieldUtil.JSONValueToHashMap(json);
    }

}