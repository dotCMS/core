package com.dotcms.rest.api.v1.versionable;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.Map;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_EDIT;

/**
 * @author erickgonzalez
 */
public class VersionableHelper {

    private final ContentletAPI  contentletAPI;
    private final TemplateAPI    templateAPI;
    private final ContainerAPI   containerAPI;
    private final PermissionAPI  permissionAPI;


    private final Map<String, VersionableDeleteStrategy> assetTypeByVersionableDeleteMap;
    private final VersionableDeleteStrategy              defaultVersionableDeleteStrategy;

    public VersionableHelper(final TemplateAPI        templateAPI,
            final ContentletAPI      contentletAPI,
            final PermissionAPI      permissionAPI,
            final ContainerAPI containerAPI) {

        this.templateAPI    = templateAPI;
        this.containerAPI   = containerAPI;
        this.contentletAPI  = contentletAPI;
        this.permissionAPI  = permissionAPI;

        //Map to call the Delete Version method of an element using the type as the key
        this.assetTypeByVersionableDeleteMap =
                new ImmutableMap.Builder<String, VersionableDeleteStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::deleteContentByInode)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::deleteTemplateByInode)
                        .put(Inode.Type.CONTAINERS.getValue(), this::deleteContainerByInode)
                        .put(Inode.Type.LINKS.getValue(),      this::deleteLinkByInode)
                        .build();
        //Default Method to Delete Version of an element
        this.defaultVersionableDeleteStrategy = this::deleteContentByInode;

    }

    public Map<String, VersionableDeleteStrategy> getAssetTypeByVersionableDeleteMap() {
        return assetTypeByVersionableDeleteMap;
    }

    public VersionableDeleteStrategy getDefaultVersionableDeleteStrategy() {
        return defaultVersionableDeleteStrategy;
    }

    // DELETE VERSION
    /**
     * This interface is just a general encapsulation to delete versions for all kind of types
     */
    @FunctionalInterface
    interface VersionableDeleteStrategy {

        void deleteVersionByInode (final String inode, final User user, final boolean respectFrontEndRoles)
                throws DotDataException, DotSecurityException;
    }

    @WrapInTransaction
    private void deleteTemplateByInode(final String inode, final User user,
            final boolean respectFrontEndRoles)
            throws DotDataException, DotSecurityException {

        final Template template = this.templateAPI.find(inode, user, respectFrontEndRoles);

        if (null != template && InodeUtils.isSet(template.getInode())) {
            this.checkPermission(user, template);
            this.checkIsLiveOrIsWorking(template);
            this.permissionAPI.removePermissions(template);
            this.templateAPI.deleteVersionByInode(inode);
        } else {

            throw new DoesNotExistException("The template inode: " + inode + " does not exists");
        }
    }

    @WrapInTransaction
    private void deleteContainerByInode (final String inode, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {

            final Container container = this.containerAPI.find(inode, user, respectFrontEndRoles);

            if (null != container && InodeUtils.isSet(container.getInode())) {
                // Delete any content type relationships before deleting version
                this.checkPermission(user, container);
                this.checkIsLiveOrIsWorking(container);
                this.containerAPI.deleteContainerContentTypesByContainerInode(container);
                WebAssetFactory.deleteAssetVersion(container);
            } else {

                throw new DoesNotExistException("The container inode: " + inode + " does not exists");
            }

    }

    @WrapInTransaction
    private void deleteLinkByInode (final String inode, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {

            final Link link = LinkFactory.getLinkFromInode(inode, user.getUserId());

            if (null != link && InodeUtils.isSet(link.getInode())) {
                this.checkPermission(user, link);
                this.checkIsLiveOrIsWorking(link);
                WebAssetFactory.deleteAssetVersion(link);
            } else {

                throw new DoesNotExistException("The link inode: " + inode + " does not exists");
            }
    }

    @WrapInTransaction
    private void deleteContentByInode(final String inode, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {

            final Contentlet contentlet = this.contentletAPI.find(inode, user, respectFrontEndRoles);

            if (null != contentlet && InodeUtils.isSet(contentlet.getInode())) {
                this.checkPermission(user, contentlet);
                this.checkIsLiveOrIsWorking(contentlet);
                contentletAPI.deleteVersion(contentlet, user, respectFrontEndRoles);
            } else {

                throw new DoesNotExistException("The contentlet inode: " + inode + " does not exists");
            }
    }
    // END DELETE VERSION METHODS

    private void checkPermission(final User user, final Permissionable asset) throws DotDataException, DotSecurityException {

        //Check Edit Permissions over Template
        if(!this.permissionAPI.doesUserHavePermission(asset, PERMISSION_EDIT, user)){
            Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to Edit");
            throw new DotSecurityException("User does not have Permissions to Edit");
        }
    }

    private void checkIsLiveOrIsWorking(final Versionable asset)
            throws DotSecurityException, DotDataException {
        if(asset.isWorking() || asset.isLive()){
            throw new DotStateException("The versionable with Inode " + asset.getInode() + " that you are trying to delete is on Working or Live Status");
        }
    }

}
