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
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

    @Inject
    Logger logger;

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
     * Given scenario: A push command is executed using a relative path.
     * Expected result: The command should find the folder to push and complete successfully with an exit code 0 (OK).
     * @throws IOException
     */
    @Test
    void testPushWithRelativePath() throws IOException {

        // Create a temporal folder for the push
        var tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        // Get the relative path of the temporal folder
        var relativePath = Path.of("").toAbsolutePath().relativize(tempFolder.toAbsolutePath());

        try {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(PushCommand.NAME,
                        relativePath.toString(), "--dry-run");
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

    /**
     * Given scenario: A push command is executed in watch mode. Then we simulate changes in the file system using a separate thread
     * Expected Result: The command that starts in watch mode remains suspended until changes are made. Then it should process the changes and exit.
     * We simply verify that the command reports it
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    void testSimplePushInWatchMode() throws IOException, InterruptedException {

        // Create a temporary folder for the push
        Path tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);

        try {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                // Latch to signal when the changes are done
                CountDownLatch changeLatch = new CountDownLatch(1);
                // Latch to wait for command processing time
                CountDownLatch commandLatch = new CountDownLatch(1);
                // Latch to signal that the command has started
                CountDownLatch commandStartLatch = new CountDownLatch(1);
                logger.debug("Starting command thread. Command will remain suspended until changes are made in a separate thread");
                // Start the command execution in a new thread
                Thread commandThread = new Thread(() -> {
                    commandLine.setOut(out);
                    commandLine.setErr(out);
                    try {
                        commandStartLatch.countDown(); // Signal that the command has started
                        commandLine.execute(PushCommand.NAME,
                                tempFolder.toAbsolutePath().toString(), "--watch", "1");
                    } catch (Exception e) {
                        // Quietly ignore exceptions
                    } finally {
                        commandLatch.countDown();
                    }
                });
                commandThread.start();

                // Wait for the command to start
                commandStartLatch.await();

                // Scheduled executor for introducing delay
                final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

                    // Simulate changes in the tempFolder with a delay
                    logger.debug(
                            "Starting change task. This will create a new file in the temp folder (workspace) and feed it with content every second. during: a 5 seconds total time");
                    Runnable changeTask = () -> {
                        try {
                            final Path newFile = tempFolder.resolve("newFile.txt");
                            Files.createFile(newFile);
                            for (int i = 0; i < 5; i++) {
                                // Create a new file
                                Files.writeString(newFile, "Hello, world! " + i + "\n");
                                // Use a latch to control timing
                                CountDownLatch innerLatch = new CountDownLatch(1);
                                logger.debug(" File updated now will be waiting for 1 second");
                                scheduler.schedule(innerLatch::countDown, 1, TimeUnit.SECONDS);
                                innerLatch.await();
                            }
                        } catch (IOException | InterruptedException e) {
                            // Quietly ignore exceptions
                        } finally {
                            changeLatch.countDown();
                        }
                    };

                    // Schedule the change task to run immediately
                    scheduler.execute(changeTask);

                    // Wait for changes to be made
                    changeLatch.await();

                    // Allow some time for the command to process the changes
                    commandLatch.await(10, TimeUnit.SECONDS);

                    // Interrupt the command thread to simulate sending a signal like CTRL-C
                    logger.debug(
                            "Interrupting command thread. This will simulate a signal like CTRL-C");
                    commandThread.interrupt();
                    commandThread.join();
                    logger.debug("Shutting down scheduler");
                    // Terminate the scheduler
                    scheduler.shutdown();
                    scheduler.awaitTermination(10, TimeUnit.SECONDS);

                    logger.debug("Running assertions");
                    // Validate the output of the command
                    final String output = writer.toString();
                    Assertions.assertTrue(output.contains("No changes in Languages to push"));
                    Assertions.assertTrue(output.contains("No changes in Sites to push"));
                    Assertions.assertTrue(output.contains("No changes in ContentTypes to push"));
                    Assertions.assertTrue(output.contains(" No changes in Files to push"));

            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }


    /**
     * Given scenario: A push command is executed in watch mode. Then we simulate changes in the file system using a separate thread
     * The path to the workspace is a relative path e.g.  files/live/en-us/default
     * We're trying to simulate a case on which we're running the command from a valid existing workspace
     * But passing a relative path to the files folder we want to push
     * Expected Result: The command that starts in watch mode remains suspended until changes are made. Then it should process the changes and exit.
     * It should be able to resolve any relative path and succeed at pushing the changes. No exceptions should be thrown
     * we should be able to deal with relative paths
     * examples
     *    -  push files/demo.com/en-us/live/folder-to-watch --watch
     *    -  push ./files/demo.com/en-us/live/folder-to-watch --watch
     *
     * @throws IOException if there's a problem accessing the files and folders needed for the test.
     * @throws InterruptedException if there's a problem with the thread synchronization.
     */
    @Test
    void testPullThenPushUsingRelativePathInWatchMode() throws IOException, InterruptedException {
        // Create a temporary folder for the push
        Path tempFolder = createTempFolder();
        // And a workspace for it
        workspaceManager.getOrCreate(tempFolder);
        try {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                //Let's seed the workspace with some content
                int status = commandLine.execute(PullCommand.NAME,
                        "--workspace", tempFolder.toAbsolutePath().toString());

                Assertions.assertEquals(ExitCode.OK, status);

                Assertions.assertTrue(tempFolder.toFile().isDirectory());
                Assertions.assertTrue(tempFolder.resolve("content-types").toFile().isDirectory());
                Assertions.assertTrue(tempFolder.resolve("languages").toFile().isDirectory());
                Assertions.assertTrue(tempFolder.resolve("sites").toFile().isDirectory());
                Assertions.assertTrue(tempFolder.resolve("files").toFile().isDirectory());
                Assertions.assertTrue(tempFolder.resolve(Path.of("files","live")).toFile().isDirectory());
                Assertions.assertTrue(tempFolder.resolve(Path.of("files","working")).toFile().isDirectory());

                //Now let's create a relative path to the workspace
                final Path folderToWatchPath = Path.of("files", "live", "en-us", "default", "folder-to-watch");
                final Path resolvedFolderToWatchPath = tempFolder.resolve(folderToWatchPath);
                //Create the directory we're going to watch
                Files.createDirectories(resolvedFolderToWatchPath);

                // Latch to signal when the changes are done
                CountDownLatch changeLatch = new CountDownLatch(1);
                // Latch to wait for command processing time
                CountDownLatch commandLatch = new CountDownLatch(1);
                // Latch to signal that the command has started
                CountDownLatch commandStartLatch = new CountDownLatch(1);
                logger.debug("Starting command thread. Command will remain suspended until changes are made in a separate thread");
                // Start the command execution in a new thread This time using a relative path
                Thread commandThread = new Thread(() -> {
                    commandLine.setOut(out);
                    commandLine.setErr(out);
                    try {
                        commandStartLatch.countDown(); // Signal that the command has started
                        commandLine.execute(PushCommand.NAME,
                                // Path to the files folder
                                resolvedFolderToWatchPath.toString(),
                                "--watch", "1");
                    } catch (Exception e) {
                        // Quietly ignore exceptions
                    } finally {
                        commandLatch.countDown();
                    }
                });
                commandThread.start();

                // Wait for the command to start
                commandStartLatch.await();

                //Now Let's introduce a change in the workspace
                final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

                // Simulate changes in the tempFolder with a delay
                Runnable changeTask = () -> {
                    try {
                        final Path newFile = resolvedFolderToWatchPath.resolve("newFile.txt");
                        Files.createFile(newFile);
                        for (int i = 0; i < 5; i++) {
                            // Create a new file
                            Files.writeString(newFile, "Hello, world! " + i + "\n");
                            // Use a latch to control timing
                            CountDownLatch innerLatch = new CountDownLatch(1);
                            logger.debug(" File updated now will be waiting for 1 second");
                            scheduler.schedule(innerLatch::countDown, 1, TimeUnit.SECONDS);
                            innerLatch.await();
                        }
                    } catch (IOException | InterruptedException e) {
                        // Quietly ignore exceptions
                    } finally {
                        changeLatch.countDown();
                    }
                };

                // Schedule the change task to run immediately
                scheduler.execute(changeTask);

                // Wait for changes to be made
                changeLatch.await();

                // Allow some time for the command to process the changes
                commandLatch.await(10, TimeUnit.SECONDS);
                logger.debug("Running assertions");
                // Validate the output of the command
                final String output = writer.toString();
                Assertions.assertTrue(output.contains("Running in Watch Mode on"));
                Assertions.assertTrue(output.contains("No changes in Languages to push"));
                Assertions.assertTrue(output.contains("No changes in Sites to push"));
                Assertions.assertTrue(output.contains("No changes in ContentTypes to push"));
                //Asser no exceptions were thrown
                Assertions.assertFalse(output.contains("Exception"));
                Assertions.assertFalse(output.contains("Unable to access the path"));
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }


}
