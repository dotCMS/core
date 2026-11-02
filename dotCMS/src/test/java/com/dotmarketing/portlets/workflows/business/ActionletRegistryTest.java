package com.dotmarketing.portlets.workflows.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/**
 * Checks that actionlet lookups stay consistent while another thread registers and unregisters
 * an actionlet, as an OSGi plugin does when its bundle starts and stops (issue #38354).
 */
public class ActionletRegistryTest {

    private static final List<Class<? extends WorkFlowActionlet>> BUILT_INS =
            List.of(AlphaActionlet.class, BravoActionlet.class, CharlieActionlet.class);

    private static final int READERS = 4;
    private static final int READS_PER_READER = 20_000;

    /**
     * Given a registry with three built-in actionlets, when one thread repeatedly adds and removes
     * a plugin actionlet while several threads look actionlets up, then no lookup throws, returns
     * null, or misses a built-in actionlet.
     */
    @Test
    public void lookupsStayConsistentWhileAnActionletIsAddedAndRemoved() throws Exception {
        final ActionletRegistry registry = new ActionletRegistry(BUILT_INS);
        final AtomicBoolean done = new AtomicBoolean(false);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch readersDone = new CountDownLatch(READERS);

        final Thread writer = new Thread(() -> {
            try {
                while (!done.get()) {
                    registry.add(PluginActionlet.class);
                    registry.remove(PluginActionlet.class.getCanonicalName());
                }
            } catch (final Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        final List<Thread> readers = new ArrayList<>();
        for (int i = 0; i < READERS; i++) {
            readers.add(new Thread(() -> {
                try {
                    for (int n = 0; n < READS_PER_READER && failure.get() == null; n++) {
                        checkLookups(registry);
                    }
                } catch (final Throwable t) {
                    failure.compareAndSet(null, t);
                } finally {
                    readersDone.countDown();
                }
            }));
        }

        writer.start();
        readers.forEach(Thread::start);
        final boolean finished = readersDone.await(2, TimeUnit.MINUTES);
        done.set(true);
        writer.join(TimeUnit.SECONDS.toMillis(30));

        if (failure.get() != null) {
            throw new AssertionError("Actionlet lookup failed during a concurrent refresh", failure.get());
        }
        assertTrue("Readers did not finish in time", finished);
    }

    /**
     * Registering a class with the same name twice keeps one entry, removing it drops it, and the
     * actionlets stay ordered by localized name.
     */
    @Test
    public void addAndRemoveKeepOrderingAndNoDuplicates() {
        final ActionletRegistry registry = new ActionletRegistry(
                List.of(CharlieActionlet.class, AlphaActionlet.class, BravoActionlet.class));

        registry.add(PluginActionlet.class);
        registry.add(PluginActionlet.class);
        assertEquals(List.of(AlphaActionlet.class, BravoActionlet.class, CharlieActionlet.class,
                PluginActionlet.class), classesOf(registry.findAll()));

        assertEquals(PluginActionlet.class,
                registry.remove(PluginActionlet.class.getCanonicalName()).getClass());
        assertNull(registry.find(PluginActionlet.class.getCanonicalName()));
        assertNull(registry.newActionlet(PluginActionlet.class.getName()));
        assertEquals(List.of(AlphaActionlet.class, BravoActionlet.class, CharlieActionlet.class),
                classesOf(registry.findAll()));
    }

    /**
     * Removing a name that is not registered returns null and leaves the actionlets unchanged,
     * instead of throwing a NullPointerException.
     */
    @Test
    public void removeOfAnUnknownNameChangesNothing() {
        final ActionletRegistry registry = new ActionletRegistry(BUILT_INS);

        assertNull(registry.remove("com.example.NoSuchActionlet"));
        assertEquals(List.of(AlphaActionlet.class, BravoActionlet.class, CharlieActionlet.class),
                classesOf(registry.findAll()));
    }

    private static void checkLookups(final ActionletRegistry registry) {
        for (final Class<? extends WorkFlowActionlet> builtIn : BUILT_INS) {
            final WorkFlowActionlet found = registry.find(builtIn.getCanonicalName());
            if (found == null || found.getClass() != builtIn) {
                throw new AssertionError("find(" + builtIn.getSimpleName() + ") returned " + found);
            }
            final WorkFlowActionlet created = registry.newActionlet(builtIn.getName());
            if (created == null || created.getClass() != builtIn) {
                throw new AssertionError("newActionlet(" + builtIn.getSimpleName() + ") returned " + created);
            }
        }
        final List<WorkFlowActionlet> all = registry.findAll();
        final List<Class<?>> classes = classesOf(all);
        if (all.contains(null) || !classes.containsAll(BUILT_INS)) {
            throw new AssertionError("findAll() returned " + classes);
        }
    }

    private static List<Class<?>> classesOf(final List<WorkFlowActionlet> actionlets) {
        final List<Class<?>> classes = new ArrayList<>();
        actionlets.forEach(a -> classes.add(a == null ? null : a.getClass()));
        return classes;
    }

    /** Minimal actionlet whose localized name is its plain name, so no language lookup is needed. */
    public abstract static class TestActionlet extends WorkFlowActionlet {

        @Override
        public String getLocalizedName() {
            return getName();
        }

        @Override
        public String getHowTo() {
            return getName();
        }

        @Override
        public List<WorkflowActionletParameter> getParameters() {
            return List.of();
        }

        @Override
        public void executeAction(final WorkflowProcessor processor,
                final Map<String, WorkflowActionClassParameter> params) {
        }
    }

    public static class AlphaActionlet extends TestActionlet {
        @Override
        public String getName() {
            return "Alpha";
        }
    }

    public static class BravoActionlet extends TestActionlet {
        @Override
        public String getName() {
            return "Bravo";
        }
    }

    public static class CharlieActionlet extends TestActionlet {
        @Override
        public String getName() {
            return "Charlie";
        }
    }

    public static class PluginActionlet extends TestActionlet {
        @Override
        public String getName() {
            return "Plugin";
        }
    }
}
