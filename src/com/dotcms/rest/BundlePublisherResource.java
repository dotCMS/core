package com.dotcms.rest;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

@Path("/bundlePublisher")
public class BundlePublisherResource extends WebResource {

	public static String MY_TEMP = "";
	private PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();

    /**
     * Method that receives from a server a bundle with the intention of publish it.<br/>
     * When a Bundle file is received on this end point is required to validate if the sending server is an allowed<br/>
     * server on this end point and if the security tokens match. If all the validations are correct the bundle will be add it<br/>
     * to the {@link PublishThread Publish Thread}.
     *
     * @param bundle         Bundle file stream
     * @param fileDetail     Bundle file Details
     * @param auth_token_enc Authentication token
     * @param groupId        Group who sent the Bundle
     * @param endpointId     End-point who sent the Bundle
     * @param req            HttpRequest
     * @return Returns a {@link Response} object with a 200 status code if success or a 500 error code if anything fails on the Publish process
     * @see PublishThread
     */
	@POST
	@Path("/publish")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response publish(
			@FormDataParam("bundle") InputStream bundle,
			@FormDataParam("bundle") FormDataContentDisposition fileDetail,
			@FormDataParam("AUTH_TOKEN") String auth_token_enc,
			@FormDataParam("GROUP_ID") String groupId,
			@FormDataParam("ENDPOINT_ID") String endpointId,
			@FormDataParam("BUNDLE_NAME") String bundleName,
			@Context HttpServletRequest req) {

		String remoteIP = "";
		try {
			String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
			remoteIP = req.getRemoteHost();
			if(!UtilMethods.isSet(remoteIP))
				remoteIP = req.getRemoteAddr();
			
			HibernateUtil.startTransaction();
			
			PublishingEndPoint mySelf = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);
			
			if(!isValidToken(auth_token, remoteIP, mySelf)) {
				bundle.close();
				return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
			}
			
			String bundlePath = ConfigUtils.getBundlePath()+File.separator+MY_TEMP;
			String fileName=fileDetail.getFileName();
			String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));
			
			PublishAuditStatus status = PublishAuditAPI.getInstance().updateAuditTable(endpointId, groupId, bundleFolder);
			
			if(bundleName.trim().length()>0) {
			    // save bundle if it doesn't exists
			    if(APILocator.getBundleAPI().getBundleById(bundleFolder)!=null) {
                    Bundle b = new Bundle();
                    b.setId(bundleFolder);
                    b.setName(bundleName);
                    b.setPublishDate(Calendar.getInstance().getTime());
                    b.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
                    APILocator.getBundleAPI().saveBundle(b);
			    }
			}
			
			//Write file on FS
			FileUtil.writeToFile(bundle, bundlePath+fileName);
			
			//Start thread
			if(!status.getStatus().equals(Status.PUBLISHING_BUNDLE)) {
				new Thread(new PublishThread(fileName, groupId, endpointId, status)).start();
			}
			
			HibernateUtil.commitTransaction();
			
			return Response.status(HttpStatus.SC_OK).build();
		} catch (NumberFormatException e) {
		    try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(this, "error rollback",e1);
            }
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (Exception e) {
		    try {
                HibernateUtil.rollbackTransaction();
            } catch (DotHibernateException e1) {
                Logger.error(this, "error rollback",e1);
            }
			Logger.error(PublisherQueueJob.class, "Error caused by remote call of: "+remoteIP);
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		}
		finally {
		    try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.error(this, "error close session",e);
            }
		}
		
		return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
	}
	
    /**
     * Validates a received token
     *
     * @param token    Token to validate
     * @param remoteIP Sender IP
     * @param mySelf   Current end point
     * @return True if valid
     * @throws IOException If fails reading the security token
     */
    private boolean isValidToken ( String token, String remoteIP, PublishingEndPoint mySelf ) throws IOException {
		
		//My key
        String myKey;
		if(mySelf != null) {
            myKey = retrieveKeyString( PublicEncryptionFactory.decryptString( mySelf.getAuthKey().toString() ) );
		} else {
			return false;
		}
		
        return token.equals( myKey );
	}
	
    private String retrieveKeyString ( String token ) throws IOException {

		String key = null;
		if(token.contains(File.separator)) {
			File tokenFile = new File(token);
            if ( tokenFile != null && tokenFile.exists() ) {
				key = FileUtils.readFileToString(tokenFile, "UTF-8").trim();
            }
		} else {
			key = token;
		}
		
		return key;
	}

}
