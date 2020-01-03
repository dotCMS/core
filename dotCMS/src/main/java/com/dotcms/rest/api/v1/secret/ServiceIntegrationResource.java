package com.dotcms.rest.api.v1.secret;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationDetailedView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationHostView;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationView;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

@Path("/v1/service/integration")
public class ServiceIntegrationResource {

    private final WebResource webResource;
    private ServiceIntegrationHelper helper;

    @VisibleForTesting
    public ServiceIntegrationResource(final WebResource webResource,final ServiceIntegrationHelper helper) {
        this.webResource = webResource;
        this.helper = helper;
    }

    public ServiceIntegrationResource() {
       this(new WebResource(),new ServiceIntegrationHelper());
    }

    @GET
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response listAvailableServices(@Context final HttpServletRequest request,
                                                @Context final HttpServletResponse response
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
            final List<ServiceIntegrationView> integrationViews = helper.getAvailableDescriptorViews(user);
            if(!integrationViews.isEmpty()){
                return Response.ok(new ResponseEntityView(integrationViews)).build(); // 200
            }
            throw new DoesNotExistException("Nope. No service integration is available to configure. Try uploading a file!");
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception on listing available service integration descriptors.", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @GET
    @Path("/{serviceKey}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getServiceIntegrationByKey(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("serviceKey") final String serviceKey
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
            final Optional<ServiceIntegrationHostView> serviceIntegrationView = helper
                    .getServiceIntegrationHostView(serviceKey, user);
            if (serviceIntegrationView.isPresent()) {
                return Response.ok(new ResponseEntityView(serviceIntegrationView.get()))
                        .build(); // 200
            }
            throw new DoesNotExistException(
                    String.format("Nope. No service integration was found for key `%s`. ",
                            serviceKey));
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception getting service integration with message: " + e.getMessage(),
                    e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @GET
    @Path("/{serviceKey}/{hostId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getDetailedServiceIntegration(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("serviceKey") final String serviceKey,
            @PathParam("hostId") final String hostId
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

                //if the hostId is set it will be used to bring the configuration detail for that specific host
                final Optional<ServiceIntegrationDetailedView> serviceIntegrationDetailedView = helper
                        .getServiceIntegrationHostDetailedView(serviceKey, hostId, user);
                if (serviceIntegrationDetailedView.isPresent()) {
                    return Response.ok(new ResponseEntityView(serviceIntegrationDetailedView.get()))
                            .build(); // 200
                }
                throw new DoesNotExistException(String.format(
                        "Nope. No service integration was found for key `%s` and hostId `%s`. ",
                        serviceKey, hostId));

        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception getting service integration and secrets with message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @DELETE
    @Path("/{serviceKey}/{hostId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteAllServiceIntegrationSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("serviceKey") final String serviceKey,
            @PathParam("hostId") final String hostId
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
            //this will remove a specific configuration for the key and host combination. All the secrets at once will be lost.
            helper.deleteServiceIntegrationSecrets(serviceKey, hostId, user);
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(), "Exception getting service integration and secrets with message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @POST
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response createServiceIntegration(
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
            helper.createServiceIntegration(multipart, user);
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception saving/creating a service integration with message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @POST
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response createServiceIntegrationSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
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
            helper.saveUpdateSecret(secretForm, user);
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception creating secret with message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @PUT
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateServiceIntegrationIndividualSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
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
            helper.saveUpdateSecret(secretForm, user);
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception saving/updating secret with message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @DELETE
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteIndividualServiceIntegrationSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
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
            helper.deleteSecret(secretForm, user);
            return Response.ok(new ResponseEntityView(OK)).build(); // 200
        } catch (Exception e) {
            Logger.error(this.getClass(),"Exception creating secret with message: " + e.getMessage(), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

}
