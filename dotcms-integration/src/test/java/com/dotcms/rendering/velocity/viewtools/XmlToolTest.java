package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.liferay.util.StringPool;
import graphql.AssertException;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import org.junit.BeforeClass;
import org.junit.Test;

public class XmlToolTest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_xmltool_using_private_ip_should_throw_an_exception() {
        XmlTool xmlTool = new XmlTool();
        try {
            xmlTool.read(new URL("https://localhost:9999/test"));
        } catch (Exception e){
            assert (e instanceof DotRuntimeException);
            assert (e.getMessage().contains("XMLTool Cannot access private subnets"));
        }
    }

    /**
     * Method to test: {@link XmlTool#read(URL)}
     * when: Read a xml file
     * should: get all the children nodes
     * @throws Exception
     */
    @Test
    public void read() throws Exception {
        final boolean allowAccessToPrivateSubnets = Config.getBooleanProperty(
                "ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false);

        try {
            Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", true);

            final XmlTool xmlTool = new XmlTool();
            final File xmlFile = new File(Thread.currentThread()
                    .getContextClassLoader().getResource("xml/test.xml").getFile());
            xmlTool.read(xmlFile.getAbsolutePath());

            final Iterator<XmlTool> xmlToolIterator = xmlTool.children().iterator();

            assertTrue(xmlToolIterator.hasNext());

            final String expected_1 = "<CD><TITLE>EmpireBurlesque</TITLE><ARTIST>BobDylan</ARTIST><COUNTRY>USA</COUNTRY><COMPANY>Columbia</COMPANY><PRICE>10.90</PRICE><YEAR>1985</YEAR></CD>";
            final String expected_2 = "<CD><TITLE>Hide_your_heart</TITLE><ARTIST>Bonnie_Tyler</ARTIST><COUNTRY>UK</COUNTRY><COMPANY>CBSRecords</COMPANY><PRICE>9.90</PRICE><YEAR>1988</YEAR></CD>";

            int i = 0;

            for (; xmlToolIterator.hasNext(); i++) {
                final XmlTool node = xmlToolIterator.next();
                final String xmlContent = node.toString()
                        .replaceAll(StringPool.SPACE, "")
                        .replaceAll("\n", "");

                if (i == 0) {
                    assertEquals(expected_1, xmlContent);
                } else if (i == 1) {
                    assertEquals(expected_2, xmlContent);
                } else {
                    throw new AssertException("Should have just two lines");
                }
            }
        } finally {
            Config.setProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", allowAccessToPrivateSubnets);
        }
    }
}
