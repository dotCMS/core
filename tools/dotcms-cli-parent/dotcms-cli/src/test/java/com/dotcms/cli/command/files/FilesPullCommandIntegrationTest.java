package com.dotcms.cli.command.files;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.command.CommandTest;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@QuarkusTest
class FilesPullCommandIntegrationTest extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @Test
    void Test_Command_Files_Pull_Option_Not_Found() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "siteDontExist");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path);
            Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, status);
        }
    }

    /**
     * This method tests the functionality of the "Files Pull" command when the specified asset is
     * not found. The expected status code is CommandLine.ExitCode.SOFTWARE, which is compared
     * against the actual status code returned by the execute method. If the status codes match, the
     * test passes.
     */
    @Test
    void Test_Command_Files_Pull_Option_Not_Found2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default/test1/image4 copy.jpg");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path);
            Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, status);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Invalid_Protocol() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path);
            Assertions.assertEquals(CommandLine.ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Valid_Protocol() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path,
                    "--workspace", tempFolder.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Valid_Protocol2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s/", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path,
                    "--workspace", tempFolder.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Preserve() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, "--workspace", tempFolder.toString(), "-p");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Preserve2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, "--workspace", tempFolder.toString(), "--preserve");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Include_Empty() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, "--workspace", tempFolder.toString(), "-ie");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Include_Empty2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, "--workspace", tempFolder.toString(),
                    "--includeEmptyFolders");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    private Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID();
        return Files.createTempDirectory(randomFolderName);
    }

    private void deleteTempDirectory(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); // Deletes the file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); // Deletes the directory after its content has been deleted
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
