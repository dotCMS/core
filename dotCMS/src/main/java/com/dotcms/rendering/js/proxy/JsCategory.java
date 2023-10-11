package com.dotcms.rendering.js.proxy;

import com.dotcms.publishing.manifest.ManifestItem;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.model.Category;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyHashMap;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
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
    public Date getModDate() {
        return this.category.getModDate();
    }

    protected List<PermissionSummary> acceptedPermissionsInternal() {
        return this.category.acceptedPermissions();
    }
    //The following methods are part of the permissionable interface
    //to define what kind of permissions are accepted by categories
    //and also how categories should behave in terms of cascading
    @HostAccess.Export
    // todo: proxuy the PermissionSummary
    public Object acceptedPermissions() {
        return JsProxyFactory.createProxy(this.acceptedPermissionsInternal());
    }

    protected Permissionable getParentPermissionableInternal() throws DotDataException {
        return this.category.getParentPermissionable();
    }
    @HostAccess.Export
    // todo: proxy the Permissionable
    public Object getParentPermissionable() throws DotDataException {
        return JsProxyFactory.createProxy(this.getParentPermissionableInternal());
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

    protected ManifestItem.ManifestInfo getManifestInfoInternal() {

        return this.category.getManifestInfo();
    }

    @HostAccess.Export
    // todo: there is not proxy for the ManifestInfo yet
    public Object getManifestInfo() {

        return JsProxyFactory.createProxy(this.getManifestInfoInternal());
    }
}
