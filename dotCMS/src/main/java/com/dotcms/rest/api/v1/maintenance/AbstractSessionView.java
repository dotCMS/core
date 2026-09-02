package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Immutable view of a single active HTTP session as displayed in the Logged Users tab.
 * <p>
 * The real session id is never exposed; clients receive {@link #token()} (HMAC-SHA256
 * of the session id keyed by the caller's CSRF secret) and pass it back to
 * {@code DELETE /v1/maintenance/_sessions/{token}}.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = SessionView.class)
@JsonDeserialize(as = SessionView.class)
@Schema(description = "Active HTTP session entry exposed in the Logged Users tab")
public interface AbstractSessionView {

    @Schema(
            description = "HMAC-obfuscated session token. Pass this value back to "
                    + "DELETE /v1/maintenance/_sessions/{token} to invalidate the session.",
            example = "a1b2c3d4e5f6g7h8",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String token();

    @Schema(
            description = "True if this entry represents the caller's own session.",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean isCurrent();

    @Schema(
            description = "User id associated with the session (anonymous user id if no login).",
            example = "dotcms.org.1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String userId();

    @Nullable
    @Schema(
            description = "Email of the user associated with the session. Null for sessions "
                    + "without an associated user account.",
            example = "admin@dotcms.com"
    )
    String userEmail();

    @Nullable
    @Schema(
            description = "Full name of the user associated with the session. Null for sessions "
                    + "without an associated user account.",
            example = "Admin User"
    )
    String userFullName();

    @Nullable
    @Schema(
            description = "Remote IP address recorded when the session was created. Null if "
                    + "the address was never captured (e.g. the session was created outside the "
                    + "servlet request pipeline that records it).",
            example = "192.168.1.100"
    )
    String address();

    @Nullable
    @Schema(
            description = "Human-readable elapsed time since the session was created.",
            example = "2 hours ago"
    )
    String sessionTime();
}
