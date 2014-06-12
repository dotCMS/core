package com.dotcms.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.httpclient.HttpStatus;

import com.csvreader.CsvWriter;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherQueueJob;
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
import com.liferay.portal.model.User;
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

	@GET
	@Path("/getdata/{params:.*}")
	@Produces("application/zip")
	public Response getData(@Context HttpServletRequest request, @PathParam("params") String params)  {
		InitDataObject initData = init(params, true, request, true);

        Map<String, String> paramsMap = initData.getParamsMap();
        User user = initData.getUser();
        String auth_token_enc = paramsMap.get("authtoken");

        String remoteIP = null;

        try {

        	String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
        	remoteIP = request.getRemoteHost();
        	if(!UtilMethods.isSet(remoteIP))
        		remoteIP = request.getRemoteAddr();

        	HibernateUtil.startTransaction();

        	PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
        	final PublishingEndPoint mySelf = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

        	if(!BundlePublisherResource.isValidToken(auth_token, remoteIP, mySelf)) {
        		return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
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
        	Logger.error(PublisherQueueJob.class, "Error caused by remote call of: "+remoteIP);
        	Logger.error(PublisherQueueJob.class,e.getMessage(),e);
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
	@Path("/getdata/{params:.*}")
	@Produces("application/zip")
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

        	FormDataMultiPart form = new FormDataMultiPart();
        	form.field("AUTH_TOKEN",
        			PushPublisher.retriveKeyString(
        					PublicEncryptionFactory.decryptString(endpoint.getAuthKey().toString())));

        	//Sending bundle to endpoint
        	com.sun.jersey.api.client.WebResource resource = client.resource(endpoint.toURL()+"/api/integrity/getdata");

        	ClientResponse response = resource.accept("application/zip").get(ClientResponse.class);

        	if(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK) {

        		InputStream zipFile = response.getEntityInputStream();
        		ZipInputStream zin = new ZipInputStream(zipFile);

        		ZipEntry ze = null;
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

//        		FileOutputStream fos = new FileOutputStream(ConfigUtils.getIntegrityPath() + File.separator + mySelf.getId() + File.separator + "integrity.zip");

			}




        } catch(Exception e) {

        }


        return Response.ok("").build();

	}


}