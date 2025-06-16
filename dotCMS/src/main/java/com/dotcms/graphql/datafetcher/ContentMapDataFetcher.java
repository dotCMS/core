package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rest.ContentHelper;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
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
            // Resolve hydrated contentlet map with optional variant handling
            Map<String, Object> hydratedMap = getHydratedMapWithVariantFallback(request, user, contentlet, render);
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

    /**
     * Attempts to retrieve a hydrated contentlet map, respecting an explicitly requested variant if present.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If a variant is explicitly provided via {@link VariantAPI#VARIANT_KEY} in the request,
     *       it is set on the contentlet.</li>
     *   <li>If the variant is {@code DEFAULT}, no fallback is performed on error.</li>
     *   <li>If no variant is provided, the contentlet will implicitly use the {@code DEFAULT} variant.</li>
     *   <li>If a variant is set and fails to resolve (e.g., the contentlet has no version for it),
     *       a fallback attempt is made using the {@code DEFAULT} variant.</li>
     * </ul>
     *
     * @param request    the current HTTP request, used to extract the variant key if provided
     * @param user       the user requesting the content, used for permissions and rendering
     * @param contentlet the contentlet to hydrate
     * @param render     whether renderable fields should be velocity-rendered
     * @return a hydrated content map including field values (rendered if requested)
     */
    private Map<String, Object> getHydratedMapWithVariantFallback(
            final HttpServletRequest request,
            final User user,
            final Contentlet contentlet,
            final boolean render
    ) {
        final Object variantAttr = request.getAttribute(VariantAPI.VARIANT_KEY);

        // Set variantId only if explicitly requested
        if (UtilMethods.isSet(variantAttr)) {
            final String variantName = variantAttr.toString();

            // Avoid fallback loop if DEFAULT is already being used
            assert VariantAPI.DEFAULT_VARIANT.name() != null;
            if (VariantAPI.DEFAULT_VARIANT.name().equals(variantName)) {
                contentlet.setVariantId(variantName);
                return ContentletUtil.getContentPrintableMap(user, contentlet, false, render);
            }

            try {
                contentlet.setVariantId(variantName);
                return ContentletUtil.getContentPrintableMap(user, contentlet, false, render);
            } catch (DotStateException e) {
                Logger.debug(this, () -> String.format(
                        "Variant '%s' not found for contentlet '%s'. Falling back to DEFAULT variant.",
                        variantName, contentlet.getIdentifier()
                ));

                contentlet.setVariantId(VariantAPI.DEFAULT_VARIANT.name());
                return ContentletUtil.getContentPrintableMap(user, contentlet, false, render);
            }
        }

        // No variantAttr: implicitly uses DEFAULT variant
        return ContentletUtil.getContentPrintableMap(user, contentlet, false, render);
    }
}
