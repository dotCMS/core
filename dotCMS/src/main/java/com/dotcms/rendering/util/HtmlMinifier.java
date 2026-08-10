package com.dotcms.rendering.util;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Elements that generate no box at all ({@code display: none}). They are <b>transparent</b> to
     * whitespace collapsing in exactly the way a removed comment is: the element paints nothing, so
     * the whitespace on either side of it ends up separating the surrounding content and is
     * rendered. Dropping it joins words, which is why
     * {@code hola <script>x</script> mundo} must not become {@code hola<script>x</script>mundo}.
     * <p>
     * Keyed on the tag name, so it cannot see an element hidden by the {@code hidden} attribute or
     * by {@code style="display:none"}. Both leave the same corruption reachable; see the limitations
     * note on the pull request.
     */
    private static final Set<String> INVISIBLE_TAGS =
            Set.of("script", "style", "template", "noscript", "dialog");

    /**
     * Elements that participate in inline layout, where whitespace between tags is rendered and is
     * therefore significant. Whitespace touching any element <i>not</i> in this set can be removed
     * outright; whitespace between two inline elements is collapsed to a single space instead.
     */
    private static final Set<String> INLINE_TAGS = Set.of(
            "a", "abbr", "acronym", "audio", "b", "bdi", "bdo", "big", "br", "button", "canvas",
            "cite", "code", "data", "datalist", "del", "dfn", "em", "embed", "font", "i", "iframe",
            "img", "input", "ins", "kbd", "label", "map", "mark", "math", "meter", "nobr", "object",
            "output", "picture", "progress", "q", "rp", "rt", "rtc", "ruby", "s", "samp", "select",
            "slot", "small", "span", "strike", "strong", "sub", "sup", "svg", "textarea", "time",
            "tt", "u", "var", "video", "wbr");

    /**
     * Every element beside which whitespace is painted, and therefore must survive: the inline-level
     * ones because they sit in an inline formatting context, and the invisible ones because they
     * generate no box, so the whitespace either side of them separates whatever surrounds them.
     * <p>
     * Whitespace touching an element <i>not</i> in this set can be removed outright.
     */
    private static final Set<String> WHITESPACE_SIGNIFICANT_TAGS =
            Stream.concat(INLINE_TAGS.stream(), INVISIBLE_TAGS.stream())
                    .collect(Collectors.toUnmodifiableSet());

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

        return minifyBestEffort(html);
    }

    /**
     * Minifies without consulting the feature flag, for callers that have already established it is
     * on and must not read it twice.
     * <p>
     * Reading it once matters for two reasons. Each read probes the system table for two keys that
     * normally do not exist, and while those misses are served from memory today, an operator who
     * adds a remote provider to {@code cache.default.chain} turns them into network round trips per
     * render. It also closes the window where the flag flipping between two reads would buffer a
     * whole page and then serve it unminified anyway.
     * <p>
     * Minification stays strictly best-effort: any failure returns the original markup, so a bug
     * here cannot take a page down.
     *
     * @param html the rendered markup, may be {@code null}
     * @return the minified markup, or the original when empty or on error
     */
    public static String minifyBestEffort(final String html) {

        if (null == html || html.isEmpty() || !looksLikeHtml(html)) {
            return html;
        }

        return Try.of(() -> minify(html))
                .onFailure(e -> Logger.warnAndDebug(HtmlMinifier.class,
                        "Unable to minify HTML, serving it unmodified: " + e.getMessage(), e))
                .getOrElse(html);
    }

    /**
     * Decides whether a payload is HTML at all, because these seams do not only carry HTML: a VTL
     * page can render JSON, XML or CSV through exactly the same code path.
     * <p>
     * Collapsing whitespace in those changes data rather than formatting. A run of spaces inside a
     * JSON string is part of the value, and a newline in CSV separates records, so minifying a CSV
     * body merges every row onto one line. Nothing is lost by skipping them either, since a payload
     * with no markup has no insignificant whitespace to remove in the first place.
     * <p>
     * Deliberately errs towards <i>not</i> minifying: an XML document is skipped on its declaration,
     * because whitespace in XML text nodes is significant. The residual case it cannot see is a
     * non-HTML payload that happens to contain markup, such as JSON carrying an HTML fragment in a
     * string value. Gating on the response content type would be the stronger check, but
     * {@code VelocityLiveMode} currently calls {@code setContentType(CHARSET)} with a charset rather
     * than a media type, so that signal is not usable as it stands.
     *
     * @param content the rendered payload, never {@code null} or empty
     * @return {@code true} when the payload carries markup and is safe to minify
     */
    private static boolean looksLikeHtml(final String content) {

        if (content.stripLeading().regionMatches(true, 0, "<?xml", 0, 5)) {
            return false;
        }

        for (int index = 0; index < content.length(); index++) {
            if (content.charAt(index) == '<' && isMarkupStart(content, index)) {
                return true;
            }
        }

        return false;
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
                    // Emit the pending space first, as the preserve and markup branches below do.
                    // A conditional comment is markup only to browsers nobody ships any more; every
                    // modern engine treats it as a comment, paints nothing, and renders the
                    // whitespace either side of it. Dropping it here joined words, and only looked
                    // right when whitespace on the far side happened to put the space back.
                    if (pendingSpace) {
                        out.append(' ');
                    }
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
                if (pendingSpace && keepsAdjacentWhitespace(preserveTag)) {
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
        return null == lastTag || keepsAdjacentWhitespace(lastTag);
    }

    /**
     * Decides whether whitespace is significant given what follows it. It is significant when the
     * following content is text, or the opening/closing tag of an inline element.
     * <p>
     * A comment that is about to be removed is <b>transparent</b>: it cannot keep a space alive on
     * its own, but neither can it kill one, so the decision is made on whatever follows it. Judging
     * the comment itself would drop the space in {@code <span>a</span> <!--c--><span>b</span>} and
     * render "ab" where the source renders "a b".
     *
     * @return {@code true} when a separating space must be kept
     */
    private static boolean isSignificantAfter(final String html, final int index) {

        // Looping rather than recursing, so a run of consecutive comments cannot grow the stack.
        int cursor = index;
        while (cursor < html.length()) {

            if (!isMarkupStart(html, cursor)) {
                // Followed by text content, which includes a bare '<' such as in `a < b`.
                return true;
            }

            if (!html.startsWith("<!--", cursor)) {
                return keepsAdjacentWhitespace(tagNameAt(html, cursor));
            }

            if (html.startsWith("<!--[if", cursor)) {
                // Conditional comments are kept, so they count as markup in their own right.
                return true;
            }

            final int commentEnd = html.indexOf("-->", cursor);
            if (commentEnd < 0) {
                // An unterminated comment runs to the end of the document, taking the space with it.
                return false;
            }

            cursor = commentEnd + 3;
        }

        // Trailing whitespace at end of document.
        return false;
    }

    /**
     * @param tagName a lower cased tag name, may be {@code null} for degenerate markup like
     * {@code </>}
     * @return {@code true} when whitespace beside this tag is rendered and must be kept
     */
    private static boolean keepsAdjacentWhitespace(final String tagName) {
        return null != tagName && WHITESPACE_SIGNIFICANT_TAGS.contains(tagName);
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
