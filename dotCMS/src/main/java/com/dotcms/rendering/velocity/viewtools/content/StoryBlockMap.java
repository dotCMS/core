package com.dotcms.rendering.velocity.viewtools.content;


import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rendering.velocity.viewtools.content.util.RenderableFactory;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Converts the json into a map and gets returned when it is requested like this $contentlet.storyBlock (var name of the field is storyBlock).
 * This will allow you to do:
 * $contentlet.storyBlock.type
 * $contentlet.storyBlock.content
 * $contentlet.storyBlock.toHtml
 */
public class StoryBlockMap implements Renderable {

    final static String DEFAULT_TEMPLATE_STOCK_BLOCK_PATH = "static/storyblock/";
    final static RenderableFactory renderableFactory = new RenderableFactory();

    private final String type;
    private final String render;
    private final String content;
    private final JSONObject jsonContFieldValue;

    public StoryBlockMap(final Field field,final Contentlet contentlet) throws JSONException {

        final com.dotcms.contenttype.model.field.Field fieldTransformed = new LegacyFieldTransformer(field).from();
        final Object contFieldValue = APILocator.getContentletAPI().getFieldValue(contentlet,fieldTransformed);
        this.jsonContFieldValue = new JSONObject(contFieldValue.toString());
        type = jsonContFieldValue.get("type").toString();
        render = jsonContFieldValue.get("render").toString();
        content = jsonContFieldValue.get("content").toString();
    }

    public StoryBlockMap(final Object contFieldValue) throws JSONException {

        this.jsonContFieldValue = new JSONObject(contFieldValue.toString());
        type = jsonContFieldValue.get("type").toString();
        render = jsonContFieldValue.get("render").toString();
        content = jsonContFieldValue.get("content").toString();
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public String toHtml() {

        final StringBuilder builder = new StringBuilder();

        try {

            final JSONArray items = this.jsonContFieldValue.getJSONArray("content");
            for (int i = 0; i < items.length(); ++i) {

                try {
                    // todo: do we have to catch this?
                    final JSONObject jsonObjectItem = items.getJSONObject(i);
                    final Renderable renderable = renderableFactory.create(jsonObjectItem, this.processType(jsonObjectItem));
                    builder.append(renderable.toHtml());
                } catch (JSONException | DotRuntimeException e) {
                    Logger.error(this, e.getMessage(), e);
                    this.addError (DEFAULT_TEMPLATE_STOCK_BLOCK_PATH, builder, e);
                }
            }
        } catch (JSONException e) {
            Logger.error(this, e.getMessage(), e);
            this.addError (DEFAULT_TEMPLATE_STOCK_BLOCK_PATH, builder, e);
        }

        return builder.toString();
    }

    @Override
    public String toHtml(final String baseTemplatePath) {

        final StringBuilder builder = new StringBuilder();

        try {
            final JSONArray items = this.jsonContFieldValue.getJSONArray("content");

            for (int i = 0; i < items.length(); ++i) {
                try {
                    final JSONObject jsonObjectItem = items.getJSONObject(i);
                    final Renderable renderable = renderableFactory.create(jsonObjectItem, this.processType(jsonObjectItem));
                    builder.append(renderable.toHtml(baseTemplatePath));
                } catch (JSONException | DotRuntimeException e) {
                    Logger.error(this, e.getMessage(), e);
                    this.addError (baseTemplatePath, builder, e);
                }
            }
        } catch (JSONException e) {
            Logger.error(this, e.getMessage(), e);
            this.addError (baseTemplatePath, builder, e);
        }

        return builder.toString();
    }

    private String processType(final JSONObject jsonObjectItem) throws JSONException {
        // heading is a special composite case, type + level
        final String type = jsonObjectItem.get("type").toString();

        return type + ("heading".equalsIgnoreCase(type)?
                jsonObjectItem.getJSONObject("attrs").get("level").toString(): StringPool.BLANK);
    }

    private void addError(final String path, final StringBuilder builder, final Exception e) {

        // todo: this could be a generic rendereable too, for errors
        final StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter, true));
        builder.append("<pre>")
                .append("<code>")
                .append("\tpath: ").append(path).append("\n")
                .append("\terror message: ").append(e.getMessage()).append("\n")
                .append("\terror stacktrace: ").append(stringWriter.toString()).append("\n")
                .append("</code>")
                .append("</pre>");
    }
}
