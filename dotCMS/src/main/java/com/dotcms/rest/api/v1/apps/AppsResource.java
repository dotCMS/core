package com.dotcms.rest.api.v1.apps;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.apps.view.AppView;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.SwaggerCompliant;


/**
 * Resource API that deals with secrets and their usage on third-party apps integrations.
 */
@SwaggerCompliant(value = "Rules engine and business logic APIs", batch = 6)
@Path("/v1/apps")
@Tag(name = "Apps")
public class AppsResource {

    private final WebResource webResource;
    private AppsHelper helper;

    @VisibleForTesting
    public AppsResource(final WebResource webResource,
            final AppsHelper helper) {
        this.webResource = webResource;
        this.helper = helper;
    }

    public AppsResource() {
        this(new WebResource(), new AppsHelper());
    }


    /**
     * List all the apps available to integrate with.
     * @param request
     * @param response
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "List available apps",
        description = "Retrieves a list of all third-party applications available for integration with the system. Supports optional filtering to search for specific apps."
    )
    @ApiResponse(responseCode = "200", description = "Apps retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityAppListView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to list apps")
    @ApiResponse(responseCode = "500", description = "Internal server error retrieving apps")
    @GET
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response listAvailableApps(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @Parameter(description = "Filter text to search apps by name or description") @QueryParam("filter") final String filter
    ) {
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            final List<AppView> appViews = helper.getAvailableDescriptorViews(user, filter);
            return Response.ok(new ResponseEntityAppListView(appViews)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(), "Exception on listing all available apps.", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Once you have a list of all the available apps you can take the key and feed this endpoint to get a detailed view.
     * The Detailed view will include all sites that have a configuration for the specified app.
     * Url example: http://localhost:8080/api/v1/apps/lol_1579927726215?filter=lol&per_page=100&orderby=name&direction=DESC
     * @param request
     * @param response
     * @param key app unique identifier
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Get app by key",
        description = "Retrieves detailed information about a specific app identified by its unique key. Includes configuration settings and site-specific settings with pagination support."
    )
    @ApiResponse(responseCode = "200", description = "App details retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityAppView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to access app")
    @ApiResponse(responseCode = "404", description = "App not found")
    @ApiResponse(responseCode = "500", description = "Internal server error retrieving app details")
    @GET
    @Path("/{key}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response getAppByKey(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the app to retrieve", required = true) @PathParam("key") final String key,
            @BeanParam final PaginationContext paginationContext
    ) {
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            return helper.getAppSiteView(request, key, paginationContext, user);
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),
                    String.format("Exception getting app for key: `%s`.",key) , e
            );
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Once you have a detailed view with all the available sites.
     * You can take a site-id and feed into this endpoint.
     * To explore the specific configuration for that site.
     * @param request
     * @param response
     * @param key app unique identifier
     * @param siteId site
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Get app detail for specific site",
        description = "Retrieves detailed configuration and settings for a specific app within a particular site. Returns site-specific app configurations and secret values."
    )
    @ApiResponse(responseCode = "200", description = "App detail retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityAppView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to access app details")
    @ApiResponse(responseCode = "404", description = "App or site not found")
    @ApiResponse(responseCode = "500", description = "Internal server error retrieving app details")
    @GET
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response getAppDetail(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the app", required = true) @PathParam("key") final String key,
            @Parameter(description = "Site identifier to get app details for", required = true) @PathParam("siteId") final String siteId
    ) {
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            final Optional<AppView> appSiteDetailedView = helper
                        .getAppSiteDetailedView(key, siteId, user);
            if (appSiteDetailedView.isPresent()) {
                return Response.ok(new ResponseEntityAppView(appSiteDetailedView.get()))
                .build(); // 200
            }
            throw new DoesNotExistException(String.format(
                        "No app was found for key `%s` and siteId `%s`. ",
                        key, siteId));

        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(), "Exception getting app and secrets with message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This basically allows you to upload a new app definition which has to be specified through a yml file.
     * @see <a href=https://auth5.dotcms.com/devwiki/apps>Apps</a>
     * @param request
     * @param response
     * @param multipart
     * @return Response
     * @throws DotSecurityException
     * @throws IOException
     * @throws DotDataException
     */
    @Operation(
        summary = "Create new app",
        description = "Creates a new third-party application integration by uploading the app configuration and metadata. Accepts multipart form data containing the app descriptor and resources."
    )
    @ApiResponse(responseCode = "200", description = "App created successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityAppListView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid app configuration or missing required fields")
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to create apps")
    @ApiResponse(responseCode = "409", description = "Conflict - app with same key already exists")
    @ApiResponse(responseCode = "500", description = "Internal server error creating app")
    @POST
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response createApp(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Multipart form data containing app configuration files and metadata", required = true) final FormDataMultiPart multipart
    ) {
        try {

            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();

            final User user = initData.getUser();
            final List<AppView> apps = helper
                    .createApp(multipart, user);
            return Response.ok(new ResponseEntityAppListView(apps)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),"Exception saving/creating app.", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This is the endpoint used to feed secrets into the system for a specific app/host configuration.
     * This endpoint behaves as a form.
     * @param request
     * @param response
     * @param secretForm
     * @return response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Create app secrets",
        description = "Creates or updates secret values for a specific app configuration within a site. Secrets are encrypted values used for secure integration with third-party services."
    )
    @ApiResponse(responseCode = "200", description = "App secrets created successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityStringView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid secret form data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to manage app secrets")
    @ApiResponse(responseCode = "404", description = "App or site not found")
    @ApiResponse(responseCode = "500", description = "Internal server error creating app secrets")
    @POST
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public final Response createAppSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the app", required = true) @PathParam("key") final String key,
            @Parameter(description = "Site identifier where secrets will be stored", required = true) @PathParam("siteId") final String siteId,
            @RequestBody(description = "Secret form containing key-value pairs for app configuration", required = true) final SecretForm secretForm
    ) {
        try {
            secretForm.checkValid();
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            helper.saveSecretForm(key, siteId, secretForm, user);
            return Response.ok(new ResponseEntityStringView(OK)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),
                    String.format("Exception creating secret integration with form `%s` ",secretForm), e
            );
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This is the endpoint used to feed/update secrets for a specific app/host configuration.
     * @param request
     * @param response
     * @param secretForm form
     * @return response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Update individual app secret",
        description = "Updates specific secret values for an app configuration within a site. Allows for partial updates of existing secret configurations without affecting other secrets."
    )
    @ApiResponse(responseCode = "200", description = "App secret updated successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityStringView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid secret form data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to update app secrets")
    @ApiResponse(responseCode = "404", description = "App, site, or secret not found")
    @ApiResponse(responseCode = "500", description = "Internal server error updating app secret")
    @PUT
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public final Response updateAppIndividualSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the app", required = true) @PathParam("key") final String key,
            @Parameter(description = "Site identifier where secret is stored", required = true) @PathParam("siteId") final String siteId,
            @RequestBody(description = "Secret form containing updated key-value pairs", required = true) final SecretForm secretForm
    ) {
        try {
            secretForm.checkValid();
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            helper.saveUpdateSecrets(key, siteId, secretForm, user);
            return Response.ok(new ResponseEntityStringView(OK)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),
               String.format("Exception saving/updating secret with form `%s` ",secretForm), e
            );
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This is the endpoint used to delete individual secrets for a specific app/host configuration.
     * @param request
     * @param response
     * @param secretForm form
     * @return response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Delete individual app secret",
        description = "Deletes specific secret values from an app configuration. Removes individual secret keys while preserving other app configurations and secrets."
    )
    @ApiResponse(responseCode = "200", description = "App secret deleted successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityStringView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid delete secret form data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to delete app secrets")
    @ApiResponse(responseCode = "404", description = "App or secret not found")
    @ApiResponse(responseCode = "500", description = "Internal server error deleting app secret")
    @DELETE
    @Path("/")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public final Response deleteIndividualAppSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Delete secret form specifying which secrets to remove", required = true) final DeleteSecretForm secretForm
    ) {
        try {
            secretForm.checkValid();
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            helper.deleteSecret(secretForm, user);
            return Response.ok(new ResponseEntityStringView(OK)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),
               String.format("Exception creating secret with form `%s`",secretForm), e
            );
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This will remove a specific configuration for the key and site combination.
     * All the secrets at once will be lost.
     * But the Site Configuration and App description remains intact.
     * @param request
     * @param response
     * @param key App unique identifier
     * @param siteId site
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Delete all app secrets",
        description = "Removes all secret configurations for a specific app and site combination. This clears all stored secrets while preserving the app definition and site configuration structure."
    )
    @ApiResponse(responseCode = "200", description = "All app secrets deleted successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityStringView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to delete app secrets")
    @ApiResponse(responseCode = "404", description = "App or site not found")
    @ApiResponse(responseCode = "500", description = "Internal server error deleting app secrets")
    @DELETE
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response deleteAllAppSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the app", required = true) @PathParam("key") final String key,
            @Parameter(description = "Site identifier where secrets are stored", required = true) @PathParam("siteId") final String siteId
    ) {
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            //this will remove a specific configuration for the key and site combination. All the secrets at once will be lost.
            helper.deleteAppSecrets(key, siteId, user);
            return Response.ok(new ResponseEntityStringView(OK)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),
              String.format("Exception getting service integration and secrets for `%s`, and `%s` ",key, siteId) , e
            );
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * This endpoint removes all integrations associated with an app.
     * @param request
     * @param response
     * @param serviceKey service unique identifier
     * @param removeDescriptor if passed the descriptor will be removed as well.
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Delete app completely",
        description = "Removes an entire app integration including all configurations, secrets, and optionally the app descriptor. This is a destructive operation that removes all app-related data across all sites."
    )
    @ApiResponse(responseCode = "200", description = "App deleted successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityStringView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to delete apps")
    @ApiResponse(responseCode = "404", description = "App not found")
    @ApiResponse(responseCode = "500", description = "Internal server error deleting app")
    @DELETE
    @Path("/{key}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public final Response deleteApp(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the app to delete", required = true) @PathParam("key") final String serviceKey,
            @Parameter(description = "Whether to also remove the app descriptor definition") @QueryParam("removeDescriptor") final boolean removeDescriptor
    ) {
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            helper.removeApp(serviceKey, user, removeDescriptor);
            return Response.ok(new ResponseEntityStringView(OK)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),String.format("Exception creating secret for key %s",serviceKey), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Secrets export
     * @param request
     * @param response
     * @param exportSecretForm
     * @return
     */
    @Operation(
        summary = "Export app secrets",
        description = "Exports app secret configurations to a downloadable file. Allows backup and migration of app configurations between environments. Returns a binary export file."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Secrets exported successfully as file download (binary stream - no JSON schema)",
                    content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid export form data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to export secrets",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error exporting secrets",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/export")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response exportSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Export configuration specifying which secrets to export", required = true) final ExportSecretForm exportSecretForm
    ) {
        exportSecretForm.checkValid();
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            //no need to close i'll get closed upon writing the response
            return Response.ok(helper.exportSecrets(exportSecretForm, user), MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition", "attachment; filename=appSecrets.export")
                    .build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),"Exception exporting secrets.", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * Secrets import
     * @param request
     * @param response
     * @param form
     * @return
     */
    @Operation(
        summary = "Import app secrets",
        description = "Imports app secret configurations from an uploaded export file. Allows restoration and migration of app configurations from other environments. Accepts multipart form data with the export file."
    )
    @ApiResponse(responseCode = "200", description = "Secrets imported successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityStringView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid import file or missing form data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - backend user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to import secrets")
    @ApiResponse(responseCode = "409", description = "Conflict - import would overwrite existing configurations")
    @ApiResponse(responseCode = "500", description = "Internal server error importing secrets")
    @POST
    @Path("/import")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response importSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Multipart form data containing the secrets export file to import", required = true) final FormDataMultiPart form
    ) {
        try {
            final InitDataObject initData =
                    new WebResource.InitBuilder(webResource)
                            .requiredBackendUser(true)
                            .requiredFrontendUser(false)
                            .requestAndResponse(request, response)
                            .rejectWhenNoUser(true)
                            .init();
            final User user = initData.getUser();
            helper.importSecrets(form, user);
            return Response.ok(new ResponseEntityStringView(OK)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),"Exception importing secrets.", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

}
