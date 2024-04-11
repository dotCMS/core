package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.template.TemplateHelper;
import com.dotcms.rest.api.v1.template.TemplateView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handle {@link com.dotmarketing.portlets.templates.model.Template} pagination
 * @author jsanca
 */
public class TemplatePaginator implements PaginatorOrdered<TemplateView> {

    public static final String HOST_PARAMETER_ID = "host";

    private final TemplateAPI templateAPI;
    private final TemplateHelper templateHelper;

    @VisibleForTesting
    public TemplatePaginator(final TemplateAPI templateAPI, final TemplateHelper templateHelper) {

        this.templateAPI    = templateAPI;
        this.templateHelper = templateHelper;
    }

    @Override
    public PaginatedArrayList<TemplateView> getItems(final User user, final String filter, final int limit, final int offset,
                                                     final String orderby, final OrderDirection direction,
                                                     final Map<String, Object> extraParams) {
        String hostId   = null;
        boolean archive = false;

        if (extraParams != null) {
            hostId  = (String) extraParams.get(HOST_PARAMETER_ID);
            archive = (boolean)extraParams.getOrDefault("archive", false);
        }

        final Map<String, Object> params = Map.of("filter", filter);

        String orderByDirection = orderby;
        if (UtilMethods.isSet(direction) && UtilMethods.isSet(orderby)) {
            orderByDirection = this.mapOrderBy(orderByDirection) + " " + direction.toString().toLowerCase();
        }

        try {
            final PaginatedArrayList<Template> allTemplates =
                    (PaginatedArrayList<Template>) templateAPI.findTemplates(user, archive, params, hostId,
                    null, null, null, offset, limit, orderByDirection);

            if (UtilMethods.isSet(hostId)) {

                allTemplates.stream().sorted(
                        direction == OrderDirection.ASC?Comparator.comparing(this::hostname):
                                Comparator.comparing(this::hostname).reversed());
            }

            final PaginatedArrayList<TemplateView> templates =
                    new PaginatedArrayList<>();
            templates.addAll(allTemplates.stream().map(template -> this.templateHelper.toTemplateView(template, user)).collect(Collectors.toList()));
            templates.setQuery(allTemplates.getQuery());
            templates.setTotalResults(templateAPI.findTemplates(user, archive, params, hostId,
                    null, null, null, 0, -1, orderByDirection).size());

            return templates;
        } catch (DotSecurityException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new PaginationException(e);
        }
    }

    private final Map<String, String> orderByMapping = Map.of("name", "title", "modDate", "mod_date");

    private String mapOrderBy(final String orderBy) {
        return this.orderByMapping.getOrDefault(orderBy.trim(), orderBy);
    }

    private String hostname (final Template template) {

        try {
            return Host.class.cast(template.getParentPermissionable()).getHostname();
        } catch (Exception e) {
            return StringPool.BLANK;
        }
    }

}
