package com.dotcms.content.elasticsearch.util;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.CharMatcher;
import com.google.common.hash.Hashing;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.queryparser.classic.QueryParser;

public class ESUtils {

	public final static String SHA_256 = "_sha256";

	final static Set<String> TO_ESCAPE_COLLECTION =
			Stream.of("\\", "+", "-", "!", "(", ")", ":",
					"^", "[", "]", "\"", "{", "}",
					"~",
					"*", "?", "|", "&",
					" "
			).collect(Collectors.collectingAndThen(toSet(), Collections::unmodifiableSet));

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
				+ (fieldValue == null ? "" : fieldValue.toString().toLowerCase()) + "_"
				+ languageId, Charset.forName("UTF-8")).toString();
	}

	/**
	 * Returns a String where those characters that QueryParser expects to be escaped are escaped by
	 * a preceding <code>\</code> excluding the "/", we found some cases where we don't want to
	 * scape it.
	 * This method is a copy of the {@link QueryParser#escape(String)} where we remove the
	 * scape for slashes "/" and we included the scape for white spaces " "
	 */
	public static String escapeExcludingSlashIncludingSpace(final String toEscape) {

		final StringBuilder escapedString = new StringBuilder();
		for (int i = 0; i < toEscape.length(); i++) {
			final char c = toEscape.charAt(i);
			// These characters are part of the query syntax and must be escaped
			if (TO_ESCAPE_COLLECTION.contains(String.valueOf(c))) {
				escapedString.append('\\');
			}
			escapedString.append(c);
		}
		return escapedString.toString();
	}

}