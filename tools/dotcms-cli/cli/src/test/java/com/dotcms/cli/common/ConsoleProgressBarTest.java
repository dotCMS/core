package com.dotcms.cli.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsoleProgressBarTest {

    /**
     * Given scenario: more steps are reported than the total the bar was given, which happens on a
     * push to a new site because the site's root folder is processed but not counted, and the
     * animation draws a frame while the task is still running.
     * Expected result: the frame is drawn without throwing. Before the fix the bar asked for 106
     * of its 100 characters and threw StringIndexOutOfBoundsException, which failed the whole push.
     */
    @Test
    void run_moreStepsThanTotal_doesNotThrow() {
        final ConsoleProgressBar progressBar =
                new ConsoleProgressBar(Mockito.mock(OutputOptionMixin.class), 1, 100);
        progressBar.setTotalSteps(16);
        for (int step = 0; step < 17; step++) {
            progressBar.incrementStep();
        }

        // Still running for the first frames, then done so run() returns
        final CompletableFuture<Void> task = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS)
                .execute(() -> task.complete(null));
        progressBar.setFuture(task);

        assertDoesNotThrow(progressBar::run);
    }
}
