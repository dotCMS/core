package com.dotcms.rest.api.v1.system.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * User information for permission responses.
 * Contains only safe, non-sensitive user data for embedding in permission contexts.
 *
 * @author dotCMS
 * @since 24.01
 */
@Schema(description = "User information")
public class UserInfoView {

    @JsonProperty("id")
    @Schema(
        description = "User identifier",
        example = "admin@dotcms.com",
        required = true
    )
    private final String id;

    @JsonProperty("name")
    @Schema(
        description = "User's full name",
        example = "Admin User",
        required = true
    )
    private final String name;

    @JsonProperty("email")
    @Schema(
        description = "User's email address",
        example = "admin@dotcms.com",
        required = true
    )
    private final String email;

    /**
     * Constructs user information.
     *
     * @param id User identifier
     * @param name User's full name
     * @param email User's email address
     */
    public UserInfoView(final String id, final String name, final String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
