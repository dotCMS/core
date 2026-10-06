package com.dotmarketing.portlets.templates.design.bean;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LayoutChangesTest {

    @Test
    public void getOldInstanceId_whenLegacyValue_returnsStartValue() {
        final LayoutChanges.ContainerChanged changed = new LayoutChanges.ContainerChanged(
                "container-1",
                ContainerUUID.UUID_LEGACY_VALUE,
                "2"
        );

        assertEquals(ContainerUUID.UUID_START_VALUE, changed.getOldInstanceId());
    }

    @Test
    public void getOldInstanceId_whenNormalValue_returnsItUnchanged() {
        final LayoutChanges.ContainerChanged changed = new LayoutChanges.ContainerChanged(
                "container-1",
                "5",
                "6"
        );

        assertEquals("5", changed.getOldInstanceId());
    }

    @Test
    public void getOldInstanceId_whenStartValue_returnsItUnchanged() {
        final LayoutChanges.ContainerChanged changed = new LayoutChanges.ContainerChanged(
                "container-1",
                ContainerUUID.UUID_START_VALUE,
                "2"
        );

        assertEquals(ContainerUUID.UUID_START_VALUE, changed.getOldInstanceId());
    }

    @Test
    public void isRemove_whenNewInstanceIsDefault_returnsTrue() {
        final LayoutChanges.ContainerChanged changed = new LayoutChanges.ContainerChanged(
                "container-1",
                "1",
                ContainerUUID.UUID_DEFAULT_VALUE
        );

        assertTrue(changed.isRemove());
        assertFalse(changed.isNew());
        assertFalse(changed.isMoved());
    }

    @Test
    public void isNew_whenOldInstanceIsDefault_returnsTrue() {
        final LayoutChanges.ContainerChanged changed = new LayoutChanges.ContainerChanged(
                "container-1",
                ContainerUUID.UUID_DEFAULT_VALUE,
                "1"
        );

        assertTrue(changed.isNew());
        assertFalse(changed.isRemove());
        assertFalse(changed.isMoved());
    }

    @Test
    public void isMoved_whenBothInstancesAreNonDefault_returnsTrue() {
        final LayoutChanges.ContainerChanged changed = new LayoutChanges.ContainerChanged(
                "container-1",
                "1",
                "2"
        );

        assertTrue(changed.isMoved());
        assertFalse(changed.isNew());
        assertFalse(changed.isRemove());
    }
}