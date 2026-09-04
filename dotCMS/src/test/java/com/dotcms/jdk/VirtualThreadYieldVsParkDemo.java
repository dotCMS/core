package com.dotcms.jdk;

/**
 * Live demo for the Java 25 talk: <b>{@code Thread.yield()} gives up the turn, not the carrier. Only
 * a park actually frees it.</b>
 *
 * <p>Two virtual threads loop forever on two carriers while a third, unrelated virtual thread waits
 * to be scheduled. What the loop body does decides whether the third one ever runs:
 *
 * <pre>
 *   mode=busy    while (true) { spin++; }        innocent NEVER runs
 *   mode=yield   Thread.yield()                  innocent NEVER runs   &lt;-- the surprise
 *   mode=sleep   Thread.sleep(1)                 innocent runs immediately
 * </pre>
 *
 * <p>Why {@code yield} is not enough: a carrier is a {@code ForkJoinPool} worker and it drains
 * <b>its own local queue first, LIFO</b>. A virtual thread that yields is re-submitted to that same
 * local queue, so the worker immediately picks it back up. The third thread was submitted externally
 * and sits in the shared submission queue, which a worker only visits once its local queue is empty.
 * {@code Thread.sleep} parks on a timer, the local queue genuinely empties, and only then does the
 * worker go looking and find the waiting task.
 *
 * <p>The talk conclusion: there is <b>no escape hatch for CPU-bound work on a virtual thread</b>. If
 * a task does not block on something that parks, it does not belong on a virtual thread. File I/O
 * fails the same test for the same reason — it never parks either (see #37038 / PR #37041).
 *
 * <p><b>Run it</b> (no flags needed; it pins the carrier count itself):
 *
 * <pre>
 *   java -Dmode=busy  dotCMS/src/test/java/com/dotcms/jdk/VirtualThreadYieldVsParkDemo.java
 *   java -Dmode=yield dotCMS/src/test/java/com/dotcms/jdk/VirtualThreadYieldVsParkDemo.java
 *   java -Dmode=sleep dotCMS/src/test/java/com/dotcms/jdk/VirtualThreadYieldVsParkDemo.java
 * </pre>
 *
 * <p>Not a JUnit test on purpose, and {@code System.out} is deliberate — see the note on
 * {@link VirtualThreadCarrierStarvationDemo}.
 *
 * @see VirtualThreadCarrierStarvationDemo
 * @see VirtualThreadCarrierTimelineDemo
 */
public class VirtualThreadYieldVsParkDemo {

    private static final int CARRIERS = 2;

    public static void main(final String[] args) throws InterruptedException {
        // Read lazily on first virtual thread creation, so setting it here is enough.
        if (System.getProperty("jdk.virtualThreadScheduler.parallelism") == null) {
            System.setProperty("jdk.virtualThreadScheduler.parallelism", String.valueOf(CARRIERS));
        }

        final String mode = System.getProperty("mode", "busy");   // busy | yield | sleep
        System.out.println("mode = " + mode
                + " | carriers = " + System.getProperty("jdk.virtualThreadScheduler.parallelism"));

        for (int i = 0; i < CARRIERS; i++) {
            Thread.ofVirtual().name("hog-" + i).start(() -> {
                long spin = 0;
                while (true) {
                    spin++;
                    try {
                        switch (mode) {
                            case "yield" -> Thread.yield();   // re-queued on the SAME worker, LIFO
                            case "sleep" -> Thread.sleep(1);  // parks: the local queue empties
                            default -> { /* busy: never asks to get off the carrier */ }
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }

        Thread.sleep(200);   // let the hogs take both carriers

        // A completely unrelated part of the system.
        Thread.ofVirtual().name("innocent").start(
                () -> System.out.println(">>> the INNOCENT virtual thread RAN"));

        Thread.sleep(3000);
        System.out.println("--- done: no '>>>' above means the innocent never ran ---");
        System.exit(0);
    }
}
