package com.dotcms.rest.api.v1.system.role;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Result of granting a role to a user through {@code POST /v1/roles/{roleId}/users/{userId}}
 * (issue #36937).
 *
 * The grant is idempotent: {@code granted} is {@code true} whenever the user holds the role
 * after the call, including when the user already held it (directly or by inheritance) and
 * nothing changed — legacy {@code RoleAPIImpl.addRoleToUser} silently no-ops in that case.
 *
 * @author hassandotcms
 * @since Aug 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RoleUserGrantView.class)
@JsonDeserialize(as = RoleUserGrantView.class)
@Schema(description = "Role grant result")
public interface AbstractRoleUserGrantView {

    /**
     * Whether the user holds the role after the call.
     *
     * @return {@code true} when the user holds the role after the call
     */
    @Schema(
            description = "Whether the user holds the role after the call. The grant is "
                    + "idempotent, so this is also true when the user already held the role "
                    + "and nothing changed",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean granted();

    /**
     * Id of the granted role.
     *
     * @return the granted role's id
     */
    @Schema(
            description = "Id of the granted role",
            example = "48190c8c-42c4-46af-8d1a-0cd5db894797",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String roleId();

    /**
     * The user the role was granted to.
     *
     * @return minimal payload of the target user
     */
    @Schema(
            description = "The user the role was granted to",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    RoleMemberUserView user();
}
