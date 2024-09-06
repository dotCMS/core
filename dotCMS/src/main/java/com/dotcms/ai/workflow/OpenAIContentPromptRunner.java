package com.dotcms.ai.workflow;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.util.VelocityContextFactory;
import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class is responsible for running workflows that generate content prompts using OpenAI. It
 * implements the AsyncWorkflowRunner interface and overrides its methods to provide the
 * functionality needed. This class is designed to handle long-running tasks in a separate thread
 * and needs to be serialized to a persistent storage.
 *
 * @author Daniel Silva
 * @since Mar 27th, 2024
 */
public class OpenAIContentPromptRunner implements Runnable {

    final User user;
    final String prompt;
    final boolean overwriteField;
    final String fieldToWrite;
    final String model;
    final float temperature;
    final Contentlet contentlet;

    OpenAIContentPromptRunner(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) {

        this(
                processor.getContentlet(),
                processor.getUser(),
                params.get(OpenAIParams.OPEN_AI_PROMPT.key).getValue(),
                Boolean.parseBoolean(params.get(OpenAIParams.OVERWRITE_FIELDS.key).getValue()),
                params.get(OpenAIParams.FIELD_TO_WRITE.key).getValue(),
                params.get(OpenAIParams.MODEL.key).getValue(),
                Try.of(() ->
                                Float.parseFloat(
                                        params
                                                .get(OpenAIParams.TEMPERATURE.key)
                                                .getValue()))
                        .getOrElse(ConfigService.INSTANCE.config().getConfigFloat(AppKeys.COMPLETION_TEMPERATURE))
        );
    }

    OpenAIContentPromptRunner(final Contentlet contentlet,
                              final User user,
                              final String prompt,
                              final boolean overwriteField,
                              final String fieldToWrite,
                              final String model,
                              final float temperature) {

        if (UtilMethods.isEmpty(contentlet::getIdentifier)) {
            throw new IllegalArgumentException(
                    "Content must be saved and have an identifier before running AI Content Prompt");
        }
        this.contentlet = contentlet;
        this.prompt = prompt;
        this.overwriteField = overwriteField;
        this.fieldToWrite = fieldToWrite;
        this.user = user;
        this.model = model;
        this.temperature = temperature;
    }

    @Override
    public void run() {

        if (UtilMethods.isEmpty(prompt)) {
            Logger.error(OpenAIContentPromptActionlet.class, String.format("No prompt was found for Contentlet ID " +
                    "'%s'", this.contentlet.getIdentifier()));
            // may be a toast saying that the prompt is empty for that contentlet
            return;
        }

        final Contentlet workingContentlet = this.contentlet;
        final ContentType type = workingContentlet.getContentType();

        Logger.info(this.getClass(), "Running OpenAI Modify Content for: " + workingContentlet.getTitle());

        final Optional<Field> fieldToTry = Try.of(() -> type.fieldMap().get(this.fieldToWrite)).toJavaOptional();

        try {
            final String response = openAIRequest(workingContentlet);
            if (fieldToTry.isPresent()) {
                setProperty(workingContentlet, fieldToTry.get().variable(), response);
            } else {
                final JSONObject tryJson = parseJsonResponse(response);
                setJsonProperties(workingContentlet, tryJson);
            }

            this.sendSuccessNotification();
        } catch (final Exception e) {

            handleError(e, user);
        }
    }

    private void handleError(final Exception e, final User user) {

        final String errorMsg = String.format("Error on generating content: %s", ExceptionUtil.getErrorMessage(e));
        SystemMessageEventUtil.getInstance().pushMessage(new SystemMessageBuilder()
                .setMessage(errorMsg)
                .setLife(5000)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setSeverity(MessageSeverity.ERROR).create(), List.of(user.getUserId()));
        Logger.error(this.getClass(), errorMsg, e);
        throw new DotRuntimeException(e);
    }

    private String openAIRequest(final Contentlet workingContentlet) throws Exception {

        final Context ctx = VelocityContextFactory.getMockContext(workingContentlet, user);
        final String parsedPrompt = VelocityUtil.eval(prompt, ctx);
        final JSONObject openAIResponse = APILocator.getDotAIAPI()
                .getCompletionsAPI()
                .raw(buildRequest(parsedPrompt, model, temperature), user.getUserId());

        try {
            return openAIResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (final Exception e) {

            Logger.error(this.getClass(), String.format("Unable to parse JSON for Contentlet ID " +
                    "'%s': %s", workingContentlet.getIdentifier(), ExceptionUtil.getErrorMessage(e)), e);
            throw new BadAIJsonFormatException(e.getMessage(),e);
        }
    }

    private void setProperty(final Contentlet contentlet, final String fieldVar, final String value) {
        if (value != null && contentlet.getContentType().fieldMap().get(fieldVar) != null) {

            if (overwriteField || UtilMethods.isEmpty(contentlet.getStringProperty(fieldVar))) {

                Logger.debug(this.getClass(), "setting field: " + fieldVar + " to " + value);
                final String cleanValue = cleanHTML(value);
                contentlet.setProperty(fieldVar, cleanValue);
            }
        }

    }

    private JSONObject parseJsonResponse(final String responseIn) throws BadAIJsonFormatException {

        final String response = responseIn.replaceAll("\\R+", StringPool.SPACE);
        final String finalResponse = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
        return Try.of(() -> new JSONObject(finalResponse))
                .onFailure(
                        e -> {
                            Logger.warn(this.getClass(), e.getMessage());
                            Logger.warn(this.getClass(), "initial response:\n" + responseIn);
                            Logger.warn(this.getClass(), "final response:\n" + responseIn);
                        })
                .getOrElseThrow(BadAIJsonFormatException::new);
    }

    private void setJsonProperties(final Contentlet contentlet, final JSONObject json) {

        Logger.info(this.getClass(), "Setting json:\n" + json.toString(2));
        for (final Map.Entry entry : (Set<Map.Entry>) json.getAsMap().entrySet()) {

            setProperty(contentlet, entry.getKey().toString(), (String) entry.getValue());
        }
    }

    private JSONObject buildRequest(final String prompt, final String model, final float temperature) {
        final JSONArray messages = new JSONArray();
        messages.add(Map.of("role", "user", "content", prompt));

        final JSONObject json = new JSONObject();
        json.put("messages", messages);
        json.put("model", model);
        json.put("temperature", temperature);
        json.put("stream", false);
        Logger.debug(this.getClass(), "Open AI Request:\n" + json.toString(2));

        return json;
    }

    private String cleanHTML(final String text) {

        if (ContentToStringUtil.impl.get().isHtml(text)) {
            return text
                    .replaceAll("\\s+", " ")
                    .replaceAll("\\>\\s+\\<", "><");
        }

        return text;
    }

    /**
     * Sends a notification to the dotCMS back-end indicating that the current Contentlet has been
     * updated with AI-generated content.
     */
    private void sendSuccessNotification() {

        final String msg = String.format("Content with ID '%s' has been updated by AI",
                this.contentlet.getIdentifier());
        SystemMessageEventUtil.getInstance().pushSimpleTextEvent(msg, this.user.getUserId());

        try {

            APILocator.getNotificationAPI().generateNotification(
                    new I18NMessage("OpenAI Content Prompt"),
                    new I18NMessage(msg),
                    null,
                    NotificationLevel.INFO,
                    NotificationType.GENERIC,
                    this.user.getUserId(),
                    this.user.getLocale()
            );

            APILocator.getSystemEventsAPI().push(SystemEventType.AI_CONTENT_PROMPT,
                    new Payload(contentlet.getIdentifier()));
        } catch (final DotDataException e) {
            Logger.warnAndDebug(this.getClass(), ExceptionUtil.getErrorMessage(e), e);
        }
    }

}
