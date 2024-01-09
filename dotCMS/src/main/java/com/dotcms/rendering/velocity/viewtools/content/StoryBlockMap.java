package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.rendering.velocity.viewtools.content.util.RenderableFactory;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.util.StringPool;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.velocity.context.Context;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

/**
 * Converts the json into a map and gets returned when it is requested like this $contentlet.storyBlock (var name of the field is storyBlock).
 * This will allow you to do:
 * $contentlet.storyBlock.type
 * $contentlet.storyBlock.content
 * $contentlet.storyBlock.toHtml
 */
public class StoryBlockMap implements Renderable, Serializable {

    final static String DEFAULT_TEMPLATE_STOCK_BLOCK_PATH = "static/storyblock/";
    final static RenderableFactory renderableFactory = new RenderableFactory();

    private final String type;
    private final String content;
    private final JSONObject jsonContFieldValue;
    private final String htmlContFieldValue;
    private final Context context;

    private static final String TYPE_KEY = "type";
    private static final String CONTENT_KEY = "content";
    private static final String HTML_CONTENT = "html";

    public StoryBlockMap(final Field field, final Contentlet contentlet, final Context context) throws JSONException {
        final com.dotcms.contenttype.model.field.Field fieldTransformed = new LegacyFieldTransformer(field).from();
        final Object contFieldValue = APILocator.getContentletAPI().getFieldValue(contentlet,fieldTransformed);
        if (JsonUtil.isValidJSON(contFieldValue.toString())) {
            this.jsonContFieldValue = new JSONObject(contFieldValue.toString());
            this.htmlContFieldValue = StringPool.BLANK;
            this.type = jsonContFieldValue.get(TYPE_KEY).toString();
            this.content = jsonContFieldValue.get(CONTENT_KEY).toString();
        } else {
            this.htmlContFieldValue = contFieldValue.toString();
            this.jsonContFieldValue = null;
            this.type = HTML_CONTENT;
            this.content = this.htmlContFieldValue;
        }
        this.context = context;
    }

    public StoryBlockMap(final Object contFieldValue) throws JSONException {

        if (null == contFieldValue || !UtilMethods.isSet(contFieldValue.toString())) {

            throw new JSONException("Invalid Json Value");
        }

        if (JsonUtil.isValidJSON(contFieldValue.toString())) {
            this.jsonContFieldValue = new JSONObject(contFieldValue.toString());
            this.htmlContFieldValue = StringPool.BLANK;
            this.type = jsonContFieldValue.get(TYPE_KEY).toString();
            this.content = jsonContFieldValue.get(CONTENT_KEY).toString();
        } else {
            this.htmlContFieldValue = contFieldValue.toString();
            this.jsonContFieldValue = null;
            this.type = HTML_CONTENT;
            this.content = this.htmlContFieldValue;
        }
        this.context = null;
    }

    /**
     * Returns the type of content that this Story Block field is holding. For example, {@code "doc"}.
     *
     * @return The Story Block type.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the raw content of this Story Block field. If it holds plain HTML code, it will return it s it is. If it
     * holds JSON data, it will return the value of the {@code content} attribute.
     *
     * @return The raw content.
     */
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
        if (UtilMethods.isSet(this.htmlContFieldValue)) {
            builder.append(this.htmlContFieldValue);
            return builder.toString();
        }
        try {
            final JSONArray items = this.jsonContFieldValue.getJSONArray("content");
            for (int i = 0; i < items.length(); ++i) {
                final JSONObject jsonObjectItem = items.getJSONObject(i);
                final Renderable renderable = renderableFactory.create(jsonObjectItem, this.processType(jsonObjectItem), this.context);
                builder.append(renderable.toHtml());
            }
        } catch (final JSONException e) {
            Logger.error(this, e.getMessage(), e);
            this.addError(DEFAULT_TEMPLATE_STOCK_BLOCK_PATH, builder, e);
        }

        return builder.toString();
    }

    @Override
    public String toHtml(final String baseTemplatePath) {
        final StringBuilder builder = new StringBuilder();
        if (UtilMethods.isSet(this.htmlContFieldValue)) {
            builder.append(this.htmlContFieldValue);
            return builder.toString();
        }
        try {
            final JSONArray items = this.jsonContFieldValue.getJSONArray("content");
            for (int i = 0; i < items.length(); ++i) {
                final JSONObject jsonObjectItem = items.getJSONObject(i);
                final Renderable renderable = renderableFactory.create(jsonObjectItem, this.processType(jsonObjectItem), this.context);
                builder.append(renderable.toHtml(baseTemplatePath));
            }
        } catch (final JSONException e) {
            Logger.error(this, e.getMessage(), e);
            this.addError(baseTemplatePath, builder, e);
        }

        return builder.toString();
    }

    /**
     * Returns the JSON object representation of this Story Block field.
     *
     * @return The {@link JSONObject} field value.
     */
    public JSONObject getJson() {
        return this.jsonContFieldValue;
    }

    private String processType(final JSONObject jsonObjectItem) throws JSONException {
        // heading is a special composite case, type + level
        final String type = jsonObjectItem.get("type").toString();
        return type + ("heading".equalsIgnoreCase(type)?
                jsonObjectItem.getJSONObject("attrs").get("level").toString(): StringPool.BLANK);
    }

    /**
     * Generates an error message when the Story Block Map fails to render its contents as plain HTML code.
     *
     * @param path    The Velocity Template used to render the contents of this Story Block Map.
     * @param builder The {@link StringBuilder} that will hold the error message.
     * @param e       The exception that caused the error.
     */
    private void addError(final String path, final StringBuilder builder, final Exception e) {
        // todo: this could be a generic rendereable too, for errors
        final StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter, true));
        builder.append("<pre>")
                .append("<code>")
                .append("\tpath: ").append(path).append("\n")
                .append("\terror message: ").append(e.getMessage()).append("\n")
                .append("\terror stacktrace: ").append(stringWriter).append("\n")
                .append("</code>")
                .append("</pre>");
    }

}
