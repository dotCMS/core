/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.sitesearch;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.business.IndiciesInfo;
import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexTag;
import com.dotcms.content.index.VersionedIndices;
import com.dotcms.content.index.VersionedIndicesAPI;
import com.dotcms.content.index.VersionedIndicesImpl;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.DotSearchException;
import com.dotcms.content.index.domain.SearchHit;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.content.index.opensearch.OSIndexAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publishing.job.SiteSearchJobProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.ScheduledTask;
import com.dotmarketing.quartz.TaskRuntimeValues;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Try;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.generic.Bodies;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.quartz.SchedulerException;

/**
 * OpenSearch implementation of {@link SiteSearchAPI}.
 *
 * <p>Vendor-specific counterpart to {@link ESSiteSearchAPI}. The two implementations are kept
 * functionally symmetric and are selected at runtime by the {@link SiteSearchAPIImpl} router based
 * on the migration phase. This class confines every {@code org.opensearch.*} type to its private
 * helpers — the {@link SiteSearchAPI} contract it implements is vendor-neutral.</p>
 *
 * <h2>Index source of truth</h2>
 * <p>Where {@link ESSiteSearchAPI} reads the active site-search index name from the legacy
 * {@code IndiciesAPI}, this class uses {@link VersionedIndicesAPI} — the canonical OpenSearch index
 * registry — via the {@code siteSearch} slot of the default ({@link VersionedIndices#OPENSEARCH_3X})
 * versioned indices. Index <em>lifecycle</em> operations (create/list/delete/alias) are delegated to
 * the OpenSearch {@link IndexAPI} provider ({@link OSIndexAPIImpl}) directly rather than the neutral
 * router, because the {@link SiteSearchAPIImpl} router is already the single phase-aware fan-out point
 * — routing through the neutral {@code IndexAPI} router here would dual-write a second time.</p>
 *
 * <h2>Index naming</h2>
 * <p>Site-search index names flow through this class as plain <em>logical</em> names
 * (e.g. {@code sitesearch_1718000000000}) — that is what {@link #listIndices()}, the search/alias
 * parameters, and the {@code siteSearch} pointer expose, so the ES∪OS merge in the router keeps
 * deduplicating and nothing user-facing ever shows a {@code .os} suffix. The {@code .os}
 * {@link com.dotcms.content.index.IndexTag} is applied at the <strong>physical boundary only</strong>
 * (see {@link #physicalName(String)} / {@link #osTagged(String)}), so the actual OpenSearch index is
 * {@code cluster_<id>.sitesearch_….os} — consistent with every other migrated index and with the
 * {@code .os}-tagged pointer persisted by {@link VersionedIndicesAPI}. Tagging the physical index also
 * removes the single-cluster ({@code esSameAsOs()}) name-collision hazard the previous bare naming
 * carried (issue #36672).</p>
 *
 * @author Fabrizio Araya
 * @see ESSiteSearchAPI
 * @see SiteSearchAPIImpl
 * @see com.dotcms.content.index.opensearch.OSSearchAPIImpl
 */
@ApplicationScoped
@Default
public class OSSiteSearchAPI implements SiteSearchAPI {

    /**
     * Response deserializer with {@code TDocument} bound to {@code Object} (JSON objects become
     * {@code Map}). The query body is sent through the low-level (generic) client so nested
     * sub-aggregations are preserved; the bare {@code SearchResponse._DESERIALIZER} has no document
     * deserializer bound and would fail on a hit carrying a {@code _source}. Mirrors
     * {@link com.dotcms.content.index.opensearch.OSSearchAPIImpl}.
     */
    private static final JsonpDeserializer<SearchResponse<Object>> SEARCH_RESPONSE_DESERIALIZER =
            SearchResponse.createSearchResponseDeserializer(JsonpDeserializer.of(Object.class));

    private final OSClientProvider clientProvider;
    private final IndexAPI indexApi;

    /** CDI-managed constructor. */
    @Inject
    public OSSiteSearchAPI() {
        this(CDIUtils.getBeanThrows(OSClientProvider.class),
                CDIUtils.getBeanThrows(OSIndexAPIImpl.class));
    }

    /** Package-private constructor for testing. */
    @VisibleForTesting
    OSSiteSearchAPI(final OSClientProvider clientProvider,
            final IndexAPI indexApi) {
        this.clientProvider = clientProvider;
        this.indexApi = indexApi;
    }

    // =========================================================================
    // Index listing
    // =========================================================================

    @Override
    public List<String> listIndices() {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return Collections.emptyList();
        }
        // The physical OS indices are .os-tagged; strip back to logical names so the ES∪OS merge in
        // SiteSearchAPIImpl.listIndices() deduplicates and no .os leaks to the portlet (issue #36672).
        final List<String> indices = indexApi.listIndices().stream()
                .filter(IndexType.SITE_SEARCH::is)
                .map(IndexTag::strip)
                .distinct()
                .collect(Collectors.toList());

        Collections.sort(indices);
        Collections.reverse(indices);
        setDefaultToSpecificPosition(indices, 0);
        return indices;
    }

    /**
     * Moves the active (default) site-search index to {@code indexPosition} of the list, mirroring
     * {@link ESSiteSearchAPI} but resolving the default from {@link VersionedIndicesAPI}.
     */
    private void setDefaultToSpecificPosition(final List<String> list, final int indexPosition) {
        if (list == null || list.size() <= 1) {
            return;
        }
        final String defaultIndice = defaultSiteSearchIndex().orElse(null);
        if (UtilMethods.isSet(defaultIndice) && !list.isEmpty()) {
            final int index = list.indexOf(defaultIndice);
            if (index < 0) {
                Logger.warn(this.getClass(), String.format(
                        "The default site search '%s' index was not found in the list of indices.",
                        defaultIndice));
            } else {
                list.remove(index);
                list.add(indexPosition, defaultIndice);
            }
        }
    }

    @Override
    public List<String> listClosedIndices() {
        final List<String> indices = new ArrayList<>();
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return indices;
        }
        for (final String indexName : indexApi.getClosedIndexes()) {
            if (IndexType.SITE_SEARCH.is(indexName)) {
                indices.add(IndexTag.strip(indexName)); // logical form — see listIndices (issue #36672)
            }
        }
        Collections.sort(indices);
        Collections.reverse(indices);
        return indices;
    }

    // =========================================================================
    // Search & aggregations
    // =========================================================================

    @Override
    public SiteSearchResults search(final String query, final int start, final int rows) {
        final SiteSearchResults results = new SiteSearchResults();
        if (query == null) {
            results.setError("null query");
            return results;
        }
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return results;
        }
        try {
            return search(defaultSiteSearchIndex().orElse(null), query, start, rows);
        } catch (final Exception e) {
            results.setError(e.getMessage());
        }
        return results;
    }

    @Override
    public SiteSearchResults search(String indexName, String query, final int offset, final int limit) {
        if (!UtilMethods.isSet(query)) {
            query = "*";
        }
        final SiteSearchResults results = new SiteSearchResults();

        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return results;
        }

        final boolean isJson = StringUtils.isJson(query);

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if (query.contains("/")) {
            query = query.replaceAll("/", "\\\\/");
        }

        results.setQuery(query);
        results.setLimit(limit);
        results.setOffset(offset);

        try {
            if (indexName == null) {
                indexName = defaultSiteSearchIndex().orElse(null);
            }
            if (!IndexType.SITE_SEARCH.is(indexName)) {
                throw new DotSearchException(indexName + " is not a sitesearch index");
            }
            results.setIndex(indexName);

            final JSONObject body;
            if (!isJson) {
                body = new JSONObject();
                body.put("query", new JSONObject().put("query_string",
                        new JSONObject().put("query", query).put("default_field", "*")));
                if (limit > 0) {
                    body.put("size", limit);
                }
                if (offset > 0) {
                    body.put("from", offset);
                }
                body.put("highlight", new JSONObject().put("fields",
                        new JSONObject().put("content", new JSONObject().put("fragment_size", 255))));
            } else {
                body = new JSONObject(query);
            }

            final ContentSearchResponse response = rawSearch(physicalName(indexName), body);
            results.setTook(response.tookMillis() + "ms");
            if (!isJson) {
                results.setQuery(body.toString());
            }

            final SearchHits hits = response.hits();
            results.setTotalResults(hits.getTotalHits().value());

            float maxScore = 0f;
            for (final SearchHit hit : hits) {
                final SiteSearchResult ssr = new SiteSearchResult(new HashMap<>(hit.getSourceAsMap()));
                ssr.setScore(hit.getScore());
                maxScore = Math.max(maxScore, hit.getScore());
                // TODO OS: the neutral SearchHit DTO does not carry per-field highlights yet.
                // Site-search highlights are a best-effort extra (the ES path also swallows
                // highlight failures); set empty until the neutral hit exposes highlight fragments.
                ssr.setHighLight(new String[0]);
                results.getResults().add(ssr);
            }
            results.setMaxScore(maxScore);

        } catch (final Exception e) {
            Logger.error(OSSiteSearchAPI.class, e.getMessage(), e);
            results.setError(e.getMessage());
        }

        return results;
    }

    @Override
    public Map<String, Aggregation> getAggregations(String indexName, String query)
            throws DotDataException {
        indexName = resolveIndexOrAlias(indexName);
        if (indexName == null || !IndexType.SITE_SEARCH.is(indexName)) {
            throw new DotSearchException(indexName + " is not a sitesearch index or alias");
        }

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if (query.contains("/")) {
            query = query.replaceAll("/", "\\\\\\\\/");
        }

        try {
            final ContentSearchResponse response = rawSearch(physicalName(indexName), new JSONObject(query));
            return response.aggregationTree();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error getting aggregations for query.\n" + e.getMessage(), e);
            throw new DotSearchException("Error getting aggregations for query.\n" + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #getAggregations(String, String)} instead.
     */
    @Deprecated
    @Override
    public Map<String, Aggregation> getFacets(String indexName, String query) throws DotDataException {
        indexName = resolveIndexOrAlias(indexName);
        if (indexName == null || !IndexType.SITE_SEARCH.is(indexName)) {
            throw new DotSearchException(indexName + " is not a sitesearch index or alias");
        }

        //https://github.com/elasticsearch/elasticsearch/issues/2980
        if (query.contains("/")) {
            query = query.replaceAll("/", "\\\\\\\\/");
        }

        try {
            final ContentSearchResponse response = rawSearch(physicalName(indexName), new JSONObject(query));
            return response.aggregationTree();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error getting Facets for query.\n" + e.getMessage(), e);
            throw new DotSearchException("Error getting Facets for query.\n" + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Default index activation / inspection
    // =========================================================================

    @Override
    public boolean isDefaultIndex(final String indexName) throws DotDataException {
        return indexName != null && indexName.equals(defaultSiteSearchIndex().orElse(null));
    }

    @Override
    public void activateIndex(final String indexName) throws DotDataException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        if (!IndexType.SITE_SEARCH.is(indexName)) {
            return;
        }
        final VersionedIndicesImpl.Builder builder = copyDefaultIndices();
        builder.siteSearch(indexName);
        saveDefaultIndices(builder);
    }

    @Override
    public void deactivateIndex(final String indexName) throws DotDataException, IOException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        if (!IndexType.SITE_SEARCH.is(indexName)) {
            return;
        }
        // Rebuild the default indices without the site-search slot. saveIndices() does a
        // delete-by-version then re-insert, so omitting the slot clears the pointer while preserving
        // the content live/working rows. If site-search was the ONLY slot for this version, the
        // rebuilt info would be empty (saveIndices rejects empty), so drop the version row instead.
        final VersionedIndicesImpl rebuilt = copyDefaultIndicesExceptSiteSearch().build();
        final VersionedIndicesAPI api = APILocator.getVersionedIndicesAPI();
        if (rebuilt.hasAnyIndex()) {
            api.saveIndices(rebuilt);
        } else {
            api.removeVersion(rebuilt.version());
        }
        api.clearCache();

        // I-5 fallback invalidation. defaultSiteSearchIndex() falls back to the legacy IndiciesAPI
        // pointer when the versioned slot is empty. In phases where ES is NOT a write provider
        // (Phase 3), the router does not fan this deactivation out to ESSiteSearchAPI, so the legacy
        // pointer would survive and the fallback would keep reporting this index as the active
        // default — permanently blocking its deletion. Clear the legacy pointer here when it still
        // points at the index being deactivated. In dual-write phases ESSiteSearchAPI also clears it
        // via the fan-out, so this is idempotent (issue #36360 review).
        clearLegacyPointerIfDefault(indexName);
    }

    /**
     * Clears the legacy {@link com.dotmarketing.business.IndiciesAPI} site-search pointer when it
     * still points at {@code indexName}. Complements the I-5 read-fallback in
     * {@link #defaultSiteSearchIndex()} so a deactivated Phase-0 default is not resurrected by that
     * fallback in phases where ES is not part of the write fan-out (e.g. Phase 3). Matches on the
     * exact name so deactivating one index never clears a different index's legacy pointer.
     */
    private void clearLegacyPointerIfDefault(final String indexName) throws DotDataException {
        final String legacyDefault = legacyDefaultSiteSearchIndex().orElse(null);
        if (legacyDefault == null || !legacyDefault.equals(indexName)) {
            return;
        }
        final IndiciesInfo info = APILocator.getIndiciesAPI().loadIndicies();
        final IndiciesInfo.Builder builder = IndiciesInfo.Builder.copy(info);
        builder.setSiteSearch(null);
        APILocator.getIndiciesAPI().point(builder.build());
    }

    // =========================================================================
    // Index creation / mapping
    // =========================================================================

    @Override
    public synchronized boolean createSiteSearchIndex(String indexName, final String alias, final int shards)
            throws DotSearchException, IOException {
        if (indexName == null) {
            return false;
        }
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return false;
        }

        indexName = indexName.toLowerCase();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // OpenSearch-format resources, kept separate from their es-*.json counterparts so the OS
        // index lifecycle never depends on an ES-named file. Settings: the legacy
        // es-sitesearch-settings.json uses ES-only token filter syntax (e.g. edgeNGram / side) that
        // the typed OpenSearch IndexSettings deserializer rejects; os-sitesearch-settings.json
        // declares the same analyzers (standard_content, partial_content) in OpenSearch syntax.
        // The mapping is functionally identical to es-sitesearch-mapping.json today, but owning a
        // dedicated os-sitesearch-mapping.json decouples the two vendors — a future ES mapping
        // change cannot silently alter OS behaviour.
        // Read via getResourceAsStream so the index lifecycle works when these resources are packaged
        // inside a JAR (new File(url.getPath()) only works for filesystem URLs and NPEs if missing).
        final String settings = readResource(classLoader, "os-sitesearch-settings.json");
        final String mapping = readResource(classLoader, "os-sitesearch-mapping.json");

        try {
            // Create the .os-tagged physical index (osTagged), matching physicalName()/putMapping()
            // so create, mapping, writes and searches all hit the same index (issue #36672).
            indexApi.createIndex(osTagged(indexName), settings, shards);
        } catch (final Exception e) {
            throw new DotSearchException("Error creating OpenSearch site search index: " + e.getMessage(), e);
        }

        if (UtilMethods.isSet(alias)) {
            indexApi.createAlias(osTagged(indexName), alias);
        }

        putMapping(indexName, mapping);

        return true;
    }

    /**
     * Applies the mapping to the site-search index via a raw {@code PUT /<index>/_mapping}.
     *
     * <p>Done here rather than via {@code MappingOperationsOS} on purpose: that helper force-tags the
     * physical name with {@code .os}, which would target a different index than the untagged one this
     * class creates and queries (see the class "Index naming" note), leaving the real index on the
     * dynamic default mapping (string fields become {@code text}, which then breaks keyword
     * aggregations such as {@code mimeType}). Forwarding to the same untagged physical name used by
     * {@code createIndex}/search/put keeps the mapping on the index that is actually hit.</p>
     */
    /**
     * Reads a UTF-8 classpath resource fully into a String via {@code getResourceAsStream}, so it
     * resolves whether the resource sits on the filesystem or inside a packaged JAR. Throws a clear
     * {@link DotSearchException} when the resource is absent rather than NPE-ing on a null URL.
     */
    private static String readResource(final ClassLoader classLoader, final String resource)
            throws DotSearchException {
        try (final InputStream in = classLoader.getResourceAsStream(resource)) {
            if (in == null) {
                throw new DotSearchException(
                        "Required OpenSearch site search resource not found on the classpath: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new DotSearchException(
                    "Error reading OpenSearch site search resource " + resource + ": " + e.getMessage(), e);
        }
    }

    private void putMapping(final String indexName, final String mapping) throws DotSearchException {
        final String endpoint = "/" + physicalName(indexName) + "/_mapping";
        try (final Response response = clientProvider.getClient().generic()
                .execute(Requests.builder()
                        .method("PUT")
                        .endpoint(endpoint)
                        .body(Bodies.json(mapping))
                        .build())) {
            final int status = response.getStatus();
            if (status < 200 || status >= 300) {
                throw new DotSearchException("Error applying mapping to OpenSearch site search index "
                        + indexName + " — HTTP " + status + " — "
                        + response.getBody().map(Body::bodyAsString).orElse(""));
            }
        } catch (final IOException e) {
            throw new DotSearchException("Error applying mapping to OpenSearch site search index: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public synchronized boolean setAlias(String indexName, final String alias) {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return false;
        }
        if (UtilMethods.isNotSet(indexName) || UtilMethods.isNotSet(alias)) {
            throw new IllegalArgumentException(String.format(
                    " either one or both params aren't set. index: `%s`, alias: `%s` ", indexName, alias));
        }
        indexName = indexName.toLowerCase();
        indexApi.createAlias(osTagged(indexName), alias); // alias points at the .os index (issue #36672)
        // createAlias is void and throws on failure, so reaching here means the alias was created.
        // (Legacy ESSiteSearchAPI returns false here, but its only caller — ESSiteSearchPublisher —
        // ignores the result, so the divergence is benign; reporting success honestly is correct.)
        return true;
    }

    /**
     * Mirrors {@link ESSiteSearchAPI#deleteOldSiteSearchIndices()} but resolves the active index from
     * {@link VersionedIndicesAPI} and deletes through the OpenSearch {@link IndexAPI} provider.
     */
    @Override
    public void deleteIndex(final String indexName) throws DotDataException, IOException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        if (!IndexType.SITE_SEARCH.is(indexName)) {
            throw new DotDataException("Index '" + indexName + "' is not a site-search index");
        }
        // Deletes only from THIS engine (indexApi is the direct OSIndexAPIImpl, not the router). The
        // physical OS index is .os-tagged (osTagged), matching create/search. Active-index protection
        // is enforced by the SiteSearchAPIImpl router before dispatch.
        // Idempotent per engine: during migration a site-search index can exist on only one engine
        // (e.g. a Phase-0 ES-only index has no OpenSearch twin), so skip when it is absent here
        // rather than letting deleteMultiple throw index_not_found and crash the fan-out — which
        // would otherwise abort the Site Search build mid-switch (issue #36360, I-7).
        final String osIndexName = osTagged(indexName);
        if (!indexApi.indexExists(osIndexName)) {
            Logger.info(this.getClass(),
                    "Site-search index '" + indexName + "' is absent on this engine; nothing to delete.");
            return;
        }
        indexApi.deleteMultiple(new String[]{osIndexName});
    }

    @Override
    public void deleteOldSiteSearchIndices() {
        final List<String> indicesToRemove = new ArrayList<>(listIndices());

        // Keep the default (active) site-search index.
        defaultSiteSearchIndex().ifPresent(indicesToRemove::remove);

        // Keep any index that backs an alias. indicesToRemove holds logical names; the alias lookup
        // runs against the .os-tagged physical names, and the returned keys are stripped back to
        // logical so the removeAll matches (issue #36672).
        final List<String> indicesWithAlias = indexApi.getIndexAlias(
                        indicesToRemove.stream().map(OSSiteSearchAPI::osTagged).collect(Collectors.toList()))
                .keySet().stream().map(IndexTag::strip).collect(Collectors.toList());
        indicesToRemove.removeAll(indicesWithAlias);

        // Keep indices created within the last 24 hours.
        final Date yesterday = Date.from(Instant.now().minus(Duration.ofDays(1)));
        final long yesterdayTimestamp =
                Long.parseLong(ContentletIndexAPIImpl.timestampFormatter.format(yesterday));

        final List<String> recent = new ArrayList<>();
        for (final String index : indicesToRemove) {
            try {
                final long indexTimestamp = Long.parseLong(index.split("_")[1]);
                if (indexTimestamp >= yesterdayTimestamp) {
                    recent.add(index);
                }
            } catch (final RuntimeException e) {
                Logger.warn(this.getClass(),
                        "Unable to parse timestamp from site search index '" + index + "': " + e.getMessage());
            }
        }
        indicesToRemove.removeAll(recent);

        if (!indicesToRemove.isEmpty()) {
            Logger.info(this.getClass(),
                    "The following indices will be deleted: " + String.join(",", indicesToRemove));
            indexApi.deleteMultiple(
                    indicesToRemove.stream().map(OSSiteSearchAPI::osTagged).toArray(String[]::new));
        }
    }

    // =========================================================================
    // Document operations
    // =========================================================================

    @Override
    public void putToIndex(final String idx, final SiteSearchResult res, final String resultType) {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        requireValidIndexName(idx);
        try {
            if (res.getContentLength() == 0) {
                return;
            }
            if (res.getTitle() == null && res.getFileName() != null) {
                res.setTitle(res.getFileName());
            }

            // Strip HTML out of text content.
            if (res.getContent() != null && UtilMethods.isSet(res.getMimeType())
                    && res.getMimeType().contains("text/")) {
                res.getMap().put("content_raw", res.getContent());
                res.setContent(res.getContent().replaceAll("\\<.*?\\>", ""));
            }

            String desc = res.getDescription();
            if (!UtilMethods.isSet(res.getDescription()) && UtilMethods.isSet(res.getContent())) {
                desc = UtilMethods.prettyShortenString(res.getContent(), 500);
            }
            res.setDescription(desc);

            if (res.getMap().containsKey("keywords") && res.getMap().containsKey("seokeywords")) {
                res.setKeywords((String) res.getMap().get("seokeywords"));
            } else {
                res.setKeywords((String) res.getMap().get("keywords"));
            }

            Logger.debug(this.getClass(),
                    () -> "writing to index " + idx + " type: " + resultType + " url:" + res.getUrl());
            final String json = new ESMappingAPIImpl().toJsonString(res.getMap());

            final String endpoint = "/" + physicalName(idx) + "/_doc/" + res.getId();
            try (final Response response = clientProvider.getClient().generic()
                    .execute(Requests.builder()
                            .method("PUT")
                            .endpoint(endpoint)
                            .query(Map.of("refresh", "true"))
                            .body(Bodies.json(json))
                            .build())) {
                final int status = response.getStatus();
                if (status < 200 || status >= 300) {
                    throw new DotSearchException("putToIndex failed for doc " + res.getId()
                            + " on index " + idx + " — HTTP " + status);
                }
            }
        } catch (final DotSearchException e) {
            // Already a neutral failure signal — never swallow it. Propagating lets the phase
            // router apply its per-phase policy: in Phase 3 (OS is primary) the failure is
            // re-thrown so the publishing pipeline observes the data loss; in the shadow phases
            // (1/2, OS secondary) PhaseRouter swallows it and logs at WARN, so ES stays unaffected.
            throw e;
        } catch (final Exception e) {
            throw new DotSearchException("putToIndex failed for doc " + res.getId()
                    + " on index " + idx + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void putToIndex(final String idx, final List<SiteSearchResult> res, final String resultType) {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        for (final SiteSearchResult r : res) {
            putToIndex(idx, r, resultType);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public SiteSearchResult getFromIndex(final String index, final String id) {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return null;
        }
        try {
            final String physical = physicalName(index);
            final GetResponse<Map> response = clientProvider.getClient()
                    .get(g -> g.index(physical).id(id), Map.class);
            if (response.found() && response.source() != null) {
                final SiteSearchResult ssr = new SiteSearchResult(new HashMap<>(response.source()));
                ssr.setScore(1);
                return ssr;
            }
        } catch (final Exception e) {
            Logger.error(OSSiteSearchAPI.class, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void deleteFromIndex(final String idx, final String docId) {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        requireValidIndexName(idx);
        try {
            Logger.debug(this.getClass(), () -> "deleting doc " + docId + " from index " + idx);
            final String endpoint = "/" + physicalName(idx) + "/_doc/" + docId;
            try (final Response response = clientProvider.getClient().generic()
                    .execute(Requests.builder()
                            .method("DELETE")
                            .endpoint(endpoint)
                            .query(Map.of("refresh", "true"))
                            .build())) {
                final int status = response.getStatus();
                // 404 is benign — the document was already absent (idempotent delete).
                if (status >= 400 && status != 404) {
                    throw new DotSearchException("deleteFromIndex failed for doc " + docId
                            + " on index " + idx + " — HTTP " + status);
                }
            }
        } catch (final DotSearchException e) {
            throw e; // propagate; PhaseRouter applies the per-phase primary/shadow policy
        } catch (final Exception e) {
            throw new DotSearchException("deleteFromIndex failed for doc " + docId
                    + " on index " + idx + ": " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Quartz task scheduling — vendor-independent (identical to ESSiteSearchAPI)
    // =========================================================================

    @Override
    public List<ScheduledTask> getTasks() throws SchedulerException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return null;
        }
        return QuartzUtils.getScheduledTasks(ES_SITE_SEARCH_NAME);
    }

    @Override
    public ScheduledTask getTask(final String taskName) throws SchedulerException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return null;
        }
        for (final ScheduledTask task : getTasks()) {
            if (task.getJobName() != null && task.getJobName().equals(taskName)) {
                return task;
            }
        }
        return null;
    }

    @Override
    public void scheduleTask(final SiteSearchConfig config)
            throws SchedulerException, ParseException, ClassNotFoundException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        final String name = config.getJobName();
        final String cronString = config.getCronExpression();

        if (config.getJobId() == null) {
            config.setJobId(UUIDGenerator.generateUuid());
        }

        final ScheduledTask task = new CronScheduledTask(name, ES_SITE_SEARCH_NAME, "Site Search ",
                SiteSearchJobProxy.class.getCanonicalName(), new Date(), null, 1, config, cronString);
        task.setSequentialScheduled(true);

        QuartzUtils.scheduleTask(task);
    }

    @Override
    public void deleteTask(final String taskName) throws SchedulerException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        final ScheduledTask t = getTask(taskName);
        QuartzUtils.pauseJob(t.getJobName(), ES_SITE_SEARCH_NAME);
        QuartzUtils.removeTaskRuntimeValues(t.getJobName(), ES_SITE_SEARCH_NAME);
        QuartzUtils.removeJob(t.getJobName(), ES_SITE_SEARCH_NAME);
    }

    @Override
    public void pauseTask(final String taskName) throws SchedulerException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        final ScheduledTask t = getTask(taskName);
        QuartzUtils.pauseJob(t.getJobName(), ES_SITE_SEARCH_NAME);
    }

    @Override
    public SiteSearchPublishStatus getTaskProgress(final String taskName) throws SchedulerException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return null;
        }
        final TaskRuntimeValues trv = QuartzUtils.getTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME);
        if (!(trv instanceof SiteSearchPublishStatus)) {
            QuartzUtils.setTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME, new SiteSearchPublishStatus());
        }
        return (SiteSearchPublishStatus) QuartzUtils.getTaskRuntimeValues(taskName, ES_SITE_SEARCH_NAME);
    }

    @Override
    public boolean isTaskRunning(final String jobName) throws SchedulerException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return false;
        }
        return QuartzUtils.isJobRunning(jobName, ES_SITE_SEARCH_NAME);
    }

    @Override
    public void executeTaskNow(final SiteSearchConfig config)
            throws SchedulerException, ParseException, ClassNotFoundException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            return;
        }
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 10);
        final String cron = new SimpleDateFormat("ss mm H d M ? yyyy").format(cal.getTime());
        config.setCronExpression(cron);
        scheduleTask(config);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * The physical index name to use against the OpenSearch client: the {@code .os} tag is applied
     * (so Site Search follows the same naming pattern as every other migrated index) and then the
     * cluster-id prefix, matching how {@link OSIndexAPIImpl} builds its requests. Callers pass logical
     * (untagged) names; the tag is added only here, at the physical boundary (issue #36672).
     */
    private String physicalName(final String indexName) {
        return indexApi.getNameWithClusterIDPrefix(osTagged(indexName));
    }

    /**
     * Adds the {@code .os} tag to a logical Site Search index name for calls into the OpenSearch
     * {@link IndexAPI} provider, which adds only the cluster prefix — not the tag. Idempotent
     * ({@link IndexTag#tag(String)} leaves an already-tagged name unchanged), so it is safe on any
     * form. Keeps the physical OpenSearch index in lock-step with the {@code .os}-tagged pointer
     * stored in {@link VersionedIndicesAPI}.
     */
    private static String osTagged(final String logicalName) {
        return IndexTag.OS.tag(logicalName);
    }

    /** Characters OpenSearch forbids in an index name (plus the space). */
    private static final java.util.regex.Pattern INVALID_INDEX_NAME_CHARS =
            java.util.regex.Pattern.compile("[\\\\/*?\"<>|,# ]");

    /**
     * Guards a caller-supplied site-search index name before it is interpolated into an OpenSearch
     * REST endpoint (e.g. {@code /<index>/_doc/<id>}). Fails fast with a clear
     * {@link IllegalArgumentException} on a null/blank name (the NPE risk raised in review) or a name
     * carrying characters OpenSearch rejects, instead of letting a malformed name reach the cluster
     * as a cryptic HTTP 400.
     */
    private static void requireValidIndexName(final String idx) {
        if (UtilMethods.isNotSet(idx)) {
            throw new IllegalArgumentException("Site search index name must not be null or blank");
        }
        if (!idx.equals(idx.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Site search index name must be lowercase: `" + idx + "`");
        }
        if (INVALID_INDEX_NAME_CHARS.matcher(idx).find()) {
            throw new IllegalArgumentException(
                    "Site search index name contains characters OpenSearch forbids: `" + idx + "`");
        }
    }

    /**
     * Resolves a site-search index name or alias to the backing index name, mirroring the
     * alias-fallback in {@link ESSiteSearchAPI#getAggregations(String, String)}.
     */
    private String resolveIndexOrAlias(String indexName) throws DotDataException {
        if (indexName == null) {
            indexName = defaultSiteSearchIndex().orElse(null);
        }
        if (indexName != null && !indexApi.indexExists(osTagged(indexName))) {
            // try using it as an alias: resolve against the .os-tagged physical names, then strip the
            // matched index back to its logical form (issue #36672).
            indexName = IndexTag.strip(indexApi.getAliasToIndexMap(
                    listIndices().stream().map(OSSiteSearchAPI::osTagged).collect(Collectors.toList()))
                    .get(indexName));
        }
        return indexName;
    }

    /**
     * The active site-search index name from the default OpenSearch versioned indices, as a logical
     * (untagged) name.
     *
     * <p>{@link VersionedIndicesAPI} canonicalises stored names to the {@code .os}-tagged form, so the
     * raw value carries the tag. This class works in logical (untagged) space on its API surface — the
     * tag is (re)applied at the physical boundary by {@link #physicalName(String)} / {@link #osTagged}
     * — so the tag is stripped here, at the DB→logical boundary, to keep comparisons and list lookups
     * consistent (see the class "Index naming" note).</p>
     */
    private Optional<String> defaultSiteSearchIndex() {
        final Optional<String> versioned =
                loadDefaultIndices().flatMap(VersionedIndices::siteSearch).map(IndexTag::strip);
        if (versioned.isPresent()) {
            return versioned;
        }
        // Fallback to the legacy pointer. A default activated before the migration started lives only
        // in the legacy IndiciesAPI store — Phase 0 never populates VersionedIndices (the versioned
        // site-search slot is written only when an index is activated in a dual-write phase). Without
        // this fallback, switching reads to OpenSearch (Phase 2/3) leaves $sitesearch.search with no
        // resolvable default until the operator manually re-activates one (issue #36360, I-5).
        return legacyDefaultSiteSearchIndex();
    }

    /**
     * The active site-search index name from the legacy {@link com.dotmarketing.business.IndiciesAPI}
     * store, used as a fallback when the OpenSearch versioned store has no site-search pointer yet
     * (e.g. a default carried over from Phase 0). Returns empty on any error or a blank pointer.
     */
    private Optional<String> legacyDefaultSiteSearchIndex() {
        return Try.of(() -> APILocator.getIndiciesAPI().loadIndicies().getSiteSearch())
                .toJavaOptional()
                .filter(UtilMethods::isSet);
    }

    private Optional<VersionedIndices> loadDefaultIndices() {
        return Try.of(() -> APILocator.getVersionedIndicesAPI().loadDefaultVersionedIndices())
                .getOrElse(Optional.empty());
    }

    /** Builder seeded with every present slot of the default versioned indices. */
    private VersionedIndicesImpl.Builder copyDefaultIndices() {
        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        loadDefaultIndices().ifPresent(info -> {
            builder.version(info.version());
            info.live().ifPresent(builder::live);
            info.working().ifPresent(builder::working);
            info.reindexLive().ifPresent(builder::reindexLive);
            info.reindexWorking().ifPresent(builder::reindexWorking);
            info.siteSearch().ifPresent(builder::siteSearch);
        });
        return builder;
    }

    /** Builder seeded with every present slot of the default versioned indices except site-search. */
    private VersionedIndicesImpl.Builder copyDefaultIndicesExceptSiteSearch() {
        final VersionedIndicesImpl.Builder builder = VersionedIndicesImpl.builder();
        loadDefaultIndices().ifPresent(info -> {
            builder.version(info.version());
            info.live().ifPresent(builder::live);
            info.working().ifPresent(builder::working);
            info.reindexLive().ifPresent(builder::reindexLive);
            info.reindexWorking().ifPresent(builder::reindexWorking);
        });
        return builder;
    }

    private void saveDefaultIndices(final VersionedIndicesImpl.Builder builder) throws DotDataException {
        final VersionedIndicesAPI api = APILocator.getVersionedIndicesAPI();
        api.saveIndices(builder.build());
        api.clearCache();
    }

    /**
     * Executes a raw JSON search body against {@code physicalIndex} through the low-level (generic)
     * client and maps the response to the neutral {@link ContentSearchResponse}. The body is forwarded
     * verbatim (rather than round-tripped through the typed {@code SearchRequest} model) so nested
     * sub-aggregations are preserved; mirrors
     * {@link com.dotcms.content.index.opensearch.OSSearchAPIImpl}.
     */
    private ContentSearchResponse rawSearch(final String physicalIndex, final JSONObject body) {
        final OpenSearchClient client = clientProvider.getClient();
        final JsonpMapper mapper = client._transport().jsonpMapper();
        try (final Response response = client.generic().execute(Requests.builder()
                .method("POST")
                .endpoint("/" + physicalIndex + "/_search")
                .query(Map.of("typed_keys", "true"))
                .json(body.toString())
                .build())) {

            final int status = response.getStatus();
            final Body responseBody = response.getBody().orElseThrow(() -> new DotSearchException(
                    "OS site search returned an empty body (HTTP " + status + ")"));

            if (status < 200 || status >= 300) {
                throw new DotSearchException(
                        "OS site search failed: HTTP " + status + " — " + responseBody.bodyAsString());
            }

            try (final InputStream is = responseBody.body();
                 final jakarta.json.stream.JsonParser parser = mapper.jsonProvider().createParser(is)) {
                final SearchResponse<Object> searchResponse =
                        SEARCH_RESPONSE_DESERIALIZER.deserialize(parser, mapper);
                return ContentSearchResponse.from(searchResponse);
            }
        } catch (final IOException e) {
            throw new DotSearchException("OS site search execution failed: " + e.getMessage(), e);
        }
    }
}
