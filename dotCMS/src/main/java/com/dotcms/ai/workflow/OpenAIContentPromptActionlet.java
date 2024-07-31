package com.dotcms.ai.workflow;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotmarketing.portlets.workflows.actionlet.Actionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.*;
import com.google.common.collect.ImmutableList;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Actionlet(onlyBatch = true)
public class OpenAIContentPromptActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        WorkflowActionletParameter overwriteParameter = new MultiSelectionWorkflowActionletParameter(OpenAIParams.OVERWRITE_FIELDS.key,
                "Overwrite existing content (true|false)", Boolean.toString(true), true,
                () -> ImmutableList.of(
                        new MultiKeyValue(Boolean.toString(false), Boolean.toString(false)),
                        new MultiKeyValue(Boolean.toString(true), Boolean.toString(true)))
        );


        return List.of(
                new WorkflowActionletParameter(OpenAIParams.FIELD_TO_WRITE.key, "The field where you want to write the results.  " +
                        "<br>If your response is being returned as a json object, this field can be left blank" +
                        "<br>and the keys of the json object will be used to update the content fields.", "", false),
                overwriteParameter,
                new WorkflowActionletParameter(OpenAIParams.OPEN_AI_PROMPT.key, "The prompt that will be sent to the AI", "We need an attractive search result in Google. Return a json object that includes the fields \"pageTitle\" for a meta title of less than 55 characters and \"metaDescription\" for the meta description of less than 300 characters using this content:\\n\\n${fieldContent}\\n\\n", true),
                new WorkflowActionletParameter(OpenAIParams.RUN_DELAY.key, "Update the content asynchronously, after X seconds. O means run in-process", "5", true),
                new WorkflowActionletParameter(OpenAIParams.MODEL.key, "The AI model to use, defaults to " + ConfigService.INSTANCE.config().getConfig(AppKeys.TEXT_MODEL_NAMES), ConfigService.INSTANCE.config().getConfig(AppKeys.TEXT_MODEL_NAMES), false),
                new WorkflowActionletParameter(OpenAIParams.TEMPERATURE.key, "The AI temperature for the response.  Between .1 and 2.0.  Defaults to " + ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_TEMPERATURE), ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_TEMPERATURE), false)
        );
    }

    @Override
    public String getName() {
        return "AI Content Prompt";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will send the value of the 'openAIPrompt' field to AI and write the returned results to a field or fields of your choosing.  If the AI returns a JSON object, the key/values of that JSON will be merged with your content's fields.  The prompt can also take velocity (content can be referenced as $dotContentMap)";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
        int delay = Try.of(() -> Integer.parseInt(params.get(OpenAIParams.RUN_DELAY.key).getValue())).getOrElse(5);

        Runnable task = new AsyncWorkflowRunnerWrapper(new OpenAIContentPromptRunner(processor, params));
        if (delay > 0) {
            OpenAIThreadPool.schedule(task, delay , TimeUnit.SECONDS);
            final SystemMessageBuilder message = new SystemMessageBuilder().setMessage("Content being generated in the background")
                    .setLife(3000)
                    .setType(MessageType.SIMPLE_MESSAGE)
                    .setSeverity(MessageSeverity.SUCCESS);

            SystemMessageEventUtil.getInstance().pushMessage(message.create(), List.of(processor.getUser().getUserId()));
        } else {
            task.run();
        }


    }


}
