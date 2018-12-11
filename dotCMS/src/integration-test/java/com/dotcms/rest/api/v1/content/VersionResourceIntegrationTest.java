package com.dotcms.rest.api.v1.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;

public class VersionResourceIntegrationTest extends BaseWorkflowIntegrationTest {

    private static final String ADMIN_DEFAULT_ID = "dotcms.org.1";
    private static final String ADMIN_DEFAULT_MAIL = "admin@dotcms.com";
    private static final String ADMIN_NAME = "User Admin";

    private static VersionResource versionResource;

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
                .init(anyBoolean(), any(HttpServletRequest.class), anyBoolean())).thenReturn(dataObject);

        versionResource = new VersionResource(APILocator.getContentletAPI(),
                APILocator.getLanguageAPI(), webResource);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Find_All_Expect_OK() throws DotDataException, DotSecurityException {
        final String identifier = "f4a02846-7ca4-4e08-bf07-a61366bbacbb";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource.findAllVersions(request, identifier, 2);

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
        final Response response = versionResource.findAllVersions(request, "nonsense", 2);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Find_All_By_Lang_Expect_OK() throws DotDataException, DotSecurityException {
        final String identifier = "a9f30020-54ef-494e-92ed-645e757171c2";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource
                .findAllVersionsGroupByLang(request, identifier, 2);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final ResponseEntityView entityView = ResponseEntityView.class.cast(response.getEntity());
        final Map versionsByLangMap = Map.class.cast(entityView.getEntity());
        assertNotNull(versionsByLangMap);
        final List<Map<String, Object>> enVersions = (List<Map<String, Object>>) versionsByLangMap.get("en-us");
        final List<Map<String, Object>> esVersions = (List<Map<String, Object>>) versionsByLangMap.get("es-es");
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

        for(Contentlet contentlet:contentByLangMap.get("en-us")){
            assertTrue(enInodes.contains(contentlet.getInode()));
        }

        for(Contentlet contentlet:contentByLangMap.get("es-es")){
            assertTrue(esInodes.contains(contentlet.getInode()));
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_Find_All_By_Lang_Expect_404() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource.findAllVersionsGroupByLang(request, "nonsense", 2);
        assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }


    @SuppressWarnings("unchecked")
    @Test
    public void test_diff() throws DotDataException, DotSecurityException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Response response = versionResource.diff(request, "4e5acb67-3743-40f5-a207-8b8e6b63fa7b", "a8fc0128-e25e-435b-95f1-364687e9665e");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }


}
