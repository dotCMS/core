package com.dotmarketing.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Pins the portlet ids that issue #37574 adds to the product's portlet id registry, plus one
 * existing id as a regression guard on the name-derivation rule.
 *
 * @author hassandotcms
 */
public class PortletIDTest {

    /**
     * Given Scenario: the Tools portlet id and its Beta alias are looked up in the registry.
     * Expected Result: {@code tools} and {@code tools-beta}, the ids every Tools gate accepts.
     */
    @Test
    public void toolsIds() {
        assertEquals("tools", PortletID.TOOLS.toString());
        assertEquals("tools-beta", PortletID.TOOLS_BETA.toString());
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
