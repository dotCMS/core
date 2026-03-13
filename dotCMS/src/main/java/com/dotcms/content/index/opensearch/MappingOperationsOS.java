package com.dotcms.content.index.opensearch;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.business.MappingOperationsES;
import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.IndexMappingRestOperations;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.opensearch.client.opensearch._types.mapping.FieldMapping;
import org.opensearch.client.opensearch.generic.Bodies;
import org.opensearch.client.opensearch.generic.Requests;
import org.opensearch.client.opensearch.generic.Response;
import org.opensearch.client.opensearch.indices.GetFieldMappingResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;
import org.opensearch.client.opensearch.indices.get_field_mapping.TypeFieldMappings;
import org.opensearch.client.opensearch.indices.get_mapping.IndexMappingRecord;

import javax.enterprise.context.ApplicationScoped;
import jakarta.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OpenSearch implementation of {@link IndexMappingRestOperations}.
 *
 * <p>Wraps the three mapping REST calls ({@code putMapping}, {@code getMapping},
 * {@code getFieldMappingAsMap}) using the OpenSearch Java client v3.x.
 * Vendor-specific types are confined to this class.</p>
 *
 * <p>{@code putMapping} uses the generic HTTP client because the typed
 * {@code PutMappingRequest} builder does not accept a raw JSON string body;
 * the raw JSON approach avoids re-serializing the ES-generated mapping.</p>
 *
 * @author Fabrizio Araya
 * @see MappingOperationsES
 * @see com.dotcms.content.elasticsearch.business.ESMappingAPIImpl
 */
@ApplicationScoped
public class MappingOperationsOS implements IndexMappingRestOperations {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OSClientProvider clientProvider;
    private final IndexAPI osIndexAPI;

    /** CDI-managed default constructor. */
    public MappingOperationsOS() {
        this(CDIUtils.getBeanThrows(OSClientProvider.class),
                CDIUtils.getBeanThrows(OSIndexAPIImpl.class));
    }

    /** Package-private constructor for unit testing. */
    @VisibleForTesting
    MappingOperationsOS(final OSClientProvider clientProvider, final IndexAPI osIndexAPI) {
        this.clientProvider = clientProvider;
        this.osIndexAPI = osIndexAPI;
    }

    /**
     * Puts the given JSON mapping on all specified indexes via raw HTTP PUT so the
     * mapping string is forwarded verbatim (the typed OS client builder does not
     * accept a raw JSON body for put-mapping).
     */
    @Override
    public boolean putMapping(final List<String> indexes, final String mapping) throws IOException {
        boolean allAcknowledged = true;
        for (final String index : indexes) {
            final String prefixed = osIndexAPI.getNameWithClusterIDPrefix(index);
            final String endpoint = "/" + prefixed + "/_mapping";
            try (Response response = clientProvider.getClient().generic()
                    .execute(Requests.builder()
                            .method("PUT")
                            .endpoint(endpoint)
                            .body(Bodies.json(mapping))
                            .build())) {
                final int status = response.getStatus();
                if (status < 200 || status >= 300) {
                    Logger.error(this, "putMapping failed for index " + prefixed
                            + " — HTTP " + status);
                    allAcknowledged = false;
                }
            }
        }
        return allAcknowledged;
    }

    @Override
    public String getMapping(final String index) throws IOException {
        final String prefixed = osIndexAPI.getNameWithClusterIDPrefix(index);
        final GetMappingResponse response = clientProvider.getClient()
                .indices()
                .getMapping(r -> r.index(prefixed));

        final IndexMappingRecord record = response.get(prefixed);
        if (record == null) {
            return "{}";
        }

        final StringWriter sw = new StringWriter();
        try (final JsonGenerator gen = clientProvider.getClient()
                ._transport().jsonpMapper()
                .jsonProvider().createGenerator(sw)) {
            record.mappings().serialize(gen,
                    clientProvider.getClient()._transport().jsonpMapper());
        }
        return sw.toString();
    }

    @Override
    public Map<String, Object> getFieldMappingAsMap(final String index,
            final String fieldName) throws IOException {
        final String prefixed = osIndexAPI.getNameWithClusterIDPrefix(index);
        final GetFieldMappingResponse response = clientProvider.getClient()
                .indices()
                .getFieldMapping(r -> r.index(prefixed).fields(fieldName));

        final TypeFieldMappings typeMappings = response.get(prefixed);
        if (typeMappings == null) {
            return Collections.emptyMap();
        }

        final FieldMapping fieldMapping = typeMappings.mappings().get(fieldName);
        if (fieldMapping == null) {
            return Collections.emptyMap();
        }

        // Serialize FieldMapping (PlainJsonSerializable) to JSON, then parse as plain Map
        final StringWriter sw = new StringWriter();
        try (final JsonGenerator gen = clientProvider.getClient()
                ._transport().jsonpMapper()
                .jsonProvider().createGenerator(sw)) {
            fieldMapping.serialize(gen,
                    clientProvider.getClient()._transport().jsonpMapper());
        }
        return MAPPER.readValue(sw.toString(), new TypeReference<Map<String, Object>>() {});
    }
}