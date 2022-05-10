package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.nio.charset.StandardCharsets;
import org.junit.BeforeClass;
import org.junit.Test;

public class XStreamFactoryTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }


    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: Call several times {@link XStreamFactory#getInstance()} without parameter or with null
     * Should: Return always the same {@link XStream} object
     */
    @Test
    public void notEncoding(){
        final XStream instance1 = XStreamFactory.INSTANCE.getInstance(null);
        final XStream instance2 = XStreamFactory.INSTANCE.getInstance(null);
        final XStream instance3 = XStreamFactory.INSTANCE.getInstance();

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotNull(instance3);

        assertEquals(instance1, instance2);
        assertEquals(instance2, instance3);
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: Call several times {@link XStreamFactory#getInstance()} with UTF-8 encoding
     * Should: Return always the same {@link XStream} object
     */
    @Test
    public void withEncoding(){
        final XStream instance1 = XStreamFactory.INSTANCE.getInstance(StandardCharsets.UTF_8);
        final XStream instance2 = XStreamFactory.INSTANCE.getInstance(StandardCharsets.UTF_8);

        assertNotNull(instance1);
        assertNotNull(instance2);

        assertEquals(instance1, instance2);

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType).nextPersisted();

        final XStream xStream = new XStream(new DomDriver(StandardCharsets.UTF_8.toString()));
        final String xml_1 = xStream.toXML(contentlet);

        final String xml_2 = instance1.toXML(contentlet);

        assertEquals(xml_1, xml_2
        );
    }
}
