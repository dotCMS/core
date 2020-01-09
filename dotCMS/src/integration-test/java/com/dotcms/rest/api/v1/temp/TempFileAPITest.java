package com.dotcms.rest.api.v1.temp;

import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TempFileAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test(expected = BadRequestException.class)
    public void testValidURL_urlReturn400_throwsException() throws IOException {
        final String url =  "https://raw.githubusercontent.com/url/throws/400.jpg";
        APILocator.getTempFileAPI().validUrl(url);
    }

    @Test(expected = BadRequestException.class)
    public void testValidURL_urlReturn404_throwsException() throws IOException {
        final String url =  "https://raw.githubusercontent.com/dotCMS/core/throws/dotCMS/404.jpg";
        APILocator.getTempFileAPI().validUrl(url);
    }

    @Test
    public void testValidURL_urlReturn200_returnTrue() throws IOException {
        final String url =  "https://raw.githubusercontent.com/dotCMS/core/master/dotCMS/src/main/webapp/html/images/skin/logo.gif";
        assertTrue(APILocator.getTempFileAPI().validUrl(url));
    }

    @Test
    public void testValidURL_noHTTP_returnFalse() throws IOException {
        final String url =  "test://raw.githubusercontent.com/dotCMS/core/master/dotCMS/src/main/webapp/html/images/skin/logo.gif";
        assertFalse(APILocator.getTempFileAPI().validUrl(url));
    }
}
