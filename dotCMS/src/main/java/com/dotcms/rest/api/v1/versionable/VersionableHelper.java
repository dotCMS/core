package com.dotcms.rest.api.v1.versionable;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.api.v1.template.TemplateHelper;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
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
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

public class VersionableHelper {

    private final ContentletAPI  contentletAPI;
    private final TemplateAPI    templateAPI;
    private final ContainerAPI   containerAPI;
    private final VersionableAPI versionableAPI;
    private final PermissionAPI  permissionAPI;
    private final RoleAPI        roleAPI;

    private final Map<String, VersionableFinderStrategy> assertTypeByVersionableFinderMap;
    private final VersionableFinderStrategy              defaultVersionableFinderStrategy;

    private final Map<String, VersionableDeleteStrategy> assertTypeByVersionableDeleteMap;
    private final VersionableDeleteStrategy              defaultVersionableDeleteStrategy;

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
        this.assertTypeByVersionableFinderMap =
                new ImmutableMap.Builder<String, VersionableFinderStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::findAllVersionsByContentletId)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::findAllVersionsByTemplateId)
                        .build();
        this.defaultVersionableFinderStrategy = this::findAllVersionsByVersionableId;

        this.assertTypeByVersionableDeleteMap =
                new ImmutableMap.Builder<String, VersionableDeleteStrategy>()
                        .put(Inode.Type.CONTENTLET.getValue(), this::deleteContentByInode)
                        .put(Inode.Type.TEMPLATE.getValue(),   this::deleteTemplateByInode)
                        .put(Inode.Type.CONTAINERS.getValue(), this::deleteContainerByInode)
                        .put(Inode.Type.LINKS.getValue(),      this::deleteLinkByInode)
                        .build();
        this.defaultVersionableDeleteStrategy = this::deleteContentByInode;
    }

    public Map<String, VersionableFinderStrategy> getAssertTypeByVersionableFinderMap() {
        return assertTypeByVersionableFinderMap;
    }

    public VersionableFinderStrategy getDefaultVersionableFinderStrategy() {
        return defaultVersionableFinderStrategy;
    }

    public Map<String, VersionableDeleteStrategy> getAssertTypeByVersionableDeleteMap() {
        return assertTypeByVersionableDeleteMap;
    }

    public VersionableDeleteStrategy getDefaultVersionableDeleteStrategy() {
        return defaultVersionableDeleteStrategy;
    }

    /////////
    /**
     * This interface is just a general encapsulation to get versiones for all kind of types
     */
    @FunctionalInterface
    interface VersionableFinderStrategy {

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

    /////
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
}
