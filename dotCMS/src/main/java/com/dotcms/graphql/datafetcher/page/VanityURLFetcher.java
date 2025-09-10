package com.dotcms.graphql.datafetcher.page;

import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Optional;

/**
 * GraphQL Data Fetcher implementation for retrieving Vanity URLs associated with pages.
 * This fetcher resolves vanity URL information based on the page's URI, host, and language settings.
 * It integrates with dotCMS's Vanity URL API to retrieve {@link CachedVanityUrl} objects.
 *
 * @see DataFetcher
 * @see CachedVanityUrl
 */
public class VanityURLFetcher implements DataFetcher<CachedVanityUrl> {

    @Override
    public CachedVanityUrl get(final DataFetchingEnvironment environment) throws Exception {
        final DotGraphQLContext context = environment.getContext();

        // First check if we already have the cached vanity URL from PageDataFetcher
        CachedVanityUrl cachedVanityUrl = (CachedVanityUrl) context.getParam("cachedVanityUrl");
        if (cachedVanityUrl != null) {
            return cachedVanityUrl;
        }

        final User user = context.getUser();
        final Contentlet page = environment.getSource();
        final Host host = APILocator.getHostAPI().find(
                page.getHost(), user, true
        );
        final Language language = APILocator.getLanguageAPI().getLanguage(page.getLanguageId());

        // Resolve the vanity URL based on the page's URI, host, and language.
        // Try "pageURI" first, fallback to "url" if not set
        String uri = page.getStringProperty("pageURI");
        if (!UtilMethods.isSet(uri)) {
            uri = page.getStringProperty("url");
        }

        if (UtilMethods.isSet(uri)) {
            final Optional<CachedVanityUrl> vanityUrlOpt = APILocator.getVanityUrlAPI().
                    resolveVanityUrl(uri, host, language);
            if (vanityUrlOpt.isPresent()) {
                return vanityUrlOpt.get();
            }
        }

        return null;
    }
}
