package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileUpload.model.FileUpload;
import com.dotmarketing.util.FileUtilTest;
import com.liferay.util.FileUtil;
import com.sun.xml.bind.v2.util.XmlFactory;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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

    @Test
    public void aaa() throws IOException {
        final File file = new File(
                "/Users/freddyrodriguez/DOTCMS/tomcat9/webapps/ROOT/starter/com.dotmarketing.beans.ContainerStructure_0000000000.xml");

        final byte[] bytes = FileUtil.getBytes(file);

        final String xml = new String(bytes);

        System.out.println("xml = " + xml);


        /*final String xml = XStreamFactory.INSTANCE.getInstance().toXML(containerStructure);
        System.out.println("containerStructure = " + containerStructure);*/

        XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        System.out.println("xml = " + xml);
    }

    @Test
    public void aaa2() throws IOException {
        final String xml = "      <com.dotmarketing.beans.ContainerStructure>\n"
                + "    <id>11ee8430-ca4e-44f5-895a-28a9fdbca087</id>\n"
                + "    <structureId>2a3e91e4-fbbf-4876-8c5b-2233c1739b05</structureId>\n"
                + "    <containerInode>1dd3d033-b0e4-4ea1-b3ad-c79ab8ff838e</containerInode>\n"
                + "    <containerId>d71d56b4-0a8b-4bb2-be15-ffa5a23366ea</containerId>\n"
                + "    <code>$!{body}</code>\n"
                + "  </com.dotmarketing.beans.ContainerStructure>";

        final Object o = XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        System.out.println("xml = " + xml);
    }

    @Test
    public void aaa3() throws IOException {
        ContainerStructure containerStructure = new ContainerStructure();
        containerStructure.setContainerInode("containerInode");
        containerStructure.setStructureId("structureId");
        containerStructure.setCode("code");

        final String xml = XStreamFactory.INSTANCE.getInstance().toXML(containerStructure);

        System.out.println("s = " + xml);

    }

    @Test
    public void ccc() throws IOException {
        final File file = new File(Thread.currentThread()
                .getContextClassLoader().getResource("xmlStreamFactory/com.liferay.portal.model.Company.xml").getFile());

        final byte[] bytes = FileUtil.getBytes(file);

        final String xml = new String(bytes);

        System.out.println("xml = " + xml);


        /*final String xml = XStreamFactory.INSTANCE.getInstance().toXML(containerStructure);
        System.out.println("containerStructure = " + containerStructure);*/

        XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        System.out.println("xml = " + xml);
    }


    @Test
    public void ccc2() throws IOException {
        final String xml = " <com.liferay.portal.model.Company>\n"
                + "      <__new>false</__new>\n"
                + "      <__modified>false</__modified>\n"
                + "      <__companyId>dotcms.org</__companyId>\n"
                + "      <__key>rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlYy5TZWNyZXRLZXlTcGVjW0cLZuIwYU0CAAJMAAlhbGdvcml0aG10ABJMamF2YS9sYW5nL1N0cmluZztbAANrZXl0AAJbQnhwdAADQUVTdXIAAltCrPMX+AYIVOACAAB4cAAAACCY0IUeWDxw+s+VhAxXL0f0fQciai7SJo+JsiuA3IirwQ==</__key>\n"
                + "      <__portalURL>localhost</__portalURL>\n"
                + "      <__homeURL>/html/images/backgrounds/bg-11.jpg</__homeURL>\n"
                + "      <__mx></__mx>\n"
                + "      <__name>dotcms.org</__name>\n"
                + "      <__shortName>dotcms.org</__shortName>\n"
                + "      <__type>#576be8</__type>\n"
                + "      <__size>#1b3359</__size>\n"
                + "      <__street>#2f3e6c</__street>\n"
                + "      <__city>/dA/8c3df099-20ba-47ed-b7a3-2896be5bdc02/asset/company_logo.png</__city>\n"
                + "      <__zip>33133</__zip>\n"
                + "      <__phone>3058581422</__phone>\n"
                + "      <__fax></__fax>\n"
                + "      <__emailAddress>dotCMS Website &lt;website@dotcms.com&gt;</__emailAddress>\n"
                + "      <__authType>emailAddress</__authType>\n"
                + "      <__autoLogin>true</__autoLogin>\n"
                + "      <__strangers>false</__strangers>\n"
                + "      <__keyObj class=\"javax.crypto.spec.SecretKeySpec\">\n"
                + "        <key>mNCFHlg8cPrPlYQMVy9H9H0HImou0iaPibIrgNyIq8E=</key>\n"
                + "        <algorithm>AES</algorithm>\n"
                + "      </__keyObj>\n"
                + "    </com.liferay.portal.model.Company>";

        final Object o = XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        System.out.println("xml = " + xml);
    }
}
