package com.dotmarketing.portlets.contentlet.model;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.ResourceLink.ResourceLinkBuilder;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

public class ResourceLinkTest {

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
                Host host = mock(Host.class);
                when(host.getHostname()).thenReturn(hostName);
                return host;
            }

            @Override
            Identifier getIdentifier(final Contentlet contentlet) throws DotDataException {
                final Identifier identifier = mock(Identifier.class);
                when(identifier.getInode()).thenReturn("83864b2c-3988-4acc-953d-ff8d0ba5e093");
                when(identifier.getParentPath()).thenReturn(path);
                return identifier;
            }

            @Override
            FileAsset getFileAsset(final Contentlet contentlet){
                FileAsset fileAsset = mock(FileAsset.class);
                when(fileAsset.getMimeType()).thenReturn(mimeType);
                when(fileAsset.getFileName()).thenReturn(htmlFileName);
                return fileAsset;
            }

        };
        return resourceLinkBuilder;
    }

    @Test
    public void test_Html_ResourceLink_Expect_Downloadable_No_Port_Number() throws Exception{

        final String mimeType = "text/html";
        final String htmlFileName = "comments-list.html";
        final String path = "/application/comments/angular/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 1L;
        final boolean isSecure = false;

        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("dotcms.org.1");
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getLanguageId()).thenReturn(languageId);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn("48190c8c-42c4-46af-8d1a-0cd5db894797");
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertFalse(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/application/comments/angular/comments-list.html?language_id=1",link.getResourceLinkAsString());

    }

    @Test
    public void test_html_ResourceLink_Expect_Downloadable_Secure_Site_Port_Number() throws Exception{

        final String mimeType = "text/html";
        final String htmlFileName = "comments-list.html";
        final String path = "/application/comments/angular/";
        final String hostName = "localhost";
        final long languageId = 1L;
        final boolean isSecure = false;

        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("dotcms.org.1");
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getLanguageId()).thenReturn(languageId);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn("48190c8c-42c4-46af-8d1a-0cd5db894797");
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(8080);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertFalse(link.isDownloadRestricted());
        assertEquals("http://localhost:8080/application/comments/angular/comments-list.html?language_id=1",link.getResourceLinkAsString());

    }


    @Test
    public void test_vtl_ResourceLink_Expect_Restricted_Download_No_Port_Number() throws Exception{

        final String mimeType = "text/velocity";
        final String htmlFileName = "widget-code.vtl";
        final String path = "/application/comments/angular/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("dotcms.org.1");
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getLanguageId()).thenReturn(languageId);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn("48190c8c-42c4-46af-8d1a-0cd5db894797");
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/application/comments/angular/widget-code.vtl?language_id=2",link.getResourceLinkAsString());

    }


    @Test
    public void test_vm_ResourceLink_Expect_Restricted_Download_No_Port_Number() throws Exception{

        final String mimeType = "text/velocity";
        final String htmlFileName = "any.vm";
        final String path = "/any/";
        final String hostName = "demo.dotcms.com";
        final long languageId = 2L;
        final boolean isSecure = false;

        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("dotcms.org.1");
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");

        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getContentType()).thenReturn(mockFileAssetContentType());
        when(contentlet.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)).thenReturn(htmlFileName);
        when(contentlet.getLanguageId()).thenReturn(languageId);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn("48190c8c-42c4-46af-8d1a-0cd5db894797");
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = getResourceLinkBuilder(hostName, path, mimeType, htmlFileName);
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertEquals("http://demo.dotcms.com/any/any.vm?language_id=2",link.getResourceLinkAsString());

    }
}
