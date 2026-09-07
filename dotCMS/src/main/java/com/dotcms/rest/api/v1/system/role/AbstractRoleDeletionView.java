package com.dotcms.rest.api.v1.system.role;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * Result of deleting a role through {@code DELETE /v1/roles/{roleId}}.
 * Reports the id of the removed role and how many users had the role at the moment of
 * deletion — the deletion cascades, so those users lost the role (issue #36939).
 *
 * @author hassandotcms
 * @since Aug 2026
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = RoleDeletionView.class)
@JsonDeserialize(as = RoleDeletionView.class)
@Schema(description = "Role deletion result with the cascade blast radius")
public interface AbstractRoleDeletionView {

    /**
     * Whether the role was deleted.
     *
     * @return {@code true} when the role was deleted
     */
    @Schema(
            description = "Whether the role was deleted",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean deleted();

    /**
     * Id of the deleted role.
     *
     * @return the deleted role's id
     */
    @Schema(
            description = "Id of the deleted role",
            example = "48190c8c-42c4-46af-8d1a-0cd5db894797",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String roleId();

    /**
     * Number of users the role was directly assigned to when it was deleted. The deletion
     * cascades: the role was removed from every one of these users. Users who only inherited
     * the role through the role hierarchy are not counted.
     *
     * @return count of directly-assigned users that lost the role
     */
    @Schema(
            description = "Number of users the role was removed from by the cascading deletion. "
                    + "Counts direct assignments only; users inheriting the role through the "
                    + "role hierarchy are not included",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    int usersAffected();
}
