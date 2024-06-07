package com.dotcms.rest.api.v1.template;

import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Theme;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.templates.design.bean.Body;
import com.dotmarketing.portlets.templates.design.bean.Sidebar;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutColumn;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayoutRow;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.model.TemplateWrapper;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Provides utility methods that allow classes such as the {@link TemplateResource} to interact with
 * Templates in dotCMS. Additionally, provides useful mechanisms to transform Template to and from
 * intermediate objects such as {@link TemplateView}, {@link TemplateLayoutView},
 * {@link SidebarView}, and so on.
 *
 * @author jsanca
 * @since Aug 20th, 2020
 */
public class TemplateHelper {

    private final ContainerAPI containerAPI;

    public TemplateHelper() {
        this(APILocator.getContainerAPI());
    }

    @VisibleForTesting
    public TemplateHelper(final ContainerAPI containerAPI) {
        this.containerAPI = containerAPI;
    }

    public Host getHost (final String hostId, final Supplier<Host> hostSupplier) {

        if (UtilMethods.isSet(hostId)) {

            return Try.of(()->APILocator.getHostAPI().find(hostId, APILocator.systemUser(), false))
                    .getOrElse(hostSupplier);
        }

        return hostSupplier.get();
    }

    /**
     * Takes a given Template and transforms it into a TemplateView representation that is used to
     * generate a JSON response.
     *
     * @param template The {@link Template} object.
     * @param user     The {@link User} requesting the view.
     *
     * @return The {@link TemplateView} object with the Template data.
     */
    public TemplateView toTemplateView(final Template template, final User user) {

        final TemplateLayout layout = UtilMethods.isSet(template.getDrawedBody()) ?
                DotTemplateTool.getTemplateLayout(template.getDrawedBody()) : null;
        final Theme templateTheme =
                Try.of(() -> APILocator.getThemeAPI().findThemeById(template.getTheme(), user,
                        false)).getOrNull();
        if (null != templateTheme) {
            template.setThemeName(templateTheme.getName());
        }

        Host parentHost = null;
        if(template instanceof TemplateWrapper) {
            parentHost = ((TemplateWrapper) template).getHost();
        } else {
            try{
                parentHost = APILocator.getTemplateAPI().getTemplateHost(template);
            }catch(DotDataException e){
                Logger.warn(this, "Could not find host for template = " + template.getIdentifier());
            }
        }

        final TemplateView.Builder builder = new TemplateView.Builder()
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
                .hasLiveVersion(Try.of(template::hasLiveVersion).getOrElse(false))
                .deleted(Try.of(template::isDeleted).getOrElse(false))
                .live(Try.of(template::isLive).getOrElse(false))
                .locked(Try.of(template::isLocked).getOrElse(false))
                .lockedBy(Try.of(()->APILocator.getVersionableAPI().getLockedBy(template).orElse(null)).getOrNull())
                .working(Try.of(template::isWorking).getOrElse(false))

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
                .themeInfo(null != templateTheme ? new ThemeView(templateTheme) : null);

        if (template.isAnonymous()){
            try {
                final String customLayoutTemplateLabel = LanguageUtil.get("editpage.properties.template.custom.label");
                builder.fullTitle(customLayoutTemplateLabel);
                builder.htmlTitle(customLayoutTemplateLabel);
            } catch (LanguageException e) {
                Logger.error(this.getClass(),
                        "Exception on toTemplateView exception message: " + e.getMessage(), e);
            }
        } else if(parentHost != null) {
            builder.hostName(parentHost.getHostname());
            builder.hostId(parentHost.getIdentifier());

            builder.fullTitle(template.getTitle() + " (" + parentHost.getHostname() + ")" );
            builder.htmlTitle("<div>" + template.getTitle()  + "</div><small>" + parentHost.getHostname() + "</div></small>" );
        } else {
            builder.fullTitle(template.getTitle());
            builder.htmlTitle(template.getTitle());
        }

        return builder.build();
    }

    public TemplateLayout toTemplateLayout(final TemplateLayoutView templateLayoutView) {

        TemplateLayout layout = null;

        if (null != templateLayoutView) {
            layout = new TemplateLayout();
            layout.setBody(this.toBody(templateLayoutView.getBody()));
            layout.setSidebar(this.toSideBar(templateLayoutView.getSidebar()));
            layout.setTitle(templateLayoutView.getTitle());
            layout.setWidth(templateLayoutView.getWidth());
            layout.setHeader(templateLayoutView.isHeader());
            layout.setFooter(templateLayoutView.isFooter());
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
                    this.toSideBarView(layout.getSidebar())
                    ):
                null;
    }

    private SidebarView toSideBarView(final Sidebar sidebar) {

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

                        Logger.info(this, ()-> "Container id: " + containerIdOrPath +
                                " in layout: " + templateLayout.getTitle() + " does not exist!");
                    }
                }
            }
        }

        return containers;
    }

}
