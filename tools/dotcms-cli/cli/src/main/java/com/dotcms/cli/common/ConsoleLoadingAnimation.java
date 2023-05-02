package com.dotcms.cli.common;

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
    private final String[] animationChars;
    private final long animationDelay;

    /**
     * Creates a new ConsoleLoadingAnimation object with the given parameters.
     *
     * @param futureResult   the Future object that will be used to check if the animation should
     *                       stop.
     * @param animationChars the array of characters that will be used to create the animation.
     * @param animationDelay the delay between each animation frame.
     */
    public ConsoleLoadingAnimation(Future<?> futureResult, String[] animationChars,
            long animationDelay) {
        this.futureResult = futureResult;
        this.animationChars = animationChars;
        this.animationDelay = animationDelay;
    }

    /**
     * Runs the animation in the console while the Future object is not done yet. If the animation
     * is interrupted, the thread is stopped and the animation is cleared from the console.
     */
    @Override
    public void run() {
        int counter = 0;
        try {
            while (!futureResult.isDone()) {
                System.out.print("\r" + animationChars[counter % animationChars.length]);
                counter++;
                Thread.sleep(animationDelay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.print("\r"); // Clear the animation
        }
    }
}

