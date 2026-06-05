package com.dotcms.ai.workflow;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.util.VelocityContextFactory;
import com.dotmarketing.beans.Host;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The OpenAIAutoTagRunner class is responsible for running workflows that automatically tag content using OpenAI.
 * It implements the AsyncWorkflowRunner interface and overrides its methods to provide the functionality needed.
 * This class is designed to handle long-running tasks in a separate thread and needs to be serialized to a persistent storage.
 */
public class OpenAIAutoTagRunner implements Runnable {

    static final String DOT_AI_TAGGED="dot:autoTagged";
    static final String SYSTEM_PROMPT_TAGGING_FREEFORM = "Generate up to 25 SEO keywords that best match the user's text. Return your answer as RFC8259 compliant JSON that follows this format:\n" + "{\n" + "\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\"]\n" + "}";
    static final String SYSTEM_PROMPT_TAGGING_CONSTRAIN = "Given this list of keywords: \n\n\n$!{constrainTags}\n\n\nwhich of these keywords best describe the user's text?  Return your answer as RFC8259 compliant JSON that follows this format:\n" + "{\n" + "\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\"]\n" + "}";

    final User user;
    final boolean overwriteField;
    final boolean limitTags;
    final Contentlet contentlet;

    OpenAIAutoTagRunner(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params) {
        this(
                processor.getContentlet(),
                processor.getUser(),
                Boolean.parseBoolean(params.get(OpenAIParams.OVERWRITE_FIELDS.key).getValue()),
                Boolean.parseBoolean(params.get(OpenAIParams.LIMIT_TAGS_TO_HOST.key).getValue())
        );
    }

    OpenAIAutoTagRunner(final Contentlet contentlet,
                        final User user,
                        final boolean overwriteField,
                        final boolean limitTags) {

        if (UtilMethods.isEmpty(contentlet::getIdentifier)) {
            throw new IllegalArgumentException("Content must be saved and have an identifier before running AI Content Prompt");
        }
        this.contentlet = contentlet;
        this.overwriteField = overwriteField;
        this.limitTags = limitTags;
        this.user = user;
    }


    @Override
    public void run() {

        final Contentlet workingContentlet = this.contentlet;

        Logger.debug(this.getClass(), "Running OpenAI Auto Tag Content for : " + workingContentlet.getTitle());

        final Optional<String> contentToTag = ContentToStringUtil.impl.get().turnContentletIntoString(workingContentlet);
        if (contentToTag.isEmpty()) {
            Logger.debug(this.getClass(), "No content found to auto tag, returning :" + workingContentlet.getTitle());
            return;
        }

        // this may be some way configurable
        final Optional<Field> fieldToTry = workingContentlet.getContentType().fields(TagField.class).stream().findFirst();
        if (fieldToTry.isEmpty()) {
            Logger.debug(this.getClass(), "No tag field found to auto tag, returning :" + workingContentlet.getTitle());
            return;
        }

        try {

            final String response = openAIRequest(workingContentlet, contentToTag.get());
            final List<String> tags = new ArrayList<>();
            // we should use Map instead of this object
            final JSONArray tryArray = parseJsonResponse(response);

            if (this.limitTags) {
                final Set<String> constrainTags = APILocator.getTagAPI().findTopTags(workingContentlet.getHost());
                tryArray.removeIf(t -> !constrainTags.contains(t.toString().toLowerCase()));
            }

            if (tryArray.isEmpty()) {
                Logger.info(this.getClass(), "No keywords found, returning :" + workingContentlet.getTitle());
                return;
            }

            tryArray.add(DOT_AI_TAGGED);
            tryArray.forEach(t -> tags.add((String) t));

            contentlet.setProperty(fieldToTry.get().variable(), tags);
        } catch (Exception e) {
            this.handleError(e, user);
        }
    }

    private void handleError(final Exception e, final User user) {
        final String errorMsg = String.format("Error on generating tags: %s", ExceptionUtil.getErrorMessage(e));
        SystemMessageEventUtil.getInstance().pushMessage(new SystemMessageBuilder()
                .setMessage(errorMsg)
                .setLife(5000)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setSeverity(MessageSeverity.ERROR).create(), List.of(user.getUserId()));
        Logger.error(this.getClass(), errorMsg, e);
        throw new DotRuntimeException(e);
    }

    private String openAIRequest(final Contentlet workingContentlet, final String contentToTag) throws Exception {
        final Context ctx = VelocityContextFactory.getMockContext(workingContentlet, user);
        final String systemPrompt = this.limitTags ? SYSTEM_PROMPT_TAGGING_CONSTRAIN : SYSTEM_PROMPT_TAGGING_FREEFORM;
        final Set<String> constrainTags = this.limitTags ? APILocator.getTagAPI().findTopTags(workingContentlet.getHost()) : Set.of();
        ctx.put("constrainTags", String.join("\n", constrainTags));

        final String parsedSystemPrompt = VelocityUtil.eval(systemPrompt, ctx);
        final String parsedContentPrompt = VelocityUtil.eval(contentToTag, ctx);

        final Host host = Try.of(() -> APILocator.getHostAPI().find(workingContentlet.getHost(), user, false)).getOrNull();
        final AppConfig appConfig = ConfigService.INSTANCE.config(host);
        final JSONObject openAIResponse = APILocator.getDotAIAPI().getCompletionsAPI(appConfig)
                .prompt(parsedSystemPrompt, parsedContentPrompt, null, 0f, 2000, user.getUserId());

        return openAIResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    private JSONArray parseJsonResponse(final String responseIn) {
        Logger.debug(this.getClass(),"---- response ----- \n" + responseIn + "\n/---- response -----");
        final String response = responseIn.replaceAll("\\R+", " ");
        final String finalResponse = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
        return Try.of(() -> new JSONObject(finalResponse).getJSONArray("keywords")).getOrElseThrow(BadAIJsonFormatException::new);
    }

}
