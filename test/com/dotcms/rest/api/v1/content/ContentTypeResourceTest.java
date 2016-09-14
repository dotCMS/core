package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.BaseMessageResources;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
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
        RestUtilTest.initMockContext();
        HttpServletRequest mockHttpRequest = RestUtilTest.getMockHttpRequest();

        final WebResource webResource = mock(WebResource.class);
        final StructureAPI structureAPI = mock(StructureAPI.class);
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        final LanguageAPI languageAPI = mock(LanguageAPI.class);
        final InitDataObject initData = mock(InitDataObject.class);
        final User user = mock(User.class);
        final List<Structure> structures = getStructures();

        when(webResource.init(null, true, mockHttpRequest, true, null)).thenReturn(initData);
        when(initData.getUser()).thenReturn(user);
        when(structureAPI.find(user, false, true)).thenReturn(structures);

        ContentTypeHelper contentTypeHelper = new ContentTypeHelper(webResource, structureAPI, layoutAPI, languageAPI);
        ContentTypeResource contentTypeResource = new ContentTypeResource(contentTypeHelper);

        Response response = contentTypeResource.getTypes(mockHttpRequest);

        RestUtilTest.verifySuccessResponse(response);

        List<BaseContentTypesView> entity = (List<BaseContentTypesView>) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(4, entity.size());

        compare(structures.get(0), entity.get(0), 0);
        compare(structures.get(1), entity.get(1), 0);
        compare(structures.get(2), entity.get(2), 0);
        compare(structures.get(3), entity.get(3), 0);
        compare(structures.get(4), entity.get(3), 1);
    }

    private void compare(Structure structure, BaseContentTypesView baseContentTypesView, int index){
        assertEquals(Structure.Type.getType(structure.getStructureType()).name(), baseContentTypesView.getName());
        assertEquals(structure.getInode(), baseContentTypesView.getTypes().get(index).getInode());
        assertEquals(structure.getName(), baseContentTypesView.getTypes().get(index).getName());
        assertNotNull(baseContentTypesView.getLabel());
        assertEquals(Structure.Type.getType(structure.getStructureType()).name(),
                baseContentTypesView.getTypes().get(index).getType());
    }

    private List<Structure> getStructures() {
        Structure structure1 = mock(Structure.class);
        when(structure1.getStructureType()).thenReturn(Structure.Type.CONTENT.getType());
        when(structure1.getName()).thenReturn("Structure 1");
        when(structure1.getInode()).thenReturn("1111");

        Structure structure2 = mock(Structure.class);
        when(structure2.getStructureType()).thenReturn(Structure.Type.FILEASSET.getType());
        when(structure2.getName()).thenReturn("Structure 2");
        when(structure2.getInode()).thenReturn("2222");

        Structure structure3 = mock(Structure.class);
        when(structure3.getStructureType()).thenReturn(Structure.Type.HTMLPAGE.getType());
        when(structure3.getName()).thenReturn("Structure 3");
        when(structure3.getInode()).thenReturn("3333");

        Structure structure4 = mock(Structure.class);
        when(structure4.getStructureType()).thenReturn(Structure.Type.WIDGET.getType());
        when(structure4.getName()).thenReturn("Structure 4");
        when(structure4.getInode()).thenReturn("4444");

        Structure structure5 = mock(Structure.class);
        when(structure5.getStructureType()).thenReturn(Structure.Type.WIDGET.getType());
        when(structure5.getName()).thenReturn("Structure 5");
        when(structure5.getInode()).thenReturn("5555");

        return list(structure1,structure2,structure3,structure4,structure5);
    }
}
