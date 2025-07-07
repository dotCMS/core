package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.Config;
import com.liferay.portal.util.WebKeys;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VelocityRequestWrapper} class.
 * Tests the setAttribute and removeAttribute methods with blacklisted and non-blacklisted attributes.
 */
@RunWith(MockitoJUnitRunner.class)
public class VelocityRequestWrapperTest extends UnitTestBase {

    @Mock
    private HttpServletRequest mockRequest;

    private static boolean originalConfigValue;

    /**
     * Sets up the config property before class loading to ensure SET_VALUE_BLACKLIST is initialized correctly.
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