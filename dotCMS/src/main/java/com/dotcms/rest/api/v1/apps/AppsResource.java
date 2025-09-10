package com.dotcms.rest.api.v1.apps;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
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
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * Resource API that deals with secrets and their usage on third-party apps integrations.
 */
@Path("/v1/apps")
@Tag(name = "Apps", description = "Third-party application integration and configuration")
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
    @GET
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response listAvailableApps(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @QueryParam("filter") final String filter
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
            return Response.ok(new ResponseEntityView<>(appViews)).build(); // 200
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
    @GET
    @Path("/{key}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getAppByKey(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String key,
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
    @GET
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getAppDetail(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String key,
            @PathParam("siteId") final String siteId
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
                return Response.ok(new ResponseEntityView<>(appSiteDetailedView.get()))
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
    @POST
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response createApp(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final FormDataMultiPart multipart
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
            return Response.ok(new ResponseEntityView<>(apps)).build(); // 200
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
    @POST
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response createAppSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String key,
            @PathParam("siteId") final String siteId,
            final SecretForm secretForm
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
            return Response.ok(new ResponseEntityView<>(OK)).build(); // 200
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
    @PUT
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateAppIndividualSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String key,
            @PathParam("siteId") final String siteId,
            final SecretForm secretForm
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
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
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
    @DELETE
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteIndividualAppSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final DeleteSecretForm secretForm
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
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
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
    @DELETE
    @Path("/{key}/{siteId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteAllAppSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String key,
            @PathParam("siteId") final String siteId
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
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
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
    @DELETE
    @Path("/{key}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteApp(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String serviceKey,
            @QueryParam("removeDescriptor") final boolean removeDescriptor
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
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
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
    @POST
    @Path("/export")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response exportSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final ExportSecretForm exportSecretForm
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
    @POST
    @Path("/import")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response importSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final FormDataMultiPart form
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
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            //By doing this mapping here. The resource becomes integration test friendly.
            Logger.error(this.getClass(),"Exception importing secrets.", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

}
