package com.dotcms.jobs.business.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.jobs.business.processor.JobQueueEntryPoint;
import com.dotcms.jobs.business.processor.Queue;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.Test;

/**
 * Unit tests for {@link JobQueueEntryPoints} (#37883): how the registry decides whether a queue is
 * entered through the generic job endpoint or through a dedicated endpoint, and which route a
 * refusal names. The Jandex index is built in memory from the small fixture classes below, so no
 * Maven index step is involved.
 */
public class JobQueueEntryPointsTest {

    /** A dedicated endpoint, declared on its REST method. */
    @Path("/v1/things")
    public static class ThingResource {

        @POST
        @Path("/_make")
        @JobQueueEntryPoint("thingQueue")
        public void make() {
        }
    }

    /** A dedicated endpoint whose class and method paths are written without clean slashes. */
    @Path("v1/odd/")
    public static class OddSlashResource {

        @POST
        @Path("_go")
        @JobQueueEntryPoint("oddQueue")
        public void go() {
        }
    }

    /** The processor of a queue fed by the generic endpoint. */
    @Queue(value = "genericQueue", genericEntry = true)
    public static class GenericProcessor {
    }

    /** The processor of a queue whose entry is a dedicated endpoint (declares nothing itself). */
    @Queue("thingQueue")
    public static class ThingProcessor {
    }

    /** A processor a plugin registers: present in the runtime, absent from the core index. */
    @Queue("pluginQueue")
    public static class PluginProcessor {
    }

    private static Index indexOf(final Class<?>... classes) throws IOException {
        final Indexer indexer = new Indexer();
        for (final Class<?> clazz : classes) {
            final String resource = clazz.getName().replace('.', '/') + ".class";
            try (InputStream in = clazz.getClassLoader().getResourceAsStream(resource)) {
                indexer.index(in);
            }
        }
        return indexer.complete();
    }

    private static JobQueueEntryPoints coreRegistry() throws IOException {
        return new JobQueueEntryPoints(indexOf(ThingResource.class, OddSlashResource.class,
                GenericProcessor.class, ThingProcessor.class));
    }

    /**
     * Given a REST method annotated with {@code @JobQueueEntryPoint("thingQueue")}.
     * When the registry resolves that queue.
     * Then it is a dedicated entry and its route is the verb plus {@code /api} plus the class
     * {@code @Path} plus the method {@code @Path}, read from the annotated method itself.
     */
    @Test
    public void dedicatedQueueResolvesVerbAndRouteFromTheAnnotatedMethod() throws IOException {
        final JobQueueEntryPoints.EntryPoint entry =
                coreRegistry().resolve("thingQueue", ThingProcessor.class);

        assertFalse(entry.generic());
        assertEquals("POST /api/v1/things/_make", entry.route().orElseThrow());
    }

    /**
     * Given class and method paths written without leading or trailing slashes.
     * When the registry builds the route.
     * Then exactly one slash separates every segment.
     */
    @Test
    public void routeJoinsSegmentsWithSingleSlashes() throws IOException {
        final JobQueueEntryPoints.EntryPoint entry =
                coreRegistry().resolve("oddQueue", ThingProcessor.class);

        assertEquals("POST /api/v1/odd/_go", entry.route().orElseThrow());
    }

    /**
     * Given a processor declaring {@code @Queue(genericEntry = true)}.
     * When the registry resolves its queue.
     * Then the generic endpoint may create jobs on it.
     */
    @Test
    public void queueDeclaredGenericResolvesToGeneric() throws IOException {
        final JobQueueEntryPoints.EntryPoint entry =
                coreRegistry().resolve("genericQueue", GenericProcessor.class);

        assertTrue(entry.generic());
    }

    /**
     * Given a queue whose processor is not in the core index (a plugin, or a queue registered by
     * name at runtime, as the integration tests do with {@code demoQueue}).
     * When the registry resolves it.
     * Then it is treated as generic, so existing plugins keep working.
     */
    @Test
    public void queueAbsentFromTheIndexResolvesToGeneric() throws IOException {
        final JobQueueEntryPoints.EntryPoint entry =
                coreRegistry().resolve("pluginQueue", PluginProcessor.class);

        assertTrue(entry.generic());
    }

    /**
     * Given a queue name nothing is registered for.
     * When the registry resolves it.
     * Then it is generic: the generic endpoint then answers its usual "queue not found".
     */
    @Test
    public void unknownQueueResolvesToGeneric() throws IOException {
        assertTrue(coreRegistry().resolve("noSuchQueue", null).generic());
    }

    /**
     * Given no Jandex index (an IDE run that skipped the Maven index step).
     * When the registry resolves a queue that declares {@code genericEntry = true}.
     * Then it is still generic: the flag is read from the processor class by reflection.
     */
    @Test
    public void withoutIndexAGenericQueueStaysGeneric() {
        final JobQueueEntryPoints noIndex = new JobQueueEntryPoints(null);

        assertTrue(noIndex.resolve("genericQueue", GenericProcessor.class).generic());
    }

    /**
     * Given no Jandex index and a queue that does not declare {@code genericEntry = true}.
     * When the registry resolves it.
     * Then it is refused, with no route to name: failing closed, so a missing index never reopens
     * the hole.
     */
    @Test
    public void withoutIndexAnUndeclaredQueueIsRefusedWithoutARoute() {
        final JobQueueEntryPoints noIndex = new JobQueueEntryPoints(null);

        final JobQueueEntryPoints.EntryPoint entry =
                noIndex.resolve("thingQueue", ThingProcessor.class);

        assertFalse(entry.generic());
        assertFalse(entry.route().isPresent());
    }
}
