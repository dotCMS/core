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
 * — plus legacy values that are not Block Editor JSON at all, e.g. HTML from a converted WYSIWYG
 * field — are resolved through the canonical
 * {@link com.dotcms.contenttype.business.StoryBlockAPI#toMapOrPassthrough(Object, java.util.function.Supplier)}
 * conversion, which never reshapes stored data and never throws: unparseable values are returned
 * unchanged (matching {@code _map} and the Page REST API) with a rate-limited WARN. A
 * missing/blank value resolves to an empty json object per the existing contract.
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

        storyBlockMap.put("json", APILocator.getStoryBlockAPI().toMapOrPassthrough(fieldValue,
                () -> String.format("field: '%s', identifier: %s, inode: %s, languageId: %d",
                        variableName, contentlet.getIdentifier(), contentlet.getInode(),
                        contentlet.getLanguageId())));
        return storyBlockMap;
    }
}
