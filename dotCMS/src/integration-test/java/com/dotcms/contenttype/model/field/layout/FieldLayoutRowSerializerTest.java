package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class FieldLayoutRowSerializerTest {


    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test()
    public void testSerialize() throws IOException {

        final RowField rowField = ImmutableRowField.builder()
                .name("Row Field 1")
                .sortOrder(0)
                .build();

        final ColumnField columnField = ImmutableColumnField.builder()
                .name("Column Field 1")
                .sortOrder(0)
                .build();

        final List<Field> fields = CollectionsUtils.list(
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(2)
                        .build()
        );

        final List<FieldLayoutColumn> columns = new ArrayList<>();
        columns.add(new FieldLayoutColumn(columnField, fields));

        final FieldLayoutRow fieldLayoutrow = new FieldLayoutRow(rowField, columns);

        final JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        final SerializerProvider serializerProvider = mock(SerializerProvider.class);

        final ObjectMapper mapper = mock(ObjectMapper.class);

        final ObjectWriter writer =  mock(ObjectWriter.class);
        when(writer.withAttribute("type", null)).thenReturn(writer);
        when(writer.withAttribute("internationalization", null)).thenReturn(writer);
        when(mapper.writer()).thenReturn(writer);

        final FieldLayoutRowSerializer fieldLayoutRowSerializer = new FieldLayoutRowSerializer(mapper);
        fieldLayoutRowSerializer.serialize(fieldLayoutrow, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer = new JsonFieldTransformer(rowField);
        verify(jsonGenerator).writeObjectField("divider", jsonFieldDividerTransformer.mapObject());

        verify(jsonGenerator).writeEndObject();
        verify(jsonGenerator).flush();

        writer.writeValue(jsonGenerator, columns);
    }

    @Test()
    public void testSerializeWhenPassContentTypeInternationalization() throws IOException, DotDataException, DotSecurityException {

        final ContentType contactUs = APILocator.getContentTypeAPI(APILocator.systemUser()).find("ContactUs");

        final long languageId = 1;
        final boolean live = true;
        final User user = mock(User.class);
        final ContentTypeInternationalization contentTypeInternationalization =
                new ContentTypeInternationalization(languageId, live, user);

        final RowField rowField = ImmutableRowField.builder()
                .name("Row Field 1")
                .sortOrder(0)
                .build();

        final ColumnField columnField = ImmutableColumnField.builder()
                .name("Column Field 1")
                .sortOrder(0)
                .build();

        final List<Field> fields = CollectionsUtils.list(
                ImmutableTextField.builder()
                        .name("Text Field")
                        .sortOrder(2)
                        .build()
        );

        final List<FieldLayoutColumn> columns = new ArrayList<>();
        columns.add(new FieldLayoutColumn(columnField, fields));

        final FieldLayoutRow fieldLayoutrow = new FieldLayoutRow(rowField, columns);

        final JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        final SerializerProvider serializerProvider = mock(SerializerProvider.class);
        when(serializerProvider.getAttribute("internationalization")).thenReturn(contentTypeInternationalization);
        when(serializerProvider.getAttribute("type")).thenReturn(contactUs);

        final ObjectMapper mapper = mock(ObjectMapper.class);

        final ObjectWriter writer =  mock(ObjectWriter.class);
        when(writer.withAttribute("type", contactUs)).thenReturn(writer);
        when(writer.withAttribute("internationalization", contentTypeInternationalization)).thenReturn(writer);
        when(mapper.writer()).thenReturn(writer);

        final FieldLayoutRowSerializer fieldLayoutRowSerializer = new FieldLayoutRowSerializer(mapper);
        fieldLayoutRowSerializer.serialize(fieldLayoutrow, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeStartObject();

        final JsonFieldTransformer jsonFieldDividerTransformer = new JsonFieldTransformer(rowField);
        verify(jsonGenerator).writeObjectField("divider", jsonFieldDividerTransformer.mapObject());

        verify(jsonGenerator).writeEndObject();
        verify(jsonGenerator).flush();

        writer.writeValue(jsonGenerator, columns);
    }
}
