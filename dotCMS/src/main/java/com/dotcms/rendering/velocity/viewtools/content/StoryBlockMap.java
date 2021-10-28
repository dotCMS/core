package com.dotcms.rendering.velocity.viewtools.content;


import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.liferay.util.StringPool;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Converts the json into a map and gets returned when it is requested like this $contentlet.storyBlock (var name of the field is storyBlock).
 * This will allow you to do:
 * $contentlet.storyBlock.type
 * $contentlet.storyBlock.render
 * $contentlet.storyBlock.content
 */

public class StoryBlockMap {

    private String type = StringPool.BLANK;
    private String render = StringPool.BLANK;
    private String content = StringPool.BLANK;

    public StoryBlockMap(final Field field,final Contentlet contentlet) throws JSONException {
        final com.dotcms.contenttype.model.field.Field fieldTransformed = new LegacyFieldTransformer(field).from();
        final Object contFieldValue = APILocator.getContentletAPI().getFieldValue(contentlet,fieldTransformed);
        final JSONObject jsonContFieldValue = new JSONObject(contFieldValue.toString());
        type = jsonContFieldValue.get("type").toString();
        render = jsonContFieldValue.get("render").toString();
        content = jsonContFieldValue.get("content").toString();
    }

    public String getType() {
        return type;
    }

    public String getRender() {
        return render;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
