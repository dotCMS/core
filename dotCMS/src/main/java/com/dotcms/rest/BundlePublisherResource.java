package com.dotcms.rest;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.QueryParam;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Path("/bundlePublisher")
public class BundlePublisherResource {

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
    @Path ("/publish")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response publish(
			@QueryParam("FILE_NAME") String fileName,
			@QueryParam("AUTH_TOKEN") String auth_token_enc,
			@QueryParam("GROUP_ID") String groupId,
			@QueryParam("ENDPOINT_ID") String endpointId,
			@QueryParam("type") String type,
			@QueryParam("callback") String callback,
			@QueryParam("BUNDLE_NAME") String bundleName,
			@QueryParam("FORCE_PUSH") final boolean forcePush,
			@Context HttpServletRequest req
	) {
    	try {
    		try (InputStream bundleStream = req.getInputStream()) {
		        //Creating an utility response object
		        Map<String, String> paramsMap = new HashMap<String, String>();
		        paramsMap.put( "type", type );
		        paramsMap.put( "callback", callback );
		        ResourceResponse responseResource = new ResourceResponse( paramsMap );

				String remoteIP = "";
				try {
					String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
					remoteIP = req.getRemoteHost();
					if(!UtilMethods.isSet(remoteIP))
						remoteIP = req.getRemoteAddr();

					HibernateUtil.startTransaction();

					PublishingEndPoint mySelf = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

					if(!isValidToken(auth_token, remoteIP, mySelf)) {
						bundleStream.close();
		                return responseResource.responseError( HttpStatus.SC_UNAUTHORIZED );
		            }

					String bundlePath = ConfigUtils.getBundlePath()+File.separator+MY_TEMP;
					String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));

		            PublishAuditStatus status = PublishAuditAPI.getInstance().updateAuditTable( mySelf.getId(), mySelf.getId(), bundleFolder, true );

		            if(bundleName.trim().length()>0) {
					    // save bundle if it doesn't exists
		                Bundle foundBundle = APILocator.getBundleAPI().getBundleById( bundleFolder );
		                if ( foundBundle == null || foundBundle.getId() == null ) {
		                    Bundle bundle = new Bundle();
							bundle.setId(bundleFolder);
							bundle.setName(bundleName);
							bundle.setPublishDate(Calendar.getInstance().getTime());
							bundle.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
							bundle.setForcePush(forcePush);
		                    APILocator.getBundleAPI().saveBundle(bundle);
					    }
					}

					//Write file on FS
					FileUtil.writeToFile(bundleStream, bundlePath+fileName);

					//Start thread
					if(!status.getStatus().equals(Status.PUBLISHING_BUNDLE)) {
						new Thread(new PublishThread(fileName, groupId, endpointId, status)).start();
					}

					HibernateUtil.closeAndCommitTransaction();

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
    		}
    	} catch (IOException e) {
    		Logger.error(PublisherQueueJob.class,e.getMessage(),e);
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
    public static boolean isValidToken ( String token, String remoteIP, PublishingEndPoint mySelf ) throws IOException {

        //My key
        String myKey;
        if ( mySelf != null ) {
            myKey = retrieveKeyString( PublicEncryptionFactory.decryptString( mySelf.getAuthKey().toString() ) );
        } else {
            return false;
        }

        return token.equals( myKey );
    }

    public static String retrieveKeyString ( String token ) throws IOException {

        String key = null;
        if ( token.contains( File.separator ) ) {
            File tokenFile = new File( token );
            if ( tokenFile != null && tokenFile.exists() ) {
                key = FileUtils.readFileToString( tokenFile, "UTF-8" ).trim();
            }
        } else {
            key = token;
        }

        return key;
    }

}
