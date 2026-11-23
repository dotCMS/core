package com.liferay.portlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.liferay.portal.model.Company;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.Encryptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Checks the portal URLs that {@link PortletURLImpl} builds.
 *
 * @author hassandotcms
 */
class PortletURLImplTest {

    /**
     * Method to test: {@link PortletURLImpl#toString()} after {@link PortletURLImpl#setEncrypt(boolean)}
     * Given scenario: a URL for a portlet, a layout and one parameter, with {@code setEncrypt(true)}
     * called on it
     * Expected result: the URL carries every value URL-encoded as it was set, and nothing else
     */
    @Test
    void testToString_whenSetEncryptCalled_shouldKeepValuesAsSet() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(WebKeys.CTX_PATH)).thenReturn("/c");

        // the company has a real key, so any transformation of the values would change the URL
        final Company company = mock(Company.class);
        when(company.getKeyObj()).thenReturn(Encryptor.generateKey());

        try (MockedStatic<PortalUtil> portalUtil = mockStatic(PortalUtil.class, CALLS_REAL_METHODS)) {
            portalUtil.when(() -> PortalUtil.getAuthorizedPath(any(HttpServletRequest.class))).thenReturn("");
            portalUtil.when(() -> PortalUtil.getCompany(any(HttpServletRequest.class))).thenReturn(company);

            final PortletURLImpl url = new PortletURLImpl(request, "site-browser", "layout-1", false);
            url.setParameter("struts_action", "/ext/browser/view_browser");
            url.setEncrypt(true);

            assertEquals("/c/portal/layout?p_l_id=layout-1&p_p_id=site-browser&p_p_action=0"
                    + "&_site_browser_struts_action=%2Fext%2Fbrowser%2Fview_browser", url.toString());
        }
    }

}
