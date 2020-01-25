package com.dotcms.rest.api.v1.secret;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.secret.view.ServiceIntegrationView;
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


/**
 * Resource API that deals with secrets and their usage on third-party service integrations
 */
@Path("/v1/service-integrations")
public class ServiceIntegrationResource {

    private final WebResource webResource;
    private ServiceIntegrationHelper helper;

    @VisibleForTesting
    public ServiceIntegrationResource(final WebResource webResource,
            final ServiceIntegrationHelper helper) {
        this.webResource = webResource;
        this.helper = helper;
    }

    public ServiceIntegrationResource() {
        this(new WebResource(), new ServiceIntegrationHelper());
    }


    /**
     * List all the services available to integrate with.
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
    public final Response listAvailableServices(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        final List<ServiceIntegrationView> integrationViews = helper
                .getAvailableDescriptorViews(user);
        if (!integrationViews.isEmpty()) {
            return Response.ok(new ResponseEntityView(integrationViews)).build(); // 200
        }
        final String message = "No service integration is available to configure. Try uploading a file!";
        Logger.error(ServiceIntegrationResource.class, message);
        throw new DoesNotExistException(message);
    }

    /**
     * Once you have a list of all the available services you can take the key and feed this endpoint to get a detailed view.
     * The Detailed view will include all sites that have a configuration for the specified service.
     * @param request
     * @param response
     * @param serviceKey service unique identifier
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{key}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getServiceIntegrationByKey(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String serviceKey
    ) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        final Optional<ServiceIntegrationView> serviceIntegrationView = helper
                .getServiceIntegrationSiteView(serviceKey, user);
        if (serviceIntegrationView.isPresent()) {
            return Response.ok(new ResponseEntityView(serviceIntegrationView.get()))
                    .build(); // 200
        }
        final String message = String
                .format("No service integration was found for key `%s`. ", serviceKey);
        Logger.error(ServiceIntegrationResource.class, message);
        throw new DoesNotExistException(message);

    }

    /**
     * Once you have a detailed view with all the available sites.
     * You can take a site-id and feed into this endpoint.
     * To explore the specific configuration for that site.
     * @param request
     * @param response
     * @param serviceKey service unique identifier
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
    public final Response getDetailedServiceIntegration(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String serviceKey,
            @PathParam("siteId") final String siteId
    ) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        final Optional<ServiceIntegrationView> serviceIntegrationDetailedView = helper
                .getServiceIntegrationSiteDetailedView(serviceKey, siteId, user);
        if (serviceIntegrationDetailedView.isPresent()) {
            return Response.ok(new ResponseEntityView(serviceIntegrationDetailedView.get()))
                    .build(); // 200
        }

        final String message = String
                .format("Nope. No service integration was found for key `%s` and siteId `%s`. ",
                        serviceKey, siteId);
        Logger.error(ServiceIntegrationResource.class, message);
        throw new DoesNotExistException(message);
    }

    /**
     * This basically allows you to upload a new service definition which has to be specified through a yml file.
     * @see <a href=https://auth5.dotcms.com/devwiki/service-descriptor>Service descriptors</a>
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
    public final Response createServiceIntegration(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final FormDataMultiPart multipart
    ) throws DotSecurityException, IOException, DotDataException {
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
    }

    /**
     * This is the endpoint used to feed secrets into the system for a specific service/host configuration.
     * @param request
     * @param response
     * @param secretForm
     * @return response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @POST
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response createServiceIntegrationSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final SecretForm secretForm
    ) throws DotDataException, DotSecurityException {

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
    }

    /**
     * This is the endpoint used to feed/update secrets for a specific service/host configuration.
     * @param request
     * @param response
     * @param secretForm form
     * @return response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @Path("/")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateServiceIntegrationIndividualSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final SecretForm secretForm
    ) throws DotDataException, DotSecurityException {
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
    }

    /**
     * This is the endpoint used to delete individual secrets for a specific service/host configuration.
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
    public final Response deleteIndividualServiceIntegrationSecret(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final DeleteSecretForm secretForm
    ) throws DotDataException, DotSecurityException {

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
    }

    /**
     * This will remove a specific configuration for the key and site combination.
     * All the secrets at once will be lost.
     * But the Site Configuration and Service description remains intact.
     * @param request
     * @param response
     * @param serviceKey service unique identifier
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
    public final Response deleteAllServiceIntegrationSecrets(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String serviceKey,
            @PathParam("siteId") final String siteId
    ) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        helper.deleteServiceIntegrationSecrets(serviceKey, siteId, user);
        return Response.ok(new ResponseEntityView(OK)).build(); // 200
    }

    /**
     * This endpoint removes all integrations associated with an service.
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
    public final Response deleteServiceIntegration(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("key") final String serviceKey,
            @QueryParam("removeDescriptor") final boolean removeDescriptor
    ) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        helper.removeServiceIntegration(serviceKey, user, removeDescriptor);
        return Response.ok(new ResponseEntityView(OK)).build(); // 200

    }

}
