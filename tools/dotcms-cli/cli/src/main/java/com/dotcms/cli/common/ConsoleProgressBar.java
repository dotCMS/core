package com.dotcms.cli.common;

import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * A console progress bar that can be used to display the progress of a task.
 */
public class ConsoleProgressBar implements Runnable {

    private final char incomplete = '░'; // U+2591 Unicode Character
    private final char complete = '█'; // U+2588 Unicode Character

    private Future<?> futureResult;
    private final long animationDelay;

    private int totalSteps;
    private int currentStep;

    OutputOptionMixin out;

    /**
     * Constructs a new ConsoleProgressBar object.
     *
     * @param output         the output option mixin to use for printing the progress bar
     * @param animationDelay the delay between animation frames in milliseconds
     */
    public ConsoleProgressBar(OutputOptionMixin output, long animationDelay) {

        this.out = output;

        this.animationDelay = animationDelay;

        this.totalSteps = 0;
        this.currentStep = 0;
    }

    /**
     * Sets the future result associated with this progress bar.
     *
     * @param futureResult the future result to set
     */
    public void setFuture(final Future<?> futureResult) {
        this.futureResult = futureResult;
    }

    /**
     * Sets the total number of steps for the progress bar.
     *
     * @param totalSteps the total number of steps
     */
    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    /**
     * Increments the current step of the progress bar.
     */
    public synchronized void incrementStep() {
        currentStep++;
    }

    /**
     * Runs the progress bar animation until the associated future result is done.
     */
    @Override
    public void run() {

        StringBuilder builder = null;

        try {
            while (!futureResult.isDone()) {

                if (builder == null && totalSteps > 0) {
                    builder = new StringBuilder();
                    Stream.generate(() -> incomplete).limit(totalSteps).forEach(builder::append);
                }

                printProgress(builder, currentStep, totalSteps);

                Thread.sleep(animationDelay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (currentStep >= totalSteps && builder != null) {
                // We reached 100% progress
                printProgress(builder, totalSteps, totalSteps);
            }
        }
    }

    /**
     * Prints the progress bar and current progress percentage.
     *
     * @param builder     the string builder containing the progress bar characters
     * @param currentStep the current step of the progress
     * @param totalSteps  the total number of steps
     */
    private void printProgress(StringBuilder builder, final int currentStep, final int totalSteps) {

        if (totalSteps > 0) {
            int progress = (int) ((double) currentStep / totalSteps * 100);

            builder.replace(
                    0,
                    currentStep,
                    builder.substring(0, currentStep).replace(incomplete, complete)
            );

            var progressString = String.format("  @|bold,yellow %d%s|@", progress, "%");

            String progressBar = "\r" + builder + progressString;
            this.out.print(progressBar);
        }
    }

}

