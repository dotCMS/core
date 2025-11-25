package com.dotcms.util.pagination;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.content.ContentReport;
import com.dotcms.rest.api.v1.content.ContentReportParams;
import com.dotcms.rest.api.v1.content.ContentReportView;
import com.dotcms.rest.api.v1.content.SiteContentReport;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.List;

import static com.liferay.util.StringPool.BLANK;

/**
 * This implementation of the {@link ContentReportPaginator} provides the Content Report for all
 * Content Types living under a specific Site in dotCMS.
 *
 * @author Jose Castro
 * @since Mar 8th, 2024
 */
public class SiteContentReportPaginator extends ContentReportPaginator<ContentReportView> {

    private final ContentReport contentReportHelper;
    final ContentTypeAPI contentTypeAPI;

    /**
     * Creates a new instance of this Paginator.
     *
     * @param user The {@link User} that will access the data provided by this Paginator.
     */
    public SiteContentReportPaginator(final User user) {
        this.contentReportHelper = new SiteContentReport(user);
        this.contentTypeAPI = APILocator.getContentTypeAPI(user, false);
    }

    @Override
    public PaginatedArrayList<ContentReportView> getItems(final ContentReportParams params) throws PaginationException {
        final PaginatedArrayList<ContentReportView> result = new PaginatedArrayList<>();
        final String siteId = params.extraParam(ContentReportPaginator.SITE_PARAM, BLANK);
        final BaseContentType type = BaseContentType.ANY;
        try {
            final List<ContentReportView> report = contentReportHelper.generateContentReport(params);
            result.addAll(report);
            result.setTotalResults(this.getTotalRecords(params.filter(), type, List.of(siteId)));
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("Failed to generate the Content Report for Site " +
                    "'%s': %s", siteId, ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new PaginationException(e);
        }
        return result;
    }

    /**
     * Returns the total amount of the specified Base Content Types living in a list of Site.
     *
     * @param condition Condition that the base Content Type needs to meet.
     * @param type      The Base Content Type to search for.
     * @param siteIds   One or more IDs of the Sites where the total amount of Content Types will be
     *                  determined.
     *
     * @return The total amount of Base Types living in the specified list of Site.
     */
    private long getTotalRecords(final String condition, final BaseContentType type,
                                 final List<String> siteIds) {
        try {
            return this.contentTypeAPI.countForSites(condition, type, siteIds);
        } catch (final DotDataException e) {
            Logger.debug(this, () -> String.format("Total Content Type count for Sites [ %s ] " +
                    "could not be determined: %s", siteIds, ExceptionUtil.getErrorMessage(e)));
        }
        return -1;
    }

}
