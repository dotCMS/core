package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.contenttype.ContentTypeForm.ContentTypeFormDeserialize;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.json.JSONTokener;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@link ContentTypeResource}'s form
 */
@JsonDeserialize(using = ContentTypeFormDeserialize.class)
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

    public List<WorkflowFormEntry> getWorkflows() {
        return entries.get(0).workflows;
    }

    public List<Tuple2<SystemAction, String>> getSystemActions() {
        return entries.get(0).systemActions;
    }

    public Object getRequestJson() {
        return requestJson;
    }


    public static final class ContentTypeFormDeserialize extends JsonDeserializer<ContentTypeForm> {

        public static final String WORKFLOW_ATTRIBUTE_NAME = "workflow";
        public static final String SYSTEM_ACTION_ATTRIBUTE_NAME = "systemActionMappings";

        private static final String WORKFLOW_SCHEME_ID_ATTRIBUTE = "id";
        private static final String WORKFLOW_SCHEME_VARIABLE_NAME_ATTRIBUTE = "variableName";

        @Override
        public ContentTypeForm deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {

            final String json = jsonParser.readValueAsTree().toString();
            return buildForm(json);
        }

        @VisibleForTesting
        public ContentTypeForm buildForm(final String json) {

            final List<ContentType> typesToSave = new JsonContentTypeTransformer(json).asList();
            final List<List<WorkflowFormEntry>> workflows = getWorkflowsFromJson(json);
            final List<List<Tuple2<SystemAction, String>>> systemActionWorkflowActionIds =
                    systemActionWorkflowActionIdMapFromJson(json);

            final List<ContentTypeFormEntry> entries = getContentTypeFormEntries(typesToSave, workflows, systemActionWorkflowActionIds);

            return new ContentTypeForm(entries, json);
        }

        private List<List<Tuple2<SystemAction, String>>> systemActionWorkflowActionIdMapFromJson(
                final String json) {

            final List<List<Tuple2<SystemAction, String>>> systemActionWorkflowActionIdMapList = new ArrayList<>();

            try {

                for (Object jsonObject : new JSONArray(json)) {

                    final JSONObject fieldJsonObject = (JSONObject) jsonObject;
                    systemActionWorkflowActionIdMapList.add(
                            getSystemActionsWorkflowActionIds(fieldJsonObject)
                    );
                }
            } catch (JSONException e) {

                try {
                    final JSONObject fieldJsonObject = new JSONObject(json);
                    systemActionWorkflowActionIdMapList.add(
                            getSystemActionsWorkflowActionIds(fieldJsonObject)
                    );
                } catch (JSONException e1) {
                    throw new DotRuntimeException(e1);
                }
            }

            return systemActionWorkflowActionIdMapList;
        }

        private List<ContentTypeFormEntry> getContentTypeFormEntries(
                final List<ContentType> typesToSave,
                final List<List<WorkflowFormEntry>> workflows,
                final List<List<Tuple2<SystemAction, String>>> systemActionWorkflowActionIds) {

            final List<ContentTypeFormEntry> entries = new ArrayList<>();

            for (int i = 0; i < workflows.size(); i++) {
                final List<WorkflowFormEntry> contentTypeWorkflows = workflows.get(i);
                final ContentType contentType = typesToSave.get(i);
                final var systemActionWorkflowActions = systemActionWorkflowActionIds.get(i);

                final ContentTypeFormEntry entry = new ContentTypeFormEntry(
                        contentType, contentTypeWorkflows, systemActionWorkflowActions
                );
                entries.add(entry);
            }
            return entries;
        }

        /**
         * Extracts the workflows from the json object found in the form
         *
         * @param json the json object found in the form
         * @return a list of workflow entries
         */
        private static List<List<WorkflowFormEntry>> getWorkflowsFromJson(final String json) {

            final List<List<WorkflowFormEntry>> workflows = new ArrayList<>();

            final Object jsonStructure;
            try {
                jsonStructure = new JSONTokener(json).nextValue();
            } catch (JSONException e) {
                throw new DotRuntimeException(e);
            }

            if (jsonStructure instanceof JSONArray) {

                final JSONArray jsonArray = (JSONArray) jsonStructure;
                for (int i = 0; i < jsonArray.size(); i++) {

                    final JSONObject jsonObject = jsonArray.getJSONObject(i);

                    List<WorkflowFormEntry> foundWorkflows = getWorkflows(jsonObject);
                    workflows.add(foundWorkflows);
                }

            } else if (jsonStructure instanceof JSONObject) {

                final JSONObject jsonObject = (JSONObject) jsonStructure;

                List<WorkflowFormEntry> foundWorkflows = getWorkflows(jsonObject);
                workflows.add(foundWorkflows);
            } else {
                throw new DotRuntimeException("Invalid JSON structure");
            }

            return workflows;
        }

        /**
         * Extracts the workflows from the json object found in the form
         *
         * @param fieldJsonObject the json object found in the form
         * @return a list of workflow entries
         * @throws JSONException if the json object is not well-formed
         */
        private static List<WorkflowFormEntry> getWorkflows(JSONObject fieldJsonObject)
                throws JSONException {

            List<WorkflowFormEntry> workflowsArray = new ArrayList<>();

            if (fieldJsonObject.has(WORKFLOW_ATTRIBUTE_NAME)) {

                final JSONArray workflowsJSONArray = (JSONArray) fieldJsonObject.get(
                        WORKFLOW_ATTRIBUTE_NAME);

                for (Object entry : workflowsJSONArray) {

                    Logger.debug(
                            ContentTypeFormDeserialize.class,
                            String.format("Content Type Workflow form entry [%s]", entry.toString())
                    );

                    final WorkflowFormEntry workflowFormEntry;

                    if (entry instanceof String) { // Entry is a workflow id

                        workflowFormEntry = WorkflowFormEntry.builder().
                                id((String) entry).
                                build();
                    } else {

                        final JSONObject workflowJsonObject = (JSONObject) entry;
                        workflowFormEntry = mapToWorkflowFormEntry(workflowJsonObject);
                    }

                    workflowsArray.add(workflowFormEntry);
                }
            }

            return workflowsArray;
        }

        /**
         * Maps a json object to a {@link WorkflowFormEntry}
         *
         * @param workflowJsonObject the json object to map
         * @return a {@link WorkflowFormEntry}
         * @throws JSONException if the json object is not well-formed
         */
        private static WorkflowFormEntry mapToWorkflowFormEntry(final JSONObject workflowJsonObject)
                throws JSONException {

            String workflowId = null;
            if (workflowJsonObject.has(WORKFLOW_SCHEME_ID_ATTRIBUTE) &&
                    UtilMethods.isSet(workflowJsonObject.getString(WORKFLOW_SCHEME_ID_ATTRIBUTE))) {
                workflowId = workflowJsonObject.getString(WORKFLOW_SCHEME_ID_ATTRIBUTE);
            }

            String variableName = null;
            if (workflowJsonObject.has(WORKFLOW_SCHEME_VARIABLE_NAME_ATTRIBUTE) &&
                    UtilMethods.isSet(workflowJsonObject.getString(WORKFLOW_SCHEME_VARIABLE_NAME_ATTRIBUTE))) {
                variableName = workflowJsonObject.getString(WORKFLOW_SCHEME_VARIABLE_NAME_ATTRIBUTE);
            }

            return WorkflowFormEntry.builder().
                    id(workflowId).
                    variableName(variableName).
                    build();
        }

        private static List<Tuple2<SystemAction, String>> getSystemActionsWorkflowActionIds(
                final JSONObject fieldJsonObject) throws JSONException {

            List<Tuple2<SystemAction, String>> tuple2List = null;

            if (fieldJsonObject.has(SYSTEM_ACTION_ATTRIBUTE_NAME)) {

                SystemAction systemAction;
                String workflowActionId;

                tuple2List = new ArrayList<>();

                final JSONObject systemActionWorkflowActionIdJSONObject = (JSONObject) fieldJsonObject.get(SYSTEM_ACTION_ATTRIBUTE_NAME);
                final Iterator keys = systemActionWorkflowActionIdJSONObject.keys();

                while(keys.hasNext())  {

                    final String systemActionName = keys.next().toString();
                    systemAction = SystemAction.fromString(systemActionName);
                    workflowActionId = systemActionWorkflowActionIdJSONObject.getString(systemActionName);
                    tuple2List.add(Tuple.of(systemAction, workflowActionId));
                }
            }

            return tuple2List;
        }

    }

    public static class ContentTypeFormEntry {
        ContentType  contentType;
        List<WorkflowFormEntry> workflows;
        List<Tuple2<SystemAction, String>> systemActions;

        ContentTypeFormEntry(final ContentType contentType, final List<WorkflowFormEntry> workflows,
                final List<Tuple2<SystemAction, String>> systemActions) {

            this.systemActions = systemActions;
            this.contentType   = contentType;
            this.workflows = workflows;
        }
    }
}
