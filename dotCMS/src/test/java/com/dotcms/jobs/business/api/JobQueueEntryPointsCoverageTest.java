package com.dotcms.jobs.business.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.jobs.business.processor.JobQueueEntryPoint;
import com.dotcms.jobs.business.processor.Queue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.Test;

/**
 * Build-time guard for #37883: every job queue in core declares exactly one entry point, either
 * {@code @Queue(genericEntry = true)} or a REST method annotated {@code @JobQueueEntryPoint}. A new
 * queue that declares nothing, or both, fails this test instead of silently staying open on the
 * generic job endpoint.
 * <p>
 * It indexes the compiled main classes itself, so it does not depend on the Maven Jandex step.
 */
public class JobQueueEntryPointsCoverageTest {

    private static final DotName QUEUE = DotName.createSimple(Queue.class.getName());
    private static final DotName ENTRY_POINT = DotName.createSimple(JobQueueEntryPoint.class.getName());

    /** A queue that declares nothing, used to prove the detector can fail. */
    @Queue("undeclaredFixtureQueue")
    public static class UndeclaredProcessor {
    }

    /** A queue that declares generic entry and also has a dedicated endpoint. */
    @Queue(value = "doubleFixtureQueue", genericEntry = true)
    public static class DoubleProcessor {
    }

    /** The dedicated endpoint of {@link DoubleProcessor}'s queue. */
    public static class DoubleResource {

        @JobQueueEntryPoint("doubleFixtureQueue")
        public void create() {
        }
    }

    /**
     * Counts the entry points each queue in the index declares: one for {@code genericEntry} plus
     * one for every method annotated {@code @JobQueueEntryPoint} naming it.
     */
    private static Map<String, Integer> declarationsPerQueue(final Index index) {
        final Map<String, Integer> counts = new TreeMap<>();
        for (final AnnotationInstance queue : index.getAnnotations(QUEUE)) {
            if (queue.target().kind() == AnnotationTarget.Kind.CLASS) {
                final AnnotationValue generic = queue.value("genericEntry");
                counts.merge(queue.value().asString(),
                        generic != null && generic.asBoolean() ? 1 : 0, Integer::sum);
            }
        }
        for (final AnnotationInstance entry : index.getAnnotations(ENTRY_POINT)) {
            if (entry.target().kind() == AnnotationTarget.Kind.METHOD) {
                counts.merge(entry.value().asString(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Index indexOfCompiledMainClasses() throws IOException {
        final Path classes = Paths.get("target", "classes");
        assertTrue("compiled classes not found at " + classes.toAbsolutePath(),
                Files.isDirectory(classes));
        final Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(classes)) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".class"))
                    .collect(Collectors.toList())) {
                try (InputStream in = Files.newInputStream(file)) {
                    indexer.index(in);
                }
            }
        }
        return indexer.complete();
    }

    private static Index indexOf(final Class<?>... classes) throws IOException {
        final Indexer indexer = new Indexer();
        for (final Class<?> clazz : classes) {
            try (InputStream in = clazz.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(in);
            }
        }
        return indexer.complete();
    }

    /**
     * Given every compiled core class.
     * When the entry points each {@code @Queue} declares are counted.
     * Then every queue declares exactly one.
     */
    @Test
    public void everyCoreQueueDeclaresExactlyOneEntryPoint() throws IOException {
        final Map<String, Integer> counts = declarationsPerQueue(indexOfCompiledMainClasses());

        final Map<String, Integer> offenders = counts.entrySet().stream()
                .filter(e -> e.getValue() != 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertTrue("queues must declare exactly one entry point (queue=declarations): " + offenders,
                offenders.isEmpty());
        assertTrue("expected the core queues to be found, got " + counts.keySet(),
                counts.size() >= 9);
    }

    /**
     * Given a queue that declares nothing and a queue that declares two entry points.
     * When the same counting is applied.
     * Then it reports 0 and 2, so the guard above would fail for them.
     */
    @Test
    public void detectorReportsUndeclaredAndDoublyDeclaredQueues() throws IOException {
        final Map<String, Integer> counts = declarationsPerQueue(
                indexOf(UndeclaredProcessor.class, DoubleProcessor.class, DoubleResource.class));

        assertEquals(Integer.valueOf(0), counts.get("undeclaredFixtureQueue"));
        assertEquals(Integer.valueOf(2), counts.get("doubleFixtureQueue"));
    }
}
