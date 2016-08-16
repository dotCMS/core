package com.dotcms.rest.api.v1.content;

import com.dotcms.cms.login.LoginService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.AuthenticationHelper;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link ContentTypeResource}
 */
public class ContentTypeResourceTest extends BaseMessageResources {

    @Test
    public void testNoContentLetTypes() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);
        final LoginService loginService = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ContentTypeHelper contentletHelper  = mock(ContentTypeHelper.class);
        final StructureAPI structureAPI  = mock(StructureAPI.class);
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final AuthenticationHelper authenticationHelper = AuthenticationHelper.INSTANCE;
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);


        final WebResource webResource = mock(WebResource.class);
        final String userId = "admin@dotcms.com";
        final String pass = "pass";
        final ServletContext context = mock(ServletContext.class);
        final User user = new User();

        user.setLocale(new Locale.Builder().setLanguage("en").setRegion("US").build());

        Config.CONTEXT = context;
        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        this.initMessages();

        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);

        final ContentTypeResource contentletResource =
                new ContentTypeResource(webResource, contentletHelper, structureAPI, layoutAPI, languageAPI);

        final Response response1 = contentletResource.getTypes(request);
        System.out.println(response1);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue( !((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).isEmpty());
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Persona").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Form").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Content").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Widget").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Page").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").size() == 0);

    }


    @Test
    public void testFileContentLetTypes() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);
        final LoginService loginService = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ContentTypeHelper contentletHelper  = mock(ContentTypeHelper.class);
        final StructureAPI structureAPI  = mock(StructureAPI.class);
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final AuthenticationHelper authenticationHelper = AuthenticationHelper.INSTANCE;
        final List<Structure> structures = new ArrayList();
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);

        final WebResource webResource = mock(WebResource.class);
        final String userId = "admin@dotcms.com";
        final String pass = "pass";
        final ServletContext context = mock(ServletContext.class);
        final User user = new User();

        user.setLocale(new Locale.Builder().setLanguage("en").setRegion("US").build());

        Config.CONTEXT = context;
        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        this.initMessages();

        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);

        final Structure document = new Structure();
        document.setStructureType(4);
        document.setName("Document");
        document.setInode("d8262b9f-84ea-46f9-88c4-0c8959271d67");
        structures.add(document);

        final Structure asset = new Structure();
        asset.setStructureType(4);
        asset.setName("File Asset");
        asset.setInode("33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d");
        structures.add(asset);

        final Structure video = new Structure();
        video.setStructureType(4);
        video.setName("Video");
        video.setInode("e65543eb-6b81-42e0-a59b-1bb9fd7bfce4");
        structures.add(video);

        when(structureAPI.find(user, false, true)).thenReturn(structures);

        final ContentTypeResource contentletResource =
                new ContentTypeResource(webResource, contentletHelper, structureAPI, layoutAPI, languageAPI);

        final Response response1 = contentletResource.getTypes(request);
        System.out.println(response1);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue( !((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).isEmpty());
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Persona").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Form").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Content").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Widget").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("Page").size() == 0);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").size() == 3);
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").get(0).getType().equals("File"));
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").get(0).getName().equals("Document"));
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").get(1).getType().equals("File"));
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").get(1).getName().equals("File Asset"));
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").get(2).getType().equals("File"));
        assertTrue(((Map<String, List<ContentTypeView>>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).get("File").get(2).getName().equals("Video"));

    }
}
