package com.dotcms.jdk;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Live demo for the Java 25 talk: <b>what {@code -XX:+UseCompactObjectHeaders} (JEP 519) actually
 * costs and buys, measured on dotCMS's own objects.</b>
 *
 * <p>Every Java object carries a fixed prefix the JVM owns and your code never sees: the
 * <i>object header</i>. Classically 12 bytes — an 8-byte mark word (identity hash, lock state, GC
 * age, forwarding pointer; multiplexed, so a plain object leaves most of it unused) plus a 4-byte
 * compressed klass pointer (which class this is). Compact headers narrow the klass pointer to 22
 * bits and fold it <i>into</i> the spare room of the mark word, giving a single <b>8-byte</b> header.
 * Same information, one word less, on every object in the heap.
 *
 * <p><b>The flag is not on by default in JDK 25</b> ({@code UseCompactObjectHeaders = false
 * {product}}). dotCMS opted in deliberately: {@code container/tomcat9/bin/setenv.sh} for the
 * production container and {@code parent/pom.xml} for the surefire/failsafe JVMs.
 *
 * <h2>Why this demo relaunches itself</h2>
 *
 * The flag is read once at JVM startup, so no single process can show both sides and no JUnit test
 * can compare them — which is exactly why {@code CacheSizingUtilTest} was rewritten to assert
 * layout-<i>independent</i> invariants instead of byte counts. This class therefore spawns two child
 * JVMs, identical but for {@code -XX:+/-UseCompactObjectHeaders}, and prints their results side by
 * side.
 *
 * <h2>How it measures</h2>
 *
 * Two independent instruments, neither of which is a sizing API reporting on itself:
 *
 * <ol>
 *   <li><b>Allocation counter</b> (the headline number) — {@code ThreadMXBean
 *       .getCurrentThreadAllocatedBytes()} around the fill loop, divided by N. The JVM counts the
 *       bytes it actually handed out, so this is exact and does not depend on the collector running.
 *   <li><b>Heap delta</b> (the cross-check) — used heap before and after the fill, with the backing
 *       array allocated <i>before</i> the baseline so its own N reference slots are excluded. Noisy
 *       by nature, but it answers the fair question the counter cannot: the bytes are not merely
 *       un-allocated, they are genuinely not resident.
 * </ol>
 *
 * Payloads are shared singletons, so what the first three rows measure is the object shell itself.
 * Both numbers are printed; the headline is snapped to the object alignment, which is exact rather
 * than cosmetic because every footprint — and every sum of footprints — is a multiple of it.
 *
 * <p>When the dotCMS classpath is present the real records
 * {@code com.dotcms.content.index.domain.ContentSearchHit} / {@code SiteSearchHit} are loaded and
 * measured. Standalone, the demo falls back to local twins with an identical field layout and says
 * so in the output.
 *
 * <h2>What to look for on the slide</h2>
 *
 * <pre>
 *   shape                12-byte hdr   8-byte hdr   saved
 *   java.lang.Object            16 B          8 B     8 B   the header and nothing else
 *   ContentSearchHit            40 B         32 B     8 B   5 refs + 1 float = 24 B of real data
 *   SiteSearchHit               40 B         40 B     0 B   one MORE field, and it costs nothing
 *   realistic hit             3176 B       2992 B    184 B   the same hit carrying its _source
 * </pre>
 *
 * <p>Every row is exact and reproducible run to run — that is what the allocation counter buys. The
 * last row is a graph of some sixty objects, so its absolute size depends on how this demo builds the
 * map; what carries over to production is the ratio, not the constant.
 *
 * The second and third rows are the point. {@code SiteSearchHit} carries one extra reference than
 * {@code ContentSearchHit}, yet with 12-byte headers <b>both weigh the same</b>: objects are aligned
 * to 8 bytes, so 12+24=36 rounds up to 40 and the extra field lands in padding that was already paid
 * for. With 8-byte headers the short shape lands exactly on 32 and the long one is the one rounding
 * up — so the very same field now costs 8 bytes.
 *
 * <p>The lesson is not "objects got 4 bytes smaller". It is that <b>compact headers change which
 * refactors are worth doing</b>: splitting these two shapes apart (#36899) returned nothing before
 * the flag and returns 8 bytes per hit after it.
 *
 * <p>The final row keeps that honest, and is the row to end on. Against a hit carrying its own
 * 20-field {@code _source}, the split is worth 0.25% while the flag itself is worth 5.8% —
 * because the flag shrinks <i>every object in the graph</i>, the map nodes and strings and char
 * arrays, not just the record you were looking at. That is also why the payoff shows up in the
 * caches, where dotCMS holds millions of small objects, rather than in any one data class.
 *
 * <p><b>Run it</b> — one command prints both columns:
 *
 * <pre>
 *   # standalone, no build required (uses local twins)
 *   java dotCMS/src/test/java/com/dotcms/jdk/CompactObjectHeadersDemo.java
 *
 *   # against the real dotCMS records (after ./mvnw install -pl :dotcms-core --am -DskipTests)
 *   java -cp dotCMS/target/classes:dotCMS/target/test-classes com.dotcms.jdk.CompactObjectHeadersDemo
 * </pre>
 *
 * Options: {@code -Dn=1000000} instances per shape, {@code -Dchild=on|off} to run a single side in
 * the current JVM instead of spawning children.
 *
 * <p>Not a JUnit test on purpose: the whole point is a comparison across two JVMs, and a test runs in
 * the one surefire started — which is precisely why {@code CacheSizingUtilTest} had to give up on byte
 * counts. It is a {@code main} demo, compiled by the real build so it cannot silently rot, and named
 * {@code *Demo} so surefire skips it. {@code System.out} is deliberate: the console output <em>is</em>
 * the artifact being shown to an audience; the Logger-only rule targets production code.
 *
 * @author Fabrizio Araya
 * @see <a href="https://openjdk.org/jeps/519">JEP 519 — Compact Object Headers</a>
 * @see VirtualThreadCarrierTimelineDemo
 */
public final class CompactObjectHeadersDemo {

    /** Instances allocated per shape. Large enough that per-object rounding is invisible. */
    private static final int N = Integer.getInteger("n", 1_000_000);

    /** Instances allocated and discarded before measuring, to move JIT/reflection warmup out of the window. */
    private static final int WARMUP = 20_000;

    /** Payloads shared by every instance, so only the object shell shows up in the heap delta. */
    private static final String SHARED_ID = "shared-id";
    private static final String SHARED_INDEX = "shared-index";
    private static final Map<String, Object> SHARED_MAP = Map.of();
    private static final List<Object> SHARED_LIST = List.of();

    private CompactObjectHeadersDemo() {
    }

    public static void main(final String[] args) throws Exception {
        final String child = System.getProperty("child");
        if (child != null) {
            runOneSide();
            return;
        }
        runBothSides();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shapes under measurement
    // ─────────────────────────────────────────────────────────────────────────

    /** Local stand-in for {@code ContentSearchHit}: 5 references + 1 float. */
    record ContentHitTwin(String getId, String getIndex, Map<String, Object> getSourceAsMap,
                          float getScore, Map<String, Object> getFields, List<Object> getSortValues) {
    }

    /** Local stand-in for {@code SiteSearchHit}: the same plus one highlights reference. */
    record SiteHitTwin(String getId, String getIndex, Map<String, Object> getSourceAsMap,
                       float getScore, Map<String, Object> getFields, List<Object> getSortValues,
                       Map<String, List<String>> getHighlights) {
    }

    /**
     * One shape to measure.
     *
     * @param divisor how much to scale N down for this shape — a shape that retains kilobytes cannot
     *                be allocated a million times, and does not need to be: the measurement noise is
     *                a fraction of a byte per instance either way.
     */
    private record Shape(String label, String note, int divisor, Supplier<Object> factory) {
    }

    private static List<Shape> shapes() {
        final List<Shape> shapes = new ArrayList<>();

        shapes.add(new Shape("java.lang.Object", "header only, no fields", 1, Object::new));

        final Supplier<Object> realContent = realHitFactory(
                "com.dotcms.content.index.domain.ContentSearchHit", false);
        final Supplier<Object> realSite = realHitFactory(
                "com.dotcms.content.index.domain.SiteSearchHit", true);

        shapes.add(new Shape("ContentSearchHit",
                realContent != null ? "real record, 5 refs + 1 float" : "TWIN, 5 refs + 1 float", 1,
                realContent != null ? realContent
                        : () -> new ContentHitTwin(SHARED_ID, SHARED_INDEX, SHARED_MAP, 1.0f,
                                SHARED_MAP, SHARED_LIST)));

        shapes.add(new Shape("SiteSearchHit",
                realSite != null ? "real record, 6 refs + 1 float" : "TWIN, 6 refs + 1 float", 1,
                realSite != null ? realSite
                        : () -> new SiteHitTwin(SHARED_ID, SHARED_INDEX, SHARED_MAP, 1.0f,
                                SHARED_MAP, SHARED_LIST, Map.of())));

        shapes.add(new Shape("realistic hit", "+ its own 20-field _source map", 20,
                CompactObjectHeadersDemo::realisticHit));

        return shapes;
    }

    /**
     * Builds a factory for a real dotCMS hit record when it is on the classpath, or {@code null}
     * when running standalone. The canonical constructor is
     * {@code (String, String, Map, float, Map, List[, Map])}.
     */
    private static Supplier<Object> realHitFactory(final String className, final boolean withHighlights) {
        try {
            final Class<?> type = Class.forName(className);
            final Constructor<?> ctor = withHighlights
                    ? type.getDeclaredConstructor(String.class, String.class, Map.class, float.class,
                            Map.class, List.class, Map.class)
                    : type.getDeclaredConstructor(String.class, String.class, Map.class, float.class,
                            Map.class, List.class);
            final Object[] argv = withHighlights
                    ? new Object[] {SHARED_ID, SHARED_INDEX, SHARED_MAP, 1.0f, SHARED_MAP,
                            SHARED_LIST, SHARED_MAP}
                    : new Object[] {SHARED_ID, SHARED_INDEX, SHARED_MAP, 1.0f, SHARED_MAP,
                            SHARED_LIST};
            ctor.newInstance(argv); // fail fast here rather than inside the measurement loop
            return () -> {
                try {
                    return ctor.newInstance(argv);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            };
        } catch (ReflectiveOperationException | LinkageError notOnClasspath) {
            return null;
        }
    }

    /**
     * A hit as it actually arrives from the engine: the record shell plus a private 20-entry
     * {@code _source} map. Nothing is shared, so the heap delta is the whole retained cost — which is
     * the honesty check against the shell-only rows above.
     */
    private static Object realisticHit() {
        final Map<String, Object> source = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            source.put("field_" + i, "value_" + i);
        }
        return new SiteHitTwin("id-" + source.hashCode(), "working-index", source, 1.0f,
                new HashMap<>(), List.of(), Map.of());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Single side: measure in THIS JVM and print machine-readable rows
    // ─────────────────────────────────────────────────────────────────────────

    private static final ThreadMXBean THREADS =
            (ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static void runOneSide() {
        for (final Shape shape : shapes()) {
            final int instances = N / shape.divisor();
            final double allocated = allocatedBytesPerInstance(shape.factory(), instances);
            final double heap = heapDeltaPerInstance(shape.factory(), instances);
            System.out.println("ROW\t" + shape.label() + "\t" + snapToAlignment(allocated) + "\t"
                    + String.format("%.2f", allocated) + "\t" + String.format("%.0f", heap) + "\t"
                    + shape.note());
        }
    }

    /**
     * Bytes the JVM handed this thread per instance — the exact figure. Warmup runs first so the JIT
     * and the reflection caches do their one-time allocation outside the measured window, and the
     * backing array is allocated before it so its own slots are not counted.
     */
    private static double allocatedBytesPerInstance(final Supplier<Object> factory, final int instances) {
        warmUp(factory);
        final Object[] holder = new Object[instances];

        final long before = THREADS.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < instances; i++) {
            holder[i] = factory.get();
        }
        final long after = THREADS.getCurrentThreadAllocatedBytes();

        if (holder[instances - 1] == null) {
            throw new IllegalStateException("unreachable");
        }
        return (double) (after - before) / instances;
    }

    /**
     * Rounds a measurement to the nearest multiple of {@code ObjectAlignmentInBytes} (8 here).
     *
     * <p>This is not cosmetic. The JVM pads every object up to the alignment, so a true footprint is
     * always a multiple of it — and so is any sum of them. The heap delta below carries a fraction of
     * a byte of noise per instance (JIT and reflection allocate a little of their own), which snapping
     * removes without inventing anything. The raw average is printed too so the correction is visible.
     */
    private static long snapToAlignment(final double raw) {
        final long alignment = Long.parseLong(vmOption("ObjectAlignmentInBytes"));
        return Math.round(raw / alignment) * alignment;
    }

    /** Lets the JIT compile the factory and reflection build its caches before anything is measured. */
    private static void warmUp(final Supplier<Object> factory) {
        final Object[] warmup = new Object[WARMUP];
        for (int i = 0; i < WARMUP; i++) {
            warmup[i] = factory.get();
        }
        if (warmup[WARMUP - 1] == null) {
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * Heap delta per instance — the independent cross-check. The backing array is allocated and
     * settled <i>before</i> the fill, so its own reference slots are excluded. This one is genuinely
     * noisy: it depends on the collector having settled, and it under-reports for shapes that retain
     * large graphs. Read it as confirmation of the allocation counter, never instead of it.
     */
    private static double heapDeltaPerInstance(final Supplier<Object> factory, final int instances) {
        warmUp(factory);
        final Object[] holder = new Object[instances];
        final long baseline = settledHeap();

        for (int i = 0; i < instances; i++) {
            holder[i] = factory.get();
        }

        final long filled = settledHeap();
        final double perInstance = (double) (filled - baseline) / instances;

        // Keep the array strongly reachable across the second measurement.
        if (holder[instances - 1] == null) {
            throw new IllegalStateException("unreachable");
        }
        return perInstance;
    }

    /** Used heap after coaxing the collector, so the delta reflects live objects only. */
    private static long settledHeap() {
        final Runtime runtime = Runtime.getRuntime();
        long used = Long.MAX_VALUE;
        for (int attempt = 0; attempt < 6; attempt++) {
            System.gc();
            final long now = runtime.totalMemory() - runtime.freeMemory();
            if (now >= used) {
                break; // stopped shrinking
            }
            used = now;
        }
        return used;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parent side: spawn both children and print the comparison
    // ─────────────────────────────────────────────────────────────────────────

    private static void runBothSides() throws Exception {
        System.out.println();
        System.out.println("Compact object headers (JEP 519) — measured on " + N + " instances per shape");
        System.out.println("JVM: " + Runtime.version() + "   compressed oops: " + vmOption("UseCompressedOops")
                + "   object alignment: " + vmOption("ObjectAlignmentInBytes") + " bytes");
        System.out.println("Default for UseCompactObjectHeaders on this JDK: " + vmOption("UseCompactObjectHeaders")
                + "   (dotCMS turns it on in setenv.sh and parent/pom.xml)");

        final Map<String, String> classicRaw = new LinkedHashMap<>();
        final Map<String, String> compactRaw = new LinkedHashMap<>();
        final Map<String, Long> classic = measureIn("-XX:-UseCompactObjectHeaders", classicRaw);
        final Map<String, Long> compact = measureIn("-XX:+UseCompactObjectHeaders", compactRaw);
        final Map<String, String> notes = new LinkedHashMap<>();
        shapes().forEach(s -> notes.put(s.label(), s.note()));

        System.out.println();
        System.out.printf("%-20s %14s %14s %10s   %s%n",
                "shape", "12-byte hdr", "8-byte hdr", "saved", "");
        System.out.println("-".repeat(86));
        for (final String label : notes.keySet()) {
            final long before = classic.getOrDefault(label, -1L);
            final long after = compact.getOrDefault(label, -1L);
            System.out.printf("%-20s %11d B %11d B %8d B   %s%n",
                    label, before, after, before - after, notes.get(label));
        }
        System.out.println();
        System.out.println("Read the two hit rows together: one extra reference field costs "
                + (classic.getOrDefault("SiteSearchHit", 0L) - classic.getOrDefault("ContentSearchHit", 0L))
                + " B with the classic header and "
                + (compact.getOrDefault("SiteSearchHit", 0L) - compact.getOrDefault("ContentSearchHit", 0L))
                + " B with the compact one.");
        System.out.println("Same field, same code. 8-byte alignment decides what you actually pay.");

        final long hitBefore = classic.getOrDefault("realistic hit", 0L);
        final long hitAfter = compact.getOrDefault("realistic hit", 0L);
        final long splitSaving = classic.getOrDefault("SiteSearchHit", 0L)
                - compact.getOrDefault("ContentSearchHit", 0L);
        System.out.println();
        System.out.printf("But keep the magnitudes straight. On a hit that carries its own _source, "
                        + "the flag returns %d B of %d (%.1f%%), while splitting the two record shapes "
                        + "apart returns %d B of the same %d (%.2f%%).%n",
                hitBefore - hitAfter, hitBefore, 100.0 * (hitBefore - hitAfter) / hitBefore,
                splitSaving, hitBefore, 100.0 * splitSaving / hitBefore);
        System.out.println("The flag pays off because it shrinks EVERY object in the graph — the map "
                + "nodes, the strings, the char arrays — not because it shrank your record.");
        System.out.println();
        System.out.println("Unrounded measurements — allocation counter, then the heap-delta cross-check:");
        System.out.printf("    %-20s %26s   %26s%n", "", "12-byte header", "8-byte header");
        System.out.printf("    %-20s %13s %12s   %13s %12s%n",
                "", "allocated", "heap", "allocated", "heap");
        for (final String label : notes.keySet()) {
            System.out.printf("    %-20s %s   %s%n",
                    label, classicRaw.get(label), compactRaw.get(label));
        }
        System.out.println();
    }

    /** Runs this class in a child JVM with the given flag and parses its ROW lines. */
    private static Map<String, Long> measureIn(final String headerFlag, final Map<String, String> raw)
            throws Exception {
        final String java = ProcessHandle.current().info().command()
                .orElse(System.getProperty("java.home") + "/bin/java");

        final List<String> command = new ArrayList<>(List.of(java,
                headerFlag,
                "-Xmx2g",
                "-Dchild=" + headerFlag,
                "-Dn=" + N));

        // Launched as `java Demo.java`, this class lives in a memory class loader and no child could
        // find it on a class path — so hand the child the source file and let it compile it again.
        final String sourceFile = System.getProperty("jdk.launcher.sourcefile");
        if (sourceFile != null) {
            command.add(sourceFile);
        } else {
            // --enable-preview: dotCMS compiles with it, and preview-marked classes refuse to load
            // without it. Harmless when the class path holds no preview classes.
            command.addAll(List.of("--enable-preview",
                    "-cp", System.getProperty("java.class.path"),
                    CompactObjectHeadersDemo.class.getName()));
        }

        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final Map<String, Long> rows = new LinkedHashMap<>();
        final List<String> transcript = new ArrayList<>();
        try (var reader = process.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                transcript.add(line);
                if (line.startsWith("ROW\t")) {
                    final String[] parts = line.split("\t");
                    rows.put(parts[1], Long.parseLong(parts[2]));
                    raw.put(parts[1], String.format("%10s B  %10s B", parts[3], parts[4]));
                }
            }
        }
        if (process.waitFor() != 0 || rows.isEmpty()) {
            transcript.forEach(System.out::println);
            throw new IllegalStateException("child JVM failed for " + headerFlag);
        }
        return rows;
    }

    private static String vmOption(final String name) {
        try {
            return ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                    .getVMOption(name).getValue();
        } catch (RuntimeException unsupported) {
            return "n/a";
        }
    }
}
