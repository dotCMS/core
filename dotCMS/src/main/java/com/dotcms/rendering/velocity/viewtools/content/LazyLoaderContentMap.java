package com.dotcms.rendering.velocity.viewtools.content;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.Lazy;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Supplier;

/**
 * This class is just a lazy wrapper of the content map used on the velocity context
 * the idea is to have this lazy supplier to be user on the container velocity context
 * so users can use the dotcontentmap on the container, but do not pay the price of requesting again to the system
 * the content information if they do not need it.
 * @author jsanca
 */
public class LazyLoaderContentMap implements Serializable {

    private final Lazy<ContentMap> contentMapSupplier;

    public LazyLoaderContentMap(final Supplier<ContentMap> contentMapSupplier) {
        this.contentMapSupplier = Lazy.of(contentMapSupplier);
    }

    protected ContentMap load () {

        return contentMapSupplier.get();
    }

    public Object get(String fieldVariableName) {
        return load().get(fieldVariableName);
    }

    public Object getRaw(String fieldVariableName) {
        return load().getRaw(fieldVariableName);
    }

    public String getShortyUrl() throws IOException{
        return load().getShortyUrl();
    }

    public String getShorty() throws IOException{
        return load().getShorty();
    }

    public String getShortyInode() throws IOException{
        return load().getShortyInode();
    }

    public String getShortyUrlInode() throws IOException{
        return load().getShortyUrlInode();
    }

    public String getUrlMap(){

        return load().getUrlMap();
    }

    public Structure getStructure() {

        return load().getStructure();
    }

    public String getContentletsTitle() {

        return  load().getContentletsTitle();
    }

    public boolean isLive() throws Exception {

        return load().isLive();
    }

    public boolean isWorking() throws Exception {
        return load().isWorking();
    }

    public String toString() {

        return load().toString();
    }

    public Boolean isHTMLPage() {

        return load().isHTMLPage();
    }

    public Contentlet getContentObject() {

        return load().getContentObject();
    }

    public Object getFieldVariables(String fieldVariableName) {
        return load().getFieldVariables(fieldVariableName);
    }

    /**
     * Recovery the field variables as a json object
     * @param fieldVariableName String field var name
     * @return Map
     */
    public Object getFieldVariablesJson(final String fieldVariableName) {

        return load().getFieldVariablesJson(fieldVariableName);
    }
}
