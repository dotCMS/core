package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.ESIndexAPI.INDEX_OPERATIONS_TIMEOUT_IN_MS;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexMappingRestOperations;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.google.common.annotations.VisibleForTesting;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch implementation of {@link IndexMappingRestOperations}.
 *
 * <p>Wraps the three mapping REST calls ({@code putMapping}, {@code getMapping},
 * {@code getFieldMappingAsMap}) that use the ES {@code RestHighLevelClient}.
 * All vendor-specific types are confined to this class and never exposed
 * through the {@link IndexMappingRestOperations} contract.</p>
 *
 * @author Fabrizio Araya
 * @see MappingOperationsOS
 * @see com.dotcms.content.elasticsearch.business.ESMappingAPIImpl
 */
@ApplicationScoped
public class MappingOperationsES implements IndexMappingRestOperations {

    private final IndexAPI esIndexAPI;

    /** CDI-managed default constructor. */
    public MappingOperationsES() {
        this(APILocator.getESIndexAPI());
    }

    /** Package-private constructor for unit testing. */
    @VisibleForTesting
    MappingOperationsES(final IndexAPI esIndexAPI) {
        this.esIndexAPI = esIndexAPI;
    }

    @Override
    public boolean putMapping(final List<String> indexes, final String mapping) throws IOException {
        final PutMappingRequest request = new PutMappingRequest(
                indexes.stream()
                        .map(esIndexAPI::getNameWithClusterIDPrefix)
                        .toArray(String[]::new));
        request.setTimeout(TimeValue.timeValueMillis(INDEX_OPERATIONS_TIMEOUT_IN_MS));
        request.source(mapping, XContentType.JSON);

        return RestHighLevelClientProvider.getInstance()
                .getClient()
                .indices()
                .putMapping(request, RequestOptions.DEFAULT)
                .isAcknowledged();
    }

    @Override
    public String getMapping(final String index) throws IOException {
        final GetMappingsRequest request = new GetMappingsRequest();
        request.indices(index);

        return RestHighLevelClientProvider.getInstance()
                .getClient()
                .indices()
                .getMapping(request, RequestOptions.DEFAULT)
                .mappings()
                .get(index)
                .source()
                .string();
    }

    @Override
    public Map<String, Object> getFieldMappingAsMap(final String index,
            final String fieldName) throws IOException {
        final GetFieldMappingsRequest request = new GetFieldMappingsRequest();
        request.indices(index).fields(fieldName);

        final GetFieldMappingsResponse response = RestHighLevelClientProvider.getInstance()
                .getClient()
                .indices()
                .getFieldMapping(request, RequestOptions.DEFAULT);

        final GetFieldMappingsResponse.FieldMappingMetadata meta =
                response.mappings().getOrDefault(index, Collections.emptyMap()).get(fieldName);

        return meta != null ? meta.sourceAsMap() : Collections.emptyMap();
    }
}