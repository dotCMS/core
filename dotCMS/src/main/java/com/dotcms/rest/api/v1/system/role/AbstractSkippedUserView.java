package com.dotcms.rest.api.v1.system.role;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * A user the bulk-removal endpoint could not remove, with the reason
 * (issue #36938).
 *
 * @author hassandotcms
 * @since Aug 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = SkippedUserView.class)
@JsonDeserialize(as = SkippedUserView.class)
@Schema(description = "A user skipped by the bulk removal, with the reason")
public interface AbstractSkippedUserView {

    /** No user matches the submitted id. */
    String REASON_NOT_FOUND = "not_found";
    /** Not a direct member: held only through the role hierarchy, or not a member at all. */
    String REASON_INHERITED = "inherited";
    /** Unexpected per-user failure, logged server-side. */
    String REASON_ERROR = "error";

    /**
     * Id of the skipped user, as submitted in the request.
     *
     * @return the skipped user's id
     */
    @Schema(
            description = "Id of the skipped user, as submitted in the request",
            example = "dotcms.org.2807",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String userId();

    /**
     * Why the user was skipped: {@code not_found} — no user matches the id;
     * {@code inherited} — the user is not a direct member of the role (holds it only through
     * the role hierarchy, or not at all), so there is no direct membership to remove;
     * {@code error} — an unexpected per-user failure, logged server-side.
     *
     * @return the skip reason
     */
    @Schema(
            description = "Why the user was skipped",
            allowableValues = {REASON_NOT_FOUND, REASON_INHERITED, REASON_ERROR},
            example = REASON_INHERITED,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String reason();
}
