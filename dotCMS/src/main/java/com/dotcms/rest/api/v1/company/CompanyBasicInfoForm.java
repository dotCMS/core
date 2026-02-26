package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.dotmarketing.util.UtilMethods;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

/**
 * Form for saving company basic information and branding settings.
 * Field names use semantic names that map to the Liferay Company model internally.
 *
 * @author hassandotcms
 */
@Schema(description = "Company basic information and branding settings")
public class CompanyBasicInfoForm extends Validated {

    @JsonProperty("portalURL")
    @Schema(description = "Portal URL for the dotCMS instance", example = "http://localhost:8080",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "portalURL is required")
    private final String portalURL;

    @JsonProperty("emailAddress")
    @Schema(description = "Company email address used as the system sender", example = "admin@dotcms.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "emailAddress is required")
    private final String emailAddress;

    @JsonProperty("mx")
    @Schema(description = "Mail exchange domain (derived from emailAddress if not provided)", example = "dotcms.com")
    private final String mx;

    @JsonProperty("primaryColor")
    @Schema(description = "Primary branding color (hex)", example = "#C336E5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "primaryColor is required")
    private final String primaryColor;

    @JsonProperty("secondaryColor")
    @Schema(description = "Secondary branding color (hex)", example = "#54428E",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "secondaryColor is required")
    private final String secondaryColor;

    @JsonProperty("backgroundColor")
    @Schema(description = "Background branding color (hex)", example = "#3C1361")
    private final String backgroundColor;

    @JsonProperty("backgroundImage")
    @Schema(description = "Background image path (dotAsset path)", example = "/dA/abc123/background.png")
    private final String backgroundImage;

    @JsonProperty("loginScreenLogo")
    @Schema(description = "Login screen logo path (dotAsset path starting with /dA)", example = "/dA/abc123/logo.png")
    private final String loginScreenLogo;

    @JsonProperty("navBarLogo")
    @Schema(description = "Navigation bar logo path (dotAsset path starting with /dA, Enterprise only)",
            example = "/dA/abc123/nav-logo.png")
    private final String navBarLogo;

    @JsonCreator
    public CompanyBasicInfoForm(
            @JsonProperty("portalURL") final String portalURL,
            @JsonProperty("emailAddress") final String emailAddress,
            @JsonProperty("mx") final String mx,
            @JsonProperty("primaryColor") final String primaryColor,
            @JsonProperty("secondaryColor") final String secondaryColor,
            @JsonProperty("backgroundColor") final String backgroundColor,
            @JsonProperty("backgroundImage") final String backgroundImage,
            @JsonProperty("loginScreenLogo") final String loginScreenLogo,
            @JsonProperty("navBarLogo") final String navBarLogo) {
        this.portalURL = portalURL;
        this.emailAddress = emailAddress;
        this.mx = mx;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.backgroundColor = backgroundColor;
        this.backgroundImage = backgroundImage;
        this.loginScreenLogo = loginScreenLogo;
        this.navBarLogo = navBarLogo;
    }

    @Override
    public void checkValid() {
        super.checkValid();

        if (!UtilMethods.isSet(portalURL)) {
            throw new BadRequestException("portalURL is required");
        }
        if (!UtilMethods.isSet(emailAddress)) {
            throw new BadRequestException("emailAddress is required");
        }
        if (!UtilMethods.isSet(primaryColor)) {
            throw new BadRequestException("primaryColor is required");
        }
        if (!UtilMethods.isSet(secondaryColor)) {
            throw new BadRequestException("secondaryColor is required");
        }
        if (UtilMethods.isSet(loginScreenLogo) && !loginScreenLogo.startsWith("/dA")) {
            throw new BadRequestException("loginScreenLogo must be a dotAsset path starting with /dA");
        }
        if (UtilMethods.isSet(navBarLogo) && !navBarLogo.startsWith("/dA")) {
            throw new BadRequestException("navBarLogo must be a dotAsset path starting with /dA");
        }
    }

    public String getPortalURL() {
        return portalURL;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getMx() {
        return mx;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public String getLoginScreenLogo() {
        return loginScreenLogo;
    }

    public String getNavBarLogo() {
        return navBarLogo;
    }
}
