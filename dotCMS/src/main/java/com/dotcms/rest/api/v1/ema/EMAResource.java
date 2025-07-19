package com.dotcms.rest.api.v1.ema;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;


/**
 * Resource API that deals with secrets and their usage on third-party apps integrations.
 * @author jsanca
 */
@SwaggerCompliant(value = "Modern APIs and specialized services", batch = 7)
@Tag(name = "Apps")
@Path("/v1/ema")
public class EMAResource {

    private static final String EMA_APP_KEY = "dotema-config-v2";

    private final WebResource webResource;
    private AppsAPI appsAPI;

    @VisibleForTesting
    public EMAResource(final WebResource webResource,
            final AppsAPI appsAPI) {
        this.webResource = webResource;
        this.appsAPI = appsAPI;
    }

    public EMAResource() {
        this(new WebResource(), APILocator.getAppsAPI());
    }


    @Operation(
        summary = "Get EMA configuration details",
        description = "Returns the Enterprise Mobile Application (EMA) configuration for the current site. Retrieves app secrets and configuration from the dotema-config-v2 app."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "EMA configuration retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityEmaConfigurationView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "EMA configuration not found for current site",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response getDetails(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response
    ) throws DotDataException, DotSecurityException {

        final Host site  = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();

        Logger.debug(this, ()-> "Getting EMA config for site: " + site.getHostname());

        final Optional<AppDescriptor> appDescriptorOptional = appsAPI
                .getAppDescriptor(EMA_APP_KEY, APILocator.systemUser()); // we use the system b/c we don't want to check permissions, but only have access to this app and should be backend
        if (appDescriptorOptional.isPresent()) {

            final Optional<AppSecrets> optionalAppSecrets = appsAPI
                    .getSecrets(EMA_APP_KEY, false, site, APILocator.systemUser());

            if (optionalAppSecrets.isPresent()) {

                final AppSecrets appSecrets = optionalAppSecrets.get();
                final Secret configSecret = appSecrets.getSecrets().get("configuration");
                final String configJson   = configSecret.getString();

                return Response.ok(new ResponseEntityView<>(new JSONObject(configJson))).build();
            }
        }

        throw new DoesNotExistException(String.format(
                "No configuration was found for EMA on the current site `%s`. ", site.getHostname()));
    }
}
