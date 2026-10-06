package com.dotcms.rest.api.v1.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.browser.SystemHostMode;
import org.junit.Test;

/**
 * What a browse scope decides: whether the folder constraint is applied, and which host clause is
 * emitted.
 *
 * <p>Both are expressed as pure functions so they can be pinned without resolving a path, which
 * needs a site lookup and a database. The listing itself is exercised end to end by the
 * integration tests; what is asserted here is the decision, case by case.</p>
 *
 * <p><b>The case that matters most is the one about no scope at all inside a folder.</b> The Asset
 * Picker calls this same endpoint and addresses folders by path without ever naming a scope. If
 * "all" were ever implemented as unconditional folder-skipping, those requests would silently
 * start returning every descendant, and nothing else in the suite would notice.</p>
 */
public class ContentDriveHelperBrowseScopeTest {

    private static final boolean AT_THE_SITE_ROOT = true;
    private static final boolean INSIDE_A_FOLDER = false;
    private static final boolean CHIP_ON = true;
    private static final boolean CHIP_OFF = false;

    // ---- which folder constraint applies -------------------------------------------------

    /**
     * Given no browse scope, When the request is at the site root, Then the folder constraint is
     * dropped — the whole site at any depth, which is what the site root returns today.
     */
    @Test
    public void testNoScopeAtTheSiteRootStillListsTheWholeSite() {
        assertTrue("today's site-root behavior must survive untouched",
                ContentDriveHelper.skipsFolderConstraint(null, AT_THE_SITE_ROOT));
    }

    /**
     * Given no browse scope, When the request names a folder, Then the folder constraint applies.
     * This is the Asset Picker's shape, and the regression this whole design is arranged around.
     */
    @Test
    public void testNoScopeInsideAFolderStaysInThatFolder() {
        assertFalse("a folder request must never become recursive",
                ContentDriveHelper.skipsFolderConstraint(null, INSIDE_A_FOLDER));
    }

    /**
     * Given the all-site-content scope at the root, Then it decides exactly what no scope decides
     * there. Saying it explicitly and leaving it unsaid must not differ.
     */
    @Test
    public void testAllSiteContentAtTheRootMatchesSayingNothing() {
        assertEquals("the explicit spelling must agree with the implicit one",
                ContentDriveHelper.skipsFolderConstraint(null, AT_THE_SITE_ROOT),
                ContentDriveHelper.skipsFolderConstraint(BrowseScope.ALL, AT_THE_SITE_ROOT));
    }

    /**
     * Given the site-root scope, Then the folder constraint applies, which is the whole point of
     * it: the system folder's path is already {@code /}, so applying it lists what sits at the
     * root rather than everything beneath it.
     */
    @Test
    public void testTheSiteRootScopeAppliesTheFolderConstraint() {
        assertFalse("the root scope must constrain to the root",
                ContentDriveHelper.skipsFolderConstraint(BrowseScope.ROOT, AT_THE_SITE_ROOT));
    }

    /**
     * Given the System Host scope, Then the folder constraint applies too. System Host has no
     * folders, so everything in it sits at its root.
     */
    @Test
    public void testTheSystemHostScopeAppliesTheFolderConstraint() {
        assertFalse("System Host content all sits at its root",
                ContentDriveHelper.skipsFolderConstraint(BrowseScope.SYSTEM_HOST, AT_THE_SITE_ROOT));
    }

    // ---- which host clause is emitted ----------------------------------------------------

    /**
     * Given no browse scope, Then the chip decides, exactly as it does today.
     */
    @Test
    public void testWithNoScopeTheChipDecides() {
        assertEquals(SystemHostMode.INCLUDE, ContentDriveHelper.systemHostModeFor(null, CHIP_ON));
        assertEquals(SystemHostMode.EXCLUDE, ContentDriveHelper.systemHostModeFor(null, CHIP_OFF));
    }

    /**
     * Given the all-site-content scope, Then the chip still decides. This is the only scope where
     * the chip has anything to say, which is why it is the only one where it is offered.
     */
    @Test
    public void testInAllSiteContentTheChipStillDecides() {
        assertEquals(SystemHostMode.INCLUDE,
                ContentDriveHelper.systemHostModeFor(BrowseScope.ALL, CHIP_ON));
        assertEquals(SystemHostMode.EXCLUDE,
                ContentDriveHelper.systemHostModeFor(BrowseScope.ALL, CHIP_OFF));
    }

    /**
     * Given the site-root scope, Then System Host is excluded whatever the chip says. The chip is
     * disabled there, but a request can still carry a stale value, and the scope must win.
     */
    @Test
    public void testTheSiteRootScopeExcludesSystemHostWhateverTheChipSays() {
        assertEquals(SystemHostMode.EXCLUDE,
                ContentDriveHelper.systemHostModeFor(BrowseScope.ROOT, CHIP_ON));
        assertEquals(SystemHostMode.EXCLUDE,
                ContentDriveHelper.systemHostModeFor(BrowseScope.ROOT, CHIP_OFF));
    }

    /**
     * Given the System Host scope, Then System Host is the only thing listed, whatever the chip
     * says. Same reasoning as the root scope: the scope already answers the question the chip asks.
     */
    @Test
    public void testTheSystemHostScopeReturnsSystemHostAloneWhateverTheChipSays() {
        assertEquals(SystemHostMode.ONLY,
                ContentDriveHelper.systemHostModeFor(BrowseScope.SYSTEM_HOST, CHIP_ON));
        assertEquals(SystemHostMode.ONLY,
                ContentDriveHelper.systemHostModeFor(BrowseScope.SYSTEM_HOST, CHIP_OFF));
    }
}
