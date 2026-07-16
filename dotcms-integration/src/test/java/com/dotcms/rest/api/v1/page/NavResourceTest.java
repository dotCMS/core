package com.dotcms.rest.api.v1.page;

import static org.mockito.Mockito.mock;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Base64;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class NavResourceTest{

    private static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //Test data
        final Template template = new TemplateDataGen().nextPersisted();

        site = new SiteDataGen().nextPersisted();
        Folder aboutUsFolder = new FolderDataGen()
                .name("about-us")
                .title("about-us")
                .site(site)
                .showOnMenu(true)
                .nextPersisted();
        HTMLPageDataGen.publish(new HTMLPageDataGen(aboutUsFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .title("index")
                .nextPersisted());

        Folder locationsFolder = new FolderDataGen()
                .name("locations")
                .title("locations")
                .parent(aboutUsFolder)
                .showOnMenu(true)
                .nextPersisted();
        HTMLPageDataGen.publish(new HTMLPageDataGen(locationsFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .title("index")
                .nextPersisted());

        Folder ourTeamFolder = new FolderDataGen()
                .name("our-team")
                .title("our-team")
                .parent(locationsFolder)
                .showOnMenu(true)
                .nextPersisted();
        HTMLPageDataGen.publish(new HTMLPageDataGen(ourTeamFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .title("index")
                .nextPersisted());
    }

    /**
     * BasicAuth
     */
    private HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));

        Mockito.when(request.getParameter("host_id")).thenReturn(site.getIdentifier());

        return request;
    }

    @Test
    public void getAboutUsNav_WhenDepthIsNotAValidNumber_BadRequest() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","asdad","1");
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),response.getStatus());
    }

    @Test
    public void getAboutUsNav_WhenLanguageIsNotAValidNumber_BadRequest() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","2","asdad");
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),response.getStatus());
    }

    @Test
    public void getAboutUsNav_WhenLanguageDoesNotExists_NotFound() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","2","99");
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),response.getStatus());
    }

    @Test
    public void getAboutUsNav_WhenDepthAndLanguageAreValid_Success() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","2","1");
        Assert.assertEquals(Status.OK.getStatusCode(),response.getStatus());

        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(response.getEntity());
        Assert.assertTrue(responseEntityView.toString().contains("about-us"));

    }

}
