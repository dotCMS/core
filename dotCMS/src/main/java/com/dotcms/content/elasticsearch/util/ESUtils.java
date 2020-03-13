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

	/**
	 * Returns a String where those characters that QueryParser expects to be escaped are escaped by
	 * a preceding <code>\</code> excluding the "/", we found some cases where we don't want to
	 * scape it.
	 * This method is a copy of the {@link QueryParser#escape(String)} where we remove the
	 * scape for slashes "/" and we included the scape for white spaces " "
	 */
	public static String escapeExcludingSlashIncludingSpace(final String s) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			// These characters are part of the query syntax and must be escaped
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
					|| c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}'
					|| c == '~'
					|| c == '*' || c == '?' || c == '|' || c == '&'
					|| c == ' ') {
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb.toString();
	}

}