package com.dotcms.rest.api.v1.asset.bulkdelete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link FolderBulkDeleteForm} — the shape only (#37063, contracts §1).
 * <p>
 * Deliberately minimal, matching {@code AbstractFolderDeletionRequestForm}'s own simplicity: the
 * form is a plain carrier for {@code assetPaths}, with no self-validation. The empty-selection and
 * over-the-maximum refusals (FR-004) need the configured ceiling and produce a specific
 * {@code errorCode}, so they are exercised where that is observable —
 * {@link FolderBulkDeleteResourceIT} (T015) — not here.
 */
public class FolderBulkDeleteFormTest {

    /**
     * Method to test: {@link FolderBulkDeleteForm#assetPaths()}
     * Given Scenario: A form built with several paths
     * ExpectedResult: The paths are held in the order given
     */
    @Test
    public void test_assetPaths_holdsGivenOrder() {
        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder()
                .assetPaths(List.of("//default/a/", "//default/b/", "//default/c/"))
                .build();

        assertEquals(List.of("//default/a/", "//default/b/", "//default/c/"), form.assetPaths());
    }

    /**
     * Method to test: {@link FolderBulkDeleteForm#assetPaths()}
     * Given Scenario: A form built with no paths at all
     * ExpectedResult: The form itself builds without error — an empty list is a valid *shape*, even
     * though it is refused as a submission (FR-004). Business validation belongs to
     * FolderBulkDeleteHelper, not the form.
     */
    @Test
    public void test_assetPaths_emptyList_isAValidShape() {
        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder().build();

        assertTrue(form.assetPaths().isEmpty());
    }
}
