package com.dotcms.rest.api.v1.drive.export;

import com.dotcms.browser.ExportCandidates;
import com.dotcms.browser.ExportRow;
import com.dotcms.browser.ReadableRowFilter;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.rest.api.v1.asset.AssetPathResolver;
import com.dotcms.rest.api.v1.asset.ResolvedAssetAndPath;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import java.util.Collections;
import java.util.regex.Pattern;
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

    static final String INVALID_FOLDER_PATH = "Invalid folder path.";
    static final String FOLDER_NOT_FOUND = "Folder not found.";

    /** A folder path: {@code //}, a host name, then a non-empty path; a site root is not one. */
    private static final Pattern FOLDER_PATH = Pattern.compile("^//[^/\\s]+/.+$");
    /** Folder paths per content query, well under any database's parameter limit. */
    static final int FOLDER_PATH_BATCH = 500;
    /** Rows per content query. */
    static final int FOLDER_ROWS_CHUNK = 1000;

    private final VersionableAPI versionableAPI;
    private final IdentifierAPI identifierAPI;
    private final ContentTypeAPI contentTypeAPI;
    private final ReadableRowFilter readableRowFilter;
    private final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final FolderAPI folderAPI = APILocator.getFolderAPI();

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
     * Each selected folder gives every contentlet below it, at any depth, in every language,
     * archived included, skipping subfolders the user cannot read. Rows are kept only when the
     * user can read them, and appear once per identifier and language: the selected contentlets
     * first, then the folders' content.
     *
     * @param form    the request; its selection is resolved
     * @param site    the site being browsed
     * @param user    the {@link User} the export runs as
     * @param maxRows the most rows the export may hold
     * @return the readable rows, one per identifier and language, in selection order, with
     *         {@link ExportCandidates#overLimit()} set when more than {@code maxRows} exist
     * @throws BadRequestException when a folder path does not parse or names a site root
     * @throws NotFoundException   when a folder does not resolve, names a file, belongs to another
     *                             site, or cannot be read
     * @throws DotDataException    if the rows cannot be read
     */
    public ExportCandidates resolve(final DriveExportRequestForm form, final Host site,
            final User user, final int maxRows) throws DotDataException {
        final ExportSelectionForm selection = form.selection();
        // Every folder is validated before any content is read, so an error never comes with a
        // partial result
        final List<Folder> selectedFolders = selection == null ? List.of()
                : resolveFolders(selection.folders(), site, user);

        final Map<String, Optional<ContentType>> typesByVariable = new HashMap<>();
        final Map<String, ExportRow> selected = new LinkedHashMap<>();
        if (selection != null) {
            for (final ExportContentletRefForm ref : selection.contentlets()) {
                contentletRow(ref, site, typesByVariable)
                        .ifPresent(row -> selected.putIfAbsent(row.key(), row));
            }
        }

        final Map<String, ExportRow> rows = new LinkedHashMap<>();
        if (addAll(rows, readableRowFilter.filter(new ArrayList<>(selected.values()), user),
                maxRows)) {
            return overLimit(rows);
        }
        final List<String> folderPaths = readableFolderPaths(selectedFolders, user);
        for (int start = 0; start < folderPaths.size(); start += FOLDER_PATH_BATCH) {
            final List<String> batch =
                    folderPaths.subList(start, Math.min(start + FOLDER_PATH_BATCH, folderPaths.size()));
            ExportRow last = null;
            while (true) {
                final List<ExportRow> chunk = readFolderChunk(site, batch, last);
                if (chunk.isEmpty()) {
                    break;
                }
                last = chunk.get(chunk.size() - 1);
                if (addAll(rows, readableRowFilter.filter(chunk, user), maxRows)) {
                    return overLimit(rows);
                }
                if (chunk.size() < FOLDER_ROWS_CHUNK) {
                    break;
                }
            }
        }
        return new ExportCandidates(new ArrayList<>(rows.values()), false);
    }

    /**
     * Adds rows not seen yet, by identifier and language, in order.
     *
     * @param rows    the rows so far
     * @param toAdd   the rows to add
     * @param maxRows the limit
     * @return {@code true} as soon as there are more than {@code maxRows} rows
     */
    private static boolean addAll(final Map<String, ExportRow> rows, final List<ExportRow> toAdd,
            final int maxRows) {
        for (final ExportRow row : toAdd) {
            rows.putIfAbsent(row.key(), row);
            if (rows.size() > maxRows) {
                return true;
            }
        }
        return false;
    }

    private static ExportCandidates overLimit(final Map<String, ExportRow> rows) {
        return new ExportCandidates(new ArrayList<>(rows.values()), true);
    }

    /**
     * Resolves the selected folder paths, as the system user so that "missing" and "unreadable"
     * cannot be told apart, then checks READ for the user.
     *
     * @param paths the selected paths, {@code //<hostname>/<path>/}
     * @param site  the site being browsed
     * @param user  the {@link User} the export runs as
     * @return the folders, in selection order
     * @throws BadRequestException when a path does not parse or names a site root
     * @throws NotFoundException   when a path does not resolve, names a file, belongs to another
     *                             site, or the user cannot read the folder
     * @throws DotDataException    if a folder cannot be read
     */
    private List<Folder> resolveFolders(final List<String> paths, final Host site, final User user)
            throws DotDataException {
        final List<Folder> folders = new ArrayList<>();
        for (final String path : paths) {
            if (path == null || !FOLDER_PATH.matcher(path).matches()) {
                throw new BadRequestException(INVALID_FOLDER_PATH);
            }
            final ResolvedAssetAndPath resolved;
            try {
                resolved = AssetPathResolver.newInstance().resolve(path, APILocator.systemUser());
            } catch (final IllegalArgumentException e) {
                throw new BadRequestException(INVALID_FOLDER_PATH);
            } catch (final NotFoundException | NotFoundInDbException | DotSecurityException e) {
                throw new NotFoundException(FOLDER_NOT_FOUND);
            }
            final Folder folder = resolved.resolvedFolder();
            if (UtilMethods.isSet(resolved.asset()) || folder == null || folder.isSystemFolder()
                    || resolved.resolvedHost() == null
                    || !site.getIdentifier().equals(resolved.resolvedHost().getIdentifier())
                    || !permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ,
                    user, false)) {
                throw new NotFoundException(FOLDER_NOT_FOUND);
            }
            folders.add(folder);
        }
        return folders;
    }

    /**
     * The paths of every folder whose content is exported: each selected folder and every readable
     * folder below it. The folder API prunes below an unreadable folder, so content under it is
     * never read (FR-013).
     *
     * @param selectedFolders the selected folders, already readable
     * @param user            the {@link User} the export runs as
     * @return the folder paths, each once
     * @throws DotDataException if the subfolders cannot be read
     */
    private List<String> readableFolderPaths(final List<Folder> selectedFolders, final User user)
            throws DotDataException {
        final Map<String, String> pathsByInode = new LinkedHashMap<>();
        for (final Folder folder : selectedFolders) {
            pathsByInode.putIfAbsent(folder.getInode(), folder.getPath());
            final List<Folder> subFolders;
            try {
                subFolders = folderAPI.findSubFoldersRecursively(folder, user, false);
            } catch (final DotSecurityException e) {
                throw new DotDataException(e.getMessage(), e);
            }
            subFolders.forEach(sub -> pathsByInode.putIfAbsent(sub.getInode(), sub.getPath()));
        }
        return new ArrayList<>(pathsByInode.values());
    }

    /**
     * Reads the next chunk of contentlet rows directly in the given folders, in
     * {@code (identifier, language)} order: the working version in every language, archived
     * included, never a Form or the Host type.
     *
     * @param site        the site the folders belong to
     * @param folderPaths at most {@link #FOLDER_PATH_BATCH} folder paths
     * @param after       the last row of the previous chunk, or {@code null}
     * @return the rows, in order
     * @throws DotDataException if the query fails
     */
    private static List<ExportRow> readFolderChunk(final Host site, final List<String> folderPaths,
            final ExportRow after) throws DotDataException {
        final StringBuilder sql = new StringBuilder("select cvi.working_inode as inode, "
                + "cvi.identifier as identifier, cvi.lang as lang, "
                + "struc.inode as structure_inode "
                + "from identifier id "
                + "join contentlet_version_info cvi on cvi.identifier = id.id "
                + "and cvi.variant_id = '" + VariantAPI.DEFAULT_VARIANT.name() + "' "
                + "join structure struc on struc.velocity_var_name = id.asset_subtype "
                + "where id.host_inode = ? and id.asset_type = 'contentlet' "
                + "and struc.structuretype <> " + BaseContentType.FORM.getType() + " "
                + "and struc.velocity_var_name <> '" + Host.HOST_VELOCITY_VAR_NAME + "' "
                + "and id.parent_path in (")
                .append(String.join(",", Collections.nCopies(folderPaths.size(), "?")))
                .append(") ");
        if (after != null) {
            sql.append("and (cvi.identifier, cvi.lang) > (?, ?) ");
        }
        sql.append("order by cvi.identifier, cvi.lang limit ?");

        final DotConnect dotConnect = new DotConnect().setSQL(sql.toString());
        dotConnect.addParam(site.getIdentifier());
        folderPaths.forEach(dotConnect::addParam);
        if (after != null) {
            dotConnect.addParam(after.identifier());
            dotConnect.addParam(after.languageId());
        }
        dotConnect.addParam(FOLDER_ROWS_CHUNK);

        final List<ExportRow> chunk = new ArrayList<>();
        for (final Map<String, Object> row : dotConnect.loadObjectResults()) {
            chunk.add(new ExportRow((String) row.get("inode"), (String) row.get("identifier"),
                    ((Number) row.get("lang")).longValue(), (String) row.get("structure_inode")));
        }
        return chunk;
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
