package com.dotcms.cli.command.files;

import static com.dotcms.cli.common.FilesUtils.isDirectoryNotEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.files.PushService;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.cli.common.FilesTestHelperService;
import com.dotcms.common.WorkspaceManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import jakarta.inject.Inject;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class FilesPushCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    FilesTestHelperService filesTestHelper;

    @InjectMocks
    PushService pushService;

    @BeforeEach
    public void setupTest() throws IOException {

        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @BeforeEach
    public void setupMocks() {
        pushService = Mockito.mock(PushService.class);
        Mockito.when(pushService.traverseLocalFolders(any(), any(), any(), anyBoolean(), anyBoolean(),
                        anyBoolean(), anyBoolean()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void testPushNoWorkspace() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString());
                Assertions.assertEquals(CommandLine.ExitCode.USAGE, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method tests the behavior of pushing files with no specified path and a valid
     * workspace.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testPushNoPath() throws IOException {

        // Create a workspace in the current directory
        workspaceManager.getOrCreate(Path.of("."));

        final CommandLine commandLine = createCommand();

        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(
                    FilesCommand.NAME, FilesPush.NAME, "--dry-run"
            );
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        }
    }

    /**
     * This method tests the behavior of pushing files with no specified path and an invalid
     * workspace.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testPushNoPathInvalidWorkspace() throws IOException {

        // Cleaning up old traces of a possible workspace created in the current directory
        // by other tests
        var workspace = workspaceManager.findWorkspace(Path.of("."));
        if (workspace.isPresent()) {
            workspaceManager.destroy(workspace.get());
        }

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(
                    FilesCommand.NAME, FilesPush.NAME
            );
            Assertions.assertEquals(CommandLine.ExitCode.USAGE, status);
        }
    }

    @Test
    void testPush() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();;
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString());
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushDryRun() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "--dry-run");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushRemoveAssets() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "-ra");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushRemoveAssets2() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "--removeAssets");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushRemoveFolders() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "-rf");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushRemoveFolders2() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "--removeFolders");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushFailFast() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "-ff");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushFailFast2() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "--fail-fast");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushRetryAttemptsNoValue() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "--retry-attempts");
                Assertions.assertEquals(CommandLine.ExitCode.USAGE, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    @Test
    void testPushRetryAttempts() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(FilesCommand.NAME,
                        FilesPush.NAME, tempFolder.toAbsolutePath().toString(),
                        "--retry-attempts", "4");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given Scenario: Pull down a workspace doesn't matter if it is empty,
     * Call the push command with a path that doesn't match any files folder but exists within the workspace.
     * Expected Result: When the command is called passing a folder that exists within the workspace but its outside files it should return OK cause we default to the workspace root
     * If the command is called with a path that doesn't match any files folder in the workspace, it should return OK.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testPushNonFilesMatchingPath() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            //Pull down a workspace if empty
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path, "--workspace",
                    tempFolder.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(FilesCommand.NAME, FilesPush.NAME, "--workspace",
                    tempFolder.toAbsolutePath().toString(), "content-types");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            Assertions.assertTrue(isDirectoryNotEmpty(tempFolder));

            //But if called with a path that doesn't match any files folder in the workspace
            status = commandLine.execute(FilesCommand.NAME, FilesPush.NAME, "--workspace",
                    tempFolder.toAbsolutePath().toString(), "non-existing-folder");
            Assertions.assertEquals(ExitCode.SOFTWARE, status);

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given Scenario: Pull down a workspace including some data, then create files locally and push a file using the specific path.
     * Expected Result: When the command is called using the --dry-run-flag and passing a folder that exists and points to the directory that contains the file to be pushed,
     * The output should show the file that will be pushed. The file that will not be pushed should not be shown in the output.
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void testPushUsingSpecificPath() throws IOException {

        // Create a temporal folder for the pull
        var tempFolder = createTempFolder();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            //Let's start by creating some data to pull down
            final String testSite1 = filesTestHelper.prepareData(true);

            //Now lets pull down the data, so we have a workspace to work with
            final String path = String.format("//%s", testSite1);
            int status = commandLine.execute(FilesCommand.NAME, FilesPull.NAME, path, "--workspace",
                    tempFolder.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            //Now lets place a new file in the workspace for us to push it using a specific path
            final Path pathUsedForPush = Path.of(tempFolder.toString(), "/files/live/en-us/", testSite1,
                    "/folder1/subFolder1-1/subFolder1-1-1");

            //This is the file that will be pushed directly using a folder path
            final Path newFilePath1 = Path.of(pathUsedForPush.toString(), "textFileToPush.txt");
            Files.write(newFilePath1, "I am a text file that will be pushed directly using the folder path".getBytes());

            //In this path we will also add a new file that will not be used for the push
            final Path pathWithNewContentButNotUsedForPush = Path.of(tempFolder.toString(), "/files/live/en-us/", testSite1,
                    "/folder3/");
            //This file should remain in the workspace after pushing, it should not show in the output
            final Path newFilePath2 = Path.of(pathWithNewContentButNotUsedForPush.toString(), "excludedTextFile.txt");
            Files.write(newFilePath2, "I am a text file that will NOT be pushed directly using the folder path".getBytes());
            // Reset writer for the next command to simplify the output assertion
            writer.getBuffer().setLength(0);
            //Now we will push the file using the specific path and the --dry-run flag
            status = commandLine.execute(FilesCommand.NAME, FilesPush.NAME,
                    pathUsedForPush.toAbsolutePath().toString(), "--dry-run");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            //We should see the file that we're pushing in the output and not the one that falls outside the path
            final String string = writer.toString();
            Assertions.assertTrue(string.contains("textFileToPush.txt"));
            Assertions.assertFalse(string.contains("excludedTextFile.txt"));

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }


}
