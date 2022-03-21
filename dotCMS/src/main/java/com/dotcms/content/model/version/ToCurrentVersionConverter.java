package com.dotcms.content.model.version;

import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jonpeterson.jackson.module.versioning.VersionedModelConverter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class ToCurrentVersionConverter implements VersionedModelConverter {

    public static final List<String> systemFieldTypes = Arrays.asList("Tags", "Categories", "Constant");

    @Override
    public ObjectNode convert(final ObjectNode modelData, final String modelVersion,
            final String targetModelVersion,
            final JsonNodeFactory nodeFactory) {
        // we made model version an int
        final int version = Integer.parseInt(modelVersion);
        final int target = Integer.parseInt(targetModelVersion);
        if (version < target) {
            removeSystemFields(modelData);
        }
        return modelData;
    }

    /**
     *
     */
    private void removeSystemFields(final ObjectNode modelData) {
        modelData.remove("host");
        modelData.remove("folder");
        final JsonNode fields = modelData.get("fields");
        if (null != fields) {
            final Iterator<JsonNode> elements = fields.elements();
            while (elements.hasNext()) {
                final ObjectNode next = (ObjectNode) elements.next();
                final String type = next.get("type").asText();
                if (systemFieldTypes.contains(type)) {
                    Logger.debug(ToCurrentVersionConverter.class, "");
                    elements.remove();
                }
            }
        }
    }

}
