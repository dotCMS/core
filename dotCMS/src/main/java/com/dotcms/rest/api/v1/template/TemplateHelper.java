package com.dotcms.rest.api.v1.template;

import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.templates.business.FileAssetTemplateUtil;
import com.dotmarketing.portlets.templates.design.bean.Body;
import com.dotmarketing.portlets.templates.design.bean.Sidebar;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutColumn;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                .identifier(FileAssetTemplateUtil.getInstance().isFileAssetTemplate(template)?
                        FileAssetTemplateUtil.getInstance().getFullPath(FileAssetTemplate.class.cast(template)):template.getIdentifier())
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
                .lockedBy(Try.of(()->APILocator.getVersionableAPI().getLockedBy(template).orElse(null)).getOrNull())
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
                .layout(this.toLayoutView(layout))
                .containers(this.findContainerInLayout(layout))
                .build();
    }

    public TemplateLayout toTemplateLayout(final TemplateLayoutView templateLayoutView) {

        TemplateLayout layout = null;

        if (null != templateLayoutView) {

            layout = new TemplateLayout();
            layout.setBody(this.toBody(templateLayoutView.getBody()));
            layout.setSidebar(this.toSideBar(templateLayoutView.getSidebar()));
            layout.setTitle(templateLayoutView.getTitle());
            layout.setWidth(templateLayoutView.getWidth());
        }

        return layout;
    }

    private Sidebar toSideBar(final SidebarView sidebarView) {

        return null != sidebarView?
                new Sidebar(sidebarView.getContainers(), sidebarView.getLocation(), sidebarView.getWidth(), -1): null;
    }

    private Body toBody(final BodyView bodyView) {

        return null != bodyView?
                new Body(this.toTemplateLayoutRowList(bodyView.getRows())): null;
    }

    private List<TemplateLayoutRow> toTemplateLayoutRowList(final List<TemplateLayoutRowView> rows) {

        return null != rows?
                rows.stream().map(this::toTemplateLayoutRow).collect(Collectors.toList()):null;
    }

    private TemplateLayoutRow toTemplateLayoutRow(final TemplateLayoutRowView templateLayoutRowView) {

        return new TemplateLayoutRow(this.toTemplateLayoutColumnList(templateLayoutRowView.getColumns()), templateLayoutRowView.getStyleClass());
    }

    private List<TemplateLayoutColumn> toTemplateLayoutColumnList(final List<TemplateLayoutColumnView> columns) {

        return null != columns?
                columns.stream().map(this::toTemplateLayoutColumn).collect(Collectors.toList()):null;
    }

    private TemplateLayoutColumn toTemplateLayoutColumn(final TemplateLayoutColumnView templateLayoutColumnView) {
        final TemplateLayoutColumn templateLayoutColumn = new TemplateLayoutColumn(templateLayoutColumnView.getContainers(),
                        0,
                        templateLayoutColumnView.getLeftOffset(),
                        templateLayoutColumnView.getStyleClass());

        templateLayoutColumn.setWidth(templateLayoutColumnView.getWidth());
        return templateLayoutColumn;
    }

    private TemplateLayoutView toLayoutView(final TemplateLayout layout) {

        return null != layout?
                new TemplateLayoutView(layout.getWidth(),
                    layout.getTitle(), layout.isHeader(), layout.isFooter(),
                    this.toBodyView(layout.getBody()),
                    this.toSiderBarView(layout.getSidebar())
                    ):
                null;
    }

    private SidebarView toSiderBarView(final Sidebar sidebar) {

        return null != sidebar?
                new SidebarView(sidebar.getContainers(), sidebar.getLocation(), sidebar.getWidth()):
                null;
    }

    private BodyView toBodyView(final Body body) {

        return null != body?
                new BodyView(this.toTemplateLayoutRowViewList(body.getRows())):
                null;
    }

    private List<TemplateLayoutRowView> toTemplateLayoutRowViewList(final List<TemplateLayoutRow> rows) {

        return null != rows?
                rows.stream().map(this::toTemplateLayoutRowView).collect(Collectors.toList()): null;
    }

    private TemplateLayoutRowView toTemplateLayoutRowView(final TemplateLayoutRow templateLayoutRow) {

        return null != templateLayoutRow? new TemplateLayoutRowView(templateLayoutRow.getStyleClass(),
                this.toTemplateLayoutColumnViewList(templateLayoutRow.getColumns())):
                null;
    }

    private List<TemplateLayoutColumnView> toTemplateLayoutColumnViewList(final List<TemplateLayoutColumn> columns) {

        return null != columns? columns.stream().map(this::toTemplateLayoutColumnView).collect(Collectors.toList()): null;
    }

    private TemplateLayoutColumnView toTemplateLayoutColumnView(final TemplateLayoutColumn templateLayoutColumn) {

        return null != templateLayoutColumn? new TemplateLayoutColumnView(
                templateLayoutColumn.getContainers(), templateLayoutColumn.getWidth(),
                templateLayoutColumn.getLeftOffset(), templateLayoutColumn.getStyleClass()):
                null;
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
