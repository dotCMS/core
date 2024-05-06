package com.dotcms.rendering.js.proxy;

import com.dotcms.rendering.velocity.viewtools.content.Renderable;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap;
import com.dotmarketing.util.json.JSONObject;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;


/**
 * This class is used to expose the {@link com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap} object to the javascript engine.
 * @author jsanca
 */
public class JsStoryBlockMap implements Renderable, Serializable, JsProxyObject<StoryBlockMap> {

    private final StoryBlockMap storyBlockMap;

    public JsStoryBlockMap(final StoryBlockMap storyBlockMap) {
        this.storyBlockMap = storyBlockMap;
    }


    @Override
    public StoryBlockMap getWrappedObject() {
        return storyBlockMap;
    }

    @HostAccess.Export
    /**
     * Returns the type of content that this Story Block field is holding. For example, {@code "doc"}.
     *
     * @return The Story Block type.
     */
    public String getType() {
        return this.storyBlockMap.getType();
    }

    @HostAccess.Export
    /**
     * Returns the raw content of this Story Block field. If it holds plain HTML code, it will return it s it is. If it
     * holds JSON data, it will return the value of the {@code content} attribute.
     *
     * @return The raw content.
     */
    public String getContent() {
        return this.storyBlockMap.getContent();
    }

    @HostAccess.Export
    @Override
    public String toString() {
        return this.storyBlockMap.toString();
    }

    @HostAccess.Export
    @Override
    public String toHtml() {
        return this.storyBlockMap.toHtml();
    }

    @HostAccess.Export
    @Override
    public String toHtml(final String baseTemplatePath) {
        return this.storyBlockMap.toHtml(baseTemplatePath);
    }

    @HostAccess.Export
    /**
     * Returns the JSON object representation of this Story Block field.
     *
     * @return The {@link JSONObject} field value.
     */
    public Object getJson() {
        return JsProxyFactory.createProxy(this.getJsonInternal());
    }

    public JSONObject getJsonInternal() {
        return this.storyBlockMap.getJson();
    }
}
