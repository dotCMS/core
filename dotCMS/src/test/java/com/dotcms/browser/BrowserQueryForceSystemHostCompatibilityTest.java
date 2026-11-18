package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.Test;

/**
 * {@code forceSystemHost(boolean)} still exists on the builder, and still means what it meant.
 *
 * <p>{@link SystemHostMode} replaced that boolean because the host predicate has three shapes and
 * a flag can only name two. The enum is the right model, but the boolean is a {@code public}
 * method on a {@code public} class that has shipped for years, so callers outside this repository
 * may hold it: a static plugin, a jar on the container classpath, or customer code compiled
 * against an older core. None of those appear in a grep of this repository, and that is exactly
 * why the breakage would reach a customer before it reached us.</p>
 *
 * <p>So the boolean stays as a deprecated delegate, and these tests are the guard. They assert
 * the method is present and public (source and binary compatibility), and that both of its values
 * still resolve to the mode that produced the identical query before the enum existed. Deleting
 * the delegate, or re-pointing it at a different mode, turns them red.</p>
 *
 * <p>Asserted on the builder rather than on a built {@link BrowserQuery}, because {@code build()}
 * resolves the site and folder and loads the user's roles through {@code APILocator}: a database
 * bootstrap this unit test has no business requiring to answer a question about one setter.</p>
 */
public class BrowserQueryForceSystemHostCompatibilityTest {

    /**
     * The signature legacy callers compiled against is still there, still public, and still
     * returns the builder so a call chain keeps working. This is the assertion that would have
     * caught the removal.
     */
    @Test
    public void testTheDeprecatedBooleanSetterIsStillPresentAndPublic() throws Exception {
        final Method method =
                BrowserQuery.Builder.class.getMethod("forceSystemHost", boolean.class);

        assertTrue("legacy callers reach it from outside this package",
                Modifier.isPublic(method.getModifiers()));
        assertEquals("a call chain must keep working",
                BrowserQuery.Builder.class, method.getReturnType());
        assertTrue("callers must be told what to move to",
                method.isAnnotationPresent(Deprecated.class));
    }

    /**
     * {@code true} admitted System Host alongside the site: the index query read
     * {@code +(conhost:<site> OR conhost:SYSTEM_HOST)} and the SQL read
     * {@code host_inode = ? or host_inode = 'SYSTEM_HOST'}. That is {@link SystemHostMode#INCLUDE}.
     */
    @Test
    public void testTrueStillMeansSystemHostAlongsideTheSite() throws Exception {
        assertEquals(SystemHostMode.INCLUDE, modeAfterForceSystemHost(true));
    }

    /**
     * {@code false} matched the site alone, which is {@link SystemHostMode#EXCLUDE}. Worth pinning
     * separately from the default below: a caller that passes {@code false} explicitly must land
     * in the same place as one that never mentions System Host at all.
     */
    @Test
    public void testFalseStillMeansTheSiteAlone() throws Exception {
        assertEquals(SystemHostMode.EXCLUDE, modeAfterForceSystemHost(false));
    }

    /**
     * The boolean defaulted to {@code false}, so a builder that never mentions System Host must
     * still exclude it. Several callers rely on this without knowing they do: the assets API, the
     * older file browser, the legacy admin browser and a Velocity viewtool among them.
     */
    @Test
    public void testSayingNothingStillExcludesSystemHost() throws Exception {
        assertEquals(SystemHostMode.EXCLUDE, modeOf(BrowserQuery.builder()));
    }

    /**
     * The delegate is a translation, not a second channel: whichever setter a caller reaches for,
     * the builder ends up in the same state, so no query can depend on which one was used.
     */
    @Test
    public void testTheTwoSettersLeaveTheBuilderInTheSameState() throws Exception {
        assertEquals("true must be indistinguishable from INCLUDE",
                modeOf(BrowserQuery.builder().systemHostMode(SystemHostMode.INCLUDE)),
                modeAfterForceSystemHost(true));
        assertEquals("false must be indistinguishable from EXCLUDE",
                modeOf(BrowserQuery.builder().systemHostMode(SystemHostMode.EXCLUDE)),
                modeAfterForceSystemHost(false));
    }

    /**
     * Invoked reflectively so the test compiles without a deprecation warning of its own, and so
     * that a removal fails here as a missing method rather than as a compile error in this file.
     */
    private static SystemHostMode modeAfterForceSystemHost(final boolean forceSystemHost)
            throws Exception {
        final Method method =
                BrowserQuery.Builder.class.getMethod("forceSystemHost", boolean.class);

        return modeOf(
                (BrowserQuery.Builder) method.invoke(BrowserQuery.builder(), forceSystemHost));
    }

    private static SystemHostMode modeOf(final BrowserQuery.Builder builder) throws Exception {
        final Field field = BrowserQuery.Builder.class.getDeclaredField("systemHostMode");
        field.setAccessible(true);

        return (SystemHostMode) field.get(builder);
    }
}
