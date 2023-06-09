package com.dotcms.cli.common;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.Future;

/**
 * The ConsoleLoadingAnimation class is responsible for printing an animation in the console while a
 * {@link java.util.concurrent.Future Future} is not done yet. The animation can be customized by
 * using one of the available arrays of characters.
 *
 * @see java.lang.Runnable
 */
public class ConsoleLoadingAnimation implements Runnable {

    public static final String[] ANIMATION_CHARS_SIMPLE = new String[]{
            "|",
            "/",
            "-",
            "\\"
    };

    public static final String[] ANIMATION_CHARS_BLOCKS = new String[]{
            "\u2591", // ░
            "\u2592", // ▒
            "\u2593", // ▓
            "\u2588"  // █
    };

    public static final String[] ANIMATION_CHARS_GEOMETRIC = new String[]{
            "\u25B2", // ▲
            "\u25B6", // ▶
            "\u25BC", // ▼
            "\u25C0"  // ◀
    };

    public static final String[] ANIMATION_CHARS_BOX = new String[]{
            "\u2500", // ─
            "\u2502", // │
            "\u250C", // ┌
            "\u2510", // ┐
            "\u2514", // └
            "\u2518"  // ┘
    };

    private final Future<?> futureResult;
    private final Queue<String> animationQueue;
    private final long animationDelay;

    private final OutputOptionMixin out;

    /**
     * Creates a new ConsoleLoadingAnimation object with the given parameters.
     *
     * @param output         the OutputOptionMixin object that will be used to print the animation.
     * @param futureResult   the Future object that will be used to check if the animation should
     *                       stop.
     * @param animationChars the array of characters that will be used to create the animation.
     * @param animationDelay the delay between each animation frame.
     */
    public ConsoleLoadingAnimation(
            OutputOptionMixin output,
            Future<?> futureResult,
            String[] animationChars,
            long animationDelay
    ) {

        this.out = output;

        this.futureResult = futureResult;
        this.animationDelay = animationDelay;

        this.animationQueue = new ArrayDeque<>();  // Initialize an ArrayDeque as the animationQueue
        // Add characters from the array to the queue
        animationQueue.addAll(Arrays.asList(animationChars));
    }

    /**
     * Runs the animation in the console while the Future object is not done yet. If the animation
     * is interrupted, the thread is stopped and the animation is cleared from the console.
     */
    @Override
    public void run() {

        try {
            while (!futureResult.isDone()) {

                var nextCharacter = animationQueue.poll();  // Retrieve the next character from the front of the queue
                animationQueue.offer(nextCharacter);  // Add the character back to the end of the queue
                this.out.print(String.format("\r@|bold,yellow %s|@", nextCharacter));

                Thread.sleep(animationDelay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.out.print("\r"); // Clear the animation
        }
    }
}

