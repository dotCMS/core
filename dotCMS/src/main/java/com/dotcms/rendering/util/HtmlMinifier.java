package com.dotcms.rendering.util;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.Set;

/**
 * Minifies rendered HTML before it is written to the response (and, for LIVE mode, before it is
 * stored in the page cache).
 * <p>
 * This is a deliberately conservative, dependency-free minifier: it only collapses whitespace that
 * is guaranteed to be insignificant in HTML. It does <b>not</b> attempt to minify CSS or JavaScript,
 * rewrite attributes, or strip optional end tags.
 * <p>
 * Content inside {@code <pre>}, {@code <textarea>}, {@code <script>} and {@code <style>} is copied
 * through untouched, since whitespace and newlines are significant there (rendered output for the
 * first two, and JavaScript automatic semicolon insertion for {@code <script>}).
 *
 * @see FeatureFlagName#FEATURE_FLAG_MINIFY_HTML
 */
public class HtmlMinifier {

    /**
     * Tags whose text content must be preserved byte-for-byte.
     */
    private static final Set<String> PRESERVE_TAGS = Set.of("pre", "textarea", "script", "style");

    /**
     * Elements that participate in inline layout, where whitespace between tags is rendered and is
     * therefore significant. Whitespace touching any element <i>not</i> in this set can be removed
     * outright; whitespace between two inline elements is collapsed to a single space instead.
     */
    private static final Set<String> INLINE_TAGS = Set.of(
            "a", "abbr", "b", "bdi", "bdo", "big", "br", "button", "cite", "code", "data",
            "datalist", "del", "dfn", "em", "font", "i", "img", "input", "ins", "kbd", "label",
            "map", "mark", "meter", "nobr", "object", "output", "picture", "progress", "q", "ruby",
            "s", "samp", "select", "slot", "small", "span", "strike", "strong", "sub", "sup",
            "textarea", "time", "tt", "u", "var", "wbr");

    private HtmlMinifier() {
        // utility
    }

    /**
     * @return whether HTML minification is enabled. Defaults to {@code false} (opt-in).
     */
    public static boolean isEnabled() {
        return Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_MINIFY_HTML, false);
    }

    /**
     * Minifies the given HTML if the feature flag is on; otherwise returns it unchanged.
     * <p>
     * Minification is strictly best-effort: any failure returns the original markup so that a bug
     * here can never take a page down.
     *
     * @param html the rendered markup, may be {@code null}
     * @return the minified markup, or the original when disabled, empty or on error
     */
    public static String minifyIfEnabled(final String html) {

        if (null == html || html.isEmpty() || !isEnabled()) {
            return html;
        }

        return Try.of(() -> minify(html))
                .onFailure(e -> Logger.warnAndDebug(HtmlMinifier.class,
                        "Unable to minify HTML, serving it unmodified: " + e.getMessage(), e))
                .getOrElse(html);
    }

    /**
     * Collapses insignificant whitespace outside of comments and whitespace-sensitive elements.
     *
     * @param html the markup to minify, never {@code null}
     * @return the minified markup
     */
    public static String minify(final String html) {

        final StringBuilder out = new StringBuilder(html.length());
        final int length = html.length();
        int index = 0;
        // Tracks whether the last character emitted was a collapsed whitespace run, so that
        // whitespace spanning a tag boundary does not produce two spaces.
        boolean pendingSpace = false;
        // Name of the tag emitted last, or null when text was emitted last. Tracked as we go rather
        // than recovered by scanning the output backwards, so that a literal '>' in text content is
        // never mistaken for the end of a tag.
        String lastTag = null;

        while (index < length) {

            final char current = html.charAt(index);

            if (isHtmlWhitespace(current)) {
                // Collapse the run, deferring the write: the space is only emitted if the content
                // that follows turns out to need it, so trailing whitespace and whitespace around
                // block-level tags is dropped entirely.
                int lookahead = index;
                while (lookahead < length && isHtmlWhitespace(html.charAt(lookahead))) {
                    lookahead++;
                }
                pendingSpace = out.length() > 0
                        && isSignificantBefore(lastTag)
                        && isSignificantAfter(html, lookahead);
                index = lookahead;
                continue;
            }

            if (html.startsWith("<!--", index)) {
                // Drop comments entirely, but keep conditional comments, which are markup.
                final int end = html.indexOf("-->", index);
                final int commentEnd = end < 0 ? length : end + 3;
                if (html.startsWith("<!--[if", index)) {
                    out.append(html, index, commentEnd);
                    pendingSpace = false;
                    // Treated as text so that whitespace around it is kept, which is the safe
                    // reading: what the conditional comment wraps is unknown at this point.
                    lastTag = null;
                }
                index = commentEnd;
                continue;
            }

            final String preserveTag = matchPreserveTag(html, index);
            if (null != preserveTag) {
                // Copy the element, including its content and closing tag, verbatim.
                final int end = findPreserveTagEnd(html, index, preserveTag);
                if (pendingSpace && INLINE_TAGS.contains(preserveTag)) {
                    out.append(' ');
                }
                pendingSpace = false;
                out.append(html, index, end);
                index = end;
                lastTag = preserveTag;
                continue;
            }

            if (isMarkupStart(html, index)) {
                // Copy the tag as a unit: whitespace inside a quoted attribute value is part of the
                // value and must survive, so it can not go through the text path above.
                if (pendingSpace) {
                    out.append(' ');
                    pendingSpace = false;
                }
                lastTag = tagNameAt(html, index);
                index = appendTag(html, index, out);
                continue;
            }

            if (pendingSpace) {
                out.append(' ');
                pendingSpace = false;
            }
            out.append(current);
            index++;
            lastTag = null;
        }

        return out.toString();
    }

    /**
     * Copies a tag verbatim from {@code index} (which must point at the opening {@code <}) through
     * its closing {@code >}, collapsing only the whitespace that separates attributes.
     * <p>
     * Quoting is tracked so that whitespace <i>inside</i> an attribute value is copied byte-for-byte
     * -- collapsing it would silently rewrite form values, JSON data attributes and {@code alt}
     * text -- and so that a {@code >} inside a quoted value does not end the tag early.
     *
     * @return the index just past the tag, or the end of the document when the tag is unterminated
     */
    private static int appendTag(final String html, final int index, final StringBuilder out) {

        final int length = html.length();
        int cursor = index;
        char openQuote = 0;

        while (cursor < length) {

            final char current = html.charAt(cursor);

            if (openQuote != 0) {
                // Inside an attribute value: everything, whitespace included, is content.
                out.append(current);
                if (current == openQuote) {
                    openQuote = 0;
                }
                cursor++;
                continue;
            }

            if (current == '"' || current == '\'') {
                openQuote = current;
                out.append(current);
                cursor++;
                continue;
            }

            if (current == '>') {
                out.append(current);
                return cursor + 1;
            }

            if (isHtmlWhitespace(current)) {
                int lookahead = cursor;
                while (lookahead < length && isHtmlWhitespace(html.charAt(lookahead))) {
                    lookahead++;
                }
                // One space is always enough to separate attributes, and none is needed when the
                // tag ends right after, as in {@code <div class="a" >}. The space before a self
                // closing '/' is kept, since dropping it would append the slash to an unquoted
                // attribute value.
                if (lookahead < length && html.charAt(lookahead) != '>') {
                    out.append(' ');
                }
                cursor = lookahead;
                continue;
            }

            out.append(current);
            cursor++;
        }

        return length;
    }

    /**
     * Decides whether whitespace is significant given what precedes it. It is significant when the
     * preceding content is text, or the closing/opening tag of an inline element.
     *
     * @param lastTag the name of the tag emitted last, or {@code null} when text was emitted last
     * @return {@code true} when a separating space must be kept
     */
    private static boolean isSignificantBefore(final String lastTag) {
        return null == lastTag || isInlineTag(lastTag);
    }

    /**
     * Decides whether whitespace is significant given what follows it. It is significant when the
     * following content is text, or the opening/closing tag of an inline element.
     *
     * @return {@code true} when a separating space must be kept
     */
    private static boolean isSignificantAfter(final String html, final int index) {

        if (index >= html.length()) {
            // Trailing whitespace at end of document.
            return false;
        }

        if (!isMarkupStart(html, index)) {
            // Followed by text content, which includes a bare '<' such as in `a < b`.
            return true;
        }

        if (html.startsWith("<!--", index)) {
            // A comment is about to be removed; it must not keep the space alive on its own.
            return html.startsWith("<!--[if", index);
        }

        return isInlineTag(tagNameAt(html, index));
    }

    /**
     * @param tagName a lower cased tag name, may be {@code null} for degenerate markup like
     * {@code </>}
     * @return {@code true} when the tag denotes an inline element
     */
    private static boolean isInlineTag(final String tagName) {
        return null != tagName && INLINE_TAGS.contains(tagName);
    }

    /**
     * A {@code <} only opens markup when a tag name, {@code /}, {@code !} or {@code ?} follows it;
     * anywhere else it is literal text, as in {@code 3 < 4}.
     *
     * @return {@code true} when {@code index} points at the start of a tag, comment or declaration
     */
    private static boolean isMarkupStart(final String html, final int index) {

        if (html.charAt(index) != '<' || index + 1 >= html.length()) {
            return false;
        }

        final char next = html.charAt(index + 1);

        return Character.isLetter(next) || next == '/' || next == '!' || next == '?';
    }

    /**
     * @param index the position of the opening {@code <}
     * @return the lower cased tag name at {@code index}, or {@code null} when there is none
     */
    private static String tagNameAt(final String html, final int index) {

        int start = index + 1;
        if (start < html.length() && html.charAt(start) == '/') {
            start++;
        }

        int end = start;
        while (end < html.length() && !isTagNameBoundary(html.charAt(end))) {
            end++;
        }

        return end > start ? html.substring(start, end).toLowerCase() : null;
    }

    /**
     * @return the preserved tag name starting at {@code index}, or {@code null} if there is none
     */
    private static String matchPreserveTag(final String html, final int index) {

        if (!isMarkupStart(html, index) || html.charAt(index + 1) == '/') {
            return null;
        }

        // Matched on the whole name, so a prefix such as <scriptural> is not treated as <script>.
        final String name = tagNameAt(html, index);

        return null != name && PRESERVE_TAGS.contains(name) ? name : null;
    }

    /**
     * @return {@code true} when the character terminates a tag name
     */
    private static boolean isTagNameBoundary(final char character) {
        return character == '>' || character == '/' || isHtmlWhitespace(character);
    }

    /**
     * Whether the character is one of the five that HTML treats as collapsible whitespace.
     * <p>
     * {@link Character#isWhitespace(char)} is deliberately <b>not</b> used: it also matches
     * characters that HTML renders rather than collapses, such as the ideographic space
     * ({@code U+3000}) common in CJK copy and the thin space ({@code U+2009}). Collapsing those
     * would silently alter page content.
     *
     * @return {@code true} when the character is insignificant whitespace in HTML
     */
    private static boolean isHtmlWhitespace(final char character) {
        return character == ' ' || character == '\t' || character == '\n' || character == '\r'
                || character == '\f';
    }

    /**
     * Finds the index just past the closing tag of a preserved element. Falls back to the end of the
     * document when the markup is unbalanced, so malformed input still round-trips safely.
     *
     * @return the exclusive end index of the preserved region
     */
    private static int findPreserveTagEnd(final String html, final int index, final String tag) {

        final int closeTagStart = indexOfIgnoreCase(html, "</" + tag, index + 1);
        if (closeTagStart < 0) {
            return html.length();
        }

        final int end = html.indexOf('>', closeTagStart);

        return end < 0 ? html.length() : end + 1;
    }

    /**
     * @return the index of {@code needle} at or after {@code from}, ignoring case, or -1
     */
    private static int indexOfIgnoreCase(final String haystack, final String needle, final int from) {

        final int max = haystack.length() - needle.length();
        for (int i = from; i <= max; i++) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }

        return -1;
    }
}
