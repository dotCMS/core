package com.dotcms.rest.api.v1.drive.export;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.export.ContentletCsvWriter;
import com.dotcms.content.export.HostFolderMode;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.portal.model.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.StreamingOutput;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ExportFileBuilder}: the export is written to a temp file before
 * anything is sent, and no temp file is left behind, whatever happens (research R11, FR-031,
 * FR-032, SC-008).
 * <p>
 * The CSV content itself is {@link ContentletCsvWriter}'s job and is covered by its own test; here
 * a fake writer writes known text, so each case checks only what the builder adds.
 */
public class ExportFileBuilderTest extends IntegrationTestBase {

    private static User user;
    private static HttpServletRequest request;
    private static ContentType blog;
    private static ContentType news;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = APILocator.systemUser();
        request = mock(HttpServletRequest.class);

        final String unique = String.valueOf(System.currentTimeMillis());
        blog = new ContentTypeDataGen().name("Blog " + unique).velocityVarName("blogExport" + unique)
                .field(new FieldDataGen().type(TextField.class).name("title").velocityVarName("title").next())
                .nextPersisted();
        news = new ContentTypeDataGen().name("News " + unique).velocityVarName("newsExport" + unique)
                .field(new FieldDataGen().type(TextField.class).name("title").velocityVarName("title").next())
                .nextPersisted();
    }

    /**
     * Writes {@code <type variable> header} and one line per inode, and reports one row per inode
     * unless told otherwise.
     */
    private static class FakeWriter extends ContentletCsvWriter {

        private final int reportedRows;
        private final boolean fail;

        FakeWriter() {
            this(-1, false);
        }

        FakeWriter(final int reportedRows, final boolean fail) {
            this.reportedRows = reportedRows;
            this.fail = fail;
        }

        @Override
        public int write(final ContentType contentType, final List<String> inodes, final User user,
                final HttpServletRequest request, final HostFolderMode hostFolderMode,
                final Writer out) throws DotDataException, IOException {
            out.write(contentType.variable() + " header\r\n");
            if (fail) {
                throw new DotDataException("writer failed halfway");
            }
            for (final String inode : inodes) {
                out.write(inode + "\r\n");
            }
            return reportedRows >= 0 ? reportedRows : inodes.size();
        }
    }

    private static List<Path> filesIn(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Method to test: {@link ExportFileBuilder#buildCsv}
     * Given Scenario: One content type with two rows, built in the real export directory
     * ExpectedResult: A temp file under {@code <assetTempPath>/drive-export/} holding the writer's
     * output, named as Content Search names it, with 2 rows and 1 file
     */
    @Test
    public void test_buildCsv_writesTheFileInTheExportDirectory() throws Exception {
        final Path exportDirectory =
                Path.of(ConfigUtils.getAssetTempPath(), ExportFileBuilder.DIRECTORY_NAME);
        final ExportFile file = new ExportFileBuilder(new FakeWriter(), exportDirectory)
                .buildCsv(blog, List.of("inode-1", "inode-2"), user, request);
        try {
            assertEquals(exportDirectory.toAbsolutePath().normalize(),
                    file.path().getParent().toAbsolutePath().normalize());
            assertEquals(blog.variable() + " header\r\ninode-1\r\ninode-2\r\n",
                    Files.readString(file.path(), StandardCharsets.UTF_8));
            assertEquals(ExportFileNames.csvName(blog.name(), blog.variable(), new Date()),
                    file.fileName());
            assertEquals("text/csv; charset=UTF-8", file.mediaType());
            assertEquals(2, file.contentlets());
            assertEquals(1, file.files());
        } finally {
            Files.deleteIfExists(file.path());
        }
    }

    /**
     * Method to test: {@link ExportFileBuilder#buildZip}
     * Given Scenario: Two content types, two rows and one row
     * ExpectedResult: A zip named after the site with one entry per content type, named by
     * {@link ExportFileNames}, holding each type's CSV; 3 rows and 2 files
     */
    @Test
    public void test_buildZip_writesOneEntryPerContentType() throws Exception {
        final Path directory = Files.createTempDirectory("drive-export-zip");
        final Map<ContentType, List<String>> rows = new LinkedHashMap<>();
        rows.put(news, List.of("news-1"));
        rows.put(blog, List.of("blog-1", "blog-2"));

        final ExportFile file = new ExportFileBuilder(new FakeWriter(), directory)
                .buildZip("demo.dotcms.com", rows, user, request);
        try {
            assertEquals(ExportFileNames.zipName("demo.dotcms.com", new Date()), file.fileName());
            assertEquals("application/zip", file.mediaType());
            assertEquals(3, file.contentlets());
            assertEquals(2, file.files());

            final Map<String, String> expectedNames = ExportFileNames.csvNames(List.of(
                    new ExportFileNames.TypeName(news.id(), news.name(), news.variable()),
                    new ExportFileNames.TypeName(blog.id(), blog.name(), blog.variable())), new Date());
            final Map<String, String> entries = new LinkedHashMap<>();
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file.path()))) {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            assertEquals(new ArrayList<>(expectedNames.values()), new ArrayList<>(entries.keySet()));
            assertEquals(blog.variable() + " header\r\nblog-1\r\nblog-2\r\n",
                    entries.get(expectedNames.get(blog.id())));
            assertEquals(news.variable() + " header\r\nnews-1\r\n",
                    entries.get(expectedNames.get(news.id())));
        } finally {
            Files.deleteIfExists(file.path());
        }
    }

    /**
     * Method to test: {@link ExportFileBuilder#buildCsv} and {@link ExportFileBuilder#buildZip}
     * Given Scenario: The writer fails halfway
     * ExpectedResult: The failure propagates and no temp file is left in the directory
     */
    @Test
    public void test_writerFailure_deletesTheTempFile() throws Exception {
        final Path directory = Files.createTempDirectory("drive-export-fail");
        final ExportFileBuilder builder = new ExportFileBuilder(new FakeWriter(-1, true), directory);

        assertThrows(DotDataException.class,
                () -> builder.buildCsv(blog, List.of("inode-1"), user, request));
        assertTrue(filesIn(directory).isEmpty());

        assertThrows(DotDataException.class,
                () -> builder.buildZip("demo.dotcms.com", Map.of(blog, List.of("inode-1")), user,
                        request));
        assertTrue(filesIn(directory).isEmpty());
    }

    /**
     * Method to test: {@link ExportFileBuilder#toStreamingOutput(ExportFile)}
     * Given Scenario: A finished file streamed to a response that accepts it, and another streamed
     *                 to a response whose stream fails
     * ExpectedResult: The bytes sent are the file's; the file is deleted in both cases
     */
    @Test
    public void test_streamingOutput_deletesTheFileAfterSending() throws Exception {
        final Path directory = Files.createTempDirectory("drive-export-stream");
        final ExportFileBuilder builder = new ExportFileBuilder(new FakeWriter(), directory);

        final ExportFile sent = builder.buildCsv(blog, List.of("inode-1"), user, request);
        final byte[] expected = Files.readAllBytes(sent.path());
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        ExportFileBuilder.toStreamingOutput(sent).write(received);
        assertArrayEquals(expected, received.toByteArray());
        assertFalse(Files.exists(sent.path()));

        final ExportFile failing = builder.buildCsv(blog, List.of("inode-1"), user, request);
        final StreamingOutput output = ExportFileBuilder.toStreamingOutput(failing);
        final OutputStream broken = new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                throw new IOException("client went away");
            }
        };
        assertThrows(IOException.class, () -> output.write(broken));
        assertFalse(Files.exists(failing.path()));
    }

    /**
     * Method to test: {@link ExportFileBuilder#buildCsv}
     * Given Scenario: The directory holds a leftover file older than one hour and a recent one
     * ExpectedResult: Starting an export deletes the old leftover and keeps the recent file
     */
    @Test
    public void test_startingAnExport_sweepsLeftoversOlderThanOneHour() throws Exception {
        final Path directory = Files.createTempDirectory("drive-export-sweep");
        final Path old = Files.writeString(directory.resolve("old-export.csv"), "old");
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(2, ChronoUnit.HOURS)));
        final Path recent = Files.writeString(directory.resolve("recent-export.csv"), "recent");

        final ExportFile file = new ExportFileBuilder(new FakeWriter(), directory)
                .buildCsv(blog, List.of("inode-1"), user, request);
        try {
            assertFalse("the leftover older than one hour is swept", Files.exists(old));
            assertTrue("a recent file, possibly another export in progress, is kept",
                    Files.exists(recent));
        } finally {
            Files.deleteIfExists(file.path());
            Files.deleteIfExists(recent);
        }
    }

    /**
     * Method to test: {@link ExportFileBuilder#buildCsv} and {@link ExportFileBuilder#buildZip}
     * Given Scenario: Every row became unreadable while writing, so the writer reports zero rows
     * ExpectedResult: The result reports zero rows and the temp file is already gone
     */
    @Test
    public void test_zeroRowsWritten_deletesTheFile() throws Exception {
        final Path directory = Files.createTempDirectory("drive-export-empty");
        final ExportFileBuilder builder = new ExportFileBuilder(new FakeWriter(0, false), directory);

        final ExportFile csv = builder.buildCsv(blog, List.of("inode-1"), user, request);
        assertEquals(0, csv.contentlets());
        assertFalse(Files.exists(csv.path()));

        final ExportFile zip = builder.buildZip("demo.dotcms.com", Map.of(blog, List.of("inode-1")),
                user, request);
        assertEquals(0, zip.contentlets());
        assertFalse(Files.exists(zip.path()));
        assertTrue(filesIn(directory).isEmpty());
    }
}
