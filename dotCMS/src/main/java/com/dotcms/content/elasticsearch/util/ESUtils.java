package com.dotcms.content.elasticsearch.util;

import com.google.common.base.CharMatcher;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;


public class ESUtils {

	public static String escape(final String text) {

		StringBuilder escapedText = new StringBuilder(QueryParser.escape(text));

		if(CharMatcher.whitespace().matchesAnyOf(text)) {
			escapedText.insert(0,"\"").append("\"");
		}

		return escapedText.toString();
	}

}