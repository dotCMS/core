package com.dotcms.rest.api.v1.versionable;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.rendering.velocity.services.TemplateLoader;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final Map<String, VersionableFinderStrategy> assetTypeByVersionableFindVersionMap;
    private final VersionableFinderStrategy defaultVersionableFindVersionStrategy;

    private final Map<String, VersionableRestoreStrategy> assetTypeByVersionableRestoreVersionMap;
    private final VersionableRestoreStrategy              defaultVersionableRestoreStrategy;

    private final Map<String, VersionableFindAllStrategy> assetTypeByVersionableFindAllMap;
    private final VersionableFindAllStrategy defaultVersionableFindAllStrategy;

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

        //Map to call the Find Version method of an element using the type as the key
        this.assetTypeByVersionableFindVersionMap =
                new ImmutableMap.Builder<String, VersionableFinderStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::findContentletVersion)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::findTemplateVersion)
                        .put(Inode.Type.CONTAINERS.getValue(), this::findContainerVersion)
                        .put(Inode.Type.LINKS.getValue(),      this::findLinkVersion)
                        .build();
        //Default Method to Find Version of an element
        this.defaultVersionableFindVersionStrategy = this::findContentletVersion;

        //Map to call the Find All Versions method of an element using the type as the key
        this.assetTypeByVersionableFindAllMap =
                new ImmutableMap.Builder<String, VersionableFindAllStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::findAllVersionsByContentletId)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::findAllVersionsByTemplateId)
                        .build();
        //Default Method to Find All Versions of an element
        this.defaultVersionableFindAllStrategy = this::findAllVersionsByVersionableId;

        //Map to call the Restore Versions method of an element using the type as the key
        this.assetTypeByVersionableRestoreVersionMap =
                new ImmutableMap.Builder<String, VersionableRestoreStrategy>()
                .put(Inode.Type.CONTENTLET.getValue(), this::restoreContentByInode)
                .put(Inode.Type.TEMPLATE.getValue(),   this::restoreTemplateByInode)
                .put(Inode.Type.CONTAINERS.getValue(), this::restoreContainerByInode)
                .put(Inode.Type.LINKS.getValue(),      this::restoreLinkByInode)
                        .build();
        //Default Method to Restore All Versions of an element
        this.defaultVersionableRestoreStrategy = this::restoreContentByInode;
    }

    public Map<String, VersionableDeleteStrategy> getAssetTypeByVersionableDeleteMap() {
        return assetTypeByVersionableDeleteMap;
    }

    public VersionableDeleteStrategy getDefaultVersionableDeleteStrategy() {
        return defaultVersionableDeleteStrategy;
    }

    public Map<String, VersionableFinderStrategy> getAssetTypeByVersionableFindVersionMap() {
        return assetTypeByVersionableFindVersionMap;
    }

    public Map<String, VersionableRestoreStrategy>  getAssetTypeByVersionableRestoreVersionMap() {

        return assetTypeByVersionableRestoreVersionMap;
    }

    public VersionableRestoreStrategy getDefaultVersionableRestoreVersionStrategy() {

        return defaultVersionableRestoreStrategy;
    }

    public VersionableFinderStrategy getDefaultVersionableFindVersionStrategy() {
        return defaultVersionableFindVersionStrategy;
    }

    public Map<String, VersionableFindAllStrategy> getAssetTypeByVersionableFindAllMap() {
        return assetTypeByVersionableFindAllMap;
    }

    public VersionableFindAllStrategy getDefaultVersionableFindAllStrategy() {
        return defaultVersionableFindAllStrategy;
    }

    public void checkWritePermissions(final Permissionable permissionable, final User user) throws DotSecurityException {

        this.permissionAPI.checkPermission(permissionable, PermissionLevel.WRITE, user);
    }

    // RESTORE VERSION
    /**
     * This interface is just a general encapsulation to restore versions for all kind of types
     */
    @FunctionalInterface
    interface VersionableRestoreStrategy {

        VersionableView restoreVersion (final Versionable versionable, final User user, final boolean respectFrontEndRoles)
                throws DotDataException, DotSecurityException;
    }

    @WrapInTransaction
    private VersionableView restoreTemplateByInode(final Versionable versionable, final User user,
                                       final boolean respectFrontEndRoles)
            throws DotDataException, DotSecurityException {

        try {

            final WebAsset version      = (WebAsset) versionable;
            final WebAsset workingAsset = WebAssetFactory.getBackAssetVersion(version);

            CacheLocator.getTemplateCache().remove(workingAsset.getInode());
            HibernateUtil.addCommitListener("invalidate-template"+ versionable.getInode(),
                    ()->new TemplateLoader().invalidate(versionable));

            return new VersionableView(workingAsset);
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    @WrapInTransaction
    private VersionableView restoreContainerByInode (final Versionable versionable, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {

        try {

            final WebAsset version      = (WebAsset) versionable;
            final WebAsset workingAsset = WebAssetFactory.getBackAssetVersion(version);

            CacheLocator.getContainerCache().remove((Container)versionable);
            HibernateUtil.addCommitListener("invalidate-container"+versionable.getInode(),
                    ()->new ContainerLoader().invalidate(versionable));

            return new VersionableView(workingAsset);
        } catch (Exception e) {
           throw new DotDataException(e.getMessage(), e);
        }
    }

    @WrapInTransaction
    private VersionableView restoreLinkByInode (final Versionable versionable, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {

        try {

            final Link linkVersion      = (Link)versionable;
            final WebAsset version      = (WebAsset) versionable;
            final WebAsset workingLink  = WebAssetFactory.getBackAssetVersion(version);

            // Get parents of the old version so you can update the working
            // information to this new version.
            final List<Inode> parents = APILocator.getMenuLinkAPI()
                    .getParentContentlets(linkVersion.getInode())
                    .stream().map(this::getInode).collect(Collectors.toList());

            //update parents to new version delete old versions parents if not live.
            for (final Inode parentInode : parents) {

                if(InodeUtils.isSet(parentInode.getInode())){

                    parentInode.addChild(workingLink);

                    //to keep relation types from parent only if it exists
                    this.saveTree(linkVersion, workingLink, parentInode);

                    // checks type of parent and deletes child if not live version.
                    if (!linkVersion.isLive()) {

                        parentInode.deleteChild(linkVersion);
                    }
                }
            }

            final ContentletLoader contentletLoader = new ContentletLoader();
            //Rewriting the parents contentlets of the link
            APILocator.getMenuLinkAPI().getParentContentlets
                    (workingLink.getInode()).stream().filter(this::isWorking)
                    .forEach(contentletLoader::invalidate);

            return new VersionableView(workingLink);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private void saveTree(final Link linkVersion, final WebAsset workingLink,
                          final Inode parentInode) {

        final Tree tree = TreeFactory.getTree(parentInode, linkVersion);
        if ((tree.getRelationType() != null) && (tree.getRelationType().length() != 0)) {

            final Tree newTree = TreeFactory.getTree(parentInode, workingLink);
            newTree.setRelationType(tree.getRelationType());
            newTree.setTreeOrder(0);
            TreeFactory.saveTree(newTree);
        }
    }

    @NotNull
    private Inode getInode(final Contentlet contentlet) {

        final Inode inode = new Inode();
        inode.setInode(contentlet.getInode());
        return inode;
    }

    private boolean isWorking(final Contentlet contentlet) {

        try {

            return contentlet.isWorking();
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    private VersionableView restoreContentByInode(final Versionable versionable, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {

        final Contentlet contentletVersionToRestore = (Contentlet) versionable;
        this.contentletAPI.restoreVersion(contentletVersionToRestore, user, respectFrontEndRoles);

        return new VersionableView(contentletVersionToRestore);
    }
    // END RESTORE VERSION METHODS



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

            throw new DoesNotExistException("The template inode: " + inode + " does not exist");
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
                this.containerAPI.deleteVersionByInode(inode);
            } else {

                throw new DoesNotExistException("The container inode: " + inode + " does not exist");
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

                throw new DoesNotExistException("The link inode: " + inode + " does not exist");
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

                throw new DoesNotExistException("The contentlet inode: " + inode + " does not exist");
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

    // FIND VERSION
    /**
     * This interface is just a general encapsulation to get version for all kind of types
     */
    @FunctionalInterface
    interface VersionableFinderStrategy {

        VersionableView findVersion(String inode, User user, boolean respectFrontendRoles)
                throws DotDataException, DotSecurityException;
    }

    private VersionableView findTemplateVersion(final String inode, final User user, final boolean respectFrontEndRoles)
            throws DotDataException, DotSecurityException {
        return new VersionableView(this.templateAPI.find(inode, user, respectFrontEndRoles));
    }

    private VersionableView findContainerVersion(final String inode, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {
        return new VersionableView(this.containerAPI.find(inode, user, respectFrontEndRoles));
    }

    private VersionableView findLinkVersion(final String inode, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {
        return new VersionableView(LinkFactory.getLinkFromInode(inode, user.getUserId()));
    }

    private VersionableView findContentletVersion(final String inode, final User user, final boolean respectFrontEndRoles)
            throws DotSecurityException, DotDataException {
        return new VersionableView(contentletAPI.find(inode, user, respectFrontEndRoles));
    }
    //END FIND VERSION METHODS

    // FIND ALL VERSIONS
    /**
     * This interface is just a general encapsulation to get versiones for all kind of types
     */
    @FunctionalInterface
    interface VersionableFindAllStrategy {

        List<VersionableView> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles)
                throws DotDataException, DotSecurityException;
    }

    private List<VersionableView> findAllVersionsByContentletId(final Identifier identifier, final User user, final boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {

        return this.contentletAPI.findAllVersions(identifier, user, respectFrontendRoles)
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }

    private List<VersionableView> findAllVersionsByTemplateId(final Identifier identifier, final User user, final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        return this.templateAPI.findAllVersions(identifier, user, respectFrontendRoles)
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }

    private List<VersionableView> findAllVersionsByVersionableId(final Identifier identifier, final User user, final boolean respectFrontendRoles)
            throws DotSecurityException, DotDataException {

        return APILocator.getVersionableAPI().findAllVersions(identifier, user, respectFrontendRoles)
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }
    // END FIND ALL VERSIONS METHODS

}
