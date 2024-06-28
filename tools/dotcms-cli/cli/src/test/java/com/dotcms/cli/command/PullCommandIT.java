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
import com.dotcms.cli.common.OutputOptionMixin;
import com.dotcms.cli.common.PullMixin;
import com.dotcms.cli.common.WorkspaceParams;
import com.dotcms.common.WorkspaceManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.stream.Stream;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
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

            when(pullCommands.stream())
                    .thenReturn(Stream.of(dotPull1, dotPull2, dotPull3));

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

            when(pullMixin.workspace()).thenReturn(WorkspaceParams.builder().workspacePath(tempFolder.toAbsolutePath()).build());

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
     * This test ensures that all DotPull instances are called in the order specified by their
     * getOrder() method during the execution of the PullCommand's call() method. The test employs
     * Mockito's InOrder verification mode and specific CommandLine mocks for each DotPull instance
     * to check that the DotPull commands are processed in the correct sequence.
     *
     * @throws Exception If there is any exception during the test execution. The exceptions could
     *                   arise from file operations (like creating and deleting temp directories) or
     *                   from the execution of the command.
     */
    @Test
    void testAllPullCommandsAreCalledInOrder() throws Exception {

        // Create a temporal folder
        var tempFolder = createTempFolder();

        try {

            // And a workspace for it
            workspaceManager.getOrCreate(tempFolder);

            // Define pull commands
            DotPull dotPull1 = mock(DotPull.class);
            when(dotPull1.getOrder()).thenReturn(2);

            DotPull dotPull2 = mock(DotPull.class);
            when(dotPull2.getOrder()).thenReturn(1);

            DotPull dotPull3 = mock(DotPull.class);
            when(dotPull3.getOrder()).thenReturn(4);

            DotPull dotPull4 = mock(DotPull.class);
            when(dotPull4.getOrder()).thenReturn(3);

            when(pullCommands.stream())
                    .thenReturn(Stream.of(dotPull1, dotPull2, dotPull3, dotPull4));
            pullCommand.pullCommands = pullCommands;

            // Define matching command lines for each dot pull.
            CommandLine commandLine1 = mock(CommandLine.class);
            CommandLine commandLine2 = mock(CommandLine.class);
            CommandLine commandLine3 = mock(CommandLine.class);
            CommandLine commandLine4 = mock(CommandLine.class);

            doReturn(commandLine1).when(pullCommand).createCommandLine(dotPull1);
            doReturn(commandLine2).when(pullCommand).createCommandLine(dotPull2);
            doReturn(commandLine3).when(pullCommand).createCommandLine(dotPull3);
            doReturn(commandLine4).when(pullCommand).createCommandLine(dotPull4);

            when(commandSpec.commandLine()).
                    thenReturn(commandLine2, commandLine1, commandLine4, commandLine3);

            when(commandLine1.getParseResult()).thenReturn(parseResult);
            when(commandLine2.getParseResult()).thenReturn(parseResult);
            when(commandLine3.getParseResult()).thenReturn(parseResult);
            when(commandLine4.getParseResult()).thenReturn(parseResult);

            when(parseResult.expandedArgs()).thenReturn(new ArrayList<>());

            when(pullMixin.workspace()).thenReturn(WorkspaceParams.builder().workspacePath(tempFolder.toAbsolutePath()).build());

            pullCommand.call();

            // Verify the calls to createCommandLine and execute were in the right order
            InOrder inOrder = inOrder(
                    pullCommand, commandLine1, commandLine2, commandLine3, commandLine4
            );
            inOrder.verify(pullCommand).createCommandLine(dotPull2);
            inOrder.verify(commandLine2).execute(any());
            inOrder.verify(pullCommand).createCommandLine(dotPull1);
            inOrder.verify(commandLine1).execute(any());
            inOrder.verify(pullCommand).createCommandLine(dotPull4);
            inOrder.verify(commandLine4).execute(any());
            inOrder.verify(pullCommand).createCommandLine(dotPull3);
            inOrder.verify(commandLine3).execute(any());

            inOrder.verifyNoMoreInteractions(); // Checks if there are no more calls after the last one checked

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

}
