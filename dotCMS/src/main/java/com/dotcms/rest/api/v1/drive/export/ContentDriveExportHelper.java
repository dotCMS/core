package com.dotcms.rest.api.v1.drive.export;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 * Runs a Content Drive export: resolves which rows the request exports and builds the file.
 * <p>
 * In filter mode the rows are everything the Drive listing shows for the request's query, which
 * must name exactly one content type; the result is one CSV. In selection mode the rows come from
 * the selected folders and contentlets; the result is a zip with one CSV per content type.
 */
public class ContentDriveExportHelper {

    /**
     * Builds the export a request asks for.
     *
     * @param form    the request body
     * @param user    the {@link User} the export runs as
     * @param request the HTTP request, used for the download links of binary fields
     * @return the finished file; when nothing readable matched it reports zero rows and holds no
     *         file
     * @throws com.dotcms.rest.exception.BadRequestException when the request is invalid
     * @throws DotDataException     if the rows cannot be read or the file cannot be written
     * @throws DotSecurityException if the user cannot read the site or folder being exported
     * @throws IOException          if the temp file cannot be written
     */
    public ExportFile export(final DriveExportRequestForm form, final User user,
            final HttpServletRequest request)
            throws DotDataException, DotSecurityException, IOException {
        throw new UnsupportedOperationException("not implemented");
    }
}
