package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.util.StoryBlockUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Transforms the value of a Story Block field from a JSON-as-String into a Java Map representation. This way, its value
 * can be returned as a proper JSON object in a REST response.
 *
 * @author Jose Castro
 * @since May 2nd, 2022
 */
public class StoryBlockViewStrategy extends AbstractTransformStrategy<Contentlet> {

    /**
     * Creates an instance of this Story Block View Strategy.
     *
     * @param toolBox Provides easy access to several dotCMS APIs.
     */
    StoryBlockViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    @Override
    protected Map<String, Object> transform(final Contentlet source, final Map<String, Object> map, final Set<TransformOptions> options,
                                            final User user) throws DotDataException, DotSecurityException {
        if (null == source.getContentType() || UtilMethods.isNotSet(source.getContentType().id())) {
            throw new DotDataException(
                    String.format("Content Type in Contentlet '%s' is not set", source.getIdentifier()));
        }
        final List<Field> storyBlockFields = source.getContentType().fields(StoryBlockField.class);
        if (UtilMethods.isSet(storyBlockFields)) {
            storyBlockFields.forEach(field -> {

                final String fieldValue =
                        Try.of(() -> source.get(field.variable()).toString()).getOrElse(StringPool.BLANK);
                if (!JsonUtil.isValidJSON(fieldValue)) {
                    map.put(field.variable(), fieldValue);
                } else {
                    LinkedHashMap jsonAsMap = null;
                    try {
                        jsonAsMap =
                                new LinkedHashMap(ContentletJsonHelper.INSTANCE.get().objectMapper().readValue(
                                        Try.of(() -> fieldValue).getOrElse(StringPool.BLANK), LinkedHashMap.class));
                    } catch (final JsonProcessingException e) {
                        Logger.warn(StoryBlockField.class, String.format("An error occurred when transforming Story " +
                                                                                 "Block JSON data in field '%s' [%s] " +
                                                                                 "into a Map: %s", field.variable(),
                                field.id(), e.getMessage()));
                    }
                    if (jsonAsMap != null) {
                        enrichWithReadingStats(jsonAsMap, fieldValue);
                    }
                    map.put(field.variable(), jsonAsMap);
                }

            });
        }
        return map;
    }

    /**
     * Ensures that the reading statistics ({@code charCount}, {@code wordCount}, and
     * {@code readingTime}) are present in the {@code attrs} of the given Story Block map. If any
     * of these values are missing — which can happen with content created via the API or older
     * content that pre-dates the Block Editor stats tracking feature — they are computed from the
     * raw JSON and added to the map so that the API response always exposes these fields.
     *
     * @param jsonAsMap  The deserialized Story Block map to enrich
     * @param fieldValue The original JSON string of the Story Block field
     */
    @SuppressWarnings("unchecked")
    private void enrichWithReadingStats(final LinkedHashMap<String, Object> jsonAsMap,
                                        final String fieldValue) {
        final Object attrsObj = jsonAsMap.get("attrs");
        Map<String, Object> attrs = (attrsObj instanceof Map) ? (Map<String, Object>) attrsObj : null;
        final boolean missingStats = attrs == null
                || !attrs.containsKey("charCount")
                || !attrs.containsKey("wordCount")
                || !attrs.containsKey("readingTime");

        if (!missingStats) {
            return;
        }

        if (attrs == null) {
            attrs = new LinkedHashMap<>();
            jsonAsMap.put("attrs", attrs);
        }

        final int charCount = StoryBlockUtil.computeCharCount(fieldValue);
        final int wordCount = StoryBlockUtil.computeWordCount(fieldValue);
        final int readingTime = StoryBlockUtil.computeReadingTime(wordCount);
        attrs.put("charCount", charCount);
        attrs.put("wordCount", wordCount);
        attrs.put("readingTime", readingTime);
    }

}
