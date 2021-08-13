package com.dotmarketing.portlets.structure.model;

import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ContentletRelationshipsSerializer extends JsonSerializer<ContentletRelationships> {

    private static final String RELATIONSHIP_KEY = "__##relationships##__";

    final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void serialize(final ContentletRelationships contentletRelationships,
            final JsonGenerator gen,
            final SerializerProvider serializers) throws IOException {

        final Contentlet contentlet = contentletRelationships.getContentlet();

        final DotContentletTransformer transformer = new DotTransformerBuilder()
                .defaultOptions().content(contentlet).build();
        final Map<String, Object> contentletMap = transformer.toMaps().stream().findFirst()
                .orElse(Collections.emptyMap());

        final Map<String, Object> contentletMapWithRelationships = new HashMap<>();
        contentletMapWithRelationships.putAll(contentletMap);
        contentletMapWithRelationships.remove(RELATIONSHIP_KEY);

        MAPPER.writer()
                .withAttribute("relationshipsRecords", contentletRelationships.getRelationshipsRecords())
                .withAttribute("contentlet", contentletMapWithRelationships);
    }
}
