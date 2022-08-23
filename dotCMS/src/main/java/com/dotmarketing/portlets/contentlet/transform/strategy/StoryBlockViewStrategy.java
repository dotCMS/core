package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
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

                final String fieldValue = source.get(field.variable()).toString();
                if (!JsonUtil.isJson(fieldValue)) {
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
                    map.put(field.variable(), jsonAsMap);
                }

            });
        }
        return map;
    }

}
