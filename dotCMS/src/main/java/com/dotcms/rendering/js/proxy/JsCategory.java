package com.dotcms.rendering.js.proxy;

import com.dotmarketing.portlets.categories.model.Category;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import java.io.Serializable;
import java.util.Map;

/**
 * This class is used to expose the Category object to the javascript engine.
 * @author jsanca
 */
public class JsCategory implements Serializable, JsProxyObject<Category> {

    private final Category category;

    public JsCategory(final Category category) {
        this.category = category;
    }

    public Category getCategoryObject () {
        return category;
    }

    @Override
    public Category  getWrappedObject() {
        return this.getCategoryObject();
    }

    @HostAccess.Export
    /**
     * @return Returns the active.
     */
    public boolean isActive() {
        return this.category.isActive();
    }

    @HostAccess.Export
    public String getInode() {
        return this.category.getInode();
    }

    @HostAccess.Export
    public java.lang.String getCategoryName() {
        return this.category.getCategoryName();
    }

    @HostAccess.Export
    public int getSortOrder() {
        return this.category.getSortOrder();
    }

    @HostAccess.Export
    /**
     * Returns the description.
     * @return String
     */
    public String getDescription() {
        return this.category.getDescription();
    }

    @HostAccess.Export
    /**
     * @return Returns the key.
     */
    public String getKey() {
        return this.category.getKey();
    }

    @HostAccess.Export
    public String getKeywords() {
        return this.category.getKeywords();
    }

    @HostAccess.Export
    public boolean hasActiveChildren() {

        return this.category.hasActiveChildren();
    }

    @HostAccess.Export
    public ProxyHashMap getMap () {

        final Map map = this.category.getMap();
        return ProxyHashMap.from(map);
    }

    @HostAccess.Export
    public String getCategoryVelocityVarName() {
        return this.category.getCategoryVelocityVarName();
    }

    @HostAccess.Export
    public Object getModDate() {
        return JsProxyFactory.createProxy(this.category.getModDate());
    }

    @HostAccess.Export
    public boolean isParentPermissionable() {
        return this.category.isParentPermissionable();
    }

    @HostAccess.Export
    @Override
    public String toString() {
        return this.category.toString();
    }

}
