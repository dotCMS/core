package com.dotcms.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PullMixin;
import com.dotcms.common.WorkspaceManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
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
@TestProfile(DotCMSITProfile.class)
class PullCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Mock
    Instance<DotPull> pullCommands;

    @Inject
    WorkspaceManager workspaceManager;

    @Mock
    CommandLine commandLine;

    @Mock
    ParseResult parseResult;

    @Mock
    OutputOptionMixin outputOptionMixin;

    @Mock
    PullMixin pullMixin;

    @Mock
    CommandLine.Model.CommandSpec commandSpec;

    @InjectMocks
    @Spy
    PullCommand pullCommand;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    /**
     * This test checks for a simple pull situation where everything should work as expected.
     */
    @Test
    void testSimplePull() throws IOException {

        // Create a temporal folder
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(PullCommand.NAME,
                        "--workspace", tempFolder.toAbsolutePath().toString(), "--short");
                Assertions.assertEquals(ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This test checks for a situation where an incorrect option is passed to the pull command.
     */
    @Test
    void testIncorrectOption() throws IOException {

        // Create a temporal folder
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(PullCommand.NAME,
                        "--workspace", tempFolder.toAbsolutePath().toString(), "--does-not-exist");
                Assertions.assertEquals(ExitCode.USAGE, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This test checks that all pull commands are called.
     */
    @Test
    void testAllPullCommandsAreCalled() throws Exception {

        // Create a temporal folder
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {
            DotPull dotPull1 = mock(DotPull.class);
            DotPull dotPull2 = mock(DotPull.class);
            DotPull dotPull3 = mock(DotPull.class);

            when(pullCommands.iterator()).
                    thenReturn(Arrays.asList(dotPull1, dotPull2, dotPull3).iterator());

            pullCommand.pullCommands = pullCommands;

            doReturn(commandLine).
                    when(pullCommand).
                    createCommandLine(any(DotPull.class));
            when(commandSpec.commandLine()).
                    thenReturn(commandLine);

            when(commandLine.getParseResult()).
                    thenReturn(parseResult);
            when(parseResult.expandedArgs()).
                    thenReturn(new ArrayList<>());

            when(pullMixin.workspace()).thenReturn(tempFolder.toAbsolutePath());

            pullCommand.call();

            verify(pullCommand).createCommandLine(dotPull1);
            verify(pullCommand).createCommandLine(dotPull2);
            verify(pullCommand).createCommandLine(dotPull3);
            // Make sure we executed all the pull subcommands
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
