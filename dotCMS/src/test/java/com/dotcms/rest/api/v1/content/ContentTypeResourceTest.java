package com.dotcms.rest.api.v1.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotcms.util.ContentTypeUtil;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Test for the {@link ContentTypeResource}
 */
public class ContentTypeResourceTest extends UnitTestBase {

    @Test
    public void testNoContentLetTypes() throws Exception {

        final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final StructureAPI structureAPI  = mock(StructureAPI.class);
        final ContentTypeUtil contentTypeUtil  = mock(ContentTypeUtil.class);
        final InitDataObject initDataObject  = mock(InitDataObject.class);
        final WebResource webResource = mock(WebResource.class);

        final ContentTypeHelper contentTypeHelper  = new ContentTypeHelper(webResource, structureAPI, contentTypeUtil);

        final ServletContext context = mock(ServletContext.class);
        final User user = new User();

        user.setLocale(new Locale.Builder().setLanguage("en").setRegion("US").build());

        Config.CONTEXT = context;
        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        this.initMessages();

        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);

        when(contentTypeAPI.findAll()).thenReturn(new ArrayList<ContentType>());

        ContentTypeResource contentTypeResource = new ContentTypeResource(contentTypeHelper, webResource);

        final Response response1 = contentTypeResource.getRecentBaseTypes(request);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
        assertTrue( ((List)ResponseEntityView.class.cast(response1.getEntity()).getEntity()).isEmpty());
    }


    @Test
    public void testFileContentLetTypes() throws Exception {

        final HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final StructureAPI structureAPI  = mock(StructureAPI.class);
        final ContentTypeUtil contentTypeUtil  = mock(ContentTypeUtil.class);
        final InitDataObject initDataObject  = mock(InitDataObject.class);

        final WebResource webResource = mock(WebResource.class);

        final ContentTypeHelper contentTypeHelper  = new ContentTypeHelper(webResource, structureAPI, contentTypeUtil);

        final ServletContext context = mock(ServletContext.class);
        final User user = new User();

        user.setLocale(new Locale.Builder().setLanguage("en").setRegion("US").build());

        Config.CONTEXT = context;
        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        this.initMessages();

        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);

        final List<ContentType> contentTypes = getContentTypes();
        when(contentTypeAPI.findAll()).thenReturn(contentTypes);

        ContentTypeResource contentTypeResource = new ContentTypeResource(contentTypeHelper, webResource);

        final Response response1 = contentTypeResource.getRecentBaseTypes(request);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getEntity());

        List<BaseContentTypesView> entity = (List) ResponseEntityView.class.cast(response1.getEntity()).getEntity();
        assertTrue( !entity.isEmpty());

        for (BaseContentTypesView baseContentTypesView : entity) {
            switch (baseContentTypesView.getName()){
                case "CONTENT":
                    assertEquals(1, baseContentTypesView.getTypes().size());
                    ContentTypeView contentTypeView = baseContentTypesView.getTypes().get(0);
                    assertEquals("Document", contentTypeView.getName());
                    assertEquals("CONTENT", contentTypeView.getType());
                    assertEquals("d8262b9f-84ea-46f9-88c4-0c8959271d67", contentTypeView.getInode());
                    break;
                case "FILEASSET":
                    assertEquals(2, baseContentTypesView.getTypes().size());
                    ContentTypeView contentTypeView1 = baseContentTypesView.getTypes().get(0);
                    assertEquals("File Asset", contentTypeView1.getName());
                    assertEquals("FILEASSET", contentTypeView1.getType());
                    assertEquals("33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d", contentTypeView1.getInode());

                    ContentTypeView contentTypeView2 = baseContentTypesView.getTypes().get(1);
                    assertEquals("Video", contentTypeView2.getName());
                    assertEquals("FILEASSET", contentTypeView2.getType());
                    assertEquals("e65543eb-6b81-42e0-a59b-1bb9fd7bfce4", contentTypeView2.getInode());
                    break;
            }
        }
    }

    private List<ContentType> getContentTypes() {
        final List<ContentType> contentTypes = new ArrayList<>();

        contentTypes.add(ContentTypeBuilder.builder(SimpleContentType.class)
        	.id("d8262b9f-84ea-46f9-88c4-0c8959271d67")
        	.name("Document")
        	.variable("testtestingStructure")
        	.build()
        );
        contentTypes.add(ContentTypeBuilder.builder(FileAssetContentType.class)
            .id("33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d")
            .name("File Asset")
            .variable("testtestingStructure")
            .build()
        );
        contentTypes.add(ContentTypeBuilder.builder(FileAssetContentType.class)
        	.id("e65543eb-6b81-42e0-a59b-1bb9fd7bfce4")
        	.name("Video")
        	.variable("testtestingStructure")
        	.build()
        );

        return contentTypes;
    }
}
