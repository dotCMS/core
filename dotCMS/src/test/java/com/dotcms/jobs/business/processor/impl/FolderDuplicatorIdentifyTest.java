package com.dotcms.jobs.business.processor.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.portlets.folders.model.Folder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

/**
 * Unit tests for {@link FolderDuplicator#identifyDuplicate(Set, List, String)}: telling which new
 * folder is this run's duplicate, since the shipped folder copy returns nothing (#37062, US2).
 */
public class FolderDuplicatorIdentifyTest {

    private static Folder folder(final String name) {
        final Folder folder = new Folder();
        folder.setName(name);
        return folder;
    }

    /**
     * Given Scenario: The parent gained one folder, named after the source plus _copy
     * ExpectedResult: That folder is the duplicate
     */
    @Test
    public void test_identify_findsTheOneNewCopy() {
        final Optional<Folder> found = FolderDuplicator.identifyDuplicate(
                Set.of("alpha", "beta"),
                List.of(folder("alpha"), folder("beta"), folder("alpha_copy")), "alpha");

        assertEquals("alpha_copy", found.orElseThrow().getName());
    }

    /**
     * Given Scenario: alpha_copy already existed, so the walk named the new one alpha_copy_copy
     * ExpectedResult: The new alpha_copy_copy is found, not the older alpha_copy
     */
    @Test
    public void test_identify_findsARepeatedSuffix() {
        final Optional<Folder> found = FolderDuplicator.identifyDuplicate(
                Set.of("alpha", "alpha_copy"),
                List.of(folder("alpha"), folder("alpha_copy"), folder("alpha_copy_copy")),
                "alpha");

        assertEquals("alpha_copy_copy", found.orElseThrow().getName());
    }

    /**
     * Given Scenario: A folder appeared that is not named after the source
     * ExpectedResult: It is ignored, so there is no duplicate to report
     */
    @Test
    public void test_identify_ignoresAnUnrelatedNewFolder() {
        assertTrue(FolderDuplicator.identifyDuplicate(Set.of("alpha"),
                List.of(folder("alpha"), folder("gamma")), "alpha").isEmpty());
    }

    /**
     * Given Scenario: No folder appeared at all
     * ExpectedResult: Empty
     */
    @Test
    public void test_identify_noNewFolder_isEmpty() {
        assertTrue(FolderDuplicator.identifyDuplicate(Set.of("alpha"),
                List.of(folder("alpha")), "alpha").isEmpty());
    }

    /**
     * Given Scenario: Two matching folders appeared, say another author duplicated the same folder
     * at the same moment
     * ExpectedResult: Empty. Guessing could put this run's content into someone else's duplicate,
     * so the ambiguous case fails safe.
     */
    @Test
    public void test_identify_twoNewCandidates_failsSafe() {
        assertTrue(FolderDuplicator.identifyDuplicate(Set.of("alpha"),
                List.of(folder("alpha"), folder("alpha_copy"), folder("alpha_copy_copy")),
                "alpha").isEmpty());
    }

    /**
     * Given Scenario: A new folder whose name only starts with the source name
     * ExpectedResult: Not a duplicate: alphabet_copy does not duplicate alpha
     */
    @Test
    public void test_identify_prefixOnlyMatch_isNotADuplicate() {
        assertTrue(FolderDuplicator.identifyDuplicate(Set.of("alpha"),
                List.of(folder("alpha"), folder("alphabet_copy")), "alpha").isEmpty());
    }
}
