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
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY;
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
 *
 * @since 25.xx
 */
public class LongTextPreviewStrategy extends AbstractTransformStrategy<Contentlet> {

    static final int MAX_PREVIEW_LENGTH = 150;

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
        applyPreview(contentType.fields(StoryBlockField.class), map, LongTextPreviewStrategy::extractStoryBlockPreview);

        return map;
    }

    private void applyPreview(final List<Field> fields, final Map<String, Object> map,
            final Function<Object, String> extractor) {
        if (!UtilMethods.isSet(fields)) {
            return;
        }
        fields.stream()
                // AC-008: the "title" key is independently populated by COMMON_PROPS from
                // Contentlet#getTitle() -- never overwrite it with a truncated preview, even when
                // the content type's title-source field is itself WYSIWYG/TextArea/Story Block.
                .filter(field -> !TITTLE_KEY.equals(field.variable()))
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
        return truncate(Jsoup.parse((String) rawValue).text());
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
     */
    private static void collectText(final Object node, final StringBuilder out) {
        if (out.length() >= MAX_PREVIEW_LENGTH) {
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
                    if (out.length() >= MAX_PREVIEW_LENGTH) {
                        break;
                    }
                    collectText(child, out);
                }
            }
        } else if (node instanceof List) {
            for (final Object child : (List<?>) node) {
                if (out.length() >= MAX_PREVIEW_LENGTH) {
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
        // Avoid splitting a UTF-16 surrogate pair (e.g. an emoji) at the boundary -- that would
        // leave a lone high surrogate at the end of the preview (found in review).
        final int cutIndex = Character.isHighSurrogate(text.charAt(MAX_PREVIEW_LENGTH - 1))
                ? MAX_PREVIEW_LENGTH - 1 : MAX_PREVIEW_LENGTH;
        return text.substring(0, cutIndex);
    }

}
