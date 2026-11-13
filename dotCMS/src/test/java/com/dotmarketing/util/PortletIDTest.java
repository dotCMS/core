package com.dotmarketing.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Pins the portlet ids that issues #37574 and #37356 add to or remove from the product's portlet
 * id registry, plus one existing id as a regression guard on the name-derivation rule.
 *
 * @author hassandotcms
 */
public class PortletIDTest {

    /**
     * Given Scenario: the Tools portlet id is looked up in the registry.
     * Expected Result: {@code tools}, the id every Tools gate accepts now that the portlet is
     * promoted.
     */
    @Test
    public void toolsId() {
        assertEquals("tools", PortletID.TOOLS.toString());
    }

    /**
     * Given Scenario: the ids of the old Users and Roles &amp; Tools screens, kept after the Angular
     * portlets took over {@code users} and {@code roles}, are looked up in the registry.
     * Expected Result: {@code users-legacy} and {@code roles-legacy}, the ids declared in
     * {@code portlet.xml}.
     */
    @Test
    public void legacyIds() {
        assertEquals("users-legacy", PortletID.USERS_LEGACY.toString());
        assertEquals("roles-legacy", PortletID.ROLES_LEGACY.toString());
    }

    /**
     * Given Scenario: the registry is searched for the Users, Roles and Tools beta ids.
     * Expected Result: none of them is declared, since the beta portlets are gone.
     */
    @Test
    public void betaIdsAreGone() {
        for (final PortletID portletId : PortletID.values()) {
            assertFalse("The registry still declares " + portletId.toString(),
                    portletId.toString().equals("users-beta")
                            || portletId.toString().equals("roles-beta")
                            || portletId.toString().equals("tools-beta"));
        }
    }

    /**
     * Given Scenario: the Language Variables tool, which ships with the product but is stored as
     * a database row, is looked up in the registry.
     * Expected Result: its stored id {@code c_Language-Variables}, so the custom-tool predicate
     * treats it as a product tool.
     */
    @Test
    public void languageVariablesId() {
        assertEquals("c_Language-Variables", PortletID.LANGUAGE_VARIABLES.toString());
    }

    /**
     * Given Scenario: an existing constant with a derived id.
     * Expected Result: unchanged, proving the additions did not disturb the derivation rule.
     */
    @Test
    public void existingDerivedIdUnchanged() {
        assertEquals("roles", PortletID.ROLES.toString());
    }
}
