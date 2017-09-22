package com.dotcms.rest;

import com.dotcms.integritycheckers.IntegrityType;
import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.integrity.IntegrityDataGeneratorThread;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.repackage.com.google.common.cache.Cache;
import com.dotcms.repackage.com.google.common.cache.CacheBuilder;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.WebApplicationException;
import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.Invocation.Builder;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.Cookie;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.NewCookie;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.StreamingOutput;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataMultiPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataParam;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
 * <li>Workflows.</li>
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
        PROCESSING, ERROR, FINISHED, NO_CONFLICTS, CANCELED
    }

    public static final String INTEGRITY_DATA_TO_CHECK_ZIP_FILE_NAME = "DataToCheck.zip";
    public static final String INTEGRITY_DATA_TO_FIX_ZIP_FILE_NAME = "DataToFix.zip";


    private static class EndpointState {
    	private final Map<String, Cookie> cookies = new ConcurrentHashMap<String, Cookie>();

    	public Map<String, Cookie> getCookies() {
    		return cookies;
    	}

    	public void addCookie(String name, Cookie cookie) {
    		cookies.put(name, cookie);
    	}
    }

    private void cacheEndpointState(String endpointId, Map<String, NewCookie> cookiesMap) {

    	EndpointState endpointState = endpointStateCache.getIfPresent(endpointId);
    	if (endpointState == null) {
    		endpointStateCache.put(endpointId, endpointState = new EndpointState());
    	}

    	for (Map.Entry<String, NewCookie> cookieEntry : cookiesMap.entrySet()) {
    		endpointState.addCookie(cookieEntry.getKey(), cookieEntry.getValue());
    	}
    }

    private void applyEndpointState(String endpointId, Builder requestBuilder) {

    	final EndpointState endpointState = endpointStateCache.getIfPresent(endpointId);

    	if (endpointState != null) {
        	for (Cookie cookie : endpointState.getCookies().values()) {
        		requestBuilder.cookie(cookie);
        	}
    	}
    }

    // https://github.com/dotCMS/core/issues/9067
	private Response postWithEndpointState(String endpointId, String url, MediaType mediaType, Entity<?> entity) {

		final Builder requestBuilder = RestClientBuilder.newClient().target(url).request(mediaType);

		applyEndpointState(endpointId, requestBuilder);

		final Response response = requestBuilder.post(entity);

		cacheEndpointState(endpointId, response.getCookies());

		return response;
	}


    /**
     * <p>Returns a zip with data from structures, workflow schemes and folders for integrity check
     *
     * Usage: /getdata
     *
     */

    @POST
    @Path("/generateintegritydata/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response generateIntegrityData(@Context HttpServletRequest request, @FormDataParam("AUTH_TOKEN") String auth_token_enc)  {

        String remoteIP = null;
        try {

            if ( !UtilMethods.isSet( auth_token_enc ) ) {
                return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: 'endpoint' is a required param." ).build();
            }


            String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
            remoteIP = request.getRemoteHost();
            if(!UtilMethods.isSet(remoteIP))
                remoteIP = request.getRemoteAddr();

            PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
            final PublishingEndPoint requesterEndPoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

            if(!BundlePublisherResource.isValidToken(auth_token, remoteIP, requesterEndPoint)) {
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            ServletContext servletContext = request.getSession().getServletContext();

            if(servletContext.getAttribute("integrityRunning")!=null && ((Boolean) servletContext.getAttribute("integrityRunning"))) {
                throw new WebApplicationException(Response.status(HttpStatus.SC_CONFLICT).entity("Already Running").build());
            }

            String transactionId = UUIDGenerator.generateUuid();
            servletContext.setAttribute("integrityDataRequestID", transactionId);

            // start data generation process
            IntegrityDataGeneratorThread idg = new IntegrityDataGeneratorThread( requesterEndPoint, request.getSession().getServletContext() );
            idg.start();
            //Saving the thread on the session context for a later use
            servletContext.setAttribute( "integrityDataGeneratorThread_" + transactionId, idg );

            return Response.ok(transactionId).build();

        } catch (Exception e) {
            Logger.error(IntegrityResource.class, "Error caused by remote call of: "+remoteIP);
            Logger.error(IntegrityResource.class,e.getMessage(),e);
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
    public Response getIntegrityData(@Context HttpServletRequest request, @FormDataParam("AUTH_TOKEN") String auth_token_enc, @FormDataParam("REQUEST_ID") String requestId)  {
        String remoteIP = null;

        try {

            String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
            remoteIP = request.getRemoteHost();
            if(!UtilMethods.isSet(remoteIP))
                remoteIP = request.getRemoteAddr();

            PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
            final PublishingEndPoint requesterEndPoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

            if(!BundlePublisherResource.isValidToken(auth_token, remoteIP, requesterEndPoint) || !UtilMethods.isSet(requestId)) {
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            ServletContext servletContext = request.getSession().getServletContext();
            if(!UtilMethods.isSet(servletContext.getAttribute("integrityDataRequestID"))
                    || !((String) servletContext.getAttribute("integrityDataRequestID")).equals(requestId)) {
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            ProcessStatus integrityDataGeneratorStatus = (ProcessStatus) servletContext.getAttribute("integrityDataGenerationStatus");

            if(UtilMethods.isSet( integrityDataGeneratorStatus )) {
                switch (integrityDataGeneratorStatus) {
                    case PROCESSING:
                        return Response.status(HttpStatus.SC_PROCESSING).build();
                    case FINISHED:
                        StreamingOutput output = new StreamingOutput() {
                            public void write(OutputStream output) throws IOException, WebApplicationException {
                                InputStream is = Files.newInputStream(Paths.get(
                                        ConfigUtils.getIntegrityPath() + File.separator
                                                + requesterEndPoint.getId() + File.separator
                                                + INTEGRITY_DATA_TO_CHECK_ZIP_FILE_NAME));

                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                //read from is to buffer
                                while((bytesRead = is.read(buffer)) !=-1){
                                    output.write(buffer, 0, bytesRead);
                                }
                                is.close();
                                //flush OutputStream to write any buffered data to file
                                output.flush();
                                output.close();

                            }
                        };
                        return Response.ok(output).build();

                    case CANCELED:
                        return Response.status( HttpStatus.SC_RESET_CONTENT ).entity( servletContext.getAttribute( "integrityDataGenerationError" ) ).build();

                    case ERROR:
                        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(servletContext.getAttribute("integrityDataGenerationError")).build();

                    default:
                        break;
                }
            }

        } catch (Exception e) {
            Logger.error(IntegrityResource.class, "Error caused by remote call of: "+remoteIP, e);
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
	 * @param request
	 *            - The {@link HttpServletRequest} that started the process.
	 * @param params
	 *            - The execution parameters for running the process: The
	 *            end-point ID.
	 * @return The REST {@link Response} with the status of the operation.
	 */
    @GET
    @Path("/checkintegrity/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response checkIntegrity(@Context HttpServletRequest request, @PathParam("params") String params)  {
        InitDataObject initData = webResource.init(params, true, request, true, null);

        Map<String, String> paramsMap = initData.getParamsMap();

        final HttpSession session = request.getSession();
        final User loggedUser = initData.getUser();

        JSONObject jsonResponse = new JSONObject();

        //Validate the parameters
        final String endpointId = paramsMap.get( "endpoint" );
        if ( !UtilMethods.isSet( endpointId ) ) {
            return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: endpoint is a required Field.").build();
        }

        // return if we already have the data
        try {
            IntegrityUtil integrityUtil = new IntegrityUtil();

            if(integrityUtil.doesIntegrityConflictsDataExist(endpointId)) {

                jsonResponse.put( "success", true );
                jsonResponse.put( "message", "Integrity Checking Initialized..." );

                //Setting the process status
                setStatus( request, endpointId, ProcessStatus.FINISHED );

                return response( jsonResponse.toString(), false );
            }
        } catch(JSONException e) {
            Logger.error(IntegrityResource.class, "Error setting return message in JSON response", e);
            return response( "Error setting return message in JSON response" , true );
        } catch(Exception e) {
            Logger.error(IntegrityResource.class, "Error checking existence of integrity data", e);
            return response( "Error checking existence of integrity data" , true );
        }

        try {

            //Setting the process status
            setStatus( request, endpointId, ProcessStatus.PROCESSING );

            final PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findEndPointById(endpointId);
            final String authToken = PushPublisher.retriveKeyString(PublicEncryptionFactory.decryptString(endpoint.getAuthKey().toString()));

            FormDataMultiPart form = new FormDataMultiPart();
            form.field("AUTH_TOKEN",authToken);

            //Sending bundle to endpoint
            String url = endpoint.toURL()+"/api/integrity/generateintegritydata/";

            Response response = postWithEndpointState(
            	endpoint.getId(), url, MediaType.TEXT_PLAIN_TYPE, Entity.entity(form, form.getMediaType())
            );

            if(response.getStatus() == HttpStatus.SC_OK) {
                final String integrityDataRequestID = response.readEntity(String.class);

                Thread integrityDataRequestChecker = new Thread() {
                    public void run(){

                        FormDataMultiPart form = new FormDataMultiPart();
                        form.field("AUTH_TOKEN",authToken);
                        form.field("REQUEST_ID",integrityDataRequestID);

                        String url = endpoint.toURL()+"/api/integrity/getintegritydata/";

                        boolean processing = true;

                        while(processing) {

                        	Response response = postWithEndpointState(
                        		endpoint.getId(), url, new MediaType("application", "zip"), Entity.entity(form, form.getMediaType())
                        	);

                            if ( response.getStatus() == HttpStatus.SC_OK ) {

                                processing = false;

                                InputStream zipFile = response.readEntity(InputStream.class);
                                String outputDir = ConfigUtils.getIntegrityPath() + File.separator + endpoint.getId();

                                try {

                                    IntegrityUtil.unzipFile(zipFile, outputDir);

                                } catch(Exception e) {

                                    //Special handling if the thread was interrupted
                                    if ( e instanceof InterruptedException ) {
                                        //Setting the process status
                                        setStatus( session, endpointId, ProcessStatus.CANCELED, null );
                                        Logger.debug( IntegrityResource.class, "Requested interruption of the integrity checking process [unzipping Integrity Data] by the user.", e );
                                        throw new RuntimeException( "Requested interruption of the integrity checking process [unzipping Integrity Data] by the user.", e );
                                    }

                                    //Setting the process status
                                    setStatus( session, endpointId, ProcessStatus.ERROR, null );
                                    Logger.error(IntegrityResource.class, "Error while unzipping Integrity Data", e);
                                    throw new RuntimeException("Error while unzipping Integrity Data", e);
                                }

                                // set session variable
                                // call IntegrityChecker
                                boolean conflictPresent = false;

                                IntegrityUtil integrityUtil = new IntegrityUtil();
                                try {
                                	HibernateUtil.startTransaction();
                                	integrityUtil.completeDiscardConflicts(endpointId);
                                    HibernateUtil.commitTransaction();
                                    
                                    HibernateUtil.startTransaction();
                                    conflictPresent = integrityUtil.completeCheckIntegrity(endpointId);
                                    HibernateUtil.commitTransaction();
                                } catch(Exception e) {
                                    try {
                                        HibernateUtil.rollbackTransaction();
                                    } catch (DotHibernateException e1) {
                                        Logger.error(IntegrityResource.class, "Error while rolling back transaction", e);
                                    }

                                    //Special handling if the thread was interrupted
                                    if ( e instanceof InterruptedException ) {
                                        //Setting the process status
                                        setStatus( session, endpointId, ProcessStatus.CANCELED, null );
                                        Logger.debug( IntegrityResource.class, "Requested interruption of the integrity checking process by the user.", e );
                                        throw new RuntimeException( "Requested interruption of the integrity checking process by the user.", e );
                                    }

                                    Logger.error(IntegrityResource.class, "Error checking integrity", e);

                                    //Setting the process status
                                    setStatus( session, endpointId, ProcessStatus.ERROR, null );
                                    throw new RuntimeException("Error checking integrity", e);
                                } finally {
                                    try {
                                        integrityUtil.dropTempTables(endpointId);
                                        HibernateUtil.closeSession();
                                    } catch (DotHibernateException e) {
                                            Logger.warn(this, e.getMessage(), e);
                                    } catch (DotDataException e) {
                                        Logger.error(IntegrityResource.class, "Error while deleting temp tables", e);
                                    } finally {
                                        DbConnectionFactory.closeConnection();
                                    }
                                }

                                if(conflictPresent) {
                                    //Setting the process status
                                    setStatus( session, endpointId, ProcessStatus.FINISHED, null );
                                } else {
                                    String noConflictMessage;
                                    try {
                                        noConflictMessage = LanguageUtil.get( loggedUser.getLocale(), "push_publish_integrity_conflicts_not_found" );
                                    } catch ( LanguageException e ) {
                                        noConflictMessage = "No Integrity Conflicts found";
                                    }
                                    //Setting the process status
                                    setStatus( session, endpointId, ProcessStatus.NO_CONFLICTS, noConflictMessage );
                                }

                            } else if ( response.getStatus() == HttpStatus.SC_PROCESSING ) {

                                continue;
                            } else if ( response.getStatus() == HttpStatus.SC_RESET_CONTENT ) {
                                processing = false;
                                //Setting the process status
                                setStatus( session, endpointId, ProcessStatus.CANCELED, null );
                            } else {
                                setStatus( session, endpointId, ProcessStatus.ERROR, null );
                                Logger.error( this.getClass(), "Response indicating a " + response.getStatusInfo().getReasonPhrase() + " (" + response.getStatus() + ") Error trying to retrieve the Integrity data from the Endpoint [" + endpointId + "]." );
                                processing = false;
                            }
                        }
                    }
                };

                //Start the integrity check
                integrityDataRequestChecker.start();
                addThreadToSession( session, integrityDataRequestChecker, endpointId, integrityDataRequestID );

            } else if ( response.getStatus() == HttpStatus.SC_UNAUTHORIZED ) {
                setStatus( session, endpointId, ProcessStatus.ERROR, null );
                Logger.error( this.getClass(), "Response indicating Not Authorized received from Endpoint. Please check Auth Token. Endpoint Id: " + endpointId );
                return response( "Response indicating Not Authorized received from Endpoint. Please check Auth Token. Endpoint Id:" + endpointId, true );
            } else {
                setStatus( session, endpointId, ProcessStatus.ERROR, null );
                Logger.error( this.getClass(), "Response indicating a " + response.getStatusInfo().getReasonPhrase() + " (" + response.getStatus() + ") Error trying to connect with the Integrity API on the Endpoint. Endpoint Id: " + endpointId );
                return response( "Response indicating a " + response.getStatusInfo().getReasonPhrase() + " (" + response.getStatus() + ") Error trying to connect with the Integrity API on the Endpoint. Endpoint Id: " + endpointId, true );
            }

            jsonResponse.put( "success", true );
            jsonResponse.put( "message", "Integrity Checking Initialized..." );

        } catch(Exception e) {

            //Special handling if the thread was interrupted
            if ( e instanceof InterruptedException || e.getCause() instanceof InterruptedException ) {
                //Setting the process status
                setStatus( session, endpointId, ProcessStatus.CANCELED, null );
                Logger.debug( IntegrityResource.class, "Requested interruption of the integrity checking process by the user.", e );
                return response( "Requested interruption of the integrity checking process by the user for End Point server: [" + endpointId + "]" , true );
            }

            //Setting the process status
            setStatus( session, endpointId, ProcessStatus.ERROR, null );
            Logger.error( this.getClass(), "Error initializing the integrity checking process for End Point server: [" + endpointId + "]", e );
            return response( "Error initializing the integrity checking process for End Point server: [" + endpointId + "]" , true );
        }


        return response( jsonResponse.toString(), false );

    }

    /**
     * Method that will interrupt the integrity checking running processes locally and in the end point server
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/cancelIntegrityProcess/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response cancelIntegrityProcess ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = webResource.init(params, true, request, true, null);
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
                jsonResponse.put( "success", false );
                jsonResponse.put( "message", "No checking process found for End point server [" + endpointId + "]" );
            } else if ( session.getAttribute( "integrityThread_" + endpointId ) == null ) {
                //And prepare the response
                jsonResponse.put( "success", false );
                jsonResponse.put( "message", "No checking process found for End point server [" + endpointId + "]" );
            } else {

                //Search for the status on session
                ProcessStatus status = (ProcessStatus) session.getAttribute( "integrityCheck_" + endpointId );

                //And prepare the response
                jsonResponse.put( "endPoint", endpointId );
                if ( status == ProcessStatus.PROCESSING ) {

                    //Get the thread associated to this endpoint and the integrity request id
                    Thread runningThread = (Thread) session.getAttribute( "integrityThread_" + endpointId );
                    String integrityDataRequestId = (String) session.getAttribute( "integrityDataRequest_" + endpointId );

                    //Find the registered auth token in order to connect to the end point server
                    PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findEndPointById( endpointId );
                    String authToken = PushPublisher.retriveKeyString( PublicEncryptionFactory.decryptString( endpoint.getAuthKey().toString() ) );

                    FormDataMultiPart form = new FormDataMultiPart();
                    form.field( "AUTH_TOKEN", authToken );
                    form.field( "REQUEST_ID", integrityDataRequestId );

                    //Prepare the connection
                    String url = endpoint.toURL() + "/api/integrity/cancelIntegrityProcessOnEndpoint/";

                    //Execute the call
                	Response response = postWithEndpointState(
                		endpoint.getId(), url, MediaType.APPLICATION_JSON_TYPE, Entity.entity(form, form.getMediaType())
                	);

                    if ( response.getStatus() == HttpStatus.SC_OK ) {
                        //Nothing to do here, we found no process to cancel
                    } else if ( response.getStatus() == HttpStatus.SC_RESET_CONTENT ) {
                        //Expected return status if a cancel was made on the end point server
                    } else {
                        Logger.error( this.getClass(), "Response indicating a " + response.getStatusInfo().getReasonPhrase() + " (" + response.getStatus() + ") Error trying to interrupt the running process on the Endpoint [ " + endpointId + "]." );
                    }

                    //Interrupt the Thread process
                    runningThread.interrupt();

                    //Remove the thread from the session
                    clearThreadInSession( request, endpointId );

                    jsonResponse.put( "success", true );
                    jsonResponse.put( "message", LanguageUtil.get( initData.getUser().getLocale(), "IntegrityCheckingCanceled" ) );
                } else {
                    jsonResponse.put( "success", false );
                    jsonResponse.put( "message", "The integrity process for End Point server: [" + endpointId + "] was already stopped." );
                }
            }

            responseMessage.append( jsonResponse.toString() );

        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error checking the integrity process status for End Point server: [" + endpointId + "]", e );
            return response( "Error checking the integrity process status for End Point server: [" + endpointId + "]", true );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Method expected to run on an end point server in order to interrupt the integrity checking process if running
     *
     * @param request
     * @param auth_token_enc
     * @param requestId
     * @return
     */
    @POST
    @Path("/cancelIntegrityProcessOnEndpoint/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces (MediaType.APPLICATION_JSON)
    public Response cancelIntegrityProcessOnEndpoint ( @Context HttpServletRequest request, @FormDataParam ("AUTH_TOKEN") String auth_token_enc, @FormDataParam ("REQUEST_ID") String requestId ) {

        String remoteIP = null;

        try {

            remoteIP = request.getRemoteHost();
            if ( !UtilMethods.isSet( remoteIP ) ) {
                remoteIP = request.getRemoteAddr();
            }

            //Search for the given end point
            PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
            PublishingEndPoint requesterEndPoint = endpointAPI.findEnabledSendingEndPointByAddress( remoteIP );

            //Verify the authentication token
            String auth_token = PublicEncryptionFactory.decryptString( auth_token_enc );
            if ( !BundlePublisherResource.isValidToken( auth_token, remoteIP, requesterEndPoint ) || !UtilMethods.isSet( requestId ) ) {
                return Response.status( HttpStatus.SC_UNAUTHORIZED ).build();
            }

            ServletContext servletContext = request.getSession().getServletContext();
            if ( !UtilMethods.isSet( servletContext.getAttribute( "integrityDataRequestID" ) ) || !((String) servletContext.getAttribute( "integrityDataRequestID" )).equals( requestId ) ) {
                return Response.status( HttpStatus.SC_UNAUTHORIZED ).build();
            }

            //Verify the status and if the process it is still running we will interrupt it
            ProcessStatus integrityDataGeneratorStatus = (ProcessStatus) servletContext.getAttribute( "integrityDataGenerationStatus" );
            if ( UtilMethods.isSet( integrityDataGeneratorStatus ) ) {
                switch ( integrityDataGeneratorStatus ) {
                    case PROCESSING:

                        //Verify if the thread is on the session for this given request id
                        if ( servletContext.getAttribute( "integrityDataGeneratorThread_" + requestId ) != null ) {

                            //If found interrupt the process
                            IntegrityDataGeneratorThread integrityDataGeneratorThread = (IntegrityDataGeneratorThread) servletContext.getAttribute( "integrityDataGeneratorThread_" + requestId );
                            integrityDataGeneratorThread.interrupt();
                            servletContext.removeAttribute( "integrityDataGeneratorThread_" + requestId );

                            return Response.status( HttpStatus.SC_RESET_CONTENT ).entity( "Interrupted checking process on End Point server ( " + remoteIP + ")." ).build();
                        }
                    default:
                        break;
                }
            }

        } catch ( Exception e ) {
            Logger.error( IntegrityResource.class, "Error caused by remote call of: " + remoteIP, e );
            return response( "Error interrupting checking process on End Point server ( " + remoteIP + "). [" + e.getMessage() + "]", true );
        }

        return response( "Interrupted checking process on End Point server ( " + remoteIP + ").", false );
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
    public Response checkIntegrityProcessStatus ( @Context final HttpServletRequest request, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = webResource.init(params, true, request, true, null);
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
                } else if ( status == ProcessStatus.CANCELED) {
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
    public Response getIntegrityResult ( @Context HttpServletRequest request, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = webResource.init(params, true, request, true, null);
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
            JSONArray tabResponse = null;
            JSONObject errorContent = null;

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
    public Response discardConflicts ( @Context final HttpServletRequest request, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = webResource.init(params, true, request, true, null);
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
     * @param auth_token_enc
     * @param type
     * @return
     * @throws JSONException
     */
    @POST
    @Path("/fixconflictsfromremote/{params:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public Response fixConflictsFromRemote ( @Context final HttpServletRequest request,
                                             @FormDataParam("DATA_TO_FIX") InputStream dataToFix, @FormDataParam("AUTH_TOKEN") String auth_token_enc,
                                             @FormDataParam("TYPE") String type ) throws JSONException {

        String remoteIP = null;
        JSONObject jsonResponse = new JSONObject();
        IntegrityUtil integrityUtil = new IntegrityUtil();
        PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        PublishingEndPoint requesterEndPoint = null;
        try {
            String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
            remoteIP = request.getRemoteHost();
            if(!UtilMethods.isSet(remoteIP))
                remoteIP = request.getRemoteAddr();

            requesterEndPoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

            if(!BundlePublisherResource.isValidToken(auth_token, remoteIP, requesterEndPoint)) {
                return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
            }

            HibernateUtil.startTransaction();
            integrityUtil.fixConflicts(dataToFix, requesterEndPoint.getId(), IntegrityType.valueOf(type.toUpperCase()) );
            HibernateUtil.commitTransaction();
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
	 * @param request
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
    public Response fixConflicts ( @Context final HttpServletRequest request, @PathParam ("params") String params ) throws JSONException {

        InitDataObject initData = webResource.init(params, true, request, true, null);
        Map<String, String> paramsMap = initData.getParamsMap();
        JSONObject jsonResponse = new JSONObject();

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
            if(whereToFix.equals("local")) {

            	HibernateUtil.startTransaction();
                integrityUtil.fixConflicts(endpointId, integrityTypeToFix);
                HibernateUtil.commitTransaction();
                jsonResponse.put( "success", true );
                jsonResponse.put( "message", "Conflicts fixed in Local Endpoint" );

                IntegrityType[] types = IntegrityType.values();
                boolean isThereAnyConflict = false;
                // Check if we still have other conflicts
                for (IntegrityType integrityType : types) {
					if (!integrityType.equals(integrityTypeToFix)) {
	                	List<Map<String, Object>> results = integrityUtil.getIntegrityConflicts(endpointId, integrityType);
	                	if(!results.isEmpty()) {
	                		isThereAnyConflict = true;
	                		break;
	                	}
                	}
                }

                if(!isThereAnyConflict)
                	clearStatus( request, endpointId );

            } else  if(whereToFix.equals("remote")) {
                integrityUtil.generateDataToFixZip(endpointId, integrityTypeToFix);

                final Client client = RestClientBuilder.newClient();

                PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findEndPointById(endpointId);
                String outputPath = ConfigUtils.getIntegrityPath() + File.separator + endpointId;
                File bundle = new File(outputPath + File.separator + INTEGRITY_DATA_TO_FIX_ZIP_FILE_NAME);

                FormDataMultiPart form = new FormDataMultiPart();
                form.field("AUTH_TOKEN",
                        PushPublisher.retriveKeyString(
                                PublicEncryptionFactory.decryptString(endpoint.getAuthKey().toString())));

                form.field("TYPE", type);
                form.bodyPart(new FileDataBodyPart("DATA_TO_FIX", bundle, MediaType.MULTIPART_FORM_DATA_TYPE));
                String url = endpoint.toURL()+"/api/integrity/fixconflictsfromremote/";
                WebTarget webTarget = client.target(url);
                Response response = webTarget.request(MediaType.TEXT_PLAIN_TYPE).post(Entity.entity(form, form.getMediaType()));

                if(response.getStatus() == HttpStatus.SC_OK) {
                    jsonResponse.put( "success", true );
                    jsonResponse.put( "message", "Fix Conflicts Process successfully started at Remote." );
                    clearStatus( request, endpointId );
                } else {
					Logger.error(this.getClass(), "Response indicating a " + response.getStatusInfo().getReasonPhrase()
							+ " (" + response.getStatus() + ") Error trying to fix conflicts on " + whereToFix
							+ " end-point [" + endpointId + "].");
                    return Response.status( HttpStatus.SC_BAD_REQUEST ).entity("Endpoint with id: " + endpointId + " returned server error." ).build();
                }
            } else {
                return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: 'whereToFix' has an invalid value.").build();
            }

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
     * Removes the status for the checking integrity process of a given enpoint from session
     *
     * @param request
     * @param endpointId
     */
    private void clearStatus ( HttpServletRequest request, String endpointId ) {
        clearStatus( request.getSession(), endpointId );
    }

    /**
     * Removes the status for the checking integrity process of a given enpoint from session
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
     * Sets the status for the checking integrity process of a given enpoint in session
     *
     * @param request
     * @param endpointId
     * @param status
     */
    private void setStatus ( HttpServletRequest request, String endpointId, ProcessStatus status ) {
        setStatus( request, endpointId, status, null );
    }

    /**
     * Sets the status for the checking integrity process of a given enpoint in session
     *
     * @param request
     * @param endpointId
     * @param status
     * @param message
     */
    private void setStatus ( HttpServletRequest request, String endpointId, ProcessStatus status, String message ) {
        setStatus( request.getSession(), endpointId, status, message );
    }

    /**
     * Sets the status for the checking integrity process of a given enpoint in session
     *
     * @param session
     * @param endpointId
     * @param status
     * @param message
     */
    private void setStatus ( HttpSession session, String endpointId, ProcessStatus status, String message ) {
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
