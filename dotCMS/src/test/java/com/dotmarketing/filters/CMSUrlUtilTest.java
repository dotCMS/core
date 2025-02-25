package com.dotmarketing.filters;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static com.dotmarketing.filters.CMSUrlUtil.isDotAdminRequest;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nollymar 
 */
public class CMSUrlUtilTest {

    /**
     * Method To Test: {@link CMSUrlUtil#getURIFromRequest(HttpServletRequest)}
     * Given Scenario: Call {@link CMSUrlUtil#getURIFromRequest(HttpServletRequest)}
     * with a request that contains a mocked URI with a plus sign and CMS_FILTER_URI_OVERRIDE is not set
     * ExpectedResult: The URI returned must be the same one mocked,
     * including the plus sign
     */
    @Test
    public void testGetURIFromRequestWhenFilterIsNotSet() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("dotcms+test.txt");
        final String result = CMSUrlUtil.getInstance().getURIFromRequest(request);
        assertEquals("dotcms+test.txt", result);
    }

    /**
     * Method To Test: {@link CMSUrlUtil#getURIFromRequest(HttpServletRequest)}
     * Given Scenario: Call {@link CMSUrlUtil#getURIFromRequest(HttpServletRequest)}
     * when CMS_FILTER_URI_OVERRIDE is set. It should return the CMS_FILTER_URI_OVERRIDE value
     * ExpectedResult: The URI returned must be the same one mocked,
     * including the plus sign
     */
    @Test
    public void testGetURIFromRequestWhenFilterIsSet() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CMS_FILTER_URI_OVERRIDE)).thenReturn("dotcms+test.txt");
        final String result = CMSUrlUtil.getInstance().getURIFromRequest(request);
        assertEquals("dotcms+test.txt", result);
    }

    /**
     * Method To Test: {@link CMSUrlUtil#getInodeFromUrlPath(String)}
     * Given Scenario: Invoke with a page live url
     * ExpectedResult: the contentlet identifier will be returned
     */
    @Test
    public void test_getIdentifierFromUrlPath() {
        final String liveUrlPath = "/LIVE/27e8f845c3bd21ad1c601b8fe005caa6/dotParser_1695072095296.container";
        final String contentIdentifier = CMSUrlUtil.getInstance().getInodeFromUrlPath(liveUrlPath);
        assertNotNull(contentIdentifier);
        assertEquals("27e8f845c3bd21ad1c601b8fe005caa6", contentIdentifier);

        final String templateUrlPath = "CONTENT/27e8f845c3bd21ad1c601b8fe005caa6_1695072095296.content";
        final String contentIdentifier2 = CMSUrlUtil.getInstance().getInodeFromUrlPath(templateUrlPath);
        assertNotNull(contentIdentifier2);
        assertEquals("27e8f845c3bd21ad1c601b8fe005caa6", contentIdentifier2);

        final String feUrlPath = "/data/shared/assets/c/e/ce837ff5-dc6f-427a-8f60-d18afc395be9/fileAsset/openai-summarize.vtl";
        final String contentIdentifier3 = CMSUrlUtil.getInstance().getInodeFromUrlPath(feUrlPath);
        assertNotNull(contentIdentifier3);
        assertEquals("ce837ff5-dc6f-427a-8f60-d18afc395be9", contentIdentifier3);


        final String template2UrlPath = "LIVE/d2e56042255158023d03164cd3852ead.templatelayout";
        final String contentIdentifier4 = CMSUrlUtil.getInstance().getInodeFromUrlPath(template2UrlPath);
        assertNotNull(contentIdentifier4);
        assertEquals("d2e56042255158023d03164cd3852ead", contentIdentifier4);

    }

    /**
     * Given scenario: Test the request comes from dotAdmin
     * Expected result: Should return true if the referer is a valid dotAdmin referer
     */
    @Test
    public void testDotAdminRequestValidReferer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("referer")).thenReturn("http://localhost:8080/dotAdmin/somepage");
        assertTrue( "Should be true for valid dotAdmin referer", isDotAdminRequest(request));
    }

    /**
     * Given scenario: Test the request comes from dotAdmin
     * Expected result: Should return true if the referer is a valid dotAdmin referer
     */
    @Test
    public void testDotAdminRequestWithDifferentDomain() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("referer")).thenReturn("http://otherdomain.com/dotAdmin/somepage");
        assertTrue( "Should be true for valid dotAdmin referer", isDotAdminRequest(request));
    }

    /**
     * Given scenario: Test the request comes from dotAdmin
     * Expected result: Should return true if the referer is a valid dotAdmin referer
     */
    @Test
    public void testDotAdminRequestWithoutDotAdmin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("referer")).thenReturn("http://localhost:8080/anotherPath/somepage");
        assertFalse("Should be false if /dotAdmin is not present", isDotAdminRequest(request));
    }

}
