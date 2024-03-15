package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

import java.util.List;

/**
 * This helper class provides utility methods to generate Content Reports for different dotCMS
 * objects.
 * <p> Each dotCMS object may have its own way to generate the Content Report. For instance, the
 * report for a Site uses the Content Type API, whereas the report for a Folder uses the Folder API
 * to retrieve the required data. You just need to make sure that the implementation you add for a
 * specific dotCMS object transforms its data into the expected {@link ContentReportView}
 * object.</p>
 *
 * @author Jose Castro
 * @since Mar 7th, 2024
 */
public abstract class ContentReportHelper {

    final User user;
    final ContentTypeAPI contentTypeAPI;
    final Lazy<HostAPI> siteAPI = Lazy.of(APILocator::getHostAPI);
    final Lazy<FolderAPI> folderAPI = Lazy.of(APILocator::getFolderAPI);

    /**
     * Default class constructor. Specifying a User is required in order to access the Content Type
     * API.
     *
     * @param user The {@link User} accessing this Helper.
     */
    public ContentReportHelper(final User user) {
        this.user = user;
        this.contentTypeAPI = APILocator.getContentTypeAPI(user, false);
    }

    /**
     * Generates the Content Report for a specific dotCMS object. Every implementation must define
     * its own way to pass down the expected filtering criteria, and specific information of the
     * Site, Folder, or any other dotCMS object that the report will be generated for.
     *
     * @param params The {@link ContentReportParams} object with the filtering criteria used to
     *               generate the report.
     *
     * @return The list of the {@link ContentReportView} objects that make up the report.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException The specified User does not have the necessary permissions to
     *                              perform this action.
     */
    public abstract List<ContentReportView> generateContentReport(final ContentReportParams params) throws DotDataException, DotSecurityException;

}
