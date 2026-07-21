package com.dotcms.content.index.opensearch;

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
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;

/**
 * OpenSearch implementation of {@link SearchAPI}.
 *
 * <p>Executes the same JSON query format used by the Elasticsearch path by forwarding the JSON body
 * verbatim through the low-level (generic) client against the index resolved from
 * {@link com.dotcms.content.index.VersionedIndicesAPI}. The body is sent untouched (rather than
 * round-tripped through the typed {@code SearchRequest} model) so nested sub-aggregations are
 * preserved; see {@link #executeSearch}.</p>
 *
 * <p>Permissions are injected using the same Lucene-based filter logic as the ES path;
 * since the filter is expressed as a JSON query object, it is vendor-neutral.</p>
 */
@ApplicationScoped
public class OSSearchAPIImpl implements SearchAPI {

    /**
     * Response deserializer with the {@code TDocument} bound to {@code Object} (JSON objects become
     * {@code Map}), matching what {@code client.search(request, Object.class)} uses internally. We
     * deserialize the raw response ourselves because the request is sent through the generic client
     * (see {@link #executeSearch}); the bare {@code SearchResponse._DESERIALIZER} has no document
     * deserializer bound and would fail on any hit carrying a {@code _source}.
     */
    private static final JsonpDeserializer<SearchResponse<Object>> SEARCH_RESPONSE_DESERIALIZER =
            SearchResponse.createSearchResponseDeserializer(JsonpDeserializer.of(Object.class));

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

        // Normalize the query the same way search() does, so the raw path resolves mixed-case
        // field names (e.g. "contentType" -> the physical lower-case index field "contenttype").
        // Reuses the existing lowercasing helper for parity; idempotent when the caller already
        // lowercased (search() delegates here after lowercasing). Symmetric with the ES raw path.
        final String normalizedQuery = StringUtils.lowercaseStringExceptMatchingTokens(
                query, com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX);

        final JSONObject completeQueryJSON;
        try {
            completeQueryJSON = new JSONObject(normalizedQuery);
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
     * <p>The full JSON body (query, aggs, _source, from, size, sort, etc.) is forwarded verbatim to
     * OpenSearch via the low-level (generic) client against the resolved index, then the response is
     * deserialized with {@link SearchResponse#_DESERIALIZER}. The body is intentionally <b>not</b>
     * round-tripped through the typed {@code SearchRequest} model: that model drops nested
     * sub-aggregations (the sibling {@code "aggs"} of a bucket aggregation), which would silently
     * strip a {@code top_hits} nested under a {@code terms} aggregation (#36026).</p>
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
            final ContentletAPI contentletAPI = APILocator.getContentletAPIImpl();
            contentletAPI.addPermissionsToQuery(perms, user, roles, respectFrontendRoles);
            // Secondary category-permission filter: mirror the ES path
            // (ESContentletAPIImpl.applyPermissionsToQuery). Without this the categoryperms:
            // clause is never added under OS, so content restricted by category read permissions
            // leaks to users who lack the category role (Phase 3 permission-filter gap).
            contentletAPI.addCategoryPermissionsToQuery(perms, user, roles, respectFrontendRoles);
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

        // sortBy extends the body's sort clause. Applied directly on the JSON body (see below) so the
        // raw query — including any nested aggregations — reaches OpenSearch untouched.
        if (UtilMethods.isSet(sortBy)) {
            applySortBy(queryJson, sortBy);
        }

        final OpenSearchClient client = clientProvider.getClient();
        final JsonpMapper mapper = client._transport().jsonpMapper();

        // Forward the query body verbatim through the low-level (generic) client instead of
        // round-tripping it through the typed SearchRequest model. The opensearch-java request model
        // does not carry nested sub-aggregations: the sibling "aggs" key on a bucket aggregation is
        // dropped by the Aggregation deserializer, so a typed round-trip silently strips, e.g., a
        // top_hits nested under a terms aggregation (#36026). typed_keys=true is required so the
        // aggregation results in the response carry their type prefix and can be deserialized.
        try (final Response response = client.generic().execute(Requests.builder()
                .method("POST")
                .endpoint("/" + indexToHit + "/_search")
                .query(Map.of("typed_keys", "true"))
                .json(queryJson.toString())
                .build())) {

            final int status = response.getStatus();
            final Body body = response.getBody().orElseThrow(() -> new DotStateException(
                    "OS search returned an empty body (HTTP " + status + ")"));

            if (status < 200 || status >= 300) {
                throw new DotStateException(
                        "OS search failed: HTTP " + status + " — " + body.bodyAsString());
            }

            try (final InputStream is = body.body();
                 final jakarta.json.stream.JsonParser parser =
                         mapper.jsonProvider().createParser(is)) {
                final SearchResponse<Object> searchResponse =
                        SEARCH_RESPONSE_DESERIALIZER.deserialize(parser, mapper);
                return ContentSearchResponse.from(searchResponse);
            }

        } catch (final IOException e) {
            throw new DotStateException("OS search execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Appends a {@code sortBy} clause (e.g. {@code "title desc, moddate asc"}) to the query JSON's
     * {@code sort} array, mirroring {@link ContentFactoryIndexOperationsOS#addBuilderSort} but on the
     * raw JSON body: each field sorts on its {@code _dotraw} keyword variant with
     * {@code unmapped_type=keyword}. Any pre-existing {@code sort} in the body is preserved.
     */
    private void applySortBy(final JSONObject queryJson, final String sortBy) {
        try {
            final Object existing = queryJson.has("sort") ? queryJson.get("sort") : null;
            final JSONArray sortArray =
                    existing instanceof JSONArray ? (JSONArray) existing : new JSONArray();
            if (existing != null && !(existing instanceof JSONArray)) {
                sortArray.put(existing); // normalize a single object/string sort into an array
            }
            for (final String sort : sortBy.split(",")) {
                final String[] parts = sort.trim().split(" ");
                final String order =
                        parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? "desc" : "asc";
                sortArray.put(new JSONObject().put(parts[0].toLowerCase() + "_dotraw",
                        new JSONObject().put("order", order).put("unmapped_type", "keyword")));
            }
            queryJson.put("sort", sortArray);
        } catch (final JSONException e) {
            throw new DotStateException("Unable to apply sortBy to OS query.", e);
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
