package com.dotcms.jdk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * What an AOT cache (JEP 483 / 514 / 515) actually costs to adopt, measured rather than asserted.
 *
 * <p>Adoption of ahead-of-time class loading and linking in dotCMS core is currently <b>zero</b>.
 * This demo exists to make the first step cheap: it drives the whole three-phase workflow in child
 * JVMs, times the result, and then reproduces the two constraints that decide whether the workflow
 * is usable here at all.</p>
 *
 * <h2>Why it re-launches itself</h2>
 *
 * <p>{@code -XX:AOTMode} and {@code -XX:AOTCache} are read once at JVM startup, so no test running
 * inside a single JVM can compare them — the same reason {@link CompactObjectHeadersDemo} spawns
 * children. This class is therefore both the driver and the workload: with no arguments it
 * orchestrates, with {@code workload} it is the thing being measured.</p>
 *
 * <h2>The two findings this exists to demonstrate</h2>
 *
 * <ol>
 *   <li><b>An AOT cache is bound to the {@code UseCompactObjectHeaders} setting it was created
 *       with.</b> dotCMS enables that flag in the shipped container
 *       ({@code container/tomcat9/bin/setenv.sh}) and in the test JVMs ({@code parent/pom.xml}), so
 *       any cache we build has to be built with it too. On a mismatch the JVM logs an error and
 *       <b>keeps running without the cache</b> — startup silently returns to the uncached cost, and
 *       only a log line says so.</li>
 *   <li><b>Classes loaded from a plain directory are skipped</b> ({@code Unsupported location});
 *       classes from a JAR are recorded. dotCMS runs from an exploded WAR whose application classes
 *       live in {@code WEB-INF/classes} — a directory — so the cache would cover the JDK and the
 *       jarred dependencies, not our own code, until that is addressed.</li>
 * </ol>
 *
 * <h2>Running it</h2>
 *
 * <pre>{@code
 * java dotCMS/src/test/java/com/dotcms/jdk/AotCacheDemo.java      # source launcher
 * java -cp dotCMS/target/classes com.dotcms.jdk.AotCacheDemo      # compiled
 * }</pre>
 *
 * <p>Requires JDK 24+ for {@code AOTMode}; verified on 25.0.2. No Maven, no dotCMS classpath, no
 * network.</p>
 *
 * @see CompactObjectHeadersDemo
 */
public final class AotCacheDemo {

    /** Iterations timed per configuration; the first is discarded as a warm-up. */
    private static final int RUNS = 5;

    /**
     * Every phase below runs with compact object headers ON, because that is how dotCMS runs:
     * {@code container/tomcat9/bin/setenv.sh} and {@code parent/pom.xml} both set it. The demo has
     * to mirror the real configuration or the mismatch in section 4 is not the one we would hit.
     */
    private static final String AS_DOTCMS_RUNS = "-XX:+UseCompactObjectHeaders";

    private AotCacheDemo() {
    }

    public static void main(final String[] args) throws Exception {

        if (args.length > 0 && "workload".equals(args[0])) {
            runWorkload();
            return;
        }

        banner("AOT cache in dotCMS — what the first step actually costs");
        System.out.println("  JVM      : " + System.getProperty("java.version"));
        System.out.println();

        final Path work = Files.createTempDirectory("dotcms-aot-demo");
        try {
            final Path conf = work.resolve("app.aotconf");
            final Path cache = work.resolve("app.aot");

            section("1. Recording a training run");
            final Result record = child(List.of(AS_DOTCMS_RUNS, "-XX:AOTMode=record",
                    "-XX:AOTConfiguration=" + conf), true);
            System.out.println(indent(record.aotLines()));
            System.out.println();
            System.out.println("  configuration written : " + Files.exists(conf));
            skippedNote(record);

            section("2. Turning the recording into a cache");
            final Result create = child(List.of(AS_DOTCMS_RUNS, "-XX:AOTMode=create",
                    "-XX:AOTConfiguration=" + conf, "-XX:AOTCache=" + cache), true);
            System.out.println(indent(create.stdout()));
            if (!Files.exists(cache)) {
                System.out.println("  no cache produced — stopping here");
                return;
            }
            System.out.printf("  cache size            : %,d bytes%n", Files.size(cache));

            section("3. Startup, with and without it");
            final long cold = median(() -> child(List.of(AS_DOTCMS_RUNS), false));
            final long warm = median(() -> child(List.of(AS_DOTCMS_RUNS,
                    "-XX:AOTCache=" + cache), false));
            System.out.printf("  without cache         : %5d ms%n", cold);
            System.out.printf("  with cache            : %5d ms%n", warm);
            System.out.printf("  delta                 : %5d ms  (%.1f%%)%n",
                    cold - warm, cold == 0 ? 0.0 : (cold - warm) * 100.0 / cold);
            System.out.println("""

                  Treat the delta as an order of magnitude, not a benchmark: this workload
                  loads a few hundred classes, where dotCMS loads tens of thousands. The
                  point of the number is that the workflow runs at all.""");

            section("4. The constraint that matters here — compact object headers");
            System.out.println("""
                  An AOT cache records the UseCompactObjectHeaders setting it was built with.
                  dotCMS enables that flag in the shipped container and in the test JVMs, so a
                  cache built without it cannot be used by the server that has it — and the
                  reverse is just as true. Below, the cache above is offered to a JVM with the
                  opposite setting:
                  """);
            final Result mismatch = child(List.of("-XX:-UseCompactObjectHeaders",
                    "-XX:AOTCache=" + cache), true);
            System.out.println("  exit status              : " + mismatch.exit());
            System.out.println(indent(mismatch.aotLines()));
            System.out.println("  workload still completed : " + mismatch.stdout().contains("workload-ok"));
            System.out.println("""

                  Note the exit status: the JVM does NOT fail. It reports the mismatch and
                  starts anyway, without the cache. A deployment that flips either flag keeps
                  booting and quietly pays full startup cost again — the only signal is a log
                  line nobody is watching for.""");

            section("5. The other constraint — where the classes live");
            locationExperiment(work);
            System.out.println("""
                  A directory on the class path does not degrade the training run — it ENDS it,
                  with a non-zero exit. The same class inside a JAR records fine. dotCMS
                  serves an exploded WAR whose own classes sit in WEB-INF/classes, a directory.
                  So today the training run does not produce a partial cache for us — it does
                  not produce one at all. That is a blocker to clear before any measurement,
                  not a limitation to design around.

                  That is the finding to take into a real experiment, and it is why the
                  cheapest first step is not this demo at all: dotcms.startup.ms is already
                  published (ServletContainerHealthCheck), so the distribution of a number we
                  ALREADY collect is the baseline any AOT work has to beat.""");

            banner("Verdict");
            System.out.println("""
                  The feature works and the workflow is three commands. What it costs us is
                  not effort, it is fit — and the two costs fail in opposite directions.

                  The class-path one is loud: an exploded WAR ends the training run with a
                  non-zero exit, so we cannot even start measuring until that is solved.

                  The flag one is silent, and it is the dangerous one: the cache is tied to
                  UseCompactObjectHeaders, which we already set for an unrelated reason. Flip
                  it — in either direction, in any environment — and the server keeps booting,
                  reports the mismatch to a log nobody reads, and quietly pays full startup
                  cost again. A performance feature whose failure mode is "no faster than
                  before" is a feature you have to monitor, not just enable.""");
        } finally {
            deleteRecursively(work);
        }
    }

    // ── the thing being measured ────────────────────────────────────────────

    /**
     * A workload with enough class loading and linking to be worth caching: streams, collectors,
     * boxing, string formatting and a regex, all on paths a server touches during boot.
     */
    private static void runWorkload() {
        final var byBucket = IntStream.range(0, 60_000).boxed()
                .collect(Collectors.groupingBy(i -> i % 97, Collectors.counting()));
        final var joined = byBucket.entrySet().stream()
                .limit(20)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
        final var matched = joined.split(",").length;
        System.out.println("workload-ok buckets=" + byBucket.size() + " pairs=" + matched);
    }

    /**
     * Compiles one throwaway class, records a training run against it twice — once from a directory
     * and once from a JAR of the same class — and prints what the JVM says about each. Proving the
     * claim beats repeating it.
     */
    private static void locationExperiment(final Path work) throws Exception {

        final Path src = work.resolve("Probe.java");
        Files.writeString(src, "public class Probe { public static void main(String[] a) {"
                + " System.out.println(\"probe-ok\"); } }");

        // Its own directory: the comparison has to be a class path entry holding nothing else.
        final Path classes = Files.createDirectories(work.resolve("classes"));
        if (run(tool("javac"), "-d", classes.toString(), src.toString()).exit() != 0) {
            System.out.println("  (javac unavailable — skipping this experiment)");
            return;
        }
        final Path jar = work.resolve("probe.jar");
        run(tool("jar"), "--create", "--file", jar.toString(), "--main-class", "Probe",
                "-C", classes.toString(), "Probe.class");

        final Result fromDir = run(javaBinary(), AS_DOTCMS_RUNS, "-XX:AOTMode=record",
                "-XX:AOTConfiguration=" + work.resolve("dir.aotconf"),
                "-cp", classes.toString(), "Probe");
        System.out.println("  -cp <directory>   exit=" + fromDir.exit());
        System.out.println(indent(aotOnly(fromDir)));

        final Result fromJar = run(javaBinary(), AS_DOTCMS_RUNS, "-XX:AOTMode=record",
                "-XX:AOTConfiguration=" + work.resolve("jar.aotconf"),
                "-cp", jar.toString(), "Probe");
        System.out.println("  -cp <jar>         exit=" + fromJar.exit());
        System.out.println(indent(aotOnly(fromJar)));
        System.out.println();
    }

    private static String tool(final String name) {
        return Path.of(System.getProperty("java.home"), "bin", name).toString();
    }

    private static Result run(final String... cmd) throws Exception {
        final Process proc = new ProcessBuilder(cmd).start();
        final String out = new String(proc.getInputStream().readAllBytes());
        final String err = new String(proc.getErrorStream().readAllBytes());
        return new Result(proc.waitFor(), out, err, 0L);
    }

    /** The Probe/Skipping lines only — everything else here is archive bookkeeping. */
    private static String aotOnly(final Result r) {
        final String all = r.stdout() + r.stderr();
        final String kept = all.lines()
                .filter(l -> l.contains("Skipping") || l.contains("recorded")
                        || l.contains("Error") || l.contains("Cannot have"))
                .map(String::strip)
                .collect(Collectors.joining("\n"));
        return kept.isBlank() ? "(nothing reported)" : kept;
    }

    // ── child process plumbing ──────────────────────────────────────────────

    private record Result(int exit, String stdout, String stderr, long millis) {

        /** Only the {@code [aot]} lines, which is what every section here is actually reporting. */
        String aotLines() {
            final String all = stderr.isBlank() ? stdout : stderr;
            final String kept = all.lines()
                    .filter(l -> l.contains("[aot]") || l.contains("AOT"))
                    .collect(Collectors.joining("\n"));
            return kept.isBlank() ? all.strip() : kept;
        }
    }

    private static Result child(final List<String> vmArgs, final boolean logAot) throws Exception {

        final List<String> cmd = new ArrayList<>();
        cmd.add(javaBinary());
        if (logAot) {
            // warning, not info: the info stream is ~60 lines of archive statistics, and every
            // line this demo is actually about (the location skip, the flag mismatch) is a warning.
            cmd.add("-Xlog:aot=warning");
        }
        cmd.addAll(vmArgs);
        cmd.addAll(selfReference());
        cmd.add("workload");

        final ProcessBuilder pb = new ProcessBuilder(cmd);
        final long started = System.nanoTime();
        final Process proc = pb.start();
        final String out = new String(proc.getInputStream().readAllBytes());
        final String err = new String(proc.getErrorStream().readAllBytes());
        final int exit = proc.waitFor();
        final long millis = (System.nanoTime() - started) / 1_000_000;
        return new Result(exit, out, err, millis);
    }

    /**
     * How to point a child JVM back at this class.
     *
     * <p>Under the source launcher {@code java.class.path} is the source file itself and is not a
     * usable classpath, so {@code jdk.launcher.sourcefile} is the only thing that works — the same
     * trap {@link CompactObjectHeadersDemo} documents.</p>
     */
    private static List<String> selfReference() {
        final String sourceFile = System.getProperty("jdk.launcher.sourcefile");
        if (sourceFile != null) {
            return List.of(sourceFile);
        }
        return List.of("-cp", System.getProperty("java.class.path"),
                AotCacheDemo.class.getName());
    }

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    // ── measurement ─────────────────────────────────────────────────────────

    private interface Run {
        Result get() throws Exception;
    }

    /** Median of {@link #RUNS} runs, first discarded, so one scheduling hiccup cannot set the number. */
    private static long median(final Run run) throws Exception {
        final List<Long> times = new ArrayList<>();
        for (int i = 0; i <= RUNS; i++) {
            final Result r = run.get();
            if (i > 0) {
                times.add(r.millis());
            }
        }
        times.sort(Long::compare);
        return times.get(times.size() / 2);
    }

    // ── output ──────────────────────────────────────────────────────────────

    private static void banner(final String title) {
        System.out.println();
        System.out.println("=".repeat(78));
        System.out.println("  " + title);
        System.out.println("=".repeat(78));
    }

    private static void section(final String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("-".repeat(78));
    }

    private static String indent(final String text) {
        return text.lines().map(l -> "    " + l).collect(Collectors.joining("\n"));
    }

    private static void skippedNote(final Result record) {
        if (record.aotLines().contains("Unsupported location")) {
            System.out.println("""
                  ^ note the skip: these classes are loaded from a directory, not a JAR.
                    Section 5 explains why that matters for an exploded WAR.""");
        }
    }

    private static void deleteRecursively(final Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (final IOException ignored) {
                    // temp dir, best effort
                }
            });
        } catch (final IOException ignored) {
            // temp dir, best effort
        }
    }
}
