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
					"=", " "
			).collect(Collectors.collectingAndThen(toSet(), Collections::unmodifiableSet));

	/**
	 * Characters that the Elasticsearch {@code query_string} parser reads as range operators
	 * ({@code field:<value}, {@code field:>=value}). Unlike the other reserved characters they cannot
	 * be backslash-escaped, so they are removed from the value instead.
	 */
	static final Set<Character> TO_REMOVE_COLLECTION = Set.of('<', '>');

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
	 * Makes a value safe to place after {@code field:} in a {@code query_string} query. Every reserved
	 * character is escaped with a preceding <code>\</code>, except:
	 * <ul>
	 *     <li>{@code /}, which is left as-is because URL map values may contain path segments;</li>
	 *     <li>{@code <} and {@code >}, which are removed. Elasticsearch treats them as range operators
	 *     and does not allow them to be escaped, so leaving them in turns an exact-match filter
	 *     into a range that matches unrelated content.</li>
	 * </ul>
	 * White space is escaped as well, so the value stays a single term. Based on
	 * {@link QueryParser#escape(String)}, which also omits {@code <} and {@code >}.
	 *
	 * @param toEscape the raw value, typically a segment captured from a URL map pattern
	 * @return the escaped value; empty if it contained nothing but {@code <} and {@code >}
	 */
	public static String escapeExcludingSlashIncludingSpace(final String toEscape) {

		final StringBuilder escapedString = new StringBuilder();
		for (int i = 0; i < toEscape.length(); i++) {
			final char c = toEscape.charAt(i);
			if (TO_REMOVE_COLLECTION.contains(c)) {
				continue;
			}
			// These characters are part of the query syntax and must be escaped
			if (TO_ESCAPE_COLLECTION.contains(String.valueOf(c))) {
				escapedString.append('\\');
			}
			escapedString.append(c);
		}
		return escapedString.toString();
	}

}