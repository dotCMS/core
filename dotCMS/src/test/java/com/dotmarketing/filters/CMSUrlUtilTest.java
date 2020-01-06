package com.dotmarketing.filters;

import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

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
        String result = CMSUrlUtil.getInstance().getURIFromRequest(request);
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
        String result = CMSUrlUtil.getInstance().getURIFromRequest(request);
        assertEquals("dotcms+test.txt", result);
    }

}
