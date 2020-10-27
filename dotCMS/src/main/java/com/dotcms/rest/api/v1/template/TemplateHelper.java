package com.dotcms.rest.api.v1.template;

import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.util.pagination.TemplateView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.ContainerFinderByIdOrPathStrategy;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.business.LiveContainerFinderByIdOrPathStrategyResolver;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Helper for templates
 * @author jsanca
 */
public class TemplateHelper {

    private final PermissionAPI  permissionAPI;
    private final RoleAPI roleAPI;
    private final ContainerAPI containerAPI;

    public TemplateHelper() {
        this(APILocator.getPermissionAPI(),
                APILocator.getRoleAPI(),
                APILocator.getContainerAPI());
    }

    @VisibleForTesting
    public TemplateHelper(final PermissionAPI  permissionAPI,
                          final RoleAPI        roleAPI,
                          final ContainerAPI containerAPI) {

        this.permissionAPI  = permissionAPI;
        this.roleAPI        = roleAPI;
        this.containerAPI   = containerAPI;
    }

    public TemplateView toTemplateView(final Template template, final User user) {

        final TemplateLayout layout = UtilMethods.isSet(template.getDrawedBody())?
                DotTemplateTool.getTemplateLayout(template.getDrawedBody()): null;

        return new TemplateView.Builder()
                .name(template.getName())
                .friendlyName(template.getFriendlyName())
                .title(template.getTitle())
                .identifier(template.getIdentifier())
                .image(template.getImage())
                .selectedimage(template.getSelectedimage())
                .inode(template.getInode())

                .theme(template.getTheme())
                .themeName(template.getThemeName())
                .headCode(template.getHeadCode())
                .header(template.getHeader())
                .body(template.getBody())
                .drawed(template.isDrawed())
                .drawedBody(template.getDrawedBody())
                .footer(template.getFooter())

                .isNew(template.isNew())
                .hasLiveVersion(Try.of(()->template.hasLiveVersion()).getOrElse(false))
                .deleted(Try.of(()->template.isDeleted()).getOrElse(false))
                .live(Try.of(()->template.isLive()).getOrElse(false))
                .locked(Try.of(()->template.isLocked()).getOrElse(false))
                .working(Try.of(()->template.isWorking()).getOrElse(false))

                .canRead(Try.of(()-> APILocator.getPermissionAPI().doesUserHavePermission(template, PermissionAPI.PERMISSION_READ, user)).getOrElse(false))
                .canWrite(Try.of(()->APILocator.getPermissionAPI().doesUserHavePermission(template, PermissionAPI.PERMISSION_EDIT, user)).getOrElse(false))
                .canPublish(Try.of(()->APILocator.getPermissionAPI().doesUserHavePermission(template, PermissionAPI.PERMISSION_PUBLISH, user)).getOrElse(false))

                .categoryId(template.getCategoryId())
                .countAddContainer(template.getCountAddContainer())
                .countContainers(template.getCountContainers())
                .modDate(template.getModDate())
                .modUser(template.getModUser())
                .owner(template.getOwner())
                .showOnMenu(template.isShowOnMenu())
                .sortOrder(template.getSortOrder())
                .layout(layout)
                .containers(this.findContainerInLayout(layout))
                .build();
    }

    private Set<ContainerView> findContainerInLayout (final TemplateLayout templateLayout) {

        final Set<ContainerView> containers = new HashSet<>();
        final User user = APILocator.systemUser();

        if (null != templateLayout) {

            final Set<String> containerIdSet = templateLayout.getContainersIdentifierOrPath();
            if (null != containerIdSet) {

                for (final String containerIdOrPath : containerIdSet) {

                    final Optional<Container> container = Try.of(()->this.containerAPI.findContainer(containerIdOrPath, user, false, false))
                            .onFailure(e -> Logger.error(TemplateHelper.class, e.getMessage(), e))
                            .getOrElse(Optional.empty());

                    if (container.isPresent()) {

                        containers.add(new ContainerView(container.get()));
                    } else {

                        Logger.info(this, ()-> "The container id: " + containerIdOrPath +
                                " is on the layout: " + templateLayout.getTitle() + " does not exists!");
                    }
                }
            }
        }

        return containers;
    }

    public void checkPermission(final User user, final Template currentTemplate, final int permissionType) throws DotDataException, DotSecurityException {

        if (!this.permissionAPI.doesUserHavePermission(currentTemplate,  permissionType, user)) {

            final Role cmsOwner      = this.roleAPI.loadCMSOwnerRole();
            final boolean isCMSOwner = this.permissionAPI.getRoles(currentTemplate.getInode(), PermissionAPI.PERMISSION_PUBLISH, "CMS Owner", 0, -1)
                    .stream().anyMatch(role -> role.getId().equals(cmsOwner.getId()))
                    || this.permissionAPI.getRoles(currentTemplate.getInode(), PermissionAPI.PERMISSION_WRITE, "CMS Owner", 0, -1)
                    .stream().anyMatch(role -> role.getId().equals(cmsOwner.getId()));

            if(!isCMSOwner) {

                throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
            }
        }
    }
}
