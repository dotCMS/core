package com.dotcms.content.index;

import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * Vendor-neutral search API for executing full-text index queries.
 *
 * <p>This interface replaces the legacy {@code ESSeachAPI} whose method signatures
 * leaked {@code org.elasticsearch.action.search.SearchResponse} and
 * {@code ESSearchResults} into application code.  Implementations delegate to the
 * active search back-end (Elasticsearch or OpenSearch) as determined by the current
 * migration phase.</p>
 *
 * <p>All methods accept the same Lucene/JSON query format used by the existing search
 * paths, so call sites only need to change the method names and return types.</p>
 *
 * @see SearchAPIImpl  Phase-aware router (register via {@code APILocator.getSearchAPI()})
 */
public interface SearchAPI {

    /**
     * Executes a JSON search query and returns the matching contentlets loaded from the DB.
     *
     * <p>Equivalent to {@code ESSeachAPI.esSearch()} but returns a vendor-neutral
     * {@link ContentSearchResults} instead of {@code ESSearchResults}.</p>
     *
     * @param query                the JSON search query
     * @param live                 {@code true} to query the live index
     * @param user                 the user performing the action (may be {@code null} when
     *                             {@code respectFrontendRoles} is {@code true})
     * @param respectFrontendRoles whether front-end roles should be applied
     * @return populated result list; never {@code null}
     */
    ContentSearchResults<Contentlet> search(
            String query,
            boolean live,
            User user,
            boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException;

    /**
     * Executes a raw JSON search query and returns the index response without loading
     * contentlets from the database.
     *
     * <p>Equivalent to {@code ESSeachAPI.esSearchRaw()} but returns a vendor-neutral
     * {@link ContentSearchResponse} instead of {@code SearchResponse}.</p>
     *
     * @param query                the JSON search query
     * @param live                 {@code true} to query the live index
     * @param user                 the user performing the action
     * @param respectFrontendRoles whether front-end roles should be applied
     * @return raw search response; never {@code null}
     */
    ContentSearchResponse searchRaw(
            String query,
            boolean live,
            User user,
            boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException;

    /**
     * Returns related content for a given contentlet identifier (no pagination).
     *
     * @param contentletIdentifier identifier of the content whose relations will be searched
     * @param relationshipName     name of the relationship field
     * @param pullParents          {@code true} to search for parents, {@code false} for children
     * @param live                 {@code true} to query the live index
     * @param user                 the user performing the action
     * @param respectFrontendRoles whether front-end roles should be applied
     */
    ContentSearchResponse searchRelated(
            String contentletIdentifier,
            String relationshipName,
            boolean pullParents,
            boolean live,
            User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException;

    /**
     * Returns related content for a given contentlet (no pagination).
     *
     * @param contentlet           the content whose relations will be searched
     * @param relationshipName     name of the relationship field
     * @param pullParents          {@code true} to search for parents, {@code false} for children
     * @param live                 {@code true} to query the live index
     * @param user                 the user performing the action
     * @param respectFrontendRoles whether front-end roles should be applied
     */
    ContentSearchResponse searchRelated(
            Contentlet contentlet,
            String relationshipName,
            boolean pullParents,
            boolean live,
            User user,
            boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException;

    /**
     * Returns paginated related content for a given contentlet identifier.
     *
     * @param contentletIdentifier identifier of the content whose relations will be searched
     * @param relationshipName     name of the relationship field
     * @param pullParents          {@code true} to search for parents, {@code false} for children
     * @param live                 {@code true} to query the live index
     * @param user                 the user performing the action
     * @param respectFrontendRoles whether front-end roles should be applied
     * @param limit                maximum number of results ({@code -1} for no limit)
     * @param offset               result offset for pagination
     * @param sortBy               sort expression, or {@code null} for default sort
     */
    ContentSearchResponse searchRelated(
            String contentletIdentifier,
            String relationshipName,
            boolean pullParents,
            boolean live,
            User user,
            boolean respectFrontendRoles,
            int limit,
            int offset,
            String sortBy)
            throws DotDataException, DotSecurityException;

    /**
     * Returns paginated related content for a given contentlet.
     *
     * @param contentlet           the content whose relations will be searched
     * @param relationshipName     name of the relationship field
     * @param pullParents          {@code true} to search for parents, {@code false} for children
     * @param live                 {@code true} to query the live index
     * @param user                 the user performing the action
     * @param respectFrontendRoles whether front-end roles should be applied
     * @param limit                maximum number of results ({@code -1} for no limit)
     * @param offset               result offset for pagination
     * @param sortBy               sort expression, or {@code null} for default sort
     */
    ContentSearchResponse searchRelated(
            Contentlet contentlet,
            String relationshipName,
            boolean pullParents,
            boolean live,
            User user,
            boolean respectFrontendRoles,
            int limit,
            int offset,
            String sortBy)
            throws DotDataException, DotSecurityException;
}
