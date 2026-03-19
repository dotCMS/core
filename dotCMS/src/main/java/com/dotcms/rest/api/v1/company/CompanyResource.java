package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * REST resource for managing company configuration settings including
 * branding, authentication type, and security key regeneration.
 * Replaces the legacy form-encoded endpoints in CMSConfigResource.
 *
 * @author hassandotcms
 */
@Path("/v1/company")
@SwaggerCompliant(value = "Company configuration and system settings APIs", batch = 3)
@Tag(name = "Company Configuration", description = "Company settings and branding management")
public class CompanyResource {

    private final WebResource webResource;
    private final CompanyConfigHelper helper;

    public CompanyResource() {
        this(new WebResource(), new CompanyConfigHelper());
    }

    @VisibleForTesting
    public CompanyResource(final WebResource webResource,
                           final CompanyConfigHelper helper) {
        this.webResource = webResource;
        this.helper = helper;
    }

    /**
     * Returns the full company configuration for the admin editing UI.
     * Includes branding colors, logos, auth type, and metadata.
     */
    @Operation(
            summary = "Get company configuration",
            description = "Returns the full company configuration including branding, "
                    + "authentication settings, and metadata for the admin editing UI."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Company configuration retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityCompanyConfigView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityCompanyConfigView getCompanyConfig(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response) {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        return new ResponseEntityCompanyConfigView(helper.getCompanyConfig(user));
    }

    /**
     * Saves company basic information including portal URL, email, and branding settings.
     * Replaces the legacy POST /api/config/saveCompanyBasicInfo endpoint.
     */
    @Operation(
            summary = "Save company basic info and branding",
            description = "Updates the company's portal URL, email address, branding colors, "
                    + "logos, and background image. Navigation bar logo requires Enterprise license."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Company basic info updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityCompanyConfigView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - invalid parameters",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/basic-info")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseEntityCompanyConfigView saveBasicInfo(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Company basic info and branding settings",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CompanyBasicInfoForm.class))
            )
            final CompanyBasicInfoForm form) {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        form.checkValid();

        return new ResponseEntityCompanyConfigView(helper.saveBasicInfo(form, user));
    }

    /**
     * Saves the company authentication type.
     * Replaces the legacy POST /api/config/saveCompanyAuthTypeInfo endpoint.
     */
    @Operation(
            summary = "Save company authentication type",
            description = "Updates the authentication method used for user login. "
                    + "Accepts 'emailAddress' or 'userId'."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Authentication type updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityCompanyConfigView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request - invalid auth type",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/auth-type")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseEntityCompanyConfigView saveAuthType(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Authentication type setting",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CompanyAuthTypeForm.class))
            )
            final CompanyAuthTypeForm form) {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        form.checkValid();

        return new ResponseEntityCompanyConfigView(helper.saveAuthType(form, user));
    }

    /**
     * Saves company locale information (language and timezone).
     * Moved from ConfigurationResource._saveCompanyLocaleInfo.
     */
    @Operation(
            summary = "Save company locale info",
            description = "Updates the locale (language and timezone) for the current company. "
                    + "This sets the default language and timezone for the system."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Locale settings updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid locale parameters (e.g. invalid timezone)",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/locale-info")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseEntityStringView saveLocaleInfo(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Locale settings to apply",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CompanyLocaleForm.class))
            )
            final CompanyLocaleForm form) {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        form.checkValid();
        helper.saveLocaleInfo(form, user);

        return new ResponseEntityStringView("OK");
    }

    /**
     * Regenerates the company security key.
     * Replaces the legacy POST /api/config/regenerateKey endpoint.
     */
    @Operation(
            summary = "Regenerate company security key",
            description = "Regenerates the company's security key and returns the SHA-256 digest "
                    + "of the new key. This operation cannot be undone."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Security key regenerated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/_regenerateKey")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityStringView regenerateKey(
            @Parameter(hidden = true) @Context final HttpServletRequest request,
            @Parameter(hidden = true) @Context final HttpServletResponse response)
            throws DotDataException, DotSecurityException {

        final User user = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                .requiredPortlet("maintenance")
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init()
                .getUser();

        return new ResponseEntityStringView(helper.regenerateKey(user));
    }
}
