package com.dotcms.content.index.opensearch;

import static com.dotcms.content.index.opensearch.ContentFactoryIndexOperationsOS.addBuilderSort;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.SearchAPI;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.common.model.ImmutableContentletSearch;
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
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

/**
 * OpenSearch implementation of {@link SearchAPI}.
 *
 * <p>Executes the same JSON query format used by the Elasticsearch path by deserialising the
 * JSON body with {@link SearchRequest#_DESERIALIZER} and setting the resolved index from
 * {@link com.dotcms.content.index.VersionedIndicesAPI}.</p>
 *
 * <p>Permissions are injected using the same Lucene-based filter logic as the ES path;
 * since the filter is expressed as a JSON query object, it is vendor-neutral.</p>
 */
@ApplicationScoped
public class OSSearchAPIImpl implements SearchAPI {

    private final OSClientProvider clientProvider;

    /** CDI constructor. */
    @Inject
    public OSSearchAPIImpl() {
        this(CDIUtils.getBeanThrows(OSClientProvider.class));
    }

    /** Package-private for testing. */
    OSSearchAPIImpl(final OSClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    // -------------------------------------------------------------------------
    // SearchAPI implementation
    // -------------------------------------------------------------------------

    @Override
    public ContentSearchResults<Contentlet> search(
            final String query,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        final String normalized = query != null
                ? StringUtils.lowercaseStringExceptMatchingTokens(
                        query, com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX)
                : query;

        final ContentSearchResponse resp = searchRaw(normalized, live, user, respectFrontendRoles);
        final ContentSearchResults<Contentlet> results = new ContentSearchResults<>(resp, new ArrayList<>());
        results.setQuery(normalized);
        results.setRewrittenQuery(normalized);

        if (resp.hits() == null) {
            return results;
        }

        final long start = System.currentTimeMillis();
        final List<ContentletSearch> list = new ArrayList<>();

        for (final com.dotcms.content.index.domain.SearchHit sh : resp.hits()) {
            try {
                final Map<String, Object> sourceMap = sh.getSourceAsMap();
                list.add(
                        ImmutableContentletSearch.builder()
                                .inode(sourceMap.get("inode").toString())
                                .build()
                );
            } catch (final Exception e) {
                Logger.error(this, e.getMessage(), e);
            }
        }

        final List<String> inodes = new ArrayList<>();
        for (final ContentletSearch conwrap : list) {
            inodes.add(conwrap.getInode());
        }

        final List<Contentlet> contentlets =
                APILocator.getContentletAPIImpl().findContentlets(inodes);
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

        return executeSearch(completeQueryJSON, live, user, respectFrontendRoles, -1, -1, null);
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

        final JSONObject completeQueryJSON =
                buildRelatedQuery(contentlet, relationshipName, pullParents);
        return executeSearch(completeQueryJSON, false, user, respectFrontendRoles,
                limit, offset, sortBy);
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
     * Executes a search against the active OpenSearch index, applying permissions and sorting.
     *
     * <p>Uses {@link SearchRequest#_DESERIALIZER} to parse the full JSON body (query, aggs,
     * _source, from, size, sort, etc.) and then overlays the index resolved from
     * {@link com.dotcms.content.index.VersionedIndicesAPI}.</p>
     */
    private ContentSearchResponse executeSearch(
            final JSONObject queryJson,
            final boolean live,
            final User user,
            final boolean respectFrontendRoles,
            final int limit,
            final int offset,
            final String sortBy)
            throws DotSecurityException, DotDataException {

        final String indexToHit = resolveIndex(live);

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
                    boolFilters =
                            new JSONArray("[" + permissionsFilter + "," + currentQueryJSON + "]");
                }

                final JSONObject filteredJSON = new JSONObject().put("bool",
                        new JSONObject().put("must", new JSONObject().put("bool",
                                new JSONObject().put("must", boolFilters))));
                queryJson.put("query", filteredJSON);
            } catch (final JSONException e) {
                throw new DotStateException("Unable to apply permissions to OS query.", e);
            }
        }

        // Override pagination from parameters
        try {
            if (limit > 0) {
                queryJson.put("size", limit);
            }
            if (offset > 0) {
                queryJson.put("from", offset);
            }
        } catch (final JSONException e) {
            throw new DotStateException("Unable to set pagination params.", e);
        }

        final OpenSearchClient client = clientProvider.getClient();
        final JsonpMapper mapper = client._transport().jsonpMapper();

        try {
            // Parse body fields from JSON using the SearchRequest deserializer
            final SearchRequest bodyTemplate;
            try (final InputStream is = new ByteArrayInputStream(
                    queryJson.toString().getBytes(StandardCharsets.UTF_8));
                 final jakarta.json.stream.JsonParser parser = mapper.jsonProvider()
                         .createParser(is)) {
                bodyTemplate = SearchRequest._DESERIALIZER.deserialize(parser, mapper);
            }

            // Build the final request with the resolved index added
            final SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(indexToHit);

            if (bodyTemplate.query() != null) {
                requestBuilder.query(bodyTemplate.query());
            }
            if (bodyTemplate.aggregations() != null && !bodyTemplate.aggregations().isEmpty()) {
                requestBuilder.aggregations(bodyTemplate.aggregations());
            }
            if (bodyTemplate.source() != null) {
                requestBuilder.source(bodyTemplate.source());
            }
            if (bodyTemplate.from() != null) {
                requestBuilder.from(bodyTemplate.from());
            }
            if (bodyTemplate.size() != null) {
                requestBuilder.size(bodyTemplate.size());
            }
            if (bodyTemplate.sort() != null && !bodyTemplate.sort().isEmpty()) {
                requestBuilder.sort(bodyTemplate.sort());
            }
            if (bodyTemplate.highlight() != null) {
                requestBuilder.highlight(bodyTemplate.highlight());
            }
            if (bodyTemplate.postFilter() != null) {
                requestBuilder.postFilter(bodyTemplate.postFilter());
            }
            if (bodyTemplate.trackTotalHits() != null) {
                requestBuilder.trackTotalHits(bodyTemplate.trackTotalHits());
            }

            // sortBy parameter overrides / extends body sort
            if (UtilMethods.isSet(sortBy)) {
                addBuilderSort(sortBy, requestBuilder);
            }

            final SearchResponse<Object> response =
                    client.search(requestBuilder.build(), Object.class);
            return ContentSearchResponse.from(response);

        } catch (final IOException e) {
            throw new DotStateException("OS search execution failed: " + e.getMessage(), e);
        }
    }

    private String resolveIndex(final boolean live) {
        final Optional<VersionedIndices> optional = Try
                .of(() -> APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices())
                .getOrElse(Optional.empty());

        if (optional.isEmpty()) {
            throw new com.dotmarketing.exception.DotRuntimeException(
                    "Unable to load versioned indices for OS search");
        }

        final VersionedIndices indices = optional.get();
        if (live) {
            return indices.live().orElseThrow(
                    () -> new com.dotmarketing.exception.DotRuntimeException(
                            "No live index found for OS search"));
        }
        return indices.working().orElseThrow(
                () -> new com.dotmarketing.exception.DotRuntimeException(
                        "No working index found for OS search"));
    }
}
