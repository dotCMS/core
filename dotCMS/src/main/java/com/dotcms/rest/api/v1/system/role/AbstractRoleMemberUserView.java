package com.dotcms.rest.api.v1.system.role;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Minimal user payload carried by role-membership responses. Deliberately slim — display
 * fields only; clients needing the full user must call the users API.
 *
 * @author hassandotcms
 * @since Aug 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RoleMemberUserView.class)
@JsonDeserialize(as = RoleMemberUserView.class)
@Schema(description = "Minimal user payload: display fields only")
public interface AbstractRoleMemberUserView {

    /**
     * Id of the user.
     *
     * @return the user's id
     */
    @Schema(
            description = "Id of the user",
            example = "dotcms.org.2807",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String userId();

    /**
     * Email address of the user.
     *
     * @return the user's email address
     */
    @Schema(
            description = "Email address of the user",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email();

    /**
     * Full name of the user.
     *
     * @return the user's full name
     */
    @Schema(
            description = "Full name of the user",
            example = "Jane Doe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String fullName();
}
