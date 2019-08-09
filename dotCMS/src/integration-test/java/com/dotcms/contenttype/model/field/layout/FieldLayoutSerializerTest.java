package com.dotcms.contenttype.model.field.layout;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.ContentTypeInternationalization;
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

import static org.mockito.Mockito.*;

public class FieldLayoutSerializerTest {

    @BeforeClass
    public static void prepare() throws Exception{
        IntegrationTestInitService.getInstance().init();
    }

    @Test()
    public void testSerialize() throws IOException, DotDataException, DotSecurityException {
        final ContentType contactUs = APILocator.getContentTypeAPI(APILocator.systemUser()).find("ContactUs");
        final FieldLayout fieldLayout = new FieldLayout(contactUs);

        final ObjectMapper mapper = mock(ObjectMapper.class);

        final ObjectWriter writer =  mock(ObjectWriter.class);
        when(writer.withAttribute("type", fieldLayout.getContentType())).thenReturn(writer);
        when(writer.withAttribute("internationalization", null)).thenReturn(writer);
        when(mapper.writer()).thenReturn(writer);

        final JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        final SerializerProvider serializerProvider = mock(SerializerProvider.class);

        final FieldLayoutSerializer fieldLayouSerializer = new FieldLayoutSerializer(mapper);
        fieldLayouSerializer.serialize(fieldLayout, jsonGenerator, serializerProvider);

        writer.writeValue(jsonGenerator, fieldLayout.getRows());
    }

    @Test()
    public void testSerializeWhenPassContentTypeInternationalization() throws IOException, DotDataException, DotSecurityException {
        final ContentType contactUs = APILocator.getContentTypeAPI(APILocator.systemUser()).find("ContactUs");

        final long languageId = 1;
        final boolean live = true;
        final User user = mock(User.class);
        final ContentTypeInternationalization contentTypeInternationalization =
                new ContentTypeInternationalization(languageId, live, user);

        final FieldLayout fieldLayout = new FieldLayout(contactUs);
        fieldLayout.setContentTypeInternationalization(contentTypeInternationalization);

        final ObjectMapper mapper = mock(ObjectMapper.class);

        final ObjectWriter writer =  mock(ObjectWriter.class);
        when(writer.withAttribute("type", fieldLayout.getContentType())).thenReturn(writer);
        when(writer.withAttribute("internationalization", contentTypeInternationalization)).thenReturn(writer);
        when(mapper.writer()).thenReturn(writer);

        final JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        final SerializerProvider serializerProvider = mock(SerializerProvider.class);

        final FieldLayoutSerializer fieldLayouSerializer = new FieldLayoutSerializer(mapper);
        fieldLayouSerializer.serialize(fieldLayout, jsonGenerator, serializerProvider);

        writer.writeValue(jsonGenerator, fieldLayout.getRows());
    }
}
