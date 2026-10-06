package com.dotcms.rest.api.v1.vtl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.velocity.util.introspection.Info;
import org.junit.Test;

/**
 * Unit test for {@link CollectingInvalidReferenceHandler}. Velocity's
 * {@code InvalidReferenceEventHandler} callbacks are invoked directly with the same arguments the
 * engine passes at evaluation time (verified against the {@code ASTReference} call sites in
 * velocity-1.7): a bare undefined variable arrives at {@code invalidGetMethod} with a null base
 * object and null property; a bad method call arrives at {@code invalidMethod}. This keeps the test
 * hermetic — it does not boot the dotCMS-coupled Velocity engine — while still exercising the
 * classification, reference capture, position mapping, cap, and the "never substitute a value"
 * contract that guarantees output is unchanged.
 */
public class CollectingInvalidReferenceHandlerTest {

    private static Info info(final int line, final int column) {
        return new Info("dynamic velocity", line, column);
    }

    @Test
    public void undefined_top_level_reference_is_classified_and_captured() {
        final CollectingInvalidReferenceHandler handler = new CollectingInvalidReferenceHandler();

        // Bare $noSuchVar: engine passes object == null, property == null.
        final Object substituted =
                handler.invalidGetMethod(null, "$noSuchVar", null, null, info(3, 1));

        assertNull("handler must not substitute a value (output stays unchanged)", substituted);
        final List<VelocityWarningView> warnings = handler.getWarnings();
        assertEquals(1, warnings.size());
        final VelocityWarningView w = warnings.get(0);
        assertEquals("UNDEFINED_REFERENCE", w.getType());
        assertEquals("$noSuchVar", w.getReference());
        assertEquals(Integer.valueOf(3), w.getLine());
        assertEquals(Integer.valueOf(1), w.getColumn());
        assertTrue(w.getMessage().contains("noSuchVar"));
    }

    @Test
    public void null_property_on_real_object_is_a_null_result_not_undefined() {
        final CollectingInvalidReferenceHandler handler = new CollectingInvalidReferenceHandler();

        // $real.missing where $real resolves but .missing is null: object != null.
        handler.invalidGetMethod(null, "$real.missing", new Object(), "missing", info(4, 7));

        final VelocityWarningView w = handler.getWarnings().get(0);
        assertEquals("NULL_METHOD_RESULT", w.getType());
        assertTrue(w.getMessage().contains("missing"));
    }

    @Test
    public void bad_method_on_real_object_is_invalid_method() {
        final CollectingInvalidReferenceHandler handler = new CollectingInvalidReferenceHandler();

        final Object substituted =
                handler.invalidMethod(null, "$list.badMethod()", new Object(), "badMethod", info(2, 5));

        assertNull(substituted);
        final VelocityWarningView w = handler.getWarnings().get(0);
        assertEquals("INVALID_METHOD", w.getType());
        assertTrue(w.getMessage().contains("badMethod"));
    }

    @Test
    public void method_on_null_reference_is_reported_as_undefined() {
        final CollectingInvalidReferenceHandler handler = new CollectingInvalidReferenceHandler();

        handler.invalidMethod(null, "$missing.call()", null, "call", info(1, 1));

        assertEquals("UNDEFINED_REFERENCE", handler.getWarnings().get(0).getType());
    }

    @Test
    public void null_set_is_collected() {
        final CollectingInvalidReferenceHandler handler = new CollectingInvalidReferenceHandler();

        final boolean handled =
                handler.invalidSetMethod(null, "$x", "$nullThing", info(5, 1));

        assertTrue("must not swallow the default #set behavior", !handled);
        assertEquals("NULL_SET", handler.getWarnings().get(0).getType());
    }

    @Test
    public void position_is_omitted_when_velocity_reports_zero() {
        final CollectingInvalidReferenceHandler handler = new CollectingInvalidReferenceHandler();

        handler.invalidGetMethod(null, "$x", null, null, info(0, 0));

        final VelocityWarningView w = handler.getWarnings().get(0);
        assertNull(w.getLine());
        assertNull(w.getColumn());
    }

    @Test
    public void warnings_are_capped() {
        final CollectingInvalidReferenceHandler handler = new CollectingInvalidReferenceHandler();

        for (int i = 0; i < CollectingInvalidReferenceHandler.MAX_WARNINGS + 25; i++) {
            handler.invalidGetMethod(null, "$missing" + i, null, null, info(i + 1, 1));
        }

        assertEquals(CollectingInvalidReferenceHandler.MAX_WARNINGS, handler.getWarnings().size());
    }
}
