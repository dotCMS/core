package com.dotcms.rest;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.pusher.AuthCredentialPushPublishUtil;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.util.EnterpriseFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.liferay.util.StringPool.BLANK;

@Path("/bundlePublisher")
@Tag(name = "Bundle", description = "Content bundle management and deployment")
public class BundlePublisherResource {

	public static String MY_TEMP = "";

	/**
	 * Receives a Bundle from another sending dotCMS instance with the intention of publishing it.
	 * <p>When a Bundle file is received on this endpoint, it's necessary to check whether the
	 * sending server is allowed to send data to this server, and if the security tokens match. If
	 * all the validations are correct, the bundle will be added to the {@link PushPublisherJob
	 * Publish Thread}.
	 *
	 * @param type      response type
	 * @param callback  response callback
	 * @param forcePush If the {@code Force Push Everything} filter is selected, this parameter
	 *                  will be {@code true}.
	 * @param filterKey The ID of the Push Publishing Filter that was selected to generate the
	 *                  Bundle.
	 * @param request   The current instance of the {@link HttpServletRequest}.
	 * @param response  The current instance of the {@link HttpServletResponse}.
	 *
	 * @return Returns a {@link Response} object with a 200 status code if success or a 500 error
	 * code if anything fails on the Publish process
	 *
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
			@QueryParam("filterkey")   final String filterKey,
			@Context final HttpServletRequest  request,
			@Context final HttpServletResponse response) throws Exception {
		if (LicenseManager.getInstance().isCommunity()) {
			throw new InvalidLicenseException("License required");
		}
		Logger.debug(this, String.format("Publishing bundle with type: [ %s ], callback: [ %s ], forcePush: [ %s ], filterKey: [ %s ]",
				type, callback, forcePush, filterKey));
		final Map<String, String> paramsMap = new HashMap<>();
		paramsMap.put("type", type);
		paramsMap.put("callback", callback);
		paramsMap.put("filterKey", UtilMethods.isSet(filterKey) ? filterKey : BLANK);
		final ResourceResponse responseResource = new ResourceResponse(paramsMap);
		final String remoteIP = UtilMethods.isSet(request.getRemoteHost())?
				request.getRemoteHost() : request.getRemoteAddr();

		if (request.getInputStream().isFinished()) {
			Logger.error(this.getClass(), "Push Publishing failed from " + remoteIP + ": Bundle expected");
			return responseResource.responseError(HttpStatus.SC_BAD_REQUEST);
		}

		final AuthCredentialPushPublishUtil.PushPublishAuthenticationToken pushPublishAuthenticationToken
				= AuthCredentialPushPublishUtil.INSTANCE.processAuthHeader(request);

		final Optional<Response> failResponse = PushPublishResourceUtil.getFailResponse(request, pushPublishAuthenticationToken);

		if (failResponse.isPresent()) {
			return failResponse.get();
		}
		final Bundle bundle = this.publishBundle(forcePush, filterKey, request, remoteIP);
		return Response.ok(bundle).build();
	}

	/**
	 * Retrieves the Bundle from the {@link HttpServletRequest} object and saves it to the file
	 * system. Then, it calls the {@link PushPublisherJob} to start importing its contents.
	 *
	 * @param forcePush If the {@code Force Push Everything} filter is selected, this parameter
	 *                  will be {@code true}.
	 * @param filterKey The ID of the Push Publishing Filter that was selected to generate the
	 *                  Bundle.
	 * @param request   The current instance of the {@link HttpServletRequest}.
	 * @param remoteIP  The IP address of the sending server.
	 *
	 * @return Returns the {@link Bundle} object that was saved.
	 *
	 * @throws Exception An error occurred when trying to save and process the Bundle.
	 */
	@WrapInTransaction
	private Bundle publishBundle(final boolean forcePush,
								 final String filterKey,
								 final HttpServletRequest request,
								 final String remoteIP) throws Exception {

		Logger.debug(BundlePublisherResource.class, "Publishing bundle from " + remoteIP);
		Logger.debug(BundlePublisherResource.class, "Force Push: " + forcePush);

		final String fileNameSent = getFileNameFromRequest(request);
		final String fileName = UtilMethods.isSet(fileNameSent) ? fileNameSent : generatedBundleFileName();

		Logger.debug(BundlePublisherResource.class, "Bundle file name: " + fileName);

		Bundle bundle;

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
				bundle.setFilterKey(filterKey);
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

}
