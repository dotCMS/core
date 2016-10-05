package com.dotcms.rest.api.v1.content;

import com.dotcms.cms.login.LoginService;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.util.ContentTypeUtil;
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
        final StructureAPI structureAPI  = mock(StructureAPI.class);
        final ContentTypeUtil contentTypeUtil  = mock(ContentTypeUtil.class);
        final InitDataObject initDataObject  = mock(InitDataObject.class);

        final WebResource webResource = mock(WebResource.class);

        final ServletContext context = mock(ServletContext.class);
        final User user = new User();

        user.setLocale(new Locale.Builder().setLanguage("en").setRegion("US").build());

        Config.CONTEXT = context;
        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        this.initMessages();

        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);

        ContentTypeHelper contentTypeHelper = new ContentTypeHelper(webResource, structureAPI, contentTypeUtil);
        ContentTypeResource contentTypeResource = new ContentTypeResource(contentTypeHelper);

        final Response response1 = contentTypeResource.getTypes(request);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue( ((List<ContentTypeView>)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).isEmpty());


    }


    @Test
    public void testFileContentLetTypes() throws Exception {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final StructureAPI structureAPI  = mock(StructureAPI.class);
        final ContentTypeUtil contentTypeUtil  = mock(ContentTypeUtil.class);
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final List<Structure> structures = new ArrayList();


        final WebResource webResource = mock(WebResource.class);

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

        ContentTypeHelper contentTypeHelper = new ContentTypeHelper(webResource, structureAPI, contentTypeUtil);
        ContentTypeResource contentTypeResource = new ContentTypeResource(contentTypeHelper);

        final Response response1 = contentTypeResource.getTypes(request);
        System.out.println(response1);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());

        List<BaseContentTypesView> entity = (List<BaseContentTypesView>) ResponseEntityView.class.cast(response1.getEntity()).getEntity();
        assertTrue(!entity.isEmpty());
        assertEquals(1, entity.size());
        assertEquals(BaseContentType.FILEASSET.name(), entity.get(0).getName());
        assertEquals(3, entity.get(0).getTypes().size());
        assertTrue(entity.get(0).getTypes().get(0).getType().equals("FILEASSET"));
        assertTrue(entity.get(0).getTypes().get(0).getName().equals("Document"));
        assertTrue(entity.get(0).getTypes().get(1).getType().equals("FILEASSET"));
        assertTrue(entity.get(0).getTypes().get(1).getName().equals("File Asset"));
        assertTrue(entity.get(0).getTypes().get(2).getType().equals("FILEASSET"));
        assertTrue(entity.get(0).getTypes().get(2).getName().equals("Video"));

    }
}
