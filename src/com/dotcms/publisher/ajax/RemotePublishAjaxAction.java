package com.dotcms.publisher.ajax;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.*;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushUtils;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.*;
import com.dotcms.rest.PublishThread;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.*;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.hadoop.mapred.lib.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RemotePublishAjaxAction extends AjaxAction {

	public static final String DIALOG_ACTION_EXPIRE="expire";
	public static final String DIALOG_ACTION_PUBLISH="publish";
	public static final String DIALOG_ACTION_PUBLISH_AND_EXPIRE="publishexpire";

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
				throw new DotRuntimeException(e.getMessage());
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
            String _assetId = request.getParameter( "assetIdentifier" );
            String _contentPushPublishDate = request.getParameter( "remotePublishDate" );
            String _contentPushPublishTime = request.getParameter( "remotePublishTime" );
            String _contentPushExpireDate = request.getParameter( "remotePublishExpireDate" );
            String _contentPushExpireTime = request.getParameter( "remotePublishExpireTime" );
            String _contentFilterDate = request.getParameter( "remoteFilterDate" );
            String _iWantTo = request.getParameter( "iWantTo" );
            String whoToSendTmp = request.getParameter( "whoToSend" );
            String forcePushStr = request.getParameter( "forcePush" );
            boolean forcePush = (forcePushStr!=null && forcePushStr.equals("true"));
            List<String> whereToSend = Arrays.asList( whoToSendTmp.split( "," ) );
            List<Environment> envsToSendTo = new ArrayList<Environment>();

            // Lists of Environments to push to
            for (String envId : whereToSend) {
            	Environment e = APILocator.getEnvironmentAPI().findEnvironmentById(envId);

            	if(e!=null) {
            		envsToSendTo.add(e);
            	}
			}

            //Put the selected environments in session in order to have the list of the last selected environments
            request.getSession().setAttribute( WebKeys.SELECTED_ENVIRONMENTS, envsToSendTo );

            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-H-m" );
            Date publishDate = dateFormat.parse( _contentPushPublishDate + "-" + _contentPushPublishTime );

            List<String> ids;
            if ( _assetId.startsWith( "query_" ) ) { //Support for lucene queries

                String luceneQuery = _assetId.replace( "query_", "" );
                List<String> queries = new ArrayList<String>();
                queries.add( luceneQuery );
                ids = PublisherUtil.getContentIds( queries );

            } else {

                String[] _assetsIds = _assetId.split( "," );//Support for multiple ids in the assetIdentifier parameter
                List<String> assetsIds = Arrays.asList( _assetsIds );

                ids = getIdsToPush( assetsIds, _contentFilterDate, dateFormat );
            }

            //Response map with the status of the addContents operation (error messages and counts )
            Map<String, Object> responseMap = null;

            if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH ) || _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE ) ) {
            	Bundle bundle = new Bundle(null, publishDate, null, getUser().getUserId(), forcePush);
            	APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);

                responseMap = publisherAPI.addContentsToPublish( ids, bundle.getId(), publishDate, getUser() );
            }
            if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE ) || _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE ) ) {
                if ( (!"".equals( _contentPushExpireDate.trim() ) && !"".equals( _contentPushExpireTime.trim() )) ) {
                    Date expireDate = dateFormat.parse( _contentPushExpireDate + "-" + _contentPushExpireTime );

                    Bundle bundle = new Bundle(null, publishDate, expireDate, getUser().getUserId(), forcePush);
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

                //And send it back to the user
                response.getWriter().println( jsonResponse.toString() );
            }
        } catch ( Exception e ) {
            Logger.error( RemotePublishAjaxAction.class, e.getMessage(), e );
            response.sendError( HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error Publishing Bundle: " + e.getMessage() );
        }
    }

    /**
     * Allow the user to send or try to send again failed and successfully sent bundles, in order to do that<br/>
     * we send the bundle again to que publisher queue job which will try to remote publish again the bundle.
     * @param request  HttpRequest
     * @param response HttpResponse
     * @throws IOException If fails sending back to the user a proper response
     * @throws DotPublisherException If fails retrieving the Bundle related information like elements on it and statuses
     * @throws LanguageException If fails using i18 messages
     */
    public void retry ( HttpServletRequest request, HttpServletResponse response ) throws IOException, DotPublisherException, LanguageException {

        PublisherAPI publisherAPI = PublisherAPI.getInstance();
        PublishAuditAPI publishAuditAPI = PublishAuditAPI.getInstance();

        //Read the parameters
        String bundlesIds = request.getParameter( "bundlesIds" );
        String[] ids = bundlesIds.split( "," );

        StringBuilder responseMessage = new StringBuilder();

        for ( String bundleId : ids ) {

            if ( bundleId.trim().isEmpty() ) {
                continue;
            }

            PublisherConfig basicConfig = new PublisherConfig();
            basicConfig.setId( bundleId );
            File bundleRoot = BundlerUtil.getBundleRoot( basicConfig );

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
            if ( !(status.getStatus().equals( Status.FAILED_TO_PUBLISH ) || status.getStatus().equals( Status.SUCCESS )) ) {
                appendMessage( responseMessage, "publisher_retry.error.only.failed.publish", bundleId, true );
                continue;
            }

            /*
            Verify if the bundle exist and was created correctly..., meaning, if there is not a .tar.gz file is because
            something happened on the creation of the bundle.
             */
            File bundleFile = new File( bundleRoot + File.separator + ".." + File.separator + basicConfig.getId() + ".tar.gz" );
            if ( !bundleFile.exists() ) {
                Logger.error( this.getClass(), "No Bundle with id: " + bundleId + " found." );
                appendMessage( responseMessage, "publisher_retry.error.not.found", bundleId, true );
                continue;
            }

            if ( !BundlerUtil.bundleExists( basicConfig ) ) {
                Logger.error( this.getClass(), "No Bundle Descriptor for bundle id: " + bundleId + " found." );
                appendMessage( responseMessage, "publisher_retry.error.not.descriptor.found", bundleId, true );
                continue;
            }

            try {

                //Read the bundle to see what kind of configuration we need to apply
                String bundlePath = ConfigUtils.getBundlePath() + File.separator + basicConfig.getId();
                File xml = new File( bundlePath + File.separator + "bundle.xml" );
                PushPublisherConfig config = (PushPublisherConfig) BundlerUtil.xmlToObject( xml );

                //We can not retry Received Bundles, just bundles that we are trying to send
                Boolean sending = sendingBundle( request, config, bundleId );
                if ( !sending ) {
                    appendMessage( responseMessage, "publisher_retry.error.cannot.retry.received", bundleId, true );
                    continue;
                }

                if ( status.getStatus().equals( Status.SUCCESS ) ) {

                    //Get the bundle
                    Bundle bundle = APILocator.getBundleAPI().getBundleById( bundleId );
                    if ( bundle == null ) {
                        Logger.error( this.getClass(), "No Bundle with id: " + bundleId + " found." );
                        appendMessage( responseMessage, "publisher_retry.error.not.found", bundleId, true );
                        continue;
                    }
                    bundle.setForcePush( true );
                    APILocator.getBundleAPI().updateBundle( bundle );
                }

                //Clean the number of tries, we want to try it again
                auditHistory.setNumTries( 0 );
                publishAuditAPI.updatePublishAuditStatus( config.getId(), status.getStatus(), auditHistory );

                //Get the identifiers on this bundle
                List<String> identifiers = new ArrayList<String>();
                List<PublishQueueElement> assets = config.getAssets();
                if ( config.getLuceneQueries() != null && !config.getLuceneQueries().isEmpty() ) {
                    identifiers.addAll( PublisherUtil.getContentIds( config.getLuceneQueries() ) );
                }
                if ( assets != null && !assets.isEmpty() ) {
                    for ( PublishQueueElement asset : assets ) {
                        identifiers.add( asset.getAsset() );
                    }
                }

                //Now depending of the operation lets add it to the queue job
                if ( config.getOperation().equals( PushPublisherConfig.Operation.PUBLISH ) ) {
                    publisherAPI.addContentsToPublish( identifiers, bundleId, new Date(), getUser() );
                } else {
                    publisherAPI.addContentsToUnpublish( identifiers, bundleId, new Date(), getUser() );
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
			if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CONTENT_PUBLISHING_TOOL", getUser())){
				response.sendError(401);
				return;
			}
		} catch (DotDataException e1) {
			Logger.error(RemotePublishAjaxAction.class,e1.getMessage(),e1);
			response.sendError(401);
			return;
		}
        Map<String, String> map = getURIParams();
		response.setContentType("application/x-tgz");

		String bid = map.get("bid");

		PublisherConfig config = new PublisherConfig();
		config.setId(bid);
		File bundleRoot = BundlerUtil.getBundleRoot(config);

		ArrayList<File> list = new ArrayList<File>(1);
		list.add( bundleRoot );
		File bundle = new File(bundleRoot+File.separator+".."+File.separator+config.getId()+".tar.gz");
		if(!bundle.exists()){
			response.sendError(500, "No Bundle Found");
			return;
		}

		response.setHeader("Content-Disposition", "attachment; filename=" + config.getId()+".tar.gz");
		BufferedInputStream in = null;
		try{
			in = new BufferedInputStream(new FileInputStream(bundle));
			byte[] buf = new byte[4096];
			int len;

			while ((len = in.read(buf, 0, buf.length))!= -1){
				response.getOutputStream().write(buf, 0, len);
			}
        } catch ( Exception e ) {
            Logger.warn( this.getClass(), "Error Downloading Bundle.", e );
        } finally {
			try{
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
    public void downloadUnpushedBundle ( HttpServletRequest request, HttpServletResponse response ) throws IOException {
    	try {
			if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CONTENT_PUBLISHING_TOOL", getUser())){
				response.sendError(401);
				return;
			}
		} catch (DotDataException e1) {
			Logger.error(RemotePublishAjaxAction.class,e1.getMessage(),e1);
			response.sendError(401);
			return;
		}
        //Read the parameters
        Map<String, String> map = getURIParams();
        String bundleId = map.get( "bundleId" );
        String paramOperation = map.get( "operation" );
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

        File bundle;
        String generatedBundleId;
        try {
            //Generate the bundle file for this given operation
            Map<String, Object> bundleData = generateBundle( bundleId, operation );
            bundle = (File) bundleData.get( "file" );
            generatedBundleId = (String) bundleData.get( "id" );
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Error trying to generate bundle with id: " + bundleId, e );
            response.sendError( 500, "Error trying to generate bundle with id: " + bundleId );
            return;
        }

        response.setContentType( "application/x-tgz" );
        response.setHeader( "Content-Disposition", "attachment; filename=" + bundle.getName() );
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream( new FileInputStream( bundle ) );
            byte[] buf = new byte[4096];
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
            File bundleRoot = BundlerUtil.getBundleRoot( generatedBundleId );
            File compressedBundle = new File( ConfigUtils.getBundlePath() + File.separator + generatedBundleId + ".tar.gz" );
            if ( compressedBundle.exists() ) {
                compressedBundle.delete();
                if ( bundleRoot.exists() ) {
                    com.liferay.util.FileUtil.deltree( bundleRoot );
                }
            }
        }
    }

    /**
     * Generates an Unpublish bundle for a given bundle id  operation (publish/unpublish)
     *
     * @param bundleId The Bundle id of the Bundle we want to generate
     * @param operation Download for publish or un-publish
     * @return The generated requested Bundle file
     * @throws DotPublisherException If fails retrieving the Bundle contents
     * @throws DotDataException If fails finding the system user
     * @throws DotPublishingException If fails initializing the Publisher
     * @throws IllegalAccessException If fails creating new Bundlers instances
     * @throws InstantiationException If fails creating new Bundlers instances
     * @throws DotBundleException If fails generating the Bundle
     * @throws IOException If fails compressing the all the Bundle contents into the final Bundle file
     */
    @SuppressWarnings ("unchecked")
    private Map<String, Object> generateBundle ( String bundleId, PushPublisherConfig.Operation operation ) throws DotPublisherException, DotDataException, DotPublishingException, IllegalAccessException, InstantiationException, DotBundleException, IOException {
    	
        PushPublisherConfig pconf = new PushPublisherConfig();
        PublisherAPI pubAPI = PublisherAPI.getInstance();

        List<PublishQueueElement> tempBundleContents = pubAPI.getQueueElementsByBundleId( bundleId );
        List<PublishQueueElement> assetsToPublish = new ArrayList<PublishQueueElement>(); // all assets but contentlets

        for ( PublishQueueElement c : tempBundleContents ) {
            if ( !c.getType().equals( "contentlet" ) ) {
                assetsToPublish.add( c );
            }
        }

        pconf.setDownloading( true );
        pconf.setOperation(operation);

        // all types of assets in the queue but contentlets are passed here, which are passed through lucene queries
        pconf.setAssets( assetsToPublish );
        //Queries creation
        pconf.setLuceneQueries( PublisherUtil.prepareQueries( tempBundleContents ) );
        pconf.setId( bundleId );
        pconf.setUser( APILocator.getUserAPI().getSystemUser() );

        //BUNDLERS

        List<Class<IBundler>> bundlers = new ArrayList<Class<IBundler>>();
        List<IBundler> confBundlers = new ArrayList<IBundler>();

        Publisher publisher = new PushPublisher();
        publisher.init( pconf );
        //Add the bundles for this publisher
        for ( Class clazz : publisher.getBundlers() ) {
            if ( !bundlers.contains( clazz ) ) {
                bundlers.add( clazz );
            }
        }

        //Create a new bundle id for this generated bundle
        String newBundleId = UUID.randomUUID().toString();
        pconf.setId( newBundleId );
        File bundleRoot = BundlerUtil.getBundleRoot( pconf );

        // Run bundlers
        BundlerUtil.writeBundleXML( pconf );
        for ( Class<IBundler> c : bundlers ) {

            IBundler bundler = c.newInstance();
            confBundlers.add( bundler );
            bundler.setConfig( pconf );
            BundlerStatus bundlerStatus = new BundlerStatus( bundler.getClass().getName() );
            //Generate the bundler
            bundler.generate( bundleRoot, bundlerStatus );
        }

        pconf.setBundlers( confBundlers );

        //Compressing bundle
        ArrayList<File> list = new ArrayList<File>();
        list.add( bundleRoot );
        File bundle = new File( bundleRoot + File.separator + ".." + File.separator + pconf.getId() + ".tar.gz" );

        Map<String, Object> bundleData = new HashMap<String, Object>();
        bundleData.put( "id", newBundleId );
        bundleData.put( "file", PushUtils.compressFiles( list, bundle, bundleRoot.getAbsolutePath() ) );
        return bundleData;
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

    	try {
			if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CONTENT_PUBLISHING_TOOL", getUser())){
				response.sendError(401);
				return;
			}
		} catch (DotDataException e1) {
			Logger.error(RemotePublishAjaxAction.class,e1.getMessage(),e1);
			response.sendError(401);
			return;
		}
    	
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        @SuppressWarnings("unchecked")
		List<FileItem> items = (List<FileItem>) upload.parseRequest(request);

		InputStream bundle = items.get(0).getInputStream();
		String bundleName = items.get(0).getName();
		String bundlePath = ConfigUtils.getBundlePath()+File.separator;
		String bundleFolder = bundleName.substring(0, bundleName.indexOf(".tar.gz"));
		String endpointId = getUser().getUserId();
		response.setContentType("text/html; charset=utf-8");
		PrintWriter out = response.getWriter();

		PublishAuditStatus status;
		try {
			status = PublishAuditAPI.getInstance().updateAuditTable(endpointId, null, bundleFolder);

	//		Write file on FS
			FileUtil.writeToFile(bundle, bundlePath+bundleName);

			if(!status.getStatus().equals(Status.PUBLISHING_BUNDLE)) {
				new Thread(new PublishThread(bundleName, null, endpointId, status)).start();
			}

			out.print("<html><head><script>isLoaded = true;</script></head><body><textarea>{'status':'success'}</textarea></body></html>");

		} catch (DotPublisherException e) {
			// TODO Auto-generated catch block
			out.print("<html><head><script>isLoaded = true;</script></head><body><textarea>{'status':'error'}</textarea></body></html>");
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

        PublisherAPI publisherAPI = PublisherAPI.getInstance();
        String _assetId = request.getParameter( "assetIdentifier" );
        String _contentFilterDate = request.getParameter( "remoteFilterDate" );
        String bundleName = request.getParameter( "bundleName" );
        String bundleId = request.getParameter( "bundleSelect" );

        try {
            Bundle bundle;

            if ( bundleId == null || bundleName.equals( bundleId ) ) {
                // if the user has a unsent bundle with that name just add to it
                bundle=null;
                for(Bundle b : APILocator.getBundleAPI().getUnsendBundlesByName(getUser().getUserId(), bundleName, 1000, 0)) {
                    if(b.getName().equalsIgnoreCase(bundleName)) {
                        bundle=b;
                    }
                }
                
                if(bundle==null) {
                    bundle = new Bundle( bundleName, null, null, getUser().getUserId() );
                    APILocator.getBundleAPI().saveBundle( bundle );
                }
            } else {
                bundle = APILocator.getBundleAPI().getBundleById( bundleId );
            }

            //Put the selected bundle in session in order to have last one selected
            request.getSession().setAttribute( WebKeys.SELECTED_BUNDLE, bundle );

            List<String> ids;
            if ( _assetId.startsWith( "query_" ) ) { //Support for lucene queries

                String luceneQuery = _assetId.replace( "query_", "" );
                List<String> queries = new ArrayList<String>();
                queries.add( luceneQuery );
                ids = PublisherUtil.getContentIds( queries );

            } else {

                String[] _assetsIds = _assetId.split( "," );//Support for multiple ids in the assetIdentifier parameter
                List<String> assetsIds = Arrays.asList( _assetsIds );

                ids = getIdsToPush( assetsIds, _contentFilterDate, new SimpleDateFormat( "yyyy-MM-dd-H-m" ) );
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

                //And send it back to the user
                response.getWriter().println( jsonResponse.toString() );
            }
        } catch ( Exception e ) {
            Logger.error( RemotePublishAjaxAction.class, e.getMessage(), e );
            response.sendError( HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error Adding content to Bundle: " + e.getMessage() );
        }
    }

    /**
     * Updates the assets in the given bundle with the publish/expire dates and destination environments and set them ready to be pushed
     *
     * @param request  HttpRequest
     * @param response HttpResponse
     * @throws WorkflowActionFailureException If fails trying to Publish the bundle contents
     */
    public void pushBundle ( HttpServletRequest request, HttpServletResponse response ) throws WorkflowActionFailureException, IOException {

        try {

            PublisherAPI publisherAPI = PublisherAPI.getInstance();

            //Read the form values
            String bundleId = request.getParameter( "assetIdentifier" );
            String _contentPushPublishDate = request.getParameter( "remotePublishDate" );
            String _contentPushPublishTime = request.getParameter( "remotePublishTime" );
            String _contentPushExpireDate = request.getParameter( "remotePublishExpireDate" );
            String _contentPushExpireTime = request.getParameter( "remotePublishExpireTime" );
            String _iWantTo = request.getParameter( "iWantTo" );
            String whoToSendTmp = request.getParameter( "whoToSend" );
            List<String> whereToSend = Arrays.asList(whoToSendTmp.split(","));
            List<Environment> envsToSendTo = new ArrayList<Environment>();

            // Lists of Environments to push to
            for (String envId : whereToSend) {
            	Environment e = APILocator.getEnvironmentAPI().findEnvironmentById(envId);

            	if(e!=null) {
            		envsToSendTo.add(e);
            	}
			}

            //Put the selected environments in session in order to have the list of the last selected environments
            request.getSession().setAttribute( WebKeys.SELECTED_ENVIRONMENTS, envsToSendTo );
            //Clean up the selected bundle
            request.getSession().removeAttribute( WebKeys.SELECTED_BUNDLE );

            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-H-m" );
            Date publishDate = dateFormat.parse( _contentPushPublishDate + "-" + _contentPushPublishTime );
            Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);
            APILocator.getBundleAPI().saveBundleEnvironments(bundle, envsToSendTo);

            if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH )) {
            	bundle.setPublishDate(publishDate);
             	APILocator.getBundleAPI().updateBundle(bundle);

             	publisherAPI.publishBundleAssets(bundle.getId(), publishDate);

            } else if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE )) {
            	if ( (!"".equals( _contentPushExpireDate.trim() ) && !"".equals( _contentPushExpireTime.trim() )) ) {
                    Date expireDate = dateFormat.parse( _contentPushExpireDate + "-" + _contentPushExpireTime );
                    bundle.setExpireDate(expireDate);
                	APILocator.getBundleAPI().updateBundle(bundle);

                	publisherAPI.unpublishBundleAssets(bundle.getId(), expireDate);
                }

            } else if(_iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE ) ) {
                if ( (!"".equals( _contentPushExpireDate.trim() ) && !"".equals( _contentPushExpireTime.trim() )) ) {
                    Date expireDate = dateFormat.parse( _contentPushExpireDate + "-" + _contentPushExpireTime );
                    bundle.setPublishDate(publishDate);
                    bundle.setExpireDate(expireDate);
                	APILocator.getBundleAPI().updateBundle(bundle);

                	publisherAPI.publishAndExpireBundleAssets(bundle.getId(), publishDate, expireDate, getUser());
                }
            }

        } catch ( Exception e ) {
            Logger.error( RemotePublishAjaxAction.class, e.getMessage(), e );
            response.sendError( HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error Push Publishing Bundle: " + e.getMessage() );
        }
    }

    /**
     * Returns the list of ids the user is trying to remote publish.
     */
    private List<String> getIdsToPush ( List<String> assetIds, String _contentFilterDate, SimpleDateFormat dateFormat )
            throws ParseException, DotDataException {

        List<String> ids = new ArrayList<String>();

        for ( String _assetId : assetIds ) {

            if ( _assetId != null && !_assetId.trim().isEmpty() ) {

                if ( ids.contains( _assetId ) ) {
                    continue;
                }

                // check for the categories
                if ( _assetId.contains( "user_" ) || _assetId.contains( "users_" ) ) {//Trying to publish users
                    //If we are trying to push users a filter date must be available
                    if ( _assetId.contains( "users_" ) ) {
                        Date filteringDate = dateFormat.parse( _contentFilterDate );
                        //Get users where createdate >= ?
                        List<String> usersIds = APILocator.getUserAPI().getUsersIdsByCreationDate( filteringDate, 0, -1 );
                        if ( usersIds != null ) {
                            for ( String id : usersIds ) {
                                ids.add( "user_" + id );
                            }
                        }
                    } else {
                        ids.add( _assetId );
                    }
                } else if ( _assetId.equals( "CAT" ) ) {
                    ids.add( _assetId );
                } else if ( _assetId.contains( ".jar" ) ) {//Check for OSGI jar bundles
                    ids.add( _assetId );
                } else {
                    // if the asset is a folder put the inode instead of the identifier
                    try {
                        Folder folder = null;
                        try {
                            folder = APILocator.getFolderAPI().find( _assetId, getUser(), false );
                        } catch ( DotSecurityException e ) {
                            Logger.error( getClass(), "User: " + getUser() + " does not have permission to access folder. Folder identifier: " + _assetId );
                        } catch ( DotDataException e ) {
                            Logger.info( getClass(), "FolderAPI.find(): Identifier is null" );
                        }

                        if ( folder != null && UtilMethods.isSet( folder.getInode() ) ) {
                            ids.add( _assetId );
                        } else {
                            // if the asset is not a folder and has identifier, put it, if not, put the inode
                            Identifier iden = APILocator.getIdentifierAPI().findFromInode( _assetId );
                            if ( !ids.contains( iden.getId() ) ) {//Multiples languages have the same identifier
                                ids.add( iden.getId() );
                            }
                        }

                    } catch ( DotStateException e ) {
                        ids.add( _assetId );
                    }
                }
            }

        }

        return ids;
    }

}
