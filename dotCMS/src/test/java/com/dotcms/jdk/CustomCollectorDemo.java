package com.dotcms.jdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Live demo for the Java 25 talk: <b>when a custom {@link Collector} is worth writing, and when the
 * JDK already wrote it for you.</b>
 *
 * <p>A collector is four pieces and three type parameters {@code <T, A, R>}: what goes <b>in</b>, the
 * <b>mutable</b> accumulator it builds up internally, and what comes <b>out</b>. That A and R may
 * differ is the whole trick — accumulate into something mutable because that is what is efficient,
 * and let the <i>finisher</i> convert it once, at the end.
 *
 * <pre>
 *   supplier      the bucket                ArrayList::new
 *   accumulator   put one element in it     List::add
 *   combiner      merge two buckets         (a, b) -&gt; { a.addAll(b); return a; }
 *   finisher      turn A into R             List::copyOf
 * </pre>
 *
 * <h2>Part 1 — the one NOT worth writing</h2>
 *
 * {@link #toImmutableList()} is the collector everybody writes first. It works, and it is redundant:
 * {@code Stream.toList()} and {@code Collectors.toUnmodifiableList()} have shipped since Java 16 and
 * 10. It earns its place here only as the anatomy lesson, plus two traps that are worth seeing once:
 *
 * <ul>
 *   <li><b>{@code IDENTITY_FINISH} silently skips the finisher.</b> Declaring it does not fail — the
 *       stream simply never calls the finisher, so a method promising an immutable list hands back a
 *       plain mutable {@code ArrayList}. The characteristic means "the accumulator already <i>is</i>
 *       the result"; declaring it while having a finisher is lying to the stream.
 *   <li><b>Copy and view are not interchangeable.</b> {@code List.copyOf} copies and rejects
 *       {@code null}; {@code Collections.unmodifiableList} wraps and accepts it. In a codebase where
 *       an unset content field arrives as {@code null}, that difference shows up in production, not
 *       in tests.
 * </ul>
 *
 * <h2>Part 2 — the one that IS worth writing</h2>
 *
 * {@link #mergingReportingConflicts} merges a stream into a map <i>and reports which keys collided</i>.
 * {@code Collectors.toMap} offers only two bad answers to a duplicate key: throw
 * {@code IllegalStateException} (losing every other collision), or take a merge function and discard
 * the loser in silence. Neither can tell the caller that something clashed at all.
 *
 * <p>This is the criterion for writing your own: <b>all four pieces have to do real work.</b>
 *
 * <ul>
 *   <li>the <b>supplier</b> builds an accumulator holding two coordinated structures at once — no JDK
 *       collector does that, and {@code Collectors.teeing} cannot, because it feeds two independent
 *       collectors while here the detection <i>is</i> the merge;
 *   <li>the <b>combiner</b> is not decoration: joining two halves in parallel can surface a collision
 *       neither half saw alone;
 *   <li>the <b>finisher</b> returns an immutable, typed {@code record}, so the caller cannot forget to
 *       look at the conflicts — they are in the return type.
 * </ul>
 *
 * <p>The dotCMS-shaped use is merging field maps from several contentlets, or consolidating settings
 * from several sources, where {@code toMap} forces a choice between blowing up and silently
 * overwriting.
 *
 * <h2>The counterexample worth keeping in mind</h2>
 *
 * {@code ContentletIndexAPIImpl.addContentToIndex} splits contentlets three ways by
 * {@code IndexPolicy} with {@code CollectionsUtils.partition} plus positional
 * {@code get(0)/get(1)/get(2)} — two parallel lists the compiler never cross-checks. That one does
 * <b>not</b> want a custom collector: {@code Collectors.groupingBy(Contentlet::getIndexPolicy)} has
 * been the right answer since Java 8. Writing a collector there would be using the new toy for its
 * own sake.
 *
 * <p><b>Run it</b> (no build required):
 *
 * <pre>
 *   java dotCMS/src/test/java/com/dotcms/jdk/CustomCollectorDemo.java
 * </pre>
 *
 * <p>Not a JUnit test on purpose: the console output <em>is</em> the artifact being shown to an
 * audience. It is a {@code main} demo, compiled by the real build so it cannot silently rot, and named
 * {@code *Demo} so surefire skips it. {@code System.out} is deliberate for the same reason; the
 * Logger-only rule targets production code.
 *
 * @author Fabrizio Araya
 * @see CompactObjectHeadersDemo
 */
public final class CustomCollectorDemo {

    private CustomCollectorDemo() {
    }

    public static void main(final String[] args) {
        anatomy();
        identityFinishTrap();
        copyVersusView();
        worthWriting();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1 — anatomy, on a collector you do not actually need
    // ─────────────────────────────────────────────────────────────────────────

    /** The collector everyone writes first. Correct, and already in the JDK twice over. */
    static <T> Collector<T, List<T>, List<T>> toImmutableList() {
        return Collector.of(
                ArrayList::new,                                 // supplier
                List::add,                                      // accumulator
                (left, right) -> { left.addAll(right); return left; },  // combiner
                List::copyOf);                                  // finisher
    }

    /** Same, but freezing with a read-only <i>view</i> instead of a copy. Not the same thing. */
    static <T> Collector<T, List<T>, List<T>> toUnmodifiableView() {
        return Collector.of(
                ArrayList::new,
                List::add,
                (left, right) -> { left.addAll(right); return left; },
                Collections::unmodifiableList);
    }

    private static void anatomy() {
        header("1. Anatomy — supplier, accumulator, combiner, finisher");

        final List<String> names = List.of("ana", "beto", "caro", "dani");
        final List<String> result = names.stream().map(String::toUpperCase)
                .collect(toImmutableList());

        System.out.println("  result                " + result);
        System.out.println("  class                 " + result.getClass().getSimpleName()
                + "   <- no longer an ArrayList: the finisher ran");
        try {
            result.add("EVA");
            System.out.println("  add()                 NO ERROR — the list is not immutable!");
        } catch (UnsupportedOperationException expected) {
            System.out.println("  add()                 UnsupportedOperationException");
        }

        // The combiner is only ever called on a parallel stream. A broken one passes every
        // sequential test and fails the day somebody writes parallelStream().
        final List<String> parallel = names.parallelStream().map(String::toUpperCase)
                .collect(toImmutableList());
        System.out.println("  in parallel           " + parallel + "   (same: "
                + result.equals(parallel) + ")   <- the combiner only runs here");

        System.out.println("  Stream.toList()       " + names.stream().toList()
                + "   <- which is why you rarely need to write this one");
    }

    private static void identityFinishTrap() {
        header("2. Trap — IDENTITY_FINISH silently skips the finisher");

        final Collector<String, List<String>, List<String>> lying = Collector.of(
                ArrayList::new,
                List::add,
                (left, right) -> { left.addAll(right); return left; },
                List::copyOf,
                Collector.Characteristics.IDENTITY_FINISH);   // <- the lie

        final List<String> shouldBeImmutable = Stream.of("a").collect(lying);
        System.out.println("  declared             immutable (the finisher says List::copyOf)");
        System.out.println("  actual class         " + shouldBeImmutable.getClass().getSimpleName());
        shouldBeImmutable.add("b");
        System.out.println("  mutated it           " + shouldBeImmutable
                + "   <- no exception, no warning, no failure anywhere");
    }

    private static void copyVersusView() {
        header("3. Copy vs view — and what each does with null");

        final List<String> withNull = Arrays.asList("a", null);
        System.out.println("  unmodifiableList      " + withNull.stream().collect(toUnmodifiableView())
                + "        <- wraps; nulls survive");
        try {
            withNull.stream().collect(toImmutableList());
            System.out.println("  List.copyOf           accepted null");
        } catch (NullPointerException expected) {
            System.out.println("  List.copyOf           NullPointerException   <- copies; rejects null");
        }
        System.out.println("  Stream.toList()       " + withNull.stream().toList()
                + "        <- immutable AND null-tolerant");
        try {
            withNull.stream().collect(Collectors.toUnmodifiableList());
        } catch (NullPointerException expected) {
            System.out.println("  toUnmodifiableList()  NullPointerException   <- immutable, null-hostile");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2 — the collector worth writing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * What the collector returns: the merged map <i>and</i> the keys that collided. Both immutable, so
     * the caller cannot mutate the result and cannot overlook the conflicts.
     */
    record MergeResult<K, V>(Map<K, V> merged, Set<K> conflicts) {

        boolean clean() {
            return conflicts.isEmpty();
        }
    }

    /** The mutable accumulator. It never escapes the collector — the finisher freezes it. */
    private static final class Acc<K, V> {

        private final Map<K, V> map = new LinkedHashMap<>();
        private final Set<K> conflicts = new LinkedHashSet<>();

        void put(final K key, final V value) {
            // containsKey BEFORE the put: afterwards it is always true. And containsKey rather than
            // `map.put(...) != null`, so a key whose previous value was null still counts as a clash.
            if (map.containsKey(key)) {
                conflicts.add(key);
            }
            map.put(key, value);
        }

        Acc<K, V> merge(final Acc<K, V> other) {
            other.map.forEach(this::put);          // may discover a clash neither half saw alone
            this.conflicts.addAll(other.conflicts);
            return this;
        }
    }

    /**
     * Merges into a map and reports which keys collided — the answer {@code Collectors.toMap} cannot
     * give, since its only options are to throw or to discard the loser in silence.
     */
    static <T, K, V> Collector<T, ?, MergeResult<K, V>> mergingReportingConflicts(
            final Function<T, K> keyFn, final Function<T, V> valueFn) {
        return Collector.of(
                Acc<K, V>::new,
                (acc, element) -> acc.put(keyFn.apply(element), valueFn.apply(element)),
                Acc::merge,
                acc -> new MergeResult<>(
                        Collections.unmodifiableMap(new LinkedHashMap<>(acc.map)),
                        Set.copyOf(acc.conflicts)));
    }

    /** A field contributed by some contentlet — the shape this collector exists for. */
    record Field(String name, String value) {
    }

    private static void worthWriting() {
        header("4. Worth writing — merge a map AND report the collisions");

        final List<Field> fields = List.of(
                new Field("title", "Home"),
                new Field("body", "..."),
                new Field("title", "Home v2"),      // <- the collision
                new Field("author", "ana"));

        final MergeResult<String, String> merged =
                fields.stream().collect(mergingReportingConflicts(Field::name, Field::value));

        System.out.println("  merged                " + merged.merged());
        System.out.println("  conflicts             " + merged.conflicts());
        System.out.println("  clean()               " + merged.clean());
        try {
            merged.merged().put("x", "y");
            System.out.println("  put()                 NO ERROR — not immutable!");
        } catch (UnsupportedOperationException expected) {
            System.out.println("  put()                 UnsupportedOperationException");
        }

        final MergeResult<String, String> parallel =
                fields.parallelStream().collect(mergingReportingConflicts(Field::name, Field::value));
        System.out.println("  in parallel           " + parallel.merged()
                + " conflicts=" + parallel.conflicts() + "   <- the combiner detects them too");

        System.out.println();
        System.out.println("  What Collectors.toMap offers instead:");
        try {
            fields.stream().collect(Collectors.toMap(Field::name, Field::value));
        } catch (IllegalStateException expected) {
            System.out.println("    toMap(k, v)         IllegalStateException"
                    + "   <- and it cannot say how many others clashed");
        }
        System.out.println("    toMap(k, v, merge)  "
                + fields.stream().collect(Collectors.toMap(Field::name, Field::value, (a, b) -> b))
                + "   <- did anything clash? no way to know");
    }

    private static void header(final String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("-".repeat(78));
    }
}
