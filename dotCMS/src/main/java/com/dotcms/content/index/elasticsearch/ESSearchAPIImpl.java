package com.dotcms.content.index.elasticsearch;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.content.index.SearchAPI;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.enterprise.ESSeachAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import javax.enterprise.context.ApplicationScoped;

/**
 * Elasticsearch implementation of {@link SearchAPI}.
 *
 * <p>Adapter over the legacy {@link com.dotcms.enterprise.priv.ESSearchAPIImpl}: all search
 * operations are delegated to that implementation and the ES-specific result types
 * ({@code SearchResponse}, {@code ESSearchResults}) are converted to vendor-neutral DTOs
 * via {@link ContentSearchResponse#from(org.elasticsearch.action.search.SearchResponse)}.</p>
 *
 * <p>Transitional — will be deleted when the ES→OS migration completes (Phase 3 cutover).</p>
 *
 * @see com.dotcms.content.index.opensearch.OSSearchAPIImpl symmetric OS counterpart
 */
@ApplicationScoped
public class ESSearchAPIImpl implements SearchAPI {

    private final ESSeachAPI delegate;

    /** CDI constructor. */
    public ESSearchAPIImpl() {
        this(new com.dotcms.enterprise.priv.ESSearchAPIImpl());
    }

    /** Package-private for testing. */
    ESSearchAPIImpl(final ESSeachAPI delegate) {
        this.delegate = delegate;
    }

    // -------------------------------------------------------------------------
    // SearchAPI — delegate and adapt
    // -------------------------------------------------------------------------

    @Override
    public ContentSearchResults search(
            final String query,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        final ESSearchResults esResults = delegate.esSearch(query, live, user, respectFrontendRoles);
        final ContentSearchResponse response = ContentSearchResponse.from(esResults.getResponse());
        final ContentSearchResults results =
                new ContentSearchResults(response, esResults.getContentlets());
        results.setQuery(esResults.getQuery());
        results.setRewrittenQuery(esResults.getRewrittenQuery());
        results.setPopulationTook(esResults.getPopulationTook());
        return results;
    }

    @Override
    public ContentSearchResponse searchRaw(
            final String query,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        final org.elasticsearch.action.search.SearchResponse esResponse =
                delegate.esSearchRaw(query, live, user, respectFrontendRoles);
        if (esResponse == null) {
            throw new DotDataException("ES search returned null — unable to load index information");
        }
        return ContentSearchResponse.from(esResponse);
    }

    @Override
    public ContentSearchResponse searchRelated(
            final String contentletIdentifier,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        final org.elasticsearch.action.search.SearchResponse esResponse =
                delegate.esSearchRelated(contentletIdentifier, relationshipName,
                        pullParents, live, user, respectFrontendRoles);
        if (esResponse == null) {
            throw new DotDataException("ES search returned null — unable to load index information");
        }
        return ContentSearchResponse.from(esResponse);
    }

    @Override
    public ContentSearchResponse searchRelated(
            final Contentlet contentlet,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        final org.elasticsearch.action.search.SearchResponse esResponse =
                delegate.esSearchRelated(contentlet, relationshipName,
                        pullParents, live, user, respectFrontendRoles);
        if (esResponse == null) {
            throw new DotDataException("ES search returned null — unable to load index information");
        }
        return ContentSearchResponse.from(esResponse);
    }

    @Override
    public ContentSearchResponse searchRelated(
            final String contentletIdentifier,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles,
            final int limit,
            final int offset,
            final String sortBy)
            throws DotDataException, DotSecurityException {

        final org.elasticsearch.action.search.SearchResponse esResponse =
                delegate.esSearchRelated(contentletIdentifier, relationshipName,
                        pullParents, live, user, respectFrontendRoles, limit, offset, sortBy);
        if (esResponse == null) {
            throw new DotDataException("ES search returned null — unable to load index information");
        }
        return ContentSearchResponse.from(esResponse);
    }

    @Override
    public ContentSearchResponse searchRelated(
            final Contentlet contentlet,
            final String relationshipName,
            final boolean pullParents,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles,
            final int limit,
            final int offset,
            final String sortBy)
            throws DotDataException, DotSecurityException {

        final org.elasticsearch.action.search.SearchResponse esResponse =
                delegate.esSearchRelated(contentlet, relationshipName,
                        pullParents, live, user, respectFrontendRoles, limit, offset, sortBy);
        if (esResponse == null) {
            throw new DotDataException("ES search returned null — unable to load index information");
        }
        return ContentSearchResponse.from(esResponse);
    }
}
