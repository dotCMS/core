package com.dotmarketing.portlets.contentlet.business;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link HostReparentPayload} value object.
 *
 * <p>These tests verify construction, accessor correctness, null-safety, and the
 * {@link Object#toString()} representation without requiring any dotCMS infrastructure.</p>
 */
public class HostReparentPayloadTest {

    private static final String OLD_TOP = "aaaa-bbbb-cccc-dddd";
    private static final String NEW_TOP = "1111-2222-3333-4444";

    // -------------------------------------------------------------------------
    // Construction and accessor tests
    // -------------------------------------------------------------------------

    @Test
    public void constructor_setsFieldsCorrectly() {
        final HostReparentPayload payload = new HostReparentPayload(OLD_TOP, NEW_TOP);

        assertEquals("oldTopLevelHostId must match constructor arg", OLD_TOP,
                payload.getOldTopLevelHostId());
        assertEquals("newTopLevelHostId must match constructor arg", NEW_TOP,
                payload.getNewTopLevelHostId());
    }

    @Test
    public void constructor_sameValueBothFields_isAllowed() {
        // Moving within same tree → both IDs are equal; this must not throw.
        final HostReparentPayload payload = new HostReparentPayload(OLD_TOP, OLD_TOP);

        assertEquals(OLD_TOP, payload.getOldTopLevelHostId());
        assertEquals(OLD_TOP, payload.getNewTopLevelHostId());
    }

    // -------------------------------------------------------------------------
    // Null-safety tests
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullOldTopLevelHostId_throwsIllegalArgument() {
        new HostReparentPayload(null, NEW_TOP);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullNewTopLevelHostId_throwsIllegalArgument() {
        new HostReparentPayload(OLD_TOP, null);
    }

    // -------------------------------------------------------------------------
    // toString tests
    // -------------------------------------------------------------------------

    @Test
    public void toString_containsBothIds() {
        final HostReparentPayload payload = new HostReparentPayload(OLD_TOP, NEW_TOP);
        final String str = payload.toString();

        assertNotNull("toString must not return null", str);
        assertTrue("toString must contain oldTopLevelHostId", str.contains(OLD_TOP));
        assertTrue("toString must contain newTopLevelHostId", str.contains(NEW_TOP));
    }
}
