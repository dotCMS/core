package com.dotcms.content.export;

import static com.dotmarketing.portlets.contentlet.util.ContentletUtil.isNewFieldTypeAllowedOnImportExport;

import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * Writes contentlets of one content type as CSV, in the format the Content Search export has always
 * produced and the content import reads back. Content Search and the Content Drive export both use
 * it, so the two write the same file.
 * <p>
 * The format: a header {@code Identifier,languageCode,countryCode} followed by the variable of every
 * exportable field; one row per contentlet version; rows end with {@code \r\n}; carriage returns
 * inside a value are removed; a value holding a comma or a line feed is quoted, with its quotes
 * doubled.
 */
public class ContentletCsvWriter {

    /** How often, in rows, progress is logged. */
    private static final int LOGGING_POINT = 2000;
    private static final boolean DONT_RESPECT_FRONTEND_ROLES = false;

    private final ContentletAPI contentletAPI;
    private final CategoryAPI categoryAPI;
    private final TagAPI tagAPI;
    private final IdentifierAPI identifierAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;
    private final LanguageAPI languageAPI;
    private final FieldAPI fieldAPI;

    /**
     * Creates a writer that uses the standard dotCMS APIs.
     */
    public ContentletCsvWriter() {
        this(APILocator.getContentletAPI(), APILocator.getCategoryAPI(), APILocator.getTagAPI(),
                APILocator.getIdentifierAPI(), APILocator.getHostAPI(), APILocator.getFolderAPI());
    }

    /**
     * Creates a writer with the given APIs. Tests use it to inject failures.
     *
     * @param contentletAPI loads each contentlet and reads its field values
     * @param categoryAPI   resolves the categories of category fields
     * @param tagAPI        reads the tags of tag fields
     * @param identifierAPI resolves the paths of images, files and page URLs
     * @param hostAPI       resolves the host of the host/folder column
     * @param folderAPI     resolves the folder of the host/folder column
     */
    ContentletCsvWriter(final ContentletAPI contentletAPI, final CategoryAPI categoryAPI,
            final TagAPI tagAPI, final IdentifierAPI identifierAPI, final HostAPI hostAPI,
            final FolderAPI folderAPI) {
        this.contentletAPI = contentletAPI;
        this.categoryAPI = categoryAPI;
        this.tagAPI = tagAPI;
        this.identifierAPI = identifierAPI;
        this.hostAPI = hostAPI;
        this.folderAPI = folderAPI;
        this.languageAPI = APILocator.getLanguageAPI();
        this.fieldAPI = APILocator.getContentTypeFieldAPI();
    }

    /**
     * Writes the header row and then one row per contentlet version, in the order given.
     * <p>
     * Each row is loaded as {@code user}. A row the user can no longer read, or that no longer
     * exists, is skipped and not counted. A field whose value cannot be formatted is written as an
     * empty cell. Any other failure stops the write and is thrown, so the caller never sends a
     * partial file. No websocket messages are sent.
     *
     * @param contentType    the {@link ContentType} every inode belongs to; it decides the columns
     * @param inodes         the contentlet versions to write, in output order
     * @param user           the {@link User} the rows are loaded as
     * @param request        the request used to build the download links of binary fields
     * @param hostFolderMode how the host/folder column is written
     * @param out            where the CSV is written; it is flushed but not closed
     * @return the number of data rows written, not counting the header
     * @throws DotDataException if the export cannot be written
     * @throws IOException      if {@code out} fails
     */
    public int write(final ContentType contentType, final List<String> inodes, final User user,
            final HttpServletRequest request, final HostFolderMode hostFolderMode, final Writer out)
            throws DotDataException, IOException {

        final List<Field> fields = fieldAPI.byContentTypeId(contentType.id()).stream()
                .filter(field -> isNewFieldTypeAllowedOnImportExport(field))
                .collect(Collectors.toList());
        final RowContext context = new RowContext(contentType, user, request, hostFolderMode);

        out.write("Identifier,languageCode,countryCode");
        for (final Field field : fields) {
            out.write("," + field.variable());
        }
        out.write("\r\n");

        int written = 0;
        int processed = 0;
        for (final String inode : inodes) {
            processed++;
            if (processed % LOGGING_POINT == 0) {
                Logger.info(this, String.format("Exporting %d out of %d contentlet(s)", processed,
                        inodes.size()));
            }
            final Optional<Contentlet> contentlet = load(inode, user);
            if (contentlet.isEmpty()) {
                continue;
            }
            writeRow(contentlet.get(), fields, context, out);
            written++;
        }
        out.flush();
        return written;
    }

    /**
     * Loads one contentlet version as the user. A version that no longer exists, or that the user
     * cannot read, comes back empty.
     *
     * @param inode the inode of the version
     * @param user  the {@link User} it is loaded as
     * @return the contentlet, or empty when it must be skipped
     */
    private Optional<Contentlet> load(final String inode, final User user) {
        try {
            return Optional.ofNullable(contentletAPI.find(inode, user, DONT_RESPECT_FRONTEND_ROLES));
        } catch (final DotSecurityException e) {
            Logger.debug(this, () -> String.format(
                    "Skipping contentlet with Inode '%s': the user cannot read it", inode));
            return Optional.empty();
        } catch (final DotDataException e) {
            Logger.warn(this, "Unable to find contentlet with Inode: " + inode);
            return Optional.empty();
        }
    }

    /**
     * Writes one data row: identifier, language and country codes, then one cell per field.
     *
     * @param content the contentlet version
     * @param fields  the exportable fields, in column order
     * @param context what every row of this write shares
     * @param out     where the row is written
     * @throws DotDataException if the categories of the contentlet cannot be read
     * @throws IOException      if {@code out} fails
     */
    private void writeRow(final Contentlet content, final List<Field> fields,
            final RowContext context, final Writer out) throws DotDataException, IOException {
        final List<Category> categories;
        try {
            categories = categoryAPI.getParents(content, context.user, DONT_RESPECT_FRONTEND_ROLES);
        } catch (final DotSecurityException e) {
            throw new DotDataException(e.getMessage(), e);
        }
        final Language language = languageAPI.getLanguage(content.getLanguageId());

        out.write(content.getIdentifier());
        out.write("," + language.getLanguageCode());
        out.write("," + language.getCountryCode());
        for (final Field field : fields) {
            String cell;
            try {
                cell = quote(formatValue(content, field, categories, context));
            } catch (final Exception e) {
                cell = ",";
                Logger.error(this, String.format("An error occurred when exporting field '%s' [%s] of"
                                + " contentlet '%s' to CSV: %s", field.name(), field.inode(),
                        content.getInode(), e.getMessage()), e);
            }
            out.write(cell);
        }
        out.write("\r\n");
    }

    /**
     * Formats one field value the way the Content Search export does.
     *
     * @param content    the contentlet version
     * @param field      the field
     * @param categories the categories the contentlet belongs to
     * @param context    what every row of this write shares
     * @return the cell text, before quoting
     * @throws Exception whatever reading or formatting the value throws; the caller writes an
     *                   empty cell
     */
    private String formatValue(final Contentlet content, final Field field,
            final List<Category> categories, final RowContext context) throws Exception {
        final User user = context.user;
        Object value = StringPool.BLANK;
        if (contentletAPI.getFieldValue(content, field) != null) {
            value = contentletAPI.getFieldValue(content, field);
        }
        String text = StringPool.BLANK;
        if (field instanceof CategoryField) {
            final Category category = categoryAPI.find(field.values(), user, DONT_RESPECT_FRONTEND_ROLES);
            final List<Category> allChildren =
                    categoryAPI.getAllChildren(category, user, DONT_RESPECT_FRONTEND_ROLES);
            if (!categories.isEmpty()
                    && categoryAPI.canUseCategory(category, user, DONT_RESPECT_FRONTEND_ROLES)) {
                for (final Category cat : categories) {
                    if (allChildren.contains(cat)) {
                        text = text + "," + (UtilMethods.isSet(cat.getKey()) ? cat.getKey()
                                : cat.getCategoryName());
                    }
                }
            }
            if (UtilMethods.isSet(text)) {
                text = text.substring(1);
            }
        } else if (field instanceof TagField) {
            final List<Tag> tags =
                    tagAPI.getTagsByInodeAndFieldVarName(content.getInode(), field.variable());
            for (final Tag tag : tags) {
                text = text.equals(StringPool.BLANK) ? tag.getTagName() : text + "," + tag.getTagName();
            }
        } else if (field instanceof RelationshipField) {
            text = loadRelationships(((ContentletRelationships) value).getRelationshipsRecords());
        } else if (field instanceof KeyValueField) {
            text = new ObjectMapper().writeValueAsString(value);
        } else if (BaseContentType.HTMLPAGE.equals(context.contentType.baseType())
                && PageContentType.PAGE_URL_FIELD_VAR.equalsIgnoreCase(field.variable())) {
            final Identifier id = identifierAPI.find(content.getIdentifier());
            if (null != id && UtilMethods.isSet(id.getId())) {
                text = id.getPath();
            }
        } else if (field instanceof ImageField || field instanceof FileField) {
            // Both field types store the identifier of a file asset; export its path
            final String fileIdentifier = value.toString();
            if (UtilMethods.isSet(fileIdentifier)) {
                final Identifier id = identifierAPI.find(fileIdentifier);
                if (null != id && UtilMethods.isSet(id.getId())) {
                    text = id.getPath();
                }
            }
        } else if (field instanceof BinaryField) {
            text = new ResourceLink.ResourceLinkBuilder()
                    .getFileLink(context.request, user, content, field.variable());
        } else if (field instanceof HostFolderField
                && context.hostFolderMode == HostFolderMode.LOCATION) {
            text = context.location(content).orElse(value.toString());
        } else if (value instanceof Date || value instanceof Timestamp) {
            if (field instanceof DateField) {
                text = new SimpleDateFormat(WebKeys.DateFormats.EXP_IMP_DATE).format(value);
            } else if (field instanceof DateTimeField) {
                text = new SimpleDateFormat(WebKeys.DateFormats.EXP_IMP_DATETIME).format(value);
            } else if (field instanceof TimeField) {
                text = new SimpleDateFormat(WebKeys.DateFormats.EXP_IMP_TIME).format(value);
            }
        } else {
            text = value.toString();
            if (text.endsWith(",")) {
                text = text.substring(0, text.length() - 1);
            }
        }
        return text;
    }

    /**
     * Turns a value into a cell, with its leading comma: carriage returns are removed, and a value
     * holding a comma or a line feed is quoted with its quotes doubled.
     *
     * @param text the formatted value
     * @return the cell as written, starting with {@code ,}
     */
    private static String quote(final String text) {
        final String clean = text.replaceAll("\r", "");
        if (clean.contains(",") || clean.contains("\n")) {
            return ",\"" + clean.replaceAll("\"", "\"\"") + "\"";
        }
        return "," + clean;
    }

    /**
     * Builds the list of related identifiers, comma separated.
     *
     * @param relationshipRecords the related records of one relationship field
     * @return the identifiers of every related contentlet
     */
    private static String loadRelationships(final List<ContentletRelationshipRecords> relationshipRecords) {
        final StringBuilder result = new StringBuilder();
        relationshipRecords.forEach(record -> result.append(String.join(StringPool.COMMA,
                record.getRecords().stream().map(Contentlet::getIdentifier)
                        .collect(Collectors.toList()))));
        return result.toString();
    }

    /**
     * What every row of one {@link #write} call shares, including the hosts and folders already
     * resolved for the host/folder column.
     */
    private final class RowContext {

        private final ContentType contentType;
        private final User user;
        private final HttpServletRequest request;
        private final HostFolderMode hostFolderMode;
        private final Map<String, Optional<Host>> hosts = new HashMap<>();
        private final Map<String, Optional<Folder>> folders = new HashMap<>();

        /**
         * Starts the context of one {@link ContentletCsvWriter#write} call, with empty host and
         * folder caches.
         *
         * @param contentType    the content type being written
         * @param user           the user the rows are loaded as
         * @param request        the request used for binary download links
         * @param hostFolderMode how the host/folder column is written
         */
        private RowContext(final ContentType contentType, final User user,
                final HttpServletRequest request, final HostFolderMode hostFolderMode) {
            this.contentType = contentType;
            this.user = user;
            this.request = request;
            this.hostFolderMode = hostFolderMode;
        }

        /**
         * The readable location of a contentlet: {@code //<hostname><folder path>} in a folder,
         * {@code <hostname>} at a site root, {@code SYSTEM_HOST} in System Host. Hosts and folders
         * are read as the system user: the column only names where the row lives.
         *
         * @param content the contentlet version
         * @return the location, or empty when its host or folder cannot be resolved
         */
        private Optional<String> location(final Contentlet content) {
            final String hostId = content.getHost();
            if (Host.SYSTEM_HOST.equals(hostId)) {
                return Optional.of(Host.SYSTEM_HOST);
            }
            final Optional<Host> host = hosts.computeIfAbsent(hostId, this::findHost);
            if (host.isEmpty()) {
                return Optional.empty();
            }
            final String folderInode = content.getFolder();
            if (!UtilMethods.isSet(folderInode) || FolderAPI.SYSTEM_FOLDER.equals(folderInode)) {
                return Optional.of(host.get().getHostname());
            }
            final Optional<Folder> folder = folders.computeIfAbsent(folderInode, this::findFolder);
            if (folder.isEmpty()) {
                return Optional.empty();
            }
            if (folder.get().isSystemFolder()) {
                return Optional.of(host.get().getHostname());
            }
            return Optional.of("//" + host.get().getHostname() + folder.get().getPath());
        }

        /**
         * Reads a host as the system user, for the location column only.
         *
         * @param hostId the host identifier
         * @return the host, or empty when it cannot be read; the failure is logged
         */
        private Optional<Host> findHost(final String hostId) {
            try {
                return Optional.ofNullable(
                        hostAPI.find(hostId, APILocator.systemUser(), DONT_RESPECT_FRONTEND_ROLES));
            } catch (final DotDataException | DotSecurityException e) {
                Logger.warn(ContentletCsvWriter.class,
                        String.format("Unable to find host '%s': %s", hostId, e.getMessage()));
                return Optional.empty();
            }
        }

        /**
         * Reads a folder as the system user, for the location column only.
         *
         * @param folderInode the folder inode
         * @return the folder, or empty when it cannot be read; the failure is logged
         */
        private Optional<Folder> findFolder(final String folderInode) {
            try {
                return Optional.ofNullable(folderAPI.find(folderInode, APILocator.systemUser(),
                        DONT_RESPECT_FRONTEND_ROLES));
            } catch (final DotDataException | DotSecurityException e) {
                Logger.warn(ContentletCsvWriter.class,
                        String.format("Unable to find folder '%s': %s", folderInode, e.getMessage()));
                return Optional.empty();
            }
        }
    }
}
