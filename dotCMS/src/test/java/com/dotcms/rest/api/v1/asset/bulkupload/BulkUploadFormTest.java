package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.dotcms.rest.exception.ValidationException;
import org.junit.Test;

/**
 * Unit tests for {@link BulkUploadForm} — the shape validation of a submission (spec FR-003,
 * FR-004; contracts §1).
 * <p>
 * Everything here is decidable from the request alone, which is why it is a unit test. The two
 * checks that need the database — that the target exists, and that the author may add children to
 * it — are asserted against a real target in {@code BulkUploadResourceIT}.
 * <p>
 * What these protect is FR-004: a refusal has to say <b>which</b> problem it was. "Too many files"
 * and "no target" have different fixes, and an author handed one opaque 400 cannot act on it.
 */
public class BulkUploadFormTest {

    /**
     * Method to test: {@link BulkUploadForm} constructor
     * <p>
     * Given scenario: A well-formed submission targeting a folder.
     * <p>
     * Expected result: Accepted, with the declared total left absent — it is optional, and its
     * absence must not be confused with zero. A batch whose caller declared nothing is still
     * bounded, just later: the authoritative total accumulates while the body is read.
     */
    @Test
    public void test_form_acceptsAFolderTargetWithNoDeclaredTotal() {
        final BulkUploadForm form = new BulkUploadForm("DOTASSET", "folder-123", null, null);

        assertEquals("DOTASSET", form.getBaseType());
        assertEquals("folder-123", form.getFolderId());
        assertNull(form.getSiteId());
        assertNull("absent is not zero", form.getTotalSizeBytes());
    }

    /**
     * Method to test: {@link BulkUploadForm} constructor
     * <p>
     * Given scenario: A submission at a site root, which carries a site id and no folder.
     * <p>
     * Expected result: Accepted. Both targets are legitimate; what is not legitimate is sending
     * neither or both.
     */
    @Test
    public void test_form_acceptsASiteRootTarget() {
        final BulkUploadForm form = new BulkUploadForm("FILEASSET", null, "site-abc", 812345L);

        assertEquals("site-abc", form.getSiteId());
        assertEquals(Long.valueOf(812345L), form.getTotalSizeBytes());
    }

    /**
     * Method to test: {@link BulkUploadForm} constructor
     * <p>
     * Given scenario: Neither {@code folderId} nor {@code siteId} is supplied.
     * <p>
     * Expected result: Refused. There is no default target — creating the batch somewhere the
     * author did not choose is worse than refusing it.
     */
    @Test
    public void test_form_refusesWhenNeitherTargetIsGiven() {
        assertThrows(ValidationException.class,
                () -> new BulkUploadForm("DOTASSET", null, null, null));
    }

    /**
     * Method to test: {@link BulkUploadForm} constructor
     * <p>
     * Given scenario: Both {@code folderId} and {@code siteId} are supplied.
     * <p>
     * Expected result: Refused. Two targets is not a merge and not a preference order — the caller
     * has said two contradictory things and the server must not pick one. This is why the contract
     * has two explicit fields rather than one overloaded string (ADR-0020): the overloading is
     * exactly what made the ambiguity possible in the first place.
     */
    @Test
    public void test_form_refusesWhenBothTargetsAreGiven() {
        assertThrows(ValidationException.class,
                () -> new BulkUploadForm("DOTASSET", "folder-123", "site-abc", null));
    }

    /**
     * Method to test: {@link BulkUploadForm} constructor
     * <p>
     * Given scenario: A base type this feature does not create.
     * <p>
     * Expected result: Refused. Only {@code DOTASSET} and {@code FILEASSET} are file-bearing types;
     * anything else would reach the creation path with no binary to put anywhere.
     */
    @Test
    public void test_form_refusesAnUnsupportedBaseType() {
        assertThrows(ValidationException.class,
                () -> new BulkUploadForm("HTMLPAGE", "folder-123", null, null));
        assertThrows(ValidationException.class,
                () -> new BulkUploadForm(null, "folder-123", null, null));
    }

    /**
     * Method to test: {@link BulkUploadForm} constructor
     * <p>
     * Given scenario: A declared total that is negative.
     * <p>
     * Expected result: Refused as malformed rather than ignored. The declared total only ever
     * causes a refusal, never an acceptance, so a nonsensical one cannot let a batch through — but
     * silently discarding it would hide a broken client from itself.
     */
    @Test
    public void test_form_refusesANegativeDeclaredTotal() {
        assertThrows(ValidationException.class,
                () -> new BulkUploadForm("DOTASSET", "folder-123", null, -1L));
    }
}
