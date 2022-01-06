package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotRuntimeException;
import java.net.URL;
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
}
