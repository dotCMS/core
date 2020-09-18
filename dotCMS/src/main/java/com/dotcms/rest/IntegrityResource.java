package com.dotcms.rest;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.integritycheckers.IntegrityType;
import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.repackage.com.google.common.cache.Cache;
import com.dotcms.repackage.com.google.common.cache.CacheBuilder;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.quartz.job.IntegrityDataGenerationJob;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

/**
 * This REST end-point provides all the required mechanisms for the execution of
 * integrity checks related to some objects in dotCMS. When pushing content from
 * one server to another, there might be situations in which an object (e.g., a
 * Content Page) already exists in the destination server(s) with a different
 * Identifier. This will generate an error and the bundle will fail to publish.
 * <p>
 * Therefore, it is always recommended to run the Integrity Checker feature
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
public class IntegrityResource {

    private final WebResource webResource = new WebResource();

    private final static Cache<String, EndpointState> endpointStateCache =
    		CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();    

    public enum ProcessStatus {
        PROCESSING, ERROR, FINISHED, NO_CONFLICTS, CANCELLED
    }

    private static class EndpointState {
    	private final Map<String, Cookie> cookies = new ConcurrentHashMap<String, Cookie>();

    	public Map<String, Cookie> getCookies() {
    		return cookies;
    	}

    	public void addCookie(String name, Cookie cookie) {
    		cookies.put(name, cookie);
    	}
    }

    private static void cacheEndpointState(String endpointId, Map<String, NewCookie> cookiesMap) {

    	EndpointState endpointState = endpointStateCache.getIfPresent(endpointId);
    	if (endpointState == null) {
    		endpointStateCache.put(endpointId, endpointState = new EndpointState());
    	}

    	for (Map.Entry<String, NewCookie> cookieEntry : cookiesMap.entrySet()) {
    		endpointState.addCookie(cookieEntry.getKey(), cookieEntry.getValue());
    	}
    }

    private static void applyEndpointState(String endpointId, Builder requestBuilder) {

    	final EndpointState endpointState = endpointStateCache.getIfPresent(endpointId);

    	if (endpointState != null) {
        	for (Cookie cookie : endpointState.getCookies().values()) {
        		requestBuilder.cookie(cookie);
        	}
    	}
    }

    // https://github.com/dotCMS/core/issues/9067
	public static Response postWithEndpointState(String endpointId, String url, MediaType mediaType, Entity<?> entity) {

		final Builder requestBuilder = RestClientBuilder.newClient().target(url).request(mediaType);

		applyEndpointState(endpointId, requestBuilder);

		final Response response = requestBuilder.post(entity);

		cacheEndpointState(endpointId, response.getCookies());

		return response;
	}

    /**
     * Resolves remote IP address from request.
     * @param request {@link HttpServletRequest}
     * @return a String representing the remote IP address (or hostname)
     */
    private static String resolveRemoteIp(final HttpServletRequest request) {
        final String remoteIP = request.getRemoteHost();
        return !UtilMethods.isSet(remoteIP) ? remoteIP : request.getRemoteAddr();
    }

    /**
     * Tries to get the local address plus the port in a "host:port" format
     * @param request http servlet request
     * @return a string representing the address plus the port
     */
    private static String getFullLocalIp(@Context final HttpServletRequest request) {
        final String localIp = request.getLocalName();
        final Optional<String> port = HttpRequestDataUtil.getServerPort();
        return (!UtilMethods.isSet(localIp) ? localIp : request.getLocalName())
                + ':' + port.orElse(String.valueOf(request.getLocalPort()));
    }

    /**
     * <p>Returns a zip with data from structures and folders for integrity check
     */
    @POST
    @Path("/generateintegritydata/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response generateIntegrityData(
            @Context final HttpServletRequest request,
            @FormDataParam("AUTH_TOKEN") final String auth_token_digest)  {
        final String localAddress = getFullLocalIp(request);
        if (!UtilMethods.isSet(auth_token_digest)) {
            final String message = "Error: Authentication Token was not found.";
            Logger.error(IntegrityResource.class, String.format("Receiver at %s> :%s", localAddress, message));
            return Response
                    .status(HttpStatus.SC_BAD_REQUEST)
                    .entity(message)
                    .build();
        }

        final String remoteIp = resolveRemoteIp(request);
        final PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        try {
            final PublishingEndPoint requesterEndpoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIp);
            if (!BundlePublisherResource.isValidToken(auth_token_digest, remoteIp, requesterEndpoint)) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> Authentication Token is invalid for ip: %s and endpoint id %s",
                                localAddress,
                                remoteIp,
                                requesterEndpoint.getId()));
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            if (QuartzUtils.isJobRunning(
                    IntegrityDataGenerationJob.JOB_NAME,
                    IntegrityDataGenerationJob.JOB_GROUP)) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> job is already running for endpoint id: %s, so aborting generation",
                                localAddress,
                                requesterEndpoint.getId()));
                throw new WebApplicationException(
                        Response.status(HttpStatus.SC_CONFLICT)
                                .entity("Already Running")
                                .build());
            }

            final String transactionId = UUIDGenerator.generateUuid();
            IntegrityDataGenerationJob.triggerIntegrityDataGeneration(requesterEndpoint, transactionId);
            Logger.info(
                    IntegrityResource.class,
                    String.format(
                            "Receiver at %s:> job triggered for endpoint id: %s and requester id: %s",
                            localAddress,
                            requesterEndpoint.getId(),
                            transactionId));

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
            }
        }

        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Checks if the generation of Integrity Data is done.
     * If FINISHED, returns a zip with the data
     * if PROCESSING, returns HttpStatus.SC_PROCESSING
     * if ERROR, returns HttpStatus.SC_INTERNAL_SERVER_ERROR, including the error message
     *
     * Usage: /getdata
     *
     */
    @POST
    @Path("/getintegritydata/{params:.*}")
    @Produces("application/zip")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response getIntegrityData(@Context final HttpServletRequest request,
                                     @FormDataParam("AUTH_TOKEN") final String auth_token_digest,
                                     @FormDataParam("REQUEST_ID") final String requestId)  {
        final String remoteIp = resolveRemoteIp(request);
        final String localAddress = getFullLocalIp(request);

        final PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        try {
            final PublishingEndPoint requesterEndpoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIp);

            if (!BundlePublisherResource.isValidToken(auth_token_digest, remoteIp, requesterEndpoint) ||
                    !UtilMethods.isSet(requestId)) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> Authentication Token is invalid for ip: %s and endpoint id %s",
                                localAddress,
                                remoteIp,
                                requesterEndpoint.getId()));
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            if (QuartzUtils.isJobRunning(
                    IntegrityDataGenerationJob.JOB_NAME,
                    IntegrityDataGenerationJob.JOB_GROUP)) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> job is already running for endpoint id: %s, therefore it's not ready and need to wait",
                                localAddress,
                                requesterEndpoint.getId()));
                return Response.status(HttpStatus.SC_PROCESSING).build();
            }

            final Optional<IntegrityUtil.IntegrityDataExecutionMetadata> integrityMetadata =
                    IntegrityUtil.getIntegrityMetadata(requesterEndpoint.getId());
            if (!integrityMetadata.isPresent()) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation metadata for endpoint id %s is not found ",
                                localAddress,
                                requesterEndpoint.getId()));
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            if (!requestId.equals(integrityMetadata.get().getRequestId())) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation metadata for endpoint id %s has a request id %s which does not match the provided %s",
                                localAddress,
                                requesterEndpoint.getId(),
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
                                requesterEndpoint.getId()));
                return Response.status(HttpStatus.SC_PROCESSING).build();
            } else if (integrityMetadata.get().getProcessStatus() == ProcessStatus.FINISHED &&
                    IntegrityUtil.doesIntegrityDataFileExist(
                            requesterEndpoint.getId(),
                            IntegrityUtil.INTEGRITY_DATA_TO_CHECK_ZIP_FILENAME)) {
                final String zipFilePath = IntegrityUtil.getIntegrityDataFilePath(
                        requesterEndpoint.getId(),
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
                                requesterEndpoint.getId(),
                                zipFilePath));
                return Response.ok(output).build();
            } else if (integrityMetadata.get().getProcessStatus() == ProcessStatus.CANCELLED) {
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation for endpoint id %s is cancelled",
                                localAddress,
                                requesterEndpoint.getId()));
                return Response
                        .status(HttpStatus.SC_RESET_CONTENT)
                        .entity(integrityMetadata.get().getErrorMessage())
                        .build();
            } else if (integrityMetadata.get().getProcessStatus() == ProcessStatus.ERROR) {
                final String message = StringUtils.defaultString(
                        String.format(" due to '%s'", integrityMetadata.get().getErrorMessage()),
                        "");
                Logger.error(
                        IntegrityResource.class,
                        String.format(
                                "Receiver at %s:> integrity data generation for endpoint id %s has failed%s",
                                localAddress,
                                requesterEndpoint.getId(),
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
	 * 
	 * @param httpServletRequest
	 *            - The {@link HttpServletRequest} that started the process.
	 * @param params
	 *            - The execution parameters for running the process: The
	 *            end-point ID.
	 * @return The REST {@link Response} with the status of the operation.
	 */
    @GET
    @Path("/checkintegrity/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response checkIntegrity(@Context final HttpServletRequest httpServletRequest,
                                   @Context final HttpServletResponse httpServletResponse,
                                   @PathParam("params") final String params)  {
        final InitDataObject initData = webResource.init(params, httpServletRequest, httpServletResponse, true, null);
        final Map<String, String> paramsMap = initData.getParamsMap();
        final HttpSession session = httpServletRequest.getSession();
        final User loggedUser = initData.getUser();
        final JSONObject jsonResponse = new JSONObject();

        //Validate the parameters
        final String endpointId = paramsMap.get("endpoint");
        if (!UtilMethods.isSet(endpointId)) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Error: endpoint is a required Field.").build();
        }

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
            final PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findEndPointById(endpointId);
            final Optional<String> authToken = PushPublisher.retriveEndpointKeyDigest(endpoint);
            if (!authToken.isPresent()) {
              Logger.warn(IntegrityResource.class, "No Auth Token set for endpoint:" + endpointId);
              return response("No Auth Token set for endpoint", true);
            }

            final FormDataMultiPart form = new FormDataMultiPart();
            form.field("AUTH_TOKEN", authToken.get());

            //Sending bundle to endpoint
            final String url = endpoint.toURL() + "/api/integrity/generateintegritydata/";
            final Response response = postWithEndpointState(
                    endpoint.getId(),
                    url,
                    MediaType.TEXT_PLAIN_TYPE,
                    Entity.entity(form, form.getMediaType())
            );

            if (response.getStatus() == HttpStatus.SC_OK) {
                final String integrityDataRequestId = response.readEntity(String.class);
                final Thread integrityDataRequestChecker = new Thread(
                        new IntegrityDataRequestChecker(
                                authToken.get(),
                                endpoint,
                                integrityDataRequestId,
                                session,
                                initData));
                //Start the integrity check
                integrityDataRequestChecker.start();
                addThreadToSession(session, integrityDataRequestChecker, endpointId, integrityDataRequestId);
            } else if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED) {
                setStatus(session, endpointId, ProcessStatus.ERROR, null);
                final String message =
                        "Response indicating Not Authorized received from Endpoint. Please check Auth Token. Endpoint Id: "
                        + endpointId;
                Logger.error(this.getClass(), message);
                return response(message, true);
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

            //Special handling if the thread was interrupted
            if (e.getCause() instanceof InterruptedException) {
                //Setting the process status
                setStatus(session, endpointId, ProcessStatus.CANCELLED, null);
                Logger.debug(
                        IntegrityResource.class,
                        "Requested interruption of the integrity checking process by the user.",
                        e);
                return response(
                        "Requested interruption of the integrity checking process by the user for End Point server: ["
                                + endpointId + "]",
                        true );
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

    /**
     * Method that will interrupt the integrity checking running processes locally and in the end point server
     *
     * @param httpServletRequest
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/cancelIntegrityProcess/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response cancelIntegrityProcess(@Context final HttpServletRequest httpServletRequest,
                                           @Context final HttpServletResponse httpServletResponse,
                                           @PathParam("params") final String params ) {
        final StringBuilder responseMessage = new StringBuilder();
        final InitDataObject initData = webResource.init(params, httpServletRequest, httpServletResponse, true, null);
        final Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        final String endpointId = paramsMap.get("endpoint");
        if (!UtilMethods.isSet(endpointId)) {
            final Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_BAD_REQUEST);
            responseBuilder.entity(responseMessage.append("Error: endpoint is a required Field."));
            return responseBuilder.build();
        }

        try {
            final JSONObject jsonResponse = new JSONObject();
            final HttpSession session = httpServletRequest.getSession();

            //Verify if we have something set on the session
            if ( session.getAttribute( "integrityCheck_" + endpointId ) == null ) {
                //And prepare the response
                jsonResponse.put("success", false);
                jsonResponse.put("message", "No checking process found for End point server [" + endpointId + "]");
            } else if (session.getAttribute("integrityThread_" + endpointId) == null) {
                //And prepare the response
                jsonResponse.put("success", false);
                jsonResponse.put("message", "No checking process found for End point server [" + endpointId + "]");
            } else {
                //Search for the status on session
                final ProcessStatus status = (ProcessStatus) session.getAttribute("integrityCheck_" + endpointId);

                //And prepare the response
                jsonResponse.put("endPoint", endpointId);
                if (status == ProcessStatus.PROCESSING) {
                    //Find the registered auth token in order to connect to the end point server
                    final PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findEndPointById(endpointId);
                    final Optional<String> authToken = PushPublisher.retriveEndpointKeyDigest(endpoint);
                    if(!authToken.isPresent()) {
                        return Response
                                .status(HttpStatus.SC_BAD_REQUEST)
                                .entity(responseMessage.append("Error: endpoint requires an authorization key"))
                                .build();
                    }

                    final String integrityDataRequestId = (String) session.getAttribute( "integrityDataRequest_" + endpointId );
                    final FormDataMultiPart form = new FormDataMultiPart();
                    form.field("AUTH_TOKEN", authToken.get());
                    form.field("REQUEST_ID", integrityDataRequestId);
                    //Prepare the connection
                    final String url = endpoint.toURL() + "/api/integrity/cancelIntegrityProcessOnEndpoint/";
                    //Execute the call
                    final Response response = postWithEndpointState(
                            endpoint.getId(),
                            url,
                            MediaType.APPLICATION_JSON_TYPE,
                            Entity.entity(form, form.getMediaType()));

                    if (response.getStatus() == HttpStatus.SC_OK) {
                        //Nothing to do here, we found no process to cancel
                    } else if (response.getStatus() == HttpStatus.SC_RESET_CONTENT) {
                        //Expected return status if a cancel was made on the end point server
                    } else {
                        Logger.error(
                                this.getClass(),
                                "Response indicating a " + response.getStatusInfo().getReasonPhrase() + " ("
                                        + response.getStatus()
                                        + ") Error trying to interrupt the running process on the Endpoint [ "
                                        + endpointId + "].");
                    }

                    //Get the thread associated to this endpoint and the integrity request id
                    final Thread runningThread = (Thread) session.getAttribute( "integrityThread_" + endpointId );
                    //Interrupt the Thread process
                    runningThread.interrupt();
                    //Remove the thread from the session
                    clearThreadInSession(httpServletRequest, endpointId);

                    jsonResponse.put("success", true);
                    jsonResponse.put(
                            "message",
                            LanguageUtil.get(initData.getUser().getLocale(), "IntegrityCheckingCanceled"));
                }
            }

            responseMessage.append(jsonResponse.toString());
        } catch (Exception e) {
            Logger.error(
                    this.getClass(),
                    "Error checking the integrity process status for End Point server: [" + endpointId + "]",
                    e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }

            return response(
                    "Error checking the integrity process status for End Point server: [" + endpointId + "]",
                    true);
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Method expected to run on an end point server in order to interrupt the integrity checking process if running
     *
     * @param request
     * @param auth_token_digest
     * @param requestId
     * @return
     */
    @POST
    @Path("/cancelIntegrityProcessOnEndpoint/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces (MediaType.APPLICATION_JSON)
    public Response cancelIntegrityProcessOnEndpoint(
            @Context final HttpServletRequest request,
            @FormDataParam ("AUTH_TOKEN") final String auth_token_digest,
            @FormDataParam ("REQUEST_ID") final String requestId) {
        final String remoteIp = resolveRemoteIp(request);
        final PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        try {
            //Search for the given end point
            final PublishingEndPoint requesterEndPoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIp);

            //Verify the authentication token
            if (!BundlePublisherResource.isValidToken(auth_token_digest, remoteIp, requesterEndPoint) ||
                    !UtilMethods.isSet(requestId)) {
                return Response.status( HttpStatus.SC_UNAUTHORIZED ).build();
            }

            if (!QuartzUtils.isJobRunning(
                    IntegrityDataGenerationJob.JOB_NAME,
                    IntegrityDataGenerationJob.JOB_GROUP)) {
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            final Optional<IntegrityUtil.IntegrityDataExecutionMetadata> controlOpt =
                    IntegrityUtil.getIntegrityMetadata(requesterEndPoint.getId());
            if (!controlOpt.isPresent() || !requestId.equals(controlOpt.get().getRequestId())) {
                return Response.status( HttpStatus.SC_UNAUTHORIZED ).build();
            }

            final ProcessStatus processStatus = controlOpt.get().getProcessStatus();
            if (processStatus == ProcessStatus.PROCESSING) {
                IntegrityDataGenerationJob.getJobScheduler().interrupt(
                        IntegrityDataGenerationJob.JOB_NAME,
                        IntegrityDataGenerationJob.JOB_GROUP);
                return Response
                        .status(HttpStatus.SC_RESET_CONTENT)
                        .entity("Interrupted checking process on End Point server ( " + remoteIp + ").")
                        .build();
            }
        } catch ( Exception e ) {
            Logger.error( IntegrityResource.class, "Error caused by remote call of: " + remoteIp, e );
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            return response( "Error interrupting checking process on End Point server ( " + remoteIp + "). [" + e.getMessage() + "]", true );
        }

        return response( "Interrupted checking process on End Point server ( " + remoteIp + ").", false );
    }

    /**
     * Method that will verify the status of a check integrity process for a given server
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/checkIntegrityProcessStatus/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response checkIntegrityProcessStatus ( @Context final HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = webResource.init(params, request, response, true, null);
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        String endpointId = paramsMap.get( "endpoint" );
        if ( !UtilMethods.isSet( endpointId ) ) {
            Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_BAD_REQUEST );
            responseBuilder.entity( responseMessage.append( "Error: " ).append( "endpoint" ).append( " is a required Field." ) );

            return responseBuilder.build();
        }

        try {
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
                } else if ( status == ProcessStatus.CANCELLED) {
                    jsonResponse.put( "status", "canceled" );
                    jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "IntegrityCheckingCanceled" ) );
                    clearStatus( request, endpointId );
                } else {
                    jsonResponse.put( "status", "error" );
                    jsonResponse.put( "message", "Error checking the integrity process status for End Point server: [" + endpointId + "]" );
                    clearStatus( request, endpointId );
                }
            }

            responseMessage.append( jsonResponse.toString() );

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
     * Generates and returns the integrity check results for a given server
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/getIntegrityResult/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response getIntegrityResult ( @Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = webResource.init(params, request, response, true, null);
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        String endpointId = paramsMap.get( "endpoint" );
        if ( !UtilMethods.isSet( endpointId ) ) {
            Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_BAD_REQUEST );
            responseBuilder.entity( responseMessage.append( "Error: " ).append( "endpoint" ).append( " is a required Field." ) );

            return responseBuilder.build();
        }

        try {

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
     * Method that will discard the conflicts between local node and given endpoint
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/discardconflicts/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response discardConflicts ( @Context final HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = webResource.init(params, request, response, true, null);
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

        try {
            JSONObject jsonResponse = new JSONObject();

            IntegrityUtil integrityUtil = new IntegrityUtil();
            integrityUtil.discardConflicts(endpointId, IntegrityType.valueOf(type.toUpperCase()));

            clearStatus( request, endpointId );

            responseMessage.append( jsonResponse.toString() );

        } catch ( Exception e ) {
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
        	Logger.error(this.getClass(), "ERROR: Table "
					+ IntegrityType.valueOf(type.toUpperCase()).getResultsTableName()
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
     * @param auth_token_digest
     * @param type
     * @return
     * @throws JSONException
     */
    @POST
    @Path("/fixconflictsfromremote/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response fixConflictsFromRemote (@Context final HttpServletRequest request,
                                            @FormDataParam("DATA_TO_FIX") final InputStream dataToFix,
                                            @FormDataParam("AUTH_TOKEN") final String auth_token_digest,
                                            @FormDataParam("TYPE") final String type ) throws JSONException {
        final String remoteIp = resolveRemoteIp(request);
        JSONObject jsonResponse = new JSONObject();
        IntegrityUtil integrityUtil = new IntegrityUtil();
        PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        PublishingEndPoint requesterEndPoint = null;
        try {
            requesterEndPoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIp);

            if (!BundlePublisherResource.isValidToken(auth_token_digest, remoteIp, requesterEndPoint)) {
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            HibernateUtil.startTransaction();
            integrityUtil.fixConflicts(dataToFix, requesterEndPoint.getId(),
                    IntegrityType.valueOf(type.toUpperCase()));
            HibernateUtil.commitTransaction();
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch ( Exception e ) {
            try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(IntegrityResource.class, "Error while rolling back transaction", e);
            }
            Logger.error( this.getClass(), "Error fixing "+type+" conflicts from remote", e );
            return response( "Error fixing "+type+" conflicts from remote" , true );
        } finally {
			try {
				if (requesterEndPoint != null) {
					// Discard conflicts if successful or failed
					integrityUtil.discardConflicts(requesterEndPoint.getId(),
							IntegrityType.valueOf(type.toUpperCase()));
				}
			} catch (DotDataException e) {
				Logger.error(this.getClass(), "ERROR: Table "
						+ IntegrityType.valueOf(type.toUpperCase()).getResultsTableName()
						+ " could not be cleared on end-point [" + requesterEndPoint.getId()
						+ "]. Please truncate the table data manually.", e);
			}

			HibernateUtil.closeSessionSilently();
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
	 *
	 * @param httpServletRequest
	 *            - The {@link HttpServletRequest} that started the process.
	 * @param params
	 *            - The execution parameters for running the process: The
	 *            end-point ID.
	 * @return The REST {@link Response} with the status of the operation.
	 * @throws JSONException
	 *             An error occurred when generating the JSON response.
	 */
    @GET
    @Path ("/fixconflicts/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response fixConflicts ( @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse, @PathParam ("params") final String params ) throws JSONException {

        final InitDataObject initData = webResource.init(params, httpServletRequest, httpServletResponse, true, null);
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

        IntegrityUtil integrityUtil = new IntegrityUtil();
		IntegrityType integrityTypeToFix = IntegrityType.valueOf(type.toUpperCase());
        try {
            if (whereToFix.equals("local")) {

                HibernateUtil.startTransaction();
                integrityUtil.fixConflicts(endpointId, integrityTypeToFix);
                HibernateUtil.commitTransaction();
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

                final Client client = RestClientBuilder.newClient();

                PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI()
                        .findEndPointById(endpointId);
                FormDataMultiPart form = new FormDataMultiPart();
                Optional<String> authToken = PushPublisher.retriveEndpointKeyDigest(endpoint);
                if ( !authToken.isPresent() ) {
                  return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: 'auth key' is a required param." ).build();
                }

                final File bundle = new File(IntegrityUtil.getIntegrityDataFilePath(
                        endpointId,
                        IntegrityUtil.INTEGRITY_DATA_TO_FIX_ZIP_FILENAME));
                form.field("AUTH_TOKEN",authToken.get());
                form.field("TYPE", type);
                form.bodyPart(new FileDataBodyPart("DATA_TO_FIX", bundle,
                        MediaType.MULTIPART_FORM_DATA_TYPE));
                String url = endpoint.toURL() + "/api/integrity/fixconflictsfromremote/";
                WebTarget webTarget = client.target(url);
                Response response = webTarget.request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity(form, form.getMediaType()));

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
            } else {
                return Response.status(HttpStatus.SC_BAD_REQUEST)
                        .entity("Error: 'whereToFix' has an invalid value.").build();
            }
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch ( Exception e ) {
        	try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(IntegrityResource.class, "Error while rolling back transaction", e);
            }

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
			HibernateUtil.closeSessionSilently();
		}

        return response( jsonResponse.toString(), false );
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
     * Sets the status for the checking integrity process of a given endpoint in session
     *
     * @param session
     * @param endpointId
     * @param status
     * @param message
     */
    public static void setStatus ( HttpSession session, String endpointId, ProcessStatus status, String message ) {
        session.setAttribute( "integrityCheck_" + endpointId, status );
        if ( message != null ) {
            session.setAttribute( "integrityCheck_message_" + endpointId, message );
        } else {
            session.removeAttribute( "integrityCheck_message_" + endpointId );
        }
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
        session.setAttribute( "integrityThread_" + endpointId, thread );
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
            return Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR ).entity( response ).build();
        } else {
            return Response.ok( response, contentType ).build();
        }
    }

}
