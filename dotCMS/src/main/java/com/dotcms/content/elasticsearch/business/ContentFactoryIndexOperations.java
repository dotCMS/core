package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.content.model.annotation.NoExternalDependencies;
import java.util.List;


/**
 * Service interface that abstracts index operations for content factory functionality.
 * This interface provides a clean abstraction layer that can be implemented by different
 * search engine providers (Elasticsearch, OpenSearch, etc.) without exposing provider-specific types.
 *
 * @author Fabrizio Araya
 */
@NoExternalDependencies
public interface ContentFactoryIndexOperations {

    String inferIndexToHit(final String query);

    long indexCount(final String query);

    SearchHits searchHits(String query, int limit, int offset, String sortBy);

    List<String> search(String query, int limit, int offset);

}