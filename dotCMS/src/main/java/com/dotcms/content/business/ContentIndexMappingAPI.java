package com.dotcms.content.business;

import com.dotcms.content.model.annotation.IndexLibraryIndependent;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Vendor-neutral API for mapping and serializing {@link Contentlet} objects for index storage.
 *
 * <p>Implementations must not expose any vendor-specific types (Elasticsearch, OpenSearch, etc.)
 * in their public method signatures. Routing to the active index provider is handled by the
 * router implementation.</p>
 *
 * @author root
 * @since Mar 22nd, 2012
 */
@IndexLibraryIndependent
public interface ContentIndexMappingAPI {

	boolean RESPECT_FRONTEND_ROLES = Boolean.TRUE;
    boolean DONT_RESPECT_FRONTEND_ROLES = Boolean.FALSE;

    /**
     * Applies the given mapping to all specified indexes.
     *
     * @param indexes list of index names to update
     * @param mapping JSON mapping string
     * @return {@code true} if the mapping was acknowledged by all nodes
     */
    boolean putMapping(List<String> indexes, String mapping) throws IOException;

    /**
     * Applies the given mapping to a single index.
     *
     * @param indexName target index
     * @param mapping   JSON mapping string
     * @return {@code true} if the mapping was acknowledged
     */
    boolean putMapping(String indexName, String mapping) throws IOException;

    /**
     * Returns the current mapping for the given index as a JSON string.
     *
     * @param index index name
     * @return mapping JSON
     */
    String getMapping(String index) throws IOException;

    /**
     * Returns the mapping for a specific field in the given index.
     *
     * @param index     index name
     * @param fieldName field name
     * @return mapping source map, or an empty map if not found
     */
    Map<String, Object> getFieldMappingAsMap(String index, String fieldName) throws IOException;

    /**
     * Converts the given contentlet to its JSON index representation.
     *
     * @param contentlet contentlet to convert
     * @return JSON string ready for indexing
     */
    String toJson(Contentlet contentlet) throws DotMappingException;

    /**
     * Builds the lowercased property map used for indexing the given contentlet.
     *
     * @param contentlet contentlet to map
     * @return map of field names to values
     */
    Map<String, Object> toMap(Contentlet contentlet) throws DotMappingException;

    /**
     * Converts the given contentlet to a mapped object (currently equivalent to {@link #toJson}).
     *
     * @param con contentlet to convert
     * @return mapped representation
     */
    Object toMappedObj(Contentlet con) throws DotMappingException;

    /**
     * Serializes the given map to a JSON string using the configured {@code ObjectMapper}.
     *
     * @param map map to serialize
     * @return JSON string
     */
    String toJsonString(Map<String, Object> map) throws IOException;

    /**
     * Returns the identifiers of related contentlets that must be re-indexed when the given
     * contentlet changes.
     *
     * @param con the contentlet whose dependencies should be collected
     * @return list of dependent contentlet identifiers
     */
    List<String> dependenciesLeftToReindex(Contentlet con)
            throws DotStateException, DotDataException, DotSecurityException;

}