package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.util.LuceneQueryUtils;

/**
 * This Field Strategy implementation specifies the correct syntax for querying a Global Search
 * Field via Lucene query in dotCMS. The global search is exposed as the main search box that users
 * can see in the {@code Search} portlet, the dynamic search dialog in the {@code Relationships}
 * field, and any other portlet that consumes the Lucene Query Builder service. This particular
 * Strategy does not belong to a specific Content Type field, but to the generic way of finding any
 * content that matches the entered value.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class GlobalSearchAttributeStrategy implements FieldStrategy {

    /** This is the RegEx used to split the values of the field into tokens */
    private static final String VALUE_SPLIT_REGEX = "[,|\\s+]";

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        final String rawValue = fieldContext.fieldValue().toString();
        // Escape ONCE, up front, and use the escaped value in EVERY clause below — including the
        // mandatory gate. Previously only the trailing clause was escaped, so a term containing a
        // reserved character (a colon, a parenthesis, a slash) produced a gate that Elasticsearch
        // could not parse; the failure was logged and discarded and the user was told their content
        // did not exist (issue #37532, customer ticket 39185).
        //
        // LuceneQueryUtils.escape replaces a private regex that was missing "/" entirely. It is the
        // same helper TextFieldStrategy uses, so the two strategies no longer disagree about what
        // the reserved set is, and it is a vendor-neutral character walk rather than a regex —
        // which matters for the ES→OpenSearch migration (ADR-0009 flags Lucene 10 changes to
        // special-character handling).
        //
        // The "*" wildcards appended below are syntax this strategy adds itself, so they are
        // appended AFTER escaping and stay live rather than becoming literal asterisks.
        final String value = LuceneQueryUtils.escape(rawValue);
        final StringBuilder luceneQuery = new StringBuilder();
        // Mandatory gate: match either a catchall token PREFIX (fast, existing behavior) OR the
        // fieldName_dotraw raw value via wildcard. Unlike catchall (which aggregates every field
        // of the document), _dotraw is scoped to this one field, so this alternative recovers
        // mid-token and exact-full-value matches (issue #36791) — e.g. a file named
        // "IMG_1004.jpeg" tokenizes to "img_1004"+"jpeg", so a "1004" or a full-name search never
        // satisfies a catchall-only prefix gate — without reintroducing an unscoped,
        // whole-document wildcard like the old broad catchall:*value* (issue #36688).
        // Boosts are deliberately asymmetric: catchall (a real token-prefix hit) outranks
        // _dotraw (a raw substring hit that can land anywhere, including mid-word) so a document
        // found only via the substring fallback still ranks below a genuine prefix match.
        luceneQuery.append("+(catchall:").append(value).append("*^10 OR ")
                .append(fieldName).append("_dotraw:*").append(value).append("*^2)").append(" ");
        luceneQuery.append(fieldName).append(":'").append(value).append("'^15").append(" ");
        // Tokenize the RAW value so the split sees the user's real separators, then escape each
        // token individually. Empty tokens are dropped: consecutive separators used to emit a
        // term-less "title:^5" clause that could not parse (FR-028).
        final String[] titleSplit = rawValue.split(VALUE_SPLIT_REGEX);
        if (titleSplit.length > 1) {
            for (final String term : titleSplit) {
                if (term.isEmpty()) {
                    continue;
                }
                luceneQuery.append(fieldName).append(":")
                        .append(LuceneQueryUtils.escape(term)).append("^5").append(" ");
            }
        }
        luceneQuery.append("title:").append(value).append("*");
        return luceneQuery.toString();
    }

}
