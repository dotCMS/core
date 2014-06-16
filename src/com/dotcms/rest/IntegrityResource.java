package com.dotcms.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.dotcms.publisher.integrity.IntegrityDataGenerator;
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
import com.liferay.portal.model.User;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.FormDataParam;


@Path("/integrity")
public class IntegrityResource extends WebResource {

    public enum ProcessStatus {
        PROCESSING, ERROR, FINISHED
    }

    public enum IntegrityType {
	    FOLDERS,
	    SCHEMES,
	    STRUCTURES;
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

//        String auth_token_enc = paramsMap.get("authtoken");
        String remoteIP = null;

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

        	ServletContext servletContext = request.getSession().getServletContext();

        	if(servletContext.getAttribute("integrityRunning")!=null && ((Boolean) servletContext.getAttribute("integrityRunning"))) {
        		throw new WebApplicationException(Response.status(HttpStatus.SC_CONFLICT).entity("Already Running").build());
        	}

        	String transactionId = UUIDGenerator.generateUuid();
        	servletContext.setAttribute("integrityDataRequestID", transactionId);

        	// start data generation process
        	IntegrityDataGenerator idg = new IntegrityDataGenerator(requesterEndPoint, request.getSession().getServletContext() );
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
							InputStream is = new FileInputStream(ConfigUtils.getIntegrityPath() + File.separator + requesterEndPoint.getId() + File.separator + "integrity.zip");

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

		return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity("Unknown Status").build();

	}






	@GET
	@Path("/checkintegrity/{params:.*}")
	@Produces (MediaType.APPLICATION_JSON)
	public Response checkIntegrity(@Context HttpServletRequest request, @PathParam("params") String params)  {
		InitDataObject initData = init(params, true, request, true);

        Map<String, String> paramsMap = initData.getParamsMap();
        User user = initData.getUser();

        StringBuilder responseMessage = new StringBuilder();
        final HttpSession session = request.getSession();

        //Validate the parameters
        final String endpointId = paramsMap.get( "endpoint" );
        if ( !UtilMethods.isSet( endpointId ) ) {
            return Response.status( HttpStatus.SC_BAD_REQUEST ).entity( "Error: endpoint is a required Field.").build();
        }

        try {
			IntegrityUtil.getIntegrityConflicts(endpointId, IntegrityType.FOLDERS);
		} catch (Exception e) {
			Logger.info(IntegrityResource.class, "IntegrityResource");
		}

        try {

        	TrustFactory tFactory = new TrustFactory();
        	ClientConfig cc = new DefaultClientConfig();

        	if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals("")) {
        		cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(tFactory.getHostnameVerifier(), tFactory.getSSLContext()));
        	}
        	final Client client = Client.create(cc);

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
        	            		ZipInputStream zin = new ZipInputStream(zipFile);

        	            		ZipEntry ze = null;

        	            		File dir = new File(ConfigUtils.getIntegrityPath() + File.separator + endpoint.getId());

        	        			// if file doesnt exists, then create it
        	        			if (!dir.exists()) {
        	        				dir.mkdir();
        	        			}

        	        			try {

        	        				while ((ze = zin.getNextEntry()) != null) {
        	        					System.out.println("Unzipping " + ze.getName());

        	        					FileOutputStream fout = new FileOutputStream(ConfigUtils.getIntegrityPath() + File.separator + endpoint.getId() + File.separator +ze.getName());
        	        					for (int c = zin.read(); c != -1; c = zin.read()) {
        	        						fout.write(c);
        	        					}
        	        					zin.closeEntry();
        	        					fout.close();
        	        				}
        	        				zin.close();

        	        			} catch(IOException e) {
        	        				Logger.error(IntegrityResource.class, "Error while unzipping Integrity Data", e);
        	        				throw new RuntimeException("Error while unzipping Integrity Data", e);
        	        			}

        	        			// set session variable
        	        			// call IntegrityChecker
        	        			session.setAttribute( "integrityCheck_" + endpointId, ProcessStatus.PROCESSING );


        	        			try {
        	        				HibernateUtil.startTransaction();
        	        				IntegrityUtil.checkFoldersIntegrity(endpointId);
        	        				IntegrityUtil.checkStructuresIntegrity(endpointId);
        	        				IntegrityUtil.checkWorkflowSchemesIntegrity(endpointId);
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


        	        		} else if(response.getClientResponseStatus()==null || response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
        	        			processing = false;
        	        		}
        	        	}


        		    }
        		};

        		integrityDataRequestChecker.start();
        		// call integrity checker process

        	}

        	JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", "Initialized integrity checking..." );

            responseMessage.append( jsonResponse.toString() );

        } catch(Exception e) {
        	Logger.error(IntegrityResource.class,e.getMessage(),e);
        }

        return response( responseMessage.toString(), false );

	}

    /**
     * Initializes the check integrity process against a given server
     *
     * @param request
     * @param params
     * @return
     * @throws JSONException
     */
    @GET
    @Path ("/checkIntegrityExample/{params:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response checkIntegrityExample ( @Context final HttpServletRequest request, @PathParam ("params") String params ) throws JSONException {

        StringBuilder responseMessage = new StringBuilder();

        InitDataObject initData = init( params, true, request, true );
        Map<String, String> paramsMap = initData.getParamsMap();

        //Validate the parameters
        final String endpointId = paramsMap.get( "endpoint" );
        if ( !UtilMethods.isSet( endpointId ) ) {
            Response.ResponseBuilder responseBuilder = Response.status( HttpStatus.SC_BAD_REQUEST );
            responseBuilder.entity( responseMessage.append( "Error: " ).append( "endpoint" ).append( " is a required Field." ) );

            return responseBuilder.build();
        }

        try {

            final HttpSession session = request.getSession();

            //SOME MAGIC HERE!!!
            //SOME MAGIC HERE!!!
            //SOME MAGIC HERE!!!
            //SOME MAGIC HERE!!!

            session.setAttribute( "integrityCheck_" + endpointId, ProcessStatus.PROCESSING );

            //**********************************
            //**********************************
            //Mark the process as finised after 2 minutes
            int delay = 120000;// in ms = two minutes
            Timer timer = new Timer();
            timer.schedule( new TimerTask(){
                public void run() {
                    session.setAttribute( "integrityCheck_" + endpointId, ProcessStatus.FINISHED );
                }
            }, delay);
            //**********************************
            //**********************************

            //And prepare the response
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put( "success", true );
            jsonResponse.put( "message", "Initialized integrity checking..." );

            responseMessage.append( jsonResponse.toString() );

        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error initializing the integrity checking process for End Point server: [" + endpointId + "]", e );

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error initializing the integrity checking process for End Point server: [" + endpointId + "]" );
            }
            return response( responseMessage.toString(), true );
        }

        return response( responseMessage.toString(), false );
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

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error checking the integrity process status for End Point server: [" + endpointId + "]" );
            }
            return response( responseMessage.toString(), true );
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

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Structures tab data
            JSONArray tabResponse = new JSONArray();
            JSONObject errorContent = new JSONObject();

            IntegrityType[] types = IntegrityType.values();

            for (IntegrityType integrityType : types) {
            	errorContent.put( "title", integrityType.name() + " Inode Conflicts" );//Title of the check
            	List<Map<String, Object>> results = IntegrityUtil.getIntegrityConflicts(endpointId, integrityType);

            	if(!results.isEmpty()) {
            		// the columns names are the keys in the results
	            	JSONArray columns = new JSONArray();
	            	for (String keyName : results.get(0).keySet()) {
						columns.add(keyName);
					}

	            	JSONArray values = new JSONArray();

	            	for (Map<String, Object> result : results) {

	            		JSONObject columnsContent = new JSONObject();

	            		for (String keyName : result.keySet()) {
	            			columnsContent.put("keyName", result.get(keyName));
						}

	            		values.add(columnsContent);
					}

	            	errorContent.put( "values", values.toArray() );
            	}

            	tabResponse.add( errorContent );
                //And prepare the response
                jsonResponse.put( integrityType.name(), tabResponse.toArray() );
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

            if ( e.getMessage() != null ) {
                responseMessage.append( e.getMessage() );
            } else {
                responseMessage.append( "Error generating the integrity result for End Point server: [" + endpointId + "]" );
            }
            return response( responseMessage.toString(), true );
        }

        return response( responseMessage.toString(), false );
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

        Response.ResponseBuilder responseBuilder;
        if ( error ) {
            responseBuilder = Response.status( HttpStatus.SC_INTERNAL_SERVER_ERROR );
            responseBuilder.entity( response );
        } else {
            responseBuilder = Response.ok( response, contentType );
        }

        return responseBuilder.build();
    }

    /**
     * Validates a Collection or string parameters.
     *
     * @param paramsMap
     * @param responseMessage
     * @param args
     * @return True if all the params are present, false otherwise
     * @throws JSONException
     */
    private Boolean validate ( Map<String, String> paramsMap, StringBuilder responseMessage, String... args ) throws JSONException {

        for ( String param : args ) {

            //Validate the given param
            if ( !UtilMethods.isSet( paramsMap.get( param ) ) ) {

                //Prepare a proper response
                responseMessage.append( "Error: " ).append( param ).append( " is a required Field." );
                return false;
            }
        }

        return true;
    }

}