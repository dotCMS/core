package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.dotcms.config.SystemTableConfigSource;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the re-entrancy guard around {@code Config.getSystemTableValue}.
 *
 * <p>Reading a property can consult the system table, and consulting the system table reads
 * properties in order to reach the database — so without a guard the lookup recurses. The guard
 * therefore has to answer one question: <em>am I already inside a system-table lookup on this
 * thread?</em></p>
 *
 * <p>These tests install a {@link SystemTableConfigSource} that deliberately re-enters
 * {@code Config} and pin three properties of the guard:</p>
 *
 * <ul>
 *   <li>the nested lookup does not reach the source (no recursion);</li>
 *   <li>the guard is released once the outer lookup returns, so an independent later lookup is
 *       served normally;</li>
 *   <li>the guard is released even when the source throws.</li>
 * </ul>
 */
public class ConfigSystemTableRecursionGuardTest {

    private static final String PROBE_KEY = "DOT_GUARD_PROBE_KEY";
    private static final String NESTED_KEY = "DOT_GUARD_NESTED_KEY";

    private Object previousSource;
    private boolean previousEnabled;

    @Before
    public void before() throws Exception {
        this.previousSource = sourceField().get(null);
        this.previousEnabled = Config.enableSystemTableConfigSource;
        Config.enableSystemTableConfigSource = true;
    }

    @After
    public void after() throws Exception {
        sourceField().set(null, this.previousSource);
        Config.enableSystemTableConfigSource = this.previousEnabled;
    }

    /**
     * A nested lookup must not reach the config source at all.
     *
     * <p>Note the outer count: one property lookup consults the source <b>twice</b>, because
     * {@code getSystemTableValue} is handed both the environment-style key and the raw name and
     * tries them in turn. So two outer hits and zero nested hits is the passing shape.</p>
     */
    @Test
    public void test_nestedLookupDoesNotReachTheSource() throws Exception {
        final AtomicInteger outerHits = new AtomicInteger();
        final AtomicInteger nestedHits = new AtomicInteger();

        install(name -> {
            countHit(name, outerHits, nestedHits);
            // Re-enter Config from inside the source, the way reaching the database does.
            Config.getStringProperty(NESTED_KEY, null);
            return null;
        });

        Config.getStringProperty(PROBE_KEY, null);

        assertEquals("The nested lookup must never reach the source", 0, nestedHits.get());
        assertEquals("The outer lookup tries both the env key and the raw name", 2,
                outerHits.get());
    }

    /**
     * Once the outer lookup returns, the guard must be released: a later, unrelated lookup is a
     * new outer call and must reach the source again.
     */
    @Test
    public void test_guardIsReleasedAfterTheOuterLookupReturns() throws Exception {
        final AtomicInteger outerHits = new AtomicInteger();
        final AtomicInteger nestedHits = new AtomicInteger();

        install(name -> {
            countHit(name, outerHits, nestedHits);
            Config.getStringProperty(NESTED_KEY, null);
            return null;
        });

        Config.getStringProperty(PROBE_KEY, null);
        Config.getStringProperty(PROBE_KEY, null);

        assertEquals("The guard must not survive the outer lookup", 4, outerHits.get());
        assertEquals("The nested lookup must never reach the source", 0, nestedHits.get());
    }

    /**
     * The regression test: if the source throws, the guard must still be released. A guard left set
     * would silently disable the system-table config source for the rest of that thread's life —
     * and on a pooled request thread, for every request it serves afterwards.
     */
    @Test
    public void test_guardIsReleasedWhenTheSourceThrows() throws Exception {
        final AtomicInteger outerHits = new AtomicInteger();
        final AtomicInteger nestedHits = new AtomicInteger();

        install(name -> {
            countHit(name, outerHits, nestedHits);
            throw new IllegalStateException("system table unavailable");
        });

        // Config swallows source failures (Try.of(...).getOrNull()), so neither call throws here.
        Config.getStringProperty(PROBE_KEY, null);
        Config.getStringProperty(PROBE_KEY, null);

        assertEquals("A failing source must not leave the guard set and block later lookups",
                4, outerHits.get());
        assertEquals("The nested lookup must never reach the source", 0, nestedHits.get());
    }

    /**
     * Attributes a source hit to the outer or the nested lookup by property name.
     */
    private static void countHit(final String propertyName, final AtomicInteger outerHits,
            final AtomicInteger nestedHits) {
        if (null != propertyName && propertyName.contains("NESTED")) {
            nestedHits.incrementAndGet();
        } else {
            outerHits.incrementAndGet();
        }
    }

    /**
     * Sanity check that the probe key really is absent, so a non-null result in the tests above
     * could only have come from the installed source.
     */
    @Test
    public void test_probeKeyIsNotOtherwiseSet() {
        assertNull("The probe key must not be configured by any other source",
                Config.getStringProperty(PROBE_KEY, null));
    }

    /**
     * Installs a config source whose {@code getValue} runs the supplied body.
     *
     * @param body what the source should do when consulted
     */
    private void install(final SourceBody body) throws Exception {
        sourceField().set(null, new SystemTableConfigSource() {
            @Override
            public String getValue(final String propertyName) {
                return body.apply(propertyName);
            }
        });
    }

    /**
     * @return the private static {@code Config.systemTableConfigSource} field, made accessible
     */
    private static Field sourceField() throws Exception {
        final Field field = Config.class.getDeclaredField("systemTableConfigSource");
        field.setAccessible(true);
        return field;
    }

    @FunctionalInterface
    private interface SourceBody {
        String apply(String propertyName);
    }
}
