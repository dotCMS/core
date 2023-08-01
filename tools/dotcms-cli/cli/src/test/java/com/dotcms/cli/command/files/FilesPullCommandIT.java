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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@QuarkusTest
public class FilesPullCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @BeforeAll
    public static void beforeAll() {
        disableAnsi();
    }

    @AfterAll
    public static void afterAll() {
        enableAnsi();
    }

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @Test
    void Test_Command_Files_Pull_Option_Not_Found() {

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "siteDontExist");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path);
            Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, status);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Invalid_Protocol() {

        final CommandLine commandLine = getFactory().create();
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

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path, tempFolder.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Valid_Protocol2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s/", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path, tempFolder.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Recursive() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "-r");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Recursive2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "--recursive");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Recursive3() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "-r", "false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Recursive4() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "--recursive", "false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Override() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "-o");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Override2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "--override");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Override3() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "-o", "false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Override4() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "--override", "true");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Include_Empty() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "-ie");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Include_Empty2() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "--includeEmptyFolders");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Include_Empty3() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "-ie", "false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void Test_Command_Files_Pull_Option_Include_Empty4() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesPull.NAME, path, tempFolder.toString(), "--includeEmptyFolders", "true");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    private Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID().toString();
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
