package com.dotcms.rendering.js.proxy;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.viewtools.content.LazyLoaderContentMap;
import org.graalvm.polyglot.HostAccess;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class is used to expose the LazyLoaderContentMap object to the javascript engine.
 * @author jsanca
 */
public class JsLazyLoaderContentMap implements Serializable, JsProxyObject<LazyLoaderContentMap> {

    private final LazyLoaderContentMap lazyLoaderContentMap;

    public JsLazyLoaderContentMap(final LazyLoaderContentMap lazyLoaderContentMap) {
        this.lazyLoaderContentMap = lazyLoaderContentMap;
    }

    public LazyLoaderContentMap getLazyLoaderContentMap() {

        return lazyLoaderContentMap;
    }

    @Override
    public LazyLoaderContentMap  getWrappedObject() {
        return this.getLazyLoaderContentMap();
    }


    @HostAccess.Export
    public Object get(final String fieldVariableName) {
        return JsProxyFactory.createProxy(lazyLoaderContentMap.get(fieldVariableName));
    }

    @HostAccess.Export
    public Object getRaw(final String fieldVariableName) {
        return JsProxyFactory.createProxy(lazyLoaderContentMap.getRaw(fieldVariableName));
    }

    @HostAccess.Export
    public String getShortyUrl() throws IOException {
        return lazyLoaderContentMap.getShortyUrl();
    }

    @HostAccess.Export
    public String getShorty() throws IOException{
        return lazyLoaderContentMap.getShorty();
    }

    @HostAccess.Export
    public String getShortyInode() throws IOException{
        return lazyLoaderContentMap.getShortyInode();
    }

    @HostAccess.Export
    public String getShortyUrlInode() throws IOException{
        return lazyLoaderContentMap.getShortyUrlInode();
    }

    @HostAccess.Export
    public String getUrlMap(){

        return lazyLoaderContentMap.getUrlMap();
    }

    protected ContentType getContentTypeInternal() {

        return new StructureTransformer(lazyLoaderContentMap.getStructure()).from();
    }

    @HostAccess.Export
    public Object getContentType() {

        return JsProxyFactory.createProxy(this.getContentTypeInternal());
    }

    @HostAccess.Export
    public String getContentletsTitle() {

        return  lazyLoaderContentMap.getContentletsTitle();
    }

    @HostAccess.Export
    public boolean isLive() throws Exception {

        return lazyLoaderContentMap.isLive();
    }

    @HostAccess.Export
    public boolean isWorking() throws Exception {
        return lazyLoaderContentMap.isWorking();
    }

    @HostAccess.Export
    public String toString() {

        return lazyLoaderContentMap.toString();
    }

    @HostAccess.Export
    public boolean isHTMLPage() {

        return lazyLoaderContentMap.isHTMLPage();
    }


    @HostAccess.Export
    public Object getFieldVariables(String fieldVariableName) {
        return JsProxyFactory.createProxy(lazyLoaderContentMap.getFieldVariables(fieldVariableName));
    }

    @HostAccess.Export
    /**
     * Recovery the field variables as a json object
     * @param fieldVariableName String field var name
     * @return Map
     */
    public Object getFieldVariablesJson(final String fieldVariableName) {

        return JsProxyFactory.createProxy(lazyLoaderContentMap.getFieldVariablesJson(fieldVariableName));
    }
}
