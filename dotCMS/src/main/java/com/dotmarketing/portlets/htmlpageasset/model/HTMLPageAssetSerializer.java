package com.dotmarketing.portlets.htmlpageasset.model;

import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Json Serializer of {@link HTMLPageAssetSerializer}
 */
public class HTMLPageAssetSerializer extends JsonSerializer<HTMLPageAsset> {
    @Override
    public void serialize(final HTMLPageAsset pageAsset, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {

        final DotContentletTransformer transformer = new DotTransformerBuilder().defaultOptions().content(pageAsset).build();
        final Map<String, Object> pageContentletMap  = transformer.toMaps().stream().findFirst().orElse(Collections.emptyMap());
        jsonGenerator.writeObject(pageContentletMap);
    }
}