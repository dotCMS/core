package com.dotcms.browser;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * Pure unit test for the workflow SQL fragment built by
 * {@link com.dotcms.browser.BrowserAPIImpl}{@code #appendWorkflowQuery} (issue #36028).
 *
 * <p>The critical guarantee is <b>byte-identical</b> output: when no archive-target step is present
 * (the common case), {@code appendWorkflowQuery} must emit exactly the SQL it emitted before this
 * change — the global {@code cvi.deleted='false'} filter still applies separately. Only when an
 * archive-target step is present does the shape change to own {@code deleted} per branch.</p>
 *
 * <p>The method is a private static, DB-free string builder; it is exercised via reflection so no
 * {@code BrowserAPIImpl} instance (and thus no {@code APILocator} bootstrap) is required.</p>
 */
public class BrowserAPIWorkflowQueryTest {

    // Golden fragments — these mirror the SQL emitted today. If production drifts, these break.
    private static final String SCHEME_CLAUSE =
            " exists (select 1 from workflow_scheme_x_structure wss "
                    + " where wss.structure_id = struc.inode and wss.scheme_id in (?)) ";
    private static final String STEP_CLAUSE =
            " exists (select 1 from workflow_task wt "
                    + " where wt.webasset = cvi.identifier and wt.language_id = cvi.lang "
                    + " and wt.status in (?)) ";

    private static Result invoke(final Set<String> schemes, final Set<String> steps,
            final Set<String> archive) throws Exception {
        final Method method = BrowserAPIImpl.class.getDeclaredMethod("appendWorkflowQuery",
                StringBuilder.class, Set.class, Set.class, Set.class, List.class);
        method.setAccessible(true);
        final StringBuilder sql = new StringBuilder();
        final List<Object> params = new ArrayList<>();
        method.invoke(null, sql, schemes, steps, archive, params);
        return new Result(sql.toString(), params);
    }

    private static Set<String> setOf(final String... ids) {
        return new LinkedHashSet<>(List.of(ids));
    }

    /**
     * Byte-identical: scheme-only + step, no archive step → the current
     * {@code and ( <scheme> or <step> )} clause, verbatim. deleted is NOT owned here.
     */
    @Test
    public void testByteIdenticalSchemeAndStepNoArchive() throws Exception {
        final Result result = invoke(setOf("scheme-1"), setOf("step-1"), Set.of());

        final String expected = " and (" + SCHEME_CLAUSE + " or " + STEP_CLAUSE + ") ";
        assertEquals(expected, result.sql);
        assertEquals(List.of("scheme-1", "step-1"), result.params);
    }

    /** Byte-identical: scheme-only entry, no archive step. */
    @Test
    public void testByteIdenticalSchemeOnlyNoArchive() throws Exception {
        final Result result = invoke(setOf("scheme-1"), Set.of(), Set.of());

        assertEquals(" and (" + SCHEME_CLAUSE + ") ", result.sql);
        assertEquals(List.of("scheme-1"), result.params);
    }

    /** Byte-identical: step-only entry, no archive step. */
    @Test
    public void testByteIdenticalStepOnlyNoArchive() throws Exception {
        final Result result = invoke(Set.of(), setOf("step-1"), Set.of());

        assertEquals(" and (" + STEP_CLAUSE + ") ", result.sql);
        assertEquals(List.of("step-1"), result.params);
    }

    /** Empty filter → no-op, empty SQL and no params. */
    @Test
    public void testEmptyFilterIsNoOp() throws Exception {
        final Result result = invoke(Set.of(), Set.of(), Set.of());

        assertEquals("", result.sql);
        assertEquals(List.of(), result.params);
    }

    /**
     * Archive step present with a scheme and a normal step: the live branch (scheme + normal step)
     * owns {@code cvi.deleted='false'}; the archive step branch admits archived rows. Params are
     * bound scheme → normal step → archive step.
     */
    @Test
    public void testArchiveStepPresentWithLiveBranch() throws Exception {
        final Result result = invoke(setOf("scheme-1"),
                setOf("step-normal", "step-archive"), setOf("step-archive"));

        final String liveBranch = " ( cvi.deleted = 'false' and ("
                + SCHEME_CLAUSE + " or " + STEP_CLAUSE + ") ) ";
        final String expected = " and (" + liveBranch + " or " + STEP_CLAUSE + ") ";
        assertEquals(expected, result.sql);
        assertEquals(List.of("scheme-1", "step-normal", "step-archive"), result.params);
    }

    /**
     * Archive step present with no scheme and no normal step: the live branch is omitted entirely,
     * leaving only the archive branch (which admits archived rows).
     */
    @Test
    public void testArchiveStepOnlyOmitsLiveBranch() throws Exception {
        final Result result = invoke(Set.of(), setOf("step-archive"), setOf("step-archive"));

        assertEquals(" and (" + STEP_CLAUSE + ") ", result.sql);
        assertEquals(List.of("step-archive"), result.params);
    }

    private static final class Result {
        final String sql;
        final List<Object> params;

        Result(final String sql, final List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
