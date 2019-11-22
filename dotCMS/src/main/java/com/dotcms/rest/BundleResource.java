package com.dotcms.rest;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.rest.param.ISODateParam;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.LocaleUtil;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.dotcms.publisher.business.PublishAuditStatus.Status.*;

@Path("/bundle")
public class BundleResource {

    public  static final String BUNDLE_THREAD_POOL_SUBMITTER_NAME = "bundlepolling";
    private final WebResource            webResource            = new WebResource();
    private final BundleAPI              bundleAPI              = APILocator.getBundleAPI();
    private final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();


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
                                               final DeleteBundlesByIdentifierForm  deleteBundlesByIdentifierForm) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.info(this, "Deleting the bundles: " + deleteBundlesByIdentifierForm.getIdentifiers()
                + " by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        final Locale locale = LocaleUtil.getLocale(request);

        dotSubmitter.execute(() -> {

            try {

                this.deleteBundleByIdentifier(deleteBundlesByIdentifierForm, initData);
                this.sendSuccessDeleteBundleMessage(deleteBundlesByIdentifierForm.getIdentifiers().size(), initData, locale);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteBundlesByIdentifiers, couldn't delete the identifiers: "
                                + deleteBundlesByIdentifierForm.getIdentifiers() +
                                ", exception message: " + e.getMessage(), e);

                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return Response.ok(new ResponseEntityView(
                "Removing bundles in a separated process, the result of the operation will be notified")).build();
    } // deleteBundlesByIdentifiers.

    private void sendErrorDeleteBundleMessage(final InitDataObject initData,
                                              final Locale locale,
                                              final Exception e) {

        final String message = Try.of(()->LanguageUtil.get(locale, "bundle.deleted.error.msg", e.getMessage()))
                .onFailure(ex -> Logger.error(this, e.getMessage()))
                .getOrElse("An error occurred deleting bundles, please check the log, error message: " + e.getMessage());

        this.systemMessageEventUtil.pushMessage(new SystemMessageBuilder()
                .setMessage(message)
                .setLife(DateUtil.TEN_SECOND_MILLIS)
                .setSeverity(MessageSeverity.ERROR).create(), Collections.singletonList(initData.getUser().getUserId()));
    }

    private void sendSuccessDeleteBundleMessage(final int bundleDeletesSize,
                                                final InitDataObject initData,
                                                final Locale locale) {

        final String message = Try.of(()->LanguageUtil.get(locale, "bundle.deleted.success.msg", bundleDeletesSize))
                .onFailure(e -> Logger.error(this, e.getMessage()))
                .getOrElse(bundleDeletesSize + " Bundles Deleted Successfully");

        this.systemMessageEventUtil.pushMessage(new SystemMessageBuilder()
                .setMessage(message)
                .setLife(DateUtil.SEVEN_SECOND_MILLIS)
                .setSeverity(MessageSeverity.INFO).create(), Collections.singletonList(initData.getUser().getUserId()));
    }

    // one transaction for each bundle
    private void deleteBundleByIdentifier(final DeleteBundlesByIdentifierForm deleteBundlesByIdentifierForm, final InitDataObject initData) throws DotDataException {

        for (final String bundleId : deleteBundlesByIdentifierForm.getIdentifiers()) {

            this.bundleAPI.deleteBundleAndDependencies(bundleId, initData.getUser());
        }
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

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        final Locale locale = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting the bundles older than: " + olderThan
                + " by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final int bundleDeletedSize =
                        this.bundleAPI.deleteBundleAndDependenciesOlderThan(olderThan, initData.getUser()).size();

                this.sendSuccessDeleteBundleMessage(bundleDeletedSize, initData, locale);
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

        final Locale locale = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting all bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.all.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {FAILED_TO_SEND_TO_ALL_GROUPS, FAILED_TO_SEND_TO_SOME_GROUPS,
                                FAILED_TO_BUNDLE, FAILED_TO_SENT, FAILED_TO_PUBLISH, SUCCESS});

                final int bundleDeletedSize = this.bundleAPI.deleteAllBundles(initData.getUser(), statuses).size();
                this.sendSuccessDeleteBundleMessage(bundleDeletedSize, initData, locale);
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

        final Locale locale = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting all failed bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.fail.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {FAILED_TO_SEND_TO_ALL_GROUPS, FAILED_TO_SEND_TO_SOME_GROUPS,
                                FAILED_TO_BUNDLE, FAILED_TO_SENT, FAILED_TO_PUBLISH});
                final int bundleDeletedSize = this.bundleAPI.deleteAllBundles(initData.getUser(), statuses).size();
                this.sendSuccessDeleteBundleMessage(bundleDeletedSize, initData, locale);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteAllFail, couldn't delete the fail bundles, exception message: " + e.getMessage(), e);
                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return Response.ok(new ResponseEntityView(
                "Removing bundles in a separated process, the result of the operation will be notified")).build();
    } // deleteAllFail.

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

        final Locale locale = LocaleUtil.getLocale(request);

        Logger.info(this, "Deleting all success bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.success.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {SUCCESS});
                final int bundleDeletedSize = this.bundleAPI.deleteAllBundles(initData.getUser(), statuses).size();
                this.sendSuccessDeleteBundleMessage(bundleDeletedSize, initData, locale);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteAllSuccess, couldn't delete the success bundles, exception message: " + e.getMessage(), e);
                this.sendErrorDeleteBundleMessage(initData, locale, e);
            }
        });

        return Response.ok(new ResponseEntityView(
                "Removing bundles in a separated process, the result of the operation will be notified")).build();
    } // deleteAllSuccess.

}
