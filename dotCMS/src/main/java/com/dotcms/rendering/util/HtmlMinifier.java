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
    private static final String[] PRESERVE_TAGS = {"pre", "textarea", "script", "style"};

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

        while (index < length) {

            final char current = html.charAt(index);

            if (Character.isWhitespace(current)) {
                // Collapse the run, deferring the write: the space is only emitted if the content
                // that follows turns out to need it, so trailing whitespace and whitespace around
                // block-level tags is dropped entirely.
                int lookahead = index;
                while (lookahead < length && Character.isWhitespace(html.charAt(lookahead))) {
                    lookahead++;
                }
                pendingSpace = out.length() > 0
                        && isSignificantBefore(out)
                        && isSignificantAfter(html, lookahead);
                index = lookahead;
                continue;
            }

            if (html.startsWith("<!--", index)) {
                // Drop comments entirely, but keep conditional comments, which are markup.
                final int end = html.indexOf("-->", index);
                final int commentEnd = end < 0 ? length : end + 3;
                if (html.startsWith("<!--[if", index) || html.startsWith("<![endif]", index)) {
                    out.append(html, index, commentEnd);
                    pendingSpace = false;
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
                continue;
            }

            if (pendingSpace) {
                out.append(' ');
                pendingSpace = false;
            }
            out.append(current);
            index++;
        }

        return out.toString();
    }

    /**
     * Decides whether whitespace is significant given what precedes it. It is significant when the
     * preceding content is text, or the closing/opening tag of an inline element.
     *
     * @return {@code true} when a separating space must be kept
     */
    private static boolean isSignificantBefore(final StringBuilder out) {

        final int lastChar = out.length() - 1;
        if (out.charAt(lastChar) != '>') {
            // Preceded by text content.
            return true;
        }

        final int tagStart = out.lastIndexOf("<");
        if (tagStart < 0) {
            return true;
        }

        return isInlineTag(out.substring(tagStart, out.length()));
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

        if (html.charAt(index) != '<') {
            // Followed by text content.
            return true;
        }

        if (html.startsWith("<!--", index)) {
            // A comment is about to be removed; it must not keep the space alive on its own.
            return html.startsWith("<!--[if", index);
        }

        final int tagEnd = html.indexOf('>', index);

        return isInlineTag(tagEnd < 0 ? html.substring(index) : html.substring(index, tagEnd + 1));
    }

    /**
     * @param tag a full tag such as {@code <span class="x">} or {@code </div>}
     * @return {@code true} when the tag denotes an inline element
     */
    private static boolean isInlineTag(final String tag) {

        int start = 1;
        if (start < tag.length() && tag.charAt(start) == '/') {
            start++;
        }

        int end = start;
        while (end < tag.length() && !isTagNameBoundary(tag.charAt(end))) {
            end++;
        }

        return end > start && INLINE_TAGS.contains(tag.substring(start, end).toLowerCase());
    }

    /**
     * @return the preserved tag name starting at {@code index}, or {@code null} if there is none
     */
    private static String matchPreserveTag(final String html, final int index) {

        if (html.charAt(index) != '<') {
            return null;
        }

        for (final String tag : PRESERVE_TAGS) {
            final int nameEnd = index + 1 + tag.length();
            if (nameEnd <= html.length()
                    && html.regionMatches(true, index + 1, tag, 0, tag.length())
                    // Ensure it is the whole tag name, not a prefix such as <scriptural>.
                    && (nameEnd == html.length() || isTagNameBoundary(html.charAt(nameEnd)))) {
                return tag;
            }
        }

        return null;
    }

    /**
     * @return {@code true} when the character terminates a tag name
     */
    private static boolean isTagNameBoundary(final char character) {
        return character == '>' || character == '/' || Character.isWhitespace(character);
    }

    /**
     * Finds the index just past the closing tag of a preserved element. Falls back to the end of the
     * document when the markup is unbalanced, so malformed input still round-trips safely.
     *
     * @return the exclusive end index of the preserved region
     */
    private static int findPreserveTagEnd(final String html, final int index, final String tag) {

        final String closeTag = "</" + tag;
        int cursor = index + 1;

        while (cursor < html.length()) {
            final int candidate = indexOfIgnoreCase(html, closeTag, cursor);
            if (candidate < 0) {
                return html.length();
            }
            final int end = html.indexOf('>', candidate);
            if (end < 0) {
                return html.length();
            }
            return end + 1;
        }

        return html.length();
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
