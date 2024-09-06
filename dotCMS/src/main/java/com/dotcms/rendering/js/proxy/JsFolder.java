package com.dotcms.rendering.js.proxy;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates a {@link Folder} in a Js context.
 * @author jsanca
 */
public class JsFolder implements Serializable, JsProxyObject<Folder> {

    private final Folder folder;

    public JsFolder(final Folder folder) {
        this.folder = folder;
    }

    @Override
    public Folder getWrappedObject() {
        return this.folder;
    }

    @HostAccess.Export
    public String getOwner() {
        return this.folder.getOwner();
    }

    @HostAccess.Export
    public Object getIDate() {
        return JsProxyFactory.createProxy(this.folder.getIDate());
    }

    @HostAccess.Export
    public String getIdentifier() {
        return this.folder.getIdentifier();
    }

    @HostAccess.Export
    /**
     * Returns the type.
     *
     * @return String
     */
    public String getType() {
        return this.folder.getType();
    }

    @HostAccess.Export
    public Object getHost() {
        return JsProxyFactory.createProxy(this.folder.getHost());
    }

    @HostAccess.Export
    public boolean isSystemFolder() {
        return this.folder.isSystemFolder();
    }

    @HostAccess.Export
    /**
     * Returns the inode.
     * @return String
     */
    public String getInode() {
        return this.folder.getInode();
    }

    @HostAccess.Export
    /**
     * Returns the name.
     * @return String
     */
    public String getName() {
        return this.folder.getName();
    }

    @HostAccess.Export
    public boolean isParent() {
        return this.folder.isParent();
    }

    @HostAccess.Export
    public Object getChildren(final User user, final boolean live,
                                      final boolean working,
                                      final boolean archived,
                                      final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

        return JsProxyFactory.createProxy(this.folder.getChildren(user, live, working, archived, respectFrontEndPermissions));
    }

    @HostAccess.Export
    /**
     * Returns the sortOrder.
     * @return int
     */
    public int getSortOrder() {
        return this.folder.getSortOrder();
    }

    @HostAccess.Export
    /**
     * Returns the showOnMenu.
     * @return boolean
     */
    public boolean isShowOnMenu() {
        return this.folder.isShowOnMenu();
    }

    @HostAccess.Export
    /**
     * Returns the title.
     * @return String
     */
    public String getTitle() {
        return this.folder.getTitle();
    }

    @HostAccess.Export
    /**
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.folder.toString();
    }

    @HostAccess.Export
    /**
     * @return Returns the hostId.
     */
    public String getHostId() {
        return this.folder.getHostId();
    }

    @HostAccess.Export
    public String getFilesMasks() {
        return this.folder.getFilesMasks();
    }

    @HostAccess.Export
    public String getDefaultFileType() {
        return this.folder.getDefaultFileType();
    }

    @HostAccess.Export
    public Object getModDate() {
        return JsProxyFactory.createProxy(this.folder.getModDate());
    }

    @HostAccess.Export
    public Object getMap() throws DotStateException, DotDataException, DotSecurityException {

        return JsProxyFactory.createProxy(this.folder.getMap());
    }

    @HostAccess.Export
    public String getPermissionId() {
        return this.folder.getPermissionId();
    }

    @HostAccess.Export
    public boolean isParentPermissionable() {
        return this.folder.isParentPermissionable();
    }


    @HostAccess.Export
    public String getPermissionType() {
        return this.folder.getPermissionType();
    }

    @HostAccess.Export
    public String getPath() {
        return this.folder.getPath();
    }

}
