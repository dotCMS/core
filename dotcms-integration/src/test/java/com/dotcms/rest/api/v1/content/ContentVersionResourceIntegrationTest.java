package com.dotcms.rest.api.v1.content;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentVersionResourceIntegrationTest extends BaseWorkflowIntegrationTest {

    private static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    private static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    private static final String ADMIN_NAME = "User Admin";

    private static ContentVersionResource versionResource;
    private static Host site;
    private static Template template;
    private static Language spanishLanguage;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
        when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
        when(user.getFullName()).thenReturn(ADMIN_NAME);
        when(user.getLocale()).thenReturn(Locale.getDefault());

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource
                .init(any(HttpServletRequest.class), any(HttpServletResponse.class), anyBoolean())).thenReturn(dataObject);

        versionResource = new ContentVersionResource(APILocator.getContentletAPI(),
                APILocator.getLanguageAPI(), webResource);

        //Test data
        site = new SiteDataGen().nextPersisted();
        template = new TemplateDataGen().nextPersisted();
        spanishLanguage = TestDataUtils.getSpanishLanguage();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Find_All_Expect_OK() throws DotDataException, DotSecurityException {

        //Test data
        Folder aboutUsFolder = new FolderDataGen()
                .site(site)
                .showOnMenu(true)
                .nextPersisted();
        final HTMLPageAsset englishPageAsset = new HTMLPageDataGen(aboutUsFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .title("index")
                .nextPersisted();
        HTMLPageDataGen.publish(englishPageAsset);

        final String identifier = englishPageAsset.getIdentifier();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource.findVersions(request,  new EmptyHttpResponse(), null, identifier, "0",2);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
        final Map versionsMap = Map.class.cast(entityView.getEntity());
        assertNotNull(versionsMap);
        final List<Map<String,Object>> versions = (List<Map<String,Object>>)versionsMap.get("versions");

        final Set <Object> inodesByVersion = versions.stream().map(stringObjectMap -> stringObjectMap.get("inode")).collect(Collectors.toSet());

        final List<Contentlet> list = APILocator.getContentletAPI().findAllVersions(new Identifier(identifier), APILocator.systemUser(),false);
        final List<String> inodes = list.stream().map(Contentlet::getInode).collect(Collectors.toList());
        for(final String inode:inodes){
            assertTrue(inodesByVersion.contains(inode));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Find_All_Expect_404() throws DotDataException, DotSecurityException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource.findVersions(request,  new EmptyHttpResponse(), null,"nonsense", "", 2);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Find_All_By_Lang_Expect_OK() throws DotDataException, DotSecurityException {

        //Test data
        Folder aboutUsFolder = new FolderDataGen()
                .site(site)
                .showOnMenu(true)
                .nextPersisted();
        final HTMLPageAsset englishPageAsset = new HTMLPageDataGen(aboutUsFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .title("index")
                .nextPersisted();
        HTMLPageDataGen.publish(englishPageAsset);

        //Create a spanish version of the just created page
        Contentlet htmlPage = APILocator.getContentletAPI()
                .find(englishPageAsset.getInode(), APILocator.systemUser(), false);
        assertNotNull(htmlPage);

        Contentlet spanishPageAsset =
                APILocator.getContentletAPI()
                        .checkout(htmlPage.getInode(), APILocator.systemUser(), false);
        assertNotNull(spanishPageAsset);
        spanishPageAsset.setIdentifier(htmlPage.getIdentifier());
        spanishPageAsset.setInode(null);
        spanishPageAsset.setLanguageId(spanishLanguage.getId()); // spanish
        spanishPageAsset.setIndexPolicy(IndexPolicy.WAIT_FOR);

        spanishPageAsset =
                APILocator.getContentletAPI()
                        .checkin(spanishPageAsset, APILocator.systemUser(), false);
        assertNotNull(spanishPageAsset);
        assertNotNull(spanishPageAsset.getInode());
        assertEquals(englishPageAsset.getIdentifier(), spanishPageAsset.getIdentifier());

        final String identifier = englishPageAsset.getIdentifier();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource
                .findVersions(request,  new EmptyHttpResponse(), null, identifier, "1",2);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
        final Map versionsMap = Map.class.cast(entityView.getEntity());
        assertNotNull(versionsMap);
        final Map<String,Object> versions = (Map<String,Object>)versionsMap.get("versions");

        final List<Map<String, Object>> enVersions = (List<Map<String, Object>>) versions.get("en-us");
        final List<Map<String, Object>> esVersions = (List<Map<String, Object>>) versions.get("es-es");
        assertFalse(enVersions.isEmpty());
        assertFalse(esVersions.isEmpty());

        final Set<String> enInodes = enVersions.stream().map(stringObjectMap -> stringObjectMap.get("inode").toString()).collect(Collectors.toSet());
        final Set<String> esInodes = esVersions.stream().map(stringObjectMap -> stringObjectMap.get("inode").toString()).collect(Collectors.toSet());

        final LanguageAPI languageAPI = APILocator.getLanguageAPI();

        final Map<String, List<Contentlet>> contentByLangMap =
                APILocator.getContentletAPI()
                        .findAllVersions(new Identifier(identifier), APILocator.systemUser(), false)
                        .stream()
                        .collect(Collectors.groupingBy(
                                c -> languageAPI.getLanguage(c.getLanguageId()).toString())
                                );

        for(final Contentlet contentlet:contentByLangMap.get("en-us")){
            assertTrue(enInodes.contains(contentlet.getInode()));
        }

        for(final Contentlet contentlet:contentByLangMap.get("es-es")){
            assertTrue(esInodes.contains(contentlet.getInode()));
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Find_All_By_Lang_Expect_404()  throws DotDataException, DotSecurityException{
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource.findVersions(request,  new EmptyHttpResponse(), null,"nonsense", "1",2);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Bad_Request_404()  throws DotDataException, DotSecurityException{
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource.findVersions(request,  new EmptyHttpResponse(), null,null, "1",2);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void test_find_multiple_inodes() throws DotDataException, DotSecurityException {

        //Creating test data
        final Contentlet testContentlet1 = createTestContentlet();
        final Contentlet testContentlet2 = createTestContentlet();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        try {
            final Response response = versionResource.findVersions(request,
                    new EmptyHttpResponse(),
                    String.format("%s,%s", testContentlet1.getInode(), testContentlet2.getInode()),
                    null, null, 10);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
        } finally {
            ContentletDataGen.remove(testContentlet1);
            ContentletDataGen.remove(testContentlet2);
        }
    }

    /**
     * Creates test Contentlets
     */
    private Contentlet createTestContentlet() throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");

        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentGenericType.id());
        final Contentlet testContentlet = contentletDataGen
                .setProperty("title", "TestContent_" + System.currentTimeMillis())
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .languageId(1)
                .nextPersisted();

        assertNotNull(testContentlet);
        assertNotNull(testContentlet.getIdentifier());
        assertNotNull(testContentlet.getInode());

        return testContentlet;
    }

}