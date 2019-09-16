package com.dotmarketing.portlets.workflows.model;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

public class SystemActionWorkflowActionMappingDeserializer extends JsonDeserializer<SystemActionWorkflowActionMapping> {

    @Override
    public SystemActionWorkflowActionMapping deserialize(final JsonParser parser, final DeserializationContext context)
            throws IOException, JsonProcessingException {

        final ObjectNode     objectNode             = parser.readValueAsTree();
        final JsonNode       nodeWorkflowAction     = objectNode.get("workflowAction");
        final WorkflowAction workflowAction         = readValueAs(nodeWorkflowAction, WorkflowAction.class, parser);
        final String         identifier             = objectNode.get("identifier").asText();
        final WorkflowAPI.SystemAction systemAction = WorkflowAPI.SystemAction.fromString(objectNode.get("systemAction").asText());
        final Boolean        isOwnerContentType     = objectNode.get("ownerContentType").asBoolean();
        final Boolean        isOwnerScheme          = objectNode.get("ownerScheme").asBoolean();
        Object               owner                  = null;

        if (isOwnerContentType) {

            owner = readContentTypeValueAs(objectNode.get("owner"));
        }

        if (isOwnerScheme) {

            owner = readValueAs(objectNode.get("owner"), WorkflowScheme .class, parser);
        }

        return new SystemActionWorkflowActionMapping(identifier, systemAction, workflowAction, owner);
    }

    private ContentType readContentTypeValueAs(final JsonNode node) {

        final String id     = node.get("id").asText();
        final String name   = node.get("name").asText();
        final String hostId = node.get("host").asText();
        final String var    = node.get("variable").asText();

        return ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass()).id(id).name(name).host(hostId).
                variable(var) // this is the only variable important for the content type
                .build();
    }

    private <T> T readValueAs (final JsonNode node, final Class<T> clazz, final JsonParser parser)
            throws IOException {

        final JsonParser nodeParser = node.traverse();
        nodeParser.setCodec(parser.getCodec());
        return nodeParser.readValueAs(clazz);
    }
}
