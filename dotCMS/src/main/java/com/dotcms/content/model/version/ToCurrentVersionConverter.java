package com.dotcms.content.model.version;

import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jonpeterson.jackson.module.versioning.VersionedModelConverter;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This is a Version migration handler class. It serves as the entry point comparing the
 * modelVersion between whatever is currently registered in the Contentlet class (modelVersion) and
 * whatever is indicated in the source json
 */
public class ToCurrentVersionConverter implements VersionedModelConverter {

    public static final List<String> systemFieldTypes = Arrays
            .asList("Tags", "Categories", "Constant");

    @Override
    public ObjectNode convert(final ObjectNode modelData, final String modelVersion,
            final String targetModelVersion,
            final JsonNodeFactory nodeFactory) {
        // we made model version an int
        final int version = Integer.parseInt(modelVersion);
        final int target = Integer.parseInt(targetModelVersion);
        if (version < target) {
            removeSystemFields(modelData);
            //Add additional version migration code down here.
        }
        return modelData;
    }

    /**
     * On V2 We decided that SystemFields could easily grow out of sync quite easily For which we
     * remove them.
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
                    elements.remove();
                    Logger.debug(ToCurrentVersionConverter.class, () -> {
                        final String inode = Try.of(() -> modelData.get("inode").asText())
                                .getOrElse("unknown");
                        return String.format(
                                "field of type %s has been suppressed from the source json, for inode %s",
                                type, inode);
                    });
                }
            }
        }
    }

}
