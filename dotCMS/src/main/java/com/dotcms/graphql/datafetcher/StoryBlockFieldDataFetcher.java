package com.dotcms.graphql.datafetcher;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves a Story Block (Block Editor) field for GraphQL queries.
 * <p>
 * The field value can arrive in two shapes depending on the read path: an already-hydrated
 * {@link Map} (page queries, where {@code StoryBlockViewStrategy} has transformed the contentlet)
 * or a raw JSON {@link String} (collection queries, which return the raw contentlet). Both shapes
 * are resolved through the canonical {@link com.dotcms.contenttype.business.StoryBlockAPI#toMap(Object)}
 * conversion so every consumer of a Block Editor value behaves the same.
 * <p>
 * A value that cannot be resolved into JSON degrades gracefully: the field resolves to
 * {@code null} and a WARN is logged, instead of failing the whole GraphQL query.
 */
public class StoryBlockFieldDataFetcher implements DataFetcher<Map<String, Object>> {

    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String variableName = environment.getField().getName();

        Logger.debug(this, ()-> "Fetching StoryBlock field for contentlet: " + contentlet.getIdentifier() + " field: " + variableName);

        final Object fieldValue = contentlet.get(variableName);
        if (null == fieldValue
                || (fieldValue instanceof String && !UtilMethods.isSet((String) fieldValue))) {
            return null;
        }

        final Map<String, Object> storyBlockMap = new HashMap<>();
        try {
            storyBlockMap.put("json", APILocator.getStoryBlockAPI().toMap(fieldValue));
        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(), String.format(
                    "Unable to resolve Story Block field '%s' of contentlet '%s' as JSON: %s",
                    variableName, contentlet.getIdentifier(), e.getMessage()), e);
            return null;
        }

        return storyBlockMap;
    }
}
