package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/**
 * Unit tests for {@link ThreadContextUtil}, which scopes the "should this API call reindex?" flag
 * around a delegate.
 *
 * <p>These tests pin the semantics that matter and that a hand-written save/rebind/restore is easy
 * to get wrong:</p>
 *
 * <ul>
 *   <li>the flag defaults to {@code true} when nothing has bound it;</li>
 *   <li>a wrap makes it {@code false} for the delegate only;</li>
 *   <li>a nested wrap restores the <b>enclosing</b> value on exit, not the default;</li>
 *   <li>the value is restored even when the delegate throws.</li>
 * </ul>
 *
 * <p>The last one is the regression test for the whole class of bug: an exception path that skips
 * the restore leaves every later call on that thread believing it must not reindex.</p>
 */
public class ThreadContextUtilTest {

    /**
     * Nothing bound: an API call must reindex. This is the default the platform relies on.
     */
    @Test
    public void test_isReindex_defaultsToTrue() {
        assertTrue("With nothing bound, isReindex() must default to true",
                ThreadContextUtil.isReindex());
    }

    @Test
    public void test_wrapVoidNoReindex_bindsFalseForTheDelegateOnly() {
        final AtomicBoolean seenInside = new AtomicBoolean(true);

        ThreadContextUtil.wrapVoidNoReindex(() -> seenInside.set(ThreadContextUtil.isReindex()));

        assertFalse("Inside the wrap, isReindex() must be false", seenInside.get());
        assertTrue("After the wrap, isReindex() must be back to true",
                ThreadContextUtil.isReindex());
    }

    @Test
    public void test_wrapReturnNoReindex_returnsValueAndBindsFalse() {
        final String result = ThreadContextUtil.wrapReturnNoReindex(
                () -> "reindex=" + ThreadContextUtil.isReindex());

        assertEquals("The delegate's value must be returned unchanged", "reindex=false", result);
        assertTrue("After the wrap, isReindex() must be back to true",
                ThreadContextUtil.isReindex());
    }

    /**
     * The nesting case. An inner wrap must restore whatever the outer wrap had bound, not the
     * global default — this is precisely what a {@code finally} that assigns a constant gets wrong.
     */
    @Test
    public void test_nestedWrap_restoresTheEnclosingValueNotTheDefault() {
        final AtomicReference<String> trace = new AtomicReference<>("");

        ThreadContextUtil.wrapVoidNoReindex(() -> {
            trace.set(trace.get() + "outer=" + ThreadContextUtil.isReindex() + ";");

            ThreadContextUtil.wrapVoidNoReindex(
                    () -> trace.set(trace.get() + "inner=" + ThreadContextUtil.isReindex() + ";"));

            trace.set(trace.get() + "afterInner=" + ThreadContextUtil.isReindex() + ";");
        });

        assertEquals("The inner wrap must not leak the default back into the outer scope",
                "outer=false;inner=false;afterInner=false;", trace.get());
        assertTrue("After both wraps, isReindex() must be back to true",
                ThreadContextUtil.isReindex());
    }

    /**
     * The regression test: a delegate that throws must still leave the flag restored. If the
     * restore is skipped, every subsequent operation on this thread silently stops reindexing.
     */
    @Test
    public void test_wrapVoidNoReindex_restoresFlagWhenDelegateThrows() {
        try {
            ThreadContextUtil.wrapVoidNoReindex(() -> {
                throw new DotDataException("boom");
            });
            fail("The delegate's failure must not be swallowed");
        } catch (final DotRuntimeException expected) {
            // The documented contract: checked failures are wrapped.
        }

        assertTrue("A throwing delegate must not leave isReindex() stuck at false",
                ThreadContextUtil.isReindex());
    }

    @Test
    public void test_wrapReturnNoReindex_restoresFlagWhenDelegateThrows() {
        try {
            ThreadContextUtil.wrapReturnNoReindex(() -> {
                throw new DotDataException("boom");
            });
            fail("The delegate's failure must not be swallowed");
        } catch (final DotRuntimeException expected) {
            // The documented contract: checked failures are wrapped.
        }

        assertTrue("A throwing delegate must not leave isReindex() stuck at false",
                ThreadContextUtil.isReindex());
    }

    /**
     * {@code ifReindex} runs its delegate when the flag is set.
     */
    @Test
    public void test_ifReindex_runsDelegateWhenReindexing() throws Exception {
        final AtomicBoolean ran = new AtomicBoolean(false);

        ThreadContextUtil.ifReindex(() -> ran.set(true));

        assertTrue("ifReindex must run the delegate when isReindex() is true", ran.get());
    }

    /**
     * ...and skips it inside a no-reindex wrap.
     */
    @Test
    public void test_ifReindex_skipsDelegateInsideWrap() {
        final AtomicBoolean ran = new AtomicBoolean(false);

        ThreadContextUtil.wrapVoidNoReindex(() -> ThreadContextUtil.ifReindex(() -> ran.set(true)));

        assertFalse("ifReindex must skip the delegate when isReindex() is false", ran.get());
    }

    /**
     * The two-argument {@code ifReindex} is the deferral channel: when the call is not reindexing,
     * it records that the eventual reindex must include dependencies. That value travels
     * <b>callee to caller</b> — the code that consumes it runs after the code that sets it — so it
     * is deliberately <b>not</b> a scoped value, which is immutable by design. This test pins that
     * the out-channel still works.
     */
    @Test
    public void test_ifReindex_recordsIncludeDependenciesForTheDeferredReindex() {
        ThreadContextUtil.wrapVoidNoReindex(() -> {
            try {
                ThreadContextUtil.ifReindex(() -> fail("Must not index inline inside a wrap"), true);
            } catch (final Exception e) {
                throw new DotRuntimeException(e);
            }

            assertTrue("The callee must be able to record includeDependencies for the caller",
                    ThreadContextUtil.getOrCreateContext().isIncludeDependencies());
        });
    }
}
