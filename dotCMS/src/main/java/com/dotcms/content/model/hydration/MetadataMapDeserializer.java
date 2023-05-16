package com.dotcms.content.model.hydration;

import static com.dotcms.content.model.hydration.MetadataDelegate.CONTENT_TYPE;
import static com.dotcms.content.model.hydration.MetadataDelegate.IS_IMAGE;
import static com.dotcms.content.model.hydration.MetadataDelegate.NAME;
import static com.dotcms.content.model.hydration.MetadataDelegate.SHA_256;

import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MetadataMapDeserializer extends JsonDeserializer<Map<String, Object>> {

    @Override
    public Map<String, Object> deserialize(JsonParser parser, DeserializationContext ctx)
            throws IOException {

        final Map<String, Object> metadata = new HashMap<>();
        final JsonNode node = parser.getCodec().readTree(parser);
        final JsonNode nameNode = node.get(NAME);
        final JsonNode sha256Node = node.get(SHA_256);
        final JsonNode contentTypeNode = node.get(CONTENT_TYPE);
        final JsonNode isImageNode = node.get(IS_IMAGE);

        if (nameNode != null) {
            metadata.put(NAME, nameNode.asText());
        } else {
            Logger.warn(MetadataMapDeserializer.class, "Incomplete Metadata: missing attribute [name] ");
        }

        if (sha256Node != null) {
            metadata.put(SHA_256, sha256Node.asText());
        } else {
            Logger.warn(MetadataMapDeserializer.class, "Incomplete Metadata: missing attribute [sha256] ");
        }

        if (contentTypeNode != null) {
            metadata.put(CONTENT_TYPE, contentTypeNode.asText());
        } else {
            Logger.warn(MetadataMapDeserializer.class, "Incomplete Metadata: missing attribute [contentType] ");
        }

        if (isImageNode != null) {
            metadata.put(IS_IMAGE, isImageNode.asBoolean());
        } else {
            Logger.warn(MetadataMapDeserializer.class, "Incomplete Metadata: missing attribute [isImage] ");
        }

        return metadata;
    }



}