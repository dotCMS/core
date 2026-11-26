package com.dotcms.content.export;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.tag.business.TagAPI;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * Writes contentlets of one content type as CSV, in the format the Content Search export has always
 * produced and the content import reads back. Content Search and the Content Drive export both use
 * it, so the two write the same file.
 */
public class ContentletCsvWriter {

    private final ContentletAPI contentletAPI;
    private final CategoryAPI categoryAPI;
    private final TagAPI tagAPI;
    private final IdentifierAPI identifierAPI;
    private final HostAPI hostAPI;
    private final FolderAPI folderAPI;

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
     * @param out            where the CSV is written; it is not closed
     * @return the number of data rows written, not counting the header
     * @throws DotDataException if the export cannot be written
     * @throws IOException      if {@code out} fails
     */
    public int write(final ContentType contentType, final List<String> inodes, final User user,
            final HttpServletRequest request, final HostFolderMode hostFolderMode, final Writer out)
            throws DotDataException, IOException {
        throw new UnsupportedOperationException("not implemented");
    }
}
