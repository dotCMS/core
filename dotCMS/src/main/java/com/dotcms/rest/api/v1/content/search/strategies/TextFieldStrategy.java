package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.util.LuceneQueryUtils;
import com.dotmarketing.util.UtilMethods;
import com.google.common.base.CharMatcher;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.SPACE;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a Text Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class TextFieldStrategy implements FieldStrategy {

    /** This is the RegEx used to split the values of the field into tokens */
    private static final String VALUE_SPLIT_REGEX = "[,|\\s+]";

    /** The system field that holds an asset's full path as a single term. */
    private static final String PATH_FIELD = "path";
    private static final String SLASH = "/";
    private static final String URL_FIELD_VAR = "url";
    private static final String FILE_NAME_FIELD_VAR = "fileName";

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        String fieldValue = fieldContext.fieldValue().toString();
        String wildcard = "*";
        if (fieldValue.endsWith("\"") && fieldValue.startsWith("\"")) {
            wildcard = "\"";
            fieldValue = CharMatcher.is('\"').trimFrom(fieldValue).trim();
        }
        final String finalWildcard = wildcard;
        // Escape Lucene query-syntax characters in the user's term (a hyphen, colon, parenthesis,
        // etc. would otherwise fail to parse and break the whole search) — but only for the wildcard
        // contains case; an explicitly quoted phrase is left as-is. The `*` wildcards we add
        // ourselves stay outside the escaped token. The forward slash is deliberately NOT escaped
        // here: inside a wildcard term an escaped slash is matched literally and so can never match
        // a stored one. See LuceneQueryUtils.escapeForWildcardTerm.
        final boolean isWildcard = "*".equals(finalWildcard);
        final String luceneQuery;
        if (this.isFieldInURLMapPattern(fieldContext.contentType(), fieldName)) {
            luceneQuery = Arrays.stream(fieldValue.split(VALUE_SPLIT_REGEX))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(token -> isWildcard
                            ? LuceneQueryUtils.escapeForWildcardTerm(token) : token)
                    .map(token -> String.format("+%s_dotraw:%s%s%s",
                            fieldName, finalWildcard, token, finalWildcard))
                    .collect(Collectors.joining(SPACE));
        } else {
            luceneQuery = Arrays.stream(fieldValue.split(VALUE_SPLIT_REGEX))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(token -> {
                        // A path-derived URL field cannot hold the slash the user typed, so for such
                        // a term the field clauses can only add noise. The path clause replaces them.
                        final String pathClause = isWildcard
                                ? this.pathClause(fieldContext.contentType(), fieldName, token)
                                : BLANK;
                        if (!pathClause.isEmpty()) {
                            return pathClause;
                        }
                        final String term = isWildcard
                                ? LuceneQueryUtils.escapeForWildcardTerm(token) : token;
                        return String.format("+(%s:%s%s%s %s_dotraw:%s%s%s)",
                                fieldName, finalWildcard, term, finalWildcard,
                                fieldName, finalWildcard, term, finalWildcard);
                    })
                    .collect(Collectors.joining(SPACE));
        }
        return luceneQuery;
    }

    /**
     * Builds a {@code path} clause for the URL field of an asset whose URL is <b>derived from its
     * path</b> rather than stored in the field being queried.
     * <p>A Page's {@code url} field indexes only the page's own last path segment -- {@code index},
     * not {@code /store/index} -- and a File Asset's {@code fileName} indexes {@code logo.png}, not
     * {@code /application/themes/travel/images/logo.png}. Both render the full path in the listing,
     * so a user who types a path fragment is querying a value the field never held. The full path
     * <i>is</i> indexed, as {@code path}.</p>
     * <p>This applies only when the term contains a slash, and then it <b>replaces</b> the field
     * clauses rather than joining them with OR. Replacing matters: the analyzed field clause does not
     * simply fail to match a slash term, it is analyzed into its segments and <i>broadens</i>, so
     * OR-ing it in returned every page named {@code index} for a term of {@code /store/index}. Since
     * the field can never contain a slash, those clauses carry no signal for such a term, only
     * noise. A slash-free term can still match the field itself and is left exactly as it was, which
     * also means the term-dictionary scan this clause costs is paid only on terms that match nothing
     * useful today.</p>
     * <p>The slash becomes a wildcard <i>separator</i> instead of an escaped literal: {@code path}
     * holds the whole path as a single term, and an escaped slash matches nothing against it. Every
     * other Lucene operator inside a segment is still escaped, so the clause cannot be broken out
     * of. Note this makes the match "these segments, in order" rather than "these segments,
     * adjacent" -- consistent with the contains-style semantics of every other text filter here.</p>
     *
     * @param contentType The Content Type the field belongs to.
     * @param fieldName   The fully qualified field name, e.g. {@code htmlpageasset.url}.
     * @param token       The raw, unescaped user term.
     *
     * @return The complete {@code path} clause for this token, or a blank String when this field or
     * term does not need one and the regular field clauses should be used.
     */
    private String pathClause(final ContentType contentType, final String fieldName,
                              final String token) {
        if (!token.contains(SLASH) || !this.isPathDerivedUrlField(contentType, fieldName)) {
            return BLANK;
        }
        final String segments = Arrays.stream(token.split(SLASH))
                .filter(segment -> !segment.isEmpty())
                .map(LuceneQueryUtils::escapeForWildcardTerm)
                .collect(Collectors.joining("*"));
        // A lone slash leaves no segments. Every path contains one, so match any path rather than
        // emitting an empty term.
        return segments.isEmpty()
                ? "+" + PATH_FIELD + ":*"
                : String.format("+%s:*%s*", PATH_FIELD, segments);
    }

    /**
     * Determines whether the given field is the one whose value the asset's displayed URL is built
     * from, for the Base Types where that value is the path rather than a stored string.
     *
     * @param contentType The Content Type to check.
     * @param fieldName   The fully qualified field name.
     *
     * @return {@code true} for a Page's {@code url} and a File Asset's {@code fileName}.
     */
    private boolean isPathDerivedUrlField(final ContentType contentType, final String fieldName) {
        if (null == contentType || null == contentType.baseType()) {
            return false;
        }
        final String fieldVarName = fieldName.substring(fieldName.indexOf(".") + 1);
        switch (contentType.baseType()) {
            case HTMLPAGE:
                return URL_FIELD_VAR.equalsIgnoreCase(fieldVarName);
            case FILEASSET:
                return FILE_NAME_FIELD_VAR.equalsIgnoreCase(fieldVarName);
            default:
                return false;
        }
    }

    /**
     * This method checks if the field is part of the URL Map Pattern of the Content Type it belongs
     * to.
     *
     * @param contentType The Content Type to check.
     * @param fieldName   The field name to check.
     *
     * @return If the field is part of the URL Map patter, returns {@code true}.
     */
    private boolean isFieldInURLMapPattern(final ContentType contentType, final String fieldName) {
        final String fieldVarName = fieldName.substring(fieldName.indexOf(".") + 1);
        return null != contentType && UtilMethods.isSet(contentType.urlMapPattern())
                && contentType.urlMapPattern().contains("{" + fieldVarName + "}");
    }

}
