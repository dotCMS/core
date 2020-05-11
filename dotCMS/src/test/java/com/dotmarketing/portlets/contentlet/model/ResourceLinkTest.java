package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.ResourceLink.ResourceLinkBuilder;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceLinkTest {

    private static final String HOST_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    private static final String USER_ADMIN_ID = "dotcms.org.1";

    private ContentType mockFileAssetContentType(){
        return new FileAssetContentType(){

            @Override
            public String name() {
                return "File Asset";
            }

            @Override
            public String id() {
                return "33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d";
            }

            @Override
            public String description() {
                return "Default structure for all uploaded files";
            }

            @Override
            public String variable() {
                return "FileAsset";
            }
        };
    }

    private ResourceLinkBuilder getResourceLinkBuilder(final String hostName, final String path, final String mimeType, final String htmlFileName){
        final ResourceLinkBuilder resourceLinkBuilder = new ResourceLink.ResourceLinkBuilder(){

            @Override
            Host getHost(final String hostId, final User user) throws DotDataException, DotSecurityException {
                final Host host = mock(Host.class);
                when(host.getHostname()).thenReturn(hostName);
                return host;
            }

            @Override
            Identifier getIdentifier(final Contentlet contentlet) throws DotDataException {
                if(contentlet.isNew()){
                   return null;
                }
                final Identifier identifier = mock(Identifier.class);
                when(identifier.getInode()).thenReturn("83864b2c-3988-4acc-953d-ff8d0ba5e093");
                when(identifier.getParentPath()).thenReturn(path);
                return identifier;
            }

            @Override
            String getMimiType(final File binary) {
                return mimeType;
            }

            @Override
            boolean isDownloadPermissionBasedRestricted(final Contentlet contentlet,
                    final User user) throws DotDataException {
                return !USER_ADMIN_ID.equals(user.getUserId());
            }

            @Override
            Tuple2<String, String> createVersionPathIdPath (final Contentlet contentlet, final String velocityVarName,
                                                            final File binary) throws DotDataException {

                return Tuple.of("/dA/" + APILocator.getShortyAPI().shortify(contentlet.getInode()) + "/" + velocityVarName + "/" + binary.getName(),
                        "/dA/" + APILocator.getShortyAPI().shortify(contentlet.getIdentifier()) + "/" + velocityVarName + "/" + binary.getName());
            }
        };
        return resourceLinkBuilder;
    }

    private User mockAdminUser(){
        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn(USER_ADMIN_ID);
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");
        return adminUser;
    }

    private User mockLimitedUser(){
        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("anonymous");
        when(adminUser.getEmailAddress()).thenReturn("anonymous@dotcmsfakeemail.org");
        when(adminUser.getFirstName()).thenReturn("anonymous user");
        when(adminUser.getLastName()).thenReturn("anonymous");
        return adminUser;
    }

    @Test
    public void test_Html_ResourceLink_Expect_Downloadable_No_Port_Number() throws Exception{

        final String mimeType = "text/html";
        final String path = "/application/comments/angular/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 1L;
        final boolean isSecure = false;

        final File file = FileUtil.createTemporalFile("comments-list", "html", "This is a test temporal file");

        final String htmlFileName = file.getName();
        final User adminUser = mockAdminUser();

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getBinary(FileAssetAPI.BINARY_FIELD)).thenReturn(file);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertFalse(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/application/comments/angular/"+htmlFileName+"?language_id=1",link.getResourceLinkAsString());

    }

    @Test
    public void test_html_ResourceLink_Expect_Downloadable_Secure_Site_Port_Number() throws Exception{

        final String mimeType = "text/html";
        final String path = "/application/comments/angular/";
        final String hostName = "localhost";
        final long languageId = 1L;
        final boolean isSecure = false;

        final File file = FileUtil.createTemporalFile("comments-list", "html", "This is a test temporal file");

        final String htmlFileName = file.getName();

        final User adminUser = mockAdminUser();

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getBinary(FileAssetAPI.BINARY_FIELD)).thenReturn(file);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(false);
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());



        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(8080);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertEquals("http://localhost:8080/application/comments/angular/"+htmlFileName+"?language_id=1",link.getResourceLinkAsString());
        assertFalse(link.isDownloadRestricted());
    }


    @Test
    public void test_vtl_ResourceLink_WithAdminUser_Expect_Downloadable_No_Port_Number() throws Exception{

        final String mimeType = "text/velocity";
        final String path = "/application/comments/angular/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final File file = FileUtil.createTemporalFile("widget-code", "vtl", "This is a test temporal file");

        final String htmlFileName = file.getName();

        final User adminUser = mockAdminUser();

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getBinary(FileAssetAPI.BINARY_FIELD)).thenReturn(file);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertFalse(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/application/comments/angular/"+htmlFileName+"?language_id=2",link.getResourceLinkAsString());

    }

    @Test
    public void test_vtl_ResourceLink_WithLimitedUser_Expect_Downloadable_No_Port_Number() throws Exception{

        final String mimeType = "text/velocity";
        final String path = "/application/comments/angular/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final User limitedUser = mockLimitedUser();

        final File file = FileUtil.createTemporalFile("widget-code", "vtl", "This is a test temporal file");

        final String htmlFileName = file.getName();


        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getBinary(FileAssetAPI.BINARY_FIELD)).thenReturn(file);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, limitedUser, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/application/comments/angular/"+htmlFileName+"?language_id=2",link.getResourceLinkAsString());

    }



    @Test
    public void test_vm_ResourceLink_With_Admin_User_Expect_Downloadable_NoPortNumber() throws Exception{

        final String mimeType = "text/velocity";
        final String path = "/any/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final File file = FileUtil.createTemporalFile("any", "vm", "This is a test temporal file");

        final String htmlFileName = file.getName();

        final User adminUser = mockAdminUser();

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getBinary(FileAssetAPI.BINARY_FIELD)).thenReturn(file);
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertFalse(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/any/"+htmlFileName+"?language_id=2",link.getResourceLinkAsString());

    }

    @Test
    public void test_vm_ResourceLink_With_LimitedUser_ExpectRestricted_NoPortNumber() throws Exception{

        final String mimeType = "text/velocity";
        final String path = "/any/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final User limitedUser = mockLimitedUser();

        final File file = FileUtil.createTemporalFile("any", "vm", "This is a test temporal file");

        final String htmlFileName = file.getName();
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getBinary(FileAssetAPI.BINARY_FIELD)).thenReturn(file);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, limitedUser, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/any/"+htmlFileName+"?language_id=2",link.getResourceLinkAsString());

    }


    @Test
    public void test_newContentlet_withAdminUser_expectEmptyLink() throws Exception{

        final String mimeType = "text/velocity";
        final String htmlFileName = "widget-code.vtl";
        final String path = "/application/comments/angular/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final User adminUser = mockAdminUser();

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertEquals(StringPool.BLANK,link.getResourceLinkAsString());

    }

    @Test
    public void test_new_contentlet_limited_user_expect_empty_link() throws Exception{

        final String mimeType = "text/velocity";
        final String htmlFileName = "widget-code.vtl";
        final String path = "/application/comments/angular/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final User limited = mockLimitedUser();

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getIdentifier()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.getInode()).thenReturn(UUIDGenerator.generateUuid());
        when(contentlet.isFileAsset()).thenReturn(true);
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getLanguageId()).thenReturn(languageId);
        when(contentlet.isNew()).thenReturn(true);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(HOST_ID);
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, limited, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertEquals(StringPool.BLANK,link.getResourceLinkAsString());

    }

}
