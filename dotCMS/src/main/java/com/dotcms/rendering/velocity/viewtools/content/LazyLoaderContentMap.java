package com.dotcms.rendering.velocity.viewtools.content;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import io.vavr.Lazy;

import java.io.IOException;
import java.util.function.Supplier;

public class LazyLoaderContentMap {

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
}
