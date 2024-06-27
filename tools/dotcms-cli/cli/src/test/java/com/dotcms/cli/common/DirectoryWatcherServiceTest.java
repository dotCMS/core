package com.dotcms.cli.common;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for the DirectoryWatcherService class.
 */
@QuarkusTest
class DirectoryWatcherServiceTest {

    @Inject
    DirectoryWatcherService directoryWatcherService;

    private Path testDir;

    /**
     * Set up a temporary directory for testing before each test method.
     *
     * @throws IOException if an I/O error occurs
     */
    @BeforeEach
    public void setUp() throws IOException {
        // Create a temporary directory for testing
        testDir = Files.createTempDirectory("watcher-test");
    }

    /**
     * Clean up the temporary directory after each test method.
     *
     * @throws IOException if an I/O error occurs
     */
    @AfterEach
    public void tearDown() throws IOException {
        // Delete the temporary directory and its contents
        Files.walk(testDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
    }

    /**
     * Test the DirectoryWatcherService to ensure it correctly watches a directory for file creation events.
     *
     * Given scenario: A DirectoryWatcherService is set up to watch a temporary directory for changes.
     * The service should detect when a new file is created in the directory.
     *
     * Expected result: The DirectoryWatcherService should detect the creation of the new file and
     * an appropriate event should be placed in the event queue.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    @Test
    void testDirectoryWatcherService() throws IOException, InterruptedException {
        // Start watching the temporary directory for changes
        BlockingQueue<WatchEvent<?>> events = directoryWatcherService.watch(testDir, 1);

        // Use a CountDownLatch to wait for the event
        CountDownLatch latch = new CountDownLatch(1);

        // Start a new thread to monitor the event queue
        new Thread(() -> {
            try {
                WatchEvent<?> event = events.take();
                latch.countDown();
                System.out.println("Event: " + event.kind() + " for file: " + event.context());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Track if the event has been processed
        boolean eventProcessed = false;

        // Create a new temporary file in the watched directory
        for(int i = 0; i < 100; i++) {
            final Path tempFile = Files.createTempFile(testDir, "testFile", ".txt");
            eventProcessed = latch.await(2, TimeUnit.SECONDS);
            assertTrue(Files.exists(tempFile));
        }

        // Verify that the event was processed within the timeout period
        assertTrue(eventProcessed, "The event should have been processed within the timeout period");
    }
}
