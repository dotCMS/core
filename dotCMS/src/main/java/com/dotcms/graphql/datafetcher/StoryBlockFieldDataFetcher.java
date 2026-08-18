package com.dotcms.graphql.datafetcher;

import com.dotcms.util.JsonUtil;
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
 * The read path never reshapes stored data: a String value that is not a Block Editor JSON
 * document (e.g. HTML from a converted WYSIWYG field) is returned unchanged, matching {@code _map}
 * and the Page REST API. A missing/blank value resolves to an empty json object. If resolution
 * still fails, the field degrades gracefully to {@code null} with a WARN instead of failing the
 * whole GraphQL query.
 */
public class StoryBlockFieldDataFetcher implements DataFetcher<Map<String, Object>> {

    @Override
    public Map<String, Object> get(final DataFetchingEnvironment environment) throws Exception {
        final Contentlet contentlet = environment.getSource();
        final String variableName = environment.getField().getName();

        Logger.debug(this, ()-> "Fetching StoryBlock field for contentlet: " + contentlet.getIdentifier() + " field: " + variableName);

        final Object fieldValue = contentlet.get(variableName);

        final Map<String, Object> storyBlockMap = new HashMap<>();
        if (null == fieldValue
                || (fieldValue instanceof String && !UtilMethods.isSet((String) fieldValue))) {
            // Existing contract (see GraphQLTests postman collection): a Story Block field
            // without a value resolves to an empty json object, not to null.
            storyBlockMap.put("json", Map.of());
            return storyBlockMap;
        }

        if (fieldValue instanceof String && !isJsonObject((String) fieldValue)) {
            // A stored value that is not a Block Editor JSON document -- e.g. HTML from a WYSIWYG
            // field later converted to Block Editor -- is returned unchanged. The read path never
            // reshapes stored data, matching what _map and the Page REST API return.
            storyBlockMap.put("json", fieldValue);
            return storyBlockMap;
        }

        try {
            storyBlockMap.put("json", APILocator.getStoryBlockAPI().toMap(fieldValue));
        } catch (final Exception e) {
            Logger.warnAndDebug(this.getClass(), String.format(
                    "Unable to resolve Story Block field '%s' as JSON (identifier: %s, inode: %s, languageId: %d): %s",
                    variableName, contentlet.getIdentifier(), contentlet.getInode(),
                    contentlet.getLanguageId(), e.getMessage()), e);
            return null;
        }

        return storyBlockMap;
    }

    /**
     * Mirrors {@code StoryBlockAPIImpl.isJsonObject}: Story Block documents are always JSON
     * objects, so only a value whose root token is an object is parsed; everything else is a
     * legacy value passed through as-is.
     */
    private static boolean isJsonObject(final String value) {
        final String trimmed = value.trim();
        return trimmed.startsWith("{") && JsonUtil.isValidJSON(trimmed);
    }
}
