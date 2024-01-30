package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsViewContextAware;
import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsProxyFactory;
import com.dotcms.rendering.velocity.viewtools.ContainerWebAPI;
import com.dotmarketing.exception.DotDataException;
import org.apache.velocity.tools.view.context.ViewContext;
import org.graalvm.polyglot.HostAccess;

/**
 * Wraps the {@link com.dotcms.rendering.velocity.viewtools.ContainerWebAPI} (tags) into the JS context.
 * @author jsanca
 */
public class ContainerJsViewTool implements JsViewTool, JsViewContextAware {

    private final ContainerWebAPI containerWebAPI = new ContainerWebAPI();
    @Override
    public String getName() {
        return "containerAPI";
    }

    @Override
    public void setViewContext(final ViewContext viewContext) {
        this.containerWebAPI.init(viewContext);
    }

    @HostAccess.Export
    public String getStructureCode(final String containerIdentifier, final String contentTypeId) throws Exception {

        return this.containerWebAPI.getStructureCode(containerIdentifier, contentTypeId);
    }

    @HostAccess.Export
    /**
     * This method returns the personalized list of content ids that match
     * the persona of the visitor
     * @param pageId
     * @param containerId
     * @param uuid
     * @return
     */
    public Object getPersonalizedContentList(final String pageId, final String containerId, final String uuid) {

        return JsProxyFactory.createProxy(this.containerWebAPI.getPersonalizedContentList(pageId, containerId, uuid));
    }

    @HostAccess.Export
    /**
     * This method checks if the logged in user (frontend) has the required permission over
     * the passed container id
     */
    public boolean doesUserHasPermission (final String containerInode, final int permission,
            final boolean respectFrontendRoles) throws DotDataException {

        return this.containerWebAPI.doesUserHasPermission(containerInode, permission, respectFrontendRoles);
    }

    @HostAccess.Export
    /**
     * This method checks if the logged in user has the required permission to ADD any content into the
     * the passed container id
     */
    public boolean doesUserHasPermissionToAddContent (final String containerInode) throws DotDataException {

       return this.containerWebAPI.doesUserHasPermissionToAddContent(containerInode);
    }

    @HostAccess.Export
    /**
     * This method checks if the logged in user has the required permission to ADD any widget into the
     * the passed container id
     */
    public boolean doesUserHasPermissionToAddWidget (final String containerInode) throws DotDataException {

        return this.containerWebAPI.doesUserHasPermissionToAddWidget(containerInode);
    }

    @HostAccess.Export
    /**
     * This method checks if the logged in user has the required permission to ADD any form into the
     * the passed container id
     */
    public boolean doesUserHasPermissionToAddForm (final String containerInode) throws DotDataException {

        return this.containerWebAPI.doesUserHasPermissionToAddForm(containerInode);
    }

    @HostAccess.Export
    public String getBaseContentTypeUserHasPermissionToAdd(final String containerInode) throws DotDataException {
        return this.containerWebAPI.getBaseContentTypeUserHasPermissionToAdd(containerInode);
    }
}
