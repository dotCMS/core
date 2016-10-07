package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;

public class JsonTransformerTest {

    final ContentTypeFactory factory = new ContentTypeFactoryImpl();

    @BeforeClass
    public static void SetUpTests() throws FileNotFoundException, Exception {
        SuperContentTypeTest.SetUpTests();
    }

    @Test
    public void testContentTypeSerialization() throws Exception {
        List<ContentType> types = factory.findAll();
        for (ContentType type : types) {
            ContentType type2 = null;
            String json=null;
            try {
                json = new JsonContentTypeTransformer(type).asJson();

                type2 = new JsonContentTypeTransformer(json).from();



                assertThat("ContentType == ContentType2", type.equals(type2));
            } catch (Throwable t) {
                System.out.println(type);
                System.out.println(type2);
                System.out.println(json);
                throw t;
            }
        }
    }


    @Test
    public void testFieldSerialization() throws Exception {
        List<ContentType> types = factory.findAll();
        for (ContentType type : types) {
            Field field2 = null;
            String json=null;
            for (Field field : type.fields()) {

                try {
                    json = new JsonFieldTransformer(field).asJson();

                    field2 = new JsonFieldTransformer(json).from();


                    assertThat("Field1 == json Field2", field.equals(field2));
                } catch (Throwable t) {
                    System.out.println(json);
                    System.out.println(field);
                    System.out.println(field2);
                    System.out.println(t);
                    throw t;
                }
            }
        }
    }

}
