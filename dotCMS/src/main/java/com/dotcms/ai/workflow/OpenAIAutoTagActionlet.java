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
public class OpenAIAutoTagActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        WorkflowActionletParameter overwriteParameter = new MultiSelectionWorkflowActionletParameter(OpenAIParams.OVERWRITE_FIELDS.key,
                "Overwrite  tags ", Boolean.toString(true), true,
                () -> ImmutableList.of(
                        new MultiKeyValue(Boolean.toString(false), Boolean.toString(false)),
                        new MultiKeyValue(Boolean.toString(true), Boolean.toString(true)))
        );

        WorkflowActionletParameter limitTagsToHost = new MultiSelectionWorkflowActionletParameter(
                OpenAIParams.LIMIT_TAGS_TO_HOST.key,
                "Limit the keywords to pre-existing tags", "Limit", false,
                () -> ImmutableList.of(
                        new MultiKeyValue(Boolean.toString(false), Boolean.toString(false)),
                        new MultiKeyValue(Boolean.toString(true), Boolean.toString(true))
                )
        );
        return List.of(
                overwriteParameter,
                limitTagsToHost,

                new WorkflowActionletParameter(OpenAIParams.RUN_DELAY.key, "Update the content asynchronously, after X seconds. O means run in-process", "5", true),
                new WorkflowActionletParameter(OpenAIParams.MODEL.key, "The AI model to use, defaults to " + ConfigService.INSTANCE.config().getConfig(AppKeys.MODEL_NAMES), ConfigService.INSTANCE.config().getConfig(AppKeys.MODEL_NAMES), false),
                new WorkflowActionletParameter(OpenAIParams.TEMPERATURE.key, "The AI temperature for the response.  Between .1 and 2.0.", ".1", false)
        );
    }

    @Override
    public String getName() {
        return "AI Auto-Tag Content";
    }

    @Override
    public String getHowTo() {
        return "This will attempt to Auto-tag your content item based on its values ";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
        int delay = Try.of(() -> Integer.parseInt(params.get(OpenAIParams.RUN_DELAY.key).getValue())).getOrElse(5);


        Runnable task = new AsyncWorkflowRunnerWrapper(new OpenAIAutoTagRunner(processor, params));
        if (delay > 0) {
            OpenAIThreadPool.schedule(task, delay, TimeUnit.SECONDS);

            final SystemMessageBuilder message = new SystemMessageBuilder().setMessage(
                            "Content being tagged in the background")
                    .setLife(5000)
                    .setType(MessageType.SIMPLE_MESSAGE)
                    .setSeverity(MessageSeverity.SUCCESS);

            SystemMessageEventUtil.getInstance()
                    .pushMessage(message.create(), List.of(processor.getUser().getUserId()));

        } else {
            task.run();
        }


    }


}
