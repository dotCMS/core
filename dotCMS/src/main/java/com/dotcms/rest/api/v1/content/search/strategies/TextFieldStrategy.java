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
                    .map(token -> this.tokenClause(fieldContext.contentType(), fieldName, token,
                            finalWildcard, isWildcard))
                    .collect(Collectors.joining(SPACE));
        }
        return luceneQuery;
    }

    /**
     * Builds the clause for a single token, adding a {@code path} alternative for the URL field of an
     * asset whose URL is <b>derived from its path</b> rather than stored in the field being queried.
     *
     * <p>A Page's {@code url} field indexes only the page's own last path segment -- {@code index},
     * not {@code /store/index} -- and a File Asset's {@code fileName} indexes {@code logo.png}, not
     * {@code /application/themes/travel/images/logo.png}. Both render the <i>full path</i> in the
     * listing, so someone filtering by what they can see is querying a value the field never held.
     * The full path <i>is</i> indexed, as {@code path}.</p>
     *
     * <p>What the token looks like decides how the two are combined:</p>
     * <ul>
     *     <li><b>It contains a slash</b> ({@code /store/index}): the {@code path} clause
     *     <b>replaces</b> the field clauses. The field cannot hold a slash, so they carry no signal --
     *     and worse than none, because the analyzed clause does not simply fail, it is analyzed into
     *     the separate segments and <i>broadens</i>. OR-ing it in returned every page named
     *     {@code index} for a term of {@code /store/index}.</li>
     *     <li><b>It has no slash</b> ({@code store}): the {@code path} clause is OR-ed in
     *     <b>alongside</b> them, because now both are meaningful -- the field matches an asset
     *     <i>named</i> for the term, the path matches one <i>sitting in a folder</i> named for it.
     *     Requiring a leading slash to reach the second group would be an invisible rule: the listing
     *     shows a path, so typing a piece of it is the obvious thing to do, and {@code store} used to
     *     return nothing at all.</li>
     * </ul>
     *
     * <p>The token keeps its slashes and goes in whole. {@code path} holds the entire path as a single
     * term, so a contains-style wildcard over it matches exactly what was typed. Splitting the token
     * on the slash and joining the segments with {@code *} wildcards -- which an earlier version of
     * this did -- was both more expensive (an automaton branch per segment rather than a single
     * literal) and <i>less</i> precise, because it discarded the adjacency the slashes encode: a term
     * of {@code /images/} then also matched a file merely <b>named</b> {@code Images.vtl} rather than
     * one inside an {@code images} folder.</p>
     *
     * @param contentType   The Content Type the field belongs to.
     * @param fieldName     The fully qualified field name, e.g. {@code htmlpageasset.url}.
     * @param rawToken      The raw, unescaped user term.
     * @param wildcard      The delimiter wrapping the term: {@code *} for contains, {@code "} for an
     *                      explicit phrase.
     * @param isWildcard    Whether this is the wildcard contains case.
     *
     * @return The complete Lucene clause for this token.
     */
    private String tokenClause(final ContentType contentType, final String fieldName,
                               final String rawToken, final String wildcard,
                               final boolean isWildcard) {
        final String term = isWildcard
                ? LuceneQueryUtils.escapeForWildcardTerm(rawToken) : rawToken;
        final boolean pathDerived = isWildcard
                && this.isPathDerivedUrlField(contentType, fieldName);
        if (pathDerived && rawToken.contains(SLASH)) {
            return String.format("+%s:*%s*", PATH_FIELD, term);
        }
        final String pathAlternative = pathDerived
                ? String.format(" %s:*%s*", PATH_FIELD, term) : BLANK;
        return String.format("+(%s:%s%s%s %s_dotraw:%s%s%s%s)",
                fieldName, wildcard, term, wildcard,
                fieldName, wildcard, term, wildcard, pathAlternative);
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
