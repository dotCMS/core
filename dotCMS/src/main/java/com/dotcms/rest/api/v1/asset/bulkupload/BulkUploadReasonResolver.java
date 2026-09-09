package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetValidationException;
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

        if (isNameCollision(failure)) {
            return BatchFailureReason.NAME_COLLISION;
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

    /**
     * Whether this failure is the target already holding a file of that name.
     * <p>
     * <b>Recognised by type and structure, never by message text.</b> The product raises
     * {@link FileAssetValidationException} for a collision and marks the offending field as the
     * <b>host/folder</b> — because what is invalid is where the file was asked to go, not the file
     * itself. That pairing is what identifies it: the same exception class is also raised when a
     * binary fails validation, and there the invalid field is the binary. Matching on the message
     * would break the first time the language key is edited, which is the trap research R4
     * documented.
     * <p>
     * <b>This signature is not unique, and that is handled elsewhere.</b> A folder
     * {@code filesMasks} mismatch raises the same class and marks the same field
     * ({@code ESContentletAPIImpl#validateFileAsset}), differing only by the translated message —
     * so read from the exception alone the two are genuinely indistinguishable, and this reported a
     * filter mismatch as {@code NAME_COLLISION}, telling an author to rename a file whose name was
     * never the problem. The fix is not a cleverer signature: <b>both are now decided before the
     * create</b>, from the folder's own filter and a name lookup, where each is a fact
     * ({@code BulkUploadProcessor#folderRefusal}).
     * <p>
     * Which makes reaching here a <b>race</b>, and settles what to call it: a folder's filter does
     * not change between the pre-check and the create, while another batch taking the name is the
     * expected concurrent outcome this feature is specified for (FR-042, US6). So a collision is
     * not a guess here — it is the only one of the two that can actually arrive this way.
     * <p>
     * The collision itself is not this feature's rule. The unique index on the lower-cased path
     * already decided who wins — so this is <b>case-insensitive</b>, and {@code Report.pdf} and
     * {@code report.pdf} are one contended name (FR-042a). All that is added here is telling the
     * loser something they can act on: rename it. An unclassified failure sends them looking for a
     * problem with their file instead.
     */
    private boolean isNameCollision(final Exception failure) {

        // The whole cause chain, not one level: the workflow wraps the validation failure in a
        // DotRuntimeException before it reaches here, and a single getCause() check misses it.
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof DotContentletValidationException
                    && marksTheTargetInvalid((DotContentletValidationException) cause)) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    /**
     * Whether the validation failure blamed the <b>target</b> rather than the file.
     * <p>
     * That is the structural signature of a collision: what is invalid is where the file was asked
     * to go, not the file itself. The same exception class is raised when a binary fails
     * validation, and there the invalid field is the binary — which is how the two stay apart
     * without reading a translated message.
     */
    private boolean marksTheTargetInvalid(final DotContentletValidationException validation) {
        return validation.getNotValidFields().values().stream()
                .flatMap(List::stream)
                .anyMatch(field -> field != null
                        && FileAssetAPI.HOST_FOLDER_FIELD.equalsIgnoreCase(
                                field.getVelocityVarName()));
    }
}
