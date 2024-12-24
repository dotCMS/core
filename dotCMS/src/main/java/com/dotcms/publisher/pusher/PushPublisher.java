package com.dotcms.publisher.pusher;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.BundleXMLAsc;
import com.dotcms.enterprise.publishing.remote.bundler.CategoryBundler;
import com.dotcms.enterprise.publishing.remote.bundler.CategoryFullBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ContentTypeBundler;
import com.dotcms.enterprise.publishing.remote.bundler.DependencyBundler;
import com.dotcms.enterprise.publishing.remote.bundler.ExperimentBundler;
import com.dotcms.enterprise.publishing.remote.bundler.FolderBundler;
import com.dotcms.enterprise.publishing.remote.bundler.HostBundler;
import com.dotcms.enterprise.publishing.remote.bundler.LanguageBundler;
import com.dotcms.enterprise.publishing.remote.bundler.LanguageVariablesBundler;
import com.dotcms.enterprise.publishing.remote.bundler.LinkBundler;
import com.dotcms.enterprise.publishing.remote.bundler.OSGIBundler;
import com.dotcms.enterprise.publishing.remote.bundler.RelationshipBundler;
import com.dotcms.enterprise.publishing.remote.bundler.RuleBundler;
import com.dotcms.enterprise.publishing.remote.bundler.TemplateBundler;
import com.dotcms.enterprise.publishing.remote.bundler.UserBundler;
import com.dotcms.enterprise.publishing.remote.bundler.VariantBundler;
import com.dotcms.enterprise.publishing.remote.bundler.WorkflowBundler;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.EndpointDetail;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.business.PublisherQueueJob;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.TarGzipBundleOutput;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.ResourceResponse;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.system.event.local.type.pushpublish.AllPushPublishEndpointsFailureEvent;
import com.dotcms.system.event.local.type.pushpublish.AllPushPublishEndpointsSuccessEvent;
import com.dotcms.system.event.local.type.pushpublish.SinglePushPublishEndpointFailureEvent;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.EnterpriseFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.ThreadContext;
import org.glassfish.jersey.client.ClientProperties;
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This is the main content publishing class in the Push Publishing process.
 * This class defines the list of bundlers that will take the pusheable objects
 * selected by the user, and sends the zipped bundle to the destination server.
 * The purpose of the bundlers ({@link IBundler} objects) is to provide a way to
 * say how to write out the different parts and objects of the bundle.
 * <p>
 * This publisher is also aware of the publishing status of the bundle in the
 * destination server(s). This means that it updates the local status of the
 * bundle so users will know if the bundle was successfully deployed or if
 * something failed during the process.
 *
 * @author Alberto
 * @version 1.0
 * @since Oct 12, 2012
 *
 */
public class PushPublisher extends Publisher {

	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();
	private PublishingEndPointAPI publishingEndPointAPI = APILocator.getPublisherEndPointAPI();
	private LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

	public static final String PROTOCOL_HTTP  = "http";
	public static final String PROTOCOL_HTTPS = "https";
	private static final String HTTP_PORT      = "80";
	private static final String HTTPS_PORT 	   = "443";

	private static final String BUNDLE_ID      = "BundleId";
	private static final String ENDPOINT_NAME  = "EndpointName";

	@Override
	public PublisherConfig init ( PublisherConfig config ) throws DotPublishingException {
		if ( LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level ) {
			throw new RuntimeException( "need an enterprise pro license to run this bundler" );
		}

		config.setStatic(false);
		this.config = super.init( config );
		return this.config;
	}

	/**
	 * Final step of the Bundle Push Publishing. This method will generate the
	 * Bundle file compressing all the information generated by the Bundlers
	 * into a tar.gz file what will live on the assets directory. After the
	 * Bundle is created, this process will try to send the Bundle to a list of
	 * previously selected Environments.
	 *
	 * @param status
	 *            Current status of the Publishing process
	 * @return This bundle configuration ({@link PublisherConfig})
	 * @throws DotPublishingException
	 *             An error occurred which caused the publishing process to
	 *             stop.
	 */
	@Override
	public PublisherConfig process ( final PublishStatus status ) throws DotPublishingException {
		if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
			throw new RuntimeException("An Enterprise Pro License is required to run this publisher.");
		}
		PublishAuditHistory currentStatusHistory = null;

		Client client = getRestClient();

		try {
			//Compressing bundle
			File bundleRoot = BundlerUtil.getBundleRoot(this.config.getName(), false);
			final List<File> list = new ArrayList<>(1);
			list.add(bundleRoot);
			File bundleFile = new File(bundleRoot + ".tar.gz");

			List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(this.config.getId());

			client.property(ClientProperties.REQUEST_ENTITY_PROCESSING, "CHUNKED");
			client.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024);

			String contentDisposition = "attachment; filename=\"" + bundleFile.getName() + "\"";

			//Updating audit table
			currentStatusHistory = pubAuditAPI.getPublishAuditStatus(this.config.getId()).getStatusPojo();
			Map<String, Map<String, EndpointDetail>> endpointsMap = currentStatusHistory.getEndpointsMap();
			// If not empty, don't overwrite publish history already set via the PublisherQueueJob
			boolean isHistoryEmpty = endpointsMap.isEmpty();
			currentStatusHistory.setPublishStart(new Date());
			PushPublishLogger.log(this.getClass(), "Status Update: Sending to all environments");
			pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.SENDING_TO_ENDPOINTS, currentStatusHistory);
			//Increment numTries
			currentStatusHistory.addNumTries();
			// Counters for determining the publishing status
			int errorCounter = 0;
			int totalEndpoints = 0;
			for (Environment environment : environments) {
				List<PublishingEndPoint> allEndpoints = this.publishingEndPointAPI.findSendingEndPointsByEnvironment(environment.getId());
				List<PublishingEndPoint> endpoints = new ArrayList<>();
				totalEndpoints += (null != allEndpoints) ? allEndpoints.size() : 0;

				Map<String, EndpointDetail> endpointsDetail = endpointsMap.get(environment.getId());
				//Filter Endpoints list and push only to those that are enabled and are Dynamic (not S3 at the moment)
				if (null != allEndpoints) {
					for (PublishingEndPoint ep : allEndpoints) {
						if (ep.isEnabled() && getProtocols().contains(ep.getProtocol())) {
							// If pushing a bundle for the first time, always add all end-points
							if (null == endpointsDetail || endpointsDetail.isEmpty()) {
								endpoints.add(ep);
							} else {
								EndpointDetail epDetail = endpointsDetail.get(ep.getId());
								// If re-trying a bundle or just re-attempting to install a bundle,
								// send it only to those end-points whose status IS NOT success
								if (DeliveryStrategy.ALL_ENDPOINTS.equals(this.config.getDeliveryStrategy())
										|| (DeliveryStrategy.FAILED_ENDPOINTS.equals(this.config.getDeliveryStrategy())
										&& PublishAuditStatus.Status.SUCCESS.getCode() != epDetail.getStatus()
										&& Status.SUCCESS_WITH_WARNINGS.getCode() != epDetail.getStatus()
										&& PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode() != epDetail.getStatus())) {
									endpoints.add(ep);
								}
							}
						}
					}
				}
				boolean failedEnvironment = false;
				if(!environment.getPushToAll()) {
					Collections.shuffle(endpoints);
					if(!endpoints.isEmpty())
						endpoints = endpoints.subList(0, 1);
				}

				for (PublishingEndPoint endpoint : endpoints) {
					EndpointDetail detail = new EndpointDetail();

					InputStream bundleStream = new BufferedInputStream(Files.newInputStream(bundleFile.toPath()));

					try {
						Bundle bundle = APILocator.getBundleAPI().getBundleById(this.config.getId());

						//For logging purpose
						ThreadContext.put(ENDPOINT_NAME, ENDPOINT_NAME + "=" + endpoint.getServerName());
						ThreadContext.put(BUNDLE_ID, BUNDLE_ID + "=" + bundle.getName());

						if (endpoint.hasAuthKey()) {
							PushPublishLogger.log(this.getClass(), "Status Update: Sending Bundle");
							final String filterKey = bundle.getFilterKey();
							WebTarget webTarget = client.target(endpoint.toURL() + "/api/bundlePublisher/publish")
									.queryParam("FORCE_PUSH", bundle.isForcePush())
									.queryParam("filterkey", filterKey);

							Response response = webTarget.request(MediaType.APPLICATION_JSON)
									.header("Content-Disposition", contentDisposition)
									.header("Authorization", AuthCredentialPushPublishUtil.INSTANCE.getRequestToken(endpoint).get())
									.post(Entity.entity(bundleStream, MediaType.APPLICATION_OCTET_STREAM_TYPE));

							if (response.getStatus() == HttpStatus.SC_OK) {
								PushPublishLogger.log(this.getClass(), "Status Update: Bundle sent");
								detail.setStatus(PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode());
								detail.setInfo("Everything ok");
							} else if (response.getStatus() == HttpStatus.SC_UNAUTHORIZED) {

								handleInvalidTokenResponse(environment, endpoint, detail, response);
								failedEnvironment = true;
								errorCounter++;
							} else if (response.getStatus() == HttpStatus.SC_FORBIDDEN){
								markAsLicenseRequired(environment, endpoint, detail);
								failedEnvironment = true;
								errorCounter++;
							} else {

								PushPublishLogger.log(this.getClass(), "Status Update: Failed to send bundle.");
								if (currentStatusHistory.getNumTries() >= PublisherQueueJob.MAX_NUM_TRIES) {
									APILocator.getPushedAssetsAPI().deletePushedAssets(this.config.getId(), environment.getId());
								}
								detail.setStatus(PublishAuditStatus.Status.FAILED_TO_SENT.getCode());
								detail.setInfo(
										"Returned " + response.getStatus() + " status code " +
												"for the endpoint " + endpoint.getServerName() + " with address " + endpoint
												.getAddress() + getFormattedPort(endpoint.getPort()));
								failedEnvironment = true;
							}
						} else {
							markAsInValidToken(environment, endpoint, detail, AuthCredentialPushPublishUtil.INVALID_TOKEN_ERROR_KEY);
							failedEnvironment = true;
							errorCounter++;
						}
					} catch(Exception e){
						// if the bundle can't be sent after the total num of tries, delete the pushed assets for this bundle
						if (currentStatusHistory.getNumTries() >= PublisherQueueJob.MAX_NUM_TRIES) {
							APILocator.getPushedAssetsAPI().deletePushedAssets(this.config.getId(), environment.getId());
						}
						detail.setStatus(PublishAuditStatus.Status.FAILED_TO_SENT.getCode());
						String
								error =
								"An error occurred for the endpoint " + endpoint.getServerName() + " with address "
										+ endpoint.getAddress() + getFormattedPort(
										endpoint.getPort()) + ". Error: " + e.getMessage();
						detail.setInfo(error);
						failedEnvironment |= true;
						errorCounter++;
						Logger.error(this.getClass(), error, e);

						PushPublishLogger.log(this.getClass(), "Status Update: Failed to send bundle. Exception: " + e.getMessage());
					} finally{
						CloseUtils.closeQuietly(bundleStream);
						ThreadContext.remove(ENDPOINT_NAME);
						ThreadContext.remove(BUNDLE_ID);
					}
					if (isHistoryEmpty || failedEnvironment) {
						currentStatusHistory.addOrUpdateEndpoint(environment.getId(), endpoint.getId(), detail);
					}
				}
			}

			if(errorCounter==0) {
				//Updating audit table
				PushPublishLogger.log(this.getClass(), "Status Update: Bundle sent");
				pubAuditAPI.updatePublishAuditStatus(this.config.getId(),
						PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY, currentStatusHistory);

				//Triggering event listener when all endpoints are successfully sent
				localSystemEventsAPI.asyncNotify(new AllPushPublishEndpointsSuccessEvent(config));
			} else {

				/*
				If we have failed bundles we need to update the delivery strategy in order to only
				retry the failing endpoints and avoid to resent to successfully endpoints
				 */
				if (!DeliveryStrategy.FAILED_ENDPOINTS.equals(this.config.getDeliveryStrategy())) {
					updateJobDataMap(DeliveryStrategy.FAILED_ENDPOINTS);
				}

				if (errorCounter == totalEndpoints) {
					pubAuditAPI.updatePublishAuditStatus(this.config.getId(),
							PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS, currentStatusHistory);

					//Triggering event listener when all endpoints failed during the process
					localSystemEventsAPI.asyncNotify(new AllPushPublishEndpointsFailureEvent(config.getAssets()));
				} else {
					pubAuditAPI.updatePublishAuditStatus(this.config.getId(),
							PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS, currentStatusHistory);

					//Triggering event listener when at least one endpoint is successfully sent but others failed
					localSystemEventsAPI.asyncNotify(new SinglePushPublishEndpointFailureEvent(config.getAssets()));
				}
			}
			return this.config;
		} catch (Exception e) {
			//Updating audit table
			try {
				PushPublishLogger.log(this.getClass(), "Status Update: Failed to publish");
				pubAuditAPI.updatePublishAuditStatus(this.config.getId(), PublishAuditStatus.Status.FAILED_TO_PUBLISH, currentStatusHistory);
			} catch (DotPublisherException e1) {
				throw new DotPublishingException(e.getMessage(),e);
			}
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotPublishingException(e.getMessage(),e);
		} finally {
			client.close();
		}
	}

	private void handleInvalidTokenResponse(
			final Environment environment,
			final PublishingEndPoint endpoint,
			final EndpointDetail detail,
			final Response response) throws DotDataException, LanguageException {

		final Map<String, String> wwwAuthenticateHeader = ResourceResponse.getWWWAuthenticateHeader(response);
		final String errorKey = wwwAuthenticateHeader.get("error_key").replaceAll("\"", "");

		markAsInValidToken(environment, endpoint, detail, errorKey);
	}

	private void markAsInValidToken(
			final Environment environment,
			final PublishingEndPoint endpoint,
			final EndpointDetail detail,
			final String errorKey) throws DotDataException, LanguageException {

		final String message = LanguageUtil.get(String.format("push_publish.end_point.%s_message", errorKey));
		APILocator.getPublisherEndPointAPI().updateEndPoint(endpoint);

		final PublishAuditStatus.Status invalidToken = PublishAuditStatus.Status.INVALID_TOKEN;
		updatingPublishingDetailStatus(environment, endpoint, detail, errorKey, invalidToken, message);
	}

	private void markAsLicenseRequired(
			final Environment environment,
			final PublishingEndPoint endpoint,
			final EndpointDetail detail) throws DotDataException, LanguageException {

		final String message = LanguageUtil.get(String.format("push_publish.end_point.license_required_message"));
		APILocator.getPublisherEndPointAPI().updateEndPoint(endpoint);

		final PublishAuditStatus.Status licenseRequired = PublishAuditStatus.Status.LICENSE_REQUIRED;
		updatingPublishingDetailStatus(environment, endpoint, detail, null, licenseRequired, message);
	}

	private void updatingPublishingDetailStatus(
			final Environment environment,
			final PublishingEndPoint endpoint,
			final EndpointDetail detail,
			final String newAuthKey,
			final PublishAuditStatus.Status status,
			final String message) throws DotDataException {

		APILocator.getPushedAssetsAPI().deletePushedAssets(this.config.getId(), environment.getId());

		detail.setStatus(status.getCode());
		detail.setInfo(message);
		PushPublishLogger.log(this.getClass(), message);

		if (newAuthKey != null) {
			endpoint.setAuthKey(newAuthKey);
		}

		APILocator.getPublisherEndPointAPI().updateEndPoint(endpoint);

		try {
			PublisherAPI.getInstance().deleteElementsFromPublishQueueTable(this.config.getId());
		} catch (DotPublisherException e) {
			Logger.error(this.getClass(), e);
			throw new DotRuntimeException(e);
		}
	}


	/**
	 * @param port
	 * @return
	 */
	private String getFormattedPort(String port){

		if(port !=null && !port.equals(HTTP_PORT) && !port.equals(HTTPS_PORT)){
			return ":" + port;
		}
		return "";
	}

	/**
	 * Return a end point token
	 * @return
	 * @throws IOException
	 */
	//todo: I think this method can be remove
	public static Optional<String> retriveEndpointKeyDigest(final PublishingEndPoint endpoint) throws IOException {

		final Optional<String> key = retriveEndpointKey(endpoint);

		return key.isPresent() ? Optional.of(PublicEncryptionFactory.digestString(key.get())) : Optional.empty();
	}

	public static Optional<String> retriveEndpointKey(final PublishingEndPoint endpoint) throws IOException { // todo: create a method that allows to receives a key and use the com.dotcms.util.security.Encryptor instead PublicEncryptionFactory

		if(endpoint==null || endpoint.getAuthKey() ==null) {
			Logger.warn(PushPublisher.class,"Endpoint or endpoint key is null:" + endpoint);
			return Optional.empty();
		}

		String token = PublicEncryptionFactory.decryptString(endpoint.getAuthKey().toString());
		String key = null;
		if(token.contains(File.separator)) {
			File tokenFile = new File(token);
			if(tokenFile != null && tokenFile.exists())
				key = FileUtils.readFileToString(tokenFile, "UTF-8").trim();
		} else {
			key = token;
		}
		return key==null ? Optional.empty() : Optional.of(key);
	}

	@Override
	public List<Class> getBundlers () {
		boolean buildUsers = false;
		boolean buildCategories = false;
		boolean buildOSGIBundle = false;
		boolean buildLanguages = false;
		boolean buildRules = false;
		boolean buildAsset = false;
		boolean buildExperiments = false;
		List<Class> list = new ArrayList<>();
		for ( PublishQueueElement element : config.getAssets() ) {
			if ( element.getType().equals(PusheableAsset.CATEGORY.getType()) ) {
				buildCategories = true;
			} else if ( element.getType().equals(PusheableAsset.OSGI.getType()) ) {
				buildOSGIBundle = true;
			} else if ( element.getType().equals(PusheableAsset.USER.getType()) ) {
				buildUsers = true;
			} else if (element.getType().equals(PusheableAsset.LANGUAGE.getType())) {
				buildLanguages = true;
			} else if (element.getType().equals(PusheableAsset.RULE.getType())) {
				buildRules = true;
			} else if (element.getType().equals(PusheableAsset.EXPERIMENT.getType())) {
				buildExperiments = true;
				buildAsset = true;
			} else {
				buildAsset = true;
			}
		}
		if(config.getLuceneQueries().size() > 0){
			buildAsset = true;
		}
		if ( buildUsers ) {
			list.add( UserBundler.class );
		}
		if ( buildOSGIBundle ) {
			list.add( OSGIBundler.class );
		}

		if ( buildAsset || buildLanguages) {
			list.add( DependencyBundler.class );
			list.add( HostBundler.class );
			list.add( ContentBundler.class );
			list.add( FolderBundler.class );
			list.add( TemplateBundler.class );
			list.add( ContainerBundler.class );
			list.add(RuleBundler.class);
			list.add( LinkBundler.class );
			if ( Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", false) ) {
				list.add( ContentTypeBundler.class );
				list.add( RelationshipBundler.class );
			}
			list.add( LanguageVariablesBundler.class );
			list.add( WorkflowBundler.class );
			list.add( LanguageBundler.class );
		} else {
			list.add(DependencyBundler.class);
			if (buildRules) {
				list.add(HostBundler.class);
				list.add(RuleBundler.class);
			}
		}
		list.add( BundleXMLAsc.class );
		if ( buildCategories ) { // If we are PP from the categories portlet.
			list.add( CategoryFullBundler.class );
		} else { // If we are PP from anywhere else, for example a contentlet, site, folder, etc.
			list.add(CategoryBundler.class);
		}
		if(buildExperiments) {
			list.add(ExperimentBundler.class);
			list.add(VariantBundler.class);
		}
		return list;
	}

	@Override
	public Set<String> getProtocols(){
		Set<String> protocols = new HashSet<>();
		protocols.add(PROTOCOL_HTTP);
		protocols.add(PROTOCOL_HTTPS);
		return protocols;
	}

	/**
	 * Returns an instance of the REST {@link Client} used to access Push Publishing end-points and
	 * retrieve their information.
	 *
	 * @return The REST {@link Client}.
	 */
	private Client getRestClient() {
		return RestClientBuilder.newClient();
	}

	/**
	 * Allows to update the delivery strategy to the PublishQueueJob
	 */
	private void updateJobDataMap(DeliveryStrategy deliveryStrategy) {
		try {
			Scheduler sched = QuartzUtils.getScheduler();
			JobDetail job = sched.getJobDetail("PublishQueueJob", "dotcms_jobs");
			if (job == null) {
				return;
			}

			job.getJobDataMap().put("deliveryStrategy", deliveryStrategy);
			sched.addJob(job, true);
		} catch (ObjectAlreadyExistsException e) {
			// Quartz will throw this error if it is already running
			Logger.debug(this.getClass(), e.getMessage(), e);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
	}

	@Override
	public BundleOutput createBundleOutput() throws IOException {
		return new TarGzipBundleOutput(config);
	}
}
