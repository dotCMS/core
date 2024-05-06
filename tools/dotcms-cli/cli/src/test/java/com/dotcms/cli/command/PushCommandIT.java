package com.dotcms.cli.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.common.FullPushOptionsMixin;
import com.dotcms.cli.common.OutputOptionMixin;
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
import java.util.UUID;
import java.util.stream.Stream;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParseResult;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class PushCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

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
    FullPushOptionsMixin pushMixin;

    @Mock
    CommandLine.Model.CommandSpec commandSpec;

    @InjectMocks
    @Spy
    PushCommand pushCommand;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
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

            when(pushCommands.stream())
                    .thenReturn(Stream.of(dotPush1, dotPush2, dotPush3));

            pushCommand.pushCommands = pushCommands;

            doReturn(commandLine).
                    when(pushCommand).
                    createCommandLine(any(DotPush.class));
            when(commandSpec.commandLine()).
                    thenReturn(commandLine);

            when(commandLine.getParseResult()).
                    thenReturn(parseResult);
            when(parseResult.expandedArgs()).
                    thenReturn(new ArrayList<>());

            when(pushMixin.path()).thenReturn(tempFolder.toAbsolutePath());

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
     * This test ensures that all DotPush instances are called in the order specified by their
     * getOrder() method during the execution of the PushCommand's call() method. The test employs
     * Mockito's InOrder verification mode and specific CommandLine mocks for each DotPush instance
     * to check that the DotPush commands are processed in the correct sequence.
     *
     * @throws Exception If there is any exception during the test execution. The exceptions could
     *                   arise from file operations (like creating and deleting temp directories) or
     *                   from the execution of the command.
     */
    @Test
    void testAllPushCommandsAreCalledInOrder() throws Exception {

        // Create a temporal folder
        var tempFolder = createTempFolder();

        try {

            // And a workspace for it
            workspaceManager.getOrCreate(tempFolder);

            // Define push commands
            DotPush dotPush1 = mock(DotPush.class);
            when(dotPush1.getOrder()).thenReturn(2);

            DotPush dotPush2 = mock(DotPush.class);
            when(dotPush2.getOrder()).thenReturn(1);

            DotPush dotPush3 = mock(DotPush.class);
            when(dotPush3.getOrder()).thenReturn(4);

            DotPush dotPush4 = mock(DotPush.class);
            when(dotPush4.getOrder()).thenReturn(3);

            when(pushCommands.stream())
                    .thenReturn(Stream.of(dotPush1, dotPush2, dotPush3, dotPush4));
            pushCommand.pushCommands = pushCommands;

            // Define matching command lines for each dot push.
            CommandLine commandLine1 = mock(CommandLine.class);
            CommandLine commandLine2 = mock(CommandLine.class);
            CommandLine commandLine3 = mock(CommandLine.class);
            CommandLine commandLine4 = mock(CommandLine.class);

            doReturn(commandLine1).when(pushCommand).createCommandLine(dotPush1);
            doReturn(commandLine2).when(pushCommand).createCommandLine(dotPush2);
            doReturn(commandLine3).when(pushCommand).createCommandLine(dotPush3);
            doReturn(commandLine4).when(pushCommand).createCommandLine(dotPush4);

            when(commandSpec.commandLine()).
                    thenReturn(commandLine2, commandLine1, commandLine4, commandLine3);

            when(commandLine1.getParseResult()).thenReturn(parseResult);
            when(commandLine2.getParseResult()).thenReturn(parseResult);
            when(commandLine3.getParseResult()).thenReturn(parseResult);
            when(commandLine4.getParseResult()).thenReturn(parseResult);

            when(parseResult.expandedArgs()).thenReturn(new ArrayList<>());

            when(pushMixin.path()).thenReturn(tempFolder.toAbsolutePath());

            pushCommand.workspaceManager = workspaceManager;
            pushCommand.call();

            // Verify the calls to createCommandLine and execute were in the right order
            InOrder inOrder = inOrder(
                    pushCommand, commandLine1, commandLine2, commandLine3, commandLine4
            );
            inOrder.verify(pushCommand).createCommandLine(dotPush2);
            inOrder.verify(commandLine2).execute(any());
            inOrder.verify(pushCommand).createCommandLine(dotPush1);
            inOrder.verify(commandLine1).execute(any());
            inOrder.verify(pushCommand).createCommandLine(dotPush4);
            inOrder.verify(commandLine4).execute(any());
            inOrder.verify(pushCommand).createCommandLine(dotPush3);
            inOrder.verify(commandLine3).execute(any());

            inOrder.verifyNoMoreInteractions(); // Checks if there are no more calls after the last one checked

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

}
