package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ContentletTest {

    @Test
    public void testGetTitle_WhenExistingTitleField_returnItsValue() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = Mockito.mock(ContentletAPI.class);
        final UserAPI userAPI = Mockito.mock(UserAPI.class);
        Mockito.when(userAPI.getSystemUser()).thenReturn(new User());
        final Contentlet contentlet = Mockito.spy(Contentlet.class);
        contentlet.setUserAPI(userAPI);
        contentlet.setContentletAPI(contentletAPI);
        final ContentType mockContentType = Mockito.mock(ContentType.class);
        final Field fieldWithTitle = createFieldWithVarname("title");
        final List<Field> mockFieldWithTitle = Collections.singletonList(fieldWithTitle);
        Mockito.doReturn(mockContentType).when(contentlet).getContentType();
        Mockito.when(mockContentType.fields()).thenReturn(mockFieldWithTitle);

        final String expectedValue = "ManuallyAddedFieldValue";
        contentlet.getMap().put("title", expectedValue );

        assertEquals(contentlet.getTitle(), expectedValue);
    }

    private Field createFieldWithVarname(final String varname) {
        return ImmutableTextField.builder()
                .name(varname)
                .variable(varname)
                .contentTypeId("123")
                .dataType(DataTypes.TEXT)
                .build();
    }
}
