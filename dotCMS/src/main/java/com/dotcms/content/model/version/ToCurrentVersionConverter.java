package com.dotcms.content.model.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jonpeterson.jackson.module.versioning.VersionedModelConverter;
import java.util.Arrays;

/**
 *
 */
public class ToCurrentVersionConverter implements VersionedModelConverter {

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
     * @param modelData
     */
    private void removeSystemFields(final ObjectNode modelData) {
        modelData.remove("host");
        modelData.remove("folder");
        final JsonNode fields = modelData.get("fields");
        if (null != fields) {
            if (fields instanceof ObjectNode) {
                ObjectNode object = (ObjectNode) fields;
                object.remove(Arrays.asList("tags", "categories", "constant"));
            }
        }
    }

}
