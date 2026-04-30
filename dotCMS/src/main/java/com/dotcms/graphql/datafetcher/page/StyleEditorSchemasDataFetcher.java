package com.dotcms.graphql.datafetcher.page;

import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rest.api.v1.page.PageResourceHelper;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import graphql.schema.DataFetchingEnvironment;
import java.util.Collections;
import java.util.List;

/**
 * DataFetcher that returns the style editor schemas for all distinct content types present on a
 * page. Only content types that define a {@code DOT_STYLE_EDITOR_SCHEMA} metadata entry are
 * included; the result is an empty list when none are found.
 */
public class StyleEditorSchemasDataFetcher extends RedirectAwareDataFetcher<List<Object>> {

    @Override
    public List<Object> safeGet(final DataFetchingEnvironment environment,
            final DotGraphQLContext context) throws Exception {

        final boolean isStyleEditorFlagOn = ConfigUtils.isFeatureFlagOn(
                FeatureFlagName.FEATURE_FLAG_UVE_STYLE_EDITOR);

        if (!isStyleEditorFlagOn) {
            return null;
        }

        final Contentlet page = environment.getSource();
        Logger.debug(this, () -> "Fetching style editor schemas for page: " + page.getIdentifier());

        @SuppressWarnings("unchecked") final List<Object> schemas = (List<Object>) (List<?>) PageResourceHelper.getInstance()
                .getStyleEditorSchemasInPage(page.getIdentifier());
        return schemas;
    }

    @Override
    protected List<Object> onRedirect() {
        return Collections.emptyList();
    }
}