package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import java.util.List;
import java.util.Optional;

/**
 * Decides <b>why</b> a file failed, in terms a client can map to product copy (spec FR-016).
 * <p>
 * <b>Why this is a pre-check rather than exception handling.</b> The validation layer this feature
 * reuses reports an over-size file and a disallowed type through the <i>same</i>
 * {@code DotContentletValidationException}, differing only by a translated message string
 * (research R4). Matching on that text would break the first time a language key is edited, and
 * extending the validation layer to carry codes would edit a heavily-used legacy path far beyond
 * this feature's blast radius.
 * <p>
 * Staging already reported the <b>measured size</b> and the <b>resolved media type</b> for every
 * file, so both are facts before anything is created. Checking them here makes the reason a fact
 * too. What cannot be pre-checked — a name that was free a moment ago, a permission narrower than
 * the target's — is classified from the exception, where the <i>type</i> is enough and the message
 * is not needed.
 *
 * @author dotCMS
 */
public class BulkUploadReasonResolver {

    /**
     * Checks a staged file against the rules that are knowable before creating anything.
     *
     * @param sizeBytes       what staging measured, never a declared figure (FR-013)
     * @param mimeType        what staging resolved, by detection rather than by trusting the name
     * @param effectiveCeiling the ceiling that applies: the content type's own where declared,
     *                        otherwise the configured fallback; {@code -1} for unbounded
     * @param acceptedTypes   the content type's allow list, empty when it declares none
     * @return the reason this file must fail, or empty when it may be attempted
     */
    public Optional<BatchFailureReason> preCheck(final long sizeBytes,
                                                 final String mimeType,
                                                 final long effectiveCeiling,
                                                 final List<String> acceptedTypes) {

        // Size first, deliberately. A file can break both rules and carries only one reason, so the
        // order has to be fixed rather than incidental — and size is the one an author can act on
        // without understanding how the content type is configured.
        if (effectiveCeiling > 0 && sizeBytes > effectiveCeiling) {
            return Optional.of(BatchFailureReason.OVER_SIZE_LIMIT);
        }

        // An unresolvable media type is accepted, not rejected: the product skips its own type
        // check in this case (FR-012b) and this feature inherits that rather than tightening it,
        // because tightening would break FR-006's equivalence with the single-file upload.
        if (!UtilMethods.isSet(mimeType)) {
            return Optional.empty();
        }

        // No allow list means every type is allowed. Reading the empty case the other way would
        // reject every file on a default installation, where content types restrict nothing.
        if (acceptedTypes == null || acceptedTypes.isEmpty()) {
            return Optional.empty();
        }

        return acceptedTypes.stream().anyMatch(accepted -> accepts(accepted, mimeType))
                ? Optional.empty()
                : Optional.of(BatchFailureReason.DISALLOWED_FILE_TYPE);
    }

    /**
     * Whether one entry of the allow list admits this media type.
     * <p>
     * Answers a yes/no question, which is why it does not go through the product's
     * {@code BaseTypeMimeTypeMatcher}: that exists to pick the <i>best</i> matching content type
     * among several, with a precedence model this has no use for. The syntax it accepts is the
     * same — an exact type, a {@code type/*} wildcard, or a total wildcard.
     */
    private boolean accepts(final String accepted, final String mimeType) {

        final String rule = accepted == null ? "" : accepted.trim().toLowerCase();
        final String type = mimeType.trim().toLowerCase();

        if (rule.isEmpty() || "*".equals(rule) || "*/*".equals(rule)) {
            return true;
        }
        if (rule.endsWith("/*")) {
            return type.startsWith(rule.substring(0, rule.length() - 1));
        }
        return rule.equals(type);
    }

    /**
     * Classifies a failure that only became visible while creating the file.
     */
    public BatchFailureReason classify(final Exception failure) {

        if (failure instanceof DotSecurityException) {
            return BatchFailureReason.PERMISSION_DENIED;
        }

        // Everything else is UNCLASSIFIED on purpose, including the validation exception the
        // product raises for size AND type with the same class. Both of those were already decided
        // by preCheck from measured facts, so reaching here means something nobody anticipated —
        // and a wrong reason is worse than an honest unclassified one, because the author acts on
        // it. Guessing from the message text is what research R4 rejected: the size and type cases
        // differ only by a translated string, so the guess breaks the first time a language key is
        // edited.
        return BatchFailureReason.UNCLASSIFIED;
    }
}
