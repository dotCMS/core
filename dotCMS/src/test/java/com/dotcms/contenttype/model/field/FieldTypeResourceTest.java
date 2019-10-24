package com.dotcms.contenttype.model.field;


import com.dotcms.UnitTestBase;
import javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONException;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import org.junit.Test;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.dotcms.util.CollectionsUtils.toImmutableList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.dotcms.util.CollectionsUtils.list;

/**
 * {@link FieldTypeResource} test
 */
public class FieldTypeResourceTest extends UnitTestBase {

    @Test
    public void testGetFieldTypes() throws JSONException, DotSecurityException, DotDataException {
        final WebResource webResource = mock(WebResource.class);
        FieldTypeAPI fieldTypeAPI = mock(FieldTypeAPI.class);

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        final FieldType fieldType1 = mock(FieldType.class);
        final FieldType fieldType2 = mock(FieldType.class);

        final User user = new User();

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);

        List<FieldType> fieldTypes = list(fieldType1, fieldType2);
        when(fieldTypeAPI.getFieldTypes(user)).thenReturn(fieldTypes);

        FieldTypeResource fieldTypeResource = new FieldTypeResource(webResource, fieldTypeAPI);


        Response response = fieldTypeResource.getFieldTypes(request);


        RestUtilTest.verifySuccessResponse(response);

        ImmutableList<Map<String, Object>> expect = fieldTypes.stream()
                .map(fieldType -> fieldType1.toMap())
                .collect(toImmutableList());
        
        assertEquals(expect, ((ResponseEntityView) response.getEntity()).getEntity());
    }
}
