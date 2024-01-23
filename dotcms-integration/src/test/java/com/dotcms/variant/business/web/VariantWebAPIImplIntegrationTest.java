package com.dotcms.variant.business.web;

import static org.junit.Assert.assertEquals;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.mock.request.MockSession;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.web.WebAPILocator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VariantWebAPIImplIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test {@link VariantWebAPIImpl#currentVariantId()}.
     * When: Not Exists any Request set on the HttpServletRequestThreadLocal for the current Thread
     * Should: return DEFAULT Variant
     */
    @Test
    public void getDefaultWhenRequestIsNull() {

        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        final VariantWebAPI variantWebAPI = WebAPILocator.getVariantWebAPI();
        final String currentVariantId = variantWebAPI.currentVariantId();

        assertEquals(VariantAPI.DEFAULT_VARIANT.name(), currentVariantId);
    }

    /**
     * Method to test {@link VariantWebAPIImpl#currentVariantId()}.
     * When: The current request had the variantName Query Params
     * Should: return the Query Params value.
     */
    @Test
    public void getCurrentVariantIdFromQueryParams() {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Variant anotherVariant = new VariantDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variant.name());
        when(request.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(anotherVariant.name());

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        try {
            final VariantWebAPI variantWebAPI = WebAPILocator.getVariantWebAPI();

            final String currentVariantId = variantWebAPI.currentVariantId();

            assertEquals(variant.name(), currentVariantId);

            assertNotNull(mockSession.getAttribute(VariantAPI.VARIANT_KEY));

            final CurrentVariantSessionItem currentVariantSessionItem = (CurrentVariantSessionItem)
                    mockSession.getAttribute(VariantAPI.VARIANT_KEY);
            assertEquals(currentVariantSessionItem.getVariantName(), variant.name());
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        }
    }

    /**
     * Method to test {@link VariantWebAPIImpl#currentVariantId()}.
     * When: The current request had the variantName Query Params but the variant does not exists
     * Should: Return the DEFAULT Variant
     */
    @Test
    public void notExistsVariant() {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn("notExistsVariant");

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        try {
            final VariantWebAPI variantWebAPI = WebAPILocator.getVariantWebAPI();
            final String currentVariantId = variantWebAPI.currentVariantId();

            assertEquals(VariantAPI.DEFAULT_VARIANT.name(), currentVariantId);
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        }
    }

    /**
     * Method to test {@link VariantWebAPIImpl#currentVariantId()}.
     * When: The current request had not the variantName Query Params but had the referer header
     * Should: return the Query Params value.
     */
    @Test
    public void getCurrentVariantIdFromAttribute() {
        final Variant variant = new VariantDataGen().nextPersisted();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(variant.name());

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        try {
            final VariantWebAPI variantWebAPI = WebAPILocator.getVariantWebAPI();

            final String currentVariantId = variantWebAPI.currentVariantId();

            assertEquals(variant.name(), currentVariantId);

            final CurrentVariantSessionItem currentVariantSessionItem = (CurrentVariantSessionItem)
                    mockSession.getAttribute(VariantAPI.VARIANT_KEY);

            assertEquals(currentVariantSessionItem.getVariantName(), variant.name());
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        }
    }

    /**
     * Method to test {@link VariantWebAPIImpl#currentVariantId()}.
     * When: The current request had the variantName Query Params and the Session already set the Variant attribute
     * Should: Not set the Session Attribute again
     */
    @Test
    public void justOnceSessionAttributeSet() {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Variant anotherVariant = new VariantDataGen().nextPersisted();

        final HttpSession mockSession = mock(HttpSession.class);
        final HttpServletRequest request = createHttpServletRequest(variant, anotherVariant,
                mockSession);

        final CurrentVariantSessionItem currentVariantSessionItem = new CurrentVariantSessionItem(variant.name());
        when(mockSession.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final VariantWebAPI variantWebAPI = WebAPILocator.getVariantWebAPI();

        variantWebAPI.currentVariantId();

        final CurrentVariantSessionItem currentVariantSessionItemExpected = new CurrentVariantSessionItem(variant.name());

        verify(mockSession, never()).setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItemExpected);

    }

    /**
     * Method to test {@link VariantWebAPIImpl#currentVariantId()}.
     * When: The current request had the variantName Query Params and the Session already set the Variant attribute,
     * but they are different
     * Should: Must set the session Attribute again with the new value
     */
    @Test
    public void setAgainIfCurrentVariantChanged() {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Variant anotherVariant = new VariantDataGen().nextPersisted();

        final HttpSession mockSession = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(anotherVariant.name());

        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        final CurrentVariantSessionItem currentVariantSessionItem =  new CurrentVariantSessionItem(variant.name());
        when(mockSession.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final VariantWebAPI variantWebAPI = WebAPILocator.getVariantWebAPI();

        variantWebAPI.currentVariantId();

        final CurrentVariantSessionItem currentVariantSessionItemExpected = new CurrentVariantSessionItem(anotherVariant.name());

        verify(mockSession, times(1)).setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItemExpected);

    }

    /**
     * Method to test {@link VariantWebAPIImpl#currentVariantId()}.
     * When: The current request had the variantName Query Params and the Session already set the Variant attribute
     * They are the same values
     * Should: Not set the Session Attribute again
     */
    @Test
    public void notSetAgainIfCurrentVariantIsSame() {
        final Variant variant = new VariantDataGen().nextPersisted();

        final HttpSession mockSession = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variant.name());

        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        final CurrentVariantSessionItem currentVariantSessionItem =  new CurrentVariantSessionItem(variant.name());
        when(mockSession.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final VariantWebAPI variantWebAPI = WebAPILocator.getVariantWebAPI();

        variantWebAPI.currentVariantId();

        final CurrentVariantSessionItem currentVariantSessionItemExpected = new CurrentVariantSessionItem(variant.name());

        verify(mockSession, never()).setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItemExpected);

    }
    private static HttpServletRequest createHttpServletRequest(final Variant variant,
            final Variant anotherVariant,
            final HttpSession mockSession) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variant.name());
        when(request.getAttribute(VariantAPI.VARIANT_KEY)).thenReturn(anotherVariant.name());

        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);
        return request;
    }
}