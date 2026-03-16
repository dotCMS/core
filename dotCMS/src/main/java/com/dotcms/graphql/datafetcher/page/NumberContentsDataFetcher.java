package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Returns the total number of contentlets placed across all containers in the requested page.
 * This mirrors the {@code numberContents} field exposed by the REST Page API.
 */
public class NumberContentsDataFetcher extends RedirectAwareDataFetcher<Integer> {

    @Override
    public Integer safeGet(final DataFetchingEnvironment environment, final DotGraphQLContext context) throws Exception {
        try {
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

            // Step 2: count only those contentlets that exist in the requested language (one Lucene query)
            final String languageIdParam = (String) context.getParam("languageId");
            final long langId = UtilMethods.isSet(languageIdParam)
                    ? Long.parseLong(languageIdParam)
                    : APILocator.getLanguageAPI().getDefaultLanguage().getId();

            final String identifierClause = multiTrees.stream()
                    .map(MultiTree::getChild)
                    .distinct()
                    .collect(Collectors.joining(" ", "(", ")"));

            final String luceneQuery = "+languageId:" + langId + " +identifier:" + identifierClause;

            return (int) APILocator.getContentletAPI()
                    .indexCount(luceneQuery, context.getUser(), false);

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
