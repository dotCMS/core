package com.dotcms.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.httpclient.HttpStatus;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.integrity.IntegrityDataGeneratorThread;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.util.TrustFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.FormDataParam;
import com.sun.jersey.multipart.file.FileDataBodyPart;


@Path("/integrity")
public class IntegrityResource extends WebResource {

    public enum ProcessStatus {
        PROCESSING, ERROR, FINISHED
    }

    public enum IntegrityType {
	    FOLDERS("push_publish_integrity_folders_conflicts",
	    		"FoldersToCheck.csv",
	    		"FoldersToFix.csv"),

	    SCHEMES("push_publish_integrity_schemes_conflicts",
	    		"SchemesToCheck.csv",
	    		"SchemesToFix.csv"),

	    STRUCTURES("push_publish_integrity_structures_conflicts",
	    		"StructuresToCheck.csv",
	    		"StructuresToFix.csv");

	    private String label;
	    private String dataToCheckCSVName;
	    private String dataToFixCSVName;


	    IntegrityType(String label,String dataToCheckCSVName,String dataToFixCSVName) {
	    	this.label = label;
	    	this.dataToCheckCSVName = dataToCheckCSVName;
	    	this.dataToFixCSVName = dataToFixCSVName;
	    }

		public String getLabel() {
			return label;
		}

		public String getDataToCheckCSVName() {
			return dataToCheckCSVName;
		}

		public String getDataToFixCSVName() {
			return dataToFixCSVName;
		}

	}

    public static final String INTEGRITY_DATA_TO_CHECK_ZIP_FILE_NAME = "DataToCheck.zip";
    public static final String INTEGRITY_DATA_TO_FIX_ZIP_FILE_NAME = "DataToFix.zip";

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
        	IntegrityDataGeneratorThread idg = new IntegrityDataGeneratorThread(requesterEndPoint, request.getSession().getServletContext() );
        	idg.start();

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
							InputStream is = new FileInputStream(ConfigUtils.getIntegrityPath() + File.separator + requesterEndPoint.getId() + File.separator + INTEGRITY_DATA_TO_CHECK_ZIP_FILE_NAME);

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

	@GET
	@Path("/checkintegrity/{params:.*}")
	@Produces (MediaType.APPLICATION_JSON)
	public Response checkIntegrity(@Context HttpServletRequest request, @PathParam("params") String params)  {
		InitDataObject initData = init(params, true, request, true);

        Map<String, String> paramsMap = initData.getParamsMap();

        final HttpSession session = request.getSession();

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

        		session.setAttribute( "integrityCheck_" + endpointId, ProcessStatus.FINISHED );

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

        	final Client client = getRESTClient();

        	final PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findEndPointById(endpointId);
        	final String authToken = PushPublisher.retriveKeyString(PublicEncryptionFactory.decryptString(endpoint.getAuthKey().toString()));

        	FormDataMultiPart form = new FormDataMultiPart();
			form.field("AUTH_TOKEN",authToken);

        	//Sending bundle to endpoint
        	String url = endpoint.toURL()+"/api/integrity/generateintegritydata/";
        	com.sun.jersey.api.client.WebResource resource = client.resource(url);

        	ClientResponse response =
        			resource.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class, form);

        	if(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK) {
        		final String integrityDataRequestID = response.getEntity(String.class);

        		Thread integrityDataRequestChecker = new Thread() {
        			public void run(){

        				FormDataMultiPart form = new FormDataMultiPart();
        				form.field("AUTH_TOKEN",authToken);
        				form.field("REQUEST_ID",integrityDataRequestID);

        				String url = endpoint.toURL()+"/api/integrity/getintegritydata/";
        	        	com.sun.jersey.api.client.WebResource resource = client.resource(url);

        	        	boolean processing = true;

        	        	while(processing) {
        	        		ClientResponse response =
        	        				resource.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class, form);

        	        		if(response.getClientResponseStatus()!=null
        	        				&& response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK) {
        	        			processing = false;

        	        			InputStream zipFile = response.getEntityInputStream();


        	            		String outputDir = ConfigUtils.getIntegrityPath() + File.separator + endpoint.getId();

        	        			try {

        	        				IntegrityUtil.unzipFile(zipFile, outputDir);

        	        			} catch(Exception e) {
        	        				Logger.error(IntegrityResource.class, "Error while unzipping Integrity Data", e);
        	        				throw new RuntimeException("Error while unzipping Integrity Data", e);
        	        			}

        	        			// set session variable
        	        			// call IntegrityChecker
        	        			session.setAttribute( "integrityCheck_" + endpointId, ProcessStatus.PROCESSING );


        	        			try {
        	        				HibernateUtil.startTransaction();

        	        				IntegrityUtil integrityUtil = new IntegrityUtil();
        	        				integrityUtil.checkFoldersIntegrity(endpointId);
        	        				integrityUtil.checkStructuresIntegrity(endpointId);
        	        				integrityUtil.checkWorkflowSchemesIntegrity(endpointId);

        	        				HibernateUtil.commitTransaction();
        	        			} catch(Exception e) {
        	        				session.setAttribute( "integrityCheck_" + endpointId, ProcessStatus.ERROR);
        	        				Logger.error(IntegrityResource.class, "Error checking integrity", e);

        	        				try {
										HibernateUtil.rollbackTransaction();
									} catch (DotHibernateException e1) {
										Logger.error(IntegrityResource.class, "Error while rolling back transaction", e);
									}

        	        				throw new RuntimeException("Error checking integrity", e);
        	        			}


        	        			session.setAttribute( "integrityCheck_" + endpointId, ProcessStatus.FINISHED );


        	        		} else if(response.getClientResponseStatus()==null && response.getStatus()==102) {
        	        			continue;
        	        		}
        	        		else if(response.getClientResponseStatus()==null || response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
        	        			processing = false;
        	        		}
        	        	}


        		    }
        		};

        		integrityDataRequestChecker.start();
        		// call integrity checker process

        	}

        	 jsonResponse.put( "success", true );
             jsonResponse.put( "message", "Integrity Checking Initialized...");

        } catch(Exception e) {
        	Logger.error( this.getClass(), "Error initializing the integrity checking process for End Point server: [" + endpointId + "]", e );
        	return response( "Error initializing the integrity checking process for End Point server: [" + endpointId + "]" , true );
        }


        return response( jsonResponse.toString(), false );

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

        InitDataObject initData = init( params, true, request, true );
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
                } else {
                    jsonResponse.put( "status", "error" );
                    jsonResponse.put( "message", "Error checking the integrity process status for End Point server: [" + endpointId + "]" );
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

        InitDataObject initData = init( params, true, request, true );
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

            	switch (integrityType) {
				case STRUCTURES:
					columns.add("velocity_name");
					break;
				case FOLDERS:
					columns.add("folder");
					columns.add("host_name");
					break;
				case SCHEMES:
					columns.add("name");
					break;
				}

            	columns.add("local_inode");
				columns.add("remote_inode");

            	errorContent.put( "columns", columns.toArray() );

            	if(!results.isEmpty()) {
            		// the columns names are the keys in the results
            		isThereAnyConflict = isThereAnyConflict || true;

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
            	request.getSession().removeAttribute( "integrityCheck_" + endpointId);
            }

            /*
            ++++++++++++++++++++++++
            Important just in case of return custom errors
             */
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", "Success" );

            responseMessage.append( jsonResponse.toString() );

            //TODO: Clean up the session?
            //request.getSession().removeAttribute( "integrityCheck_" + endpointId );
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

        InitDataObject initData = init( params, true, request, true );
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

            responseMessage.append( jsonResponse.toString() );

        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error discarding "+type+" conflicts for End Point server: [" + endpointId + "]", e );
            return response( "Error discarding "+type+" conflicts for End Point server: [" + endpointId + "]" , true );
        }

        return response( responseMessage.toString(), false );
    }

    /**
     * Method that will fix the conflicts received from remote
     *
     * @param request
     * @param params
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

    	try {
    		String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
        	remoteIP = request.getRemoteHost();
        	if(!UtilMethods.isSet(remoteIP))
        		remoteIP = request.getRemoteAddr();

        	PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        	final PublishingEndPoint requesterEndPoint = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

        	if(!BundlePublisherResource.isValidToken(auth_token, remoteIP, requesterEndPoint)) {
        		return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
        	}

            IntegrityUtil integrityUtil = new IntegrityUtil();
            integrityUtil.fixConflicts(dataToFix, requesterEndPoint.getId(), IntegrityType.valueOf(type.toUpperCase()) );


        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error fixing "+type+" conflicts from remote", e );
            return response( "Error fixing "+type+" conflicts from remote" , true );
        }

    	jsonResponse.put( "success", true );
		jsonResponse.put( "message", "Conflicts fixed in Remote Endpoint" );
        return response( jsonResponse.toString(), false );



    }
    /**
     * Method that will fix the conflicts between local and remote.
     * If param 'whereToFix' == local, the fix will take place in local node
     * If param 'whereToFix' == remote, the fix will take place in remote node
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/fixconflicts/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response fixConflicts ( @Context final HttpServletRequest request, @PathParam ("params") String params ) throws JSONException {

        InitDataObject initData = init( params, true, request, true );
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



        try {

            IntegrityUtil integrityUtil = new IntegrityUtil();

            if(whereToFix.equals("local")) {

            	integrityUtil.fixConflicts(endpointId, IntegrityType.valueOf(type.toUpperCase()));
            	jsonResponse.put( "success", true );
        		jsonResponse.put( "message", "Conflicts fixed in Local Endpoint" );

            } else  if(whereToFix.equals("remote")) {
            	integrityUtil.generateDataToFixZip(endpointId, IntegrityType.valueOf(type.toUpperCase()));

            	final Client client = getRESTClient();

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
    			com.sun.jersey.api.client.WebResource resource = client.resource(url);

    			ClientResponse response = resource.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class, form);

    			if(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK) {
    				jsonResponse.put( "success", true );
            		jsonResponse.put( "message", "Fix Conflicts Process successfully started at Remote." );

            		integrityUtil.discardConflicts(endpointId, IntegrityType.valueOf(type.toUpperCase()));


    			} else {
            		return Response.status( HttpStatus.SC_BAD_REQUEST ).entity("Endpoint with id: " + endpointId + " returned server error." ).build();
        		}
            } else {
            	return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: 'whereToFix' has an invalid value.").build();
            }

        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error fixing "+type+" conflicts for End Point server: [" + endpointId + "]", e );
            return response( "Error fixing conflicts for endpoint: " + endpointId , true );
        }

        return response( jsonResponse.toString(), false );
    }

	private Client getRESTClient() {
		TrustFactory tFactory = new TrustFactory();
		ClientConfig cc = new DefaultClientConfig();

		if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals("")) {
			cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(tFactory.getHostnameVerifier(), tFactory.getSSLContext()));
		}
		final Client client = Client.create(cc);
		return client;
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