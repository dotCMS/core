package com.dotcms.rest;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessage;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.bundle.business.BundleDeleteResult;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublishQueueElementTransformer;
import com.dotcms.publisher.business.PublisherAPIImpl;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.manifest.ManifestUtil;
import com.dotcms.publishing.output.TarGzipBundleOutput;
import org.apache.commons.io.IOUtils;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.rest.param.ISODateParam;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.lang.StringEscapeUtils;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

import static com.dotcms.publisher.business.PublishAuditAPIImpl.NO_LIMIT_ASSETS;
import static com.dotcms.publisher.business.PublishAuditStatus.Status.FAILED_TO_BUNDLE;
import static com.dotcms.publisher.business.PublishAuditStatus.Status.FAILED_TO_PUBLISH;
import static com.dotcms.publisher.business.PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS;
import static com.dotcms.publisher.business.PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS;
import static com.dotcms.publisher.business.PublishAuditStatus.Status.FAILED_TO_SENT;
import static com.dotcms.publisher.business.PublishAuditStatus.Status.SUCCESS;
import static com.dotcms.publisher.business.PublishAuditStatus.Status.SUCCESS_WITH_WARNINGS;

@Path("/bundle")
@Tag(name = "Bundle")
public class BundleResource {

    public  static final String BUNDLE_THREAD_POOL_SUBMITTER_NAME = "bundlepolling";
    private final WebResource            webResource            = new WebResource();
    private final BundleAPI              bundleAPI              = APILocator.getBundleAPI();
    private final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();

    private final PublishQueueElementTransformer publishQueueElementTransformer =
            new PublishQueueElementTransformer();

    /**
     * Return the assets from a bundleId
     *
     * @param bundleId
     * @param request
     * @param response
     * @return
     */
    @Path("/{bundleId}/assets")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getPublishQueueElements(@PathParam("bundleId") final String bundleId,
            @QueryParam("limit") final Integer limitParam,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response) throws DotDataException {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        try {
            PublishAuditStatus publishAuditStatus = PublishAuditAPI.getInstance()
                    .getPublishAuditStatus(bundleId, 0);

            if (!UtilMethods.isSet(publishAuditStatus)) {
                final Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
                if (!UtilMethods.isSet(bundle)) {
                    throw new NotFoundException(String.format("Bundle %s not exists", bundleId));
                }
            }

            final List<PublishQueueElement> queueElements = PublisherAPIImpl.getInstance()
                    .getQueueElementsByBundleId(bundleId);

            final List<Map<String, Object>> detailedAssets;

            if (UtilMethods.isSet(queueElements)) {
                detailedAssets = publishQueueElementTransformer.transform(queueElements);
            } else {
                final int limit = UtilMethods.isSetOrGet(limitParam, NO_LIMIT_ASSETS);
                publishAuditStatus = PublishAuditAPI.getInstance()
                        .getPublishAuditStatus(bundleId, limit);

                if (!UtilMethods.isSet(publishAuditStatus)) {
                    detailedAssets = ImmutableList.of();
                } else {
                    final Map<String, String> assets = publishAuditStatus.getStatusPojo().getAssets();
                    detailedAssets = assets.entrySet().stream()
                            .map(entry -> publishQueueElementTransformer.getMap(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList());
                }
            }

            return Response.ok(detailedAssets).build();
        } catch (DotPublisherException e) {
            throw new BadRequestException(e, bundleId, e.getMessage());
        }
    }

    /**
     * Returns a list of un-send bundles (haven't been sent to any Environment) filtered by owner and name
     *
     * @param request
     * @param params
     * @return
     * @throws DotStateException
     * @throws DotDataException
     * @throws JSONException
     */
    @GET
    @Path ("/getunsendbundles/{params:.*}")
    @Produces ("application/json")
    public Response getUnsendBundles (@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam ("params") String params )
            throws DotDataException, JSONException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

        //Reading the parameters
        String userId = initData.getParamsMap().get( "userid" );
        String bundleName = request.getParameter( "name" );
        String startParam = request.getParameter( "start" );
        String countParam = request.getParameter( "count" );

        int start = 0;
        if ( UtilMethods.isSet( startParam ) ) {
            start = Integer.valueOf( startParam );
        }

        int offset = -1;
        if ( UtilMethods.isSet( countParam ) ) {
            offset = Integer.valueOf( countParam );
        }

        if ( UtilMethods.isSet( bundleName ) ) {
            if ( bundleName.equals( "*" ) ) {
                bundleName = null;
            } else {
                bundleName = bundleName.replaceAll( "\\*", "" );
            }
        }

        JSONArray jsonBundles = new JSONArray();

        //Find the unsend bundles
        List<Bundle> bundles;
        if ( bundleName == null ) {
            //Find all the bundles for this user
            bundles = APILocator.getBundleAPI().getUnsendBundles( userId, offset, start );
        } else {
            //Filter by name
            bundles = APILocator.getBundleAPI().getUnsendBundlesByName( userId, bundleName, offset, start );
        }
        for ( Bundle b : bundles ) {

            JSONObject jsonBundle = new JSONObject();
            jsonBundle.put( "id", b.getId() );
            jsonBundle.put( "name", StringEscapeUtils.unescapeJava(b.getName()));
            //Added to the response list
            jsonBundles.add( jsonBundle );
        }

        //Prepare the response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put( "identifier", "id" );
        jsonResponse.put( "label", "name" );
        jsonResponse.put( "items", jsonBundles.toArray() );
        jsonResponse.put( "numRows", bundles.size() );

        CacheControl nocache=new CacheControl();
        nocache.setNoCache(true);
        return Response.ok(jsonResponse.toString()).cacheControl(nocache).build();
    }

	@GET
	@Path("/updatebundle/{params:.*}")
	@Produces("application/json")
	public Response updateBundle(@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("params") String params) throws IOException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();


        //Creating an utility response object
	    ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );
	
		String bundleId = initData.getParamsMap().get("bundleid");
		String bundleName = URLDecoder.decode(request.getParameter("bundleName"), "UTF-8");
		try {
	
			if(!UtilMethods.isSet(bundleId)) {
	            return responseResource.response( "false" );
			}
	
			Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
			bundle.setName(bundleName);
			APILocator.getBundleAPI().updateBundle(bundle);
	
		} catch (DotDataException e) {
			Logger.error(getClass(), "Error trying to update Bundle. Bundle ID: " + bundleId);
	        return responseResource.response( "false" );
		}
	
	    return responseResource.response("true");
	}

	@GET
	@Path("/deletepushhistory/{params:.*}")
	@Produces("application/json")
	public Response deletePushHistory(@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

        String assetId = initData.getParamsMap().get("assetid");

		try {

			if(!UtilMethods.isSet(assetId)) {
                return responseResource.response( "false" );
			}

			APILocator.getPushedAssetsAPI().deletePushedAssets(assetId);

		} catch (DotDataException e) {
			Logger.error(getClass(), "Error trying to delete Pushed Assets for asset Id: " + assetId);
            return responseResource.response( "false" );
		}

        return responseResource.response( "true" );
	}

	@GET
	@Path("/deleteenvironmentpushhistory/{params:.*}")
	@Produces("application/json")
	public Response deleteEnvironmentPushHistory(@Context HttpServletRequest request, @Context final HttpServletResponse response, @PathParam("params") String params) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .params(params)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		String environmentId = initData.getParamsMap().get("environmentid");

		try {

			if(!UtilMethods.isSet(environmentId)) {
                return responseResource.response( "false" );
			}

			APILocator.getPushedAssetsAPI().deletePushedAssetsByEnvironment(environmentId);

		} catch (DotDataException e) {
			Logger.error(getClass(), "Error trying to delete Pushed Assets for environment Id: " + environmentId);
            return responseResource.response( "false" );
		}

        return responseResource.response( "true" );
	}

    /**
     * Deletes all bundles by identifier
     * Note: the response will be notified by socket message
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     * @param deleteBundlesByIdentifierForm {@link DeleteBundlesByIdentifierForm} contains the set of bundle ids to delete.
     */
	@DELETE
    @Path("/ids")
    @Produces("application/json")
    public Response deleteBundlesByIdentifiers(@Context   final HttpServletRequest request,
                                               @Context   final HttpServletResponse response,
                                               final DeleteBundlesByIdentifierForm  deleteBundlesByIdentifierForm) throws DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        if (!UtilMethods.isSet(deleteBundlesByIdentifierForm) || !UtilMethods.isSet(deleteBundlesByIdentifierForm.getIdentifiers())) {
            throw new IllegalArgumentException("List of body to delete is mandatory");
        }

        Logger.info(this, "Deleting the bundles: " + deleteBundlesByIdentifierForm.getIdentifiers()
                + " by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        final Locale locale    = LocaleUtil.getLocale(request);

        dotSubmitter.execute(() -> {

            try {

                final Tuple2<Integer, Set<String>> result = this.deleteBundleByIdentifier(deleteBundlesByIdentifierForm, initData);
                if (!UtilMethods.isSet(result._2)) { // no errors

                    this.sendSuccessDeleteBundleMessage(result._1, initData, locale);
                } else {

                    this.sendWarningDeleteBundleMessage(result._1, result._2, initData, locale);
                }
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteBundlesByIdentifiers, couldn't delete the identifiers: "
                                + deleteBundlesByIdentifierForm.getIdentifiers() +
                                ", exception message: " + e.getMessage(), e);

                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return buildResponseToDeleteEndpoint(deleteBundlesByIdentifierForm);
    } // deleteBundlesByIdentifiers.

    private static Response buildResponseToDeleteEndpoint(final DeleteBundlesByIdentifierForm deleteBundlesByIdentifierForm)
            throws DotDataException {

        final List<String> existingBundles = new ArrayList<>();
        final List<String> notExistingBundles = new ArrayList<>();

        for (String identifier : deleteBundlesByIdentifierForm.getIdentifiers()) {
            final Bundle bundleById = APILocator.getBundleAPI().getBundleById(identifier);

            if (UtilMethods.isSet(bundleById)) {
                existingBundles.add(identifier);
            } else {
                notExistingBundles.add(identifier);
            }
        }

        final StringBuffer message = new StringBuffer();

        if (!existingBundles.isEmpty()) {
            message.append(String.format(
                        "Removing the follow bundles in a separated process, the result of the operation will be notified: %s",
                        existingBundles.stream().collect(Collectors.joining(", ")))
                    );
        }

        if (!existingBundles.isEmpty() && !notExistingBundles.isEmpty()) {
            message.append(", ");
        }

        if (!notExistingBundles.isEmpty()) {
            message.append(String.format(
                    "The following bundles were not deleted because they don't exist: %s",
                    notExistingBundles.stream().collect(Collectors.joining(", ")))
            );
        }

        return Response.ok(new ResponseEntityView(message)).build();
    }

    private void sendErrorDeleteBundleMessage(final InitDataObject initData,
                                              final Locale locale,
                                              final Exception e) {

        final String message = Try.of(()->LanguageUtil.get(locale, "bundle.deleted.error.msg", e.getMessage()))
                .onFailure(ex -> Logger.error(this, e.getMessage()))
                .getOrElse("An error occurred deleting bundles, please check the log, error message: " + e.getMessage());

        this.systemMessageEventUtil.pushMessage(new SystemMessageBuilder()
                .setMessage(message)
                .setLife(DateUtil.TEN_SECOND_MILLIS)
                .setSeverity(MessageSeverity.ERROR).create(), List.of(initData.getUser().getUserId()));
    }

    private void sendWarningDeleteBundleMessage(final int bundleDeletesSize,
                                                final Set<String> failBundleSet,
                                                final InitDataObject initData,
                                                final Locale locale) throws DotDataException {

        final String userId  = initData.getUser().getUserId();
        final String message = Try.of(()->LanguageUtil.get(locale, "bundle.deleted.warning.msg", bundleDeletesSize, failBundleSet.size()))
                .onFailure(e -> Logger.error(this, e.getMessage()))
                .getOrElse(bundleDeletesSize + " Bundles Deleted Successfully, failed " + failBundleSet.size());

        Logger.error(this, " Bundles Deleted Successfully, failed " + failBundleSet.size());
        Logger.error(this, " Bundles Failed " + failBundleSet);

        this.systemMessageEventUtil.pushMessage(new SystemMessageBuilder()
                .setMessage(message)
                .setLife(DateUtil.SEVEN_SECOND_MILLIS)
                .setSeverity(MessageSeverity.WARNING).create(), List.of(userId));

        APILocator.getSystemEventsAPI().push(SystemEventType.DELETE_BUNDLE,
                new Payload(
                        message,
                        Visibility.USER,
                        userId
                )
        );
    }

    private void sendSuccessDeleteBundleMessage(final int bundleDeletesSize,
                                                final InitDataObject initData,
                                                final Locale locale) throws DotDataException {

	    final String userId  = initData.getUser().getUserId();
        final String message = Try.of(()->LanguageUtil.get(locale, "bundle.deleted.success.msg", bundleDeletesSize))
                .onFailure(e -> Logger.error(this, e.getMessage()))
                .getOrElse(bundleDeletesSize + " Bundles Deleted Successfully");

        this.systemMessageEventUtil.pushMessage(new SystemMessageBuilder()
                .setMessage(message)
                .setLife(DateUtil.SEVEN_SECOND_MILLIS)
                .setSeverity(MessageSeverity.INFO).create(), List.of(userId));

        APILocator.getSystemEventsAPI().push(SystemEventType.DELETE_BUNDLE,
                new Payload(
                        message,
                        Visibility.USER,
                        userId
                )
        );
    }

    // one transaction for each bundle
    private Tuple2<Integer, Set<String>> deleteBundleByIdentifier(final DeleteBundlesByIdentifierForm deleteBundlesByIdentifierForm, final InitDataObject initData) throws DotDataException {

	    int successCount        = 0;
	    final Set<String> fails = new LinkedHashSet<>();
        for (final String bundleId : deleteBundlesByIdentifierForm.getIdentifiers()) {

            try {

                this.bundleAPI.deleteBundleAndDependencies(bundleId, initData.getUser());
                successCount++;
            } catch (Exception e) {
                Logger.debug(this,"Exception on deleting bundle with id: " + bundleId,e);
                fails.add(bundleId);
            }
        }

        return Tuple.of(successCount, fails);
    }

    /**
     * Deletes bundles older than a date. (unsent are not going to be deleted)
     * Note: the response will be notified by socket message
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     * @param olderThan {@link ISODateParam} an ISO date, should be before now to be valid
     */
    @DELETE
    @Path("/olderthan/{olderThan}")
    @Produces("application/json")
    public Response deleteBundlesOlderThan(@Context   final HttpServletRequest request,
                                       @Context   final HttpServletResponse response,
                                       @PathParam("olderThan") final ISODateParam olderThan) {

        if(olderThan.after(new Date())) {

            throw new IllegalArgumentException("To avoid deleting bundles that publish in the future, the date can not be after the current date");
        }

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Locale locale    = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting the bundles older than: " + olderThan
                + " by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final BundleDeleteResult bundleDeleteResult =
                        this.bundleAPI.deleteBundleAndDependenciesOlderThan(olderThan, initData.getUser());

                sendDeleteResultsMessage(initData, locale, bundleDeleteResult);
            } catch (Exception e) {

                Logger.error(this.getClass(),
                        "Exception on deleteBundlesOlderThan, couldn't delete bundles older than: " + olderThan +
                                ", exception message: " + e.getMessage(), e);

                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return Response.ok(new ResponseEntityView(
                "Removing bundles in a separated process, the result of the operation will be notified")).build();
    } // deleteBundlesOlderThan.

    /**
     * Deletes all failed and succeed bundles
     * Note: the response will be notified by socket message
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     */
    @DELETE
    @Path("/all")
    @Produces("application/json")
    public Response deleteAll(@Context   final HttpServletRequest request,
                              @Context   final HttpServletResponse response) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Locale locale    = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting all bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.all.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        Status::values);

                final BundleDeleteResult bundleDeleteResult = this.bundleAPI.deleteAllBundles(initData.getUser(), statuses);
                sendDeleteResultsMessage(initData, locale, bundleDeleteResult);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteAll, couldn't delete bundles, exception message: " + e.getMessage(), e);
                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return Response.ok(new ResponseEntityView(
                "Removing bundles in a separated process, the result of the operation will be notified")).build();
    } // deleteAll.

    /**
     * Deletes all failed  bundles
     * Note: the response will be notified by socket message
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     */
    @DELETE
    @Path("/all/fail")
    @Produces("application/json")
    public Response deleteAllFail(@Context   final HttpServletRequest request,
                              @Context   final HttpServletResponse response) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Locale locale    = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting all failed bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.fail.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {FAILED_TO_SEND_TO_ALL_GROUPS, FAILED_TO_SEND_TO_SOME_GROUPS,
                                FAILED_TO_BUNDLE, FAILED_TO_SENT, FAILED_TO_PUBLISH});
                final BundleDeleteResult bundleDeleteResult = this.bundleAPI.deleteAllBundles(initData.getUser(), statuses);
                sendDeleteResultsMessage(initData, locale, bundleDeleteResult);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteAllFail, couldn't delete the fail bundles, exception message: " + e.getMessage(), e);
                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return Response.ok(new ResponseEntityView(
                "Removing bundles in a separated process, the result of the operation will be notified")).build();
    } // deleteAllFail.

    private void sendDeleteResultsMessage(InitDataObject initData, Locale locale,
            BundleDeleteResult bundleDeleteResult) throws DotDataException {
        if (UtilMethods.isSet(bundleDeleteResult.getFailedBundleSet())) {

            this.sendWarningDeleteBundleMessage(bundleDeleteResult.getDeleteBundleSet().size(),
                    bundleDeleteResult.getFailedBundleSet(), initData, locale);
        } else {

            this.sendSuccessDeleteBundleMessage(bundleDeleteResult.getDeleteBundleSet().size(),
                    initData, locale);
        }
    }

    /**
     * Deletes all success bundles
     * Note: the response will be notified by socket message
     * @param request   {@link HttpServletRequest}
     * @param response  {@link HttpServletResponse}
     */
    @DELETE
    @Path("/all/success")
    @Produces("application/json")
    public Response deleteAllSuccess(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Locale locale    = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting all success bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.success.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {SUCCESS, SUCCESS_WITH_WARNINGS});
                final BundleDeleteResult bundleDeleteResult = this.bundleAPI.deleteAllBundles(initData.getUser(), statuses);
                sendDeleteResultsMessage(initData, locale, bundleDeleteResult);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteAllSuccess, couldn't delete the success bundles, exception message: " + e.getMessage(), e);
                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return Response.ok(new ResponseEntityView(
                "Removing bundles in a separated process, the result of the operation will be notified")).build();
    } // deleteAllSuccess.


    @Path("/_download/{bundleId}")
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public final Response downloadBundle(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("bundleId") final String bundleId) {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet("publishing-queue")
                        .init();
        try {
            final Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
            if (!UtilMethods.isSet(bundle)) {
                throw new DoesNotExistException("Bundle with ID: " + bundleId + " not found");
            }
            final File bundleFile = new File(
                    ConfigUtils.getBundlePath() + File.separator + bundle.getId() + ".tar.gz");
            if (!bundleFile.exists()) {
                throw new DoesNotExistException(
                        "The bundle has not been generated for the provided bundle ID: "
                                + bundleId);
            }
            final String bundleName = bundle.getName().replaceAll("[^\\w.-]", "_");
            response.setHeader( "Content-Disposition", "attachment; filename=" +bundleName  +"-"+ bundle.getId() + ".tar.gz" );
            return Response.ok(bundleFile, "application/x-tgz").build();
        }catch (DoesNotExistException e){
            Logger.error(this,e.getMessage());
            return ExceptionMapperUtil.createResponse("",e.getMessage(),Response.Status.NOT_FOUND);
        }catch (Exception e){
            Logger.error(this,e.getMessage());
            return ExceptionMapperUtil.createResponse("",e.getMessage(),Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Path("/_generate")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public final void generateBundle(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                        @Suspended final AsyncResponse asyncResponse,
                                       final GenerateBundleForm form) {


        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet("publishing-queue")
                        .init();
        final User user = initData.getUser();
        try {
            final Bundle bundle = APILocator.getBundleAPI().getBundleById(form.bundleId);
            if (!UtilMethods.isSet(bundle)) {
                throw new DoesNotExistException("Bundle with ID: " + form.bundleId + " not found");
            }
            
            //set Filter to the bundle
            final FilterDescriptor filter = APILocator.getPublisherAPI().getFilterDescriptorByKey(form.filterKey);
            bundle.setFilterKey(filter.getKey());
            bundle.setOwner(user.getUserId());
            //set ForcePush value of the filter to the bundle
            bundle.setForcePush(
                    (boolean) filter.getFilters().getOrDefault(FilterDescriptor.FORCE_PUSH_KEY,false));
            bundle.setOperation(form.operation.ordinal());
            //Update Bundle
            APILocator.getBundleAPI().updateBundle(bundle);

            //Generate the bundle file for this given operation

            final BundleGenerator generator = new BundleGenerator(bundle, user,asyncResponse);

            final DotSubmitter submitter =
                    DotConcurrentFactory.getInstance().getSubmitter("generateBundle",
                            new DotConcurrentFactory.SubmitterConfigBuilder().poolSize(2)
                                    .maxPoolSize(4).queueCapacity(500).build()
                    );
            submitter.submit(generator);
            final String bundleName = bundle.getName().replaceAll("[^\\w.-]", "_");
            response.setContentType( "application/x-tgz" );
            response.setHeader( "Content-Disposition", "attachment; filename=" + bundleName  +"-"+ bundle.getId() + ".tar.gz" );
        }catch (DoesNotExistException e){
            Logger.error(this,e.getMessage());
            asyncResponse.resume(ExceptionMapperUtil.createResponse("",e.getMessage(),Response.Status.NOT_FOUND));
        }catch (Exception e){
            Logger.error(this,e.getMessage());
            asyncResponse.resume(ExceptionMapperUtil.createResponse("",e.getMessage(),Response.Status.INTERNAL_SERVER_ERROR));
        }
        

    } // uploadBundleSync.
    
    
    class BundleGenerator implements Runnable {


        public BundleGenerator(final Bundle bundle,  final User user, final AsyncResponse asyncResponse) {
            super();
            this.bundle = bundle;
            this.user = user;
            this.asyncResponse = asyncResponse;
            
        }

        File bundleFile;
        final Bundle bundle;
        final User user;
        final AsyncResponse asyncResponse;
        
        @Override
        public void run() {
            bundleFile = APILocator.getBundleAPI().generateTarGzipBundleFile(bundle);
            
            final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();

            final String message = "Bundle <strong>" + bundle.getName() + "</strong> can be downloaded here: <a href='/api/bundle/_download/" + bundle.getId() + "'>download</a>";

            final SystemMessage systemMessage = systemMessageBuilder.setMessage(message).setType(MessageType.SIMPLE_MESSAGE)
                            .setSeverity(MessageSeverity.SUCCESS).setLife(1000*60*12).create();

            SystemMessageEventUtil.getInstance().pushMessage(systemMessage, ImmutableList.of(user.getUserId()));

            final Response response = Response.ok(bundleFile, MediaType.APPLICATION_OCTET_STREAM).build();

            this.asyncResponse.resume(response);
        }
    }
    
    @Path("/sync")
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response uploadBundleSync(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       FormDataMultiPart multipart) throws DotPublisherException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requireLicense(true)
                .init();

        for (final BodyPart part : multipart.getBodyParts()) {

            try(InputStream inputStream = part.getEntity() instanceof InputStream ?
                    (InputStream)  part.getEntity()
                    : Try.of(() -> part.getEntityAs(InputStream.class)).getOrNull()) {

                final String fileName = this.validateInputsAndGetFileName(part, inputStream);
                if (fileName == null) {

                    continue;
                }

                final String bundleName = BundlerUtil.sanitizeBundleName(fileName);
                final String bundlePath = ConfigUtils.getBundlePath() + File.separator;

                FileUtil.writeToFile(inputStream, bundlePath + bundleName);

                final String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
                final String endpointId   = initData.getUser().getUserId();
                response.setContentType("text/html; charset=utf-8");
                PublishAuditStatus previousStatus = PublishAuditAPI
                        .getInstance().updateAuditTable(endpointId, endpointId, bundleFolder);

                final PublisherConfig config = !previousStatus.getStatus().equals(Status.PUBLISHING_BUNDLE)?
                        new PushPublisherJob().processBundle(bundleName, previousStatus): null;

                final String finalStatus = config != null ?
                        config.getPublishAuditStatus().getStatus().name():
                        Status.RECEIVED_BUNDLE.name();

                return Response.ok(ImmutableMap.of("bundleName", bundleName, "status", finalStatus))
                        .build();
            } catch (IOException e) {
                Logger.error(this, "Unable to import Bundle", e);
                throw new ServerErrorException("Unable to import Bundle", Response.Status.INTERNAL_SERVER_ERROR, e);
            }

        }

        return Response.ok().build();
    } // uploadBundleSync.

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final void uploadBundleAsync(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
                                       @Suspended final AsyncResponse asyncResponse,
                                        FormDataMultiPart multipart) throws DotPublisherException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requireLicense(true)
                .init();

        for (final BodyPart part : multipart.getBodyParts()) {

            try(InputStream inputStream = part.getEntity() instanceof InputStream ? (InputStream) part
                    .getEntity()
                    : Try.of(() -> part.getEntityAs(InputStream.class)).getOrNull()) {

                final String fileName = this.validateInputsAndGetFileName(part, inputStream);
                if (fileName == null) {
                    continue;
                }

                final String bundleName = BundlerUtil.sanitizeBundleName(fileName);
                final String bundlePath = ConfigUtils.getBundlePath() + File.separator;

                FileUtil.writeToFile(inputStream, bundlePath + bundleName);

                final String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
                final String endpointId = initData.getUser().getUserId();
                response.setContentType("text/html; charset=utf-8");
                final PublishAuditStatus previousStatus = PublishAuditAPI
                        .getInstance().updateAuditTable(endpointId, endpointId, bundleFolder);

                if (!previousStatus.getStatus().equals(Status.PUBLISHING_BUNDLE)) {

                    final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance()
                            .getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
                    dotSubmitter.execute(() -> {

                        final PublisherConfig config = new PushPublisherJob()
                                .processBundle(bundleName, previousStatus);

                        final String finalStatus =
                                config != null ? config.getPublishAuditStatus().getStatus().name()
                                        : Status.RECEIVED_BUNDLE.name();

                        asyncResponse.resume(
                                Response.ok(ImmutableMap.of("bundleName", bundleName, "status", finalStatus)).build()
                        );
                    });
                } else {

                    asyncResponse.resume(
                            Response.ok(ImmutableMap.of("bundleName", bundleName, "status", Status.RECEIVED_BUNDLE.name())).build()
                    );
                }

                return;
            } catch (IOException e) {
                Logger.error(this, "Unable to import Bundle", e);
                asyncResponse.resume(ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
            }
        }

        asyncResponse.resume(
                Response.ok(Response.ok().build()).build()
        );
    } // uploadBundleAsync.

    @Path("/{bundleId}/manifest")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response downloadManifest(@PathParam("bundleId") final String bundleId,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotDataException, IOException {

        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
        if (!UtilMethods.isSet(bundle)) {
            throw new DoesNotExistException("Bundle with ID: " + bundleId + " not found");
        }

        final File bundleTarGzipFile = TarGzipBundleOutput.getBundleTarGzipFile(bundleId);

        if (!bundleTarGzipFile.exists()) {
            throw new DoesNotExistException("The bundle not exists: " + bundleId);
        }

        final Optional<Reader> manifestInputStreamOptional = ManifestUtil.getManifestInputStream(bundleTarGzipFile);

        if (manifestInputStreamOptional.isEmpty()) {
                throw new DoesNotExistException("Manifest not exists in bundle: " + bundleId);
        }

        final String manifestFileName = String.format("manifest_%s.csv", bundleId);

        final ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                .fileName(manifestFileName)
                .build();

        return javax.ws.rs.core.Response
                .ok( (StreamingOutput) output -> {
                    try (final Reader reader = manifestInputStreamOptional.get()) {
                        IOUtils.copy(reader, output);
                        output.flush();
                    }
                })
                .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header( "Content-Disposition", contentDisposition ).build();
    }

    private String validateInputsAndGetFileName(final BodyPart part, final InputStream inputStream) {

        if (inputStream == null) {

            Logger.warn(this, () -> "Skipping part since input stream is null on body part: " + part);
            return null;
        }

        final ContentDisposition meta = part.getContentDisposition();
        if (meta == null) {

            Logger.warn(this, () -> "Skipping part since Content Disposition is null on body part: " + part);
            return null;
        }

        final String fileName = meta.getFileName();
        if (UtilMethods.isNotSet(fileName) || fileName.startsWith(".") || fileName
                .contains("/.")) {

            Logger.warn(this, () -> "Skipping part since file Name is invalid on body part: " + part);
            return null;
        }

        return fileName;
    }

}
