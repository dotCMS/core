package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Replaces WYSIWYG, TextArea and Story Block field values in a transformed map with a
 * &lt;=150-character extracted plain-text preview, instead of the raw stored value.
 * <p>
 * WYSIWYG/TextArea values store raw HTML; the preview is {@code Jsoup.parse(html).text()},
 * truncated to 150 characters. Story Block values are, by the time this strategy runs, already
 * the {@link Map} (or raw-string/{@code null} fallback) that {@link StoryBlockViewStrategy}
 * produces -- this strategy walks that structure's {@code content} arrays recursively, collecting
 * every {@code text} leaf value, and truncates the concatenation to 150 characters. This is why
 * {@link TransformOptions#LONG_TEXT_PREVIEW} must be declared after {@code STORY_BLOCK_VIEW} and
 * {@code JSON_VIEW} in the enum -- {@code EnumSet} iteration order runs this strategy last.
 * <p>
 * A content type's title-source field is trimmed like any other in-scope field, including when
 * its variable is literally {@code "title"} -- {@link Contentlet#getTitle()} returns that field's
 * own raw value verbatim (it does not strip HTML or bound its length itself), so an untrimmed
 * long-text title defeats this strategy's entire purpose for that one field (issue #37185 QA
 * follow-up: a WYSIWYG/TextArea field used as the title rode through at full, untruncated length).
 * An earlier version of this strategy exempted the {@code "title"} key outright on the theory that
 * {@code COMMON_PROPS} had already populated it and it should not be touched again -- but "already
 * populated" here just means "holds the same raw value every other in-scope field starts from",
 * and the issue's own acceptance criteria call for a *correct*, not necessarily untouched, title
 * in this exact scenario.
 *
 * @since 25.xx
 */
public class LongTextPreviewStrategy extends AbstractTransformStrategy<Contentlet> {

    static final int MAX_PREVIEW_LENGTH = 150;

    /**
     * Upper bound, in characters, on how much of a raw HTML value is handed to {@link Jsoup#parse}
     * before flattening to text and truncating. {@code MAX_PREVIEW_LENGTH} characters of visible
     * text fit inside this budget with wide margin even for markup-heavy bodies, so a full-body
     * parse is never needed just to keep a 150-character preview (found in review).
     */
    private static final int HTML_PARSE_BUDGET = 4096;

    /**
     * Fallback budget tried once when {@link #HTML_PARSE_BUDGET} extracts no visible text at all --
     * a body that opens with a large non-text span (a base64 image data URI, a long {@code <style>}
     * block) can spend the entire first budget inside it, and {@code Jsoup.text()} returns an empty
     * string even though the body has real visible text further in. Wide enough to clear a large
     * embedded resource without falling back to a genuinely unbounded full-body parse (found in
     * review -- this budget's own first draft was the regression: an empty preview instead of the
     * short one it replaced).
     */
    private static final int HTML_PARSE_BUDGET_CEILING = 65_536;

    /** Appended to a preview when truncation actually drops content (found in review). */
    private static final String TRUNCATION_MARKER = "…";

    LongTextPreviewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    @Override
    protected Map<String, Object> transform(final Contentlet source, final Map<String, Object> map,
            final Set<TransformOptions> options, final User user)
            throws DotDataException, DotSecurityException {
        final ContentType contentType = source.getContentType();
        if (null == contentType || UtilMethods.isNotSet(contentType.id())) {
            throw new DotDataException(
                    String.format("Content Type in Contentlet '%s' is not set", source.getIdentifier()));
        }

        applyPreview(contentType.fields(WysiwygField.class), map, LongTextPreviewStrategy::extractHtmlPreview);
        applyPreview(contentType.fields(TextAreaField.class), map, LongTextPreviewStrategy::extractHtmlPreview);
        final List<Field> storyBlockFields = contentType.fields(StoryBlockField.class);
        applyPreview(storyBlockFields, map, LongTextPreviewStrategy::extractStoryBlockPreview);
        removeRawCompanionKeys(storyBlockFields, map);

        return map;
    }

    /**
     * Every Story Block field carries an untouched {@code <var>_raw} companion holding the full
     * JSON schema, written far upstream of this strategy (see {@code ContentletJsonAPIImpl}) for
     * consumers that need to re-parse it (e.g. nested Block Editor reference resolution in
     * {@code StoryBlockAPIImpl}). None of those consumers read it off a listing row's map -- they
     * read it directly off the {@link Contentlet} -- so removing it here does not affect them, and
     * leaving it in defeats the whole point of this strategy for Story Block fields: the &lt;=150
     * character preview {@link #applyPreview} just wrote rides alongside the complete, untruncated
     * schema it was supposed to replace (issue #37185 QA follow-up).
     */
    private void removeRawCompanionKeys(final List<Field> storyBlockFields, final Map<String, Object> map) {
        if (!UtilMethods.isSet(storyBlockFields)) {
            return;
        }
        storyBlockFields.forEach(field -> map.remove(field.variable() + "_raw"));
    }

    private void applyPreview(final List<Field> fields, final Map<String, Object> map,
            final Function<Object, String> extractor) {
        if (!UtilMethods.isSet(fields)) {
            return;
        }
        fields.stream()
                // A field entirely absent from the row's map must stay absent -- otherwise every
                // in-scope field on the content type gets a synthesized "" entry, growing the
                // payload this strategy exists to shrink (found in review).
                .filter(field -> map.containsKey(field.variable()))
                .forEach(field -> Try.run(() ->
                        map.put(field.variable(), extractor.apply(map.get(field.variable()))))
                        .onFailure(e -> Logger.warn(LongTextPreviewStrategy.class, String.format(
                                "An error occurred extracting a long-text preview for field '%s' [%s]: %s",
                                field.variable(), field.id(), e.getMessage()))));
    }

    /** WYSIWYG/TextArea: strip HTML via Jsoup, then truncate the plain text. */
    private static String extractHtmlPreview(final Object rawValue) {
        if (!(rawValue instanceof String) || ((String) rawValue).isEmpty()) {
            return rawValue instanceof String ? (String) rawValue : StringPool.BLANK;
        }
        final String html = (String) rawValue;
        final String raw = extractVisibleText(html, HTML_PARSE_BUDGET);
        if (raw.length() >= MAX_PREVIEW_LENGTH || html.length() <= HTML_PARSE_BUDGET) {
            // A budget-limited extract (html longer than HTML_PARSE_BUDGET) that came back at or
            // over the preview bound may still have dropped real text past the cut point -- the
            // parse only ever saw the first HTML_PARSE_BUDGET characters of the raw value, never
            // the rest. `truncate` alone cannot tell the difference between "exactly 150 visible
            // characters and nothing more" and "150 visible characters because that's all this
            // prefix had room for" once it is handed exactly MAX_PREVIEW_LENGTH characters (the
            // same one-character gap already closed for the Story Block path -- found in review).
            // Forcing the marker whenever the html itself was cut removes the ambiguity.
            final boolean htmlWasCut = html.length() > HTML_PARSE_BUDGET;
            return truncate(htmlWasCut && raw.length() >= MAX_PREVIEW_LENGTH
                    ? raw + TRUNCATION_MARKER
                    : raw);
        }
        // The narrow budget's extracted text is shorter than the preview bound, even though the
        // raw value is long enough that it might carry more visible text further in -- a body
        // opening with a short intro before a large non-text span (a base64 image, a long <style>
        // block) reads as empty or as a complete short value either way, when it is neither.
        // Retrying only when the first pass came back completely empty (an earlier version of this
        // fix) missed the "short intro, more text after" shape entirely. Retry once against the
        // wider ceiling instead (found in review).
        return truncate(extractVisibleText(html, HTML_PARSE_BUDGET_CEILING));
    }

    /**
     * Flattens {@code html} to plain text via {@link Jsoup#parse}, parsing only the first
     * {@code budget} characters rather than the whole value.
     */
    private static String extractVisibleText(final String html, final int budget) {
        if (html.length() <= budget) {
            return Jsoup.parse(html).text();
        }
        return Jsoup.parse(html.substring(0, safeHtmlCutIndex(html, budget))).text();
    }

    /**
     * Backs a raw-HTML cut index off far enough to avoid splitting a UTF-16 surrogate pair or an
     * HTML character entity (e.g. {@code &amp;} cut to {@code &am}) mid-sequence -- either would
     * hand {@link Jsoup#parse} a malformed fragment right at the boundary (found in review).
     */
    private static int safeHtmlCutIndex(final String html, final int budget) {
        int cut = budget;
        if (cut > 0 && Character.isHighSurrogate(html.charAt(cut - 1))) {
            cut--;
        }
        // Entities are short -- "&amp;" is the longest common one at 5 characters, a numeric
        // reference like "&#x1F600;" runs a little longer -- so a small fixed lookback is enough
        // to catch one in progress without rescanning the whole prefix.
        final int lookback = Math.max(0, cut - 12);
        for (int i = cut - 1; i >= lookback; i--) {
            final char c = html.charAt(i);
            if (c == ';') {
                break; // any entity within the window is already closed
            }
            if (c == '&') {
                cut = i;
                break;
            }
        }
        return cut;
    }

    /**
     * Story Block: the map already holds {@link StoryBlockViewStrategy}'s output -- a
     * {@link Map} (parsed JSON), a raw {@link String} (non-JSON fallback) or {@code null}
     * (parse-failure fallback). Extract and truncate text from whichever shape it is.
     */
    private static String extractStoryBlockPreview(final Object storyBlockValue) {
        if (null == storyBlockValue) {
            return StringPool.BLANK;
        }
        if (storyBlockValue instanceof String) {
            return truncate((String) storyBlockValue);
        }
        final StringBuilder textBuilder = new StringBuilder();
        collectText(storyBlockValue, textBuilder);
        return truncate(textBuilder.toString());
    }

    /**
     * Recursively walks a Story Block JSON-tree node, collecting every {@code text} leaf value.
     * Stops once enough text has been collected for the preview bound, so a large story block is
     * not fully traversed/concatenated just to be truncated away afterward (found in review).
     *
     * <p>Deliberately overshoots {@code MAX_PREVIEW_LENGTH} by continuing past it rather than
     * stopping the instant it is reached: {@link #truncate} only appends the truncation marker
     * when the collected text is strictly longer than the bound, so a traversal that stopped
     * exactly at the bound with more text still pending would return a truncated preview with no
     * marker -- the one case the marker exists to signal (found in review).</p>
     */
    private static void collectText(final Object node, final StringBuilder out) {
        if (out.length() > MAX_PREVIEW_LENGTH) {
            return;
        }
        if (node instanceof Map) {
            final Map<?, ?> nodeMap = (Map<?, ?>) node;
            final Object text = nodeMap.get("text");
            if (text instanceof String) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append((String) text);
            }
            final Object content = nodeMap.get("content");
            if (content instanceof List) {
                for (final Object child : (List<?>) content) {
                    if (out.length() > MAX_PREVIEW_LENGTH) {
                        break;
                    }
                    collectText(child, out);
                }
            }
        } else if (node instanceof List) {
            for (final Object child : (List<?>) node) {
                if (out.length() > MAX_PREVIEW_LENGTH) {
                    break;
                }
                collectText(child, out);
            }
        }
    }

    private static String truncate(final String text) {
        if (null == text) {
            return StringPool.BLANK;
        }
        if (text.length() <= MAX_PREVIEW_LENGTH) {
            return text;
        }
        // Leave room for the truncation marker so the total visible length still honors the
        // <=150-character bound from AC-001.
        final int budget = MAX_PREVIEW_LENGTH - TRUNCATION_MARKER.length();
        // Avoid splitting a UTF-16 surrogate pair (e.g. an emoji) at the boundary -- that would
        // leave a lone high surrogate at the end of the preview (found in review).
        final int cutIndex = Character.isHighSurrogate(text.charAt(budget - 1))
                ? budget - 1 : budget;
        // A hard cut is indistinguishable from a short, complete value -- append the marker
        // exactly when content was actually dropped, so its presence signals truncation
        // (found in review).
        return text.substring(0, cutIndex) + TRUNCATION_MARKER;
    }

}
