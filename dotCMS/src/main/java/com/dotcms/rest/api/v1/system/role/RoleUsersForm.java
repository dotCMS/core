package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * Request body for {@code DELETE /v1/roles/{roleId}/users} (issue #36938): the ids of the
 * users to remove from the role.
 *
 * Note: the empty check lives in {@link #checkValid()} rather than a {@code @NotEmpty}
 * constraint — collection constraints have not been reliable with the Hibernate Validator
 * setup here.
 *
 * @author hassandotcms
 * @since Aug 2026
 */
public class RoleUsersForm extends Validated {

    @NotNull
    @Schema(
            description = "Ids of the users to remove from the role",
            example = "[\"dotcms.org.2807\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final Set<String> userIds;

    @JsonCreator
    public RoleUsersForm(@JsonProperty("userIds") final Set<String> userIds) {
        super();
        this.userIds = userIds;
    }

    public Set<String> getUserIds() {
        return this.userIds;
    }

    @Override
    public void checkValid() {
        super.checkValid();
        if (this.userIds.isEmpty()) {
            throw new BadRequestException("userIds must not be empty");
        }
        // Jackson accepts null elements in a JSON array bound to Set<String>; unchecked, a null
        // or blank entry would surface as a per-user 500/"error" deep in the batch
        if (this.userIds.stream().anyMatch(id -> !UtilMethods.isSet(id))) {
            throw new BadRequestException("userIds must not contain null or blank entries");
        }
    }
}
