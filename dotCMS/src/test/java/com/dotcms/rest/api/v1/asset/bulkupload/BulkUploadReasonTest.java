package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

/**
 * Unit tests for {@link BulkUploadReasonResolver} (spec FR-016, FR-016a; research R4).
 * <p>
 * <b>The point of every test here is that the reason is a fact, not a guess.</b> The reason is what
 * the client turns into product copy, so getting it wrong tells an author their file was the wrong
 * type when it was merely too large — and sends them off to fix the wrong thing. The validation
 * layer cannot tell those two apart without reading translated text, which is exactly why these
 * checks happen before anything is created.
 */
public class BulkUploadReasonTest {

    private static final long TEN_MB = 10L * 1024 * 1024;

    private final BulkUploadReasonResolver resolver = new BulkUploadReasonResolver();

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: A file inside the ceiling whose media type is on the allow list.
     * <p>
     * Expected result: No reason — it may be attempted. The pre-check exists to fail fast, not to
     * become a second gate that rejects what the product would accept (FR-006).
     */
    @Test
    public void test_preCheck_passesAFileThatBreaksNoRule() {
        assertFalse(resolver.preCheck(1024L, "image/png", TEN_MB, List.of("image/*")).isPresent());
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: A file over the ceiling that applies.
     * <p>
     * Expected result: {@code OVER_SIZE_LIMIT}, and not the type reason. The size is compared
     * against what staging <b>measured</b> (FR-013), because a caller can under-declare a size but
     * cannot under-report a measurement.
     */
    @Test
    public void test_preCheck_namesTheSizeRuleWhenTheFileIsTooLarge() {
        final Optional<BatchFailureReason> reason =
                resolver.preCheck(TEN_MB + 1, "image/png", TEN_MB, List.of("image/*"));

        assertTrue(reason.isPresent());
        assertEquals(BatchFailureReason.OVER_SIZE_LIMIT, reason.get());
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: A file whose resolved media type is not on the allow list.
     * <p>
     * Expected result: {@code DISALLOWED_FILE_TYPE}. This is a <b>media-type</b> rule, not a
     * file-extension one (FR-012a): the type is resolved by content detection, so renaming
     * {@code payload.exe} to {@code payload.png} does not get it past the rule — and the copy the
     * client shows must not promise otherwise.
     */
    @Test
    public void test_preCheck_namesTheTypeRuleWhenTheMediaTypeIsNotAllowed() {
        final Optional<BatchFailureReason> reason = resolver.preCheck(
                1024L, "application/x-msdownload", TEN_MB, List.of("image/*", "application/pdf"));

        assertTrue(reason.isPresent());
        assertEquals(BatchFailureReason.DISALLOWED_FILE_TYPE, reason.get());
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: A file that is both too large and of a disallowed type.
     * <p>
     * Expected result: <b>Size wins.</b> One file carries one reason, so the order has to be fixed
     * rather than incidental — and size is the one the author can act on without understanding the
     * content type's configuration.
     */
    @Test
    public void test_preCheck_reportsSizeFirstWhenAFileBreaksBothRules() {
        final Optional<BatchFailureReason> reason = resolver.preCheck(
                TEN_MB + 1, "application/x-msdownload", TEN_MB, List.of("image/*"));

        assertEquals(BatchFailureReason.OVER_SIZE_LIMIT, reason.orElseThrow());
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: A content type that declares no allow list, and one that declares no ceiling.
     * <p>
     * Expected result: Neither rule fires. Out of the box a content type restricts nothing, and
     * FR-006 requires a batch to accept what a single upload accepts — so an empty allow list means
     * "everything", never "nothing", and a {@code -1} ceiling means unbounded. Reading the empty
     * case the other way would reject every file on a default installation.
     */
    @Test
    public void test_preCheck_treatsAnUnconfiguredRuleAsUnrestricted() {
        assertFalse("no allow list means every type is allowed",
                resolver.preCheck(1024L, "application/x-msdownload", TEN_MB, List.of()).isPresent());
        assertFalse("a -1 ceiling means unbounded",
                resolver.preCheck(Long.MAX_VALUE, "image/png", -1L, List.of("image/*")).isPresent());
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: A file whose media type staging could not resolve.
     * <p>
     * Expected result: Accepted, not rejected. The product skips its own type check when the media
     * type is unresolvable (FR-012b), and this feature inherits that rather than tightening it —
     * tightening would break FR-006. The consequence is recorded in the spec: the copy must not
     * tell an author that every file was checked.
     */
    @Test
    public void test_preCheck_acceptsAFileWhoseMediaTypeCouldNotBeResolved() {
        assertFalse(resolver.preCheck(1024L, null, TEN_MB, List.of("image/*")).isPresent());
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#classify}
     * <p>
     * Given scenario: The failures that only appear while creating a file.
     * <p>
     * Expected result: Each is named from the exception <b>type</b>, never from its message. A
     * permission failure and a name collision are different problems for the author — one needs an
     * administrator, the other needs a different file name — and reporting both as "failed" makes
     * the per-file outcome worthless.
     */
    @Test
    public void test_classify_namesFailuresFromTheExceptionTypeNotItsMessage() {
        assertEquals(BatchFailureReason.PERMISSION_DENIED,
                resolver.classify(new DotSecurityException("nope")));
        assertEquals(BatchFailureReason.UNCLASSIFIED,
                resolver.classify(new IllegalStateException("something else")));
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#classify}
     * <p>
     * Given scenario: A validation exception, which the product raises for size, type <b>and</b>
     * other rules with the same class.
     * <p>
     * Expected result: {@code UNCLASSIFIED}, not a guess between them. Anything this exception
     * could mean that the author can act on was already decided by the pre-check from measured
     * facts; guessing here from message text is what R4 rejected, and a wrong guess is worse than
     * an honest "unclassified" because the author acts on it.
     */
    @Test
    public void test_classify_refusesToGuessBetweenTheCasesThatShareAnExceptionClass() {
        assertEquals(BatchFailureReason.UNCLASSIFIED,
                resolver.classify(new DotContentletValidationException("exceeds the size")));
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: The allow-list entries that admit everything — {@code *}, {@code * / *} and a
     * blank rule.
     * <p>
     * Expected result: Every media type passes.
     * <p>
     * <b>This is the branch a default installation runs</b>, and it was the one branch of
     * {@code accepts} with no test: the existing cases use {@code image/*} and a non-matching type,
     * so they exercise the wildcard *suffix* and the miss, never the total wildcard. Getting it
     * wrong rejects every file on an install that restricts nothing — a failure that would look
     * like the feature being broken outright rather than like a rule being misread.
     */
    @Test
    public void test_preCheck_admitsEverythingUnderATotalWildcard() {
        for (final String rule : new String[]{"*", "*/*", "  "}) {
            assertTrue("rule '" + rule + "' must admit any media type",
                    resolver.preCheck(10L, "application/x-msdownload", -1L,
                            List.of(rule)).isEmpty());
        }
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: An allow list naming one exact media type, and a file of that exact type.
     * <p>
     * Expected result: Accepted.
     * <p>
     * The positive exact match had no test either — only the miss did. A rule that rejected
     * everything would have passed the existing suite, because every assertion about an exact rule
     * was an assertion about a file that did not match it.
     */
    @Test
    public void test_preCheck_admitsAnExactMatchAndIsNotCaseSensitiveAboutIt() {
        assertTrue("an exact rule admits its own type",
                resolver.preCheck(10L, "application/pdf", -1L,
                        List.of("application/pdf")).isEmpty());

        assertTrue("and the comparison is case-insensitive on both sides",
                resolver.preCheck(10L, "Application/PDF", -1L,
                        List.of(" APPLICATION/pdf ")).isEmpty());
    }

    /**
     * Method to test: {@link BulkUploadReasonResolver#preCheck}
     * <p>
     * Given scenario: An allow list of several entries where only the last one matches.
     * <p>
     * Expected result: Accepted — the list is a set of alternatives, not a sequence where the first
     * entry decides.
     */
    @Test
    public void test_preCheck_admitsAMatchAnywhereInTheList() {
        assertTrue(resolver.preCheck(10L, "application/pdf", -1L,
                List.of("image/*", "text/plain", "application/pdf")).isEmpty());
    }

}
