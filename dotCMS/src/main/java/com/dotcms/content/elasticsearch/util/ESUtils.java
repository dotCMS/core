package com.dotcms.content.elasticsearch.util;

import com.google.common.base.CharMatcher;
import com.google.common.hash.Hashing;
import java.nio.charset.Charset;
import org.apache.lucene.queryparser.classic.QueryParser;

public class ESUtils {

	public final static String SHA_256 = "_sha256";

	public static String escape(final String text) {

		final StringBuilder escapedText = new StringBuilder(QueryParser.escape(text));

		if(CharMatcher.whitespace().matchesAnyOf(text)) {
			escapedText.insert(0,"\"").append("\"");
		}

		return escapedText.toString();
	}

	public static String sha256(final String fieldName, final Object fieldValue,
			final long languageId) {
		return Hashing.sha256().hashString(fieldName + "_"
				+ (fieldValue == null ? "" : fieldValue.toString()) + "_"
				+ languageId, Charset.forName("UTF-8")).toString();
	}
}