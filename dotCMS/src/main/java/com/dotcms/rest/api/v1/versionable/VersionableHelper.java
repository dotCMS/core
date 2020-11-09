package com.dotcms.rest.api.v1.versionable;

import com.dotcms.business.CloseDBIfOpened;
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
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.categories.model.Category;
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
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

public class VersionableHelper {

    private final ContentletAPI  contentletAPI;
    private final TemplateAPI    templateAPI;
    private final ContainerAPI   containerAPI;
    private final VersionableAPI versionableAPI;
    private final PermissionAPI  permissionAPI;
    private final RoleAPI        roleAPI;

    private final Map<String, versionableFindAllStrategy> assetTypeByVersionableFindAllMap;
    private final versionableFindAllStrategy              defaultVersionableFindAllStrategy;

    private final Map<String, VersionableDeleteStrategy> assetTypeByVersionableDeleteMap;
    private final VersionableDeleteStrategy              defaultVersionableDeleteStrategy;

    private final Map<String, VersionableFinderStrategy> assetTypeByVersionableFinderMap;
    private final VersionableFinderStrategy              defaultVersionableFinderStrategy;

    private final Map<String, VersionableBringBackStrategy> assetTypeByVersionableBringBackMap;
    private final VersionableBringBackStrategy              defaultVersionableBringBackStrategy;



    public VersionableHelper(final TemplateAPI        templateAPI,
                               final ContentletAPI      contentletAPI,
                               final VersionableAPI     versionableAPI,
                               final PermissionAPI      permissionAPI,
                               final RoleAPI roleAPI,
                               final ContainerAPI containerAPI) {

        this.templateAPI    = templateAPI;
        this.containerAPI   = containerAPI;
        this.contentletAPI  = contentletAPI;
        this.versionableAPI = versionableAPI;
        this.permissionAPI  = permissionAPI;
        this.roleAPI        = roleAPI;
        this.assetTypeByVersionableFindAllMap =
                new ImmutableMap.Builder<String, versionableFindAllStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::findAllVersionsByContentletId)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::findAllVersionsByTemplateId)
                        .build();
        this.defaultVersionableFindAllStrategy = this::findAllVersionsByVersionableId;

        this.assetTypeByVersionableDeleteMap =
                new ImmutableMap.Builder<String, VersionableDeleteStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::deleteContentByInode)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::deleteTemplateByInode)
                        .put(Inode.Type.CONTAINERS.getValue(), this::deleteContainerByInode)
                        .put(Inode.Type.LINKS.getValue(),      this::deleteLinkByInode)
                        .build();
        this.defaultVersionableDeleteStrategy = this::deleteContentByInode;

        this.assetTypeByVersionableFinderMap =
                new ImmutableMap.Builder<String, VersionableFinderStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::findContentletVersion)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::findTemplateVersion)
                        .put(Inode.Type.CONTAINERS.getValue(), this::findContainerVersion)
                        .put(Inode.Type.LINKS.getValue(),      this::findLinkVersion)
                        .build();
        this.defaultVersionableFinderStrategy = this::findContentletVersion;

        this.assetTypeByVersionableBringBackMap =
                new ImmutableMap.Builder<String, VersionableBringBackStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::bringBackContentletVersion)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::bringBackTemplateVersion)
                        .put(Inode.Type.CONTAINERS.getValue(), this::bringBackContainerVersion)
                        .put(Inode.Type.LINKS.getValue(),      this::bringBackLinkVersion)
                        .build();
        this.defaultVersionableBringBackStrategy = this::bringBackContentletVersion;
    }

    public Map<String, versionableFindAllStrategy> getAssetTypeByVersionableFindAllMap() {
        return assetTypeByVersionableFindAllMap;
    }

    public versionableFindAllStrategy getDefaultVersionableFindAllStrategy() {
        return defaultVersionableFindAllStrategy;
    }

    public Map<String, VersionableDeleteStrategy> getAssetTypeByVersionableDeleteMap() {
        return assetTypeByVersionableDeleteMap;
    }

    public VersionableDeleteStrategy getDefaultVersionableDeleteStrategy() {
        return defaultVersionableDeleteStrategy;
    }

    public Map<String, VersionableFinderStrategy> getAssetTypeByVersionableFinderMap() {
        return assetTypeByVersionableFinderMap;
    }

    public VersionableFinderStrategy getDefaultVersionableFinderStrategy() {
        return defaultVersionableFinderStrategy;
    }

    public Map<String, VersionableBringBackStrategy> getAssetTypeByVersionableBringBackMap() {
        return assetTypeByVersionableBringBackMap;
    }

    public VersionableBringBackStrategy getDefaultVersionableBringBackStrategy() {
        return defaultVersionableBringBackStrategy;
    }

    /// FIND ALL VERSIONS
    /**
     * This interface is just a general encapsulation to get versiones for all kind of types
     */
    @FunctionalInterface
    interface versionableFindAllStrategy {

        List<VersionableView> findAllVersions(Identifier identifier, User user, boolean respectFrontendRoles);
    }

    private List<VersionableView> findAllVersionsByContentletId(final Identifier identifier, final User user, final boolean respectFrontendRoles) {

        return Try.of(()-> this.contentletAPI.findAllVersions
                (identifier, user, respectFrontendRoles)).getOrElse(Collections.emptyList())
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }

    private List<VersionableView> findAllVersionsByTemplateId(final Identifier identifier, final User user, final boolean respectFrontendRoles) {

        return Try.of(()-> this.templateAPI.findAllVersions
                (identifier, user, respectFrontendRoles)).getOrElse(Collections.emptyList())
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }

    private List<VersionableView> findAllVersionsByVersionableId(final Identifier identifier, final User user, final boolean respectFrontendRoles) {

        return Try.of(()-> this.versionableAPI.findAllVersions
                (identifier, user, respectFrontendRoles)).getOrElse(Collections.emptyList())
                .stream().map(VersionableView::new).collect(Collectors.toList());
    }

    /// DELETE VERSION
    /**
     * This interface is just a general encapsulation to delete versions for all kind of types
     */
    @FunctionalInterface
    interface VersionableDeleteStrategy {

        void deleteVersionByInode (final String inode, final User user, final boolean respectFrontEndRoles);
    }

    @WrapInTransaction
    private void deleteTemplateByInode (final String inode, final User user, final boolean respectFrontEndRoles) {

        try {

            final Template template = this.templateAPI.find(inode, user, respectFrontEndRoles);

            if (null != template && InodeUtils.isSet(template.getInode())) {

                this.checkPermission(user, template, PERMISSION_WRITE);
                this.permissionAPI.removePermissions(template);
                this.templateAPI.deleteByInode(inode);
            } else {

                throw new DoesNotExistException("The template, inode: " + inode + " does not exists");
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    private void deleteContainerByInode (final String inode, final User user, final boolean respectFrontEndRoles) {

        try {

            final Container container = this.containerAPI.find(inode, user, respectFrontEndRoles);

            if (null != container && InodeUtils.isSet(container.getInode())) {

                // Delete any content type relationships before deleting version
                this.checkPermission(user, container, PERMISSION_WRITE);
                this.containerAPI.deleteContainerContentTypesByContainerInode(container);
                WebAssetFactory.deleteAssetVersion(container);
            } else {

                throw new DoesNotExistException("The container, inode: " + inode + " does not exists");
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    private void deleteLinkByInode (final String inode, final User user, final boolean respectFrontEndRoles) {

        try {

            final Link link = LinkFactory.getLinkFromInode(inode, user.getUserId());

            if (null != link && InodeUtils.isSet(link.getInode())) {

                this.checkPermission(user, link, PERMISSION_WRITE);
                WebAssetFactory.deleteAssetVersion(link);
            } else {

                throw new DoesNotExistException("The link, inode: " + inode + " does not exists");
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    private void deleteContentByInode(final String inode, final User user, final boolean respectFrontEndRoles) {

        try {

            final Contentlet contentlet = this.contentletAPI.find(inode, user, respectFrontEndRoles);

            if (null != contentlet && InodeUtils.isSet(contentlet.getInode())) {

                this.checkPermission(user, contentlet, PERMISSION_WRITE);
                contentletAPI.deleteVersion(contentlet, user, respectFrontEndRoles);
            } else {

                throw new DoesNotExistException("The contentlet, inode: " + inode + " does not exists");
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    ///
    private  <T extends Permissionable & Versionable> void checkPermission(final User user, final T asset, final int permissionType) throws DotDataException, DotSecurityException {

        if (!this.permissionAPI.doesUserHavePermission(asset,  permissionType, user)) {

            final Role cmsOwner      = this.roleAPI.loadCMSOwnerRole();
            final boolean isCMSOwner = this.permissionAPI.getRoles(asset.getInode(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1)
                    .stream().anyMatch(role -> role.getId().equals(cmsOwner.getId()))
                    || this.permissionAPI.getRoles(asset.getInode(), PermissionAPI.PERMISSION_WRITE, "CMS Owner", 0, -1)
                    .stream().anyMatch(role -> role.getId().equals(cmsOwner.getId()));

            if(!isCMSOwner) {

                throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
            }
        }
    }

    // FIND VERSION
    /**
     * This interface is just a general encapsulation to get version for all kind of types
     */
    @FunctionalInterface
    interface VersionableFinderStrategy {

        Optional<VersionableView> findVersion(String inode, User user, boolean respectFrontendRoles);
    }

    private Optional<VersionableView> findTemplateVersion(final String inode, final User user, final boolean respectFrontEndRoles) {

        final Template versionTemplate = Try.of(()->this.templateAPI.find(inode, user, respectFrontEndRoles)).getOrNull();
        return null != versionTemplate? Optional.of(new VersionableView(versionTemplate)): Optional.empty();
    }

    private Optional<VersionableView> findContainerVersion(final String inode, final User user, final boolean respectFrontEndRoles) {

        final Container versionContainer = Try.of(()->this.containerAPI.find(inode, user, respectFrontEndRoles)).getOrNull();
        return null != versionContainer? Optional.of(new VersionableView(versionContainer)): Optional.empty();
    }

    @CloseDBIfOpened
    private Optional<VersionableView> findLinkVersion(final String inode, final User user, final boolean respectFrontEndRoles) {

        final Link versionLink = Try.of(()-> LinkFactory.getLinkFromInode(inode, user.getUserId())).getOrNull();
        return null != versionLink? Optional.of(new VersionableView(versionLink)): Optional.empty();
    }

    @CloseDBIfOpened
    private Optional<VersionableView> findContentletVersion(final String inode, final User user, final boolean respectFrontEndRoles) {

        final Contentlet versionContentlet = Try.of(()-> contentletAPI.find(inode, user, respectFrontEndRoles)).getOrNull();
        return null != versionContentlet? Optional.of(new VersionableView(versionContentlet)): Optional.empty();
    }

    // BRIG BACK VERSION
    /**
     * This interface is just a general encapsulation to bring back version for all kind of types
     */
    @FunctionalInterface
    interface VersionableBringBackStrategy {

        Optional<VersionableView> bringBackVersion(Versionable versionable, User user, boolean respectFrontendRoles);
    }

    private Optional<VersionableView> bringBackTemplateVersion(final Versionable versionable, final User user, final boolean respectFrontEndRoles) {

        Template versionTemplate = null;

        try {

            versionTemplate = versionable instanceof Template ?
                    (Template) versionable :
                    versionable instanceof VersionableView ?
                            (Template) VersionableView.class.cast(versionable).getVersionableWrapped() :
                            this.templateAPI.find(versionable.getInode(), user, respectFrontEndRoles);

            if (null != versionTemplate) {

                this.versionableAPI.setWorking(versionTemplate);
                new TemplateLoader().invalidate(versionTemplate);
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return null != versionTemplate? Optional.of(new VersionableView(versionTemplate)): Optional.empty();
    }

    private Optional<VersionableView> bringBackContainerVersion(final Versionable versionable, final User user, final boolean respectFrontEndRoles) {

        Container versionContainer = null;

        try {

            versionContainer = versionable instanceof Container ?
                    (Container) versionable :
                    versionable instanceof VersionableView ?
                            (Container) VersionableView.class.cast(versionable).getVersionableWrapped() :
                            this.containerAPI.find(versionable.getInode(), user, respectFrontEndRoles);

            // Checking permissions
            if (null != versionContainer) {

                this.checkPermission(user, versionContainer, PERMISSION_WRITE);
                this.versionableAPI.setWorking(versionContainer);
                CacheLocator.getContainerCache().remove(versionContainer);
                new ContainerLoader().invalidate(versionContainer);
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return null != versionContainer ? Optional.of(new VersionableView(versionContainer)) : Optional.empty();
    }

    // update parents to new version delete old versions parents if not live.
    private void updateParentInodeReference (final Inode parentInode, final Link linkVersion, final Link workingLink) {

        try {

            parentInode.addChild(workingLink);
            //to keep relation types from parent only if it exists
            final Tree tree = TreeFactory.getTree(parentInode, linkVersion);
            if ((tree.getRelationType() != null) && (tree.getRelationType().length() != 0)) {

                final Tree newTree = TreeFactory.getTree(parentInode, workingLink);
                newTree.setRelationType(tree.getRelationType());
                newTree.setTreeOrder(0);
                TreeFactory.saveTree(newTree);
            }

            // checks type of parent and deletes child if not live version.
            if (!linkVersion.isLive()) {
                if (parentInode instanceof Inode) {
                    parentInode.deleteChild(linkVersion);
                }
            }
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    private Optional<VersionableView> bringBackLinkVersion(final Versionable versionable, final User user, final boolean respectFrontEndRoles) {

        Link linkVersion = null;
        final String versionLinkInode = versionable.getInode();

        try {

            linkVersion = versionable instanceof Link ?
                    (Link) versionable :
                    versionable instanceof VersionableView ?
                            (Link) VersionableView.class.cast(versionable).getVersionableWrapped() :
                            LinkFactory.getLinkFromInode(versionLinkInode, user.getUserId());

            if (null != linkVersion) {

                // Checking permissions
                this.checkPermission(user, linkVersion, PERMISSION_WRITE);
                this.versionableAPI.setWorking(linkVersion);
                final Link workingLink = linkVersion;
                final Link versionLink = LinkFactory.getLinkFromInode(versionLinkInode, user.getUserId());
                // Get parents of the old version so you can update the working information to this new version.
                Stream.concat(
                        InodeFactory.getParentsOfClass(linkVersion, Category.class).stream(),
                        InodeFactory.getParentsOfClass(linkVersion, com.dotmarketing.portlets.contentlet.business.Contentlet.class).stream())
                        .filter(parent  -> parent instanceof Inode && InodeUtils.isSet(Inode.class.cast(parent).getInode()))
                        .forEach(parent -> this.updateParentInodeReference(Inode.class.cast(parent), versionLink, workingLink));

                //Rewriting the parents contentlets of the link
                final List<com.dotmarketing.portlets.contentlet.business.Contentlet> contentlets =
                        (List<com.dotmarketing.portlets.contentlet.business.Contentlet>)InodeFactory.getParentsOfClass(workingLink,
                                com.dotmarketing.portlets.contentlet.business.Contentlet.class);

                for(com.dotmarketing.portlets.contentlet.business.Contentlet contentlet : contentlets) {
                    if (contentlet.isWorking()) {

                        final com.dotmarketing.portlets.contentlet.model.Contentlet modelContentlet =
                                this.contentletAPI.convertFatContentletToContentlet(contentlet);
                        new ContentletLoader().invalidate(modelContentlet);
                    }
                }
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return null != linkVersion? Optional.of(new VersionableView(linkVersion)): Optional.empty();
    }

    private Optional<VersionableView> bringBackContentletVersion(final Versionable versionable, final User user, final boolean respectFrontEndRoles) {

        Contentlet versionContentlet = null;

        try {

            versionContentlet = versionable instanceof Contentlet ?
                    (Contentlet) versionable :
                    versionable instanceof VersionableView ?
                            (Contentlet) VersionableView.class.cast(versionable).getVersionableWrapped() :
                            this.contentletAPI.find(versionable.getInode(), user, respectFrontEndRoles);

            if (null != versionContentlet) {

                this.contentletAPI.restoreVersion(versionContentlet, user, respectFrontEndRoles);
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return null != versionContentlet? Optional.of(new VersionableView(versionContentlet)): Optional.empty();
    }
}
