package com.dotcms.jobs.business.processor.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link FolderBulkDuplicateProcessor#isSiteRoot(String)} and
 * {@link FolderBulkDuplicateProcessor#findCoveringAncestor(String, List)}: which submitted paths
 * are refused up front as a site root, which go on to be resolved, and which can cover another
 * (#37062, US3).
 * <p>
 * A path that is not site-qualified is not a site root. It is malformed, so it must reach the
 * resolver and come back {@code PATH_NOT_FOUND} rather than {@code PROTECTED_FOLDER} (FR-013).
 */
public class FolderBulkDuplicateSiteRootTest {

    /**
     * Given Scenario: A site-qualified path naming only the site, with and without its slash
     * ExpectedResult: Both are a site root
     */
    @Test
    public void test_isSiteRoot_siteOnly_true() {
        assertTrue(FolderBulkDuplicateProcessor.isSiteRoot("//demo.dotcms.com/"));
        assertTrue(FolderBulkDuplicateProcessor.isSiteRoot("//demo.dotcms.com"));
    }

    /**
     * Given Scenario: A site-qualified path naming a folder in the site
     * ExpectedResult: Not a site root
     */
    @Test
    public void test_isSiteRoot_folderInSite_false() {
        assertFalse(FolderBulkDuplicateProcessor.isSiteRoot("//demo.dotcms.com/blogs/"));
    }

    /**
     * Given Scenario: A single word with no site, as a malformed submission can carry
     * ExpectedResult: Not a site root, so it is resolved and reported as not found
     */
    @Test
    public void test_isSiteRoot_bareWord_false() {
        assertFalse(FolderBulkDuplicateProcessor.isSiteRoot("not a path"));
    }

    /**
     * Given Scenario: A folder path with no site in front of it
     * ExpectedResult: Not a site root, so it is resolved and reported as not found
     */
    @Test
    public void test_isSiteRoot_pathWithoutSite_false() {
        assertFalse(FolderBulkDuplicateProcessor.isSiteRoot("/blogs/"));
    }

    /**
     * Given Scenario: A folder submitted with its own parent folder
     * ExpectedResult: The parent covers it
     */
    @Test
    public void test_findCoveringAncestor_parentFolder_covers() {
        assertEquals("//demo.dotcms.com/blogs/", FolderBulkDuplicateProcessor.findCoveringAncestor(
                "//demo.dotcms.com/blogs/2026/",
                List.of("//demo.dotcms.com/blogs/2026/", "//demo.dotcms.com/blogs/")));
    }

    /**
     * Given Scenario: A folder submitted with its site root, and with a bare slash
     * ExpectedResult: Neither covers it: both are refused, so neither has a duplicate to carry it
     */
    @Test
    public void test_findCoveringAncestor_rootOrBareSlash_coversNothing() {
        assertNull(FolderBulkDuplicateProcessor.findCoveringAncestor("//demo.dotcms.com/blogs/",
                List.of("//demo.dotcms.com/blogs/", "//demo.dotcms.com/", "/")));
    }
}
