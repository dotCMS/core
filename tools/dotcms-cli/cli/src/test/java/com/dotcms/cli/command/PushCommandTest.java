package com.dotcms.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PushMixin;
import com.dotcms.common.WorkspaceManager;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParseResult;

@QuarkusTest
class PushCommandTest extends CommandTest {

    @Mock
    Instance<DotPush> pushCommands;

    @Inject
    WorkspaceManager workspaceManager;

    @Mock
    CommandLine commandLine;

    @Mock
    ParseResult parseResult;

    @Mock
    OutputOptionMixin outputOptionMixin;

    @Mock
    PushMixin pushMixin;

    @Mock
    CommandLine.Model.CommandSpec commandSpec;

    @InjectMocks
    @Spy
    PushCommand pushCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * This test checks for a simple push situation where everything should work as expected.
     *
     * @throws IOException if there's a problem accessing the files and folders needed for the
     *                     test.
     */
    @Test
    void testSimplePush() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(PushCommand.NAME,
                        tempFolder.toAbsolutePath().toString(), "--dry-run");
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This test checks for a situation where an incorrect option is passed to the push command.
     *
     * @throws IOException if there's a problem accessing the files and folders needed for the
     *                     test.
     */
    @Test
    void testIncorrectOption() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(PushCommand.NAME,
                        tempFolder.toAbsolutePath().toString(), "--does-not-exist");
                Assertions.assertEquals(ExitCode.USAGE, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This test checks for a situation where an invalid workspace is provided for the push
     * command.
     *
     * @throws IOException if there's a problem accessing the files and folders needed for the
     *                     test.
     */
    @Test
    void testInvalidWorkspace() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(PushCommand.NAME,
                        tempFolder.toAbsolutePath().toString(), "--dry-run");
                Assertions.assertEquals(ExitCode.USAGE, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This test checks that all push commands are called.
     *
     * @throws Exception if there's a problem accessing the files and folders needed for the test.
     */
    @Test
    void testAllPushCommandsAreCalled() throws Exception {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {
            DotPush dotPush1 = mock(DotPush.class);
            DotPush dotPush2 = mock(DotPush.class);
            DotPush dotPush3 = mock(DotPush.class);

            when(pushCommands.iterator()).
                    thenReturn(Arrays.asList(dotPush1, dotPush2, dotPush3).iterator());
            doReturn(commandLine).
                    when(pushCommand).
                    createCommandLine(any(DotPush.class));
            when(commandSpec.commandLine()).
                    thenReturn(commandLine);

            when(commandLine.getParseResult()).
                    thenReturn(parseResult);
            when(parseResult.expandedArgs()).
                    thenReturn(new ArrayList<>());

            when(pushMixin.path()).thenReturn(tempFolder.toFile());

            pushCommand.workspaceManager = workspaceManager;
            pushCommand.call();

            verify(pushCommand).createCommandLine(dotPush1);
            verify(pushCommand).createCommandLine(dotPush2);
            verify(pushCommand).createCommandLine(dotPush3);
            // Make sure we executed all the push subcommands
            verify(commandLine, times(3)).execute(any());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This helper method is used to create a temporary folder for the test.
     *
     * @return a {@link Path} object representing a temporary directory for the test.
     * @throws IOException if there's a problem in creating the temporary directory.
     */
    private Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID();
        return Files.createTempDirectory(randomFolderName);
    }

    /**
     * This helper method is used to delete a directory and all its contents.
     *
     * @param folderPath the {@link Path} object of the directory to delete.
     * @throws IOException if there's a problem in deleting the directory or its contents.
     */
    private void deleteTempDirectory(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file); // Deletes the file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir); // Deletes the directory after its content has been deleted
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
