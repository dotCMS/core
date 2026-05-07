package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ContentFactoryIndexOperationsES.addBuilderSort;
import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.SearchAPI;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.enterprise.priv.util.SearchSourceBuilderUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * Elasticsearch implementation of {@link SearchAPI}.
 *
 * <p>Transitional class — lives in the {@code elasticsearch.*} package alongside the other
 * ES-specific implementations ({@link ESIndexAPI}, {@link ContentFactoryIndexOperationsES}).
 * Will be deleted when the ES→OS migration completes.</p>
 *
 * <p>Implements the same search logic as the legacy {@code ESSearchAPIImpl} but returns
 * vendor-neutral {@link ContentSearchResponse} / {@link ContentSearchResults} DTOs instead
 * of {@code SearchResponse} / {@code ESSearchResults}.</p>
 */
public class ContentletSearchAPIES implements SearchAPI {

    // -------------------------------------------------------------------------
    // SearchAPI implementation
    // -------------------------------------------------------------------------

    @Override
    public ContentSearchResults search(
            final String query,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        final String normalized = query != null
                ? StringUtils.lowercaseStringExceptMatchingTokens(
                        query, ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX)
                : query;

        final ContentSearchResponse resp = searchRaw(normalized, live, user, respectFrontendRoles);
        final ContentSearchResults results = new ContentSearchResults(resp, new ArrayList<>());
        results.setQuery(normalized);
        results.setRewrittenQuery(normalized);

        if (resp.hits() == null) {
            return results;
        }

        final long start = System.currentTimeMillis();
        final List<ContentletSearch> list = new ArrayList<>();

        for (final com.dotcms.content.index.domain.SearchHit sh : resp.hits()) {
            try {
                final Map<String, Object> sourceMap = sh.sourceAsMap();
                final ContentletSearch conwrapper = new ContentletSearch();
                conwrapper.setInode(sourceMap.get("inode").toString());
                list.add(conwrapper);
            } catch (final Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }

        final List<String> inodes = new ArrayList<>();
        for (final ContentletSearch conwrap : list) {
            inodes.add(conwrap.getInode());
        }

        final List<Contentlet> contentlets = APILocator.getContentletAPIImpl().findContentlets(inodes);
        for (final Contentlet contentlet : contentlets) {
            if (contentlet.getInode() != null) {
                results.add(contentlet);
            }
        }

        results.setPopulationTook(System.currentTimeMillis() - start);
        return results;
    }

    @Override
    public ContentSearchResponse searchRaw(
            final String query,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        if (!UtilMethods.isSet(query)) {
            throw new DotStateException("Search query is null");
        }

        final JSONObject completeQueryJSON;
        try {
            completeQueryJSON = new JSONObject(query);
            completeQueryJSON.put("_source", new JSONArray("[identifier, inode]"));
        } catch (final JSONException e) {
            throw new DotStateException("Unable to parse the given query.", e);
        }

        final SearchResponse esResponse =
                executeSearch(completeQueryJSON, live, user, respectFrontendRoles, -1, -1, null);
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

        final Contentlet contentlet =
                APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletIdentifier);
        return searchRelated(contentlet, relationshipName, pullParents, live, user,
                respectFrontendRoles, -1, -1, null);
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

        return searchRelated(contentlet, relationshipName, pullParents, live, user,
                respectFrontendRoles, -1, -1, null);
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

        final Contentlet contentlet =
                APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(contentletIdentifier);
        return searchRelated(contentlet, relationshipName, pullParents, live, user,
                respectFrontendRoles, limit, offset, sortBy);
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

        final JSONObject completeQueryJSON = buildRelatedQuery(contentlet, relationshipName, pullParents);
        final SearchResponse esResponse =
                executeSearch(completeQueryJSON, false, user, respectFrontendRoles, limit, offset, sortBy);
        return ContentSearchResponse.from(esResponse);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private JSONObject buildRelatedQuery(
            final Contentlet contentlet,
            final String relationshipName,
            final boolean pullParents) {
        final JSONObject criteriaMap = new JSONObject();
        try {
            if (pullParents) {
                criteriaMap.put("_source", "identifier");
                criteriaMap.put("query", new JSONObject().put("match",
                        Map.of(relationshipName.toLowerCase(), contentlet.getIdentifier())));
            } else {
                criteriaMap.put("_source", relationshipName.toLowerCase());
                criteriaMap.put("query",
                        new JSONObject().put("match", Map.of("inode", contentlet.getInode())));
            }
        } catch (final JSONException e) {
            throw new DotStateException("Unable to build related query.", e);
        }
        return new JSONObject(criteriaMap.toString());
    }

    /**
     * Executes a search against the active Elasticsearch index, applying permissions and sorting.
     */
    private SearchResponse executeSearch(
            final JSONObject queryJson,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles,
            final int limit,
            final int offset,
            final String sortBy)
            throws DotSecurityException, DotDataException {

        final String indexToHit;
        try {
            final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
            indexToHit = live ? info.getLive() : info.getWorking();
        } catch (final DotDataException ee) {
            Logger.fatal(this, "Can't get indices information", ee);
            throw new DotDataException("Unable to load index information", ee);
        }

        if (user == null && !respectFrontendRoles) {
            throw new DotSecurityException(
                    "You must specify a user if you are not respecting frontend roles");
        }

        List<Role> roles = new ArrayList<>();
        boolean isAdmin = false;
        if (user != null) {
            if (!APILocator.getRoleAPI().doesUserHaveRole(user,
                    APILocator.getRoleAPI().loadCMSAdminRole())) {
                roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
            } else {
                isAdmin = true;
            }
        }

        final RestHighLevelClient client = RestHighLevelClientProvider.getInstance().getClient();
        final SearchRequest request = new SearchRequest(indexToHit);

        final StringBuffer perms = new StringBuffer();
        if (!isAdmin && !queryJson.has("permissions:")) {
            APILocator.getContentletAPIImpl()
                    .addPermissionsToQuery(perms, user, roles, respectFrontendRoles);
        }

        if (perms.length() > 0) {
            try {
                final JSONObject permissionsFilter = new JSONObject().put("query_string",
                        new JSONObject().put("query", perms.toString().trim()));
                JSONArray boolFilters = new JSONArray("[" + permissionsFilter + "]");

                if (queryJson.has("query")) {
                    final JSONObject currentQueryJSON =
                            new JSONObject(queryJson.getJSONObject("query").toString());
                    boolFilters = new JSONArray("[" + permissionsFilter + "," + currentQueryJSON + "]");
                }

                final JSONObject filteredJSON = new JSONObject().put("bool",
                        new JSONObject().put("must", new JSONObject().put("bool",
                                new JSONObject().put("must", boolFilters))));
                queryJson.put("query", filteredJSON);
            } catch (final JSONException e) {
                throw new DotStateException("Unable to parse the given query.", e);
            }
        }

        try {
            final SearchSourceBuilder searchSourceBuilder =
                    SearchSourceBuilderUtil.getSearchSourceBuilder(queryJson.toString())
                            .timeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));

            if (limit > 0) {
                searchSourceBuilder.size(limit);
            }
            if (offset > 0) {
                searchSourceBuilder.from(offset);
            }
            if (UtilMethods.isSet(sortBy)) {
                addBuilderSort(sortBy, searchSourceBuilder);
            }

            request.source(searchSourceBuilder);
            return client.search(request, RequestOptions.DEFAULT);
        } catch (final IOException e) {
            throw new DotStateException(e.getMessage(), e);
        }
    }
}
