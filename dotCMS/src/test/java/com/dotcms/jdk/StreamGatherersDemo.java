package com.dotcms.jdk;

import java.util.List;
import java.util.Objects;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;

/**
 * Live demo for the Java 25 talk: <b>stream gatherers — the extension point the middle of a stream
 * never had.</b>
 *
 * <p>A {@code Stream}'s intermediate vocabulary closed in Java 8 and never grew: {@code map},
 * {@code filter}, {@code limit}, {@code skip}, {@code sorted}, {@code distinct}, {@code peek},
 * {@code flatMap}. Since Java 8 you have been able to write your own <b>terminal</b> operation — that
 * is what a {@code Collector} is. There was never a way to write your own <b>intermediate</b> one.
 *
 * <p>The reason it matters is that the existing operations are amnesic. {@code map} sees one element
 * and produces one; {@code filter} sees one and decides keep-or-drop; even {@code flatMap}, which can
 * change the element count, decides looking at a single element. None can remember anything about what
 * it already saw — which rules out a whole family of ordinary operations:
 *
 * <pre>
 *   batches of 50                     must accumulate 50 before emitting anything
 *   sliding window                    must remember the previous elements
 *   running total                     must remember the sum
 *   collapse consecutive duplicates   must remember the previous element
 *   stop once a condition holds       must remember that it held
 * </pre>
 *
 * All of them need state <i>between</i> elements, and there was nowhere to put it. So the way out was
 * always the same: {@code collect(toList())}, leave the stream, and finish by hand with a {@code for}
 * and a variable outside it. <b>A gatherer is where that state goes</b> — {@code .gather(...)} takes a
 * stream and returns a stream, so the pipeline survives.
 *
 * <h2>The two costs of doing it by hand, both of which are in this codebase</h2>
 *
 * <ol>
 *   <li><b>Materialising a list you only wanted to walk in pieces.</b>
 *       {@code OSIndexAPIImpl.getIndexAlias} (around lines 846-855) does
 *       {@code stream().map(...).collect(toList())} and immediately
 *       {@code Lists.partition(physicalNames, ALIAS_LOOKUP_BATCH_SIZE)}. The whole list exists solely
 *       so that it can be cut up. That is {@code .gather(windowFixed(N))} written the long way.
 *       {@code Lists.partition} appears in a dozen more places.
 *   <li><b>Forgetting the tail.</b> {@code PopulateContentletAsJSONUtil.processInsertRecord}
 *       (around 466-471) flushes when the batch reaches {@code MAX_BATCH_SIZE}, and its <i>caller</i>
 *       (around 332-334) has to remember {@code if (!paramsInsert.isEmpty()) doInsertBatch(...)} for
 *       the partly-filled remainder. Two places that must agree, duplicated again for updates.
 *       {@code windowFixed} emits the short final window on its own — watch for the lone
 *       {@code [id7]} in the output below.
 * </ol>
 *
 * <h2>What the JDK ships</h2>
 *
 * {@code windowFixed}, {@code windowSliding}, {@code fold}, {@code scan} and {@code mapConcurrent}.
 * Gatherers are <b>final since Java 24</b> (JEP 485), so none of this needs {@code --enable-preview}.
 *
 * <p>{@code mapConcurrent} is the one that connects this topic to Loom: it runs each element on a
 * <b>virtual thread</b>, under a concurrency limit you choose, and <b>preserves encounter order</b>.
 * It is the JDK's answer to "parallelise the I/O in this stream" — the thing {@code parallelStream()}
 * never did well, because it uses the common ForkJoinPool, sized for CPU work and shared with the
 * whole process.
 *
 * <p><b>Run it</b> (no build required; takes a couple of seconds for the concurrency section):
 *
 * <pre>
 *   java dotCMS/src/test/java/com/dotcms/jdk/StreamGatherersDemo.java
 * </pre>
 *
 * <p>Not a JUnit test on purpose: the console output <em>is</em> the artifact being shown to an
 * audience. It is a {@code main} demo, compiled by the real build so it cannot silently rot, and named
 * {@code *Demo} so surefire skips it. {@code System.out} is deliberate for the same reason; the
 * Logger-only rule targets production code.
 *
 * @author Fabrizio Araya
 * @see <a href="https://openjdk.org/jeps/485">JEP 485 — Stream Gatherers</a>
 * @see CustomCollectorDemo
 */
public final class StreamGatherersDemo {

    /** Stands in for the index names {@code OSIndexAPIImpl} batches before asking OpenSearch. */
    private static final List<String> INDEX_NAMES =
            List.of("idx1", "idx2", "idx3", "idx4", "idx5", "idx6", "idx7");

    private StreamGatherersDemo() {
    }

    public static void main(final String[] args) {
        batching();
        statefulBuiltIns();
        writingYourOwn();
        concurrentMapping();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. The batching case, which is the one already written by hand here
    // ─────────────────────────────────────────────────────────────────────────

    private static void batching() {
        header("1. Batching — and the partial final window nobody remembers to flush");

        System.out.println("  source                " + INDEX_NAMES + "   (7 items, batch size 3)");

        final String label = "  windowFixed(3)        ";
        final String windows = INDEX_NAMES.stream().gather(Gatherers.windowFixed(3)).toList().toString();
        System.out.println(label + windows);
        // Point at the short final window wherever it lands, rather than at a hand-counted column.
        System.out.println(" ".repeat(label.length() + windows.lastIndexOf('['))
                + "^^^^^^^ the short tail, emitted for free");
        System.out.println("  windowSliding(3)      "
                + INDEX_NAMES.stream().gather(Gatherers.windowSliding(3)).toList());
        System.out.println();
        System.out.println("  The list is never materialised: windowFixed is a stream step, so the");
        System.out.println("  batches are produced lazily as the source is consumed. Compare with");
        System.out.println("  collect(toList()) followed by Lists.partition(...), which must build the");
        System.out.println("  whole list first purely in order to cut it up.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. The other built-ins that carry state
    // ─────────────────────────────────────────────────────────────────────────

    private static void statefulBuiltIns() {
        header("2. Carrying state — scan and fold");

        final List<Integer> amounts = List.of(10, 20, 30, 40);
        System.out.println("  source                " + amounts);
        System.out.println("  scan (running total)  "
                + amounts.stream().gather(Gatherers.scan(() -> 0, Integer::sum)).toList()
                + "   <- one output per input");
        System.out.println("  fold (single value)   "
                + amounts.stream().gather(Gatherers.fold(() -> 0, Integer::sum)).toList()
                + "                 <- one output in total");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Writing your own: state, plus a boolean that can end the stream
    // ─────────────────────────────────────────────────────────────────────────

    /** Remembers the previous element, so runs of equal values collapse into one. */
    private static final class Previous<T> {

        private T value;
    }

    /**
     * Collapses consecutive duplicates. Note this is <b>not</b> {@code distinct()}: a value may
     * reappear later, it just may not repeat back to back.
     */
    static <T> Gatherer<T, ?, T> collapsingRuns() {
        return Gatherer.ofSequential(
                Previous<T>::new,
                (state, element, downstream) -> {
                    if (Objects.equals(state.value, element)) {
                        return true;                       // swallow it, keep going
                    }
                    state.value = element;
                    return downstream.push(element);
                });
    }

    /** Remembers that the cut-off condition already fired. */
    private static final class Latch {

        private boolean tripped;
    }

    /**
     * Emits elements until the first {@code ERROR} and then ends the stream. Returning {@code false}
     * from the integrator short-circuits — the rest of the source is never consumed, which no
     * {@code Collector} can do.
     */
    static Gatherer<String, ?, String> untilError() {
        return Gatherer.ofSequential(
                Latch::new,
                (state, element, downstream) -> {
                    if (state.tripped || "ERROR".equals(element)) {
                        state.tripped = true;
                        return false;                      // false ends the stream here
                    }
                    return downstream.push(element);
                });
    }

    private static void writingYourOwn() {
        header("3. Writing your own — initial state + what to do with each element");

        final List<String> statuses = List.of("OK", "OK", "OK", "ERROR", "ERROR", "OK");
        System.out.println("  source                " + statuses);
        System.out.println("  collapsingRuns()      " + statuses.stream().gather(collapsingRuns()).toList()
                + "         <- runs collapse, later repeats survive");
        System.out.println("  untilError()          " + statuses.stream().gather(untilError()).toList()
                + "            <- returning false ends the stream");
        System.out.println();
        System.out.println("  A gatherer can therefore short-circuit, which a Collector cannot: a");
        System.out.println("  collector always drains the whole stream before it can produce anything.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. mapConcurrent — where this topic meets Loom
    // ─────────────────────────────────────────────────────────────────────────

    private static void concurrentMapping() {
        header("4. mapConcurrent — bounded concurrency on virtual threads, order preserved");

        final List<Integer> ids = IntStream.rangeClosed(1, 12).boxed().toList();

        final long startSequential = System.nanoTime();
        final List<String> sequential = ids.stream().map(StreamGatherersDemo::slowIo).toList();
        final long sequentialMs = millisSince(startSequential);

        final long startConcurrent = System.nanoTime();
        final List<String> concurrent =
                ids.stream().gather(Gatherers.mapConcurrent(4, StreamGatherersDemo::slowIo)).toList();
        final long concurrentMs = millisSince(startConcurrent);

        System.out.printf("  sequential            %5d ms%n", sequentialMs);
        System.out.printf("  mapConcurrent(4)      %5d ms%n", concurrentMs);
        System.out.println("  same order as input   " + sequential.equals(concurrent));
        System.out.println("  ran on virtual threads " + ranVirtual(ids));
        System.out.println();
        System.out.println("  The bound is explicit and local. parallelStream() would instead borrow");
        System.out.println("  the common ForkJoinPool — sized for CPU work and shared process-wide —");
        System.out.println("  which is the wrong resource for blocking I/O.");
    }

    /** Stands in for a blocking call: 100 ms of waiting, no CPU. */
    private static String slowIo(final int id) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return "r" + id;
    }

    /** Reports whether mapConcurrent actually ran the mapper on virtual threads. */
    private static boolean ranVirtual(final List<Integer> ids) {
        return ids.stream()
                .gather(Gatherers.mapConcurrent(4, id -> Thread.currentThread().isVirtual()))
                .allMatch(Boolean::booleanValue);
    }

    private static long millisSince(final long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static void header(final String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("-".repeat(78));
    }
}
