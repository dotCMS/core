package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;

public class JsonContentTypeTransformerTest {

    final ContentTypeFactory factory = new ContentTypeFactoryImpl();
    @BeforeClass
    public static void SetUpTests() throws FileNotFoundException, Exception {
        SuperContentTypeTest.SetUpTests();
    }
    
    @Test
    public void testFindMethodEquals() throws Exception {
        List<ContentType> types = factory.findAll();
        for (ContentType type : types) {
            ContentType type2 = null;
            try {
                String json = new JsonContentTypeTransformer(type).asJson();
            
                type2 = new JsonContentTypeTransformer(json).from();
            
            
            
            

                assertThat("ContentType == ContentType2", type.equals(type2) );
            } catch (Throwable t) {
                System.out.println(type);
                System.out.println(type2);
                System.out.println(t);
                throw t;
            }
        }
    }
 
}