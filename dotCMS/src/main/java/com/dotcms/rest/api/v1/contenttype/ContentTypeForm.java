package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonDeserializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ContentTypeResource}'s form
 */
@JsonDeserialize(using = ContentTypeForm.ContentTypeFormDeserialize.class)
public class ContentTypeForm  {

    private final List<ContentTypeFormEntry> entries;
    private final String requestJson;

    public ContentTypeForm(final List<ContentTypeFormEntry> entries, final String requestJson) {
        this.requestJson = requestJson;
        this.entries = entries;
    }

    public Iterable<ContentTypeFormEntry> getIterable() {
        return entries;
    }

    public ContentType getContentType() {
        return entries.get(0).contentType;
    }

    public List<String> getWorkflowsIds() {
        return entries.get(0).workflowsIds;
    }

    public Object getRequestJson() {
        return requestJson;
    }


    public static final class ContentTypeFormDeserialize extends JsonDeserializer<ContentTypeForm> {

        public static final String WORKFLOW_ATTRIBUTE_NAME = "workflow";

        @Override
        public ContentTypeForm deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {

            final String json = jsonParser.readValueAsTree().toString();
            return buildForm(json);
        }

        @VisibleForTesting
        public ContentTypeForm buildForm(final String json) {
            final List<ContentType> typesToSave = new JsonContentTypeTransformer(json).asList();
            final List<List<String>> workflows = getWorkflowIdsFromJson(json);

            final List<ContentTypeFormEntry> entries = getContentTypeFormEntries(typesToSave, workflows);

            return new ContentTypeForm(entries, json);
        }

        private List<ContentTypeFormEntry> getContentTypeFormEntries(final List<ContentType> typesToSave,
                                                                     final List<List<String>> workflows) {

            final List<ContentTypeFormEntry> entries = new ArrayList<>();

            for (int i = 0; i < workflows.size(); i++) {
                final List<String> worflows = workflows.get(i);
                final ContentType contentType = typesToSave.get(i);

                final ContentTypeFormEntry entry = new ContentTypeFormEntry(contentType, worflows);
                entries.add(entry);
            }
            return entries;
        }

        private static List<List<String>> getWorkflowIdsFromJson(final String json) {
            final List<List<String>> workflows = new ArrayList<>();

            try {
                final JSONArray jarr = new JSONArray(json);

                for (int i = 0; i < jarr.size(); i++) {
                    final JSONObject fieldJsonObject = (JSONObject) jarr.get(i);
                    List<String> workflowsIds = getWorkflowsId(fieldJsonObject);
                    workflows.add(workflowsIds);
                }
            } catch (JSONException e) {

                try {
                    final JSONObject  fieldJsonObject = new JSONObject(json);
                    List<String> workflowsIds = getWorkflowsId(fieldJsonObject);
                    workflows.add(workflowsIds);
                } catch (JSONException e1) {
                    throw new DotRuntimeException(e1);
                }
            }
            return workflows;
        }

        private static List<String> getWorkflowsId(JSONObject fieldJsonObject) throws JSONException {
            List<String> worflowsArray = new ArrayList<>();

            if (fieldJsonObject.has(WORKFLOW_ATTRIBUTE_NAME)) {
                final JSONArray workflowsJaonArray = (JSONArray) fieldJsonObject.get(WORKFLOW_ATTRIBUTE_NAME);

                for (int k = 0; k < workflowsJaonArray.size(); k++) {
                    worflowsArray.add((String) workflowsJaonArray.get(k));
                }
            }

            return worflowsArray;
        }
    }

    public static class ContentTypeFormEntry {
        ContentType contentType;
        List<String> workflowsIds;

        ContentTypeFormEntry(final ContentType contentType, final List<String> workflowsIds) {
            this.contentType = contentType;
            this.workflowsIds = workflowsIds;
        }
    }
}
