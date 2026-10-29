package com.dotcms.rest;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.integritycheckers.IntegrityType;
import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.IntegrityDataGenerationJob;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.ZipUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.quartz.SchedulerException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This REST end-point provides all the required mechanisms for the execution of
 * integrity checks related to some objects in dotCMS. When pushing content from
 * one server to another, there might be situations in which an object (e.g., a
 * Content Page) already exists in the destination server(s) with a different
 * Identifier. This will generate an error and the bundle will fail to publish.
 * <p>
 * Therefore, it is always recommended running the Integrity Checker feature
 * <b>BEFORE</b> pushing a bundle. This will indicate the user that there are
 * conflicting elements in the destination server(s) and will allow them to
 * solve them, either by replacing the local data with the external data, or
 * vice versa. The following objects will be analyzed when running the Integrity
 * Checker:
 * <ul>
 * <li>Content Types.</li>
 * <li>Folders.</li>
 * <li>Legacy Pages and Content Pages.</li>
 * <li>File Assets.</li>
 * </ul>
 *
 * @author Daniel Silva
 * @version 1.0
 * @since Jun 23, 2014
 *
 */
@Path("/integrity")
@Tag(name = "Data Integrity", description = "Data integrity checking and conflict resolution")
public class IntegrityResource {

    private final WebResource webResource = new WebResource();

    private final static Cache<String, EndpointState> endpointStateCache =
            CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();

    public enum ProcessStatus {
        PROCESSING, ERROR, FINISHED, NO_CONFLICTS
    }

    private static class EndpointState {
        private final Map<String, Cookie> cookies = new ConcurrentHashMap<>();

        public Map<String, Cookie> getCookies() {
            return cookies;
        }

        public void addCookie(String name, Cookie cookie) {
            cookies.put(name, cookie);
        }
    }

    private static Response cacheEndpointState(String endpointId, Response response) {
        EndpointState endpointState = endpointStateCache.getIfPresent(endpointId);
        if (endpointState == null) {
            endpointStateCache.put(endpointId, endpointState = new EndpointState());
        }

        response.getCookies().forEach(endpointState::addCookie);
        return response;
    }

    private static Optional<Builder> builderFromEndpoint(final PublishingEndPoint endpoint,
                                                         final String url,
                                                         final MediaType mediaType) {
        final Optional<String> requestToken = AuthCredentialPushPublishUtil.INSTANCE.getRequestToken(endpoint);
        if (requestToken.isEmpty()) {
            Logger.warn(IntegrityResource.class, "No Auth Token set for endpoint: " + endpoint.getId());
            return Optional.empty();
        }

        final Builder requestBuilder = RestClientBuilder.newClient().target(url).request(mediaType);
        final EndpointState endpointState = endpointStateCache.getIfPresent(endpoint.getId());
        if (endpointState != null) {
            endpointState.getCookies().values().forEach(requestBuilder::cookie);
        }
        requestBuilder.header("Authorization", requestToken.get());

        return Optional.of(requestBuilder);
    }

    private Response postWithEndpointState(final PublishingEndPoint endpoint,
                                           final String url,
                                           final Entity<FormDataMultiPart> entity) {
        return builderFromEndpoint(endpoint, url, MediaType.TEXT_PLAIN_TYPE)
                .map(builder -> cacheEndpointState(endpoint.getId(), builder.post(entity)))
                .orElse(response("No Auth Token set for endpoint", true));
    }

    private Response getWithEndpointState(final PublishingEndPoint endpoint,
                                          final String url) {
        return builderFromEndpoint(endpoint, url, new MediaType("application", "zip"))
                .map(builder -> cacheEndpointState(endpoint.getId(), builder.get()))
                .orElse(response("No Auth Token set for endpoint", true));
    }

    /**
     * Authenticates a call made from the integrity checker of the Publishing Environments screen.
     * Only back-end users with access to the Configuration portlet, the portlet that holds that
     * screen, may run it; CMS administrators have access to every portlet.
     *
     * @param params   the URL parameters, in the {@code /key/value} form
     * @param request  the current request
     * @param response the current response
     * @return the authenticated user and the parsed URL parameters
     * @throws com.dotcms.rest.exception.SecurityException if there is no user, or the user is
     *         not a back-end user with access to the Configuration portlet
     */
    private InitDataObject initForIntegrityChecker(final String params,
                                                   final HttpServletRequest request,
                                                   final HttpServletResponse response) {
        return new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .params(params)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .rejectWhenNoUser(true)
                .requiredPortlet(PortletID.CONFIGURATION.toString())
                .init();
    }

    /**
     * Looks up the endpoint a request refers to. The integrity calls use the endpoint id for
     * session keys, results tables and folder names, so it must belong to an existing endpoint
     * before it is used.
     *
     * @param endpointId the endpoint id sent by the client
     * @return the endpoint, or empty when no endpoint has that id
     * @throws DotDataException if the endpoint cannot be loaded
     */
    private static Optional<PublishingEndPoint> findEndpoint(final String endpointId)
            throws DotDataException {
        return Optional.ofNullable(
                APILocator.getPublisherEndPointAPI().findEndPointById(endpointId));
    }

    /**
     * Builds the answer for an endpoint id that does not belong to any endpoint.
     *
     * @return a 404 response
     */
    private static Response endpointNotFound() {
        return Response.status(HttpStatus.SC_NOT_FOUND)
                .entity("Error: endpoint not found.").build();
    }

    /**
     * Parses the {@code type} parameter of the discard and fix calls.
     *
     * @param type the value sent by the client
     * @return the matching {@link IntegrityType}, or empty when the value names none
     */
    private static Optional<IntegrityType> parseIntegrityType(final String type) {
        return Arrays.stream(IntegrityType.values())
                .filter(integrityType -> integrityType.name().equalsIgnoreCase(type))
                .findFirst();
    }

    /**
     * Builds the answer for a {@code type} parameter that names no integrity type.
     *
     * @return a 400 response
     */
    private static Response invalidIntegrityType() {
        return Response.status(HttpStatus.SC_BAD_REQUEST)
                .entity("Error: 'type' has an invalid value.").build();
    }

    /**
     * Evaluates if the {@link IntegrityDataGenerationJob} is running.
     *
     * @return true if it does, otherwise false
     */
    private boolean isJobRunning() {
        try {
            return QuartzUtils.isJobRunning(
                    IntegrityDataGenerationJob.getJobScheduler(),
                    IntegrityDataGenerationJob.JOB_NAME,
                    IntegrityDataGenerationJob.JOB_GROUP,
                    IntegrityDataGenerationJob.TRIGGER_NAME,
                    IntegrityDataGenerationJob.TRIGGER_GROUP);
        } catch (SchedulerException e) {
            return false;
        }
    }

    /**
     * <p>Returns a zip with data from structures and folders for integrity check
     */
    @POST
    @Path("/_generateintegritydata")
    @Produces("text/plain")
    public Response generateIntegrityData(@Context HttpServletRequest request)  {

        if (LicenseManager.getInstance().isCommunity()) {
            throw new InvalidLicenseException("License required");
        }

        final String localAddress = RestEndPointIPUtil.getFullLocalIp(request);
        final String remoteIp = RestEndPointIPUtil.resolveRemoteIp(request);

        final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken
                = AuthCredentialPushPublishUtil.INSTANCE.processAuthHeader(request);

        final Optional<Response> failResponse = PushPublishResourceUtil.getFailResponse(
                request,
                pushPublishAuthenticationToken);

        if (failResponse.isPresent()) {
            return failResponse.get();
        }

        try {
            if (isJobRunning()) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> job is already running for remote ip: %s, so aborting generation",
                                localAddress, remoteIp));
                throw new WebApplicationException(
                        Response.status(HttpStatus.SC_CONFLICT)
                                .entity("Already Running")
                                .build());
            }

            final String transactionId = UUIDGenerator.generateUuid();
            IntegrityDataGenerationJob.triggerIntegrityDataGeneration(pushPublishAuthenticationToken.getKey(), transactionId);
            Logger.info(
                    IntegrityResource.class,
                    String.format(
                            "Receiver at %s:> job triggered for endpoint id: %s and requester id: %s",
                            localAddress, remoteIp, transactionId));

            return Response.ok(transactionId).build();
        } catch (Exception e) {
            Logger.error(
                    IntegrityResource.class,
                    String.format("Receiver at %s:> Error caused by remote call of: %s", localAddress, remoteIp));

            Logger.error(
                    IntegrityResource.class,
                    String.format("Receiver at %s:> %s", localAddress, e.getMessage()),
                    e);

            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            } else {
                return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    /**
     * Checks if the generation of Integrity Data is done.
     * If FINISHED, returns a zip with the data
     * if PROCESSING, returns HttpStatus.SC_ACCEPTED
     * if ERROR, returns HttpStatus.SC_INTERNAL_SERVER_ERROR, including the error message
     *
     * Usage: /integrityData
     *
     */
    @GET
    @Path("/{requestId}/integrityData")
    @Produces("application/zip")
    public Response getIntegrityData(@Context HttpServletRequest request,
                                     @PathParam("requestId") final String requestId)  {
        final String remoteIp = RestEndPointIPUtil.resolveRemoteIp(request);
        final String localAddress = RestEndPointIPUtil.getFullLocalIp(request);
        final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken
                = AuthCredentialPushPublishUtil.INSTANCE.processAuthHeader(request);

        try {
            final Optional<Response> failResponse = PushPublishResourceUtil.getFailResponse(request, pushPublishAuthenticationToken);

            if (failResponse.isPresent()) {
                return failResponse.get();
            }

            if (isJobRunning()) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> job is already running for endpoint id: %s, therefore it's not ready and need to wait",
                                localAddress,
                                pushPublishAuthenticationToken.getKey()));
                return Response.status(HttpStatus.SC_ACCEPTED).build();
            }

            final Optional<IntegrityUtil.IntegrityDataExecutionMetadata> integrityMetadata =
                    IntegrityUtil.getIntegrityMetadata(pushPublishAuthenticationToken.getKey());
            if (integrityMetadata.isEmpty()) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation metadata for endpoint id %s is not found ",
                                localAddress,
                                pushPublishAuthenticationToken.getKey()));
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            if (!requestId.equals(integrityMetadata.get().getRequestId())) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation metadata for endpoint id %s has a request id %s which does not match the provided %s",
                                localAddress,
                                pushPublishAuthenticationToken.getKey(),
                                integrityMetadata.get().getRequestId(),
                                requestId));
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            if (integrityMetadata.get().getProcessStatus() == ProcessStatus.PROCESSING) {
                Logger.info(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation for endpoint id %s still ongoing therefore it's not ready and need to wait",
                                localAddress,
                                pushPublishAuthenticationToken.getKey()));
                return Response.status(HttpStatus.SC_ACCEPTED).build();
            } else if (integrityMetadata.get().getProcessStatus() == ProcessStatus.FINISHED &&
                    IntegrityUtil.doesIntegrityDataFileExist(
                            pushPublishAuthenticationToken.getKey(),
                            IntegrityUtil.INTEGRITY_DATA_TO_CHECK_ZIP_FILENAME)) {
                final String zipFilePath = IntegrityUtil.getIntegrityDataFilePath(
                        pushPublishAuthenticationToken.getKey(),
                        IntegrityUtil.INTEGRITY_DATA_TO_CHECK_ZIP_FILENAME);
                final StreamingOutput output = so -> {
                    final InputStream inputStream = Files.newInputStream(Paths.get(zipFilePath));
                    final byte[] buffer = new byte[1024];
                    int bytesRead;
                    //read from is to buffer
                    while((bytesRead = inputStream.read(buffer)) != -1){
                        so.write(buffer, 0, bytesRead);
                    }

                    inputStream.close();
                    //flush OutputStream to write any buffered data to file
                    so.flush();
                    so.close();
                };

                Logger.info(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation for endpoint id %s has finished and saved at %s",
                                localAddress,
                                pushPublishAuthenticationToken.getKey(),
                                zipFilePath));
                return Response.ok(output).build();
            } else if (integrityMetadata.get().getProcessStatus() == ProcessStatus.ERROR) {
                final String message = StringUtils.defaultString(
                        String.format(" due to '%s'", integrityMetadata.get().getErrorMessage()),
                        "");
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation for endpoint id %s has failed%s",
                                localAddress,
                                pushPublishAuthenticationToken.getKey(),
                                message));
                return Response
                        .status(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                        .entity(integrityMetadata.get().getErrorMessage())
                        .build();
            }
        } catch (Exception e) {
            Logger.error(IntegrityResource.class, "Error caused by remote call of: " + remoteIp, e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

       return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
    }

    /**
     * This is the entry point of the Integrity Checker process. The local data
     * where this process was kicked off will be compared to the data in the
     * selected end-point (server), grouped by object type: HTML Page, Content
     * Type, etc. This phase is made up of 3 main steps:
     * <ol>
     * <li>The verification data is generated in the selected end-point and
     * saved in the file system as .ZIP files, separated by object type.</li>
     * <li>The .ZIP file is sent from the end-point over to the local server,
     * un-zipped, and stored in temporary tables. SQL queries with local data
     * and those temporary tables will determine if there are any data
     * conflicts between the two servers.</li>
     * <li>Users will get a list of conflicts per object type. Finally, they can
     * decide to solve the data conflicts by replacing the data in the local
     * server or in the remote end-point.</li>
     * </ol>
     * <p>Only back-end users with access to the Configuration portlet may call it, and the
     * end-point ID must belong to an existing end-point.</p>
     *
     * @param httpServletRequest
     *            - The {@link HttpServletRequest} that started the process.
     * @param httpServletResponse
     *            - The current {@link HttpServletResponse}.
     * @param params
     *            - The execution parameters for running the process: The
     *            end-point ID.
     * @return The REST {@link Response} with the status of the operation: 400 without an
     *         end-point ID, 404 when no end-point has that ID.
     * @throws com.dotcms.rest.exception.SecurityException with 401 when the user may not run
     *         the integrity checker
     */
    @GET
    @Path("/checkintegrity/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response checkIntegrity(@Context final HttpServletRequest httpServletRequest,
                                   @Context final HttpServletResponse httpServletResponse,
                                   @PathParam("params") final String params)  {
        final InitDataObject initData = initForIntegrityChecker(params, httpServletRequest, httpServletResponse);
        final Map<String, String> paramsMap = initData.getParamsMap();
        final HttpSession session = httpServletRequest.getSession();
        final User loggedUser = initData.getUser();
        final JSONObject jsonResponse = new JSONObject();

        //Validate the parameters
        final String endpointId = paramsMap.get("endpoint");
        if (!UtilMethods.isSet(endpointId)) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Error: endpoint is a required Field.").build();
        }

        final PublishingEndPoint endpoint;
        try {
            final Optional<PublishingEndPoint> foundEndpoint = findEndpoint(endpointId);
            if (foundEndpoint.isEmpty()) {
                return endpointNotFound();
            }
            endpoint = foundEndpoint.get();
        } catch (final DotDataException e) {
            Logger.error(IntegrityResource.class, "Error loading the endpoint for the integrity check", e);
            return response("Error loading the endpoint for the integrity check", true);
        }

        Logger.info(
                IntegrityResource.class,
                String.format("Endpoint id: %s", endpointId));

        // return if we already have the data
        final IntegrityUtil integrityUtil = new IntegrityUtil();
        try {
            if (integrityUtil.doesIntegrityConflictsDataExist(endpointId)) {
                jsonResponse.put("success", true );
                jsonResponse.put("message", "Integrity Checking Initialized...");

                //Setting the process status
                setStatus(httpServletRequest, endpointId, ProcessStatus.FINISHED);

                return response(jsonResponse.toString(), false);
            }
        } catch(JSONException e) {
            Logger.error(
                    IntegrityResource.class,
                    "Error setting return message in JSON response",
                    e);
            return response("Error setting return message in JSON response", true);
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch(Exception e) {
            Logger.error(IntegrityResource.class, "Error checking existence of integrity data", e);
            return response( "Error checking existence of integrity data" , true );
        }

        //Setting the process status
        setStatus(httpServletRequest, endpointId, ProcessStatus.PROCESSING);

        try {
            //Sending bundle to endpoint
            Response response = generateIntegrityCheckerRequest(endpoint);

            if (response.getStatus() == HttpStatus.SC_OK) {
                final String integrityDataRequestId = response.readEntity(String.class);
                Thread integrityDataRequestChecker = new Thread(
                        new IntegrityDataRequestChecker(loggedUser,  session, endpoint, integrityDataRequestId)
                );
                //Start the integrity check
                integrityDataRequestChecker.start();
                addThreadToSession(session, integrityDataRequestChecker, endpointId, integrityDataRequestId);
            } else if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                return handleInvalidTokenResponse(endpoint, response, session);

            } else {
                setStatus(session, endpointId, ProcessStatus.ERROR, null);
                final String message = "Response indicating a " + response.getStatusInfo().getReasonPhrase() + " ("
                        + response.getStatus()
                        + ") Error trying to connect with the Integrity API on the Endpoint. Endpoint Id: "
                        + endpointId;
                Logger.error(this.getClass(), message);
                return response(message, true);
            }

            jsonResponse.put("success", true);
            jsonResponse.put("message", "Integrity Checking Initialized...");
        } catch(Exception e) {
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }

            //Setting the process status
            setStatus(session, endpointId, ProcessStatus.ERROR, null);
            final String message = "Error initializing the integrity checking process for End Point server: ["
                    + endpointId + "]";
            Logger.error(this.getClass(), message, e);
            return response(message, true);
        }

        return response( jsonResponse.toString(), false );
    }

    private Response handleInvalidTokenResponse(
            final PublishingEndPoint endpoint,
            final Response response,
            final HttpSession session) throws LanguageException {

        final Map<String, String> wwwAuthenticateHeader = ResourceResponse.getWWWAuthenticateHeader(response);
        final String errorKey = wwwAuthenticateHeader.get("error_key").replaceAll("\"", "");

        final String message =
                String.format(
                        "%s Response indicating Not Authorized received from Endpoint. Please check Auth Token. Endpoint Id: %s",
                        LanguageUtil.get(String.format("push_publish.end_point.%s_message", errorKey)),
                        endpoint.getId()
                );

        PushPublishLogger.log(this.getClass(), message);

        setStatus( session, endpoint.getId(), ProcessStatus.ERROR, null );
        Logger.error( this.getClass(), message);

        return response(
                message,
                HttpStatus.SC_UNAUTHORIZED);
    }

    private Response generateIntegrityCheckerRequest(final PublishingEndPoint endpoint) {
        String url = endpoint.toURL() + "/api/integrity/_generateintegritydata";

        return postWithEndpointState(endpoint, url, null);
    }

    /**
     * Method that will verify the status of a check integrity process for a given server.
     * Only back-end users with access to the Configuration portlet may call it.
     *
     * @param request  the current request
     * @param response the current response
     * @param params   the URL parameters, holding the end-point ID
     * @return the process status as JSON; 400 without an end-point ID, 404 when no end-point has
     *         that ID
     * @throws JSONException if the JSON response cannot be built
     * @throws com.dotcms.rest.exception.SecurityException with 401 when the user may not run
     *         the integrity checker
     */
    @GET
    @Path ("/checkIntegrityProcessStatus/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response checkIntegrityProcessStatus ( @Context final HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = initForIntegrityChecker(params, request, response);
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        String endpointId = paramsMap.get( "endpoint" );
        if ( !UtilMethods.isSet( endpointId ) ) {
            Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_BAD_REQUEST );
            responseBuilder.entity( responseMessage.append( "Error: " ).append( "endpoint" ).append( " is a required Field." ) );

            return responseBuilder.build();
        }

        try {
            if (findEndpoint(endpointId).isEmpty()) {
                return endpointNotFound();
            }

            JSONObject jsonResponse = new JSONObject();

            HttpSession session = request.getSession();
            //Verify if we have something set on the session
            if ( session.getAttribute( "integrityCheck_" + endpointId ) == null ) {
                //And prepare the response
                jsonResponse.put( "success", true );
                jsonResponse.put( "message", "No checking process found for End point server [" + endpointId + "]" );
                jsonResponse.put( "status", "nopresent" );
            } else {

                //Search for the status on session
                ProcessStatus status = (ProcessStatus) session.getAttribute( "integrityCheck_" + endpointId );

                //And prepare the response
                jsonResponse.put( "success", true );
                jsonResponse.put( "endPoint", endpointId );
                if ( status == ProcessStatus.PROCESSING ) {
                    jsonResponse.put( "status", "processing" );
                    jsonResponse.put( "message", "Success" );
                } else if ( status == ProcessStatus.FINISHED ) {
                    jsonResponse.put( "status", "finished" );
                    jsonResponse.put( "message", "Success" );
                } else if ( status == ProcessStatus.NO_CONFLICTS ) {
                    jsonResponse.put( "status", "noConflicts" );
                    jsonResponse.put( "message", session.getAttribute( "integrityCheck_message_" + endpointId ) );
                    clearStatus( request, endpointId );
                } else {
                    jsonResponse.put( "status", "error" );
                    jsonResponse.put( "message", "Error checking the integrity process status for End Point server: [" + endpointId + "]" );
                    clearStatus( request, endpointId );
                }
            }

            responseMessage.append(jsonResponse);

        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error checking the integrity process status for End Point server: [" + endpointId + "]", e );
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            return response( "Error checking the integrity process status for End Point server: [" + endpointId + "]" , true );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Generates and returns the integrity check results for a given server.
     * Only back-end users with access to the Configuration portlet may call it.
     *
     * @param request  the current request
     * @param response the current response
     * @param params   the URL parameters, holding the end-point ID
     * @return the conflicts per integrity type as JSON; 400 without an end-point ID, 404 when no
     *         end-point has that ID
     * @throws JSONException if the JSON response cannot be built
     * @throws com.dotcms.rest.exception.SecurityException with 401 when the user may not run
     *         the integrity checker
     */
    @GET
    @Path ("/getIntegrityResult/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response getIntegrityResult ( @Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = initForIntegrityChecker(params, request, response);
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        String endpointId = paramsMap.get( "endpoint" );
        if ( !UtilMethods.isSet( endpointId ) ) {
            Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_BAD_REQUEST );
            responseBuilder.entity( responseMessage.append( "Error: " ).append( "endpoint" ).append( " is a required Field." ) );

            return responseBuilder.build();
        }

        try {
            if (findEndpoint(endpointId).isEmpty()) {
                return endpointNotFound();
            }

            JSONObject jsonResponse = new JSONObject();
            IntegrityUtil integrityUtil = new IntegrityUtil();

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Structures tab data
            JSONArray tabResponse;
            JSONObject errorContent;

            IntegrityType[] types = IntegrityType.values();
            boolean isThereAnyConflict = false;

            for (IntegrityType integrityType : types) {
                tabResponse = new JSONArray();
                errorContent = new JSONObject();

                errorContent.put( "title",   LanguageUtil.get( initData.getUser().getLocale(), integrityType.getLabel() )  );//Title of the check

                List<Map<String, Object>> results = integrityUtil.getIntegrityConflicts(endpointId, integrityType);

                JSONArray columns = new JSONArray();

                // Add first display column label
                columns.add(integrityType.getFirstDisplayColumnLabel());

                switch( integrityType ) {
                    case HTMLPAGES:
                    case FILEASSETS:
                    case HOSTS:
                        columns.add("local_working_inode");
                        columns.add("remote_working_inode");
                        columns.add("local_live_inode");
                        columns.add("remote_live_inode");
                        columns.add("language_id");
                        break;
                    case CMS_ROLES:
                        columns.add("role_key");
                        columns.add("local_role_id");
                        columns.add("remote_role_id");
                        columns.add("local_role_fqn");
                        columns.add("remote_role_fqn");
                        break;
                    default:
                        columns.add("local_inode");
                        columns.add("remote_inode");
                        break;
                }

                errorContent.put( "columns", columns.toArray() );

                if(!results.isEmpty()) {
                    // the columns names are the keys in the results
                    isThereAnyConflict = true;

                    JSONArray values = new JSONArray();
                    for (Map<String, Object> result : results) {

                        JSONObject columnsContent = new JSONObject();

                        for (String keyName : result.keySet()) {
                            columnsContent.put(keyName, result.get(keyName));
                        }

                        values.put(columnsContent);
                    }

                    errorContent.put( "values", values.toArray() );
                } else {
                    errorContent.put( "values", new JSONArray().toArray() );
                }

                tabResponse.add( errorContent );
                //And prepare the response
                jsonResponse.put( integrityType.name().toLowerCase(), tabResponse.toArray() );
            }

            if(!isThereAnyConflict) {
                clearStatus( request, endpointId );
            }

            /*
            ++++++++++++++++++++++++
            Important just in case of return custom errors
             */
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", "Success" );

            responseMessage.append( jsonResponse.toString() );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error generating the integrity result for End Point server: [" + endpointId + "]", e );
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            return response( "Error generating the integrity result for End Point server: [" + endpointId + "]" , true );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Method that will discard the conflicts between local node and given endpoint.
     * Only back-end users with access to the Configuration portlet may call it.
     *
     * @param request  the current request
     * @param response the current response
     * @param params   the URL parameters, holding the end-point ID and the integrity type
     * @return an empty JSON object; 400 for a missing parameter or an unknown type, 404 when no
     *         end-point has that ID
     * @throws JSONException if the JSON response cannot be built
     * @throws com.dotcms.rest.exception.SecurityException with 401 when the user may not run
     *         the integrity checker
     */
    @GET
    @Path ("/discardconflicts/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response discardConflicts ( @Context final HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = initForIntegrityChecker(params, request, response);
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        String endpointId = paramsMap.get( "endpoint" );
        String type = paramsMap.get( "type" );

        if ( !UtilMethods.isSet( endpointId ) ) {
            return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( responseMessage.append( "Error: " ).append( "'endpoint'" ).append( " is a required param." )).build();
        }

        if ( !UtilMethods.isSet( type ) ) {
            return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( responseMessage.append( "Error: " ).append( "'type'" ).append( " is a required param." )).build();
        }

        final Optional<IntegrityType> integrityType = parseIntegrityType(type);
        if (integrityType.isEmpty()) {
            return invalidIntegrityType();
        }

        try {
            if (findEndpoint(endpointId).isEmpty()) {
                return endpointNotFound();
            }

            JSONObject jsonResponse = new JSONObject();

            IntegrityUtil integrityUtil = new IntegrityUtil();
            integrityUtil.discardConflicts(endpointId, integrityType.get());

            clearStatus( request, endpointId );

            responseMessage.append( jsonResponse.toString() );

        } catch ( Exception e ) {
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            Logger.error(this.getClass(), "ERROR: Table "
                    + integrityType.get().getResultsTableName()
                    + " could not be cleared on end-point [" + endpointId
                    + "]. Please truncate the table data manually.", e);
            return response( "Error discarding "+type+" conflicts for End Point server: [" + endpointId + "]" , true );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Method that will fix the conflicts received from remote
     *
     * @param request
     * @param dataToFix
     * @param type
     * @return
     * @throws JSONException
     */
    @POST
    @Path("/_fixconflictsfromremote")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response fixConflictsFromRemote ( @Context final HttpServletRequest request,
                                             @FormDataParam("DATA_TO_FIX") InputStream dataToFix,
                                             @FormDataParam("TYPE") String type ) throws JSONException {

        if (LicenseManager.getInstance().isCommunity()) {
            throw new InvalidLicenseException("License required");
        }

        final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken
                = AuthCredentialPushPublishUtil.INSTANCE.processAuthHeader(request);

        final Optional<Response> failResponse = PushPublishResourceUtil.getFailResponse(request, pushPublishAuthenticationToken);

        if (failResponse.isPresent()) {
            return failResponse.get();
        }

        JSONObject jsonResponse = new JSONObject();
        IntegrityUtil integrityUtil = new IntegrityUtil();
        String key = null;

        try {
             key = pushPublishAuthenticationToken.isJWTTokenWay() ?
                    pushPublishAuthenticationToken.getToken().getId() :
                    pushPublishAuthenticationToken.getPublishingEndPoint().getId();
            integrityUtil.fixConflicts(dataToFix, key,
                    IntegrityType.valueOf(type.toUpperCase()));
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error fixing "+type+" conflicts from remote", e );
            return response( "Error fixing "+type+" conflicts from remote" , true );
        } finally {
            try {
                if (key != null) {
                    // Discard conflicts if successful or failed
                    integrityUtil.discardConflicts(key, IntegrityType.valueOf(type.toUpperCase()));
                }
            } catch (DotDataException e) {
                Logger.error(this.getClass(), "ERROR: Table "
                        + IntegrityType.valueOf(type.toUpperCase()).getResultsTableName()
                        + " could not be cleared on request id [" + key
                        + "]. Please truncate the table data manually.", e);
            }
        }

        jsonResponse.put( "success", true );
        jsonResponse.put( "message", "Conflicts fixed in Remote Endpoint" );
        return response( jsonResponse.toString(), false );
    }

    /**
     * Fixes the data conflicts between the local and remote servers. If the
     * request parameter called <code>whereToFix</code> equals
     * <code>"local"</code>, the data correction will take place in local
     * server. If the parameter equals <code>"remote"</code>, the fix will take
     * place in remote server.
     * <p>Only back-end users with access to the Configuration portlet may call it. The
     * parameters are checked before anything is fixed, written or flushed.</p>
     *
     * @param httpServletRequest
     *            - The {@link HttpServletRequest} that started the process.
     * @param httpServletResponse
     *            - The current {@link HttpServletResponse}.
     * @param params
     *            - The execution parameters for running the process: the
     *            end-point ID, the integrity type and where to fix.
     * @return The REST {@link Response} with the status of the operation: 400 for a missing or
     *         invalid parameter, 404 when no end-point has that ID.
     * @throws JSONException
     *             An error occurred when generating the JSON response.
     * @throws com.dotcms.rest.exception.SecurityException with 401 when the user may not run
     *         the integrity checker
     */
    @GET
    @Path ("/fixconflicts/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response fixConflicts ( @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam ("params") final String params ) throws JSONException {

        final InitDataObject initData = initForIntegrityChecker(params, httpServletRequest, httpServletResponse);
        final Map<String, String> paramsMap = initData.getParamsMap();
        final JSONObject jsonResponse = new JSONObject();

        //Validate the parameters
        String endpointId = paramsMap.get( "endpoint" );
        String type = paramsMap.get( "type" );
        String whereToFix = paramsMap.get( "wheretofix" );

        if ( !UtilMethods.isSet( endpointId ) ) {
            return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: 'endpoint' is a required param." ).build();
        }

        if ( !UtilMethods.isSet( type ) ) {
            return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: 'type' is a required param." ).build();
        }

        if ( !UtilMethods.isSet( whereToFix ) ) {
            return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: 'whereToFix' is a required param." ).build();
        }

        if (!whereToFix.equals("local") && !whereToFix.equals("remote")) {
            return Response.status(HttpStatus.SC_BAD_REQUEST)
                    .entity("Error: 'whereToFix' has an invalid value.").build();
        }

        final Optional<IntegrityType> parsedType = parseIntegrityType(type);
        if (parsedType.isEmpty()) {
            return invalidIntegrityType();
        }
        final IntegrityType integrityTypeToFix = parsedType.get();

        final PublishingEndPoint endpoint;
        try {
            final Optional<PublishingEndPoint> foundEndpoint = findEndpoint(endpointId);
            if (foundEndpoint.isEmpty()) {
                return endpointNotFound();
            }
            endpoint = foundEndpoint.get();
        } catch (final DotDataException e) {
            Logger.error(this.getClass(), "Error loading the endpoint to fix conflicts", e);
            return response("Error loading the endpoint to fix conflicts", true);
        }

        IntegrityUtil integrityUtil = new IntegrityUtil();
        try {
            if (whereToFix.equals("local")) {
                integrityUtil.fixConflicts(endpointId, integrityTypeToFix);
                jsonResponse.put("success", true);
                jsonResponse.put("message", "Conflicts fixed in Local Endpoint");

                IntegrityType[] types = IntegrityType.values();
                boolean isThereAnyConflict = false;
                // Check if we still have other conflicts
                for (IntegrityType integrityType : types) {
                    if (!integrityType.equals(integrityTypeToFix)) {
                        List<Map<String, Object>> results = integrityUtil
                                .getIntegrityConflicts(endpointId, integrityType);
                        if (!results.isEmpty()) {
                            isThereAnyConflict = true;
                            break;
                        }
                    }
                }

                integrityUtil.flushAllCache();

                if (!isThereAnyConflict)
                    clearStatus(httpServletRequest, endpointId);

            } else if (whereToFix.equals("remote")) {
                integrityUtil.generateDataToFixZip(endpointId, integrityTypeToFix);

                final File bundle = new File(IntegrityUtil.getIntegrityDataFilePath(
                        endpointId,
                        IntegrityUtil.INTEGRITY_DATA_TO_FIX_ZIP_FILENAME));

                final Response response = sendFixConflictsRequest(type, endpoint, bundle);

                if (response.getStatus() == HttpStatus.SC_OK) {
                    jsonResponse.put("success", true);
                    jsonResponse.put("message",
                            "Fix Conflicts Process successfully started at Remote.");
                    clearStatus(httpServletRequest, endpointId);
                } else {
                    Logger.error(this.getClass(),
                            "Response indicating a " + response.getStatusInfo().getReasonPhrase()
                                    + " (" + response.getStatus()
                                    + ") Error trying to fix conflicts on " + whereToFix
                                    + " end-point [" + endpointId + "].");
                    return Response.status(HttpStatus.SC_BAD_REQUEST)
                            .entity("Endpoint with id: " + endpointId + " returned server error.")
                            .build();
                }
            }
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch ( Exception e ) {

            Logger.error( this.getClass(), "Error fixing "+type+" conflicts for End Point server: [" + endpointId + "]", e );
            return response( "Error fixing conflicts for endpoint: " + endpointId , true );
        } finally {
            try {
                // Discard conflicts if successful or failed
                integrityUtil.discardConflicts(endpointId, integrityTypeToFix);
            } catch (DotDataException e) {
                Logger.error(this.getClass(), "ERROR: Table " + integrityTypeToFix.getResultsTableName()
                        + " could not be cleared on end-point [" + endpointId
                        + "]. Please truncate the table data manually.", e);
            }
        }

        return response( jsonResponse.toString(), false );
    }

    private Response sendFixConflictsRequest(String type, PublishingEndPoint endpoint, File bundle) {
        FormDataMultiPart form = new FormDataMultiPart();
        form.field("TYPE", type);
        form.bodyPart(
                new FileDataBodyPart("DATA_TO_FIX",
                bundle,
                MediaType.MULTIPART_FORM_DATA_TYPE));
        return postWithEndpointState(
                endpoint,
                String.format("%s/api/integrity/_fixconflictsfromremote/", endpoint.toURL()),
                Entity.entity(form, form.getMediaType()));
    }

    /**
     * Removes the status for the checking integrity process of a given endpoint from session
     *
     * @param request
     * @param endpointId
     */
    private void clearStatus ( HttpServletRequest request, String endpointId ) {
        clearStatus( request.getSession(), endpointId );
    }

    /**
     * Removes the status for the checking integrity process of a given endpoint from session
     *
     * @param session
     * @param endpointId
     */
    private void clearStatus ( HttpSession session, String endpointId ) {
        session.removeAttribute( "integrityCheck_" + endpointId );
        session.removeAttribute( "integrityCheck_message_" + endpointId );
        clearThreadInSession( session, endpointId );
    }

    /**
     * Sets the status for the checking integrity process of a given endpoint in session
     *
     * @param request
     * @param endpointId
     * @param status
     */
    private static void setStatus ( HttpServletRequest request, String endpointId, ProcessStatus status ) {
        setStatus( request, endpointId, status, null );
    }

    /**
     * Sets the status for the checking integrity process of a given endpoint in session
     *
     * @param request
     * @param endpointId
     * @param status
     * @param message
     */
    private static void setStatus ( HttpServletRequest request, String endpointId, ProcessStatus status, String message ) {
        setStatus( request.getSession(), endpointId, status, message );
    }

    /**
     * Sets the status for the checking integrity process of a given endpoint as an attribute in the
     * current session.
     *
     * @param session    The current instance of the {@link HttpSession}.
     * @param endpointId The ID of the Endpoint whose data integrity is being checked.
     * @param status     The {@link ProcessStatus} of the integrity check process.
     * @param message    An optional message associated with the integrity check process.
     */
    public static void setStatus(final HttpSession session, final String endpointId, final ProcessStatus status, final String message ) {
        session.setAttribute( "integrityCheck_" + endpointId, status );
        if ( message != null ) {
            session.setAttribute( "integrityCheck_message_" + endpointId, message );
        } else {
            session.removeAttribute( "integrityCheck_message_" + endpointId );
        }
        // Required when the Tomcat Redis Session Manager plugin is enabled. This forces the session
        // to be persisted to Redis and correctly update the Integrity Checker status
        session.setAttribute( "__dot_session_persist_now__", true);
    }

    /**
     * Removes the integrity checking thread from session and a given endpoint id
     *
     * @param request
     * @param endpointId
     */
    private void clearThreadInSession ( HttpServletRequest request, String endpointId ) {
        clearThreadInSession( request.getSession(), endpointId );
    }

    /**
     * Removes the integrity checking thread from session and a given endpoint id
     *
     * @param session
     * @param endpointId
     */
    private void clearThreadInSession ( HttpSession session, String endpointId ) {
        session.removeAttribute( "integrityThread_" + endpointId );
        session.removeAttribute( "integrityDataRequest_" + endpointId );
    }

    /**
     * Adds the integrity checking thread into the session for a given endpoint id
     *
     * @param session
     * @param thread
     * @param endpointId
     * @param integrityDataRequestID
     */
    private void addThreadToSession ( HttpSession session, Thread thread, String endpointId, String integrityDataRequestID ) {
        session.setAttribute( "integrityThread_" + endpointId, ProcessStatus.PROCESSING );
        session.setAttribute( "integrityDataRequest_" + endpointId, integrityDataRequestID );
    }

    /**
     * Prepares a Response object with a given response text. The creation depends if it is an error or not.
     *
     * @param response
     * @param error
     * @return
     */
    private Response response ( String response, Boolean error ) {
        return response( response, error, "application/json" );
    }

    /**
     * Prepares a Response object with a given response text. The creation depends if it is an error or not.
     *
     * @param response
     * @param error
     * @param contentType
     * @return
     */
    private Response response ( String response, Boolean error, String contentType ) {
        if ( error ) {
            return response(response, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } else {
            return Response.ok( response, contentType ).build();
        }
    }

    private Response response ( String response, int status ) {
        return Response.status( status ).entity( response ).build();
    }

    private class IntegrityDataRequestChecker implements Runnable{

        private final User loggedUser;
        private final HttpSession session;
        private final PublishingEndPoint endpoint;
        private final String integrityDataRequestID;

        public IntegrityDataRequestChecker(
                final User loggedUser,
                final HttpSession session,
                final PublishingEndPoint endpoint,
                final String integrityDataRequestID) {

            this.loggedUser = loggedUser;
            this.session = session;
            this.endpoint = endpoint;
            this.integrityDataRequestID = integrityDataRequestID;
        }

        @CloseDBIfOpened
        public void run(){

            boolean processing = true;

            while(processing) {

                final Response response = statusIntegrityCheckerRequest();

                if (response.getStatus() == HttpStatus.SC_OK) {

                    processing = false;

                    InputStream zipFile = response.readEntity(InputStream.class);
                    String outputDir = ConfigUtils.getIntegrityPath() + File.separator + endpoint.getId();

                    try {
                        ZipUtil.extract(zipFile, outputDir);
                    } catch (Exception e) {
                        setStatus(session, endpoint.getId(), ProcessStatus.ERROR, null);
                        Logger.error(IntegrityResource.class, "Error while unzipping Integrity Data", e);
                        throw new RuntimeException("Error while unzipping Integrity Data", e);
                    }

                    // set session variable
                    // call IntegrityChecker
                    boolean conflictPresent;

                    IntegrityUtil integrityUtil = new IntegrityUtil();
                    try {
                        IntegrityUtil.completeDiscardConflicts(endpoint.getId());
                        conflictPresent = IntegrityUtil.completeCheckIntegrity(endpoint.getId());
                        Logger.debug(IntegrityResource.class, "================ Integrity check completed ================");
                    } catch (Exception e) {
                        Logger.error(IntegrityResource.class, "Error checking integrity", e);

                        //Setting the process status
                        setStatus(session, endpoint.getId(), ProcessStatus.ERROR, null);
                        throw new RuntimeException("Error checking integrity", e);
                    } finally {
                        try {
                            integrityUtil.dropTempTables(endpoint.getId());
                        } catch (DotHibernateException e) {
                            Logger.warn(this, e.getMessage(), e);
                        } catch (DotDataException e) {
                            Logger.error(IntegrityResource.class, "Error while deleting temp tables", e);
                        }
                    }

                    Logger.debug(IntegrityResource.class, "is Conflict Present? " + conflictPresent);

                    if (conflictPresent) {
                        //Setting the process status
                        Logger.debug(IntegrityResource.class, "Setting status to finished");
                        setStatus(session, endpoint.getId(), ProcessStatus.FINISHED, null);
                    } else {
                        String noConflictMessage;
                        try {
                            Logger.debug(IntegrityResource.class, "Getting no conflicts message");
                            noConflictMessage = LanguageUtil.get(
                                    loggedUser.getLocale(),
                                    "push_publish_integrity_conflicts_not_found");
                        } catch (LanguageException e) {
                            noConflictMessage = "No Integrity Conflicts found";
                        } catch (Exception e) {
                            Logger.error(IntegrityResource.class, "Error while getting no conflicts message", e);
                            noConflictMessage = "No Integrity Conflicts found";
                        }

                        Logger.debug(IntegrityResource.class, "Setting status to no conflicts");
                        //Setting the process status
                        setStatus(session, endpoint.getId(), ProcessStatus.NO_CONFLICTS, noConflictMessage);
                    }

                } else if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
                    Logger.info(this.getClass(), "Integrity check is still processing");
                } else {
                    setStatus(session, endpoint.getId(), ProcessStatus.ERROR, null);
                    Logger.error(
                            this.getClass(),
                            "Response indicating a " + response.getStatusInfo().getReasonPhrase() + " (" + response.getStatus() + ") Error trying to retrieve the Integrity data from the Endpoint [" + endpoint.getId() + "].");
                    processing = false;
                }
            }
        }

        private Response statusIntegrityCheckerRequest() {
            return getWithEndpointState(
                    endpoint,
                    String.format("%s/api/integrity/%s/integrityData", endpoint.toURL(), integrityDataRequestID));
        }
    }
}
