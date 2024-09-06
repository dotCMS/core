package com.dotcms.rest.api.v1.content;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

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
public interface ContentReport {

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
    List<ContentReportView> generateContentReport(final ContentReportParams params) throws DotDataException, DotSecurityException;

}
