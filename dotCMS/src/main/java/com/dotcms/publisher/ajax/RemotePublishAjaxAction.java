package com.dotcms.publisher.ajax;

import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisher;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.publishing.manifest.CSVManifestBuilder;
import com.dotcms.publishing.manifest.CSVManifestReader;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.publishing.manifest.ManifestReaderFactory;
import com.dotcms.publishing.manifest.ManifestReason;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.PushPublisherJob;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

/**
 * This class handles the different action mechanisms related to the handling of 
 * bundles during the Push Publishing process. You can perform several actions 
 * on a bundle:
 * <ul>
 * 	<li>Generate a bundle by selecting a content for Push Publish.</li>
 * 	<li>Adding a content to a specific bundle.</li>
 * 	<li>Download or upload a given bundle.</li>
 * 	<li>Re-try a bundle that failed previously.</li>
 * </ul>
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since Dec 17, 2012
 *
 */
public class RemotePublishAjaxAction extends AjaxAction {

	public static final String DIALOG_ACTION_EXPIRE="expire";
	public static final String DIALOG_ACTION_PUBLISH="publish";
	public static final String DIALOG_ACTION_PUBLISH_AND_EXPIRE="publishexpire";
    public static final String ADD_ALL_CATEGORIES_TO_BUNDLE_KEY = "CAT";
    public static final String STANDARD_DATE_FORMAT = "yyyy-MM-dd-H-m";

    @Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		return;
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String cmd = map.get( "cmd" );
		Method dispatchMethod = null;

		User user = getUser();

		try{
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null) {
				String userName = map.get("u") !=null
					? map.get("u")
						: map.get("user") !=null
							? map.get("user")
								: null;

				String password = map.get("p") !=null
					? map.get("p")
							: map.get("passwd") !=null
								? map.get("passwd")
									: null;

				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
                //Set the logged user in order to make it available from this action using the getUser() method
                if ( user != null ) {
                    setUser( user );
                }

				if(user==null) {
				    setUser(request);
	                user = getUser();
				}
				if(user==null){
					response.sendError(401);
					return;
				}
			}
		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage());
			response.sendError(401);
			return;
		}

		if(null!=cmd){
			try {
				dispatchMethod = this.getClass().getMethod(cmd, new Class[]{HttpServletRequest.class, HttpServletResponse.class});
			} catch (Exception e) {
				try {
					dispatchMethod = this.getClass().getMethod("action", new Class[]{HttpServletRequest.class, HttpServletResponse.class});
				} catch (Exception e1) {
					Logger.error(this.getClass(), "Trying to get method:" + cmd);
					Logger.error(this.getClass(), e1.getMessage(), e1.getCause());
					throw new DotRuntimeException(e1.getMessage());
				}
			}
			try {
				dispatchMethod.invoke(this, new Object[]{request,response});
			} catch (Exception e) {
				Logger.error(this.getClass(), "Trying to invoke method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				throw new DotRuntimeException(e.getMessage(),e);
			}
		}

	}

	/**
     * Send to the publisher queue a list of assets for a given Operation (Publish/Unpublish) and {@link Environment Environment}
     *
     * @param request  HttpRequest
     * @param response HttpResponse
     * @throws WorkflowActionFailureException If fails adding the content for Publish
     * @see com.dotcms.publisher.business.PublisherQueueJob
     * @see Environment
     */
    public void publish ( HttpServletRequest request, HttpServletResponse response ) throws IOException, WorkflowActionFailureException {

        try {

        	PublisherAPI publisherAPI = PublisherAPI.getInstance();

            //Read the form values
            final String _assetId = request.getParameter( "assetIdentifier" );
            final String _contentPushPublishDate = request.getParameter( "remotePublishDate" );
            final String _contentPushPublishTime = request.getParameter( "remotePublishTime" );
            final String _contentPushExpireDate = request.getParameter( "remotePublishExpireDate" );
            final String _contentPushExpireTime = request.getParameter( "remotePublishExpireTime" );
            final String timezoneId = request.getParameter( "timezoneId" );
            final String _contentFilterDate = request.getParameter( "remoteFilterDate" );
            final String _iWantTo = request.getParameter( "iWantTo" );
            final String whoToSendTmp = request.getParameter( "whoToSend" );
            final List<String> whereToSend = Arrays.asList(whoToSendTmp.split(","));
            final List<Environment> envsToSendTo = new ArrayList<>();
            final String filterKey = request.getParameter("filterKey");
            final boolean forcePush = (boolean) APILocator.getPublisherAPI().getFilterDescriptorByKey(filterKey).getFilters().getOrDefault(
                    FilterDescriptor.FORCE_PUSH_KEY,false);



            // Lists of Environments to push to
            for (String envId : whereToSend) {
            	Environment e = APILocator.getEnvironmentAPI().findEnvironmentById(envId);

            	if(e!=null) {
            		envsToSendTo.add(e);
            	}
			}

            //Put the selected environments in session in order to have the list of the last selected environments
            request.getSession().setAttribute( WebKeys.SELECTED_ENVIRONMENTS + getUser().getUserId(), envsToSendTo );

            final TimeZone currentTimeZone =
                    UtilMethods.isSet(timezoneId) ? TimeZone.getTimeZone(timezoneId)
                            : APILocator.systemTimeZone();

            final Date publishDate = DateUtil
                    .convertDate(_contentPushPublishDate + "-" + _contentPushPublishTime,
                            currentTimeZone, STANDARD_DATE_FORMAT);

            List<String> ids;
            if ( _assetId.startsWith( "query_" ) ) { //Support for lucene queries

                String luceneQuery = _assetId.replace( "query_", "" );
                List<String> queries = new ArrayList<>();
                queries.add( luceneQuery );
                ids = PublisherUtil.getContentIds( queries );

            } else {

                String[] _assetsIds = _assetId.split( "," );//Support for multiple ids in the assetIdentifier parameter
                List<String> assetsIds = Arrays.asList( _assetsIds );

                ids = getIdsToPush( assetsIds, null, _contentFilterDate, new SimpleDateFormat(STANDARD_DATE_FORMAT) );
            }

            //Response map with the status of the addContents operation (error messages and counts )
            Map<String, Object> responseMap = null;

            if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH ) || _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE ) ) {
            	Bundle bundle = new Bundle(null, publishDate, null, getUser().getUserId(), forcePush,filterKey);
            	APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);

                responseMap = publisherAPI.addContentsToPublish( ids, bundle.getId(), publishDate, getUser() );
            }
            if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE ) || _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE ) ) {
                if ((UtilMethods.isSet( _contentPushExpireDate) && UtilMethods.isSet( _contentPushExpireTime.trim()))) {
                    final Date expireDate = DateUtil
                            .convertDate(_contentPushExpireDate + "-" + _contentPushExpireTime,
                                    currentTimeZone, STANDARD_DATE_FORMAT);

                    Bundle bundle = new Bundle(null, publishDate, expireDate, getUser().getUserId(), forcePush,filterKey);
                	APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);

                    responseMap = publisherAPI.addContentsToUnpublish( ids, bundle.getId(), expireDate, getUser() );
                }
            }

            //If we have errors lets return them in order to feedback the user
            if ( responseMap != null && !responseMap.isEmpty() ) {

                //Error messages
                JSONArray jsonErrors = new JSONArray( (ArrayList) responseMap.get( "errorMessages" ) );

                //Prepare the Json response
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put( "errorMessages", jsonErrors.toArray() );
                jsonResponse.put( "errors", responseMap.get( "errors" ) );
                jsonResponse.put( "total", responseMap.get( "total" ) );
                jsonResponse.put( "bundleId", responseMap.get( "bundleId" ) );

                //And send it back to the user
                response.getWriter().println( jsonResponse.toString() );
            }
        } catch ( Exception e ) {
            Logger.error( RemotePublishAjaxAction.class, e.getMessage(), e );
            response.sendError( HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error Publishing Bundle: " + e.getMessage() );
        }
    }

	/**
	 * Allows users to re-send either failed or successful bundles to the
	 * specified publishing environments. In order to do this, the bundle is
	 * sent once again to the publisher queue job which will try to remote
	 * publish it.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param response
	 *            - The {@link HttpServletResponse} object.
	 * @throws IOException
	 *             A proper response could not be sent back to the user.
	 * @throws DotPublisherException
	 *             An error occurred when retrieving bundle related information,
	 *             such as the pushed elements, status, etc.
	 * @throws LanguageException
	 *             An error occurred when internationalizing error messages.
	 */
    public void retry ( HttpServletRequest request, HttpServletResponse response ) throws IOException, DotPublisherException, LanguageException {
        PublisherAPI publisherAPI = PublisherAPI.getInstance();
        PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();

        // Read the parameters
        String bundlesIds = request.getParameter( "bundlesIds" );
		final String forcePush = request.getParameter("forcePush");//TODO: this option still lives in the retry dialog, need to remove??
		final boolean isForcePush = UtilMethods.isSet(forcePush) ? Boolean.valueOf(forcePush) : Boolean.FALSE;
		final String strategy = request.getParameter("deliveryStrategy");
		final DeliveryStrategy deliveryStrategy = "1".equals(strategy) ? DeliveryStrategy.ALL_ENDPOINTS
				: DeliveryStrategy.FAILED_ENDPOINTS;

		String[] ids = bundlesIds.split( "," );
        StringBuilder responseMessage = new StringBuilder();

        for ( String bundleId : ids ) {
            if ( bundleId.trim().isEmpty() ) {
                continue;
            }

            PublisherConfig basicConfig = new PublisherConfig();
            basicConfig.setId( bundleId );
            File bundleRoot = BundlerUtil.getBundleRoot( basicConfig.getName(), false );

            //Get the audit records related to this bundle
            PublishAuditStatus status = PublishAuditAPI.getInstance().getPublishAuditStatus( bundleId );
            String pojo_string = status.getStatusPojo().getSerialized();
            PublishAuditHistory auditHistory = PublishAuditHistory.getObjectFromString( pojo_string );

            //First we need to verify is this bundle is already in the queue job
            List<PublishQueueElement> foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
            if ( foundBundles != null && !foundBundles.isEmpty() ) {
                appendMessage( responseMessage, "publisher_retry.error.already.in.queue", bundleId, true );
                continue;
            }

            //We will be able to retry failed and successfully bundles
            if (!BundlerUtil.isRetryable(status)) {
                appendMessage( responseMessage, "publisher_retry.error.only.failed.publish", bundleId, true );
                continue;
            }

            //We need to check both Static and Push Publish for each bundle.
            //Checking static publish.

            File bundleStaticFile = new File(bundleRoot.getAbsolutePath() + PublisherConfig.STATIC_SUFFIX);
            if ( bundleStaticFile.exists() ) {

                File readBundle = new File(bundleStaticFile.getAbsolutePath() + File.separator + "bundle.xml");
                PublisherConfig readConfig = (PublisherConfig) BundlerUtil.readBundleMeta(readBundle);

                PublisherConfig configStatic = new PublisherConfig();
                configStatic.setId(bundleId);
                configStatic.setOperation(readConfig.getOperation());

                //Clean the number of tries, we want to try it again
                auditHistory.setNumTries( 0 );
                publishAuditAPI.updatePublishAuditStatus( configStatic.getId(), status.getStatus(), auditHistory, true );

                Publisher staticPublisher;

                try{

                    List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(bundleId);

                    for (Environment environment : environments) {

                        List<PublishingEndPoint> endPoints = APILocator.getPublisherEndPointAPI().findSendingEndPointsByEnvironment(environment.getId());
                        PublishingEndPoint targetEndpoint = endPoints.get(0);

                        //Processing AWS push retry
                        if (AWSS3Publisher.PROTOCOL_AWS_S3.equalsIgnoreCase(targetEndpoint.getProtocol())){
                            staticPublisher = new AWSS3Publisher();
                        }else{
                            //Processing static push retry
                            staticPublisher = new StaticPublisher();
                        }

                        staticPublisher.init(configStatic);
                        staticPublisher.process(null);
                    }

                    //Success...
                    appendMessage( responseMessage, "publisher_retry.success",
                        bundleId + PublisherConfig.STATIC_SUFFIX, false );
                } catch (DotPublishingException | DotDataException e){
                    Logger.error( this.getClass(), "Error trying to add bundle id: "
                        + bundleId + PublisherConfig.STATIC_SUFFIX + " to the Publisher.", e );
                    appendMessage( responseMessage, "publisher_retry.error.adding.to.queue",
                        bundleId + PublisherConfig.STATIC_SUFFIX, true );
                }
                continue;
            }

            //Checking push publish.
            /*
            Verify if the bundle exist and was created correctly..., meaning, if there is not a .tar.gz file is because
            something happened on the creation of the bundle.
             */
            File bundleFile = new File( ConfigUtils.getBundlePath() + File.separator + basicConfig.getId() + ".tar.gz" );
            if ( !bundleFile.exists() ) {
                Logger.warn( this.getClass(), "No Push Publish Bundle with id: " + bundleId + " found." );
                appendMessage( responseMessage, "publisher_retry.error.not.found", bundleId, true );
                continue;
            }

            if ( !BundlerUtil.bundleExists( basicConfig ) ) {
                Logger.error( this.getClass(), String.format("Bundle's tar.gzip file for %s not exists" , bundleId));
                appendMessage( responseMessage, "publisher_retry.error.not.descriptor.found", bundleId, true );
                continue;
            }

            try {

                final CSVManifestReader csvManifestReader = ManifestReaderFactory.INSTANCE
                        .createCSVManifestReader(basicConfig.getId());

                final String operation = csvManifestReader
                        .getMetadata(CSVManifestBuilder.OPERATION_METADATA_NAME);

                final PushPublisherConfig config = new PushPublisherConfig();
                config.setOperation(Operation.valueOf(operation));

                //We can not retry Received Bundles, just bundles that we are trying to send
                Boolean sending = sendingBundle( request, config, bundleId );
                if ( !sending ) {
                    appendMessage( responseMessage, "publisher_retry.error.cannot.retry.received", bundleId, true );
                    continue;
                }
                //Get the bundle
				Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
				if (null == bundle) {
					Logger.error(this.getClass(), "No Bundle with id: " + bundleId + " found.");
					appendMessage(responseMessage, "publisher_retry.error.not.found", bundleId, true);
					continue;
				}
                if (status.getStatus().equals(Status.SUCCESS) || status.getStatus()
                        .equals(Status.SUCCESS_WITH_WARNINGS)) {
					bundle.setForcePush(Boolean.TRUE);
				} else {
					bundle.setForcePush(isForcePush);
				}
                APILocator.getBundleAPI().updateBundle( bundle );

                //Clean the number of tries, we want to try it again
                auditHistory.setNumTries( 0 );
                publishAuditAPI.updatePublishAuditStatus( bundle.getId(), status.getStatus(), auditHistory, true );

                //Get the identifiers on this bundle
                HashSet<String> identifiers = new HashSet<>();
                final Collection<ManifestInfo> assets = csvManifestReader
                        .getAssets(ManifestReason.INCLUDE_BY_USER);

                if ( assets != null && !assets.isEmpty() ) {
                    for ( ManifestInfo asset : assets ) {
                        identifiers.add( asset.id() );
                    }
                }
                
                //Now depending of the operation lets add it to the queue job
                if ( config.getOperation().equals( PushPublisherConfig.Operation.PUBLISH ) ) {
					publisherAPI.addContentsToPublish(new ArrayList<>(identifiers), bundleId, new Date(),
							getUser(), deliveryStrategy);
				} else {
					publisherAPI.addContentsToUnpublish(new ArrayList<>(identifiers), bundleId, new Date(),
							getUser(), deliveryStrategy);
				}
                //Success...
                appendMessage( responseMessage, "publisher_retry.success", bundleId, false );
            } catch ( Exception e ) {
                Logger.error( this.getClass(), "Error trying to add bundle id: " + bundleId + " to the Publishing Queue.", e );
                appendMessage( responseMessage, "publisher_retry.error.adding.to.queue", bundleId, true );
            }
        }
        response.getWriter().println( responseMessage.toString() );
    }

    /**
     * Downloads a Bundle file for a given bundle id.
     *
     * @param request  HttpRequest
     * @param response HttpResponse
     * @throws IOException If fails sending back to the user response information
     */
    public void downloadBundle ( HttpServletRequest request, HttpServletResponse response ) throws IOException {
    	try {
			if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("publishing-queue", getUser())){
				response.sendError(401);
				return;
			}
		} catch (DotDataException e1) {
			Logger.error(RemotePublishAjaxAction.class,e1.getMessage(),e1);
			response.sendError(401);
			return;
		}
        Map<String, String> map = getURIParams();
        response.setContentType( "application/x-tgz" );

        String bid = map.get( "bid" );

        PublisherConfig config = new PublisherConfig();
        config.setId( bid );
        File bundleRoot = BundlerUtil.getBundleRoot( config );

        ArrayList<File> list = new ArrayList<>( 1 );
        list.add( bundleRoot );
        File bundle = new File( bundleRoot + File.separator + ".." + File.separator + config.getId() + ".tar.gz" );
        if ( !bundle.exists() ) {
            response.sendError( 500, "No Bundle Found" );
            return;
        }

        response.setHeader( "Content-Disposition", "attachment; filename=" + config.getId() + ".tar.gz" );
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream( Files.newInputStream(bundle.toPath()) );
            byte[] buf = new byte[4096];
            int len;

            while ( (len = in.read( buf, 0, buf.length )) != -1 ) {
                response.getOutputStream().write( buf, 0, len );
            }
        } catch ( Exception e ) {
            Logger.warn( this.getClass(), "Error Downloading Bundle.", e );
        } finally {
            try {
                in.close();
            } catch ( Exception ex ) {
                Logger.warn( this.getClass(), "Error Closing Stream.", ex );
            }
        }
        return;
    }

    /**
     * Generates and flush an Unpublish bundle for a given bundle id and operation (publish/unpublish)
     *
     * @param request  HttpRequest
     * @param response HttpResponse
     * @throws IOException If fails sending back to the user response information
     */
    public void downloadUnpushedBundle ( HttpServletRequest request, HttpServletResponse response )
            throws IOException, DotDataException {
    	try {
			if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("publishing-queue", getUser())){
				response.sendError(401);
				return;
			}
		} catch (DotDataException e1) {
			Logger.error(RemotePublishAjaxAction.class,e1.getMessage(),e1);
			response.sendError(401);
			return;
		}
        //Read the parameters
        final Map<String, String> map = getURIParams();
        final String bundleId = map.get( "bundleId" );
        final String paramOperation = map.get( "operation" );
        final String bundleFilter = UtilMethods.isSet(map.get("filterKey")) ? map.get("filterKey") : "";
        if ( bundleId == null || bundleId.isEmpty() ) {
            Logger.error( this.getClass(), "No Bundle Found with id: " + bundleId );
            response.sendError( 500, "No Bundle Found with id: " + bundleId );
            return;
        }

        //What we want to do with this bundle
        PushPublisherConfig.Operation operation = PushPublisherConfig.Operation.PUBLISH;
        if ( paramOperation != null && paramOperation.equalsIgnoreCase( "unpublish" ) ) {
            operation = PushPublisherConfig.Operation.UNPUBLISH;
        }

        final Bundle dbBundle = Try.of(()->APILocator.getBundleAPI().getBundleById(bundleId)).getOrElseThrow(e->new DotRuntimeException(e));
        
        
        final String bundleName = dbBundle.getName().replaceAll("[^\\w.-]", "_");
        
        
        File bundle;
        try {
            //set Filter to the bundle
            dbBundle.setFilterKey(bundleFilter);
            //set ForcePush value of the filter to the bundle
            dbBundle.setForcePush(
                    (boolean) APILocator.getPublisherAPI().getFilterDescriptorByKey(bundleFilter).getFilters().getOrDefault(FilterDescriptor.FORCE_PUSH_KEY,false));
            dbBundle.setOperation(operation.ordinal());
            //Update Bundle
            APILocator.getBundleAPI().updateBundle(dbBundle);

            //Generate the bundle file for this given operation

            bundle = APILocator.getBundleAPI().generateTarGzipBundleFile(dbBundle);
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error trying to generate bundle with id: " + bundleId, e );
            response.sendError( 500, "Error trying to generate bundle with id: " + bundleId );
            return;
        }

        response.setContentType( "application/x-tgz" );
        response.setHeader( "Content-Disposition", "attachment; filename=" + bundleName + "-" + bundle.getName() );
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream( Files.newInputStream(bundle.toPath()) );
            final byte[] buf = new byte[4096];
            int len;

            while ( (len = in.read( buf, 0, buf.length )) != -1 ) {
                response.getOutputStream().write( buf, 0, len );
            }
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error reading bundle stream for bundle id: " + bundleId, e );
            response.sendError( 500, "Error reading bundle stream for bundle id: " + bundleId );
        } finally {
            try {
                in.close();
            } catch ( Exception ex ) {
                Logger.error( this.getClass(), "Error closing Stream for bundle: " + bundleId, ex );
            }

            //Clean the just created bundle because on each download we will generate a new bundle file with a new id in order to avoid conflicts with ids
            final File bundleRoot = BundlerUtil.getBundleRoot( bundleId );
            final File compressedBundle = new File( ConfigUtils.getBundlePath() + File.separator + bundleId + ".tar.gz" );
            if ( compressedBundle.exists() ) {
                compressedBundle.delete();
                if ( bundleRoot.exists() ) {
                    com.liferay.util.FileUtil.deltree( bundleRoot );
                }
            }
        }
    }


    /**
     * Publish a given Bundle file
     *
     * @param request  HttpRequest
     * @param response HttpResponse
     * @throws FileUploadException If fails uploading the file
     * @throws IOException         If fails reading the given File content or sending back to the user a response
     */
    public void uploadBundle ( HttpServletRequest request, HttpServletResponse response ) throws FileUploadException, IOException{

        if(!Config.getBooleanProperty("ENABLE_OLD_BUNDLE_UPLOAD_ENDPOINT", false)){
            response.getWriter().println("Endpoint disabled, POST your bundle to: /api/bundle/sync or set DOT_ENABLE_OLD_BUNDLE_UPLOAD_ENDPOINT=true to use this old endpoint.");
        }




    	try {
			if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("publishing-queue", getUser())){
				response.sendError(401);
				return;
			}
		} catch (DotDataException e1) {
			Logger.error(RemotePublishAjaxAction.class,e1.getMessage(),e1);
			response.sendError(401);
			return;
		}

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload( factory );
        PrintWriter out = response.getWriter();
        PublishAuditStatus status;
        String bundleName = null;
        try {
            @SuppressWarnings ("unchecked")
            List<FileItem> items = upload.parseRequest( request );
            InputStream bundle = items.get( 0 ).getInputStream();
            bundleName = BundlerUtil.sanitizeBundleName(items.get(0).getName());
            String bundlePath = ConfigUtils.getBundlePath() + File.separator;
            String bundleFolder = bundleName.substring( 0, bundleName.indexOf( ".tar.gz" ) );
            String endpointId = getUser().getUserId();
            response.setContentType( "text/html; charset=utf-8" );
            status = PublishAuditAPI.getInstance().updateAuditTable( endpointId, endpointId, bundleFolder );

            // Write file on FS
            FileUtil.writeToFile( bundle, bundlePath + bundleName );

            if ( !status.getStatus().equals( Status.PUBLISHING_BUNDLE ) ) {
                PushPublisherJob.triggerPushPublisherJob(bundleName, status);
            }

            out.print( "<html><head><script>isLoaded = true;</script></head><body><textarea>{'status':'success'}</textarea></body></html>" );

        } catch ( DotPublisherException e ) {
            Logger.error(this, String.format("An error occurred when uploading bundle '%s'", bundleName), e);
            out.print( "<html><head><script>isLoaded = true;</script></head><body><textarea>{'status':'error'}</textarea></body></html>" );
        }

    }

    /**
     * Appends info messages to a main StringBuilder message for an easier display to the user
     *
     * @param responseMessage Response message to return to the user
     * @param messageKey      i18 key
     * @param bundleId        Current bundle
     * @param failure         True for failures, false otherwise
     * @throws LanguageException If fails using the i18 massage key
     */
    private void appendMessage ( StringBuilder responseMessage, String messageKey, String bundleId, Boolean failure ) throws LanguageException {

        String message = LanguageUtil.format( getUser().getLocale(), messageKey, new String[]{bundleId}, false );
        if ( responseMessage.length() > 0 ) {
            responseMessage.append( "<br>" );
        }
        if ( failure ) {
            responseMessage.append( "FAILURE: " ).append( message );
        } else {
            responseMessage.append( message );
        }
    }

    /**
     * Verifies what we were doing to the current bundle, it was received for this server?, or this server is trying to send it....,
     * we don't want to retry bundles we received.
     *
     * @param request
     * @param config
     * @return
     */
    private Boolean sendingBundle ( HttpServletRequest request, PushPublisherConfig config, String bundleId ) throws DotDataException {

        //Get the local address
        String remoteIP = request.getRemoteHost();
        int port = request.getLocalPort();
        if ( !UtilMethods.isSet( remoteIP ) ) {
            remoteIP = request.getRemoteAddr();
        }

        /*
         Getting the bundle end points in order to compare if this current server it is an end point or not.
         If it is is because we received this bundle as we were a targeted end point server.
         */

        List<Environment> environments = APILocator.getEnvironmentAPI().findEnvironmentsByBundleId(bundleId);

        for (Environment environment : environments) {

	        List<PublishingEndPoint> endPoints = APILocator.getPublisherEndPointAPI().findSendingEndPointsByEnvironment(environment.getId());
	        for ( PublishingEndPoint endPoint : endPoints ) {

	            //Getting the end point details
	            String endPointAddress = endPoint.getAddress();
	            String endPointPort = endPoint.getPort();

	            if ( endPointAddress.equals( remoteIP )
	                    && endPointPort.equals( String.valueOf( port ) ) ) {
	                return false;
	            }
	        }
        }

        return true;
    }

    /**
     * Adds to an specific given bundle a given asset.
     * <br/>If the given bundle does not exist a new onw will be created with that name
     *
     * @param request  HttpRequest
     * @param response HttpResponse
     */
    public void addToBundle ( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final String assetIdentifier = request.getParameter( "assetIdentifier" );
        final String contentFilterDate = request.getParameter( "remoteFilterDate" );
        final String bundleName = request.getParameter( "bundleName" );
        final String bundleId = (UtilMethods.isNotSet(request.getParameter( "bundleSelect" ))) ? request.getParameter( "bundleId" ):request.getParameter( "bundleSelect" );
        final String query = request.getParameter( "query" );
        
        try {
          
          // try by ID
          Bundle bundle=APILocator.getBundleAPI().getBundleById( bundleId );
          
          // if nothing, try by name
          if(bundle==null) {
            Optional<Bundle> optBundle = APILocator.getBundleAPI().getUnsendBundlesByName(getUser().getUserId(), bundleName, 1000, 0).stream().filter(b->b.getName().equalsIgnoreCase(bundleName)).findFirst() ;
            bundle = optBundle.isPresent() ? optBundle.get() : null;
          }
          
          // if nothing, then create a new one
          if(bundle==null) {
            bundle = new Bundle( (bundleName!=null)?bundleName:"Bundle:"  +new Date(), null, null, getUser().getUserId() );
            APILocator.getBundleAPI().saveBundle( bundle );
          }



            
            //Put the selected bundle in session in order to have last one selected
            request.getSession().setAttribute( WebKeys.SELECTED_BUNDLE + getUser().getUserId(), bundle );

            List<String> ids;
            // allow content to be added by query
            if(UtilMethods.isSet(query)) {
                String luceneQuery = query;
                List<String> queries = new ArrayList<>();
                queries.add( luceneQuery );
                ids = PublisherUtil.getContentIds( queries );
            }
            else if ( assetIdentifier.startsWith( "query_" ) ) { //Support for lucene queries in the assetIdentifier field (legacy support)

                String luceneQuery = assetIdentifier.replace( "query_", "" );
                List<String> queries = new ArrayList<>();
                queries.add( luceneQuery );
                ids = PublisherUtil.getContentIds( queries );

            } else {

                String[] assetsIds = assetIdentifier.split( "," );//Support for multiple ids in the assetIdentifier parameter
                List<String> assetsIdsList = Arrays.asList( assetsIds );

                ids = getIdsToPush( assetsIdsList,bundle.getId(), contentFilterDate, new SimpleDateFormat(STANDARD_DATE_FORMAT) );
            }

            Map<String, Object> responseMap = publisherAPI.saveBundleAssets( ids, bundle.getId(), getUser() );

            //If we have errors lets return them in order to feedback the user
            if ( responseMap != null && !responseMap.isEmpty() ) {

                //Error messages
                JSONArray jsonErrors = new JSONArray( (ArrayList) responseMap.get( "errorMessages" ) );

                //Prepare the Json response
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put( "errorMessages", jsonErrors.toArray() );
                jsonResponse.put( "errors", responseMap.get( "errors" ) );
                jsonResponse.put( "total", responseMap.get( "total" ) );
                jsonResponse.put( "bundleId", responseMap.get( "bundleId" ) );
                //And send it back to the user
                response.getWriter().println( jsonResponse.toString() );
            }
        } catch(DotPublisherException e){
            JSONObject jsonResponse = new JSONObject();

            try {
                jsonResponse.put( "errors", e.getMessage() );
            } catch (JSONException e1) {
                sendGeneralError(response, e);
            }

            //And send it back to the user
            response.getWriter().println( jsonResponse.toString() );
        }catch ( Exception e ) {
            sendGeneralError(response, e);
        }
    }

    /**
     * 
     * @param response
     * @param e
     * @throws IOException
     */
    private static void sendGeneralError( HttpServletResponse response, Throwable e) throws IOException {
        Logger.error( RemotePublishAjaxAction.class, e.getMessage(), e );
        response.sendError( HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error Adding content to Bundle: " + e.getMessage() );
    }

    /**
     * Updates the assets in the given bundle with the publish/expire dates and destination environments and set them ready to be pushed
     *
     * @param request  HttpRequest
     * @param response HttpResponse
     * @throws WorkflowActionFailureException If fails trying to Publish the bundle contents
     */
    public void pushBundle(final HttpServletRequest request, final HttpServletResponse response) throws WorkflowActionFailureException, IOException {
        response.setContentType("text/plain");
        final PublisherAPI publisherAPI = PublisherAPI.getInstance();
        final BundleAPI bundleAPI = APILocator.getBundleAPI();
        final EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        // Read the form values
        final String bundleId = request.getParameter( "assetIdentifier" );
        final String contentPushPublishDate = request.getParameter( "remotePublishDate" );
        final String contentPushPublishTime = request.getParameter( "remotePublishTime" );
        final String contentPushExpireDate = request.getParameter( "remotePublishExpireDate" );
        final String contentPushExpireTime = request.getParameter( "remotePublishExpireTime" );
        final String iWantTo = request.getParameter( "iWantTo" );
        final String whoToSendTmp = request.getParameter( "whoToSend" );
        final String filterKey = request.getParameter("filterKey");
        final String timezoneId = request.getParameter( "timezoneId" );

        try {
            final boolean forcePush = (boolean) APILocator.getPublisherAPI().getFilterDescriptorByKey(filterKey).getFilters().getOrDefault(FilterDescriptor.FORCE_PUSH_KEY,false);
            final List<String> whereToSend = Arrays.asList(whoToSendTmp.split(StringPool.COMMA));
            final List<Environment> envsToSendTo = new ArrayList<>();

            // Lists of Environments to push to
            for (final String envId : whereToSend) {
            	final Environment environment = environmentAPI.findEnvironmentById(envId);
            	if (environment != null && permissionAPI.doesUserHavePermission(environment, PermissionAPI.PERMISSION_USE, getUser())) {
            		envsToSendTo.add(environment);
            	}
			}

            if(envsToSendTo.isEmpty()) {
                Logger.warn(this.getClass(), "Push Publishing environment(s) not found - looking for :" + whereToSend);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            final TimeZone currentTimeZone =
                    UtilMethods.isSet(timezoneId) ? TimeZone.getTimeZone(timezoneId)
                            : APILocator.systemTimeZone();

            final Date publishDate = DateUtil
                    .convertDate(contentPushPublishDate + "-" + contentPushPublishTime,
                            currentTimeZone, STANDARD_DATE_FORMAT);

            //Put the selected environments in session in order to have the list of the last selected environments
            request.getSession().setAttribute( WebKeys.SELECTED_ENVIRONMENTS + getUser().getUserId(), envsToSendTo );
            //Clean up the selected bundle
            request.getSession().removeAttribute( WebKeys.SELECTED_BUNDLE + getUser().getUserId() );

            final Bundle bundle = bundleAPI.getBundleById(bundleId);
            bundle.setForcePush(forcePush);
            bundle.setFilterKey(filterKey);
            bundleAPI.saveBundleEnvironments(bundle, envsToSendTo);

            if ( iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH )) {
            	bundle.setPublishDate(publishDate);
                bundleAPI.updateBundle(bundle);
             	publisherAPI.publishBundleAssets(bundle.getId(), publishDate);
            } else if (iWantTo.equals(RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE) && UtilMethods.isSet(contentPushExpireDate) && UtilMethods.isSet(contentPushExpireTime)) {
                final Date expireDate = DateUtil
                        .convertDate(contentPushExpireDate + "-" + contentPushExpireTime,
                                currentTimeZone, STANDARD_DATE_FORMAT);
                bundle.setExpireDate(expireDate);
                bundleAPI.updateBundle(bundle);
                publisherAPI.unpublishBundleAssets(bundle.getId(), expireDate);
            } else if (iWantTo.equals(RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE) && UtilMethods.isSet(contentPushExpireDate) && UtilMethods.isSet(contentPushExpireTime)) {
                final Date expireDate = DateUtil
                        .convertDate(contentPushExpireDate + "-" + contentPushExpireTime,
                                currentTimeZone, STANDARD_DATE_FORMAT);
                bundle.setPublishDate(publishDate);
                bundle.setExpireDate(expireDate);
                bundleAPI.updateBundle(bundle);
                publisherAPI.publishAndExpireBundleAssets(bundle.getId(), publishDate, expireDate, getUser());
            }
            final Map<String, Object> dataMap = Map.of("deliveryStrategy", DeliveryStrategy.ALL_ENDPOINTS);
            publisherAPI.firePublisherQueueNow(dataMap);
        } catch (final Exception e) {
            final String errorMsg = String.format("An error occurred when trying to '%s' Bundle ID '%s' to env IDs [ " +
                                                          "%s ]: %s", iWantTo, bundleId, whoToSendTmp, e.getMessage());
            Logger.error(RemotePublishAjaxAction.class, errorMsg, e);
            response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

	/**
	 * Takes the list of IDs that the user wants to push to the destination
	 * environment, and tries to determine their type of asset: Language, User,
	 * OSGi, folder, etc. This allows the process to verify that the selected
	 * assets are actually pusheable content and the user has permission to push
	 * them.
	 * 
	 * @param assetIds
	 *            - The list of IDs selected by the user.
	 * @param bundleId
	 *            - The ID of the bundle that will contain the selected assets.
	 * @param _contentFilterDate
	 *            - A filtering date (required only when pushing a user).
	 * @param dateFormat
	 *            - The date format (required only when pushing a user).
	 * @return The list of valid pusheable assets.
	 * @throws ParseException
	 *             An error occurred when parsing the date format.
	 * @throws DotDataException
	 *             An error occurred when retrieving informaiton from the
	 *             database.
	 */
    private List<String> getIdsToPush ( List<String> assetIds, String bundleId, String _contentFilterDate, SimpleDateFormat dateFormat )
            throws ParseException, DotDataException {

    	List<String> ids = new ArrayList<>();

        for ( String assetId : assetIds ) {

            if ( assetId != null && !assetId.trim().isEmpty() ) {

                if ( ids.contains( assetId ) ) {
                    continue;
                }

                // check for the categories
                if ( assetId.contains( "user_" ) || assetId.contains( "users_" ) ) {//Trying to publish users
                    //If we are trying to push users a filter date must be available
                    if ( assetId.contains( "users_" ) ) {

                        Date filteringDate = null;

                        if(UtilMethods.isSet(_contentFilterDate)) {
                            dateFormat.parse(_contentFilterDate);
                        }
                        //Get users where createdate >= ?
                        List<String> usersIds = APILocator.getUserAPI().getUsersIdsByCreationDate( filteringDate, 0, -1 );
                        if ( usersIds != null ) {
                            for ( String id : usersIds ) {
                            	if(!UtilMethods.isSet(bundleId) || !isAssetInBundle("user_" + id, bundleId)){
                            		ids.add( "user_" + id );
                            	}
                            }
                        }
                    } else {
                    	if(!UtilMethods.isSet(bundleId) || !isAssetInBundle(assetId, bundleId)){
                    		ids.add( assetId );
                    	}
                    }
                } else if ( assetId.equals(ADD_ALL_CATEGORIES_TO_BUNDLE_KEY) ) {
                	if(!UtilMethods.isSet(bundleId) || !isAssetInBundle(assetId, bundleId)){
                		ids.add( assetId );
                	}
                } else if ( assetId.contains( ".jar" ) ) {//Check for OSGI jar bundles
                	if(!UtilMethods.isSet(bundleId) || !isAssetInBundle(assetId, bundleId)){
                		ids.add( assetId );
                	}
                } else {

                    try {
                        // if the asset is a folder put the inode instead of the identifier
                        //At first we try to find the Asset Type by hitting identifier table.
                        String assetType = APILocator.getIdentifierAPI().getAssetTypeFromDB(assetId);

                        //If we don't find the Type in table identifier we try to hit table inode.
                        if(assetType == null) {
                            assetType = InodeUtils.getAssetTypeFromDB(assetId);
                        }

                        // If we don't find the Type in Inode table, we try to 
                        // determine the type in a different way
						if (assetType == null
								&& (APILocator.getLanguageAPI().isAssetTypeLanguage(assetId) || APILocator.getRulesAPI()
										.getRuleById(assetId, getUser(), false) != null)) {
							ids.add(assetId);
						} else if(assetType != null && assetType.equals(Identifier.ASSET_TYPE_FOLDER)){
                            Folder folder = null;

                            try {
                                folder = APILocator.getFolderAPI().find( assetId, getUser(), false );
                            } catch ( DotSecurityException e ) {
                                Logger.error( getClass(), "User: " + getUser() + " does not have permission to access folder. Folder identifier: " + assetId );
                            } catch ( DotDataException e ) {
                                Logger.info( getClass(), "FolderAPI.find(): Identifier is null" );
                            }

                            if ( folder != null && UtilMethods.isSet( folder.getInode() ) ) {
                                if (!isAssetInBundle(assetId, bundleId)) {
                                    ids.add(assetId);
                                }
                            }
                        } else { // if the asset is not a folder and has identifier, put it, if not, put the inode
                            Identifier iden = APILocator.getIdentifierAPI().findFromInode( assetId );
                            if ( !ids.contains( iden.getId() ) ) {//Multiples languages have the same identifier
                            	if(!UtilMethods.isSet(bundleId) || !isAssetInBundle(iden.getId(), bundleId)){
                            		ids.add( iden.getId() );
                            	}
                            }
                        }
                    } catch ( DotStateException e ) {
                        Logger.warn(RemotePublishAjaxAction.class, "Unable to find asset id = [" + assetId + "]");

                    	if(!UtilMethods.isSet(bundleId) || !isAssetInBundle(assetId, bundleId)){
                    		ids.add( assetId );
                    	}
                    } catch (DotSecurityException e1) {
						Logger.error(RemotePublishAjaxAction.class, "User " + getUser().getUserId()
								+ " does not have permission to access asset ID " + assetId);
					}
                }
            }
        }

        return ids;
    }
    
    /**
     * Validate if the asset is already included in the bundle
     * @param assetId Asset identifier
     * @param bundleId Bundle Id
     * @return true if the asset is already included.
     */
    private boolean isAssetInBundle(String assetId, String bundleId){
    	PublisherAPI publisherAPI = PublisherAPI.getInstance();
		try {
			List<PublishQueueElement> assets = publisherAPI.getQueueElementsByAsset(assetId);
			for(PublishQueueElement element :  assets){
				if(element.getBundleId().equals(bundleId)){
					return true;
				}
			}
		} catch (DotPublisherException e) {
			Logger.error(RemotePublishAjaxAction.class, e.getMessage());
		}
		
    	return false;
    }

}
