package com.dotcms.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;

import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/bundlePublisher")
public class BundlePublisherResource extends WebResource {
	public static String MY_TEMP = "";
	private PublishingEndPointAPI endpointAPI = APILocator.getPublisherEndPointAPI();
	private PublishAuditAPI auditAPI = PublishAuditAPI.getInstance();

	@POST
	@Path("/publish")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response publish(
			@FormDataParam("bundle") InputStream bundle,
			@FormDataParam("bundle") FormDataContentDisposition fileDetail,
			@FormDataParam("AUTH_TOKEN") String auth_token_enc,
			@FormDataParam("GROUP_ID") String groupId,
			@FormDataParam("ENDPOINT_ID") String endpointId,
			@Context HttpServletRequest req) {
		String remoteIP = "";
		try {
			String auth_token = PublicEncryptionFactory.decryptString(auth_token_enc);
			remoteIP = req.getRemoteHost();
			if(!UtilMethods.isSet(remoteIP))
				remoteIP = req.getRemoteAddr();
			
			PublishingEndPoint mySelf = endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);
			
			if(!isValidToken(auth_token, remoteIP, mySelf)) {
				bundle.close();
				return Response.status(HttpStatus.SC_UNAUTHORIZED).build();
			}
			
			String bundleName = fileDetail.getFileName();
			String bundlePath = ConfigUtils.getBundlePath()+File.separator+MY_TEMP;
			String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
			
			PublishAuditStatus status = PublishAuditAPI.getInstance().updateAuditTable(endpointId, groupId, bundleFolder);
			
			//Write file on FS
			FileUtil.writeToFile(bundle, bundlePath+bundleName);
			
			
			//Start thread
			if(!status.getStatus().equals(Status.PUBLISHING_BUNDLE)) {
				new Thread(new PublishThread(bundleName, groupId, endpointId, status)).start();
			}
			
			return Response.status(HttpStatus.SC_OK).build();
		} catch (NumberFormatException e) {
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		} catch (Exception e) {
			Logger.error(PublisherQueueJob.class, "Error caused by remote call of: "+remoteIP);
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
		}
		
		return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
	}
	
	private boolean isValidToken(String token, String remoteIP, PublishingEndPoint mySelf) throws IOException, DotDataException {
		String clientKey = token;
		
		//My key
		String myKey = null;
		if(mySelf != null) {
			myKey = retriveKeyString(
					PublicEncryptionFactory.decryptString(mySelf.getAuthKey().toString()));
		} else {
			return false;
		}
		
		
		return clientKey.equals(myKey);
			
	}
	
	private String retriveKeyString(String token) throws IOException {
		String key = null;
		if(token.contains(File.separator)) {
			File tokenFile = new File(token);
			if(tokenFile != null && tokenFile.exists())
				key = FileUtils.readFileToString(tokenFile, "UTF-8").trim();
		} else {
			key = token;
		}
		
		return key;
	}

	
	
}
