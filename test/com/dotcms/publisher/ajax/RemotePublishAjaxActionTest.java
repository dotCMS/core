package com.dotcms.publisher.ajax;

import com.dotcms.TestBase;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.*;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.repackage.commons_httpclient_3_1.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.commons_io_2_0_1.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.jersey_1_12.com.sun.jersey.api.client.Client;
import com.dotcms.repackage.jersey_1_12.com.sun.jersey.api.client.ClientResponse;
import com.dotcms.repackage.jersey_1_12.com.sun.jersey.api.client.WebResource;
import com.dotcms.repackage.jersey_1_12.com.sun.jersey.api.client.config.ClientConfig;
import com.dotcms.repackage.jersey_1_12.com.sun.jersey.api.client.config.DefaultClientConfig;
import com.dotcms.repackage.jersey_1_12.com.sun.jersey.multipart.FormDataMultiPart;
import com.dotcms.repackage.jersey_1_12.com.sun.jersey.multipart.file.FileDataBodyPart;
import com.dotcms.repackage.jersey_1_12.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.junit_4_8_1.org.junit.BeforeClass;
import com.dotcms.repackage.junit_4_8_1.org.junit.Test;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.dotcms.repackage.junit_4_8_1.org.junit.Assert.*;

/**
 * @author Jonathan Gamba
 *         Date: 3/17/14
 */
public class RemotePublishAjaxActionTest extends TestBase {

    private static User user;
    private static User adminUser;

    @BeforeClass
    public static void prepare () throws DotDataException, DotSecurityException {
        user = APILocator.getUserAPI().getSystemUser();
        adminUser = APILocator.getUserAPI().loadByUserByEmail( "admin@dotcms.com", user, false );
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
        endpoint.setAddress( "222.222.222.222" );
        endpoint.setPort( "9999" );
        endpoint.setProtocol( "http" );
        endpoint.setAuthKey( new StringBuilder( PublicEncryptionFactory.encryptString( "1111" ) ) );
        endpoint.setEnabled( true );
        endpoint.setSending( false );//TODO: Shouldn't this be true as we are creating this end point to send bundles to another server..?
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
            Thread.sleep( 3000 );
            //Verify if it continues in the queue job
            foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
            x++;
        } while ( (foundBundles != null && !foundBundles.isEmpty()) && x < 100 );
        //At this points should not be here anymore
        foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
        assertTrue( foundBundles == null || foundBundles.isEmpty() );

        //Get the audit records related to this bundle
        PublishAuditStatus status = PublishAuditAPI.getInstance().getPublishAuditStatus( bundleId );
        //We will be able to retry failed and successfully bundles
        assertEquals( status.getStatus(), PublishAuditStatus.Status.FAILED_TO_PUBLISH ); //Remember, we are expecting this to fail

        //Get current status dates
        Date initialCreationDate = status.getCreateDate();
        Date initialUpdateDate = status.getStatusUpdated();

        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //++++++++++++++++++++++++++++++RETRY++++++++++++++++++++++++++++++
        //Now we can try the retry
        req = ServletTestRunner.localRequest.get();
        baseURL = "http://" + req.getServerName() + ":" + req.getServerPort() + "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/retry/u/admin@dotcms.com/p/admin";
        completeURL = baseURL + "?bundlesIds=" + UtilMethods.encodeURIComponent( bundleId );

        //Execute the call
        URL retryUrl = new URL( completeURL );
        response = IOUtils.toString( retryUrl.openStream(), "UTF-8" );//We can expect something like "Bundle id: <strong>1193e3eb-3ccd-496e-8995-29a9fcc48cbd</strong> added successfully to Publishing Queue."
        //Validations
        assertNotNull( response );
        assertTrue( response.contains( bundleId ) );
        assertTrue( response.contains( "added successfully to" ) );

        //And should be back to the queue job
        foundBundles = publisherAPI.getQueueElementsByBundleId( bundleId );
        assertNotNull( foundBundles );
        assertTrue( !foundBundles.isEmpty() );

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
        receivingFromEndpoint.setSending( true );//TODO: Shouldn't this be false as we are creating this end point to receive bundles from another server..?
        receivingFromEndpoint.setGroupId( environment.getId() );
        //Save the endpoint.
        publisherEndPointAPI.saveEndPoint( receivingFromEndpoint );

        //Find the bundle
        Bundle bundle = APILocator.getBundleAPI().getBundleById( bundleId );
        PublisherConfig basicConfig = new PublisherConfig();
        basicConfig.setId( bundleId );
        File bundleRoot = BundlerUtil.getBundleRoot( basicConfig );
        File bundleFile = new File( bundleRoot + File.separator + ".." + File.separator + bundle.getId() + ".tar.gz" );
        assertTrue( bundleFile.exists() );

        //Rename the bundle file
        String newBundleId = UUID.randomUUID().toString();
        File newBundleFile = new File( bundleRoot + File.separator + ".." + File.separator + newBundleId + ".tar.gz" );
        Boolean success = bundleFile.renameTo( newBundleFile );
        assertTrue( success );
        assertTrue( newBundleFile.exists() );

        //Prepare the post request
        ClientConfig cc = new DefaultClientConfig();
        Client client = Client.create( cc );

        FormDataMultiPart form = new FormDataMultiPart();
        form.field( "AUTH_TOKEN", PublicEncryptionFactory.encryptString( (PublicEncryptionFactory.decryptString( receivingFromEndpoint.getAuthKey().toString() )) ) );
        form.field( "GROUP_ID", UtilMethods.isSet( receivingFromEndpoint.getGroupId() ) ? receivingFromEndpoint.getGroupId() : receivingFromEndpoint.getId() );
        form.field( "BUNDLE_NAME", bundle.getName() );
        form.field( "ENDPOINT_ID", receivingFromEndpoint.getId() );
        form.bodyPart( new FileDataBodyPart( "bundle", newBundleFile, MediaType.MULTIPART_FORM_DATA_TYPE ) );

        //Sending bundle to endpoint
        WebResource resource = client.resource( receivingFromEndpoint.toURL() + "/api/bundlePublisher/publish" );
        ClientResponse clientResponse = resource.type( MediaType.MULTIPART_FORM_DATA ).post( ClientResponse.class, form );
        //Validations
        assertEquals( clientResponse.getClientResponseStatus().getStatusCode(), HttpStatus.SC_OK );

        //Get current status dates
        status = PublishAuditAPI.getInstance().getPublishAuditStatus( newBundleId );//Get the audit records related to this new bundle
        Date finalCreationDate = status.getCreateDate();
        Date finalUpdateDate = status.getStatusUpdated();
        //Validations
        assertNotSame( latestCreationDate, finalCreationDate );
        assertNotSame( latestUpdateDate, finalUpdateDate );
        assertTrue( latestCreationDate.before( finalCreationDate ) );
        assertTrue( latestUpdateDate.before( finalUpdateDate ) );
    }

}