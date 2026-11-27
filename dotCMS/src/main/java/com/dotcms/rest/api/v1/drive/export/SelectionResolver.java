package com.dotcms.rest.api.v1.drive.export;

import com.dotcms.browser.ExportCandidates;
import com.dotcms.browser.ExportRow;
import com.dotcms.browser.ReadableRowFilter;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the rows of a selection-mode export: the selected contentlets, each in the language
 * that was selected, and the content of the selected folders (research R5, R6).
 * <p>
 * An entry that does not resolve to an exportable row is dropped silently, with no difference in
 * the response, so an export never tells whether something exists that the user cannot see.
 */
public class SelectionResolver {

    private final VersionableAPI versionableAPI;
    private final IdentifierAPI identifierAPI;
    private final ContentTypeAPI contentTypeAPI;
    private final ReadableRowFilter readableRowFilter;

    /**
     * Creates a resolver that uses the standard dotCMS APIs. Content types are read as the system
     * user: whether the user may read a row is decided per row by {@link ReadableRowFilter}.
     */
    public SelectionResolver() {
        this(APILocator.getVersionableAPI(), APILocator.getIdentifierAPI(),
                APILocator.getContentTypeAPI(APILocator.systemUser()), new ReadableRowFilter());
    }

    /**
     * Creates a resolver with the given APIs.
     *
     * @param versionableAPI    finds the working version of a contentlet in a language
     * @param identifierAPI     finds the site and type of an identifier
     * @param contentTypeAPI    finds content types, as the system user
     * @param readableRowFilter keeps the rows the user can read
     */
    SelectionResolver(final VersionableAPI versionableAPI, final IdentifierAPI identifierAPI,
            final ContentTypeAPI contentTypeAPI, final ReadableRowFilter readableRowFilter) {
        this.versionableAPI = versionableAPI;
        this.identifierAPI = identifierAPI;
        this.contentTypeAPI = contentTypeAPI;
        this.readableRowFilter = readableRowFilter;
    }

    /**
     * Resolves the readable rows of a selection.
     * <p>
     * Each selected contentlet gives the working version of its identifier in its selected
     * language. It is dropped when that version does not exist, when the identifier belongs to a
     * site other than {@code site} or System Host, or when its type is a Form or the Host type.
     * Rows are deduplicated by identifier and language, then kept only when the user can read them.
     *
     * @param form    the request; its selection is resolved
     * @param site    the site being browsed
     * @param user    the {@link User} the export runs as
     * @param maxRows the most rows the export may hold
     * @return the readable rows, one per identifier and language, in selection order, with
     *         {@link ExportCandidates#overLimit()} set when more than {@code maxRows} exist
     * @throws DotDataException if the rows cannot be read
     */
    public ExportCandidates resolve(final DriveExportRequestForm form, final Host site,
            final User user, final int maxRows) throws DotDataException {
        final Map<String, Optional<ContentType>> typesByVariable = new HashMap<>();
        final Map<String, ExportRow> candidates = new LinkedHashMap<>();
        if (form.selection() != null) {
            for (final ExportContentletRefForm ref : form.selection().contentlets()) {
                contentletRow(ref, site, typesByVariable)
                        .ifPresent(row -> candidates.putIfAbsent(row.key(), row));
            }
        }

        final List<ExportRow> readable =
                readableRowFilter.filter(new ArrayList<>(candidates.values()), user);
        if (readable.size() > maxRows) {
            return new ExportCandidates(new ArrayList<>(readable.subList(0, maxRows + 1)), true);
        }
        return new ExportCandidates(readable, false);
    }

    /**
     * The row of one selected contentlet, or empty when it must be dropped.
     *
     * @param ref             the selected identifier and language
     * @param site            the site being browsed
     * @param typesByVariable content types already looked up, by variable
     * @return the row
     * @throws DotDataException if the identifier cannot be read
     */
    private Optional<ExportRow> contentletRow(final ExportContentletRefForm ref, final Host site,
            final Map<String, Optional<ContentType>> typesByVariable) throws DotDataException {
        final Optional<ContentletVersionInfo> versionInfo =
                versionableAPI.getContentletVersionInfo(ref.identifier(), ref.languageId());
        if (versionInfo.isEmpty() || !UtilMethods.isSet(versionInfo.get().getWorkingInode())) {
            return Optional.empty();
        }
        final Identifier identifier = identifierAPI.find(ref.identifier());
        if (identifier == null || !UtilMethods.isSet(identifier.getId())) {
            return Optional.empty();
        }
        final String hostId = identifier.getHostId();
        if (!site.getIdentifier().equals(hostId) && !Host.SYSTEM_HOST.equals(hostId)) {
            return Optional.empty();
        }
        final Optional<ContentType> contentType = typesByVariable.computeIfAbsent(
                identifier.getAssetSubType(),
                variable -> Try.of(() -> contentTypeAPI.find(variable)).toJavaOptional());
        if (contentType.isEmpty() || !isExportable(contentType.get())) {
            return Optional.empty();
        }
        return Optional.of(new ExportRow(versionInfo.get().getWorkingInode(), ref.identifier(),
                ref.languageId(), contentType.get().id()));
    }

    /**
     * Whether a content type's content can be exported: never a Form or the Host type (FR-007).
     *
     * @param contentType the content type
     * @return {@code true} when its content is exported
     */
    static boolean isExportable(final ContentType contentType) {
        return BaseContentType.FORM != contentType.baseType()
                && !Host.HOST_VELOCITY_VAR_NAME.equals(contentType.variable());
    }
}
