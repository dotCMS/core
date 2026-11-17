package com.liferay.portal.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the user resolution of a request in {@link PortalUtil}.
 */
class PortalUtilTest {

    /**
     * A user already resolved for the request is returned as is, without reading the session
     * again: the request keeps its user for the rest of its processing, and a request that has no
     * live session any more does not need one.
     */
    @Test
    void getUser_userAlreadyOnTheRequest_isReturnedWithoutReadingTheSession() {
        final User resolved = new User();
        resolved.setUserId("user_a");
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(resolved);

        assertSame(resolved, PortalUtil.getUser(request));

        verify(request, never()).getSession(anyBoolean());
        verify(request, never()).getSession();
        verify(request).setAttribute(WebKeys.USER_ID, "user_a");
    }
}
