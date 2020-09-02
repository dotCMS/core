package com.dotcms.rest.api.v1.template;

import com.dotcms.util.pagination.TemplateView;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

/**
 * Helper for templates
 * @author jsanca
 */
public class TemplateHelper {

    private final PermissionAPI  permissionAPI;
    private final RoleAPI roleAPI;

    public TemplateHelper() {
        this(APILocator.getPermissionAPI(),
                APILocator.getRoleAPI());
    }

    @VisibleForTesting
    public TemplateHelper(final PermissionAPI  permissionAPI,
                          final RoleAPI        roleAPI) {

        this.permissionAPI  = permissionAPI;
        this.roleAPI        = roleAPI;
    }

    public TemplateView toTemplateView(final Template template, final User user) {

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
                .build();
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
