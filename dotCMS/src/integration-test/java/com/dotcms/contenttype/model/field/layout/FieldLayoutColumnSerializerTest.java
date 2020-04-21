package com.dotcms.contenttype.model.field.layout;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldLayoutColumnSerializerTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test()
    public void testSerialize() throws IOException {
        final ColumnField columDivider =  ImmutableColumnField.builder()
                .name("Column Field 1")
                .sortOrder(0)
                .build();

        final List<Field> fields = CollectionsUtils.list(
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(2)
                        .build()
        );

        final FieldLayoutColumn fieldLayoutColumn = new FieldLayoutColumn(columDivider, fields);

        final JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        final SerializerProvider serializerProvider = mock(SerializerProvider.class);

        final FieldLayoutColumnSerializer fieldLayoutColumnSerializer = new FieldLayoutColumnSerializer();
        fieldLayoutColumnSerializer.serialize(fieldLayoutColumn, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer =
                new JsonFieldTransformer(fieldLayoutColumn.getColumn());
        verify(jsonGenerator).writeObjectField("columnDivider", jsonFieldDividerTransformer.mapObject());

        final JsonFieldTransformer jsonColumnsTransformer = new JsonFieldTransformer(fieldLayoutColumn.getFields());
        verify(jsonGenerator).writeObjectField("fields", jsonColumnsTransformer.mapList());

        verify(jsonGenerator).writeEndObject();
        verify(jsonGenerator).flush();
    }

    @Test()
    public void testSerializeWhenPassContentTypeInternationalization() throws IOException, DotDataException, DotSecurityException {

        final ContentType formContentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.FORM)
                .name("form_test")
                .fields(
                        CollectionsUtils.list(
                                ImmutableTextField.builder()
                                        .name("Text")
                                        .sortOrder(2)
                                        .build()
                        )
                )
                .nextPersisted();

        final long languageId = 1;
        final boolean live = true;
        final User user = mock(User.class);
        final ContentTypeInternationalization contentTypeInternationalization =
                new ContentTypeInternationalization(languageId, live, user);

        final ColumnField columDivider =  ImmutableColumnField.builder()
                .name("Column Field 1")
                .sortOrder(0)
                .build();

        final List<Field> fields = CollectionsUtils.list(
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(2)
                        .build()
        );

        final FieldLayoutColumn fieldLayoutColumn = new FieldLayoutColumn(columDivider, fields);

        final JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        final SerializerProvider serializerProvider = mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("internationalization")).thenReturn(contentTypeInternationalization);
        when(serializerProvider.getAttribute("type")).thenReturn(formContentType);

        final JsonFieldTransformer jsonFieldTransformer = new JsonFieldTransformer(fields.get(0));
        final Map<String, Object> fieldMap = jsonFieldTransformer.mapObject();

        final FieldLayoutColumnSerializer fieldLayoutColumnSerializer = new FieldLayoutColumnSerializer();
        fieldLayoutColumnSerializer.serialize(fieldLayoutColumn, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer =
                new JsonFieldTransformer(fieldLayoutColumn.getColumn());
        verify(jsonGenerator).writeObjectField("columnDivider", jsonFieldDividerTransformer.mapObject());

        final JsonFieldTransformer jsonColumnsTransformer = new JsonFieldTransformer(fieldLayoutColumn.getFields());
        verify(jsonGenerator).writeObjectField("fields", jsonColumnsTransformer.mapList());

        verify(jsonGenerator).writeEndObject();
        verify(jsonGenerator).flush();

        APILocator.getContentTypeFieldAPI().getFieldInternationalization(formContentType, contentTypeInternationalization, fieldMap);
    }
}
