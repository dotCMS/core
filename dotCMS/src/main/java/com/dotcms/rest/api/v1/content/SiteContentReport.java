package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.pagination.ContentReportPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This implementation of the {@link ContentReport} class provides the Content Report for one
 * or more Sites in the dotCMS repository.
 *
 * @author Jose Castro
 * @since Mar 15th, 2024
 */
public class SiteContentReport implements ContentReport {

    final User user;
    final ContentTypeAPI contentTypeAPI;
    final Lazy<HostAPI> siteAPI = Lazy.of(APILocator::getHostAPI);

    /**
     * Creates a new instance of this Helper.
     *
     * @param user The {@link User} that will access the data provided by this Helper.
     */
    public SiteContentReport(final User user) {
        this.user = user;
        this.contentTypeAPI = APILocator.getContentTypeAPI(user, false);
    }

    @Override
    public List<ContentReportView> generateContentReport(final ContentReportParams params) throws DotDataException, DotSecurityException {
        final String siteId = params.extraParam(ContentReportPaginator.SITE_PARAM);
        final Host site = this.siteAPI.get().find(siteId, params.user(), false);
        if (null == site || UtilMethods.isNotSet(site.getIdentifier())) {
            throw new DoesNotExistException("The site with the given ID does not exist: " + siteId);
        }
        final String orderByParam = SQLUtil.getOrderByAndDirectionSql(params.orderBy(),
                params.orderDirection());
        final List<ContentType> contentTypes = this.contentTypeAPI.search(params.filter(),
                BaseContentType.ANY, orderByParam, params.perPage(), params.page(),
                site.getIdentifier());
        return this.siteReportToView(contentTypes);
    }

    /**
     * Generates the final Content Report for a specific Site. It's composed of several
     * {@link ContentReportView} objects, each one representing a different Content Type and the
     * total number of its contents.
     *
     * @param contentTypes The list of {@link ContentType} objects to analyze.
     *
     * @return The list of the {@link ContentReportView} objects that make up the report.
     */
    private List<ContentReportView> siteReportToView(final List<ContentType> contentTypes) {
        if (UtilMethods.isNotSet(contentTypes)) {
            return List.of();
        }
        Map<String, Long> entriesByContentTypes = null;
        try {
            entriesByContentTypes = this.contentTypeAPI.getEntriesByContentTypes();
        } catch (final DotStateException e) {
            final String errorMsg = String.format("Error trying to retrieve total entries in all Content Types: " +
                    "%s", ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg);
            Logger.debug(this, e, () -> errorMsg);
        }
        final Map<String, Long> finalEntriesByContentTypes = null != entriesByContentTypes ?
                entriesByContentTypes : Map.of();
        return contentTypes.stream().map(contentType -> {

            final ContentReportView.Builder builder = new ContentReportView.Builder();
            builder.contentTypeName(contentType.name());
            builder.entries(finalEntriesByContentTypes.getOrDefault(Objects.requireNonNull(contentType.variable()).toLowerCase(), 0L));
            return builder.build();

        }).collect(Collectors.toList());
    }

}
