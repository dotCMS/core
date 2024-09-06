package com.dotcms.rest;


import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/bundlePublisher")
public class BundlePublisherResource {

	public static String MY_TEMP = "";

	/**
	 * Method that receives from a server a bundle with the intention of publish it.<br/>
	 * When a Bundle file is received on this end point is required to validate if the sending server is an allowed<br/>
	 * server on this end point and if the security tokens match. If all the validations are correct the bundle will be add it<br/>
	 * to the {@link PushPublisherJob Publish Thread}.
	 *
	 * @param type			  response type
	 * @param callback 		  response callback
	 * @param forcePush 	  true/false to Force the push
	 * @param request         {@link HttpServletRequest}
	 * @param response        {@link HttpServletResponse}
	 * @return Returns a {@link Response} object with a 200 status code if success or a 500 error code if anything fails on the Publish process
	 * @see PushPublisherJob
	 */
	@POST
	@Path("/publish")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public Response publish(
			@QueryParam("type")        final String type,
			@QueryParam("callback")    final String callback,
			@QueryParam("FORCE_PUSH")  final boolean forcePush,
			@Context final HttpServletRequest  request,
			@Context final HttpServletResponse response
	) throws Exception {

		if (LicenseManager.getInstance().isCommunity()) {
			throw new InvalidLicenseException("License required");
		}

		final Map<String, String> paramsMap = new HashMap<>();
		paramsMap.put("type", type);
		paramsMap.put("callback", callback);
		final ResourceResponse responseResource = new ResourceResponse(paramsMap);
		final String remoteIP = UtilMethods.isSet(request.getRemoteHost())?
				request.getRemoteHost() : request.getRemoteAddr();

		if (request.getInputStream().isFinished()) {
			Logger.error(this.getClass(), "Push Publishing failed from " + remoteIP + " bundle expected");
			return responseResource.responseError(HttpStatus.SC_BAD_REQUEST);
		}

		final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken
				= AuthCredentialPushPublishUtil.INSTANCE.processAuthHeader(request);

		final Optional<Response> failResponse = PushPublishResourceUtil.getFailResponse(request, pushPublishAuthenticationToken);

		if (failResponse.isPresent()) {
			return failResponse.get();
		}

		final Bundle bundle = this.publishBundle(forcePush, request, remoteIP);

		return Response.ok(bundle).build();
	}

	@WrapInTransaction
	private Bundle publishBundle(final boolean forcePush,
								 final HttpServletRequest request,
								 final String remoteIP) throws Exception {

		Logger.debug(BundlePublisherResource.class, "Publishing bundle from " + remoteIP);
		Logger.debug(BundlePublisherResource.class, "Force Push: " + forcePush);

		final String fileNameSent = getFileNameFromRequest(request);
		final String fileName = UtilMethods.isSet(fileNameSent) ? fileNameSent : generatedBundleFileName();

		Logger.debug(BundlePublisherResource.class, "Bundle file name: " + fileName);

		Bundle bundle = null;

		try (InputStream bundleStream = request.getInputStream()) {

			final String bundlePath         = ConfigUtils.getBundlePath()+ File.separator + MY_TEMP;
			final String bundleFolder       = fileName.substring(0, fileName.indexOf(".tar.gz"));
			final PublishAuditStatus status = PublishAuditAPI.getInstance().updateAuditTable(
					remoteIP, remoteIP, bundleFolder, true);

			// save bundle if it doesn't exists
			Logger.debug(BundlePublisherResource.class, "Checking if bundle exists: " + bundleFolder);
			bundle = APILocator.getBundleAPI().getBundleById(bundleFolder);
			if (bundle == null || bundle.getId() == null) {
				Logger.debug(BundlePublisherResource.class, "Saving bundle: " + bundleFolder);
				bundle = new Bundle();
				bundle.setId(bundleFolder);
				bundle.setName(fileName.replace(".tar.gz", ""));
				bundle.setPublishDate(Calendar.getInstance().getTime());
				bundle.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
				bundle.setForcePush(forcePush);
				APILocator.getBundleAPI().saveBundle(bundle);
				Logger.debug(BundlePublisherResource.class, "Bundle saved: " + bundleFolder);
			}

			//Write file on FS
			FileUtil.writeToFile(bundleStream, bundlePath + fileName);

			//Start thread

			if(!status.getStatus().equals(Status.PUBLISHING_BUNDLE)) {
				Logger.debug(BundlePublisherResource.class, "Triggering Push Publisher Job for bundle: " + fileName);
				PushPublisherJob.triggerPushPublisherJob(fileName, status);
			}

			return bundle;
		} catch (Exception e) {

			Logger.error(
					PublisherQueueJob.class,
					String.format("Error caused by remote call of: Remote IP - %s, bundle file name - %s, end point- %s",
							remoteIP, fileName,  remoteIP));
			Logger.error(PublisherQueueJob.class,e.getMessage(),e);
			throw e;
		}
	}

	private String getFileNameFromRequest(HttpServletRequest request) {
		try {
			final String fileNameValue = request.getHeader("Content-Disposition")
					.split(";")[1]
					.trim()
					.split("=")[1];
			return fileNameValue.substring(1, fileNameValue.length() - 1);
		} catch (Exception e) {
			return null;
		}
	}

	private String generatedBundleFileName() {
		return String.format("bundle_%d.tar.gz", System.currentTimeMillis());
	}

	private boolean isAdmin(final User user) {

		return null != user && user.isBackendUser() && user.isAdmin();
	}
}
