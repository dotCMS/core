package com.dotcms.rest.api.v1.temp;

import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.mock.request.MockSession;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class TempFileAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testValidURL_urlReturn400_throwsException() {
        final String url =  "https://raw.githubusercontent.com/url/throws/400.jpg";
        assertFalse(APILocator.getTempFileAPI().validUrl(url));
    }

    @Test
    public void testValidURL_urlReturn404_throwsException() {
        final String url =  "https://raw.githubusercontent.com/dotCMS/core/throws/dotCMS/404.jpg";
        assertFalse(APILocator.getTempFileAPI().validUrl(url));
    }

    @Test
    public void testValidURL_urlReturn200_returnTrue() {
        final String url =  "https://raw.githubusercontent.com/dotCMS/core/main/dotCMS/src/main/webapp/html/images/skin/logo.gif";
        assertTrue(APILocator.getTempFileAPI().validUrl(url));
    }

    @Test
    public void testValidURL_noHTTP_returnFalse() {
        final String url =  "test://raw.githubusercontent.com/dotCMS/core/main/dotCMS/src/main/webapp/html/images/skin/logo.gif";
        assertFalse(APILocator.getTempFileAPI().validUrl(url));
    }

    /**
     * Method to test: {@link TempFileAPI#getTempResourceId(File)}
     * Test scenario: create a temp file using the api
     * Expected: see how the method succeeds
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Get_Temp_Id_From_Temp_File() throws DotSecurityException, IOException {
        final File file = FileUtil.createTemporaryFile("test", "txt", "");
        final HttpServletRequest request = mockHttpServletRequest();
        final DotTempFile dotTempFile = APILocator.getTempFileAPI().createTempFile("temp", request, com.liferay.util.FileUtil.createInputStream(file));
        assertTrue(dotTempFile.file.exists());
        final Optional<String> tempId = APILocator.getTempFileAPI().getTempResourceId(dotTempFile.file);
        assertTrue(tempId.isPresent());
        assertEquals(tempId.get(), dotTempFile.id);
    }

    /**
     * Method to test: {@link TempFileAPI#getTempResourceId(File)}
     * Test scenario: Create any non-temp file
     * Expected: See how the method fails to extract the temp_id
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Get_Temp_Id_From_Regular_File() throws IOException {
        final File file = getFileAssetContent(true, 1L, TestFile.PNG).getBinary("fileAsset");
        final Optional<String> tempId = APILocator.getTempFileAPI().getTempResourceId(file);
        assertFalse(tempId.isPresent());
    }

    @NotNull
    public static HttpServletRequest mockHttpServletRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());
        final MockSession mockSession = new MockSession(UUIDGenerator.generateUuid());
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(Mockito.anyBoolean())).thenReturn(mockSession);

        when(request.getHeader("User-Agent")).thenReturn("any");
        when(request.getHeader("Host")).thenReturn("localhost");
        when(request.getHeader("Accept-Language")).thenReturn("any");
        when(request.getHeader("Accept-Encoding")).thenReturn("any");
        when(request.getHeader("X-Forwarded-For")).thenReturn("any");
        when(request.getHeader("Origin")).thenReturn("any");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        when(request.isSecure()).thenReturn(false);
        return request;
    }


}
