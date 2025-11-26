package com.dotcms.ai.workflow;


import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.MultiKeyValue;
import com.dotmarketing.portlets.workflows.model.MultiSelectionWorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DotEmbeddingsActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;


    @Override
    public List<WorkflowActionletParameter> getParameters() {
        WorkflowActionletParameter deleteOrInsert = new MultiSelectionWorkflowActionletParameter(
                OpenAIParams.DOT_EMBEDDING_ACTION.key,
                "Delete or insert the embeddings", "INSERT", true,
                () -> ImmutableList.of(
                        new MultiKeyValue("INSERT", "INSERT"),
                        new MultiKeyValue("DELETE", "DELETE"))
        );

        final List<WorkflowActionletParameter> params = new ArrayList<>();
        params.add(new WorkflowActionletParameter(OpenAIParams.DOT_EMBEDDING_TYPES_FIELDS.key,
                "List of {contentType}.{fieldVar} to use to generate the embeddings.  " +
                        "Each type.field should be on its own line, e.g. blog.title<br>blog.blogContent", "", false));
        params.add(new WorkflowActionletParameter(OpenAIParams.DOT_EMBEDDING_INDEX.key, "Index Name", "", false));
        params.add(deleteOrInsert);

        return params;
    }

    @Override
    public String getName() {
        return "AI - Embed Content";
    }

    @Override
    public String getHowTo() {
        return "This Actionlet will generate and save the the OpenAI 'embeddings' for a piece of content.  You can specify the index in which to store the embeddings and the list content types and fields you want to include in the index. "
                +
                "Each {type.field} should be on its own line, e.g. <br>blog.title<br>blog.blogContent<br>.  " +
                "If no field is specified, dotCMS will attempt to guess how the content " +
                "should be indexed based on its content type and/or its fields. Example usage is to add this Actionlet to the publish action, so a piece of content is added when published and also add it to the unpublish action, specifing 'DELETE', which will remove it from the index";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final ContentType type = processor.getContentlet().getContentType();

        final String updateOrDelete =
                "DELETE".equalsIgnoreCase(params.get(OpenAIParams.DOT_EMBEDDING_ACTION.key).getValue()) ? "DELETE"
                        : "INSERT";
        final Host host = Try.of(
                () -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false)).getOrNull();
        final String indexName =
                UtilMethods.isSet(params.get(OpenAIParams.DOT_EMBEDDING_INDEX.key).getValue()) ? params.get(
                        OpenAIParams.DOT_EMBEDDING_INDEX.key).getValue() : "default";

        final Map<String, List<Field>> typesAndfields = APILocator.getDotAIAPI().getEmbeddingsAPI().parseTypesAndFields(
                params.get(OpenAIParams.DOT_EMBEDDING_TYPES_FIELDS.key).getValue());

        List<Field> fields = typesAndfields.getOrDefault(type.variable(), List.of());

        APILocator.getDotAIAPI().getEmbeddingsAPI().generateEmbeddingsForContent(contentlet, fields, indexName);

    }





}
