package com.dotcms.util.pagination;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.content.ContentReport;
import com.dotcms.rest.api.v1.content.ContentReportParams;
import com.dotcms.rest.api.v1.content.ContentReportView;
import com.dotcms.rest.api.v1.content.FolderContentReport;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.util.List;
import java.util.Optional;

/**
 * This implementation of the {@link ContentReportPaginator} provides the Content Report for all the
 * Content Types living under a specific Folder in dotCMS. The generated report analyzes all the
 * child Folders inside the specified one as well.
 *
 * @author Jose Castro
 * @since Mar 8th, 2024
 */
public class FolderContentReportPaginator extends ContentReportPaginator<ContentReportView> {

    private final ContentReport contentReportHelper;

    /**
     * Creates a new instance of this Paginator.
     *
     * @param user The {@link User} that will access the data provided by this Paginator.
     */
    public FolderContentReportPaginator(final User user) {
        this.contentReportHelper = new FolderContentReport(user);
    }

    @Override
    public PaginatedArrayList<ContentReportView> getItems(final ContentReportParams params) throws PaginationException {
        final PaginatedArrayList<ContentReportView> result = new PaginatedArrayList<>();
        final String folder = params.extraParam(ContentReportPaginator.FOLDER_PARAM);
        final String site = params.extraParam(ContentReportPaginator.SITE_PARAM);
        try {
            final List<ContentReportView> report =
                    contentReportHelper.generateContentReport(params);
            result.addAll(report);
            result.setTotalResults(this.getTotalRecords(folder, site, params.user()));
            return result;
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("Failed to generate the Content Report for Folder " +
                    "'%s': %s", folder, ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new PaginationException(e);
        }
    }

    /**
     * Returns the total amount of the specified Content Types living a given Folder and its
     * sub-Folders.
     *
     * @param folder Condition that the base Content Type needs to meet.
     * @param site   One or more IDs of the Sites where the total amount of Content Types will be
     *               determined.
     * @param user   The {@link User} calling this action.
     *
     * @return The total amount of Content Types living under a Folder.
     */
    private long getTotalRecords(final String folder, final String site, final User user) {
        try {
            final Optional<Folder> folderOpt =
                    ((FolderContentReport) contentReportHelper).resolveFolder(folder, site,
                            user);
            if (folderOpt.isPresent()) {
                return Sneaky.sneak(() -> APILocator.getFolderAPI().getContentTypeCount(folderOpt.get(), user, false));
            }
        } catch (final DotDataException | DotSecurityException e) {
            Logger.debug(this, () -> String.format("Total Content Tpe count for folder '%s' in Site '%s' " +
                    "could not be determined: %s", folder, site, ExceptionUtil.getErrorMessage(e)));
        }
        return -1;
    }

}
