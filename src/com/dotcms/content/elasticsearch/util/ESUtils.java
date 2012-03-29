package com.dotcms.content.elasticsearch.util;

import org.apache.commons.lang.StringUtils;

public class ESUtils {

	// Query util methods
	private static final String[] SPECIAL_CHARS = new String[] { "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "?",
			":", "\\" };

	public static String escape(String text) {
		for (int i = SPECIAL_CHARS.length - 1; i >= 0; i--) {
			text = StringUtils.replace(text, SPECIAL_CHARS[i], "\\" + SPECIAL_CHARS[i]);
		}

		return text;
	}


}
