package com.dotmarketing.portlets.contentlet.ajax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * How the legacy edit content save finds the file staged for a binary field: the file sits in the
 * user's temp folder, under a folder named after the binary column ({@code binary1},
 * {@code binary2}, ...).
 */
class ContentletAjaxStagedBinaryTest {

    private static final String USER_ID = "user-1";
    private static final String FILE_NAME = "report.pdf";

    @TempDir
    Path tmpRoot;

    private File stage(final String folder) throws IOException {
        final Path dir = Files.createDirectories(tmpRoot.resolve(USER_ID).resolve(folder));
        return Files.writeString(dir.resolve(FILE_NAME), "content").toFile();
    }

    private Optional<File> find(final String column, final String fileName) {
        return ContentletAjax.stagedBinary(tmpRoot.toFile(), USER_ID, column, fileName);
    }

    @Test
    void stagedFile_binaryColumnWithFileInTheUserFolder_isFound() throws IOException {
        // Given a temp root with <userId>/binary1/report.pdf
        final File staged = stage("binary1");

        // When the staged file is looked up for that column and name
        // Then that file is returned
        assertEquals(Optional.of(staged), find("binary1", FILE_NAME));
    }

    @Test
    void stagedFile_binaryColumnWithoutFile_isEmpty() throws IOException {
        // Given a temp root with an empty <userId>/binary1 folder
        Files.createDirectories(tmpRoot.resolve(USER_ID).resolve("binary1"));

        // Then nothing is returned
        assertFalse(find("binary1", FILE_NAME).isPresent());
    }

    @Test
    void stagedFile_columnWithSeveralPathSegments_isEmpty() throws IOException {
        // Given <userId>/binary1 and a file with the same name in the temp root
        Files.createDirectories(tmpRoot.resolve(USER_ID).resolve("binary1"));
        Files.writeString(tmpRoot.resolve(FILE_NAME), "content");

        // When the column is made of several path segments
        // Then nothing is returned: a binary column is a single name
        assertFalse(find("binary1/../..", FILE_NAME).isPresent());
    }

    @Test
    void stagedFile_columnWithoutDigits_isEmpty() throws IOException {
        // Given a file under <userId>/binary
        stage("binary");

        // Then nothing is returned: a binary column ends in its number
        assertFalse(find("binary", FILE_NAME).isPresent());
    }

    @Test
    void stagedFile_noColumn_isEmpty() throws IOException {
        // Given a file staged for binary1
        stage("binary1");

        // Then a lookup without a column returns nothing
        assertFalse(find(null, FILE_NAME).isPresent());
    }

    @Test
    void stagedFile_noFileName_isEmpty() throws IOException {
        // Given a file staged for binary1
        stage("binary1");

        // Then a lookup without a file name returns nothing
        assertFalse(find("binary1", null).isPresent());
    }

    @Test
    void stagedFile_folderWithTheFileName_isEmpty() throws IOException {
        // Given <userId>/binary1/report.pdf where report.pdf is a folder
        Files.createDirectories(tmpRoot.resolve(USER_ID).resolve("binary1").resolve(FILE_NAME));

        // Then nothing is returned: only a file can be staged
        assertFalse(find("binary1", FILE_NAME).isPresent());
    }

    @Test
    void stagedFile_nameThatIsNotABinaryColumn_isEmpty() throws IOException {
        // Given a file under <userId>/binaryNotes
        stage("binaryNotes");

        // Then nothing is returned: only binary followed by digits is a binary column
        assertFalse(find("binaryNotes", FILE_NAME).isPresent());
    }

    @Test
    void stagedFile_binaryColumnWithTwoDigits_isFound() throws IOException {
        // Given a temp root with <userId>/binary25/report.pdf
        final File staged = stage("binary25");

        // Then that file is returned
        assertEquals(Optional.of(staged), find("binary25", FILE_NAME));
    }

    @Test
    void stagedFile_fileNameWithAFolder_isEmpty() throws IOException {
        // Given a file under <userId>/binary1 and another under <userId>/binary2
        stage("binary1");
        stage("binary2");

        // When the file name carries a folder of its own
        // Then nothing is returned
        assertFalse(find("binary1", "../binary2/" + FILE_NAME).isPresent());
    }

    @Test
    void stagedFile_fileNameWithBackslashes_isEmpty() throws IOException {
        // Given a file under <userId>/binary1 whose name is made of backslash-separated segments,
        // which is a single file name where the backslash is not a separator
        final String name = "..\\binary2\\" + FILE_NAME;
        final Path dir = Files.createDirectories(tmpRoot.resolve(USER_ID).resolve("binary1"));
        try {
            Files.writeString(dir.resolve(name), "content");
        } catch (final IOException | java.nio.file.InvalidPathException e) {
            // the platform cannot hold such a file; the lookup must still return nothing
        }

        // Then nothing is returned: a file name has no separator of any platform
        assertFalse(find("binary1", name).isPresent());
    }

    @Test
    void stagedFile_parentOrCurrentFolderAsFileName_isEmpty() throws IOException {
        // Given a file staged for binary1
        stage("binary1");

        // Then the folder references are not file names
        assertFalse(find("binary1", "..").isPresent());
        assertFalse(find("binary1", ".").isPresent());
        assertFalse(find("binary1", "").isPresent());
    }

    @Test
    void stagedFile_linkToAFileOutsideTheColumnFolder_isEmpty() throws IOException {
        // Given <userId>/binary1/report.pdf that is a link to a file outside that folder
        final Path outside = Files.writeString(tmpRoot.resolve("outside.pdf"), "content");
        final Path dir = Files.createDirectories(tmpRoot.resolve(USER_ID).resolve("binary1"));
        Files.createSymbolicLink(dir.resolve(FILE_NAME), outside);

        // Then nothing is returned: the file has to really live in the column's folder
        assertFalse(find("binary1", FILE_NAME).isPresent());
    }
}
