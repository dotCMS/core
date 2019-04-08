package com.dotcms.rest.api.v3.contenttype;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FieldResourceTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void shouldFixContentTypesFieldsBeforeReturn () throws DotSecurityException, DotDataException {
        final String typeName="fieldResourceTest" + UUIDUtil.uuid();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name(typeName).variable(typeName).build();
        type = APILocator.getContentTypeAPI(APILocator.systemUser()).save(type);

        final Field field = FieldBuilder.builder(TextField.class).name("text").contentTypeId(type.id()).build();
        APILocator.getContentTypeFieldAPI().save(field,APILocator.systemUser());

        final FieldResource fieldResource = new FieldResource();
        final Response contentTypeFields = fieldResource.getContentTypeFields(type.id(), getHttpRequest());

        final List<Field> fields = (List<Field>) ((ResponseEntityView) contentTypeFields.getEntity()).getEntity();

        assertEquals(3, fields.size());
        assertEquals(RowField.class, fields.get(0).getClass());
        assertEquals(ColumnField.class, fields.get(1).getClass());
        assertEquals(field.id(), fields.get(2).id());
    }

    private static HttpServletRequest getHttpRequest() {
        MockHeaderRequest request = new MockHeaderRequest(
                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }
}
