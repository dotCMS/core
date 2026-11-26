package com.dotcms.rest.api.v1.drive.export;

import com.dotcms.content.export.ContentletCsvWriter;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.StreamingOutput;

/**
 * Builds a Content Drive export into a temp file before anything is sent, so a failure halfway is
 * reported as an error instead of a truncated download. The file is deleted when sending ends,
 * when building fails, and by a sweep of leftovers older than one hour at the start of each export.
 */
public class ExportFileBuilder {

    /** The directory, under the asset temp path, that export files are written to. */
    public static final String DIRECTORY_NAME = "drive-export";

    private final ContentletCsvWriter writer;
    private final Path directory;

    /**
     * Creates a builder that writes with the shared CSV writer under the asset temp path.
     */
    public ExportFileBuilder() {
        this(new ContentletCsvWriter(), Path.of(ConfigUtils.getAssetTempPath(), DIRECTORY_NAME));
    }

    /**
     * Creates a builder with the given writer and directory. Tests use it to inject failures.
     *
     * @param writer    writes each content type's CSV
     * @param directory where the temp files are written
     */
    ExportFileBuilder(final ContentletCsvWriter writer, final Path directory) {
        this.writer = writer;
        this.directory = directory;
    }

    /**
     * Writes one content type's rows as a single CSV.
     *
     * @param contentType the {@link ContentType} of every inode
     * @param inodes      the rows, in output order
     * @param user        the {@link User} the rows are loaded as
     * @param request     the request used for binary links
     * @return the finished file; it holds zero rows when every row became unreadable while writing,
     *         and is then already deleted
     * @throws DotDataException if the export cannot be written; the temp file is deleted first
     * @throws IOException      if the temp file cannot be written; it is deleted first
     */
    public ExportFile buildCsv(final ContentType contentType, final List<String> inodes,
            final User user, final HttpServletRequest request) throws DotDataException, IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Writes a zip with one CSV per content type, one content type at a time.
     *
     * @param hostname      the site being browsed, which the zip is named after
     * @param inodesByType  the rows of each {@link ContentType}, in output order
     * @param user          the {@link User} the rows are loaded as
     * @param request       the request used for binary links
     * @return the finished file; it holds zero rows when every row became unreadable while writing,
     *         and is then already deleted
     * @throws DotDataException if the export cannot be written; the temp file is deleted first
     * @throws IOException      if the temp file cannot be written; it is deleted first
     */
    public ExportFile buildZip(final String hostname,
            final Map<ContentType, List<String>> inodesByType, final User user,
            final HttpServletRequest request) throws DotDataException, IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Streams a finished file to the response and deletes it afterwards, whether or not sending
     * succeeded.
     *
     * @param file the file to send
     * @return the response entity
     */
    public static StreamingOutput toStreamingOutput(final ExportFile file) {
        throw new UnsupportedOperationException("not implemented");
    }
}
