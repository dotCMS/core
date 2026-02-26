package com.dotcms.rest.api.v1.company;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Immutable view representing the company configuration for the admin UI.
 * Maps Liferay Company model fields to semantic names.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = CompanyConfigView.class)
@JsonDeserialize(as = CompanyConfigView.class)
@Schema(description = "Company configuration including branding, authentication, and metadata")
public interface AbstractCompanyConfigView {

    @Schema(
            description = "Company identifier",
            example = "dotcms.org",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String companyId();

    @Schema(
            description = "Company display name",
            example = "dotcms.org",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String companyName();

    @Schema(
            description = "Portal URL for the dotCMS instance",
            example = "http://localhost:8080",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String portalURL();

    @Schema(
            description = "Company email address used as the system sender",
            example = "admin@dotcms.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String emailAddress();

    @Schema(
            description = "Mail exchange domain for outgoing email",
            example = "dotcms.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String mx();

    @Schema(
            description = "Primary branding color (hex)",
            example = "#C336E5"
    )
    @Nullable
    String primaryColor();

    @Schema(
            description = "Secondary branding color (hex)",
            example = "#54428E"
    )
    @Nullable
    String secondaryColor();

    @Schema(
            description = "Background branding color (hex)",
            example = "#3C1361"
    )
    @Nullable
    String backgroundColor();

    @Schema(
            description = "Background image path (dotAsset path)",
            example = "/dA/abc123/background.png"
    )
    @Nullable
    String backgroundImage();

    @Schema(
            description = "Login screen logo path (dotAsset path starting with /dA)",
            example = "/dA/abc123/logo.png"
    )
    @Nullable
    String loginScreenLogo();

    @Schema(
            description = "Navigation bar logo path (dotAsset path starting with /dA, Enterprise only)",
            example = "/dA/abc123/nav-logo.png"
    )
    @Nullable
    String navBarLogo();

    @Schema(
            description = "Authentication type: 'emailAddress' or 'userId'",
            example = "emailAddress",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String authType();

    @Schema(
            description = "SHA-256 digest of the company security key (read-only, admin only)"
    )
    @Nullable
    String keyDigest();
}
