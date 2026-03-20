package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;

/**
 * Returns the total number of contentlets placed across all containers in the requested page.
 * This mirrors the {@code numberContents} field exposed by the REST Page API.
 */
public class NumberContentsDataFetcher extends RedirectAwareDataFetcher<Integer> {

    @Override
    public Integer safeGet(final DataFetchingEnvironment environment, final DotGraphQLContext context) throws Exception {
        try {
            final String pageModeAsString = (String) context.getParam("pageMode");
            final PageMode pageMode = PageMode.get(pageModeAsString);

            if (pageMode != PageMode.EDIT_MODE) {
                return null;
            }

            final Contentlet page = environment.getSource();
            Logger.debug(this, () -> "Fetching numberContents for page: " + page.getIdentifier());

            // Step 1: get placements for the requested personalization + default variant (one SQL query)
            final String personalization = WebAPILocator.getPersonalizationWebAPI()
                    .getContainerPersonalization(context.getHttpServletRequest());

            final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                    .getMultiTreesByPersonalizedPage(
                            page.getIdentifier(),
                            personalization,
                            VariantAPI.DEFAULT_VARIANT.name()
                    );

            if (multiTrees.isEmpty()) {
                return 0;
            }

            // Step 2: count placements that exist in the requested language (cache-backed).
            // No deduplication — mirrors PageView.getContentsNumber() which counts each placement
            // independently (same contentlet in two containers = 2).
            final String languageIdParam = (String) context.getParam("languageId");
            long resolvedLangId;
            try {
                resolvedLangId = UtilMethods.isSet(languageIdParam)
                        ? Long.parseLong(languageIdParam)
                        : APILocator.getLanguageAPI().getDefaultLanguage().getId();
            } catch (final NumberFormatException e) {
                Logger.warn(this, "Invalid languageId param '" + languageIdParam + "' — using default language");
                resolvedLangId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            }
            final long langId = resolvedLangId;

            return (int) multiTrees.stream()
                    .map(MultiTree::getContentlet)
                    .filter(id -> APILocator.getVersionableAPI()
                            .getContentletVersionInfo(id, langId)
                            .isPresent())
                    .count();

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected Integer onRedirect() {
        return 0;
    }
}
