package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

/**
 * User information for permission responses.
 * Contains only safe, non-sensitive user data for embedding in permission contexts.
 *
 * @author hassandotcms
 * @since 24.01
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = UserInfoView.class)
@JsonDeserialize(as = UserInfoView.class)
@Schema(description = "User information")
public interface AbstractUserInfoView {

    /**
     * Gets the user identifier.
     *
     * @return User ID
     */
    @JsonProperty("id")
    @Schema(
        description = "User identifier",
        example = "admin@dotcms.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String id();

    /**
     * Gets the user's full name.
     *
     * @return User's full name
     */
    @JsonProperty("name")
    @Schema(
        description = "User's full name",
        example = "Admin User",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name();

    /**
     * Gets the user's email address.
     *
     * @return User's email
     */
    @JsonProperty("email")
    @Schema(
        description = "User's email address",
        example = "admin@dotcms.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email();
}
