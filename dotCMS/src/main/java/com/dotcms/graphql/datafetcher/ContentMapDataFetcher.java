package com.dotcms.graphql.datafetcher;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rest.ContentHelper;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
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
            final Boolean renderArg = environment.getArgument("render");
            final boolean render = Boolean.TRUE.equals(renderArg);

            final HttpServletRequest request = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletRequest();

            final HttpServletResponse response = ((DotGraphQLContext) environment.getContext())
                    .getHttpServletResponse();

            Logger.debug(this, ()-> "Fetching content map for contentlet: " + contentlet.getIdentifier());
            if(UtilMethods.isSet(key)) {
                Object fieldValue;
                if(render) {
                    fieldValue = getRenderedFieldValue(request, response, contentlet, key);
                } else {
                    fieldValue = contentlet.get(key);
                }
                return fieldValue;
            }

            final User user = ((DotGraphQLContext) environment.getContext()).getUser();

            // Resolve hydrated contentlet map with optional variant handling
            Map<String, Object> hydratedMap = getHydratedMapWithVariantFallback(request, contentlet, render);
            final JSONObject contentMapInJSON = new JSONObject();

            // this only adds relationships to any json. We would need to return them with the transformations already
            final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(request);

            final JSONObject jsonWithRels = ContentHelper.getInstance().addRelationshipsToJSON(request, response,
                    Boolean.toString(render), user, depth, false, contentlet,
                    contentMapInJSON, null, currentLanguage.getId(), true, false,
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

            // Searches if the map contains any key ending with "_raw".
            if (mapKey.endsWith("_raw") && rawValue instanceof String) {
                // Creates a baseKey variable deleting the "_raw" string at the end of the mapKey. i.e. blogContent_raw = blogContent
                final String baseKey = mapKey.substring(0, mapKey.length() - 4);

                // Checks if the baseKey exist in the map
                if (hydratedMap.containsKey(baseKey)) {
                    // Checks if the baseValue (hydrated) is not empty. If the baseValue is empty or null, we parse the rawValue instead.
                    Object hydratedValue = hydratedMap.get(baseKey);
                    final String baseValue = (hydratedValue == null || hydratedValue.toString().trim().isEmpty())
                            ? rawValue.toString()
                            : hydratedValue.toString();

                    // Parse the baseValue as JSON
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = objectMapper.readValue(baseValue, Map.class);
                        hydratedMap.put(baseKey, parsed);
                    } catch (Exception e) {
                        Logger.warn(this, () -> "Error parsing JSON for '" + baseKey + "': " + e.getMessage());
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
     * @param contentlet the contentlet to hydrate
     * @param render     whether renderable fields should be velocity-rendered
     * @return a hydrated content map including field values (rendered if requested)
     */
    private Map<String, Object> getHydratedMapWithVariantFallback(
            final HttpServletRequest request,
            final Contentlet contentlet,
            final boolean render
    ) {
        // Create a defensive copy to avoid modifying the original contentlet
        final Contentlet contentletCopy = new Contentlet(contentlet.getMap());
        final DotTransformerBuilder transformerBuilder = new DotTransformerBuilder();
        if(render) {
            transformerBuilder.hydratedContentMapTransformer(RENDER_FIELDS);
        } else {
            transformerBuilder.hydratedContentMapTransformer();
        }
        final Object variantAttr = request.getAttribute(VariantAPI.VARIANT_KEY);

        // Set variantId only if explicitly requested
        if (UtilMethods.isSet(variantAttr)) {
            final String variantName = variantAttr.toString();

            // Avoid fallback loop if DEFAULT is already being used
            assert VariantAPI.DEFAULT_VARIANT.name() != null;
            if (VariantAPI.DEFAULT_VARIANT.name().equals(variantName)) {
                contentletCopy.setVariantId(variantName);
                return transformContentlet(transformerBuilder, contentletCopy);
            }

            try {
                contentletCopy.setVariantId(variantName);
                return transformContentlet(transformerBuilder, contentletCopy);
            } catch (DotStateException e) {
                Logger.debug(this, () -> String.format(
                        "Variant '%s' not found for contentlet '%s'. Falling back to DEFAULT variant.",
                        variantName, contentletCopy.getIdentifier()
                ));

                contentletCopy.setVariantId(VariantAPI.DEFAULT_VARIANT.name());
                return transformContentlet(transformerBuilder, contentletCopy);
            }
        }

        // No variantAttr: implicitly uses DEFAULT variant
        return transformContentlet(transformerBuilder, contentletCopy);
    }

    /**
     * Transforms the given {@link Contentlet} using the provided {@link DotTransformerBuilder}
     * and returns the first entry from the resulting hydrated content map.
     *
     * @param builder     the transformer builder preconfigured with hydration settings
     * @param contentlet  the contentlet to transform into a hydrated map
     * @return the first (and typically only) map resulting from the contentlet transformation
     */
    private Map<String, Object> transformContentlet(final DotTransformerBuilder builder, final Contentlet contentlet) {
        return builder.content(contentlet).build().toMaps().get(0);
    }



}
