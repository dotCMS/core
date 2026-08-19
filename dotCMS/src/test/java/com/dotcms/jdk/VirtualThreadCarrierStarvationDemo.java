package com.dotcms.jdk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live demo for the Java 25 talk: <b>a virtual-thread executor never runs out of workers — it runs
 * out of JVM.</b>
 *
 * <p>Ten tasks, each an uninterruptible {@code while(true)} CPU loop, are submitted to either a
 * virtual-thread-per-task executor or a fixed pool of two platform threads. In both cases only two
 * tasks ever execute, because there are only two carriers / two pool threads. The difference is the
 * blast radius:
 *
 * <pre>
 *                                  VT executor      fixed platform pool (2)
 *   tasks that started               2 of 10               2 of 10
 *   unrelated code elsewhere ran?      NO                    YES
 * </pre>
 *
 * <p>With platform threads the starvation is contained: eight tasks wait in <em>your</em> queue,
 * under <em>your</em> bound, and the OS preempts the two runners so the rest of the process keeps
 * going. With virtual threads the scarce resource is the carrier pool, which is
 * {@code availableProcessors()} wide, <b>global to the JVM and shared with every other virtual
 * thread in the process</b> — so an unrelated task that never touched this executor is starved too.
 * The VT scheduler is cooperative and will not take a carrier back by force.
 *
 * <p>This is the mechanism behind issue #37038 (fixed in PR #37041): a blocking file read pins its
 * carrier for the whole call exactly like this CPU loop does, and with 2-4 carriers in a container a
 * handful of slow reads on a network mount stalled content indexing process-wide.
 *
 * <p><b>Run it</b> (no flags needed — it pins the carrier count itself so the demo is reproducible
 * on a 24-core laptop):
 *
 * <pre>
 *   java -Dmode=vt       dotCMS/src/test/java/com/dotcms/jdk/VirtualThreadCarrierStarvationDemo.java
 *   java -Dmode=platform dotCMS/src/test/java/com/dotcms/jdk/VirtualThreadCarrierStarvationDemo.java
 * </pre>
 *
 * <p>Not a JUnit test on purpose: the hog tasks ignore interrupts and can never be reclaimed, so
 * running them inside a shared surefire JVM would starve the rest of the suite. It is a {@code main}
 * demo, compiled by the real build so it cannot silently rot, and named {@code *Demo} so surefire
 * does not pick it up. {@code System.out} is deliberate here — the console output <em>is</em> the
 * artifact being shown to an audience; the Logger-only rule targets production code.
 *
 * @see VirtualThreadYieldVsParkDemo
 * @see VirtualThreadCarrierTimelineDemo
 */
public class VirtualThreadCarrierStarvationDemo {

    private static final int CARRIERS = 2;
    private static final int TASKS = 10;

    public static void main(final String[] args) throws InterruptedException {
        // Read lazily on first virtual thread creation, so setting it here is enough: the demo does
        // not depend on the audience's core count, and needs no command-line flag.
        if (System.getProperty("jdk.virtualThreadScheduler.parallelism") == null) {
            System.setProperty("jdk.virtualThreadScheduler.parallelism", String.valueOf(CARRIERS));
        }

        final String mode = System.getProperty("mode", "vt");
        final boolean virtual = "vt".equals(mode);
        final AtomicInteger started = new AtomicInteger();

        System.out.println("mode = " + mode
                + " | carriers = " + System.getProperty("jdk.virtualThreadScheduler.parallelism")
                + " | availableProcessors = " + Runtime.getRuntime().availableProcessors());

        final ExecutorService pool = virtual
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(CARRIERS);

        for (int i = 0; i < TASKS; i++) {
            pool.submit(() -> {
                started.incrementAndGet();
                long spin = 0;
                while (true) {                 // never blocks, never parks, never yields the carrier
                    spin++;
                }
            });
        }

        Thread.sleep(500);
        System.out.println("tasks that STARTED: " + started.get() + " of " + TASKS
                + (virtual ? "   (all " + TASKS + " virtual threads exist — 8 never ran a line)" : ""));

        // A completely unrelated part of the system, which knows nothing about the pool above.
        Thread.ofVirtual().name("innocent").start(
                () -> System.out.println(">>> the INNOCENT virtual thread RAN"));

        Thread.sleep(2000);
        System.out.println("--- done: no '>>>' above means unrelated code was starved too ---");

        // The hogs ignore interrupts by design, so there is nothing to shut down gracefully.
        System.exit(0);
    }
}
