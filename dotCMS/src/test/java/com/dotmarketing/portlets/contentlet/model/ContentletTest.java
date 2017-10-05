package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ContentletTest {

    @Test
    public void testGetTitle_WhenExistingTitleField_returnItsValue() throws DotDataException, DotSecurityException {
        ContentletAPI contentletAPI = Mockito.mock(ContentletAPI.class);
        UserAPI userAPI = Mockito.mock(UserAPI.class);
        Mockito.when(userAPI.getSystemUser()).thenReturn(new User());
        Contentlet contentlet = Mockito.spy(new Contentlet(contentletAPI, userAPI));
        ContentType mockContentType = Mockito.mock(ContentType.class);
        Field fieldWithTitle = createFieldWithVarname("title");
        List<Field> mockFieldWithTitle = Collections.singletonList(fieldWithTitle);
        Mockito.doReturn(mockContentType).when(contentlet).getContentType();
        Mockito.when(mockContentType.fields()).thenReturn(mockFieldWithTitle);

        final String expectedValue = "ManuallyAddedFieldValue";
        contentlet.getMap().put("title", expectedValue );

        assertEquals(contentlet.getTitle(), expectedValue);
    }

    @Test
    public void testGetTitle_WhenNonExistingTitleField_returnGetName() throws DotDataException, DotSecurityException {
        ContentletAPI contentletAPI = Mockito.mock(ContentletAPI.class);
        UserAPI userAPI = Mockito.mock(UserAPI.class);
        User mockUser = new User();
        Mockito.when(userAPI.getSystemUser()).thenReturn(mockUser);
        Contentlet contentlet = Mockito.spy(new Contentlet(contentletAPI, userAPI));
        ContentType mockContentType = Mockito.mock(ContentType.class);
        Field fieldWithTitle = createFieldWithVarname("thisIsNotATitle");
        List<Field> mockFieldWithTitle = Collections.singletonList(fieldWithTitle);
        Mockito.doReturn(mockContentType).when(contentlet).getContentType();
        Mockito.when(mockContentType.fields()).thenReturn(mockFieldWithTitle);

        String expectedValue = "titleTakenFromElsewhere";
        Mockito.when(contentletAPI.getName(contentlet, mockUser, false))
                .thenReturn(expectedValue);

        assertEquals(contentlet.getTitle(), expectedValue);
    }

    private Field createFieldWithVarname(String varname) {
        return ImmutableTextField.builder()
                .name(varname)
                .variable(varname)
                .contentTypeId("123")
                .dataType(DataTypes.TEXT)
                .build();
    }
}
