package com.dotcms.graphql.datafetcher.page;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.page.PageResourceHelper;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DataFetcher that returns the style editor schemas for all distinct content types present on a
 * page. Only content types that define a {@code DOT_STYLE_EDITOR_SCHEMA} metadata entry are
 * included; the result is an empty list when none are found.
 *
 * <p>Schemas are only fetched in {@link PageMode#EDIT_MODE} to avoid the N+1 DB query cost on
 * public page loads. The feature flag provides an additional gate.
 */
public class StyleEditorSchemasDataFetcher extends RedirectAwareDataFetcher<List<Object>> {

    @Override
    public List<Object> safeGet(final DataFetchingEnvironment environment,
            final DotGraphQLContext context) throws Exception {

        final boolean isStyleEditorFlagOn = ConfigUtils.isFeatureFlagOn(
                FeatureFlagName.FEATURE_FLAG_UVE_STYLE_EDITOR);

        if (!isStyleEditorFlagOn) {
            return Collections.emptyList();
        }

        final String pageModeAsString = (String) context.getParam("pageMode");
        final PageMode pageMode = PageMode.get(pageModeAsString);

        if (pageMode != PageMode.EDIT_MODE) {
            return Collections.emptyList();
        }

        final Contentlet page = environment.getSource();
        Logger.debug(this, () -> "Fetching style editor schemas for page: " + page.getIdentifier());

        final List<JsonNode> jsonSchemas = PageResourceHelper.getInstance()
                .getStyleEditorSchemasInPage(page.getIdentifier());
        final ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        return jsonSchemas.stream()
                .map(node -> mapper.convertValue(node, Object.class))
                .collect(Collectors.toList());
    }

    @Override
    protected List<Object> onRedirect() {
        return Collections.emptyList();
    }
}