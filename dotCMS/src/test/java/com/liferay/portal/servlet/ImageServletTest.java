package com.liferay.portal.servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for how {@link ImageServlet} serves the stored company logo.
 */
class ImageServletTest {

    private static final String STOCK_LOGO =
            "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    /** A stored dotAsset path is forwarded to, and no error is sent. */
    @Test
    void validLogoPath_isForwarded() throws Exception {
        final RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(STOCK_LOGO)).thenReturn(dispatcher);

        new ImageServlet().serveLogo(STOCK_LOGO, request, response);

        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt());
    }

    /** The forward goes to the stored path with each segment encoded. */
    @Test
    void logoWithReservedCharactersInName_isForwardedEncoded() throws Exception {
        final RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        final String encoded =
                "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/brand%3Bv2%20(1).png";
        when(request.getRequestDispatcher(encoded)).thenReturn(dispatcher);

        new ImageServlet().serveLogo(
                "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/brand;v2 (1).png", request, response);

        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt());
    }

    /** A stored value that is not a dotAsset path gets a 404 and is never dispatched. */
    @Test
    void invalidLogoPath_returnsNotFound() throws Exception {
        new ImageServlet().serveLogo("/dA/../html/logo.png", request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    /** No stored value gets a 404. */
    @Test
    void missingLogoPath_returnsNotFound() throws Exception {
        new ImageServlet().serveLogo(null, request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    /** When the container has no dispatcher for the path, the logo gets a 404. */
    @Test
    void noDispatcherForLogoPath_returnsNotFound() throws Exception {
        when(request.getRequestDispatcher(STOCK_LOGO)).thenReturn(null);

        new ImageServlet().serveLogo(STOCK_LOGO, request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /** A stored logo that is not a dotAsset path is replaced at startup. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/dA/../html/logo.png", "/dA/abc/%2e%2e/%2e%2e/html/logo.png",
            "/dAx/logo.png", "/image/company_logo?img_id=dotcms.org"})
    void storedLogoNotADotAssetPath_isMigrated(final String storedLogo) {
        assertTrue(ImageServlet.needsLogoMigration(storedLogo), String.valueOf(storedLogo));
    }

    /** A stored dotAsset logo is kept. */
    @ParameterizedTest
    @ValueSource(strings = {STOCK_LOGO, "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/my logo (1).png",
            "/dA/a083e942-5fb0-47ef-9619-6f8f40593474/asset/company_logo.png",
            "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/O'Neil.png",
            "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/logo#2.png",
            "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/50%.png"})
    void storedDotAssetLogo_isKept(final String storedLogo) {
        assertFalse(ImageServlet.needsLogoMigration(storedLogo), storedLogo);
    }
}
