package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * Pins the status each ceiling refusal answers with (contracts §1, FR-004, FR-010a, FR-013c.2).
 * <p>
 * <b>This is the layer the whole suite used to skip.</b> Every other test drives
 * {@code BulkUploadHelper} directly and asserts the <i>exception type</i>, which was always
 * correct — while the status was decided above the helper, by a mapper that did not exist, so
 * every refusal answered <b>500</b>. The Postman collection was the only thing checking the real
 * status, and it was asserting 413 against a 500 for weeks.
 * <p>
 * <b>What 500 costs, beyond being the wrong number.</b> It tells a client the <i>server</i> broke,
 * so anything retrying on server errors re-uploads the whole batch, is refused again for the same
 * reason, and has spent the bytes twice — and the batch most likely to be retried that way is the
 * one refused for being too large.
 * <p>
 * The two ceilings stay distinguishable on purpose (FR-004): "too many files" and "too much data"
 * have different fixes, and an author handed one opaque error can act on neither.
 *
 * @author dotCMS
 */
public class BulkUploadRefusedExceptionMapperTest {

    private final BulkUploadRefusedExceptionMapper mapper = new BulkUploadRefusedExceptionMapper();

    /**
     * Method to test: {@link BulkUploadRefusedExceptionMapper#toResponse}
     * <p>
     * Given scenario: A batch refused for carrying more files than the ceiling allows.
     * <p>
     * Expected result: <b>400</b> — the request is wrong, not the server.
     * <p>
     * This is the ceiling enforced <b>while the body is read</b> (FR-010a), which is the
     * authoritative one and the one no test reached before: the Postman collection covers the
     * empty-batch and bad-baseType 400s, both decided before any part is touched.
     */
    @Test
    public void test_theFileCountCeiling_answers400() {
        final Response response = mapper.toResponse(new BulkUploadRefusedException(
                BulkUploadRefusedException.Ceiling.FILE_COUNT,
                "Batch exceeds the maximum of 100 files"));

        assertEquals("too many files is a bad request, not a server fault",
                Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * Method to test: {@link BulkUploadRefusedExceptionMapper#toResponse}
     * <p>
     * Given scenario: A batch refused for crossing the total-size ceiling.
     * <p>
     * Expected result: <b>413</b>, and specifically not the 400 its sibling gets.
     * <p>
     * Covers the <b>accumulated measured</b> total — the enforcement point (FR-013c.2). Postman
     * covers the *declared* total, which the contract itself calls "a convenience and not the
     * enforcement point", so the path that actually bounds what reaches disk was unasserted at
     * status level.
     */
    @Test
    public void test_theTotalSizeCeiling_answers413() {
        final Response response = mapper.toResponse(new BulkUploadRefusedException(
                BulkUploadRefusedException.Ceiling.TOTAL_SIZE,
                "Batch exceeds the maximum total size of 1073741824 bytes"));

        assertEquals("a size problem answers 413, so the client can tell it from a shape problem",
                413, response.getStatus());
    }

    /**
     * Method to test: {@link BulkUploadRefusedExceptionMapper#toResponse}
     * <p>
     * Given scenario: The two ceilings, side by side.
     * <p>
     * Expected result: Different statuses.
     * <p>
     * The negative control. Asserting each status alone would still pass if a later change
     * collapsed both onto one — which is precisely the outcome FR-004 forbids, and the reason the
     * exception carries which ceiling it was in the first place.
     */
    @Test
    public void test_theTwoCeilings_stayDistinguishable() {
        final int fileCount = mapper.toResponse(new BulkUploadRefusedException(
                BulkUploadRefusedException.Ceiling.FILE_COUNT, "too many")).getStatus();
        final int totalSize = mapper.toResponse(new BulkUploadRefusedException(
                BulkUploadRefusedException.Ceiling.TOTAL_SIZE, "too big")).getStatus();

        org.junit.Assert.assertNotEquals(
                "\"too many files\" and \"too much data\" have different fixes; one status for "
                        + "both leaves the author unable to act on either",
                fileCount, totalSize);
    }
}
