package com.dotcms.content.index.opensearch;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.google.common.annotations.VisibleForTesting;
import java.io.StringWriter;
import java.util.Optional;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.HitsMetadata;

/**
 * This cache will take an OpenSearch SearchRequest as a key and will return the search results for
 * that SearchRequest from cache if they are available. This entire cache can be turned off by setting
 * OS_CACHE_SEARCH_QUERIES = false in your dotmarketing-config.properties
 *
 * This is the OpenSearch equivalent of ESQueryCache, using opensearch-java client library instead
 * of the Elasticsearch client library.
 *
 * @author fabrizio
 *
 */
public class OSQueryCache implements Cachable {

    /**
     * Shared JSON mapper used to serialize OpenSearch requests to a content-based string.
     * The OpenSearch Java client's request objects (SearchRequest, CountRequest) do NOT
     * override {@code hashCode()} with content-based logic — they inherit the JVM identity
     * hash. Serializing to JSON gives a stable, content-based key so identical requests
     * share the same cache entry across different object instances.
     */
    private static final JacksonJsonpMapper JSONP_MAPPER = new JacksonJsonpMapper();

    private final DotCacheAdministrator cache;

    @VisibleForTesting
    public OSQueryCache(DotCacheAdministrator cache) {
        this.cache = cache;
    }

    public OSQueryCache() {
        this(CacheLocator.getCacheAdministrator());
    }

    final static String[] groups = new String[] {"osquerycache","osquerycountcache"};

    @Override
    public String getPrimaryGroup() {
        return groups[0];
    }

    @Override
    public String[] getGroups() {
        return groups;
    }

    @Override
    public void clearCache() {
       for(final String group : groups){
           cache.flushGroup(group);
       }
    }

    /**
     * Returns a content-based cache key for the given OpenSearch request.
     *
     * <p>The OpenSearch Java client's {@code SearchRequest} and {@code CountRequest} do
     * <strong>not</strong> override {@code Object.hashCode()} — they inherit JVM identity
     * hashes, so two requests built with identical parameters produce different hashes.
     * Using that identity hash as a cache key means every request is a cache miss.</p>
     *
     * <p>Both types implement {@link JsonpSerializable}, so we serialize them to their JSON
     * wire representation and use that string as the key. Identical requests always produce
     * identical JSON, giving stable, content-based cache lookup.</p>
     *
     * @param request the OpenSearch request (SearchRequest or CountRequest)
     * @return content-based string key for the cache
     */
    @VisibleForTesting
    final String hash(final Object request) {
        if (request instanceof JsonpSerializable) {
            final StringWriter sw = new StringWriter();
            try (final jakarta.json.stream.JsonGenerator gen =
                    JSONP_MAPPER.jsonProvider().createGenerator(sw)) {
                ((JsonpSerializable) request).serialize(gen, JSONP_MAPPER);
            } catch (final Exception e) {
                // Serialization should never fail for valid requests; fall back to identity hash
                return String.valueOf(System.identityHashCode(request));
            }
            return sw.toString();
        }
        return String.valueOf(System.identityHashCode(request));
    }

    /**
     * Takes a SearchRequest and returns an Optional<HitsMetadata> for it.
     * HitsMetadata is the OpenSearch equivalent of Elasticsearch's SearchHits.
     *
     * @param searchRequest the OpenSearch SearchRequest
     * @return Optional containing cached search results if available
     */
    public Optional<HitsMetadata<Object>> get(final SearchRequest searchRequest) {
        final String hash = hash(searchRequest);
        return Optional.ofNullable((HitsMetadata<Object>) cache.getNoThrow(hash, groups[0]));
    }

    /**
     * Puts search results into the cache using the SearchRequest as the key.
     *
     * @param searchRequest the OpenSearch SearchRequest used as cache key
     * @param hits the search results to cache
     */
    public void put(final SearchRequest searchRequest, final HitsMetadata<Object> hits) {
        final String hash = hash(searchRequest);
        cache.put(hash, hits, groups[0]);
    }

    /**
     * Takes a CountRequest and returns an Optional<Long> for it.
     *
     * @param countRequest the OpenSearch CountRequest
     * @return Optional containing cached count if available
     */
    public Optional<Long> get(final CountRequest countRequest) {
        final String hash = hash(countRequest);
        return Optional.ofNullable((Long) cache.getNoThrow(hash, groups[1]));
    }

    /**
     * Puts Long Count into the cache using the CountRequest as the key.
     *
     * @param countRequest the OpenSearch CountRequest used as cache key
     * @param count the count result to cache
     */
    public void put(final CountRequest countRequest, final Long count) {
        final String hash = hash(countRequest);
        cache.put(hash, count, groups[1]);
    }
}