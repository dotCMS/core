package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

public class FieldAPIImplTest {
    private static PermissionAPI perAPI;
    private static ContentletAPI conAPI;
    private static UserAPI userAPI;
    private static RelationshipAPI relationshipAPI;
    private static LocalSystemEventsAPI localSystemEventsAPI;
    private static LanguageVariableAPI languageVariableAPI;
    private static FieldFactory fieldFactory;

    private static FieldAPIImpl fieldAPIImpl;

    @BeforeClass
    public static void init(){
        perAPI = mock(PermissionAPI.class);
        conAPI = mock(ContentletAPI.class);
        userAPI = mock(UserAPI.class);
        relationshipAPI = mock(RelationshipAPI.class);
        localSystemEventsAPI = mock(LocalSystemEventsAPI.class);
        languageVariableAPI = mock(LanguageVariableAPI.class);
        fieldFactory = mock(FieldFactory.class);

        fieldAPIImpl = new FieldAPIImpl(
                perAPI,
                conAPI,
                userAPI,
                relationshipAPI,
                localSystemEventsAPI,
                languageVariableAPI,
                fieldFactory
        );
    }

    /**
     * when: you have to language variable for the Content Type's fields
     * Should: Change the field property name by the language variable values
     */
    @Test
    public void shouldFieldInternationalizationWork(){
        final long languageId = 1;
        final boolean live = true;
        final User user = mock(User.class);
        final Object object = mock(Object.class);

        final ContentType contentType = mock(ContentType.class);
        final ContentTypeInternationalization contentTypeInternationalization = mock(ContentTypeInternationalization.class);
        final Map<String, Object> fieldMap = Map.of(
                "attribute_1", "value_1",
                "attribute_2", "value_2",
                "attribute_3", "value_3",
                "variable", "testField",
                "attribute_4", object
        );

        when(contentType.variable()).thenReturn("contentTypeVariable");
        when(contentTypeInternationalization.getLanguageId()).thenReturn(languageId);
        when(contentTypeInternationalization.isLive()).thenReturn(live);

        final String key1 = "contentTypeVariable.testField.attribute_1";
        final String key2 = "contentTypeVariable.testField.attribute_2";
        final String key3 = "contentTypeVariable.testField.attribute_3";
        final String key4 = "contentTypeVariable.testField.variable";
        final String key5 = "contentTypeVariable.testField.attribute_4";

        when(
                this.languageVariableAPI.getLanguageVariable(key1, languageId, user, live, false)
        ).thenReturn("Internationalization_1");

        when(
                this.languageVariableAPI.getLanguageVariable(key2, languageId, user, live, false)
        ).thenReturn("Internationalization_2");

        when(
                this.languageVariableAPI.getLanguageVariable(key3, languageId, user, live, false)
        ).thenReturn(key3);

        when(
                this.languageVariableAPI.getLanguageVariable(key4, languageId, user, live, false)
        ).thenReturn(key4);

        when(
                this.languageVariableAPI.getLanguageVariable(key5, languageId, user, live, false)
        ).thenReturn(key5);

        final Map<String, Object> fieldInternationalization =
                fieldAPIImpl.getFieldInternationalization(contentType, contentTypeInternationalization, fieldMap, user);

        assertEquals(5, fieldInternationalization.size());

        assertEquals("testField", fieldInternationalization.get("variable"));
        assertEquals("Internationalization_1", fieldInternationalization.get("attribute_1"));
        assertEquals("Internationalization_2", fieldInternationalization.get("attribute_2"));
        assertEquals("value_3", fieldInternationalization.get("attribute_3"));
        assertEquals(object, fieldInternationalization.get("attribute_4"));
    }

    /**
     * when: Elasticsearch is down
     * Should: Return all the property values without change
     */
    @Test
    public void shouldFieldInternationalizationNotWork(){
        final long languageId = 1;
        final boolean live = true;
        final User user = mock(User.class);

        final ContentType contentType = mock(ContentType.class);
        final ContentTypeInternationalization contentTypeInternationalization = mock(ContentTypeInternationalization.class);
        final Map<String, Object> fieldMap = Map.of(
                "attribute_1", "value_1",
                "attribute_2", "value_2",
                "attribute_3", "value_3",
                "variable", "testField"
        );

        when(contentType.variable()).thenReturn("contentTypeVariable");
        when(contentTypeInternationalization.getLanguageId()).thenReturn(languageId);
        when(contentTypeInternationalization.isLive()).thenReturn(live);

        final ConnectException connectException = new ConnectException();
        final DotRuntimeException dotRuntimeException = new DotRuntimeException(connectException);

        when(
                languageVariableAPI.getLanguageVariable(anyString(), anyLong(), any(), anyBoolean(), anyBoolean())
        ).thenThrow(dotRuntimeException);


        final Map<String, Object> fieldInternationalization =
                fieldAPIImpl.getFieldInternationalization(contentType, contentTypeInternationalization, fieldMap, user);

        assertEquals(4, fieldInternationalization.size());

        assertEquals("testField", fieldInternationalization.get("variable"));
        assertEquals("value_1", fieldInternationalization.get("attribute_1"));
        assertEquals("value_2", fieldInternationalization.get("attribute_2"));
        assertEquals("value_3", fieldInternationalization.get("attribute_3"));
    }

    @Test
    public void test_loadVariables() throws DotDataException {
        final Field field = FieldBuilder.builder(WysiwygField.class).name("TestField").build();
        final List<FieldVariable> fieldVariables = ImmutableList.of(
                ImmutableFieldVariable.builder()
                        .key("foo")
                        .value("bar")
                        .build(),
                ImmutableFieldVariable.builder()
                        .key("hello")
                        .value("world")
                        .build()
        );
        when(fieldFactory.loadVariables(field)).thenReturn(fieldVariables);
        assertEquals(2, fieldAPIImpl.loadVariables(field).size());
        assertEquals("foo", fieldAPIImpl.loadVariables(field).get(0).key());
        assertEquals("bar", fieldAPIImpl.loadVariables(field).get(0).value());
    }

    @Test
    public void test_loadVariables_nullField() throws DotDataException {
        final Field field = null;
        assertTrue(fieldAPIImpl.loadVariables(field).isEmpty());
    }
}
