package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Handle {@link com.dotmarketing.portlets.templates.model.Template} pagination
 * @author jsanca
 */
public class TemplatePaginator implements PaginatorOrdered<TemplateView> {

    public static final String HOST_PARAMETER_ID = "host";

    private final TemplateAPI templateAPI;

    public TemplatePaginator() {
        this(APILocator.getTemplateAPI());
    }

    @VisibleForTesting
    public TemplatePaginator(final TemplateAPI templateAPI) {

        this.templateAPI = templateAPI;
    }

    @Override
    public PaginatedArrayList<TemplateView> getItems(final User user, final String filter, final int limit, final int offset,
                                                  final String orderby, final OrderDirection direction,
                                                  final Map<String, Object> extraParams) {
        String hostId = null;

        if (extraParams != null) {
            hostId = (String) extraParams.get(HOST_PARAMETER_ID);
        }

        final Map<String, Object> params = map("title", filter);

        String orderByDirection = orderby;
        if (UtilMethods.isSet(direction) && UtilMethods.isSet(orderby)) {
            orderByDirection = new StringBuffer(orderByDirection)
                    .append(" ")
                    .append(direction.toString().toLowerCase()).toString();
        }

        try {
            final PaginatedArrayList<Template> allTemplates =
                    (PaginatedArrayList<Template>) templateAPI.findTemplates(user, false, params, hostId,
                    null, null, null, offset, limit, orderByDirection);

            if (UtilMethods.isSet(hostId)) {

                allTemplates.stream().sorted(
                        direction == OrderDirection.ASC?Comparator.comparing(this::hostname):
                                Comparator.comparing(this::hostname).reversed());
            }

            final PaginatedArrayList<TemplateView> templates =
                    new PaginatedArrayList<>();
            templates.addAll(allTemplates.stream().map(template -> TemplatePaginator.toTemplateView(template, user)).collect(Collectors.toList()));
            templates.setQuery(allTemplates.getQuery());
            templates.setTotalResults(allTemplates.getTotalResults());

            return templates;
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new PaginationException(e);
        }
    }

    public static TemplateView toTemplateView(final Template template, final User user) {

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

                .canRead(Try.of(()->APILocator.getPermissionAPI().doesUserHavePermission(template, PermissionAPI.PERMISSION_READ, user)).getOrElse(false))
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

    private String hostname (final Template template) {

        try {
            return Host.class.cast(template.getParentPermissionable()).getHostname();
        } catch (Exception e) {
            return StringPool.BLANK;
        }
    }

}
