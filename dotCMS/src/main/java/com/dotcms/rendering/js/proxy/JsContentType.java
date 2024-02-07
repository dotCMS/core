package com.dotcms.rendering.js.proxy;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.ReflectionUtils;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * This class is used to expose the ContentType object to the javascript engine.
 *
 * @author jsanca
 */
public class JsContentType implements Serializable, JsProxyObject<ContentType> {

    private final ContentType contentType;

    public JsContentType(final ContentType contentType) {
        this.contentType = contentType;
    }


    @Override
    public ContentType getWrappedObject() {
        return contentType;
    }

    @HostAccess.Export
    public  String id() {
        return contentType.id();
    }

    @HostAccess.Export
    public String name() {
        return contentType.name();
    }

    @HostAccess.Export
    public String inode() {
        return contentType.inode();
    }

    @HostAccess.Export
    public  String description() {
        return contentType.description();
    }

    @HostAccess.Export
    public boolean defaultType() {
        return contentType.defaultType();
    }

    @HostAccess.Export
    public String detailPage() {
        return contentType.detailPage();
    }

    @HostAccess.Export
    public boolean fixed() {
        return contentType.fixed();
    }

    @HostAccess.Export
    public Object iDate() {
        return JsProxyFactory.createProxy(contentType.iDate());
    }

    @HostAccess.Export
    public boolean system() {
        return contentType.system();
    }

    @HostAccess.Export
    public boolean versionable() {
        return contentType.versionable();
    }

    @HostAccess.Export
    public boolean multilingualable() {
        return contentType.multilingualable();
    }

    @HostAccess.Export
    public  String variable() {
        return contentType.variable();
    }

    @HostAccess.Export
    public String urlMapPattern() {
        return contentType.urlMapPattern();
    }

    @HostAccess.Export
    public String publishDateVar() {
        return contentType.publishDateVar();
    }

    @HostAccess.Export
    public String expireDateVar() {
        return contentType.expireDateVar();
    }

    @HostAccess.Export
    public String owner() {
        return contentType.owner();
    }

    @HostAccess.Export
    public Object modDate() {
        return JsProxyFactory.createProxy(contentType.modDate());
    }

    @HostAccess.Export
    public String host() {
        return contentType.host();
    }

    @HostAccess.Export
    public String siteName() {
        return contentType.siteName();
    }

    @HostAccess.Export
    public String icon() {
        return contentType.icon();
    }

    @HostAccess.Export
    public int sortOrder() {
        return contentType.sortOrder();
    }

    @HostAccess.Export
    public Object fields() {
        return JsProxyFactory.createProxy(contentType.fields());
    }

    @HostAccess.Export
    public Object fields(final String classname) {

        final Class<? extends Field> clazz = (Class<? extends Field>) ReflectionUtils.getClassFor(classname);
        return JsProxyFactory.createProxy(contentType.fields(clazz));
    }

    @HostAccess.Export
    public Object fieldMap() {
        return JsProxyFactory.createProxy(contentType.fieldMap());
    }

    @HostAccess.Export
    public String folder() {
        return contentType.folder();
    }

    @HostAccess.Export
    public String folderPath() {
        return contentType.folderPath();
    }

}
