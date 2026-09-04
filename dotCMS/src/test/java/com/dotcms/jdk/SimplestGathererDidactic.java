package com.dotcms.jdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Gatherer;

/**
 * The smallest possible gatherer, taken apart — the teaching companion to {@link StreamGatherersDemo},
 * which shows what gatherers are <i>for</i> rather than how to write one.
 *
 * <p>Start with {@link #passThrough()}. It does nothing at all, and that is the point: it already
 * contains the entire API, so there is nothing else to learn afterwards.
 *
 * <pre>
 *   static Gatherer&lt;String, Void, String&gt; passThrough() {
 *       return Gatherer.of((state, element, downstream) -&gt; {
 *           downstream.push(element);
 *           return true;
 *       });
 *   }
 * </pre>
 *
 * <h2>The three type parameters</h2>
 *
 * {@code Gatherer<String, Void, String>} reads: what goes <b>in</b>, the <b>state</b>, what comes
 * <b>out</b>. {@code Void} means "I do not need to remember anything" — the simplest case, and the
 * reason {@link Gatherer#of(Gatherer.Integrator)} takes a single lambda with no state supplier.
 *
 * <h2>The three things the lambda receives</h2>
 *
 * <pre>
 *   state        your memory between elements — null here, because Void
 *   element      the one going past right now
 *   downstream   the rest of the pipeline; you hand it results with push()
 * </pre>
 *
 * And the returned {@code boolean} means <b>"keep going"</b>. Returning {@code false} ends the stream
 * on the spot, without consuming the rest of the source — something no {@code Collector} can do.
 *
 * <h2>Why this one shape covers the old operations</h2>
 *
 * The only real decision is <b>how many times you call {@code push}</b>. Each method below differs
 * from {@code passThrough} by a single line:
 *
 * <pre>
 *   source        [ana, beto, caro]
 *   passThrough   [ana, beto, caro]                        push once, unchanged
 *   onlyLong      [beto, caro]                             push zero or one time  -&gt; behaves like filter
 *   upperCase     [ANA, BETO, CARO]                        push something else    -&gt; behaves like map
 *   twice         [ana, ana, beto, beto, caro, caro]       push more than once    -&gt; behaves like flatMap
 * </pre>
 *
 * So a gatherer can do everything the pre-existing operations do. What makes it <i>new</i> is the
 * first parameter, {@code state}.
 *
 * <h2>Then: state, the parameter that was being ignored</h2>
 *
 * {@link #numbered()} and {@link #whenChanged()} stop ignoring it. Three things change, and only
 * three:
 *
 * <pre>
 *   Gatherer&lt;String, Counter, String&gt;    the middle type is no longer Void
 *   Gatherer.ofSequential(Counter::new,  a supplier for the initial state comes first
 *       (counter, element, downstream)   the same lambda — but the first parameter now means something
 * </pre>
 *
 * <p>The state must be an <b>object whose field you mutate</b>, never a plain {@code int}: the
 * gatherer hands the same instance to every invocation, so the memory has to live inside something
 * that survives between them. {@code ofSequential} is the honest factory here — order-dependent state
 * cannot be split across threads, and it says so instead of asking for a combiner that could not be
 * written correctly.
 *
 * <pre>
 *   numbered      [1. ana, 2. beto, 3. caro]     impossible with map:    output depends on position
 *   whenChanged   [ok, error, ok]                impossible with filter: predicate sees one element
 * </pre>
 *
 * That is the whole idea. Batching, sliding windows and running totals are this same shape with a
 * richer piece of state — see {@link StreamGatherersDemo}, which also shows what the JDK already
 * ships so you do not write them yourself.
 *
 * <p><b>Run it</b> (no build required):
 *
 * <pre>
 *   java dotCMS/src/test/java/com/dotcms/jdk/SimplestGathererDidactic.java
 * </pre>
 *
 * <p>{@code System.out} is deliberate: the console output <em>is</em> the artifact being shown. Not a
 * JUnit test, and named so surefire skips it, matching the other demos in this package.
 *
 * @author Fabrizio Araya
 * @see StreamGatherersDemo
 */
public final class SimplestGathererDidactic {

    private static final List<String> SOURCE = List.of("ana", "beto", "caro");

    /** Has a run of repeats in the middle, and one value that comes back later. */
    private static final List<String> REPEATED = List.of("ok", "ok", "ok", "error", "error", "ok");

    /** Words that come back, so a per-word tally has something to count. */
    private static final List<String> WITH_REPEATS =
            List.of("ana", "beto", "ana", "caro", "ana", "beto");

    private SimplestGathererDidactic() {
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 0 — the whole API, doing nothing
    // ─────────────────────────────────────────────────────────────────────────

    static Gatherer<String, Void, String> passThrough() {
        return Gatherer.of((state, element, downstream) -> {
            downstream.push(element);   // hand it to the rest of the pipeline
            return true;                // true = carry on with the next element
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Steps 1-3 — one line different each time
    // ─────────────────────────────────────────────────────────────────────────

    /** Push zero or one time, and you have written {@code filter}. */
    static Gatherer<String, Void, String> onlyLong() {
        return Gatherer.of((state, element, downstream) -> {
            if (element.length() > 3) {
                downstream.push(element);
            }
            return true;
        });
    }

    /** Push something other than what came in, and you have written {@code map}. */
    static Gatherer<String, Void, String> upperCase() {
        return Gatherer.of((state, element, downstream) -> {
            downstream.push(element.toUpperCase());
            return true;
        });
    }

    /** Push more than once, and you have written the expanding half of {@code flatMap}. */
    static Gatherer<String, Void, String> twice() {
        return Gatherer.of((state, element, downstream) -> {
            downstream.push(element);
            downstream.push(element);
            return true;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 4 — using the parameter that was being ignored: state
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The state has to be an <b>object whose field you mutate</b>, never a plain {@code int}. The
     * gatherer hands the same instance to every invocation, and the lambda cannot reassign its own
     * parameter in a way the next invocation would see — so the memory has to live <i>inside</i>
     * something. One tiny class is the clearest form of that.
     */
    private static final class Counter {

        private int seen;
    }

    /**
     * Numbers the elements. Impossible with {@code map}: numbering depends on how many went past
     * before, and {@code map} sees one element with no idea of its position.
     */
    static Gatherer<String, Counter, String> numbered() {
        return Gatherer.ofSequential(
                Counter::new,                                   // 1. the initial state
                (counter, element, downstream) -> {             // 2. same lambda as before
                    counter.seen++;                             //    ...but now it remembers
                    return downstream.push(counter.seen + ". " + element);
                });
    }

    /** State does not have to be a number: here it is the previous element. */
    private static final class Previous {

        private String value;
    }

    /**
     * Emits an element only when it differs from the one just before it. Note this is <b>not</b>
     * {@code distinct()}: a value that comes back later is emitted again, it just may not repeat back
     * to back. And it is not {@code filter} either — a predicate sees one element and cannot know what
     * preceded it.
     */
    static Gatherer<String, Previous, String> whenChanged() {
        return Gatherer.ofSequential(
                Previous::new,
                (previous, element, downstream) -> {
                    if (element.equals(previous.value)) {
                        return true;                            // swallow it, but keep going
                    }
                    previous.value = element;
                    return downstream.push(element);
                });
    }

    /** State does not have to be one value: here it is a tally, one counter per distinct word. */
    private static final class Tally {

        private final Map<String, Integer> timesSeen = new LinkedHashMap<>();
    }

    /**
     * Says how many times each word has appeared <b>so far</b>. Same shape as {@link #numbered()} —
     * the only change is that the state went from one counter to one counter per word.
     *
     * <p>Note what it can and cannot answer. Walking the stream it knows "this is the 2nd {@code ana}",
     * because that is settled by the time the element goes past. It cannot say "{@code ana} appears
     * twice in total": the total is only known once the source is exhausted, and by then every element
     * has already been pushed. Emitting the totals is the job of a <i>finisher</i>, the next step.
     */
    static Gatherer<String, Tally, String> occurrence() {
        return Gatherer.ofSequential(
                Tally::new,
                (tally, element, downstream) -> {
                    // merge returns the NEW value, so this counts and reads in one call
                    final int times = tally.timesSeen.merge(element, 1, Integer::sum);
                    return downstream.push(element + " (" + times + ")");
                });
    }

    public static void main(final String[] args) {
        System.out.println();
        System.out.println("The same gatherer, one line apart");
        System.out.println("-".repeat(78));
        print("source", SOURCE);
        print("passThrough", SOURCE.stream().gather(passThrough()).toList());
        print("onlyLong", SOURCE.stream().gather(onlyLong()).toList());
        print("upperCase", SOURCE.stream().gather(upperCase()).toList());
        print("twice", SOURCE.stream().gather(twice()).toList());
        System.out.println();
        System.out.println("  The only decision is how many times you call push().");

        System.out.println();
        System.out.println("Now using state — the parameter ignored above");
        System.out.println("-".repeat(78));
        print("source", SOURCE);
        print("numbered", SOURCE.stream().gather(numbered()).toList());
        System.out.println();
        print("source", REPEATED);
        print("whenChanged", REPEATED.stream().gather(whenChanged()).toList());
        System.out.println();
        print("source", WITH_REPEATS);
        print("occurrence", WITH_REPEATS.stream().gather(occurrence()).toList());
        System.out.println();
        System.out.println("  numbered() cannot be written with map: the output depends on how many");
        System.out.println("  elements went past before this one. whenChanged() cannot be written with");
        System.out.println("  filter: a predicate sees one element and nothing about its neighbours.");
        System.out.println();
    }

    private static void print(final String label, final List<String> value) {
        System.out.printf("  %-14s %s%n", label, value);
    }
}
