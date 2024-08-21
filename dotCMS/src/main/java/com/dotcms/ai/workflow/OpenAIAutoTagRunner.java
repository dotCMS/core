package com.dotcms.ai.workflow;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.util.VelocityContextFactory;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The OpenAIAutoTagRunner class is responsible for running workflows that automatically tag content using OpenAI.
 * It implements the AsyncWorkflowRunner interface and overrides its methods to provide the functionality needed.
 * This class is designed to handle long-running tasks in a separate thread and needs to be serialized to a persistent storage.
 */
public class OpenAIAutoTagRunner implements AsyncWorkflowRunner {

    static final String DOT_AI_TAGGED="dot:autoTagged";
    static final String SYSTEM_PROMPT_TAGGING_FREEFORM = "Generate up to 25 SEO keywords that best match the user's text. Return your answer as RFC8259 compliant JSON that follows this format:\n" + "{\n" + "\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\"]\n" + "}";
    static final String SYSTEM_PROMPT_TAGGING_CONSTRAIN = "Given this list of keywords: \n\n\n$!{constrainTags}\n\n\nwhich of these keywords best describe the user's text?  Return your answer as RFC8259 compliant JSON that follows this format:\n" + "{\n" + "\"keywords\":[\"keyword1\", \"keyword2\", \"keyword3\"]\n" + "}";
    static final String SELECT_TOP_TAGS =
            "select distinct(tagname), count(tinode) from " +
                    "( " +
                    "   select lower(tagname), tag_inode.inode as tinode " +
                    "   from  " +
                    "   tag, tag_inode  " +
                    "   where  " +
                    "   tag.tag_id=tag_inode.tag_id  " +
                    "   and tag.host_id=? " +
                    "UNION ALL " +
                    "   select lower(tagname), tag_inode.inode as tinode " +
                    "   from  " +
                    "   tag, tag_inode  " +
                    "   where  " +
                    "   tag.tag_id=tag_inode.tag_id  " +
                    ") as foo  " +
                    "group by lower(tagname)  " +
                    "order by count(tinode) desc " +
                    "limit 1000";

    final String identifier;
    final long language;
    final User user;
    final boolean overwriteField;
    final boolean limitTags;
    final long runAt;
    final String model;
    final float temperature;

    OpenAIAutoTagRunner(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params) {
        this(
                processor.getContentlet(),
                processor.getUser(),
                Boolean.parseBoolean(params.get(OpenAIParams.OVERWRITE_FIELDS.key).getValue()),
                Boolean.parseBoolean(params.get(OpenAIParams.LIMIT_TAGS_TO_HOST.key).getValue()),
                Try.of(() -> Integer.parseInt(params.get(OpenAIParams.RUN_DELAY.key).getValue())).getOrElse(5),
                params.get(OpenAIParams.MODEL.key).getValue(),
                Try.of(() -> Float.parseFloat(params.get(OpenAIParams.TEMPERATURE.key).getValue())).getOrElse(ConfigService.INSTANCE.config().getConfigFloat(AppKeys.COMPLETION_TEMPERATURE))
        );
    }

    OpenAIAutoTagRunner(final Contentlet contentlet,
                        final User user,
                        final boolean overwriteField,
                        final boolean limitTags,
                        final int runDelay,
                        final String model,
                        final float temperature) {

        if (UtilMethods.isEmpty(contentlet::getIdentifier)) {
            throw new DotRuntimeException("Content must be saved and have an identifier before running AI Content Prompt");
        }
        this.identifier = contentlet.getIdentifier();
        this.language = contentlet.getLanguageId();
        this.overwriteField = overwriteField;
        this.limitTags = limitTags;
        this.user = user;
        this.runAt = System.currentTimeMillis() + runDelay;
        this.model = model;
        this.temperature = temperature;
    }

    @Override
    public long getRunAt() {
        return runAt;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public long getLanguage() {
        return this.language;
    }

    @Override
    public void runInternal() {
        final Contentlet workingContentlet = getLatest(identifier, language, user);
        if (UtilMethods.isEmpty(workingContentlet::getIdentifier)){
            return;
        }

        Logger.info(this.getClass(), "Running OpenAI Auto Tag Content for : " + workingContentlet.getTitle());

        final Optional<String> contentToTag = ContentToStringUtil.impl.get().turnContentletIntoString(workingContentlet);
        if (contentToTag.isEmpty()) {
            Logger.info(this.getClass(), "No content found to auto tag, returning :" + workingContentlet.getTitle());
            return;
        }

        final Optional<Field> fieldToTry = workingContentlet.getContentType().fields(TagField.class).stream().findFirst();
        if (fieldToTry.isEmpty()) {
            Logger.info(this.getClass(), "No tag field found to auto tag, returning :" + workingContentlet.getTitle());
            return;
        }

        boolean contentNeedsSaving;
        try {
            final String response = openAIRequest(workingContentlet, contentToTag.get());
            final List<String> tags = new ArrayList<>();
            final JSONArray tryArray = parseJsonResponse(response);

            if (this.limitTags) {
                List<String> constrainTags = findTopTags(workingContentlet);
                tryArray.removeIf(t -> !constrainTags.contains(t.toString().toLowerCase()));
            }

            if (tryArray.isEmpty()) {
                Logger.info(this.getClass(), "No keywords found, returning :" + workingContentlet.getTitle());
                return;
            }

            tryArray.add(DOT_AI_TAGGED);
            tryArray.forEach(t -> tags.add((String) t));

            final Contentlet contentToSave = checkoutLatest(identifier, language, user);
            contentNeedsSaving = setProperty(contentToSave, fieldToTry.get(), tags);

            if (!contentNeedsSaving) {
                Logger.warn(this.getClass(), "Nothing to save for OpenAI response: " + response);
                return;
            }

            saveContentlet(contentToSave, user);
        } catch (Exception e) {
            handleError(e, user);
        }
    }

    private String openAIRequest(final Contentlet workingContentlet, final String contentToTag) throws Exception {
        final Context ctx = VelocityContextFactory.getMockContext(workingContentlet, user);
        final String systemPrompt = this.limitTags ? SYSTEM_PROMPT_TAGGING_CONSTRAIN : SYSTEM_PROMPT_TAGGING_FREEFORM;
        final List<String> constrainTags = this.limitTags ? findTopTags(workingContentlet) : List.of();
        ctx.put("constrainTags", String.join("\n", constrainTags));

        final String parsedSystemPrompt = VelocityUtil.eval(systemPrompt, ctx);
        final String parsedContentPrompt = VelocityUtil.eval(contentToTag, ctx);


        final JSONObject openAIResponse = CompletionsAPI
                .impl()
                .prompt(parsedSystemPrompt, parsedContentPrompt, model, temperature, 2000);

        return openAIResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    private JSONArray parseJsonResponse(final String responseIn) {
        Logger.debug(this.getClass(),"---- response ----- \n" + responseIn + "\n/---- response -----");
        final String response = responseIn.replaceAll("\\R+", " ");
        final String finalResponse = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
        return Try.of(() -> new JSONObject(finalResponse).getJSONArray("keywords")).getOrElseThrow(BadAIJsonFormatException::new);
    }

    private boolean setProperty(final Contentlet contentlet, final Field field, final List<String> tagList) {
        final String existingTags = contentlet.getStringProperty(field.variable());

        if (overwriteField || UtilMethods.isEmpty(existingTags)) {
            final String tags = String.join(",", tagList);
            Logger.debug(this.getClass(), "setting field:" + field.variable() + " to " + tags);
            contentlet.setProperty(field.variable(), tags);
            return true;
        } else if(!existingTags.contains(DOT_AI_TAGGED)){
            final String tags = String.join(",", tagList) + ", " + existingTags;
            contentlet.setProperty(field.variable(), tags);
            return true;
        } else if(existingTags.contains(DOT_AI_TAGGED)){
            Logger.info(this.getClass(), "Already autotagged, skipping");
            return false;
        }

        Logger.info(this.getClass(), "field :" + field.variable() + " already set, skipping");
        return false;
    }

    @CloseDBIfOpened
    private List<String> findTopTags(final Contentlet contentlet) {
        try (final Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            final List<Map<String, Object>> results = new DotConnect()
                    .setSQL(SELECT_TOP_TAGS)
                    .addParam(contentlet.getHost())
                    .loadObjectResults(conn);
            return results.stream().map(r -> r.get("tagname").toString()).collect(Collectors.toList());
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

}