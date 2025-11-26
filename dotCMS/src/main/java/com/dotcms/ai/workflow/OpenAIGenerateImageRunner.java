package com.dotcms.ai.workflow;

import com.dotcms.ai.api.ImageAPI;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.VelocityContextFactory;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The OpenAIGenerateImageRunner class is responsible for running workflows that generate images using OpenAI.
 * It implements the AsyncWorkflowRunner interface and overrides its methods to provide the functionality needed.
 * This class is designed to handle long-running tasks in a separate thread and needs to be serialized to a persistent storage.
 */
public class OpenAIGenerateImageRunner implements Runnable {

    private final User user;
    private final String prompt;
    private final boolean overwriteField;
    private final String fieldToWrite;
    private final Contentlet contentlet;


    public OpenAIGenerateImageRunner(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params) {
        this(
                processor.getContentlet(),
                processor.getUser(),
                params.get(OpenAIParams.OPEN_AI_PROMPT.key).getValue(),
                Try.of(() -> Boolean.parseBoolean(params.get(OpenAIParams.OVERWRITE_FIELDS.key).getValue()))
                        .getOrElse(false),
                params.get(OpenAIParams.FIELD_TO_WRITE.key).getValue()
        );
    }

    public OpenAIGenerateImageRunner(final Contentlet contentlet,
                              final User user,
                              final String prompt,
                              final boolean overwriteField,
                              final String fieldToWrite) {

        this.contentlet = contentlet;
        this.prompt = prompt;
        this.overwriteField = overwriteField;
        this.fieldToWrite = fieldToWrite;
        this.user = user;
    }

    @Override
    public void run() {

        final Contentlet workingContentlet = this.contentlet;
        final Host host = Try.of(
                        () -> APILocator.getHostAPI().find(workingContentlet.getHost(), APILocator.systemUser(), true))
                .getOrElse(APILocator.systemHost());

        final Optional<Field> fieldToTry = resolveField(workingContentlet);

        if (UtilMethods.isEmpty(prompt)) {
            Logger.info(OpenAIContentPromptActionlet.class, "no prompt found, returning");
            return;
        }

        Logger.info(this.getClass(), "Running OpenAI Generate Image Content for : " + workingContentlet.getTitle());

        if (fieldToTry.isEmpty()) {
            Logger.info(this.getClass(), "no binary field found, returning");
            return;
        }

        final Optional<Object> fieldVal = Try.of(
                        () -> APILocator.getContentletAPI().getFieldValue(workingContentlet, fieldToTry.get()))
                .toJavaOptional();
        if (fieldVal.isPresent() && UtilMethods.isSet(fieldVal.get()) && !overwriteField) {
            Logger.info(OpenAIContentPromptActionlet.class,
                    "field:" + fieldToTry.get().variable() + "  already set:" + fieldVal.get() + ", returning");
            return;
        }

        boolean setRequest = false;
        try {
            final Context ctx = VelocityContextFactory.getMockContext(workingContentlet, user);
            if (HttpServletRequestThreadLocal.INSTANCE.getRequest() == null) {
                setRequest = true;
                HttpServletRequestThreadLocal.INSTANCE.setRequest((HttpServletRequest) ctx.get("request"));
            }

            final String finalPrompt = VelocityUtil.eval(prompt, ctx);
            final ImageAPI imageAPI = APILocator.getDotAIAPI().getImageAPI(
                    ConfigService.INSTANCE.config(host),
                    user,
                    APILocator.getHostAPI(),
                    APILocator.getTempFileAPI());

            final JSONObject resp = Try.of(() -> imageAPI.sendTextPrompt(finalPrompt))
                    .onFailure(e -> Logger.warn(OpenAIGenerateImageRunner.class, "error generating image:" + e))
                    .getOrElse(JSONObject::new);

            final String tempFile = resp.optString("tempFile");
            if (UtilMethods.isEmpty(tempFile) && !new File(tempFile).exists()) {
                Logger.warn(
                        this.getClass(),
                        "Unable to generate image for contentlet: " + workingContentlet.getTitle());
                return;
            }

            contentlet.setProperty(fieldToTry.get().variable(), new File(tempFile));
        } catch (Exception e) {
            handleError(e, user);
        } finally{
            if (setRequest) {
                HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
            }
        }
    }

    /**
     * Handles any exceptions that occur during the execution of the workflow.
     * It logs the error, sends a system message to the user, and rethrows the exception as a DotRuntimeException.
     *
     * @param e the exception that occurred.
     * @param user the user who is running the workflow.
     * @throws DotRuntimeException if an exception occurs during the execution of the workflow.
     */
    private void handleError(final Exception e, final User user) {

        final String errorMsg = String.format("Error: %s", ExceptionUtil.getErrorMessage(e));
        final SystemMessageBuilder message = new SystemMessageBuilder()
                .setMessage(errorMsg)
                .setLife(5000)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setSeverity(MessageSeverity.ERROR);
        SystemMessageEventUtil.getInstance().pushMessage(message.create(), List.of(user.getUserId()));
        Logger.error(this.getClass(), errorMsg, e);
        throw new DotRuntimeException(e);
    }

    private Optional<Field> resolveField(final Contentlet contentlet) {

        final ContentType type = contentlet.getContentType();
        final Optional<Field> fieldToTry = Try.of(() -> type.fieldMap().get(this.fieldToWrite)).toJavaOptional();
        if (UtilMethods.isSet(this.fieldToWrite)) {
            return fieldToTry;
        }

        return type.fields(BinaryField.class).stream().findFirst();
    }

}
