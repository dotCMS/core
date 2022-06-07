package com.dotcms.util;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CompanyDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.publisher.pusher.wrapper.HostWrapper;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileUpload.model.FileUpload;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.FileUtilTest;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.sun.xml.bind.v2.util.XmlFactory;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
     * Should: Return allways the same {@link XStream} object
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
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a  {@link ContainerStructure}
     * Should: get a  {@link ContainerStructure} object
     *
     * @throws IOException
     */
    @Test
    public void deserializeContainerStructure() throws IOException {
        final String xml = "<com.dotmarketing.beans.ContainerStructure>\n"
                + "    <id>11ee8430-ca4e-44f5-895a-28a9fdbca087</id>\n"
                + "    <structureId>2a3e91e4-fbbf-4876-8c5b-2233c1739b05</structureId>\n"
                + "    <containerInode>1dd3d033-b0e4-4ea1-b3ad-c79ab8ff838e</containerInode>\n"
                + "    <containerId>d71d56b4-0a8b-4bb2-be15-ffa5a23366ea</containerId>\n"
                + "    <code>$!{body}</code>\n"
                + "  </com.dotmarketing.beans.ContainerStructure>";

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);

        assertTrue(ContainerStructure.class.isInstance(obj));

        final ContainerStructure containerStructure = ContainerStructure.class.cast(obj);
        assertEquals("11ee8430-ca4e-44f5-895a-28a9fdbca087", containerStructure.getId());
        assertEquals("2a3e91e4-fbbf-4876-8c5b-2233c1739b05", containerStructure.getStructureId());
        assertEquals("1dd3d033-b0e4-4ea1-b3ad-c79ab8ff838e", containerStructure.getContainerInode());
        assertEquals("d71d56b4-0a8b-4bb2-be15-ffa5a23366ea", containerStructure.getContainerId());
        assertEquals("$!{body}", containerStructure.getCode());

    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a List of {@link ContainerStructure}
     * Should: get a List of {@link ContainerStructure} after called the {@link XStream#fromXML(File)} method
     *
     * @throws IOException
     */
    @Test
    public void deserializeContainerStructureList() throws IOException {
        final File file = new File(Thread.currentThread()
                .getContextClassLoader()
                .getResource("xmlStreamFactory/com.dotmarketing.beans.ContainerStructure.xml")
                .getFile());

        final byte[] bytes = FileUtil.getBytes(file);
        final String xml = new String(bytes);

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        assertTrue(List.class.isInstance(obj));

        final List<ContainerStructure> objList = (List) obj;
        assertEquals(4, objList.size());

        final ContainerStructure containerStructure_1 = objList.get(0);
        assertEquals("11ee8430-ca4e-44f5-895a-28a9fdbca087", containerStructure_1.getId());
        assertEquals("2a3e91e4-fbbf-4876-8c5b-2233c1739b05", containerStructure_1.getStructureId());
        assertEquals("1dd3d033-b0e4-4ea1-b3ad-c79ab8ff838e", containerStructure_1.getContainerInode());
        assertEquals("d71d56b4-0a8b-4bb2-be15-ffa5a23366ea", containerStructure_1.getContainerId());
        assertEquals("$!{body}", containerStructure_1.getCode());

        final ContainerStructure containerStructure_2 = objList.get(1);
        assertEquals("48a73b57-cd65-4ef7-9234-2fe6a40286f9", containerStructure_2.getId());
        assertEquals("f6259cc9-5d78-453e-8167-efd7b72b2e96", containerStructure_2.getStructureId());
        assertEquals("e58e92b3-7135-461b-b56b-04ff143a389b", containerStructure_2.getContainerInode());
        assertEquals("56bd55ea-b04b-480d-9e37-5d6f9217dcc3", containerStructure_2.getContainerId());
        assertEquals("Any code", containerStructure_2.getCode());

        final ContainerStructure containerStructure_3 = objList.get(2);
        assertEquals("a3df5a77-acb7-466f-b18d-739469cd1666", containerStructure_3.getId());
        assertEquals("2a3e91e4-fbbf-4876-8c5b-2233c1739b05", containerStructure_3.getStructureId());
        assertEquals("e58e92b3-7135-461b-b56b-04ff143a389b", containerStructure_3.getContainerInode());
        assertEquals("56bd55ea-b04b-480d-9e37-5d6f9217dcc3", containerStructure_3.getContainerId());
        assertEquals("#dotedit($CONTENT_INODE, $body)", containerStructure_3.getCode());

        final ContainerStructure containerStructure_4 = objList.get(3);
        assertEquals("d1ae9b2c-c57f-44b6-bd38-b6e30875be1b", containerStructure_4.getId());
        assertEquals("3d4a8854-7696-40c2-b0f3-28c8ff1121f9", containerStructure_4.getStructureId());
        assertEquals("4b737156-d8c0-4cd8-ab1b-674cf58d22d9", containerStructure_4.getContainerInode());
        assertEquals("eba434c6-e67a-4a64-9c88-1faffcafb40d", containerStructure_4.getContainerId());
        assertEquals("<h1>Title</h1>", containerStructure_4.getCode());
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to serialize a List of {@link ContainerStructure} object
     * Should: get a xml code
     *
     * @throws IOException
     */
    @Test
    public void serializeContainerStructureList() {
        ContainerStructure containerStructure = new ContainerStructure();
        containerStructure.setContainerInode("containerInode");
        containerStructure.setStructureId("structureId");
        containerStructure.setCode("code");
        containerStructure.setContainerId("containerId");

        List<ContainerStructure> list = new ArrayList<>();
        list.add(containerStructure);

        final String xml = XStreamFactory.INSTANCE.getInstance().toXML(list);

        assertEquals("<list>\n"
                + "  <com.dotmarketing.beans.ContainerStructure>\n"
                + "    <code>code</code>\n"
                + "    <containerId>containerId</containerId>\n"
                + "    <containerInode>containerInode</containerInode>\n"
                + "    <id class=\"null\"/>\n"
                + "    <structureId>structureId</structureId>\n"
                + "  </com.dotmarketing.beans.ContainerStructure>\n"
                + "</list>", xml);
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a  {@link Company}
     * Should: get a  {@link Company} object
     *
     * @throws IOException
     */
    @Test
    public void deserializeCompany() {
        final String xml = " <com.liferay.portal.model.Company>\n"
                + "      <__new>false</__new>\n"
                + "      <__modified>false</__modified>\n"
                + "      <__companyId>dotcms.org</__companyId>\n"
                + "      <__key>rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlY</__key>\n"
                + "      <__portalURL>localhost</__portalURL>\n"
                + "      <__homeURL>/html/images/backgrounds/bg-11.jpg</__homeURL>\n"
                + "      <__mx></__mx>\n"
                + "      <__name>dotcms.org</__name>\n"
                + "      <__shortName>short.dotcms.org</__shortName>\n"
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

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);

        assertTrue(Company.class.isInstance(obj));

        final Company company = Company.class.cast(obj);
        assertEquals(false, company.isNew());

        assertEquals(false, company.isNew());
        assertEquals(false, company.isModified());
        assertEquals("dotcms.org", company.getCompanyId());
        assertEquals("rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlY", company.getKey());
        assertEquals("localhost", company.getPortalURL());
        assertEquals("/html/images/backgrounds/bg-11.jpg", company.getHomeURL());
        assertEquals("", company.getMx());
        assertEquals("dotcms.org", company.getName());
        assertEquals("short.dotcms.org", company.getShortName());
        assertEquals("#576be8", company.getType());
        assertEquals("#1b3359", company.getSize());
        assertEquals("#2f3e6c", company.getStreet());
        assertEquals("/dA/8c3df099-20ba-47ed-b7a3-2896be5bdc02/asset/company_logo.png", company.getCity());
        assertEquals("33133", company.getZip());
        assertEquals("3058581422", company.getPhone());
        assertEquals("", company.getFax());

        assertEquals("dotCMS Website <website@dotcms.com>", company.getEmailAddress());
        assertEquals("emailAddress", company.getAuthType());
        assertEquals(true, company.getAutoLogin());
        assertEquals(false, company.getStrangers());
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a  {@link Company} array
     * Should: get a  {@link Company} array object
     *
     * @throws IOException
     */
    @Test
    public void deserializeCompanyArray() throws IOException {
        final File file = new File(Thread.currentThread()
                .getContextClassLoader().getResource("xmlStreamFactory/com.liferay.portal.model.Company.xml").getFile());

        final byte[] bytes = FileUtil.getBytes(file);
        final String xml = new String(bytes);

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        assertTrue(List.class.isInstance(obj));

        final List<Company> objList = (List) obj;
        assertEquals(1, objList.size());
        assertTrue(Company.class.isInstance(objList.get(0)));

        final Company company = objList.get(0);

        assertEquals(false, company.isNew());
        assertEquals(false, company.isNew());
        assertEquals(false, company.isModified());
        assertEquals("dotcms.org", company.getCompanyId());
        assertEquals("rO0ABXNyAB9qYXZheC5jcnlwdG8uc3BlYy5TZ", company.getKey());
        assertEquals("localhost", company.getPortalURL());
        assertEquals("/html/images/backgrounds/bg-11.jpg", company.getHomeURL());
        assertEquals("", company.getMx());
        assertEquals("dotcms.org", company.getName());
        assertEquals("short.dotcms.org", company.getShortName());
        assertEquals("#576be8", company.getType());
        assertEquals("#1b3359", company.getSize());
        assertEquals("#2f3e6c", company.getStreet());
        assertEquals("/dA/8c3df099-20ba-47ed-b7a3-2896be5bdc02/asset/company_logo.png", company.getCity());
        assertEquals("33133", company.getZip());
        assertEquals("3058581422", company.getPhone());
        assertEquals("", company.getFax());
        assertEquals("dotCMS Website <website@dotcms.com>", company.getEmailAddress());
        assertEquals("emailAddress", company.getAuthType());
        assertEquals(true, company.getAutoLogin());
        assertEquals(false, company.getStrangers());
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to serialize a {@link Company} object
     * Should: get a xml code
     *
     * @throws IOException
     */
    @Test
    public void serializeCompany() throws IOException {
        final Company company = new Company();
        company.setCompanyId("companyId");
        company.setName("Name");
        company.setAuthType("authType");
        company.setCity("city");
        company.setFax("fax");
        company.setHomeURL("homeURL");

        final String xml = XStreamFactory.INSTANCE.getInstance().toXML(company);

        assertEquals("<com.liferay.portal.model.Company>\n"
                + "  <__new>false</__new>\n"
                + "  <__modified>true</__modified>\n"
                + "  <__companyId>companyId</__companyId>\n"
                + "  <__homeURL>homeURL</__homeURL>\n"
                + "  <__name>Name</__name>\n"
                + "  <__city>city</__city>\n"
                + "  <__fax>fax</__fax>\n"
                + "  <__authType>authType</__authType>\n"
                + "  <__autoLogin>false</__autoLogin>\n"
                + "  <__strangers>false</__strangers>\n"
                + "</com.liferay.portal.model.Company>", xml);
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a  {@link User}
     * Should: get a  {@link User} object
     *
     * @throws IOException
     */
    @Test
    public void deserializeUser() throws IOException, ParseException {
        final String xml = " <com.liferay.portal.model.User>\n"
                + "    <__new>false</__new>\n"
                + "    <__modified>true</__modified>\n"
                + "    <__userId>dotcms.org.1</__userId>\n"
                + "    <__companyId>dotcms.org</__companyId>\n"
                + "    <__password>1:1:EBk/HSdzfiWh52GO9xxbBJhZgsb2jd9Q:i=4e20:LnjrBImIZ2XRA6woT8lSZmGNrDP8LKgE</__password>\n"
                + "    <__passwordEncrypted>true</__passwordEncrypted>\n"
                + "    <__passwordReset>false</__passwordReset>\n"
                + "    <__firstName>Admin</__firstName>\n"
                + "    <__middleName></__middleName>\n"
                + "    <__lastName>User</__lastName>\n"
                + "    <__nickName></__nickName>\n"
                + "    <__male>true</__male>\n"
                + "    <__emailAddress>admin@dotcms.com</__emailAddress>\n"
                + "    <__icqId>esdmRXaEhOnpNC6QlagRqyluiZTKnh1594419144088:1594419144088</__icqId>\n"
                + "    <__favoriteActivity>48190c8c-42c4-46af-8d1a-0cd5db894797</__favoriteActivity>\n"
                + "    <__languageId>en_US</__languageId>\n"
                + "    <__timeZoneId>US/Eastern</__timeZoneId>\n"
                + "    <__skinId>718167c8-c7ff-42c0-9f50-f27e2285e6e3</__skinId>\n"
                + "    <__dottedSkins>false</__dottedSkins>\n"
                + "    <__roundedSkins>false</__roundedSkins>\n"
                + "    <__greeting>Welcome, Test Test!</__greeting>\n"
                + "    <__resolution>1024x768</__resolution>\n"
                + "    <__refreshRate>900</__refreshRate>\n"
                + "    <__createDate class=\"sql-timestamp\">2008-03-06 17:36:01.0</__createDate>\n"
                + "    <__loginDate class=\"sql-timestamp\">2010-03-18 01:10:52.186</__loginDate>\n"
                + "    <__loginIP>0:0:0:0:0:0:0:1</__loginIP>\n"
                + "    <__lastLoginDate class=\"sql-timestamp\">2022-05-04 16:45:01.445</__lastLoginDate>\n"
                + "    <__lastLoginIP>186.4.29.6</__lastLoginIP>\n"
                + "    <__failedLoginAttempts>0</__failedLoginAttempts>\n"
                + "    <__agreedToTermsOfUse>false</__agreedToTermsOfUse>\n"
                + "    <__active>true</__active>\n"
                + "    <__deleteInProgress>false</__deleteInProgress>\n"
                + "    <__additionalInfo/>\n"
                + "    <__defaultUser>false</__defaultUser>\n"
                + "    <__locale>en_US</__locale>\n"
                + "    <__timeZone class=\"sun.util.calendar.ZoneInfo\" serialization=\"custom\">\n"
                + "      <java.util.TimeZone>\n"
                + "        <default>\n"
                + "          <ID>US/Eastern</ID>\n"
                + "        </default>\n"
                + "      </java.util.TimeZone>\n"
                + "      <sun.util.calendar.ZoneInfo>\n"
                + "        <default>\n"
                + "          <checksum>-1719355438</checksum>\n"
                + "          <dstSavings>3600000</dstSavings>\n"
                + "          <rawOffset>-18000000</rawOffset>\n"
                + "          <rawOffsetDiff>0</rawOffsetDiff>\n"
                + "          <willGMTOffsetChange>false</willGMTOffsetChange>\n"
                + "          <offsets>\n"
                + "            <int>-18000000</int>\n"
                + "            <int>-14400000</int>\n"
                + "            <int>3600000</int>\n"
                + "          </offsets>\n"
                + "          <simpleTimeZoneParams>\n"
                + "            <int>2</int>\n"
                + "            <int>8</int>\n"
                + "            <int>-1</int>\n"
                + "            <int>7200000</int>\n"
                + "            <int>0</int>\n"
                + "            <int>10</int>\n"
                + "            <int>1</int>\n"
                + "            <int>-1</int>\n"
                + "            <int>7200000</int>\n"
                + "            <int>0</int>\n"
                + "          </simpleTimeZoneParams>\n"
                + "          <transitions>\n"
                + "            <long>-9048018124800000</long>\n"
                + "            <long>-6689916518399967</long>\n"
                + "          </transitions>\n"
                + "        </default>\n"
                + "      </sun.util.calendar.ZoneInfo>\n"
                + "    </__timeZone>\n"
                + "    <modificationDate class=\"sql-timestamp\">2022-05-04 16:45:01.445</modificationDate>\n"
                + "  </com.liferay.portal.model.User>";

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);

        assertTrue(User.class.isInstance(obj));

        final User user = User.class.cast(obj);
        assertEquals(false, user.isNew());

        assertEquals(true, user.isModified());
        assertEquals("dotcms.org.1", user.getUserId());
        assertEquals("dotcms.org", user.getCompanyId());
        assertEquals("1:1:EBk/HSdzfiWh52GO9xxbBJhZgsb2jd9Q:i=4e20:LnjrBImIZ2XRA6woT8lSZmGNrDP8LKgE", user.getPassword());
        assertEquals(true, user.getPasswordEncrypted());
        assertEquals("Admin", user.getFirstName());
        assertEquals("", user.getMiddleName());
        assertEquals("User", user.getLastName());
        assertEquals("", user.getNickName());
        assertEquals(true, user.isMale());
        assertEquals("admin@dotcms.com", user.getEmailAddress());

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        final Date createDate = simpleDateFormat.parse("2008-03-06 17:36:01.0");

        final SimpleDateFormat simpleDateFormat_2 = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals(simpleDateFormat_2.format(createDate), simpleDateFormat_2.format(user.getCreateDate()));

    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a List of {@link Contentlet}
     * Should: get a List of {@link Contentlet} after called the {@link XStream#fromXML(File)} method
     *
     * @throws IOException
     */
    @Test
    public void deserializeContentletList() throws IOException {
        final File file = new File(Thread.currentThread()
                .getContextClassLoader()
                .getResource("xmlStreamFactory/com.dotmarketing.portlets.contentlet.model.Contentle.xml")
                .getFile());

        final byte[] bytes = FileUtil.getBytes(file);
        final String xml = new String(bytes);

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        assertTrue(List.class.isInstance(obj));

        final List<Contentlet> objList = (List) obj;
        assertEquals(1000, objList.size());
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to serialize a {@link Template} object
     * Should: get a xml code
     *
     * @throws IOException
     */
    @Test
    public void serializeTemplate() throws IOException {
        final Template template = new Template();
        template.setOwner("system");
        template.setTitle("title");
        template.setFriendlyName("FriendlyName");

        final String xml = XStreamFactory.INSTANCE.getInstance().toXML(template);

        final String xmlExpected = "<com.dotmarketing.portlets.templates.model.Template>\n"
                + "  <type>template</type>\n"
                + "  <owner>system</owner>\n"
                + "  <source>DB</source>\n"
                + "  <title>title</title>\n"
                + "  <friendlyName>FriendlyName</friendlyName>\n"
                + "  <modUser></modUser>\n"
                + "  <sortOrder>0</sortOrder>\n"
                + "  <showOnMenu>false</showOnMenu>\n"
                + "  <image></image>\n"
                + "  <drawed>false</drawed>\n"
                + "</com.dotmarketing.portlets.templates.model.Template>";

        final String[] xmlLines = String.format(xmlExpected, template.getTitle(), template.getFriendlyName())
                .split("\n");

        for (String line : xmlLines) {
            assertTrue(xml.contains(line));
        }
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a  {@link ContainerStructure}
     * Should: get a  {@link ContainerStructure} object
     *
     * @throws IOException
     */
    @Test
    public void deserializeTemplate() throws IOException {
        final String xml = "<com.dotmarketing.portlets.templates.model.Template>\n"
                + "    <type>template</type>\n"
                + "    <owner>null</owner>\n"
                + "    <inode>418ce11b-8443-4bcd-b3e5-e155d60e996f</inode>\n"
                + "    <identifier>ea887e3a-1e9d-47cf-995a-ce060ae1fc4e</identifier>\n"
                + "    <source>DB</source>\n"
                + "    <title>anonymous_layout_1633117695089</title>\n"
                + "    <friendlyName></friendlyName>\n"
                + "    <modDate class=\"sql-timestamp\">2021-10-01 19:48:15.116</modDate>\n"
                + "    <modUser>system</modUser>\n"
                + "    <sortOrder>0</sortOrder>\n"
                + "    <showOnMenu>false</showOnMenu>\n"
                + "    <body>null</body>\n"
                + "    <image></image>\n"
                + "    <drawed>true</drawed>\n"
                + "    <drawedBody>{&quot;header&quot;:true,&quot;footer&quot;:true,&quot;body&quot;:{&quot;rows&quot;:[{&quot;columns&quot;:[{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;//demo.dotcms.com/application/containers/default/&quot;,&quot;uuid&quot;:&quot;1&quot;}],&quot;widthPercent&quot;:58,&quot;leftOffset&quot;:1,&quot;preview&quot;:false,&quot;width&quot;:7,&quot;left&quot;:0},{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;d71d56b4-0a8b-4bb2-be15-ffa5a23366ea&quot;,&quot;uuid&quot;:&quot;1&quot;}],&quot;widthPercent&quot;:33,&quot;leftOffset&quot;:9,&quot;styleClass&quot;:&quot;&quot;,&quot;preview&quot;:false,&quot;width&quot;:4,&quot;left&quot;:8}]},{&quot;columns&quot;:[{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;//demo.dotcms.com/application/containers/default/&quot;,&quot;uuid&quot;:&quot;2&quot;}],&quot;widthPercent&quot;:50,&quot;leftOffset&quot;:1,&quot;preview&quot;:false,&quot;width&quot;:6,&quot;left&quot;:0},{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;//demo.dotcms.com/application/containers/default/&quot;,&quot;uuid&quot;:&quot;3&quot;}],&quot;widthPercent&quot;:50,&quot;leftOffset&quot;:7,&quot;preview&quot;:false,&quot;width&quot;:6,&quot;left&quot;:6}]},{&quot;columns&quot;:[{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;//demo.dotcms.com/application/containers/default/&quot;,&quot;uuid&quot;:&quot;4&quot;}],&quot;widthPercent&quot;:33,&quot;leftOffset&quot;:1,&quot;preview&quot;:false,&quot;width&quot;:4,&quot;left&quot;:0},{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;//demo.dotcms.com/application/containers/default/&quot;,&quot;uuid&quot;:&quot;5&quot;}],&quot;widthPercent&quot;:33,&quot;leftOffset&quot;:5,&quot;preview&quot;:false,&quot;width&quot;:4,&quot;left&quot;:4},{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;//demo.dotcms.com/application/containers/default/&quot;,&quot;uuid&quot;:&quot;6&quot;}],&quot;widthPercent&quot;:33,&quot;leftOffset&quot;:9,&quot;styleClass&quot;:&quot;&quot;,&quot;preview&quot;:false,&quot;width&quot;:4,&quot;left&quot;:8}]},{&quot;columns&quot;:[{&quot;containers&quot;:[{&quot;identifier&quot;:&quot;//demo.dotcms.com/application/containers/default/&quot;,&quot;uuid&quot;:&quot;7&quot;}],&quot;widthPercent&quot;:100,&quot;leftOffset&quot;:1,&quot;preview&quot;:false,&quot;width&quot;:12,&quot;left&quot;:0}],&quot;styleClass&quot;:&quot;fluid&quot;}]},&quot;sidebar&quot;:{&quot;containers&quot;:[],&quot;location&quot;:&quot;&quot;,&quot;width&quot;:&quot;small&quot;,&quot;widthPercent&quot;:20,&quot;preview&quot;:false}}</drawedBody>\n"
                + "    <countAddContainer>0</countAddContainer>\n"
                + "    <countContainers>0</countContainers>\n"
                + "    <theme>ce00bd28-5f66-47f9-96ca-bbf0722a79aa</theme>\n"
                + "    <header>null</header>\n"
                + "    <footer>null</footer>\n"
                + "  </com.dotmarketing.portlets.templates.model.Template>";

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);

        assertTrue(Template.class.isInstance(obj));

        final Template template = Template.class.cast(obj);

        assertEquals("418ce11b-8443-4bcd-b3e5-e155d60e996f", template.getInode());
        assertEquals("ea887e3a-1e9d-47cf-995a-ce060ae1fc4e", template.getIdentifier());
        assertEquals("anonymous_layout_1633117695089", template.getTitle());
        assertEquals("ce00bd28-5f66-47f9-96ca-bbf0722a79aa", template.getTheme());
        assertEquals(true, template.isDrawed());
    }

    @Test
    public void deserializeTemplateList() throws IOException {
        final File file = new File(Thread.currentThread()
                .getContextClassLoader()
                .getResource("xmlStreamFactory/com.dotmarketing.portlets.templates.model.Template.xml")
                .getFile());

        final byte[] bytes = FileUtil.getBytes(file);
        final String xml = new String(bytes);

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);
        assertTrue(List.class.isInstance(obj));

        final List<Template> objList = (List) obj;
        assertEquals(93, objList.size());
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a  {@link com.dotmarketing.portlets.containers.model.Container}
     * Should: get a  {@link com.dotmarketing.portlets.containers.model.Container} object
     *
     * @throws IOException
     */
    @Test
    public void deserializeContainer() throws IOException {
        final String xml = "  <com.dotmarketing.portlets.containers.model.Container>\n"
                + "    <iDate class=\"sql-timestamp\">2020-01-07 20:44:39.11</iDate>\n"
                + "    <type>containers</type>\n"
                + "    <inode>4b737156-d8c0-4cd8-ab1b-674cf58d22d9</inode>\n"
                + "    <identifier>eba434c6-e67a-4a64-9c88-1faffcafb40d</identifier>\n"
                + "    <source>DB</source>\n"
                + "    <title>FAQ</title>\n"
                + "    <friendlyName></friendlyName>\n"
                + "    <modDate class=\"sql-timestamp\">2020-01-07 20:44:39.138</modDate>\n"
                + "    <modUser>dotcms.org.1</modUser>\n"
                + "    <sortOrder>0</sortOrder>\n"
                + "    <showOnMenu>false</showOnMenu>\n"
                + "    <code></code>\n"
                + "    <maxContentlets>20</maxContentlets>\n"
                + "    <useDiv>false</useDiv>\n"
                + "    <sortContentletsBy></sortContentletsBy>\n"
                + "    <preLoop>&lt;div class=&quot;card-group-custom card-group-corporate&quot; id=&quot;accordion1&quot; role=&quot;tablist&quot; aria-multiselectable=&quot;false&quot;&gt;</preLoop>\n"
                + "    <postLoop>&lt;/div&gt;</postLoop>\n"
                + "    <staticify>false</staticify>\n"
                + "    <luceneQuery></luceneQuery>\n"
                + "    <notes></notes>\n"
                + "  </com.dotmarketing.portlets.containers.model.Container>";

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);

        assertTrue(Container.class.isInstance(obj));

        final Container container = Container.class.cast(obj);

        assertEquals("4b737156-d8c0-4cd8-ab1b-674cf58d22d9", container.getInode());
    }

    /**
     * Method to test: {@link XStreamFactory#getInstance()}
     * When: get an instance from the Factory and try to deserialize a  {@link com.dotmarketing.portlets.folders.model.Folder}
     * Should: get a  {@link com.dotmarketing.portlets.folders.model.Folder} object
     *
     * @throws IOException
     */
    @Test
    public void deserializeFolder() throws IOException {
        final String xml = "<com.dotmarketing.portlets.folders.model.Folder>\n"
                + "    <identifier>153566e7-d757-46f8-b02d-0cdb26511177</identifier>\n"
                + "    <name>components</name>\n"
                + "    <sortOrder>0</sortOrder>\n"
                + "    <showOnMenu>false</showOnMenu>\n"
                + "    <hostId>48190c8c-42c4-46af-8d1a-0cd5db894797</hostId>\n"
                + "    <type>folder</type>\n"
                + "    <title>components</title>\n"
                + "    <filesMasks></filesMasks>\n"
                + "    <defaultFileType>33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d</defaultFileType>\n"
                + "    <modDate class=\"sql-timestamp\">2020-08-18 22:10:31.518</modDate>\n"
                + "    <owner></owner>\n"
                + "    <iDate class=\"sql-timestamp\">2020-02-26 17:50:31.769</iDate>\n"
                + "    <inode>092d885b-8b50-4684-b635-f36bdd3ef220</inode>\n"
                + "  </com.dotmarketing.portlets.folders.model.Folder>";

        final Object obj = XStreamFactory.INSTANCE.getInstance().fromXML(xml);

        assertTrue(Folder.class.isInstance(obj));

        final Folder folder = Folder.class.cast(obj);

        assertEquals("153566e7-d757-46f8-b02d-0cdb26511177", folder.getIdentifier());
        assertEquals("components", folder.getName());
        assertEquals(false, folder.isShowOnMenu());
        assertEquals("48190c8c-42c4-46af-8d1a-0cd5db894797", folder.getHostId());
        assertEquals("33888b6f-7a8e-4069-b1b6-5c1aa9d0a48d", folder.getDefaultFileType());
    }

    @Test
    public void aaa() throws DotDataException, DotSecurityException {

        final File binary = new File(Thread.currentThread().getContextClassLoader().getResource("images/test.jpg").getFile());
        final Host site = new SiteDataGen().nextPersisted();
        final Contentlet contentlet = new FileAssetDataGen(site, binary).nextPersisted();

        final String s = XStreamFactory.INSTANCE.getInstance().toXML(contentlet);
        System.out.println("s = " + s);
    }
}
