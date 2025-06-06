package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rest.ContentHelper;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.isFieldRenderable;
import static com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy.renderFieldValue;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.RENDER_FIELDS;

/**
 * {@link DataFetcher} that fetches the data when the special <code>_map<code/> GraphQL Field is requested.
 * <p>
 * The field takes the follow arguments:
 * <ul>
 * <li>key: {@link Scalars#GraphQLString} that represents the variable name of a field of the contentlet. Using this argument makes the field to return the value of only that specific field from the
 * contentlet's map. If not specified it will return all the properties in the contentlet's map
 * <li>depth: {@link Scalars#GraphQLInt} value that specifies how to return the related content. Has the same behavior as the `depth` argument in the Content REST API for related content
 * <li>render: {@link Scalars#GraphQLBoolean} that indicates whether to velocity-render the rederable fields.
 * </ul>
 */

public class ContentMapDataFetcher implements DataFetcher<Object> {

    @Override
    public Object get(final DataFetchingEnvironment environment) throws Exception {
        try {
            final Contentlet contentlet = environment.getSource();
            final String key = environment.getArgument("key");
            final int depth = environment.getArgument("depth");
            Boolean render = environment.getArgument("render");

            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            Logger.debug(this, ()-> "Fetching content map for contentlet: " + contentlet.getIdentifier());
            if(UtilMethods.isSet(key)) {
                Object fieldValue;
                if(UtilMethods.isSet(render) && render) {
                    fieldValue = getRenderedFieldValue(request, response, contentlet, key);
                } else {
                    fieldValue = contentlet.get(key);
                }
                return fieldValue;
            }

            final User user = ((DotGraphQLContext) environment.getContext()).getUser();

            final DotTransformerBuilder transformerBuilder = new DotTransformerBuilder();

            if(UtilMethods.isSet(render) && render) {
                transformerBuilder.hydratedContentMapTransformer(RENDER_FIELDS);
            } else {
                render = false;
                transformerBuilder.hydratedContentMapTransformer();
            }

            final DotContentletTransformer myTransformer = transformerBuilder.content(contentlet).build();

            final Map<String, Object> hydratedMap =  myTransformer.toMaps().get(0);
            final JSONObject contentMapInJSON = new JSONObject();

            // this only adds relationships to any json. We would need to return them with the transformations already

            final JSONObject jsonWithRels = ContentHelper.getInstance().addRelationshipsToJSON(request, response,
                    render.toString(), user, depth, false, contentlet,
                    contentMapInJSON, null, 1, true, false,
                    true);

            HashMap<String,Object> result = new ObjectMapper().readValue(jsonWithRels.toString(), HashMap.class);
            hydratedMap.putAll(result);

            enrichWithParsedRawFields(hydratedMap);

            return hydratedMap;

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Detects *_raw fields in the hydrated map and parses them as JSON if a base key exists.
     * Adds the parsed value under the base key only if it was already defined.
     *
     * @param hydratedMap the map to enrich
     */
    private void enrichWithParsedRawFields(final Map<String, Object> hydratedMap) {
        final ObjectMapper objectMapper = new ObjectMapper();
        for (Map.Entry<String, Object> entry : new HashMap<>(hydratedMap).entrySet()) {
            final String mapKey = entry.getKey();
            final Object rawValue = entry.getValue();

            if (mapKey.endsWith("_raw") && rawValue instanceof String) {
                final String baseKey = mapKey.substring(0, mapKey.length() - 4);

                if (hydratedMap.containsKey(baseKey)) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = objectMapper.readValue((String) rawValue, Map.class);
                        hydratedMap.put(baseKey, parsed);
                    } catch (Exception e) {
                        Logger.warn(this, () -> "Error parsing JSON for '" + mapKey + "': " + e.getMessage());
                    }
                }
            }
        }
    }


    private Object getRenderedFieldValue(final HttpServletRequest request,
            final HttpServletResponse response, final Contentlet contentlet,
            final String key) {
        final Field field = Try.of(()-> contentlet.getContentType().fields()
                .stream().filter((myField)->myField.variable().equals(key)).collect(
                Collectors.toList()).get(0)).getOrNull();

        if(!isFieldRenderable(field)) {
            return contentlet.get(key);
        }

        return renderFieldValue(request, response, (String) contentlet.get(key), contentlet, field.variable());
    }
}
