package com.dotcms.rest;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.rest.param.DateParam;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;

import static com.dotcms.publisher.business.PublishAuditStatus.Status.*;

@Path("/bundle")
public class BundleResource {

    public  static final String BUNDLE_THREAD_POOL_SUBMITTER_NAME = "bundlepolling";
    private final WebResource     webResource     = new WebResource();
    private final BundleAPI       bundleAPI       = APILocator.getBundleAPI();

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
     * Note: The process could be heavy, so it is handle by async response
     * @param request
     * @param response
     * @param asyncResponse response is async
     * @param deleteBundlesByIdentifierForm
     */
	@DELETE
    @Path("/ids")
    @Produces("application/json")
    public void deleteBundlesByIdentifiers(@Context   final HttpServletRequest request,
                                           @Context   final HttpServletResponse response,
                                           @Suspended final AsyncResponse asyncResponse,
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
        dotSubmitter.execute(() -> {

            Response restResponse = null;

            try {

                this.deleteBundleByIdentifier(deleteBundlesByIdentifierForm, initData);
                restResponse = Response.ok(new ResponseEntityView(
                        CollectionsUtils.map("bundlesDeleted", deleteBundlesByIdentifierForm.getIdentifiers()))
                        ).build();
                asyncResponse.resume(restResponse);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteBundlesByIdentifiers, couldn't delete the identifiers: "
                                + deleteBundlesByIdentifierForm.getIdentifiers() +
                                ", exception message: " + e.getMessage(), e);
                restResponse = ResponseUtil.mapExceptionResponse(e);
                asyncResponse.resume(restResponse);
            }
        });
    } // deleteBundlesByIdentifiers.

    // one transaction for each bundle
    private void deleteBundleByIdentifier(final DeleteBundlesByIdentifierForm deleteBundlesByIdentifierForm, final InitDataObject initData) throws DotDataException {

        for (final String bundleId : deleteBundlesByIdentifierForm.getIdentifiers()) {

            this.bundleAPI.deleteBundleAndDependencies(bundleId, initData.getUser());
        }
    }

    /**
     * Deletes bundles older than a date. (unsent are not going to be deleted)
     * Note: The process could be heavy, so it is handle by async response
     * @param request
     * @param response
     * @param asyncResponse response is async
     * @param olderThan
     */
    @DELETE
    @Path("/olderthan/{olderThan}")
    @Produces("application/json")
    public void deleteBundlesOlderThan(@Context   final HttpServletRequest request,
                                       @Context   final HttpServletResponse response,
                                       @Suspended final AsyncResponse asyncResponse,
                                       @PathParam("olderThan") final DateParam olderThan) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.info(this, "Deleting the bundles older than: " + olderThan
                + " by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            Response restResponse = null;

            try {

                restResponse = Response.ok(new ResponseEntityView(CollectionsUtils.map("bundlesDeleted",
                        this.bundleAPI.deleteBundleAndDependenciesOlderThan(olderThan, initData.getUser())))).build();
                asyncResponse.resume(restResponse);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteBundlesByIdentifiers, couldn't delete bundles older than: " + olderThan +
                                ", exception message: " + e.getMessage(), e);
                restResponse = ResponseUtil.mapExceptionResponse(e);
                asyncResponse.resume(restResponse);
            }
        });

    } // deleteBundlesOlderThan.

    /**
     * Deletes all failed and succeed bundles
     * Note: The process could be heavy, so it is handle by async response
     * @param request
     * @param response
     * @param asyncResponse response is async
     */
    @DELETE
    @Path("/all")
    @Produces("application/json")
    public void deleteAll(@Context   final HttpServletRequest request,
                          @Context   final HttpServletResponse response,
                          @Suspended final AsyncResponse asyncResponse) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.info(this, "Deleting all bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            Response restResponse = null;

            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.all.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {FAILED_TO_SEND_TO_ALL_GROUPS, FAILED_TO_SEND_TO_SOME_GROUPS,
                                FAILED_TO_BUNDLE, FAILED_TO_SENT, FAILED_TO_PUBLISH, SUCCESS});
                restResponse = Response.ok(new ResponseEntityView(CollectionsUtils.map("bundlesDeleted",
                        this.bundleAPI.deleteAllBundles(initData.getUser(), statuses)
                ))).build();
                asyncResponse.resume(restResponse);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteAll, couldn't delete bundles, exception message: " + e.getMessage(), e);
                restResponse = ResponseUtil.mapExceptionResponse(e);
                asyncResponse.resume(restResponse);
            }
        });
    } // deleteAll.

    /**
     * Deletes all failed  bundles
     * Note: The process could be heavy, so it is handle by async response
     * @param request
     * @param response
     * @param asyncResponse response is async
     */
    @DELETE
    @Path("/all/fail")
    @Produces("application/json")
    public void deleteAllFail(@Context   final HttpServletRequest request,
                              @Context   final HttpServletResponse response,
                              @Suspended final AsyncResponse asyncResponse) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.info(this, "Deleting all failed bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            Response restResponse = null;
            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.fail.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {FAILED_TO_SEND_TO_ALL_GROUPS, FAILED_TO_SEND_TO_SOME_GROUPS,
                                FAILED_TO_BUNDLE, FAILED_TO_SENT, FAILED_TO_PUBLISH});
                restResponse = Response.ok(new ResponseEntityView(CollectionsUtils.map("bundlesDeleted",
                                     this.bundleAPI.deleteAllBundles(initData.getUser(), statuses)
                                ))).build();
                asyncResponse.resume(restResponse);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteAllFail, couldn't delete the fail bundles, exception message: " + e.getMessage(), e);
                restResponse = ResponseUtil.mapExceptionResponse(e);
                asyncResponse.resume(restResponse);
            }
        });
    } // deleteAllFail.

    /**
     * Deletes all success bundles
     * Note: The process could be heavy, so it is handle by async response
     * @param request
     * @param response
     * @param asyncResponse response is async
     */
    @DELETE
    @Path("/all/success")
    @Produces("application/json")
    public void deleteAllSuccess(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @Suspended final AsyncResponse asyncResponse) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();

        Logger.info(this, "Deleting all success bundles by the user: " + initData.getUser().getUserId());

        final DotSubmitter dotSubmitter = DotConcurrentFactory
                .getInstance().getSubmitter(BUNDLE_THREAD_POOL_SUBMITTER_NAME);
        dotSubmitter.execute(() -> {

            Response restResponse = null;
            try {

                final PublishAuditStatus.Status [] statuses = Config.getCustomArrayProperty("bundle.delete.success.statuses",
                        PublishAuditStatus.Status::valueOf, PublishAuditStatus.Status.class,
                        ()-> new PublishAuditStatus.Status[] {SUCCESS});
                restResponse = Response.ok(new ResponseEntityView(CollectionsUtils.map("bundlesDeleted",
                        this.bundleAPI.deleteAllBundles(initData.getUser(), statuses)
                ))).build();
                asyncResponse.resume(restResponse);
            } catch (DotDataException e) {

                Logger.error(this.getClass(),
                        "Exception on deleteSuccessFail, couldn't delete the success bundles, exception message: " + e.getMessage(), e);
                restResponse = ResponseUtil.mapExceptionResponse(e);
                asyncResponse.resume(restResponse);
            }
        });
    } // deleteAllSuccess.

}
