package com.dotcms.rest.api.v1.drive.export;

import com.dotcms.content.export.ContentletCsvWriter;
import com.dotcms.content.export.HostFolderMode;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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

    /** How old a file in the export directory must be before a new export sweeps it away. */
    static final Duration LEFTOVER_AGE = Duration.ofHours(1);

    private static final String CSV_MEDIA_TYPE = "text/csv; charset=UTF-8";
    private static final String ZIP_MEDIA_TYPE = "application/zip";

    private final ContentletCsvWriter writer;
    private final Path directory;

    /**
     * Creates a builder that writes with the shared CSV writer under the asset temp path.
     */
    public ExportFileBuilder() {
        this(new ContentletCsvWriter(), defaultDirectory());
    }

    /**
     * The directory export files are written to: {@link #DIRECTORY_NAME} under the asset temp path.
     *
     * @return the export directory
     */
    public static Path defaultDirectory() {
        return Path.of(ConfigUtils.getAssetTempPath(), DIRECTORY_NAME);
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
        final Path file = newTempFile(".csv");
        try {
            final int rows;
            try (Writer out = new OutputStreamWriter(
                    new BufferedOutputStream(Files.newOutputStream(file)), StandardCharsets.UTF_8)) {
                rows = writer.write(contentType, inodes, user, request, HostFolderMode.LOCATION, out);
            }
            return finish(file, ExportFileNames.csvName(contentType.name(), contentType.variable(),
                    new Date()), CSV_MEDIA_TYPE, rows, 1);
        } catch (final DotDataException | IOException | RuntimeException e) {
            deleteQuietly(file);
            throw e;
        }
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
        final Date date = new Date();
        final Map<String, ContentType> typesById = new LinkedHashMap<>();
        inodesByType.keySet().forEach(type -> typesById.put(type.id(), type));
        final Map<String, String> entryNames = ExportFileNames.csvNames(typesById.values().stream()
                .map(type -> new ExportFileNames.TypeName(type.id(), type.name(), type.variable()))
                .collect(Collectors.toList()), date);

        final Path file = newTempFile(".zip");
        try {
            int rows = 0;
            try (ZipOutputStream zip = new ZipOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(file)), StandardCharsets.UTF_8)) {
                // One writer per entry, never closed: closing it would close the zip
                for (final Map.Entry<String, String> entry : entryNames.entrySet()) {
                    final ContentType type = typesById.get(entry.getKey());
                    zip.putNextEntry(new ZipEntry(entry.getValue()));
                    final Writer out = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
                    rows += writer.write(type, inodesByType.get(type), user, request,
                            HostFolderMode.LOCATION, out);
                    out.flush();
                    zip.closeEntry();
                }
            }
            return finish(file, ExportFileNames.zipName(hostname, date), ZIP_MEDIA_TYPE, rows,
                    entryNames.size());
        } catch (final DotDataException | IOException | RuntimeException e) {
            deleteQuietly(file);
            throw e;
        }
    }

    /**
     * Streams a finished file to the response and deletes it afterwards, whether or not sending
     * succeeded.
     *
     * @param file the file to send
     * @return the response entity
     */
    public static StreamingOutput toStreamingOutput(final ExportFile file) {
        return output -> {
            try {
                Files.copy(file.path(), output);
                output.flush();
            } finally {
                deleteQuietly(file.path());
            }
        };
    }

    /**
     * Creates the export directory if needed, sweeps leftovers older than one hour, and creates a
     * new temp file in it.
     *
     * @param suffix the file extension
     * @return the new, empty temp file
     * @throws IOException if the directory or the file cannot be created
     */
    private Path newTempFile(final String suffix) throws IOException {
        Files.createDirectories(directory);
        sweepLeftovers();
        return Files.createTempFile(directory, "export-", suffix);
    }

    /**
     * Deletes files in the export directory last modified more than {@link #LEFTOVER_AGE} ago: the
     * remains of an export whose sending never ended, for instance after a crash. A younger file may
     * be another export in progress and is kept.
     */
    private void sweepLeftovers() {
        final Instant cutoff = Instant.now().minus(LEFTOVER_AGE);
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile).filter(path -> isOlderThan(path, cutoff))
                    .forEach(ExportFileBuilder::deleteQuietly);
        } catch (final IOException e) {
            Logger.warn(ExportFileBuilder.class, String.format(
                    "Unable to sweep old export files in '%s': %s", directory, e.getMessage()));
        }
    }

    /**
     * Whether a file was last modified before a given instant.
     *
     * @param path   the file
     * @param cutoff the instant
     * @return {@code true} when it is older; {@code false} when it is younger or unreadable
     */
    private static boolean isOlderThan(final Path path, final Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (final IOException e) {
            return false;
        }
    }

    /**
     * Wraps a written temp file as the result. A file with zero rows is deleted right away, since
     * there is nothing to send.
     *
     * @param file      the temp file
     * @param fileName  the download name
     * @param mediaType the response media type
     * @param rows      the data rows written
     * @param files     the CSVs written
     * @return the export file
     */
    private static ExportFile finish(final Path file, final String fileName, final String mediaType,
            final int rows, final int files) {
        if (rows == 0) {
            deleteQuietly(file);
        }
        return new ExportFile(file, fileName, mediaType, rows, files);
    }

    /**
     * Deletes a file, logging instead of throwing when it cannot be deleted.
     *
     * @param path the file to delete
     */
    private static void deleteQuietly(final Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (final IOException e) {
            Logger.warn(ExportFileBuilder.class, String.format(
                    "Unable to delete export file '%s': %s", path, e.getMessage()));
        }
    }
}
