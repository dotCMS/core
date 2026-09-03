package com.dotcms.rest.api.v1.system.role;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Result of bulk-removing users from a role through {@code DELETE /v1/roles/{roleId}/users}
 * (issue #36938). The batch is PARTIAL-SUCCESS: every removable membership is removed and
 * every non-removable entry is reported in {@code skipped} with a reason — the batch never
 * fails as a whole once the role resolves.
 *
 * @author hassandotcms
 * @since Aug 2026
 */
// jdkOnly: keeps generated accessors typed as plain java.util.List so the OpenAPI generator
// inlines the arrays instead of binding them to the SHARED ImmutableListString component
// schema (which would clobber other endpoints' docs)
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*", jdkOnly = true)
@Value.Immutable
@JsonSerialize(as = RoleUsersRemovalView.class)
@JsonDeserialize(as = RoleUsersRemovalView.class)
@Schema(description = "Bulk role-membership removal result with per-user outcomes")
public interface AbstractRoleUsersRemovalView {

    /**
     * Ids of the users whose direct membership was removed.
     *
     * @return removed user ids
     */
    @Schema(
            description = "Ids of the users whose direct membership in the role was removed",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<String> removedUserIds();

    /**
     * Users that could not be removed, each with a reason.
     *
     * @return skipped users with reasons
     */
    @Schema(
            description = "Users that could not be removed, each with a reason",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<SkippedUserView> skipped();
}
