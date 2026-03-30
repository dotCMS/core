package com.dotcms.content.index;

import com.dotcms.content.model.annotation.IndexLibraryIndependent;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Vendor-neutral contract for the three index-mapping REST operations that differ
 * between Elasticsearch and OpenSearch.
 *
 * <p>Implementations of this interface ({@code MappingOperationsES},
 * {@code MappingOperationsOS}) encapsulate vendor client calls so that
 * {@code ESMappingAPIImpl} (the router) stays free of vendor imports.</p>
 *
 * <p>Follows the same pattern as {@link ContentFactoryIndexOperations}.</p>
 *
 * @author Fabrizio Araya
 * @see com.dotcms.content.elasticsearch.business.MappingOperationsES
 * @see com.dotcms.content.index.opensearch.MappingOperationsOS
 */
@IndexLibraryIndependent
public interface IndexMappingRestOperations {

    /**
     * Applies the given JSON mapping to all specified indexes.
     *
     * @param indexes list of index names
     * @param mapping JSON mapping string
     * @return {@code true} if acknowledged by all nodes
     */
    boolean putMapping(List<String> indexes, String mapping) throws IOException;

    /**
     * Returns the current mapping for the given index as a JSON string.
     *
     * @param index index name
     * @return mapping JSON
     */
    String getMapping(String index) throws IOException;

    /**
     * Returns the mapping for a specific field in the given index as a plain Java map.
     *
     * @param index     index name
     * @param fieldName field name
     * @return mapping source map, or an empty map if not found
     */
    Map<String, Object> getFieldMappingAsMap(String index, String fieldName) throws IOException;
}