package com.dotcms.rest;

import com.csvreader.CsvWriter;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.util.TrustFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
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
import org.apache.commons.httpclient.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


@Path("/integrity")
public class IntegrityResource extends WebResource {

    private enum ProcessStatus {
        PROCESSING, ERROR, FINISHED
    }

	public enum IntegrityType {
	    FOLDERS("folders"),
	    WORKFLOW_SCHEMES("workflow_schemes"),
	    STRUCTURES("structures");

	    private final String fileName;

	    IntegrityType(String fileName) {
	    	this.fileName = fileName;
	    }

	    public String getFileName() {
	    	return fileName;
	    }
	}

	/**
	 * <p>Returns a zip with data from structures, workflow schemes and folders for integrity check
	 *
	 * Usage: /getdata
	 *
	 */

	@POST
	@Path("/getdata/{params:.*}")
	@Produces("application/zip")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response getData(@Context HttpServletRequest request, @FormDataParam("AUTH_TOKEN") String auth_token_enc)  {

//        String auth_token_enc = paramsMap.get("authtoken");



        String remoteIP = null;

        try {

        	auth_token_enc = URLDecoder.decode(auth_token_enc, "UTF-8");
        	String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
        	remoteIP = request.getRemoteHost();
        	if(!UtilMethods.isSet(remoteIP))
        		remoteIP = request.getRemoteAddr();

        	PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        	final PublishingEndPoint mySelf = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

        	if(!BundlePublisherResource.isValidToken(auth_token, remoteIP, mySelf)) {
        		return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
        	}

        	File dir = new File(ConfigUtils.getIntegrityPath() + File.separator + mySelf.getId());

			// if file doesnt exists, then create it
			if (!dir.exists()) {
				dir.mkdir();
			}

        	FileOutputStream fos = new FileOutputStream(ConfigUtils.getIntegrityPath() + File.separator + mySelf.getId() + File.separator + "integrity.zip");
        	ZipOutputStream zos = new ZipOutputStream(fos);

        	// create Folders CSV
        	File foldersCsvFile = createCSV(IntegrityType.FOLDERS);
        	File structuresCsvFile = createCSV(IntegrityType.STRUCTURES);
        	File schemesCsvFile = createCSV(IntegrityType.WORKFLOW_SCHEMES);

        	addToZipFile(foldersCsvFile.getAbsolutePath(), zos, "folders.csv");
        	addToZipFile(structuresCsvFile.getAbsolutePath(), zos, "structures.csv");
        	addToZipFile(schemesCsvFile.getAbsolutePath(), zos, "workflow_schemes.csv");

        	zos.close();
        	fos.close();

        	foldersCsvFile.delete();

        	StreamingOutput output = new StreamingOutput() {
        		public void write(OutputStream output) throws IOException, WebApplicationException {
        			try {
        				InputStream is = new FileInputStream(ConfigUtils.getIntegrityPath() + File.separator + mySelf.getId() + File.separator + "integrity.zip");

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

        			} catch (Exception e) {
        				throw new WebApplicationException(e);
        			}
        		}
        	};

        	return Response.ok(output).build();

        } catch (Exception e) {
        	Logger.error(IntegrityResource.class, "Error caused by remote call of: "+remoteIP);
        	Logger.error(IntegrityResource.class,e.getMessage(),e);
        } finally {
        	try {
        		HibernateUtil.closeSession();
        	} catch (DotHibernateException e) {
        		Logger.error(this, "error close session",e);
        	}
        }


        return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();

	}

	private static File createCSV(IntegrityType type) throws Exception {

		String outputFile = ConfigUtils.getBundlePath() + File.separator + type.getFileName() + ".csv";
        File csvFile = new File(outputFile);

		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(new FileWriter(csvFile, true), '|');

			if(type == IntegrityType.FOLDERS) {
				IntegrityUtil.writeFoldersCSV(csvOutput);
			} else if(type == IntegrityType.STRUCTURES) {
				IntegrityUtil.writeStructuresCSV(csvOutput);
			}  else if(type == IntegrityType.WORKFLOW_SCHEMES) {
				IntegrityUtil.writeFoldersCSV(csvOutput);
			}

			csvOutput.close();
		} catch (IOException e) {
			Logger.error(IntegrityResource.class, "Error writing csv: " + type.name(), e);
			throw new Exception("Error writing csv: " + type.name(), e);
		} catch (DotDataException e) {
			Logger.error(IntegrityResource.class, "Error getting data from DB for: " + type.name(), e);
			throw new Exception("Error getting data from DB for: " + type.name(), e);
		}

		return csvFile;
	}

	private static void addToZipFile(String fileName, ZipOutputStream zos, String zipEntryName) throws Exception  {

		System.out.println("Writing '" + fileName + "' to zip file");

		try {

			File file = new File(fileName);
			FileInputStream fis = new FileInputStream(file);
			ZipEntry zipEntry = new ZipEntry(zipEntryName);
			zos.putNextEntry(zipEntry);

			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zos.write(bytes, 0, length);
			}

			zos.closeEntry();
			fis.close();

		} catch(FileNotFoundException f){
			Logger.error(IntegrityResource.class, "Could not find file " + fileName, f);
			throw new Exception("Could not find file " + fileName, f);
		} catch (IOException e) {
			Logger.error(IntegrityResource.class, "Error writing file to zip: " + fileName, e);
			throw new Exception("Error writing file to zip: " + fileName, e);
		}
	}


	@GET
	@Path("/checkintegrity/{params:.*}")
	@Produces("application/json")
	public Response checkIntegrity(@Context HttpServletRequest request, @PathParam("params") String params)  {
		InitDataObject initData = init(params, true, request, true);

        Map<String, String> paramsMap = initData.getParamsMap();
        User user = initData.getUser();

        String endpointId = paramsMap.get("endpointid");

        if(!UtilMethods.isSet(endpointId)) {
        	return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
        }

        try {

        	TrustFactory tFactory = new TrustFactory();

        	ClientConfig cc = new DefaultClientConfig();

        	if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals("")) {
        		cc.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(tFactory.getHostnameVerifier(), tFactory.getSSLContext()));
        	}
        	Client client = Client.create(cc);

        	PublishingEndPoint endpoint = APILocator.getPublisherEndPointAPI().findEndPointById(endpointId);

        	String authToken = PushPublisher.retriveKeyString(PublicEncryptionFactory.decryptString(endpoint.getAuthKey().toString()));

        	authToken = URLEncoder.encode(authToken, "UTF-8");

        	FormDataMultiPart form = new FormDataMultiPart();
			form.field("AUTH_TOKEN",authToken);

        	//Sending bundle to endpoint
        	String u = endpoint.toURL()+"/api/integrity/getdata/";

        	com.sun.jersey.api.client.WebResource resource = client.resource(u);

        	ClientResponse response =
        			resource.accept("application/zip").type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class, form);

        	System.out.println(response);

        	if(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK) {

        		InputStream zipFile = response.getEntityInputStream();
        		ZipInputStream zin = new ZipInputStream(zipFile);

        		ZipEntry ze = null;

        		File dir = new File(ConfigUtils.getIntegrityPath() + File.separator + endpoint.getId());

    			// if file doesnt exists, then create it
    			if (!dir.exists()) {
    				dir.mkdir();
    			}

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




        	}




        } catch(Exception e) {
        	Logger.error(IntegrityResource.class,e.getMessage(),e);
        }


        return Response.ok("").build();

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

            //Title --> "title":"XXXX YYYYYY"
            errorContent.put( "title", "Structure Inode error" );//Title of the check

            //Columns names --> "columns":["Column0","Column1","Column2","Column3"]
            JSONArray columns = new JSONArray();
            columns.add( "inode" );
            columns.add( "velocityName" );
            errorContent.put( "columns", columns.toArray() );

            //Values --> "values":[{"Column0":"value0","Column1":"value1","Column2":"value2","Column3":"value3"},{"Column0":"value0","Column1":"value1","Column2":"value2","Column3":"value3"}]
            JSONArray values = new JSONArray();

            JSONObject columnsContent = new JSONObject();
            columnsContent.put( "inode", "6546-5646-56464-54654" );
            columnsContent.put( "velocityName", "myCustomVarName" );
            values.put( columnsContent );

            columnsContent = new JSONObject();
            columnsContent.put( "inode", "6546-5646-YYYYY-XXXXX" );
            columnsContent.put( "velocityName", "myCustomVarName2" );
            values.put( columnsContent );

            columnsContent = new JSONObject();
            columnsContent.put( "inode", "6546-WWWW-YYYYY-XXXXX" );
            columnsContent.put( "velocityName", "myCustomVarName3" );
            values.put( columnsContent );

            errorContent.put( "values", values.toArray() );

            tabResponse.add( errorContent );
            //And prepare the response
            jsonResponse.put( "structures", tabResponse.toArray() );

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Folders tab data
            tabResponse = new JSONArray();
            errorContent = new JSONObject();

            //Title --> "title":"XXXX YYYYYY"
            errorContent.put( "title", "Folders Inode error" );//Title of the check

            //Columns names --> "columns":["Column0","Column1","Column2","Column3"]
            columns = new JSONArray();
            columns.add( "inode" );
            columns.add( "parent_path" );
            columns.add( "name" );
            columns.add( "host" );
            errorContent.put( "columns", columns.toArray() );

            //Values --> "values":[{"Column0":"value0","Column1":"value1","Column2":"value2","Column3":"value3"},{"Column0":"value0","Column1":"value1","Column2":"value2","Column3":"value3"}]
            values = new JSONArray();

            columnsContent = new JSONObject();
            columnsContent.put( "inode", "6546-5646-56464-54654" );
            columnsContent.put( "parent_path", "/" );
            columnsContent.put( "name", "myFolder" );
            columnsContent.put( "host", "demo.dotcms.com" );
            values.put( columnsContent );

            columnsContent = new JSONObject();
            columnsContent.put( "inode", "6546-XXXX-56464-YYYY" );
            columnsContent.put( "parent_path", "/" );
            columnsContent.put( "name", "myFolder2" );
            columnsContent.put( "host", "demo.dotcms.com" );
            values.put( columnsContent );

            errorContent.put( "values", values.toArray() );

            tabResponse.add( errorContent );
            //And prepare the response
            jsonResponse.put( "folders", tabResponse.toArray() );

            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //Workflows tab data
            tabResponse = new JSONArray();
            errorContent = new JSONObject();

            //Title --> "title":"XXXX YYYYYY"
            errorContent.put( "title", "Workflows Inode error" );//Title of the check

            //Columns names --> "columns":["Column0","Column1","Column2","Column3"]
            columns = new JSONArray();
            columns.add( "inode" );
            columns.add( "name" );
            errorContent.put( "columns", columns.toArray() );

            //Values --> "values":[{"Column0":"value0","Column1":"value1","Column2":"value2","Column3":"value3"},{"Column0":"value0","Column1":"value1","Column2":"value2","Column3":"value3"}]
            values = new JSONArray();

            columnsContent = new JSONObject();
            columnsContent.put( "inode", "6546-5646-56464-54654" );
            columnsContent.put( "name", "customWorflow1" );
            values.put( columnsContent );

            columnsContent = new JSONObject();
            columnsContent.put( "inode", "6546-xxxx-yyyyyy-54654" );
            columnsContent.put( "name", "customWorflow2" );
            values.put( columnsContent );

            errorContent.put( "values", values.toArray() );

            tabResponse.add( errorContent );
            //And prepare the response
            jsonResponse.put( "workflows", tabResponse.toArray() );

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