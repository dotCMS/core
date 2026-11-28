package com.dotcms.rest.api.v1.drive.export;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.content.export.ContentletCsvWriter;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.browser.ESQueryTooComplexException;
import com.dotcms.browser.ExportCandidates;
import com.dotcms.browser.ExportRow;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.drive.ContentDriveHelper;
import com.dotcms.rest.api.v1.drive.DriveRequestForm;
import com.dotcms.rest.api.v1.drive.export.AbstractDriveExportRequestForm.Mode;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * Runs a Content Drive export: resolves which rows the request exports and builds the file.
 * <p>
 * In filter mode the rows are everything the Drive listing shows for the request's query, which
 * must name exactly one content type; the result is one CSV. In selection mode the rows come from
 * the selected folders and contentlets; the result is a zip with one CSV per content type.
 */
public class ContentDriveExportHelper {

    /** The configuration key of the most rows one export may hold. */
    public static final String MAX_ROWS_KEY = "CONTENT_DRIVE_EXPORT_MAX_ROWS";
    /** The most rows one export may hold when {@link #MAX_ROWS_KEY} is not set. */
    public static final int MAX_ROWS_DEFAULT = 100_000;

    static final String ONE_TYPE_MESSAGE = "Choose exactly one content type you can read to export.";
    static final String FORMS_AND_SITES_MESSAGE = "Forms and sites can't be exported.";
    static final String FOLDER_NOT_FOUND_MESSAGE = "Folder not found.";

    private final ContentDriveHelper driveHelper;
    private final BrowserAPI browserAPI;
    private final ExportFileBuilder fileBuilder;
    private final HostAPI hostAPI = APILocator.getHostAPI();
    private final SelectionResolver selectionResolver = new SelectionResolver();

    /**
     * Creates a helper that uses the standard dotCMS APIs.
     */
    public ContentDriveExportHelper() {
        this(ContentDriveHelper.newInstance(), APILocator.getBrowserAPI(), new ExportFileBuilder());
    }

    /**
     * Creates a helper that writes with the given CSV writer, into the standard export directory.
     * Tests outside this package use it to make the writing fail.
     *
     * @param writer writes each content type's CSV
     */
    @VisibleForTesting
    public ContentDriveExportHelper(final ContentletCsvWriter writer) {
        this(ContentDriveHelper.newInstance(), APILocator.getBrowserAPI(),
                new ExportFileBuilder(writer, ExportFileBuilder.defaultDirectory()));
    }

    /**
     * Creates a helper with the given collaborators.
     *
     * @param driveHelper builds the listing query from the request
     * @param browserAPI  resolves the rows of a query
     * @param fileBuilder writes the file
     */
    ContentDriveExportHelper(final ContentDriveHelper driveHelper, final BrowserAPI browserAPI,
            final ExportFileBuilder fileBuilder) {
        this.driveHelper = driveHelper;
        this.browserAPI = browserAPI;
        this.fileBuilder = fileBuilder;
    }

    /**
     * Builds the export a request asks for.
     *
     * @param form    the request body
     * @param user    the {@link User} the export runs as
     * @param request the HTTP request, used for the download links of binary fields
     * @return the finished file; when nothing readable matched it reports zero rows and holds no
     *         file
     * @throws BadRequestException  when the request is invalid, or matches more rows than the limit
     * @throws NotFoundException    when the site or folder does not exist or cannot be read
     * @throws DotDataException     if the rows cannot be read or the file cannot be written
     * @throws DotSecurityException if the user cannot read the site or folder being exported
     * @throws IOException          if the temp file cannot be written
     */
    public ExportFile export(final DriveExportRequestForm form, final User user,
            final HttpServletRequest request)
            throws DotDataException, DotSecurityException, IOException {
        if (form.validate() == Mode.FILTER) {
            return write(resolveFilterMode(form.query(), user), user, request);
        }
        return exportSelection(form, user, request);
    }

    /**
     * Selection mode: the selected contentlets, and the content of the selected folders, as a zip
     * with one CSV per content type (research R5, R6).
     *
     * @param form    the request, already validated
     * @param user    the {@link User} the export runs as
     * @param request the HTTP request, used for the download links of binary fields
     * @return the zip; it reports zero rows, and holds no file, when nothing readable was selected
     * @throws NotFoundException   when the site does not exist or cannot be read
     * @throws BadRequestException when more rows are selected than the limit
     * @throws DotDataException    if the rows cannot be read or the file cannot be written
     * @throws IOException         if the temp file cannot be written
     */
    private ExportFile exportSelection(final DriveExportRequestForm form, final User user,
            final HttpServletRequest request) throws DotDataException, IOException {
        final Host site = browsedSite(form.siteId(), user);

        final int maxRows = Config.getIntProperty(MAX_ROWS_KEY, MAX_ROWS_DEFAULT);
        final ExportCandidates candidates = selectionResolver.resolve(form, site, user, maxRows);
        if (candidates.overLimit()) {
            throw new BadRequestException(overLimitMessage(maxRows));
        }

        // One CSV per content type, in the order the types first appear
        final Map<String, List<String>> inodesByTypeId = new LinkedHashMap<>();
        for (final ExportRow row : candidates.rows()) {
            inodesByTypeId.computeIfAbsent(row.contentTypeId(), id -> new ArrayList<>())
                    .add(row.inode());
        }
        final ContentTypeAPI systemContentTypeAPI =
                APILocator.getContentTypeAPI(APILocator.systemUser());
        final Map<ContentType, List<String>> inodesByType = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> entry : inodesByTypeId.entrySet()) {
            try {
                inodesByType.put(systemContentTypeAPI.find(entry.getKey()), entry.getValue());
            } catch (final DotSecurityException e) {
                throw new DotDataException(e.getMessage(), e);
            }
        }
        return fileBuilder.buildZip(site.getHostname(), inodesByType, user, request);
    }

    /**
     * Resolves the site a selection was made in. A site that does not exist and one the user
     * cannot read give the same 404, so the response never tells which applied (FR-014).
     *
     * @param siteId the site identifier from the request
     * @param user   the {@link User} the export runs as
     * @return the site
     * @throws NotFoundException when the site is missing, does not exist or cannot be read
     */
    private Host browsedSite(final String siteId, final User user) {
        if (!UtilMethods.isSet(siteId)) {
            throw new NotFoundException(FOLDER_NOT_FOUND_MESSAGE);
        }
        final Host site = Try.of(() -> hostAPI.find(siteId, user, false)).getOrNull();
        if (site == null || !UtilMethods.isSet(site.getIdentifier())) {
            throw new NotFoundException(FOLDER_NOT_FOUND_MESSAGE);
        }
        return site;
    }

    /**
     * The rows a filter-mode export resolved for one content type, before anything is written.
     *
     * @param contentType the content type every row belongs to
     * @param candidates  the readable rows, in output order
     */
    record FilterResolution(ContentType contentType, ExportCandidates candidates) {
    }

    /**
     * Filter mode, first step: checks the request and resolves its readable rows (research R1–R3).
     * Nothing is written.
     *
     * @param query the listing request
     * @param user  the {@link User} the export runs as
     * @return the content type and its readable rows
     * @throws BadRequestException  when there is not exactly one readable type, it is a Form or Host
     *                              type, the search text is too complex for the index, or more
     *                              rows match than the limit
     * @throws NotFoundException    when the site or folder does not exist or cannot be read
     * @throws DotDataException     if the rows cannot be read
     * @throws DotSecurityException if the user cannot read the site or folder
     */
    FilterResolution resolveFilterMode(final DriveRequestForm query, final User user)
            throws DotDataException, DotSecurityException {
        final ContentType contentType = resolveOneContentType(query, user);

        // Every override goes before build(): build() snapshots the builder
        final BrowserQuery browserQuery = listingQueryBuilder(query, user)
                .withContentTypes(Set.of(contentType.id()))
                .showFolders(false)
                .showLinks(false)
                .showWorking(true)
                .build();
        driveHelper.checkSearchable(browserQuery);

        final int maxRows = Config.getIntProperty(MAX_ROWS_KEY, MAX_ROWS_DEFAULT);
        final ExportCandidates candidates;
        try {
            candidates = browserAPI.getContentForExport(browserQuery, maxRows);
        } catch (final DotDataException | DotRuntimeException e) {
            // Search input the index cannot build a query from is bad input, as in the listing
            if (ExceptionUtil.causedBy(e, ESQueryTooComplexException.class)) {
                throw new BadRequestException(e, ContentDriveHelper.TOO_COMPLEX_MESSAGE);
            }
            throw e;
        }
        if (candidates.overLimit()) {
            throw new BadRequestException(overLimitMessage(maxRows));
        }
        return new FilterResolution(contentType, candidates);
    }

    /**
     * Filter mode, second step: writes the resolved rows as one CSV. A row the user can no longer
     * read by now is skipped and not counted (FR-026a).
     *
     * @param resolution what {@link #resolveFilterMode} returned
     * @param user       the {@link User} the export runs as
     * @param request    the HTTP request, used for the download links of binary fields
     * @return the CSV; it reports zero rows, and holds no file, when no row was written
     * @throws DotDataException if the file cannot be written
     * @throws IOException      if the temp file cannot be written
     */
    ExportFile write(final FilterResolution resolution, final User user,
            final HttpServletRequest request) throws DotDataException, IOException {
        final List<String> inodes = resolution.candidates().rows().stream().map(ExportRow::inode)
                .collect(Collectors.toList());
        return fileBuilder.buildCsv(resolution.contentType(), inodes, user, request);
    }

    /**
     * Builds the listing query for a request. A site or folder in {@code assetPath} that does not
     * exist and one the user cannot read give the same 404, so the response never tells which
     * applied (FR-014).
     *
     * @param query the listing request
     * @param user  the {@link User} the export runs as
     * @return the builder, unbuilt
     * @throws NotFoundException when the site or folder does not exist or cannot be read
     * @throws DotDataException  if the query cannot be built for another reason
     */
    private BrowserQuery.Builder listingQueryBuilder(final DriveRequestForm query, final User user)
            throws DotDataException {
        try {
            return driveHelper.toBrowserQueryBuilder(query, user);
        } catch (final DotSecurityException | NotFoundInDbException | NotFoundException e) {
            throw new NotFoundException(FOLDER_NOT_FOUND_MESSAGE);
        }
    }

    /**
     * Resolves the one content type a filter-mode export is for. Zero or several types, a type
     * that does not exist and a type the user cannot read all give the same message, so the
     * response never tells which applied (FR-004).
     *
     * @param query the listing request
     * @param user  the {@link User} the export runs as
     * @return the content type
     * @throws BadRequestException when there is not exactly one readable type, or it is a Form or
     *                             the Host type
     * @throws DotDataException    if the content type cannot be read for another reason
     */
    private static ContentType resolveOneContentType(final DriveRequestForm query, final User user)
            throws DotDataException {
        final List<String> requested = query.contentTypes();
        if (requested == null || requested.size() != 1) {
            throw new BadRequestException(ONE_TYPE_MESSAGE);
        }
        final ContentType contentType;
        try {
            contentType = APILocator.getContentTypeAPI(user).find(requested.get(0));
        } catch (final NotFoundInDbException | DotSecurityException e) {
            throw new BadRequestException(ONE_TYPE_MESSAGE);
        }
        if (contentType == null) {
            throw new BadRequestException(ONE_TYPE_MESSAGE);
        }
        if (BaseContentType.FORM == contentType.baseType()
                || Host.HOST_VELOCITY_VAR_NAME.equals(contentType.variable())) {
            throw new BadRequestException(FORMS_AND_SITES_MESSAGE);
        }
        return contentType;
    }

    /**
     * The message for an export over the row limit (FR-010).
     *
     * @param maxRows the limit
     * @return the message, with the limit written with thousands separators
     */
    public static String overLimitMessage(final int maxRows) {
        final String limit = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US))
                .format(maxRows);
        return "This export has more than " + limit + " rows; " + limit
                + " is the limit. Narrow the filters or the selection and try again.";
    }
}
