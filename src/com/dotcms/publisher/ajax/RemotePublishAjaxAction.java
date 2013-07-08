package com.dotcms.publisher.ajax;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.hadoop.mapred.lib.Arrays;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushUtils;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.rest.PublishThread;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

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
		String cmd = map.get("cmd");
		Method dispatchMethod = null;

		User user = getUser();

		try{
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
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
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CONTENT_PUBLISHING_TOOL", user)){
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

	public void unPublish(HttpServletRequest request, HttpServletResponse response) throws DotPublisherException {
	    PublisherAPI publisherAPI = PublisherAPI.getInstance();
        String assetId = request.getParameter("assetIdentifier");
        List<String> identifiers=new ArrayList<String>();
        if(assetId.contains(","))
            identifiers.addAll(Arrays.asList(assetId.split(",")));
        else
            identifiers.add(assetId);

        publisherAPI.addContentsToUnpublish(identifiers, UUIDGenerator.generateUuid(), new Date(), getUser());
	}

    /**
     * Send to publish a given element
     *
     * @param request
     * @param response
     * @throws WorkflowActionFailureException
     */
    public void publish ( HttpServletRequest request, HttpServletResponse response ) throws WorkflowActionFailureException {

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
            List<String> whereToSend = Arrays.asList(whoToSendTmp.split(","));
            List<Environment> envsToSendTo = new ArrayList<Environment>();

            // Lists of Environments to push to
            for (String envId : whereToSend) {
            	Environment e = APILocator.getEnvironmentAPI().findEnvironmentById(envId);

            	if(e!=null) {
            		envsToSendTo.add(e);
            	}
			}

            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-H-m" );
            Date publishDate = dateFormat.parse( _contentPushPublishDate + "-" + _contentPushPublishTime );

            List<String> assetsIds = new ArrayList<String>();
            assetsIds.add(_assetId);

            List<String> ids = getIdsToPush(assetsIds, _contentFilterDate, dateFormat);

            if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH ) || _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE ) ) {
            	Bundle bundle = new Bundle(null, publishDate, null, getUser().getUserId());
            	APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);

            	publisherAPI.addContentsToPublish( ids, bundle.getId(), publishDate, getUser() );
            }
            if ( _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE ) || _iWantTo.equals( RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE ) ) {
                if ( (!"".equals( _contentPushExpireDate.trim() ) && !"".equals( _contentPushExpireTime.trim() )) ) {
                    Date expireDate = dateFormat.parse( _contentPushExpireDate + "-" + _contentPushExpireTime );

                    Bundle bundle = new Bundle(null, publishDate, expireDate, getUser().getUserId());
                	APILocator.getBundleAPI().saveBundle(bundle, envsToSendTo);

                	publisherAPI.addContentsToUnpublish( ids, bundle.getId(), expireDate, getUser() );
                }
            }
        } catch ( DotPublisherException e ) {
            Logger.debug( PushPublishActionlet.class, e.getMessage(), e );
            throw new WorkflowActionFailureException( e.getMessage(), e );
        } catch ( ParseException e ) {
            Logger.debug( PushPublishActionlet.class, e.getMessage() );
            throw new WorkflowActionFailureException( e.getMessage() );
        } catch ( DotDataException e ) {
            Logger.error( PushPublishActionlet.class, e.getMessage(), e );
        }
    }


    /**
     * Allow the user to send again a failed bundle to que publisher queue job in order to try to republish it again
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws DotDataException
     * @throws DotPublisherException
     */
    public void retry ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException, DotDataException, DotPublisherException, LanguageException {

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

            //ONLY FAILED BUNDLES
            if ( !status.getStatus().equals( Status.FAILED_TO_PUBLISH ) ) {
                appendMessage( responseMessage, "publisher_retry.error.only.failed.publish", bundleId, true );
                continue;
            }

            /*
            Verify if the bundle exist and was created correctly..., meaning, if there is not a .tar.gz file is because
            something happened on the creation of the bundle.
             */
            File bundle = new File( bundleRoot + File.separator + ".." + File.separator + basicConfig.getId() + ".tar.gz" );
            if ( !bundle.exists() ) {
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
                Boolean sending = sendingBundle( request, config );
                if ( !sending ) {
                    appendMessage( responseMessage, "publisher_retry.error.cannot.retry.received", bundleId, true );
                    continue;
                }

                //Clean the number of tries, we want to try it again
                auditHistory.setNumTries( 0 );
                publishAuditAPI.updatePublishAuditStatus( config.getId(), status.getStatus(), auditHistory );

                //Get the identifiers on this bundle
                List<String> identifiers = new ArrayList<String>();
                List<PublishQueueElement> assets = config.getAssets();
                if ( assets == null || assets.isEmpty() ) {
                    identifiers.addAll( PublisherUtil.getContentIds( config.getLuceneQueries() ) );
                } else {
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

    public void downloadBundle ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException, DotDataException {
        Map<String, String> map = getURIParams();
		response.setContentType("application/x-tgz");

		String bid = map.get("bid");

		PublisherConfig config = new PublisherConfig();
		config.setId(bid);
		File bundleRoot = BundlerUtil.getBundleRoot(config);

		ArrayList<File> list = new ArrayList<File>(1);
		list.add(bundleRoot);
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
		}
		catch(Exception e){

		}
		finally{
			try{
				in.close();
			}
			catch(Exception ex){};
		}
		return;
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void generateBundle ( String bundleId ) throws ServletException, IOException, DotDataException {
    	PushPublisherConfig pconf = new PushPublisherConfig();
		PublisherAPI pubAPI = PublisherAPI.getInstance();

		try {
			List<PublishQueueElement> tempBundleContents = pubAPI.getQueueElementsByBundleId(bundleId);

			Map<String, String> assets = new HashMap<String, String>();
			List<PublishQueueElement> assetsToPublish = new ArrayList<PublishQueueElement>(); // all assets but contentlets

			for(PublishQueueElement c : tempBundleContents) {
				assets.put((String) c.getAsset(), c.getType());
				if(!c.getType().equals("contentlet"))
					assetsToPublish.add(c);
			}

			pconf.setPushing(false);
//			pconf.setOperation(Operation.DOWNLOAD);
			// all types of assets in the queue but contentlets are passed here, which are passed through lucene queries
			pconf.setAssets(assetsToPublish);
			//Queries creation
			pconf.setLuceneQueries(PublisherUtil.prepareQueries(tempBundleContents));
			pconf.setId(bundleId);
			pconf.setUser(APILocator.getUserAPI().getSystemUser());

			List<Class> bundlers = new ArrayList<Class>();
			List<IBundler> confBundlers = new ArrayList<IBundler>();

			Publisher p = new PushPublisher();
			p.init(pconf);

			for (Class clazz : p.getBundlers()) {
				if (!bundlers.contains(clazz)) {
					bundlers.add(clazz);
				}
			}

			File compressedBundle = new File( ConfigUtils.getBundlePath() + File.separator + pconf.getId() + ".tar.gz" );
            if ( !compressedBundle.exists() ) {

                // Run bundlers
                File bundleRoot = BundlerUtil.getBundleRoot( pconf );

                BundlerUtil.writeBundleXML( pconf );
                for ( Class<IBundler> c : bundlers ) {

                    IBundler bundler = c.newInstance();
                    confBundlers.add( bundler );
                    bundler.setConfig( pconf );
                    BundlerStatus bs = new BundlerStatus( bundler.getClass().getName() );
                    //Generate the bundler
                    bundler.generate( bundleRoot, bs );
                }

                pconf.setBundlers( confBundlers );
            }

          //Compressing bundle
			File bundleRoot = BundlerUtil.getBundleRoot(pconf);
			ArrayList<File> list = new ArrayList<File>(1);
			list.add(bundleRoot);
			File bundle = new File(bundleRoot+File.separator+".."+File.separator+pconf.getId()+".tar.gz");
			PushUtils.compressFiles(list, bundle, bundleRoot.getAbsolutePath());



		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }



	public void uploadBundle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DotDataException, FileUploadException {
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
     * @param responseMessage
     * @param messageKey
     * @param bundleId
     * @param failure
     * @throws LanguageException
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
    private Boolean sendingBundle ( HttpServletRequest request, PushPublisherConfig config ) throws DotDataException {

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
        List<PublishingEndPoint> endPoints = config.getEndpoints();//List of end points for Remote Publishing this bundle
        for ( PublishingEndPoint endPoint : endPoints ) {

            //Getting the end point details
            String endPointAddress = endPoint.getAddress();
            String endPointPort = endPoint.getPort();

            if ( endPointAddress.equals( remoteIP )
                    && endPointPort.equals( String.valueOf( port ) ) ) {
                return false;
            }
        }

        return true;
    }

    public void addToBundle(HttpServletRequest request, HttpServletResponse response) throws DotPublisherException {
	    PublisherAPI publisherAPI = PublisherAPI.getInstance();
	    String _assetId = request.getParameter("assetIdentifier");
	    String _contentFilterDate = request.getParameter( "remoteFilterDate" );
	    String isNewBundle = request.getParameter("newBundle");
	    String bundleName = request.getParameter("bundleName");
        String bundleSelect = request.getParameter("bundleSelect");

        try {
        	Bundle bundle = null;

        	if(isNewBundle.equals("true")) {
        		bundle = new Bundle(bundleName, null, null, getUser().getUserId());
        		APILocator.getBundleAPI().saveBundle(bundle);
        	} else {
        		bundle = APILocator.getBundleAPI().getBundleById(bundleSelect);
        	}

        	List<String> assetsIds = new ArrayList<String>();
        	assetsIds.add(_assetId);

        	List<String> ids = getIdsToPush(assetsIds, _contentFilterDate, new SimpleDateFormat( "yyyy-MM-dd-H-m" ));
        	publisherAPI.saveBundleAssets(ids, bundle.getId(), getUser());

        } catch ( Exception e) {
        	Logger.error( PushPublishActionlet.class, e.getMessage(), e );
		}

	}

    /**
     * Updates the assets in the given bundle with the publish/expire dates and destination environments and set them ready to be pushed
     *
     * @param request
     * @param response
     * @throws WorkflowActionFailureException
     */
    public void pushBundle ( HttpServletRequest request, HttpServletResponse response ) throws WorkflowActionFailureException {

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


        } catch ( DotPublisherException e ) {
            Logger.debug( PushPublishActionlet.class, e.getMessage(), e );
            throw new WorkflowActionFailureException( e.getMessage(), e );
        } catch ( ParseException e ) {
            Logger.debug( PushPublishActionlet.class, e.getMessage() );
            throw new WorkflowActionFailureException( e.getMessage() );
        } catch ( DotDataException e ) {
            Logger.error( PushPublishActionlet.class, e.getMessage(), e );
        }
    }

    private List<String> getIdsToPush(List<String> assetIds, String _contentFilterDate,
			SimpleDateFormat dateFormat)
			throws ParseException, DotDataException {

    	List<String> ids = new ArrayList<String>();

    	for (String _assetId : assetIds) {


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
				    } catch (DotSecurityException e) {
						Logger.error(getClass(), "User: " + getUser() + " does not have permission to access folder. Folder identifier: " + _assetId);
					} catch (DotDataException e) {
						Logger.info(getClass(), "FolderAPI.find(): Identifier is null");
					}

				    if ( folder != null && UtilMethods.isSet( folder.getInode() ) ) {
				        ids.add( _assetId );
				    } else {
				        // if the asset is not a folder and has identifier, put it, if not, put the inode
				        Identifier iden = APILocator.getIdentifierAPI().findFromInode( _assetId );
				        ids.add( iden.getId() );
				    }

				} catch(DotStateException e) {
					ids.add(_assetId);
				}
			}

    	}

		return ids;
	}

}