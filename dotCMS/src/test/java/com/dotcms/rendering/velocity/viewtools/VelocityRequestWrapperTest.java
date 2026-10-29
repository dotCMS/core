package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotmarketing.business.LoginAsSession;
import com.dotmarketing.util.Config;
import com.liferay.portal.util.WebKeys;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VelocityRequestWrapper} class.
 * Tests the setAttribute and removeAttribute methods with blacklisted and non-blacklisted attributes.
 */
@RunWith(MockitoJUnitRunner.class)
public class VelocityRequestWrapperTest extends UnitTestBase {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpSession mockSession;

    private static boolean originalConfigValue;

    /**
     * Sets up the config property before class loading to ensure the attribute lists are initialized correctly.
     */
    @BeforeClass
    public static void setUpClass() {
        // Store original config value to restore after all tests
        originalConfigValue = Config.getBooleanProperty("VELOCITY_PREVENT_SETTING_USER_ID", true);
        Config.setProperty("VELOCITY_PREVENT_SETTING_USER_ID", true);
    }

    /**
     * Restores the original config value after all tests complete.
     */
    @AfterClass
    public static void tearDownClass() {
        // Restore original config value
        Config.setProperty("VELOCITY_PREVENT_SETTING_USER_ID", originalConfigValue);
    }

    /**
     * Tests that the VelocityRequestWrapper correctly prevents setting blacklisted attributes.
     * Specifically, it checks that USER_ID attributes is not set.
     */
    @Test
    public void testSetAttribute_withBlacklistedUserIdAttribute_shouldNotSetAttribute() {
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);
        
        // When: setting a blacklisted attribute (USER_ID)
        wrapper.setAttribute(WebKeys.USER_ID, "testUserId");
        
        // Then: setAttribute should not be called on the wrapped request
        verify(mockRequest, never()).setAttribute(WebKeys.USER_ID, "testUserId");
    }

    /**
     * Tests that the VelocityRequestWrapper correctly prevents setting blacklisted attributes.
     * Specifically, it checks that USER attribute is not set.
     */
    @Test
    public void testSetAttribute_withBlacklistedUserAttribute_shouldNotSetAttribute() {
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);
        
        // When: setting a blacklisted attribute (USER)
        wrapper.setAttribute(WebKeys.USER, "testUser");
        
        // Then: setAttribute should not be called on the wrapped request
        verify(mockRequest, never()).setAttribute(WebKeys.USER, "testUser");
    }

    /**
     * Tests that the VelocityRequestWrapper allows setting non-blacklisted attributes.
     * Specifically, it checks that a custom attribute is set correctly.
     */
    @Test
    public void testSetAttribute_withNonBlacklistedAttribute_shouldSetAttribute() {
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);
        final String testKey = "testAttribute";
        final String testValue = "testValue";
        
        // When: setting a non-blacklisted attribute
        wrapper.setAttribute(testKey, testValue);
        
        // Then: setAttribute should be called on the wrapped request
        verify(mockRequest, times(1)).setAttribute(testKey, testValue);
    }

    /**
     * Tests that Velocity code cannot set the Login As principal on the session or the request;
     * only the Login As endpoint sets it.
     */
    @Test
    public void testSetAttribute_withLoginAsPrincipal_shouldNotSetAttribute() {
        when(mockRequest.getSession()).thenReturn(mockSession);
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);

        wrapper.getSession().setAttribute(WebKeys.PRINCIPAL_USER_ID, "dotcms.org.1");
        wrapper.setAttribute(WebKeys.PRINCIPAL_USER_ID, "dotcms.org.1");

        verify(mockSession, never()).setAttribute(eq(WebKeys.PRINCIPAL_USER_ID), any());
        verify(mockRequest, never()).setAttribute(eq(WebKeys.PRINCIPAL_USER_ID), any());
    }

    /**
     * Tests that Velocity code cannot remove the Login As record from the session, while ordinary
     * session attributes can still be removed.
     */
    @Test
    public void testSessionRemoveAttribute_withLoginAsSession_shouldNotRemoveAttribute() {
        when(mockRequest.getSession()).thenReturn(mockSession);
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);

        wrapper.getSession().removeAttribute(LoginAsSession.SESSION_KEY);
        wrapper.getSession().removeValue(LoginAsSession.SESSION_KEY);
        wrapper.getSession().removeAttribute("testAttribute");

        verify(mockSession, never()).removeAttribute(LoginAsSession.SESSION_KEY);
        verify(mockSession, times(1)).removeAttribute("testAttribute");
    }

    /**
     * Tests that Velocity code can still remove the session user, so a template can end a
     * visitor's session.
     */
    @Test
    public void testSessionRemoveAttribute_withUserId_shouldRemoveAttribute() {
        when(mockRequest.getSession()).thenReturn(mockSession);
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);

        wrapper.getSession().removeAttribute(WebKeys.USER_ID);

        verify(mockSession, times(1)).removeAttribute(WebKeys.USER_ID);
    }

    /**
     * Tests that Velocity code can neither set nor remove the edit-mode and OAuth native-login
     * attributes, on the request or on the session.
     */
    @Test
    public void testSetAndRemoveAttribute_withEditModeAndNativeLogin_shouldBeIgnored() {
        when(mockRequest.getSession()).thenReturn(mockSession);
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);

        for (final String key : List.of(WebKeys.LOGIN_TO_EDIT_MODE, OAuthConstants.SESSION_NATIVE_LOGIN)) {
            wrapper.setAttribute(key, "true");
            wrapper.removeAttribute(key);
            wrapper.getSession().setAttribute(key, "true");
            wrapper.getSession().removeAttribute(key);

            verify(mockRequest, never()).setAttribute(eq(key), any());
            verify(mockRequest, never()).removeAttribute(key);
            verify(mockSession, never()).setAttribute(eq(key), any());
            verify(mockSession, never()).removeAttribute(key);
        }
    }

    /**
     * Tests that ordinary session attributes set from Velocity still reach the session.
     */
    @Test
    public void testSessionSetAttribute_withNonBlacklistedAttribute_shouldSetAttribute() {
        when(mockRequest.getSession()).thenReturn(mockSession);
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);

        wrapper.getSession().setAttribute("testAttribute", "testValue");

        verify(mockSession, times(1)).setAttribute("testAttribute", "testValue");
    }

    /**
     * Tests that the VelocityRequestWrapper correctly prevents removing blacklisted attributes.
     * Specifically, it checks that USER_ID attribute is not removed.
     */
    @Test
    public void testRemoveAttribute_withBlacklistedUserIdAttribute_shouldNotRemoveAttribute() {
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);
        
        // When: removing a blacklisted attribute (USER_ID)
        wrapper.removeAttribute(WebKeys.USER_ID);
        
        // Then: removeAttribute should not be called on the wrapped request
        verify(mockRequest, never()).removeAttribute(WebKeys.USER_ID);
    }

    /**
     * Tests that the VelocityRequestWrapper correctly prevents removing blacklisted attributes.
     * Specifically, it checks that USER attribute is not removed.
     */
    @Test
    public void testRemoveAttribute_withBlacklistedUserAttribute_shouldNotRemoveAttribute() {
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);
        
        // When: removing a blacklisted attribute (USER)
        wrapper.removeAttribute(WebKeys.USER);
        
        // Then: removeAttribute should not be called on the wrapped request
        verify(mockRequest, never()).removeAttribute(WebKeys.USER);
    }

    /**
     * Tests that the VelocityRequestWrapper allows removing non-blacklisted attributes.
     * Specifically, it checks that a custom attribute is removed correctly.
     */
    @Test
    public void testRemoveAttribute_withNonBlacklistedAttribute_shouldRemoveAttribute() {
        final VelocityRequestWrapper wrapper = VelocityRequestWrapper.wrapVelocityRequest(mockRequest);
        final String testKey = "testAttribute";
        
        // When: removing a non-blacklisted attribute
        wrapper.removeAttribute(testKey);
        
        // Then: removeAttribute should be called on the wrapped request
        verify(mockRequest, times(1)).removeAttribute(testKey);
    }
}