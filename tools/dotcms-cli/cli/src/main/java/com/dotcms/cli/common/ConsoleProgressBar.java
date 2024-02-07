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
    private final int animationCharSize;

    OutputOptionMixin out;

    /**
     * Constructs a new ConsoleProgressBar object.
     *
     * @param output the output option mixin to use for printing the progress bar
     */
    public ConsoleProgressBar(OutputOptionMixin output) {

        this.out = output;

        this.animationDelay = 250;
        this.animationCharSize = 100;

        this.totalSteps = 0;
        this.currentStep = 0;
    }

    /**
     * Constructs a new ConsoleProgressBar object.
     *
     * @param output            the output option mixin to use for printing the progress bar
     * @param animationDelay    the delay between animation frames in milliseconds
     * @param animationCharSize the number of characters to use for the animation
     */
    public ConsoleProgressBar(OutputOptionMixin output, final long animationDelay, final int animationCharSize) {

        this.out = output;

        this.animationDelay = animationDelay;
        this.animationCharSize = animationCharSize;

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
     * Just add a next line marker when done.
     */
    public synchronized void done(){
        this.out.print("\n");
    }

    /**
     * Runs the progress bar animation until the associated future result is done.
     */
    @Override
    public void run() {

        StringBuilder builder = null;

        try {
            while (!futureResult.isDone()) {

                builder = initBuilder(builder);
                printProgress(builder, currentStep, totalSteps);

                Thread.sleep(animationDelay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (currentStep >= totalSteps) {

                // If there were no steps to run, we still need to show the progress bar as completed
                totalSteps = totalSteps > 0 ? totalSteps : 1;

                builder = initBuilder(builder);

                // We reached 100% progress
                printProgress(builder, totalSteps, totalSteps);
            }
        }
    }

    /**
     * Initializes the string builder with the animation characters.
     *
     * @param builder the string builder to initialize
     * @return the initialized string builder
     */
    private StringBuilder initBuilder(StringBuilder builder) {

        if (builder == null && totalSteps > 0) {
            builder = new StringBuilder();
            Stream.generate(() -> incomplete).limit(animationCharSize).forEach(builder::append);
        }

        return builder;
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
            int charSizeProgress = (int) ((double) currentStep / totalSteps * animationCharSize);

            builder.replace(
                    0,
                    charSizeProgress,
                    builder.substring(0, charSizeProgress).replace(incomplete, complete)
            );

            var progressString = String.format("  @|bold,yellow %d%s|@", progress, "%");

            String progressBar = "\r" + builder + progressString;
            this.out.print(progressBar);
        }
    }

}

