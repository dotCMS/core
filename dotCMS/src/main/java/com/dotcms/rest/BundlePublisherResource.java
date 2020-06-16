package com.dotcms.rest;

import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.mock.request.HttpHeaderHandlerHttpServletRequestWrapper;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.pusher.PushPublisher;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.EncryptorException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
     * @param fileName        File name to be published
     * @param authTokenDigest Authentication token
     * @param groupId         Group who sent the Bundle
     * @param endpointId      End-point who sent the Bundle
	 * @param type			  response type
	 * @param callback 		  response callback
	 * @param bundleName	  The name for the Bundle to publish
	 * @param forcePush 	  true/false to Force the push
     * @param request         {@link HttpServletRequest}
	 * @param response        {@link HttpServletResponse}
     * @return Returns a {@link Response} object with a 200 status code if success or a 500 error code if anything fails on the Publish process
     * @see PublishThread
     */
    @POST
    @Path ("/publish")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public Response publish(
			@QueryParam("FILE_NAME")   final String fileName,
			@QueryParam("AUTH_TOKEN")  final String authTokenDigest,
			@QueryParam("GROUP_ID")    final String groupId,
			@QueryParam("ENDPOINT_ID") final String endpointId,
			@QueryParam("type")        final String type,
			@QueryParam("callback")    final String callback,
			@QueryParam("BUNDLE_NAME") final String bundleName,
			@QueryParam("FORCE_PUSH")  final boolean forcePush,
			@Context final HttpServletRequest  request,
			@Context final HttpServletResponse response
	) throws Exception {
		final ResourceResponse responseResource = new ResourceResponse(
				CollectionsUtils.map("type", type, "callback", callback));
		final String remoteIP = UtilMethods.isSet(request.getRemoteHost())?
				request.getRemoteHost() : request.getRemoteAddr();
		final PublishingEndPoint sendingEndPointByAddress =
				this.endpointAPI.findEnabledSendingEndPointByAddress(remoteIP);

		final boolean isPPByToken = sendingEndPointByAddress == null;

		if (isPPByToken && authTokenDigest == null) {
			Logger.error(this.getClass(), "Push Publishing failed from " + remoteIP + " JWT token expected");
			return responseResource.responseError(HttpStatus.SC_UNAUTHORIZED);
		}

		if (request.getInputStream().isFinished()) {
			Logger.error(this.getClass(), "Push Publishing failed from " + remoteIP + " bundle expected");
			return responseResource.responseError(HttpStatus.SC_BAD_REQUEST);
		}

		final InitDataObject initDataObject = this.init(authTokenDigest, request, response);

		if (isPPByToken && null == initDataObject || !this.isAdmin(initDataObject.getUser())) {
			Logger.error(this.getClass(), "Push Publishing failed from " + remoteIP + " not permission");
			return responseResource.responseError(HttpStatus.SC_UNAUTHORIZED);
		} else if (!isPPByToken &&
				(sendingEndPointByAddress == null || !isValidToken(authTokenDigest, remoteIP, sendingEndPointByAddress))) {
			Logger.error(this.getClass(), "Push Publishing failed from " + remoteIP + " invalid endpoint or token");
			return responseResource.responseError(HttpStatus.SC_UNAUTHORIZED);
		}

		final Bundle bundle = this.publishBundle(fileName, groupId, endpointId, bundleName,
				forcePush, request, remoteIP, sendingEndPointByAddress);

		if (isPPByToken && bundle != null) {
			return Response.ok((bundle)).build();
		} else {
			return Response.ok().build();
		}
	}


	final InitDataObject init (final String authTokenDigest, final HttpServletRequest  request,
							   final HttpServletResponse response) {

    	try {
			return new WebResource.InitBuilder().
					rejectWhenNoUser(false). // it would be a soft validation so not reject
					requestAndResponse(
					new HttpHeaderHandlerHttpServletRequestWrapper(request,
							CollectionsUtils.map(
									"Authorization", (name, value) -> // if the authorization is set, uses it, otherwise try with the secret (could be a jwt)
											UtilMethods.isSet(value) ? value : JsonWebTokenAuthCredentialProcessor.BEARER + authTokenDigest
							)), response).init();
		}catch (Exception e) {
    		return null;
		}

	}


	@WrapInTransaction
	private Bundle publishBundle(final String fileNameSent,
								  final String groupId,
								  final String endpointId,
								  final String bundleNameSent,
								  final boolean forcePush,
								  final HttpServletRequest request,
								  final String remoteIP,
								  final PublishingEndPoint sendingEndPointByAddress) throws Exception {

    	final String fileName = UtilMethods.isSet(fileNameSent) ? fileNameSent : generatedBundleFileName();
		final String bundleName =  UtilMethods.isSet(bundleNameSent) ? bundleNameSent : fileName;

		Bundle bundle = null;

		try (InputStream bundleStream = request.getInputStream()) {

			final String bundlePath         = ConfigUtils.getBundlePath()+ File.separator + MY_TEMP;
			final String bundleFolder       = fileName.substring(0, fileName.indexOf(".tar.gz"));
			final String sendingEndPoint = sendingEndPointByAddress != null ? sendingEndPointByAddress.getId() : remoteIP;
			final PublishAuditStatus status = PublishAuditAPI.getInstance().updateAuditTable(
					sendingEndPoint, sendingEndPoint, bundleFolder, true);

			if(bundleName.trim().length() > 0) {
				// save bundle if it doesn't exists
				bundle = APILocator.getBundleAPI().getBundleById(bundleFolder);
				if (bundle == null || bundle.getId() == null) {

					bundle = new Bundle();
					bundle.setId(bundleFolder);
					bundle.setName(bundleName);
					bundle.setPublishDate(Calendar.getInstance().getTime());
					bundle.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
					bundle.setForcePush(forcePush);
					APILocator.getBundleAPI().saveBundle(bundle);
				}
			}

			//Write file on FS
			FileUtil.writeToFile(bundleStream, bundlePath + fileName);

			//Start thread

			if(!status.getStatus().equals(Status.PUBLISHING_BUNDLE)) {
				DotConcurrentFactory.getInstance()
						.getSubmitter()
						.submit(new PublishThread(fileName, groupId, endpointId, status));
			}

			return bundle;
		} catch (Exception e) {

			Logger.error(
					PublisherQueueJob.class,
					String.format("Error caused by remote call of: Remote IP - %s, bundle name - %s, end point- %s",
							remoteIP, bundleNameSent,  endpointId));
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
			throw e;
		}
	}

	private String generatedBundleFileName() {
		return String.format("bundle_%d.tar.gz", System.currentTimeMillis());
	}

	private boolean isAdmin(final User user) {

    	return null != user && user.isBackendUser() && user.isAdmin();
	}

	/**
     * Validates a received token
     *
     * @param token    Token to validate
     * @param remoteIP Sender IP
     * @param publishingEndPoint   Current end point
     * @return True if valid
     * @throws IOException If fails reading the security token
     */
    public static boolean isValidToken (final String token,
										final String remoteIP,
										final PublishingEndPoint publishingEndPoint) throws IOException, EncryptorException {

        //My key
        final  Optional<String> endpointKeyDigest = PushPublisher.retriveEndpointKeyDigest(publishingEndPoint);
        return endpointKeyDigest.isPresent()? token.equals( endpointKeyDigest.get() ): false;
    }

}
