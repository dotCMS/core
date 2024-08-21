package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.workflow.SystemAction;
import com.dotcms.model.views.CommonViews;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Optional;

/**
 * Custom deserializer for handling system action mappings. This deserializer transforms the JSON
 * structure of system action mappings into a simplified form where workflow action IDs are
 * inlined.
 */
public class SystemActionMappingsDeserializer extends JsonDeserializer<JsonNode> {

    /**
     * Deserializes the JSON content to transform system action mappings.
     *
     * @param p    the parser used for reading JSON content
     * @param ctxt the context for deserialization
     * @return a transformed JSON node or null if no mappings are present
     * @throws IOException             if an I/O error occurs
     * @throws JsonProcessingException if a JSON processing error occurs
     */
    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JacksonException {

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode transformedMappings = mapper.createObjectNode();

        JsonNode rawSystemActionMappings = p.getCodec().readTree(p);
        if (isNotNull(rawSystemActionMappings)) {

            for (SystemAction systemAction : SystemAction.values()) {

                if (rawSystemActionMappings.has(systemAction.name())) {
                    processActionValue(
                            rawSystemActionMappings.get(systemAction.name()),
                            systemAction.name(),
                            transformedMappings
                    );
                }
            }
        }

        return jsonNodeByView(ctxt, transformedMappings);
    }

    /**
     * Retrieves a JSON node based on the active view in the given context.
     *
     * @param ctxt                the deserialization context
     * @param transformedMappings the JSON node representing the transformed mappings
     * @return the JSON node based on the active view, or null if no mappings are present
     * @throws IllegalStateException if an unexpected value is encountered in the active view
     */
    private JsonNode jsonNodeByView(final DeserializationContext ctxt,
            final ObjectNode transformedMappings) {

        // Check the active view and handle accordingly
        if (ctxt.getActiveView() != null) {

            if (ctxt.getActiveView().equals(CommonViews.ContentTypeInternalView.class)) {
                return null == transformedMappings || transformedMappings.isEmpty()
                        ? null : transformedMappings;
            } else if (ctxt.getActiveView().equals(CommonViews.ContentTypeExternalView.class)) {
                return transformedMappings;
            } else {
                throw new IllegalStateException(
                        "Unexpected value: " + ctxt.getActiveView().getName());
            }
        } else {
            return transformedMappings;
        }
    }

    /**
     * Processes the value of an action and adds it to the transformed mappings.
     *
     * @param actionValue         the JSON node representing the action value
     * @param actionName          the name of the action
     * @param transformedMappings the object node to store transformed mappings
     */
    private void processActionValue(JsonNode actionValue, String actionName,
            ObjectNode transformedMappings) {

        if (isNotNull(actionValue)) {
            if (actionValue instanceof ObjectNode
                    && actionValue.has("workflowAction")) {

                processWorkflowAction(
                        actionValue.get("workflowAction"), actionName, transformedMappings
                );
            } else if (actionValue.isTextual()) {
                transformedMappings.put(actionName, actionValue.asText());
            }
        }
    }

    /**
     * Processes workflow action and extracts the ID to add to the transformed mappings.
     *
     * @param workflowAction      the JSON node representing the workflow action
     * @param actionName          the name of the action
     * @param transformedMappings the object node to store transformed mappings
     */
    private void processWorkflowAction(JsonNode workflowAction, String actionName,
            ObjectNode transformedMappings) {

        Optional.ofNullable(workflowAction.get("id"))
                .ifPresentOrElse(
                        idNode -> transformedMappings.put(actionName, idNode.asText()),
                        () -> {
                            throw new IllegalStateException("Unable to transform actionMappings. "
                                    + "Missing 'id' in workflowAction for " + actionName);
                        }
                );
    }

    /**
     * Checks if the given JSON node is not null or empty.
     *
     * @param node the JSON node to check
     * @return true if the node is not null or empty, false otherwise
     */
    private boolean isNotNull(JsonNode node) {
        return node != null && !node.isNull();
    }

}