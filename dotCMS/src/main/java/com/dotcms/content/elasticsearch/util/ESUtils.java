package com.dotcms.content.elasticsearch.util;

import com.google.common.base.CharMatcher;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;

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


}
