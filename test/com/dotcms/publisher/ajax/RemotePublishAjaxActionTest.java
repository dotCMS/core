package com.dotcms.publisher.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.LicenseTestUtil;
import com.dotcms.TestBase;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.ClientBuilder;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import junit.framework.Assert;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.FormDataMultiPart;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.MultiPartFeature;
import com.dotcms.repackage.org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.dotcms.rest.RestClientBuilder;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

/**
 * @author Jonathan Gamba
 *         Date: 3/17/14
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class RemotePublishAjaxActionTest extends TestBase {

	private static User user;
	private static User adminUser;

	@BeforeClass
	public static void prepare () throws DotDataException, DotSecurityException, Exception {
		user = APILocator.getUserAPI().getSystemUser();
		adminUser = APILocator.getUserAPI().loadByUserByEmail( "admin@dotcms.com", user, false );
		
		LicenseTestUtil.getLicense();
	}
	
	/**
	 * Testing the {@link RemotePublishAjaxAction#publish(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} and
	 * the {@link RemotePublishAjaxAction#retry(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} methods but
	 * focusing on the retry functionality in order to test <a href="https://github.com/dotCMS/dotCMS/issues/5097">github issue #5097</a>.
	 *
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws IOException
	 * @throws JSONException
	 * @throws DotPublisherException
	 * @throws InterruptedException
	 */
	@Test
	public void retry_issue5097 () throws DotSecurityException, DotDataException, IOException, JSONException, DotPublisherException, InterruptedException {

		EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
		ContentletAPI contentletAPI = APILocator.getContentletAPI();
		PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
		PublisherAPI publisherAPI = PublisherAPI.getInstance();

		HttpServletRequest req = ServletTestRunner.localRequest.get();

		Environment environment = new Environment();
		environment.setName( "TestEnvironment_" + String.valueOf( new Date().getTime() ) );
		environment.setPushToAll( false );

		//Find the roles of the admin user
		Role role = APILocator.getRoleAPI().loadRoleByKey( adminUser.getUserId() );

		//Create the permissions for the environment
		List<Permission> permissions = new ArrayList<Permission>();
		Permission p = new Permission( environment.getId(), role.getId(), PermissionAPI.PERMISSION_USE );
		permissions.add( p );

		//Create a environment
		environmentAPI.saveEnvironment( environment, permissions );

		//Now we need to create the end point
		PublishingEndPoint endpoint = new PublishingEndPoint();
		endpoint.setServerName( new StringBuilder( "TestEndPoint" + String.valueOf( new Date().getTime() ) ) );
		endpoint.setAddress( "127.0.0.1" );
		endpoint.setPort( "999" );
		endpoint.setProtocol( "http" );
		endpoint.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
		endpoint.setEnabled( true );
		endpoint.setSending( false );
		endpoint.setGroupId( environment.getId() );
		//Save the endpoint.
		publisherEndPointAPI.saveEndPoint( endpoint );

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		//++++++++++++++++++++++++++++++PUBLISH++++++++++++++++++++++++++++

		//Getting test data
		List<Contentlet> contentlets = contentletAPI.findAllContent( 0, 1 );
		//Validations
		assertNotNull( contentlets );
		assertEquals( contentlets.size(), 1 );

		//Preparing the url in order to push content
		SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
		SimpleDateFormat timeFormat = new SimpleDateFormat( "H-m" );
		String publishDate = dateFormat.format( new Date() );
		String publishTime = timeFormat.format( new Date() );

		String baseURL = "http://" + req.getServerName() + ":" + req.getServerPort() + "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish/u/admin@dotcms.com/p/admin";
		String completeURL = baseURL +
				"?remotePublishDate=" + UtilMethods.encodeURIComponent( publishDate ) +
				"&remotePublishTime=" + UtilMethods.encodeURIComponent( publishTime ) +
				"&remotePublishExpireDate=" +
				"&remotePublishExpireTime=" +
				"&iWantTo=" + RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH +
				"&whoToSend=" + UtilMethods.encodeURIComponent( environment.getId() ) +
				"&forcePush=false" +
				"&assetIdentifier=" + UtilMethods.encodeURIComponent( contentlets.get( 0 ).getIdentifier() );

		//Execute the call
		URL publishUrl = new URL( completeURL );
		String response = IOUtils.toString( publishUrl.openStream(), "UTF-8" );
		//Validations
		JSONObject jsonResponse = new JSONObject( response );
		assertNotNull( contentlets );
		assertEquals( jsonResponse.getInt( "errors" ), 0 );
		assertEquals( jsonResponse.getInt( "total" ), 1 );
		assertNotNull( jsonResponse.get( "bundleId" ) );

		//Now that we have a bundle id
		String bundleId = jsonResponse.getString( "bundleId" );
		//First we need to verify if this bundle is in the queue job
		List<PublishQueueElement> foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
		assertNotNull( foundBundles );
		assertTrue( !foundBundles.isEmpty() );

		/*
         Now lets wait until it finished, by the way, we are expecting it to fail to publish as the end point does not exist.
         Keep in mind the queue will try 3 times before to marked as failed to publish, so we have to wait a bit here....
		 */
		int x = 0;
		do {
			Thread.sleep( 60000 );
			//Verify if it continues in the queue job
			foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
			x++;
		} while ( (foundBundles != null && !foundBundles.isEmpty()) && x <= 5 );
		//At this points should not be here anymore
		publisherAPI.deleteAllElementsFromPublishQueueTable();
		foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
		assertTrue( foundBundles != null || !foundBundles.isEmpty() );

		//Get the audit records related to this bundle
		PublishAuditStatus status = PublishAuditAPI.getInstance().getPublishAuditStatus( bundleId );
		//Get current status dates
		Date initialCreationDate = status.getCreateDate();
		Date initialUpdateDate = status.getStatusUpdated();

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		//++++++++++++++++++++++++++++++RETRY++++++++++++++++++++++++++++++
		//Now we can try the retry
		PublishAuditAPI.getInstance().updatePublishAuditStatus(bundleId, PublishAuditStatus.Status.FAILED_TO_PUBLISH, PublishAuditAPI.getInstance().getPublishAuditStatus(bundleId).getStatusPojo());
		req = ServletTestRunner.localRequest.get();
		baseURL = "http://" + req.getServerName() + ":" + req.getServerPort() + "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/retry/u/admin@dotcms.com/p/admin";
		completeURL = baseURL + "?bundlesIds=" + UtilMethods.encodeURIComponent( bundleId );

		//Execute the call
		URL retryUrl = new URL( completeURL );
		response = IOUtils.toString( retryUrl.openStream(), "UTF-8" );//We can expect something like "Bundle id: <strong>1193e3eb-3ccd-496e-8995-29a9fcc48cbd</strong> added successfully to Publishing Queue."
		//Validations
		assertNotNull( response );
		assertTrue( response.contains( bundleId ) );
		//assertTrue( response.contains( "added successfully to" ) );

		//And should be back to the queue job
		foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
		assertTrue( foundBundles != null || !foundBundles.isEmpty() );

		//Get current status dates
		status = PublishAuditAPI.getInstance().getPublishAuditStatus( bundleId );//Get the audit records related to this bundle
		Date latestCreationDate = status.getCreateDate();
		Date latestUpdateDate = status.getStatusUpdated();
		//Validations
		assertNotSame( initialCreationDate, latestCreationDate );
		assertNotSame( initialUpdateDate, latestUpdateDate );
		assertTrue( initialCreationDate.before( latestCreationDate ) );
		assertTrue( initialUpdateDate.before( latestUpdateDate ) );

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		//++++++++++++++++++++SIMULATE AN END POINT++++++++++++++++++++++++
		/*
         And finally lets try to simulate a end point sending directly an already created bundle file to
         the api/bundlePublisher/publish service
		 */

		//Create a receiving end point
		PublishingEndPoint receivingFromEndpoint = new PublishingEndPoint();
		receivingFromEndpoint.setServerName( new StringBuilder( "TestReceivingEndPoint" + String.valueOf( new Date().getTime() ) ) );
		receivingFromEndpoint.setAddress( req.getServerName() );
		receivingFromEndpoint.setPort( String.valueOf( req.getServerPort() ) );
		receivingFromEndpoint.setProtocol( "http" );
		receivingFromEndpoint.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
		receivingFromEndpoint.setEnabled( true );
		receivingFromEndpoint.setSending( true );
		receivingFromEndpoint.setGroupId( environment.getId() );
		//Save the endpoint.
		publisherEndPointAPI.saveEndPoint( receivingFromEndpoint );

		//Find the bundle
		Bundle bundle = APILocator.getBundleAPI().getBundleById( bundleId );
		PublisherConfig basicConfig = new PublisherConfig();
		basicConfig.setId( bundleId );
		File bundleRoot = BundlerUtil.getBundleRoot( basicConfig );
		File bundleFile = new File( bundleRoot + File.separator + ".." + File.separator + bundle.getId() + ".tar.gz" );
		
		//lets wait one minute
		Thread.sleep( 60000 );
		
		assertTrue( bundleFile.exists() );

		//Rename the bundle file
		String newBundleId = UUID.randomUUID().toString();
		File newBundleFile = new File( bundleRoot + File.separator + ".." + File.separator + newBundleId + ".tar.gz" );
		Boolean success = bundleFile.renameTo( newBundleFile );
		assertTrue( success );
		assertTrue( newBundleFile.exists() );

		//Prepare the post request
		FormDataMultiPart form = new FormDataMultiPart();
		form.field( "AUTH_TOKEN", PublicEncryptionFactory.encryptString( (PublicEncryptionFactory.decryptString( receivingFromEndpoint.getAuthKey().toString() )) ) );
		form.field( "GROUP_ID", UtilMethods.isSet( receivingFromEndpoint.getGroupId() ) ? receivingFromEndpoint.getGroupId() : receivingFromEndpoint.getId() );
		form.field( "BUNDLE_NAME", bundle.getName() );
		form.field( "ENDPOINT_ID", receivingFromEndpoint.getId() );
		form.bodyPart( new FileDataBodyPart( "bundle", newBundleFile, MediaType.MULTIPART_FORM_DATA_TYPE ) );

		//Sending bundle to endpoint
        Response clientResponse = ClientBuilder.newClient().register(MultiPartFeature.class)
            .target(receivingFromEndpoint.toURL() + "/api/bundlePublisher/publish")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(form, form.getMediaType()));
		//Validations
		assertEquals( clientResponse.getStatus(), HttpStatus.SC_OK );

		//Get current status dates
		status = PublishAuditAPI.getInstance().getPublishAuditStatus( newBundleId );//Get the audit records related to this new bundle
		Date finalCreationDate = status.getCreateDate();
		Date finalUpdateDate = status.getStatusUpdated();
		//Validations
		assertNotSame( latestCreationDate.getTime(), finalCreationDate.getTime() );
		assertNotSame( latestUpdateDate.getTime(), finalUpdateDate.getTime() );
		
	}

	/**
	 * Check that references to archived pages on a contentlet are not included in the bundle, 
	 * to avoid issues trying to edit those contents in the receiver node
	 * @throws Exception 
	 */
	@Test
	public void push_archived_issue5086 () throws Exception {

		User systemUser = APILocator.getUserAPI().getSystemUser();
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, false);

		/*
		 * Creating testing folder
		 */
        String folderPath = "/testfolder" + UUIDGenerator.generateUuid();
		Folder folder = APILocator.getFolderAPI().createFolders(folderPath, host, systemUser, true);

		/*
		 * Creating testing pages
		 */
    
        Template template=APILocator.getTemplateAPI().findLiveTemplate("9396ac6a-d32c-4539-966e-c776e7562cfb", systemUser, false);
		
		Contentlet newHtmlPage=new Contentlet();
		newHtmlPage.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		newHtmlPage.setHost(host.getIdentifier());
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, "page1");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, "page1");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, "page1");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		newHtmlPage.setFolder(folder.getInode());
        try{
        	HibernateUtil.startTransaction();
        	newHtmlPage=APILocator.getContentletAPI().checkin(newHtmlPage, systemUser, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(RemotePublishAjaxActionTest.class, e.getMessage());
        }
		
        APILocator.getVersionableAPI().setLive(newHtmlPage);
        APILocator.getContentletAPI().publish(newHtmlPage, systemUser, false);
		Contentlet workinghtmlPageAsset = newHtmlPage;
		HibernateUtil.flush();
		
		/*
		 * Creating second test page
		 */
		
		HTMLPage newHtmlPage2 = new HTMLPage();
		newHtmlPage2.setParent(folder.getInode());
		newHtmlPage2.setPageUrl("test2."+Config.getStringProperty("VELOCITY_PAGE_EXTENSION","html"));
		newHtmlPage2.setTitle("test2");
		newHtmlPage2.setFriendlyName("test2");
		newHtmlPage2.setTemplateId(template.getIdentifier());

		if (UtilMethods.isSet(newHtmlPage2.getFriendlyName())) {
			newHtmlPage2.setFriendlyName(newHtmlPage2.getFriendlyName());
		} else {
			newHtmlPage2.setFriendlyName(newHtmlPage2.getTitle());
		}

		if (!newHtmlPage2.getPageUrl().endsWith("." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION","html"))) {
			newHtmlPage2.setPageUrl(newHtmlPage2.getPageUrl() + "." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION","html"));
		}
		WebAssetFactory.createAsset(newHtmlPage2, systemUser.getUserId(), folder);
		APILocator.getVersionableAPI().setLive(newHtmlPage2);

		WebAssetFactory.publishAsset(newHtmlPage2);
		HTMLPage workinghtmlPageAsset2 = null;
		workinghtmlPageAsset2 = newHtmlPage2;
		HibernateUtil.flush();		
		
		/*
		 * Create test contentlet
		 */
		Structure structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
		Contentlet contentlet = new Contentlet();
		contentlet.setStructureInode(structure.getInode());
		contentlet.setHost(host.getIdentifier());
		contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		contentlet.setStringProperty("title", "test5086");
		contentlet.setStringProperty("body", "test5086");
		contentlet.setHost(host.getIdentifier());

		contentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser,false);
		if(APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_PUBLISH, systemUser))
			APILocator.getVersionableAPI().setLive(contentlet);

		/*
		 * relate the page, container and contentlet
		 */
		Container containerId = null;
		for(Container container : APILocator.getTemplateAPI().getContainersInTemplate(template, systemUser, false)){
			if(container.getTitle().equals("Large Column (lg-1)")) {
				containerId = container;
				break;
			}
		}

		/*
		 * Relating content to live page
		 */
		Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find(workinghtmlPageAsset);
		Identifier containerIdentifier = APILocator.getIdentifierAPI().find(containerId);
		Identifier contenletIdentifier = APILocator.getIdentifierAPI().find(contentlet);
		MultiTree multiTree = MultiTreeFactory.getMultiTree(htmlPageIdentifier, containerIdentifier,contenletIdentifier);
		int contentletCount = MultiTreeFactory.getMultiTree(workinghtmlPageAsset.getInode()).size();

		if (!InodeUtils.isSet(multiTree.getParent1()) && !InodeUtils.isSet(multiTree.getParent2()) && !InodeUtils.isSet(multiTree.getChild())) {
			MultiTree mTree = new MultiTree(htmlPageIdentifier.getInode(), containerIdentifier.getInode(),
					contenletIdentifier.getInode(),null,contentletCount);
			MultiTreeFactory.saveMultiTree(mTree);
		}

		/*
		 * Relating content to archived page
		 */
		htmlPageIdentifier = APILocator.getIdentifierAPI().find(workinghtmlPageAsset2);
		multiTree = MultiTreeFactory.getMultiTree(htmlPageIdentifier, containerIdentifier,contenletIdentifier);
		contentletCount = MultiTreeFactory.getMultiTree(workinghtmlPageAsset.getInode()).size();

		if (!InodeUtils.isSet(multiTree.getParent1()) && !InodeUtils.isSet(multiTree.getParent2()) && !InodeUtils.isSet(multiTree.getChild())) {
			MultiTree mTree = new MultiTree(htmlPageIdentifier.getInode(), containerIdentifier.getInode(),
					contenletIdentifier.getInode(),null,contentletCount);
			MultiTreeFactory.saveMultiTree(mTree);
		}

		/*
		 * Archiving second page
		 */
		
		WebAssetFactory.unPublishAsset(workinghtmlPageAsset2, systemUser.getUserId(), folder);
		WebAssetFactory.archiveAsset(workinghtmlPageAsset2);
		
		/*
		 * Validations
		 */
		assertTrue(workinghtmlPageAsset.isLive());
		assertTrue(workinghtmlPageAsset2.isArchived());
		List<Map<String,Object>> references = APILocator.getContentletAPI().getContentletReferences(contentlet, systemUser, false);
		assertTrue(references.size() == 2);

		/*
		 * Generate test environment
		 */
		//Preparing the url in order to push content
		EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
		PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
		PublisherAPI publisherAPI = PublisherAPI.getInstance();

		HttpServletRequest req = ServletTestRunner.localRequest.get();

		Environment environment = new Environment();
		environment.setName( "TestEnvironment_" + String.valueOf( new Date().getTime() ) );
		environment.setPushToAll( false );

		/*
		 * Find the roles of the admin user
		 */
		Role role = APILocator.getRoleAPI().loadRoleByKey( adminUser.getUserId() );

		//Create the permissions for the environment
		List<Permission> permissions = new ArrayList<Permission>();
		Permission p = new Permission( environment.getId(), role.getId(), PermissionAPI.PERMISSION_USE );
		permissions.add( p );

		/*
		 * Create a environment
		 */
		environmentAPI.saveEnvironment( environment, permissions );

		/*
		 * Now we need to create the end point
		 */
		PublishingEndPoint endpoint = new PublishingEndPoint();
		endpoint.setServerName( new StringBuilder( "TestEndPoint" + String.valueOf( new Date().getTime() ) ) );
		endpoint.setAddress( "127.0.0.1" );
		endpoint.setPort( "999" );
		endpoint.setProtocol( "http" );
		endpoint.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
		endpoint.setEnabled( true );
		endpoint.setSending( false );
		endpoint.setGroupId( environment.getId() );
		/*
		 * Save the endpoint.
		 */
		publisherEndPointAPI.saveEndPoint( endpoint );
		SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
		SimpleDateFormat timeFormat = new SimpleDateFormat( "H-m" );
		String publishDate = dateFormat.format( new Date() );
		String publishTime = timeFormat.format( new Date() );

		String baseURL = "http://" + req.getServerName() + ":" + req.getServerPort() + "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish/u/admin@dotcms.com/p/admin";
		String completeURL = baseURL +
				"?remotePublishDate=" + UtilMethods.encodeURIComponent( publishDate ) +
				"&remotePublishTime=" + UtilMethods.encodeURIComponent( publishTime ) +
				"&remotePublishExpireDate=" +
				"&remotePublishExpireTime=" +
				"&iWantTo=" + RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH +
				"&whoToSend=" + UtilMethods.encodeURIComponent( environment.getId() ) +
				"&forcePush=false" +
				"&assetIdentifier=" + UtilMethods.encodeURIComponent( folder.getInode() );

		/*
		 * Execute the call
		 */
		URL publishUrl = new URL( completeURL );
		String response = IOUtils.toString( publishUrl.openStream(), "UTF-8" );
		/*
		 * Validations
		 */
		JSONObject jsonResponse = new JSONObject( response );
		assertEquals( jsonResponse.getInt( "errors" ), 0 );
		assertEquals( jsonResponse.getInt( "total" ), 1 );
		assertNotNull( jsonResponse.get( "bundleId" ) );

		/*
		 * Now that we have a bundle id
		 */
		String bundleId = jsonResponse.getString( "bundleId" );
		/*
		 * First we need to verify if this bundle is in the queue job
		 */
		List<PublishQueueElement> foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
		assertNotNull( foundBundles );
		assertTrue( !foundBundles.isEmpty() );

		/*
		 *        Now lets wait until it finished, by the way, we are expecting it to fail to publish as the end point does not exist.
		 *        Keep in mind the queue will try 3 times before to marked as failed to publish, so we have to wait a bit here....
		 */
		int x = 0;
		do {
			Thread.sleep( 60000 );
			//Verify if it continues in the queue job
			foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
			x++;
		} while ( (foundBundles != null && !foundBundles.isEmpty()) && x <= 5 );
		//At this points should not be here anymore
		publisherAPI.deleteAllElementsFromPublishQueueTable();
		foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
		assertTrue( foundBundles != null || !foundBundles.isEmpty() );
		
		
		/*
		 * Get the audit records related to this bundle
		 */
		PublishAuditStatus status = PublishAuditAPI.getInstance().getPublishAuditStatus( bundleId );
		/*
		 * We will be able to retry failed and successfully bundles
		 */
		assertEquals( PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS, status.getStatus() ); //Remember, we are expecting this to fail

		/*
		 * deleting folder, pages and content, to create the receiving endpoint environment
		 */
        try{
        	HibernateUtil.startTransaction();
        	APILocator.getContentletAPI().delete(contentlet, systemUser, false, true);
    		APILocator.getContentletAPI().unpublish(workinghtmlPageAsset, systemUser,false);
    		APILocator.getContentletAPI().archive(workinghtmlPageAsset,systemUser,false);
    		APILocator.getContentletAPI().delete(workinghtmlPageAsset, systemUser, true);
    		APILocator.getHTMLPageAPI().delete(workinghtmlPageAsset2, systemUser, true);
    		APILocator.getFolderAPI().delete(folder, systemUser, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(RemotePublishAjaxActionTest.class, e.getMessage());
        }
		
	
		Assert.assertEquals(0,MultiTreeFactory.getMultiTree(workinghtmlPageAsset.getInode()).size());
		Assert.assertEquals(0,MultiTreeFactory.getMultiTree(workinghtmlPageAsset2.getInode()).size());
		Assert.assertEquals(0,MultiTreeFactory.getMultiTreeByChild(contentlet.getIdentifier()).size());

		folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, systemUser, false);
		assertTrue(!UtilMethods.isSet(folder.getInode()));

		/*
		 * Find the bundle
		 * SIMULATE AN END POINT
		 *
		 * And finally lets try to simulate a end point sending directly an already created bundle file to
		 * the api/bundlePublisher/publish service
		 */

		/*
		 * Create a receiving end point
		 */
		PublishingEndPoint receivingFromEndpoint = new PublishingEndPoint();
		receivingFromEndpoint.setServerName( new StringBuilder( "TestReceivingEndPoint" + String.valueOf( new Date().getTime() ) ) );
		receivingFromEndpoint.setAddress( req.getServerName() );
		receivingFromEndpoint.setPort( String.valueOf( req.getServerPort() ) );
		receivingFromEndpoint.setProtocol( "http" );
		receivingFromEndpoint.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
		receivingFromEndpoint.setEnabled( true );
		receivingFromEndpoint.setSending( true );//TODO: Shouldn't this be false as we are creating this end point to receive bundles from another server..?
		receivingFromEndpoint.setGroupId( environment.getId() );
		/*
		 * Save the endpoint.
		 */
		publisherEndPointAPI.saveEndPoint( receivingFromEndpoint );

		/*
		 * Find the bundle
		 */
		Bundle bundle = APILocator.getBundleAPI().getBundleById( bundleId );
		PublisherConfig basicConfig = new PublisherConfig();
		basicConfig.setId( bundleId );
		File bundleRoot = BundlerUtil.getBundleRoot( basicConfig );
		File bundleFile = new File( bundleRoot + File.separator + ".." + File.separator + bundle.getId() + ".tar.gz" );
		assertTrue( bundleFile.exists() );

		/*
		 * Rename the bundle file
		 */
		String newBundleId = UUID.randomUUID().toString();
		File newBundleFile = new File( bundleRoot + File.separator + ".." + File.separator + newBundleId + ".tar.gz" );
		Boolean success = bundleFile.renameTo( newBundleFile );
		assertTrue( success );
		assertTrue( newBundleFile.exists() );
		
		/*
		 * Cleaning test values
		 */
		//TODO: We have the improve this test because of the new license updates
		//APILocator.getHTMLPageAPI().delete(page, systemUser, true);
		APILocator.getFolderAPI().delete(folder, systemUser, false);
		
	}

	/**
	 * Check that content reorder changes in a page are reflected in the push publishing
	 * @throws Exception 
	 */
	@Test
	public void push_container_issue5189 () throws Exception {

		User systemUser = APILocator.getUserAPI().getSystemUser();
		Host host = APILocator.getHostAPI().findDefaultHost(systemUser, false);

		/*
		 * Creating testing folder
		 */
		String folderPath = "/testfolder_" + String.valueOf( new Date().getTime() );
		Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, systemUser, true);
		if(!UtilMethods.isSet(folder.getInode())){
			folder = APILocator.getFolderAPI().createFolders(folderPath, host, systemUser, true);

			Permission p = new Permission();
			p.setInode(folder.getPermissionId());
			p.setRoleId(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_READ);
			APILocator.getPermissionAPI().save(p, folder, systemUser, true);
		}

		/*
		 * Creating testing pages
		 */
		// Adds template children from selected box
		//Identifier templateIdentifier = identifierAPI.find(templateId);
		Template template=APILocator.getTemplateAPI().findLiveTemplate("9396ac6a-d32c-4539-966e-c776e7562cfb", systemUser, false);

		Contentlet newHtmlPage=new Contentlet();
		newHtmlPage.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		newHtmlPage.setHost(host.getIdentifier());
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, "page1");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, "page1");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, "page1");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		newHtmlPage.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		newHtmlPage.setFolder(folder.getInode());
		newHtmlPage=APILocator.getContentletAPI().checkin(newHtmlPage, systemUser, false);
        APILocator.getVersionableAPI().setLive(newHtmlPage);
        APILocator.getContentletAPI().publish(newHtmlPage, systemUser, false);
		Contentlet workinghtmlPageAsset = newHtmlPage;
		HibernateUtil.flush();

		/*
		 * relate the page, container and contentlet
		 */
		Container containerId = null;
		for(Container container : APILocator.getTemplateAPI().getContainersInTemplate(template, systemUser, false)){
			if(container.getTitle().equals("Large Column (lg-1)")){
				containerId = container;
				break;
			}
		}

		/*
		 * Create test contentlet1
		 */
		Structure structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
		Contentlet contentlet1 = new Contentlet();
		contentlet1.setStructureInode(structure.getInode());
		contentlet1.setHost(host.getIdentifier());
		contentlet1.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		contentlet1.setStringProperty("title", "test5189-1");
		contentlet1.setStringProperty("body", "test5189-1");
		contentlet1.setHost(host.getIdentifier());

		contentlet1 = APILocator.getContentletAPI().checkin(contentlet1, systemUser,false);
		if(APILocator.getPermissionAPI().doesUserHavePermission(contentlet1, PermissionAPI.PERMISSION_PUBLISH, systemUser))
			APILocator.getVersionableAPI().setLive(contentlet1);

		Contentlet contentlet2 = new Contentlet();
		contentlet2.setStructureInode(structure.getInode());
		contentlet2.setHost(host.getIdentifier());
		contentlet2.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		contentlet2.setStringProperty("title", "test5189-2");
		contentlet2.setStringProperty("body", "test5189-2");
		contentlet2.setHost(host.getIdentifier());

		contentlet2 = APILocator.getContentletAPI().checkin(contentlet2, systemUser,false);
		if(APILocator.getPermissionAPI().doesUserHavePermission(contentlet2, PermissionAPI.PERMISSION_PUBLISH, systemUser))
			APILocator.getVersionableAPI().setLive(contentlet2);

		Contentlet contentlet3 = new Contentlet();
		contentlet3.setStructureInode(structure.getInode());
		contentlet3.setHost(host.getIdentifier());
		contentlet3.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
		contentlet3.setStringProperty("title", "test5189-3");
		contentlet3.setStringProperty("body", "test5189-3");
		contentlet3.setHost(host.getIdentifier());

		contentlet3 = APILocator.getContentletAPI().checkin(contentlet3, systemUser,false);
		if(APILocator.getPermissionAPI().doesUserHavePermission(contentlet3, PermissionAPI.PERMISSION_PUBLISH, systemUser))
			APILocator.getVersionableAPI().setLive(contentlet3);

		/*
		 * Relating content to live page
		 */
		Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find(workinghtmlPageAsset);
		Identifier containerIdentifier = APILocator.getIdentifierAPI().find(containerId);
		Identifier contenletIdentifier1 = APILocator.getIdentifierAPI().find(contentlet1);
		MultiTree multiTree = MultiTreeFactory.getMultiTree(htmlPageIdentifier, containerIdentifier,contenletIdentifier1);
		int contentletCount = MultiTreeFactory.getMultiTree(htmlPageIdentifier).size();

		if (!InodeUtils.isSet(multiTree.getParent1()) && !InodeUtils.isSet(multiTree.getParent2()) && !InodeUtils.isSet(multiTree.getChild())) {
			MultiTree mTree = new MultiTree(htmlPageIdentifier.getInode(), containerIdentifier.getInode(),
					contenletIdentifier1.getInode(),null,contentletCount);
			MultiTreeFactory.saveMultiTree(mTree);
		}

		Identifier contenletIdentifier2 = APILocator.getIdentifierAPI().find(contentlet2);
		multiTree = MultiTreeFactory.getMultiTree(htmlPageIdentifier, containerIdentifier,contenletIdentifier2);
		contentletCount = MultiTreeFactory.getMultiTree(htmlPageIdentifier).size();

		if (!InodeUtils.isSet(multiTree.getParent1()) && !InodeUtils.isSet(multiTree.getParent2()) && !InodeUtils.isSet(multiTree.getChild())) {
			MultiTree mTree = new MultiTree(htmlPageIdentifier.getInode(), containerIdentifier.getInode(),
					contenletIdentifier2.getInode(),null,contentletCount);
			MultiTreeFactory.saveMultiTree(mTree);
		}

		Identifier contenletIdentifier3 = APILocator.getIdentifierAPI().find(contentlet3);
		multiTree = MultiTreeFactory.getMultiTree(htmlPageIdentifier, containerIdentifier,contenletIdentifier3);
		contentletCount = MultiTreeFactory.getMultiTree(htmlPageIdentifier).size();

		if (!InodeUtils.isSet(multiTree.getParent1()) && !InodeUtils.isSet(multiTree.getParent2()) && !InodeUtils.isSet(multiTree.getChild())) {
			MultiTree mTree = new MultiTree(htmlPageIdentifier.getInode(), containerIdentifier.getInode(),
					contenletIdentifier3.getInode(),null,contentletCount);
			MultiTreeFactory.saveMultiTree(mTree);
		}


		/*
		 * Validations
		 */
		assertTrue(workinghtmlPageAsset.isLive());
		contentletCount = MultiTreeFactory.getMultiTree(htmlPageIdentifier).size();
		assertTrue(contentletCount == 3);

		/*
		 * Generate test environment
		 */
		//Preparing the url in order to push content
		EnvironmentAPI environmentAPI = APILocator.getEnvironmentAPI();
		PublishingEndPointAPI publisherEndPointAPI = APILocator.getPublisherEndPointAPI();
		PublisherAPI publisherAPI = PublisherAPI.getInstance();

		HttpServletRequest req = ServletTestRunner.localRequest.get();

		Environment environment = new Environment();
		environment.setName( "TestEnvironment_" + String.valueOf( new Date().getTime() ) );
		environment.setPushToAll( false );

		/*
		 * Find the roles of the admin user
		 */
		Role role = APILocator.getRoleAPI().loadRoleByKey( adminUser.getUserId() );

		//Create the permissions for the environment
		List<Permission> permissions = new ArrayList<Permission>();
		Permission p = new Permission( environment.getId(), role.getId(), PermissionAPI.PERMISSION_USE );
		permissions.add( p );

		/*
		 * Create a environment
		 */
		environmentAPI.saveEnvironment( environment, permissions );

		/*
		 * Now we need to create the end point
		 */
		PublishingEndPoint endpoint = new PublishingEndPoint();
		endpoint.setServerName( new StringBuilder( "TestEndPoint" + String.valueOf( new Date().getTime() ) ) );
		endpoint.setAddress( "127.0.0.1" );
		endpoint.setPort( "9999" );
		endpoint.setProtocol( "http" );
		endpoint.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
		endpoint.setEnabled( true );
		endpoint.setSending( false );//TODO: Shouldn't this be true as we are creating this end point to send bundles to another server..?
		endpoint.setGroupId( environment.getId() );
		/*
		 * Save the endpoint.
		 */
		publisherEndPointAPI.saveEndPoint( endpoint );
		SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
		SimpleDateFormat timeFormat = new SimpleDateFormat( "H-m" );
		String publishDate = dateFormat.format( new Date() );
		String publishTime = timeFormat.format( new Date() );

		String baseURL = "http://" + req.getServerName() + ":" + req.getServerPort() + "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish/u/admin@dotcms.com/p/admin";
		String completeURL = baseURL +
				"?remotePublishDate=" + UtilMethods.encodeURIComponent( publishDate ) +
				"&remotePublishTime=" + UtilMethods.encodeURIComponent( publishTime ) +
				"&remotePublishExpireDate=" +
				"&remotePublishExpireTime=" +
				"&iWantTo=" + RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH +
				"&whoToSend=" + UtilMethods.encodeURIComponent( environment.getId() ) +
				"&forcePush=false" +
				"&assetIdentifier=" + UtilMethods.encodeURIComponent( folder.getInode() );

		/*
		 * Execute the call
		 */
		URL publishUrl = new URL( completeURL );
		String response = IOUtils.toString( publishUrl.openStream(), "UTF-8" );
		/*
		 * Validations
		 */
		JSONObject jsonResponse = new JSONObject( response );
		assertEquals( jsonResponse.getInt( "errors" ), 0 );
		assertEquals( jsonResponse.getInt( "total" ), 1 );
		assertNotNull( jsonResponse.get( "bundleId" ) );

		/*
		 * Now that we have a bundle id
		 */
		String bundleId = jsonResponse.getString( "bundleId" );
		/*
		 * First we need to verify if this bundle is in the queue job
		 */
		List<PublishQueueElement> foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
		assertNotNull( foundBundles );
		assertTrue( !foundBundles.isEmpty() );

		/*
		 *        Now lets wait until it finished, by the way, we are expecting it to fail to publish as the end point does not exist.
		 *        Keep in mind the queue will try 3 times before to marked as failed to publish, so we have to wait a bit here....
		 */
		int x = 0;
		do {
			Thread.sleep( 60000 );
			//Verify if it continues in the queue job
			foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
			x++;
		} while ( (foundBundles != null && !foundBundles.isEmpty()) && x <= 5 );
		//At this points should not be here anymore
		publisherAPI.deleteAllElementsFromPublishQueueTable();
		foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
		assertTrue( foundBundles != null || !foundBundles.isEmpty() );
		
		
		/*
		 * Get the audit records related to this bundle
		 */
		PublishAuditStatus status = PublishAuditAPI.getInstance().getPublishAuditStatus( bundleId );
		/*
		 * We will be able to retry failed and successfully bundles
		 */
		assertEquals( PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS, status.getStatus() ); //Remember, we are expecting this to fail

		/*
		 * deleting folder, pages and contents, to create the receiving endpoint environment
		 */
        try{
        	HibernateUtil.startTransaction();
    		APILocator.getContentletAPI().delete(contentlet1, systemUser, false, true);
    		APILocator.getContentletAPI().delete(contentlet2, systemUser, false, true);
    		APILocator.getContentletAPI().delete(contentlet3, systemUser, false, true);
    		APILocator.getContentletAPI().unpublish(workinghtmlPageAsset, systemUser, false);
    		APILocator.getContentletAPI().archive(workinghtmlPageAsset, systemUser, false);
    		APILocator.getContentletAPI().delete(workinghtmlPageAsset, systemUser, false);
    		APILocator.getFolderAPI().delete(folder, systemUser, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(RemotePublishAjaxActionTest.class, e.getMessage());
        }

		folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, systemUser, false);
		assertTrue(!UtilMethods.isSet(folder.getInode()));

		/*
		 * Find the bundle
		 * SIMULATE AN END POINT
		 *
		 * And finally lets try to simulate a end point sending directly an already created bundle file to
		 * the api/bundlePublisher/publish service
		 */

		/*
		 * Create a receiving end point
		 */
		PublishingEndPoint receivingFromEndpoint = new PublishingEndPoint();
		receivingFromEndpoint.setServerName( new StringBuilder( "TestReceivingEndPoint" + String.valueOf( new Date().getTime() ) ) );
		receivingFromEndpoint.setAddress( req.getServerName() );
		receivingFromEndpoint.setPort( String.valueOf( req.getServerPort() ) );
		receivingFromEndpoint.setProtocol( "http" );
		receivingFromEndpoint.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
		receivingFromEndpoint.setEnabled( true );
		receivingFromEndpoint.setSending( true );//TODO: Shouldn't this be false as we are creating this end point to receive bundles from another server..?
		receivingFromEndpoint.setGroupId( environment.getId() );
		/*
		 * Save the endpoint.
		 */
		publisherEndPointAPI.saveEndPoint( receivingFromEndpoint );

		/*
		 * Find the bundle
		 */
		Bundle bundle = APILocator.getBundleAPI().getBundleById( bundleId );
		PublisherConfig basicConfig = new PublisherConfig();
		basicConfig.setId( bundleId );
		File bundleRoot = BundlerUtil.getBundleRoot( basicConfig );
		File bundleFile = new File( bundleRoot + File.separator + ".." + File.separator + bundle.getId() + ".tar.gz" );
		assertTrue( bundleFile.exists() );

		/*
		 * Rename the bundle file
		 */
		String newBundleId = UUID.randomUUID().toString();
		File newBundleFile = new File( bundleRoot + File.separator + ".." + File.separator + newBundleId + ".tar.gz" );
		Boolean success = bundleFile.renameTo( newBundleFile );
		assertTrue( success );
		assertTrue( newBundleFile.exists() );

		/*
		 * Prepare the post request
		 */
		Client client = RestClientBuilder.newClient();

		FormDataMultiPart form = new FormDataMultiPart();
		form.field( "AUTH_TOKEN", PublicEncryptionFactory.encryptString( (PublicEncryptionFactory.decryptString( receivingFromEndpoint.getAuthKey().toString() )) ) );
		form.field( "GROUP_ID", UtilMethods.isSet( receivingFromEndpoint.getGroupId() ) ? receivingFromEndpoint.getGroupId() : receivingFromEndpoint.getId() );
		form.field( "BUNDLE_NAME", bundle.getName() );
		form.field( "ENDPOINT_ID", receivingFromEndpoint.getId() );
		form.bodyPart( new FileDataBodyPart( "bundle", newBundleFile, MediaType.MULTIPART_FORM_DATA_TYPE ) );

		/*
		 * Cleaning test values
		 */
        try{
        	HibernateUtil.startTransaction();
    		APILocator.getContentletAPI().delete(contentlet1, systemUser, false, true);
    		APILocator.getContentletAPI().delete(contentlet2, systemUser, false, true);
    		APILocator.getContentletAPI().delete(contentlet3, systemUser, false, true);
    		//APILocator.getHTMLPageAPI().delete(page, systemUser, true);
    		APILocator.getFolderAPI().delete(folder, systemUser, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(RemotePublishAjaxActionTest.class, e.getMessage());
        }
	}
}
