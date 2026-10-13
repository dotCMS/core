package com.dotcms.jobs.business.api;

import com.dotcms.jobs.business.processor.JobQueueEntryPoint;
import com.dotcms.jobs.business.processor.Queue;
import com.dotcms.util.JandexClassMetadataScanner;
import com.dotmarketing.util.Logger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

/**
 * Tells, for each job queue, how it is entered: through the generic endpoint
 * {@code POST /api/v1/jobs/{queueName}}, or through a dedicated endpoint that validates the request
 * first (#37883).
 * <p>
 * A queue with a dedicated endpoint declares it with {@link JobQueueEntryPoint} on the REST method;
 * a queue fed by the generic endpoint declares {@code @Queue(genericEntry = true)}. Both are read
 * from the Jandex index of {@code dotcms-core}, so no class is loaded, and the route a refusal
 * names comes from the annotated method's own {@code @Path}, never from a copied string.
 * <p>
 * A queue whose processor is not in the index (a plugin, or one registered by name at runtime) is
 * treated as generic, with one warning per queue, so existing plugins keep working. When there is
 * no index at all the registry fails closed: only queues that declare {@code genericEntry = true}
 * are generic.
 */
public class JobQueueEntryPoints {

    private static final DotName QUEUE = DotName.createSimple(Queue.class.getName());
    private static final DotName ENTRY_POINT = DotName.createSimple(JobQueueEntryPoint.class.getName());
    private static final DotName PATH = DotName.createSimple(Path.class.getName());
    private static final List<Class<?>> VERBS =
            List.of(POST.class, GET.class, PUT.class, DELETE.class, PATCH.class);

    private static volatile JobQueueEntryPoints instance;

    /** How a queue is entered: generic, or a dedicated route (absent when no index is available). */
    public static final class EntryPoint {

        private static final EntryPoint GENERIC = new EntryPoint(true, null);

        private final boolean generic;
        private final String route;

        private EntryPoint(final boolean generic, final String route) {
            this.generic = generic;
            this.route = route;
        }

        /**
         * @return true when the generic endpoint may create jobs on the queue
         */
        public boolean generic() {
            return generic;
        }

        /**
         * @return the verb and path of the dedicated endpoint, such as
         *         {@code POST /api/v1/assets/folders/_bulkduplicate}; empty when the queue is
         *         generic, or refused because no index was available to name the route
         */
        public Optional<String> route() {
            return Optional.ofNullable(route);
        }
    }

    private final Index index;
    private final Map<String, String> dedicatedRoutes = new HashMap<>();
    private final Set<String> coreQueues = new HashSet<>();
    private final Set<String> genericCoreQueues = new HashSet<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    /**
     * Builds the registry from a Jandex index.
     *
     * @param index the index of the core classes; {@code null} when none is available, which makes
     *              the registry fail closed
     */
    public JobQueueEntryPoints(final Index index) {
        this.index = index;
        if (index == null) {
            Logger.error(JobQueueEntryPoints.class, "No Jandex index available: the generic job"
                    + " endpoint will only accept queues that declare genericEntry = true");
            return;
        }
        for (final AnnotationInstance queue : index.getAnnotations(QUEUE)) {
            if (queue.target().kind() == AnnotationTarget.Kind.CLASS) {
                final String name = queue.value().asString();
                coreQueues.add(name);
                final AnnotationValue generic = queue.value("genericEntry");
                if (generic != null && generic.asBoolean()) {
                    genericCoreQueues.add(name);
                }
            }
        }
        for (final AnnotationInstance entry : index.getAnnotations(ENTRY_POINT)) {
            if (entry.target().kind() == AnnotationTarget.Kind.METHOD) {
                dedicatedRoutes.put(entry.value().asString(), routeOf(entry.target().asMethod()));
            }
        }
    }

    /**
     * The registry for the running application, built once from the Jandex index of the core
     * classes.
     *
     * @return the shared registry
     */
    public static JobQueueEntryPoints getInstance() {
        JobQueueEntryPoints current = instance;
        if (current == null) {
            synchronized (JobQueueEntryPoints.class) {
                current = instance;
                if (current == null) {
                    current = new JobQueueEntryPoints(JandexClassMetadataScanner.getJandexIndex());
                    instance = current;
                }
            }
        }
        return current;
    }

    /**
     * Decides how a queue is entered.
     *
     * @param queueName the queue
     * @param processor the processor class registered for it; {@code null} when none is
     * @return the queue's entry point
     */
    public EntryPoint resolve(final String queueName, final Class<?> processor) {
        if (index == null) {
            return declaresGenericEntry(processor) || processor == null
                    ? EntryPoint.GENERIC : new EntryPoint(false, null);
        }
        final String route = dedicatedRoutes.get(queueName);
        if (route != null) {
            return new EntryPoint(false, route);
        }
        if (!coreQueues.contains(queueName) && warned.add(queueName)) {
            Logger.warn(this, String.format("Queue [%s] is not in the core index and declares no"
                    + " entry point; the generic job endpoint will accept it", queueName));
        }
        return EntryPoint.GENERIC;
    }

    private static boolean declaresGenericEntry(final Class<?> processor) {
        if (processor == null) {
            return false;
        }
        final Queue queue = processor.getAnnotation(Queue.class);
        return queue != null && queue.genericEntry();
    }

    /** The verb, {@code /api}, and the class and method paths of a REST method, one slash apart. */
    private static String routeOf(final MethodInfo method) {
        final String verb = VERBS.stream()
                .filter(v -> method.hasAnnotation(DotName.createSimple(v.getName())))
                .map(Class::getSimpleName)
                .findFirst().orElse("");
        final ClassInfo owner = method.declaringClass();
        final String path = "/api/" + Stream.of(pathOf(owner.classAnnotation(PATH)),
                        pathOf(method.annotation(PATH)))
                .flatMap(p -> Stream.of(p.split("/")))
                .filter(segment -> !segment.isEmpty())
                .collect(Collectors.joining("/"));
        return (verb + " " + path).trim();
    }

    private static String pathOf(final AnnotationInstance path) {
        return path == null ? "" : path.value().asString();
    }
}
