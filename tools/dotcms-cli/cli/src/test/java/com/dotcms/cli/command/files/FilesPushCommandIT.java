package com.dotcms.cli.command.files;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.client.files.PushService;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.common.WorkspaceManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import picocli.CommandLine;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class FilesPushCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    WorkspaceManager workspaceManager;

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
}
