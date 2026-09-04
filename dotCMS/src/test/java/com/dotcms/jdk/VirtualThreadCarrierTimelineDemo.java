package com.dotcms.jdk;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Live demo for the Java 25 talk: <b>you can watch the carriers</b>. Six virtual threads are handed
 * to two carriers, and the demo prints, tick by tick, which virtual thread each carrier was actually
 * running.
 *
 * <p><b>How to read the picture.</b> One <em>row</em> is one carrier: a real OS thread, the only
 * thing in the JVM that can actually execute code. One <em>cell</em> is one 100 ms tick, oldest on
 * the left, newest on the right. Inside the cell:
 *
 * <pre>
 *   1 .. 6   which of the six virtual threads was mounted on that carrier during that tick
 *   .        nobody was mounted: the carrier was idle and free for any other task in the JVM
 *   C        the unrelated 'canary' virtual thread, submitted mid-run, that only wants a carrier
 *            for a microsecond
 * </pre>
 *
 * <p>So a row that keeps repeating the same digit means one virtual thread took that carrier and
 * never gave it back. A row whose digits keep changing, with dots in between, means the tasks are
 * releasing the carrier and taking turns on it:
 *
 * <pre>
 *   mode=cpu    (never releases)             mode=sleep    (releases on every sleep)
 *   carrier-1 | 111111111111111111           carrier-1 | 1.4.2.6.3.1.5.2.
 *   carrier-2 | 222222222222222222           carrier-2 | 2.5.3.1.4.6.2.3.
 *   2 of 6 tasks ever got a carrier          6 of 6 tasks got a carrier
 *   canary: NEVER RAN                        canary: ran in 1 ms
 * </pre>
 *
 * <p>Same six tasks, same two carriers, same executor in both cases. The only difference is whether
 * the blocking call <em>releases</em> the carrier - which is the whole point: a virtual thread is
 * cheap, a carrier is not, and the carrier pool is {@code availableProcessors()} wide, global to the
 * JVM and shared with every other virtual thread in the process. Nothing takes a carrier back by
 * force; the scheduler is cooperative.
 *
 * <p><b>How the carrier is observed.</b> {@code VirtualThread.toString()} appends the carrier while
 * the thread is mounted and omits it while it is not:
 *
 * <pre>
 *   VirtualThread[#26,task-1]/runnable@ForkJoinPool-1-worker-1   &lt;- mounted, prints as a digit
 *   VirtualThread[#28,task-3]/timed_waiting                      &lt;- unmounted, prints as a dot
 * </pre>
 *
 * The JDK names the carriers {@code ForkJoinPool-1-worker-N}; the demo relabels them
 * {@code carrier-N}, because "worker" is exactly the word that makes an audience think it is looking
 * at the virtual threads instead of at the scarce OS threads underneath them.
 *
 * A platform thread polls that every tick. It has to be a <b>platform</b> thread: run the monitor on
 * a virtual thread ({@code -Dmonitor=virtual}) and the tool itself freezes mid-drawing, which is the
 * same failure the rest of the JVM is suffering.
 *
 * <p><b>Modes</b> ({@code -Dmode=}):
 *
 * <pre>
 *   compare  (default)  all four phases: sleep, socket, file, cpu - in that order
 *   sleep               Thread.sleep in a loop                 - unmounts on every sleep
 *   socket              read() on a socket nobody writes to    - unmounts once, forever
 *   file                read() on a FIFO nobody writes to      - blocking FILE i/o: measure it
 *   cpu                 uninterruptible while(true) loop       - never unmounts
 * </pre>
 *
 * <p>The order of {@code compare} is itself part of the lesson. {@code sleep} and {@code socket} give
 * the carrier back, so the JVM survives them; {@code file} parks its tasks in a read that keeps the
 * carrier, but writing to the FIFO releases them, so the phase can clean up after itself; {@code cpu}
 * cannot be cleaned up at all - its tasks ignore interrupts - so it has to run last. A phase that
 * starts with no carrier left to obtain says {@code SKIPPED} instead of drawing an empty grid.
 *
 * <p>{@code file} is the shape of issue #37038 (fixed in PR #37041): a blocking file read on a
 * network mount, on a container with 2-4 carriers, stalled content indexing process-wide. Run
 * {@code file} and {@code socket} back to back - the rows tell you which kind of blocking Loom knows
 * how to unmount and which one just eats a carrier.
 *
 * <pre>
 *   java dotCMS/src/test/java/com/dotcms/jdk/VirtualThreadCarrierTimelineDemo.java
 *   java -Dmode=file   dotCMS/src/test/java/com/dotcms/jdk/VirtualThreadCarrierTimelineDemo.java
 *   java -Dmode=socket -Dcolor=false ...VirtualThreadCarrierTimelineDemo.java
 * </pre>
 *
 * <p>Not a JUnit test on purpose: the cpu tasks ignore interrupts and can never be reclaimed, so
 * running them inside a shared surefire JVM would starve the rest of the suite. It is a {@code main}
 * demo, compiled by the real build so it cannot silently rot, and named {@code *Demo} so surefire
 * skips it. {@code System.out} is deliberate: the console output <em>is</em> the artifact being shown
 * to an audience; the Logger-only rule targets production code.
 *
 * @see VirtualThreadCarrierStarvationDemo
 * @see VirtualThreadYieldVsParkDemo
 */
public class VirtualThreadCarrierTimelineDemo {

    private static final int CARRIERS = 2;
    private static final int TASKS = 6;
    private static final int TICK_MS = 100;
    private static final int TICKS = 40;
    private static final int CANARY_TICK = 5;
    private static final char FREE = '.';
    private static final char CANARY = 'C';
    private static final boolean COLOR = !"false".equals(System.getProperty("color"));
    private static final String CSI = ((char) 27) + "[";

    /** What each of the six tasks does, and how its rows should be read. */
    private record Workload(String name, String detail, String expectation, Runnable body) {

    }

    public static void main(final String[] args) throws Exception {
        // Read lazily on first virtual thread creation, so setting it here is enough: the demo does
        // not depend on the audience's core count and needs no command-line flag.
        if (System.getProperty("jdk.virtualThreadScheduler.parallelism") == null) {
            System.setProperty("jdk.virtualThreadScheduler.parallelism", String.valueOf(CARRIERS));
        }
        final String mode = System.getProperty("mode", "compare");
        if ("compare".equals(mode)) {
            // Order matters, and it is part of the lesson. sleep and socket release the carrier, so
            // the JVM survives them. file parks its tasks in a blocking read that keeps the carrier,
            // but they can be unblocked by writing to the FIFO, so the phase cleans up after itself.
            // cpu cannot be cleaned up - the tasks ignore interrupts - so it has to be the last one.
            runPhase("sleep");
            //runPhase("socket");
            //runPhase("file");
            runPhase("cpu");     // terminal: nothing in this JVM gets a carrier after this
        } else {
            runPhase(mode);
        }
        System.out.flush();
        System.exit(0);          // the cpu tasks ignore interrupts; there is nothing to shut down
    }

    private static void runPhase(final String mode) throws Exception {
        final List<AutoCloseable> cleanup = new ArrayList<>();
        final AtomicLong releases = new AtomicLong();
        // Every phase must leave the JVM as it found it, or the next phase measures the leftovers
        // instead of its own workload. Whatever can retire, retires when this flips.
        final AtomicBoolean running = new AtomicBoolean(true);
        final Workload workload = workload(mode, releases, running, cleanup);

        System.out.println();
        System.out.println("mode = " + workload.name() + "   |   " + workload.detail());
        System.out.println("carriers = " + System.getProperty("jdk.virtualThreadScheduler.parallelism")
                + " of " + Runtime.getRuntime().availableProcessors() + " cpus"
                + "   |   tasks = " + TASKS + " virtual threads"
                + "   |   tick = " + TICK_MS + " ms");
        System.out.println();
        printLegend(workload);
        System.out.println();

        if (!carrierAvailable()) {
            System.out.println("  SKIPPED: no carrier can be obtained any more. An earlier phase took"
                    + " both carriers and");
            System.out.println("           never gave them back, so there is nothing left to measure"
                    + " here. Run this");
            System.out.println("           mode in its own JVM:  -Dmode=" + mode);
            System.out.println();
            running.set(false);
            closeAll(cleanup);
            return;
        }

        final Map<Long, Character> labels = new LinkedHashMap<>();
        final List<Thread> watched = new ArrayList<>();
        final Timeline timeline = new Timeline(labels);
        final Runnable body = workload.body();
        for (int i = 0; i < TASKS; i++) {
            // The label is decided here, not after start(), because the task reports its own carrier
            // the instant it starts running - which can happen before start() has even returned.
            final char label = (char) ('1' + i);
            final Thread task = Thread.ofVirtual().name("task-" + (i + 1)).start(() -> {
                timeline.selfReport(label);
                body.run();
            });
            labels.put(task.threadId(), label);
            watched.add(task);
        }

        final AtomicLong canaryRanAt = new AtomicLong(-1);
        final AtomicLong canarySubmittedAt = new AtomicLong();
        final Runnable monitor = () -> {
            for (int tick = 0; tick < TICKS; tick++) {
                if (tick == CANARY_TICK) {
                    // A completely unrelated part of the system, which knows nothing about the six
                    // tasks above and only wants a carrier for a microsecond.
                    canarySubmittedAt.set(System.nanoTime());
                    final Thread canary = Thread.ofVirtual().name("canary").start(() -> {
                        timeline.selfReport(CANARY);
                        canaryRanAt.set(System.nanoTime() - canarySubmittedAt.get());
                    });
                    labels.put(canary.threadId(), CANARY);
                    watched.add(canary);
                }
                timeline.sample(watched);
                timeline.draw();
                sleep(TICK_MS);
            }
        };

        if ("virtual".equals(System.getProperty("monitor"))) {
            Thread.ofVirtual().name("monitor").start(monitor).join();
        } else {
            monitor.run();
        }

        System.out.println();
        System.out.printf("  tasks that ever held a carrier           : %d of %d%n",
                timeline.everMounted().stream().filter(label -> label != CANARY).count(), TASKS);
        System.out.printf("  times a carrier changed hands            : %d   (digit changes above)%n",
                timeline.handOffs());
        System.out.printf("  blocking calls that RELEASED a carrier   : %d   (counted by the tasks)%n",
                releases.get());
        System.out.printf("  unrelated 'canary' virtual thread        : %s%n",
                canaryRanAt.get() < 0
                        ? "NEVER RAN in " + (TICKS * TICK_MS) + " ms  <-- the whole JVM is starved"
                        : String.format("ran %.2f ms after it was submitted",
                                canaryRanAt.get() / 1_000_000d));
        System.out.println();

        running.set(false);     // tell the tasks to retire...
        closeAll(cleanup);      // ...and unblock the ones parked in a read that ignores the flag
    }

    /**
     * The audience has to be told what the drawing means before it starts moving, so the legend is
     * printed once per phase, right above the rows it explains.
     */
    private static void printLegend(final Workload workload) {
        System.out.println("  HOW TO READ THE ROWS BELOW");
        legend("one row", "one CARRIER: a real OS thread, the only thing that can run code");
        legend("one cell", "one " + TICK_MS + " ms tick - oldest on the left, newest on the right");
        legend("1 .. " + TASKS, "which virtual thread that carrier was running during that tick");
        legend(String.valueOf(FREE), "carrier IDLE: nothing was mounted, so it was free for anyone else");
        legend(String.valueOf(CANARY), "an unrelated virtual thread, submitted at tick " + CANARY_TICK
                + ", that only needs a carrier for a microsecond");
        legend("same digit", "one virtual thread is holding that carrier and never gives it back");
        legend("digits change", "the tasks release the carrier, so they take turns on it");
        legend("expect", workload.expectation());
    }

    /** Fixed-width key column, wrapped text: a legend nobody can read is not a legend. */
    private static void legend(final String key, final String text) {
        final String pad = "  %-14s %s%n";
        final String indent = " ".repeat(17);
        final StringBuilder line = new StringBuilder();
        boolean first = true;
        for (final String word : text.split(" ")) {
            if (line.length() + word.length() + 1 > 74) {
                System.out.printf(first ? pad : "%s%s%n", first ? key : indent, line);
                line.setLength(0);
                first = false;
            }
            line.append(line.isEmpty() ? "" : " ").append(word);
        }
        System.out.printf(first ? pad : "%s%s%n", first ? key : indent, line);
    }

    /**
     * Submits a throw-away virtual thread and sees whether it ever gets to run. A phase that follows
     * a carrier-eating workload cannot measure anything, and an empty drawing is a worse answer than
     * saying so out loud.
     */
    private static boolean carrierAvailable() throws InterruptedException {
        final AtomicBoolean ran = new AtomicBoolean();
        Thread.ofVirtual().name("probe").start(() -> ran.set(true)).join(1_000);
        return ran.get();
    }

    private static void closeAll(final List<AutoCloseable> cleanup) {
        for (final AutoCloseable closeable : cleanup) {
            try {
                closeable.close();
            } catch (final Exception ignore) {
                // demo teardown, best effort
            }
        }
    }

    private static Workload workload(final String mode, final AtomicLong releases,
            final AtomicBoolean running, final List<AutoCloseable> cleanup) throws Exception {
        switch (mode) {
            case "cpu":
                return new Workload("cpu", "while(true) spin++   (no block, no park, no yield)",
                        "every row stuck on one digit: two tasks own both carriers forever, "
                                + "the other four never get to run",
                        () -> {
                            long spin = 0;
                            while (true) {      // deliberately ignores 'running': that IS the lesson,
                                spin++;         // nothing can take a carrier back by force
                            }
                        });
            case "sleep":
                return new Workload("sleep", "15 ms of work, then Thread.sleep(35), in a loop",
                        "digits keep changing, with dots in between: every sleep releases "
                                + "the carrier, so all six tasks take turns on the two carriers",
                        () -> {
                            while (running.get()) {
                                burn(15);
                                sleep(35);
                                releases.incrementAndGet();
                            }
                        });
            case "socket": {
                final ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
                cleanup.add(server);
                // Accept every connection and never write a byte, so all readers block in read().
                Thread.ofPlatform().daemon().name("silent-server").start(() -> {
                    while (!server.isClosed()) {
                        try {
                            cleanup.add(server.accept());
                        } catch (final Exception closed) {
                            return;
                        }
                    }
                });
                return new Workload("socket", "read() on a socket nobody ever writes to",
                        "a digit for a moment, then dots everywhere: a blocked socket read "
                                + "releases the carrier, so both carriers end up idle",
                        () -> {
                            if (!running.get()) {
                                return;         // the phase is over; do not take a carrier for nothing
                            }
                            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(),
                                    server.getLocalPort())) {
                                releases.incrementAndGet();
                                socket.getInputStream().read();   // blocks forever
                            } catch (final Exception e) {
                                throw new IllegalStateException(e);
                            }
                        });
            }
            case "file": {
                final Path fifo = fifo();
                cleanup.add(() -> unblockFifoReaders(fifo));
                return new Workload("file", "read() on a FIFO nobody ever writes to  (" + fifo + ")",
                        "this is the measurement: if the rows stay stuck on a digit, blocking "
                                + "FILE i/o kept the carrier instead of releasing it",
                        () -> {
                            if (!running.get()) {
                                return;         // opening the FIFO now would park forever on a carrier
                            }
                            try (InputStream in = Files.newInputStream(fifo)) {
                                in.read();                        // blocks forever
                            } catch (final Exception e) {
                                throw new IllegalStateException(e);
                            }
                        });
            }
            default:
                throw new IllegalArgumentException(
                        "unknown mode '" + mode + "' - use compare|cpu|sleep|socket|file");
        }
    }

    /**
     * The point of {@code file} is that a blocking file read keeps its carrier, so when the phase ends
     * both carriers are still held by tasks parked inside {@code read()}. Opening the FIFO for writing
     * hands each of them a byte; they return, finish, and the carriers come back. Without this the
     * phase would poison every phase after it - which is exactly what {@code cpu} does, and why
     * {@code cpu} runs last.
     */
    private static void unblockFifoReaders(final Path fifo) throws Exception {
        final Thread writer = Thread.ofPlatform().daemon().name("fifo-writer").start(() -> {
            try (OutputStream out = Files.newOutputStream(fifo)) {   // returns once a reader is parked
                for (int i = 0; i < TASKS * 4; i++) {
                    out.write('x');
                    out.flush();
                    sleep(25);      // let the readers that were still queued get their turn too
                }
            } catch (final Exception ignore) {
                // nobody left to unblock; the JVM is about to end the phase anyway
            }
        });
        writer.join(2_000);         // never let teardown wedge the demo
    }

    private static Path fifo() throws Exception {
        final Path fifo = Path.of(System.getProperty("java.io.tmpdir"), "vt-carrier-demo.fifo");
        Files.deleteIfExists(fifo);
        final int exit = new ProcessBuilder("mkfifo", fifo.toString())
                .redirectErrorStream(true).start().waitFor();
        if (exit != 0 || !Files.exists(fifo)) {
            throw new IllegalStateException("mkfifo failed (exit " + exit + ") - mode=file needs a FIFO");
        }
        return fifo;
    }

    /** A virtual thread telling the timeline which carrier it woke up on. */
    private record Mount(String carrier, Character label) {

    }

    /** One row per carrier, one column per tick, plus the bookkeeping the summary needs. */
    private static final class Timeline {

        /** {@code worker-10} has to sort after {@code worker-2}, so compare the number, not the text. */
        private static final Comparator<String> BY_CARRIER_NUMBER =
                Comparator.comparingInt(Timeline::carrierNumber).thenComparing(Comparator.naturalOrder());

        private final Map<Long, Character> labels;
        /**
         * Sorted, not insertion-ordered: which carrier is discovered first is a race, and a row order
         * that changes between runs is one more thing the audience has to explain away.
         */
        private final Map<String, StringBuilder> lanes = new TreeMap<>(BY_CARRIER_NUMBER);
        /** Mounts reported by the tasks themselves; drained by the monitor on the next tick. */
        private final ConcurrentLinkedQueue<Mount> selfReported = new ConcurrentLinkedQueue<>();
        private final Map<String, Character> current = new LinkedHashMap<>();
        private final Set<Character> everMounted = new LinkedHashSet<>();
        private int handOffs;
        private int linesDrawn;

        private Timeline(final Map<Long, Character> labels) {
            this.labels = labels;
        }

        /**
         * Called by each task on its own carrier, before it blocks. A {@code socket} task is mounted
         * for a few microseconds - connect, then {@code read()} - so a 100 ms sampler only catches it
         * by luck, and when it misses every task the drawing has no rows at all. The task reporting
         * its own carrier is the same observation, just from a finer-grained observer; without it the
         * picture depends on how loaded the machine is.
         */
        private void selfReport(final char label) {
            final String carrier = carrierOf(Thread.currentThread());
            if (carrier != null) {
                selfReported.add(new Mount(carrier, label));   // the monitor owns all the other state
            }
        }

        private void sample(final List<Thread> watched) {
            final Map<String, Character> mounted = new LinkedHashMap<>();
            for (final Thread virtual : watched) {
                final String carrier = carrierOf(virtual);
                if (carrier != null) {
                    final Character label = labels.get(virtual.threadId());
                    mounted.put(carrier, label);
                    everMounted.add(label);
                    lanes.computeIfAbsent(carrier, unused -> backFilled());
                }
            }
            // Mounts too short for this tick to see. Every report counts towards "ever held a
            // carrier", but a single cell can only show one label: a live reading wins over a report,
            // and among several reports on the same carrier the first one wins.
            for (Mount report = selfReported.poll(); report != null; report = selfReported.poll()) {
                everMounted.add(report.label());
                lanes.computeIfAbsent(report.carrier(), unused -> backFilled());
                mounted.putIfAbsent(report.carrier(), report.label());
            }
            for (final Map.Entry<String, StringBuilder> lane : lanes.entrySet()) {
                final Character now = mounted.get(lane.getKey());
                final Character before = current.get(lane.getKey());
                if (now != null && !now.equals(before)) {
                    handOffs++;
                }
                current.put(lane.getKey(), now);
                lane.getValue().append(now == null ? FREE : now);
            }
        }

        /**
         * A carrier is only visible while the virtual thread is mounted:
         * {@code VirtualThread[#26,task-1]/runnable@ForkJoinPool-1-worker-1}.
         */
        private static String carrierOf(final Thread virtual) {
            final String description = virtual.toString();
            final int at = description.lastIndexOf('@');
            return at < 0 ? null : description.substring(at + 1);
        }

        /** A carrier discovered late still needs a full-width row, so back-fill it as free. */
        private StringBuilder backFilled() {
            final int width = lanes.isEmpty() ? 0 : lanes.values().iterator().next().length();
            return new StringBuilder(String.valueOf(FREE).repeat(width));
        }

        private void draw() {
            if (linesDrawn > 0) {
                System.out.print(CSI + linesDrawn + "A");   // redraw the block in place
            }
            // The block gets taller as carriers are discovered, so erase downwards first: without
            // this the taller frame leaves the previous frame's last row stranded on screen.
            System.out.print(CSI + "0J");
            if (lanes.isEmpty()) {
                System.out.println("  (waiting: no task has been given a carrier yet)");
                linesDrawn = 1;
                return;
            }
            int lines = 0;
            int width = 0;
            for (final Map.Entry<String, StringBuilder> lane : lanes.entrySet()) {
                System.out.printf("  %-10s | %s%n", carrierName(lane.getKey()), colorize(lane.getValue()));
                width = Math.max(width, lane.getValue().length());
                lines++;
            }
            System.out.printf("  %-10s | %s%n", "time", axis(width));
            lines++;
            linesDrawn = lines;
        }

        /** A second marker under the cells, so the row reads as elapsed time, not as an abstract band. */
        private static String axis(final int width) {
            final int ticksPerSecond = Math.max(1, 1000 / TICK_MS);
            final StringBuilder out = new StringBuilder(width + 4);
            for (int tick = 0; tick < width; tick++) {
                if (tick % ticksPerSecond == 0) {
                    out.append(tick / ticksPerSecond).append('s');
                } else if (out.length() <= tick) {
                    out.append(' ');
                }
            }
            return out.toString();
        }

        /**
         * The JDK calls them {@code ForkJoinPool-1-worker-N}, but "worker" reads as "one of my
         * tasks"; what the row actually is, is a carrier.
         */
        private static int carrierNumber(final String carrier) {
            final int dash = carrier.lastIndexOf('-');
            try {
                return dash < 0 ? Integer.MAX_VALUE : Integer.parseInt(carrier.substring(dash + 1));
            } catch (final NumberFormatException notNumbered) {
                return Integer.MAX_VALUE;
            }
        }

        private static String carrierName(final String carrier) {
            final int dash = carrier.lastIndexOf("-worker-");
            return dash < 0 ? carrier : "carrier-" + carrier.substring(dash + "-worker-".length());
        }

        private static String colorize(final CharSequence row) {
            if (!COLOR) {
                return row.toString();
            }
            final StringBuilder out = new StringBuilder(row.length() * 12);
            for (int i = 0; i < row.length(); i++) {
                final char cell = row.charAt(i);
                if (cell == FREE) {
                    out.append(CSI).append("90m").append(cell).append(CSI).append("0m");
                } else if (cell == CANARY) {
                    out.append(CSI).append("1;97m").append(cell).append(CSI).append("0m");
                } else {
                    out.append(CSI).append("1;3").append((cell - '1') % 6 + 1).append('m')
                            .append(cell).append(CSI).append("0m");
                }
            }
            return out.toString();
        }

        private Set<Character> everMounted() {
            return everMounted;
        }

        private int handOffs() {
            return handOffs;
        }
    }

    /** Keeps the carrier busy without blocking, so the sampler can see who is mounted. */
    private static void burn(final long millis) {
        final long until = System.nanoTime() + millis * 1_000_000L;
        long spin = 0;
        while (System.nanoTime() < until) {
            spin++;
        }
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
