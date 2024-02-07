package com.dotcms.variant.business.web;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.mock.request.MockSession;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.util.LoginMode;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class CurrentVariantWebInterceptorTest  {

    /**
     * Method to test {@link CurrentVariantWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}.
     * When:
     * - The current request had not the variantName Query Params
     * - the referer is not set.
     * - There is not exists any session attribute for the current request.
     * Should: Not set any attribute on the request.
     *
     * @throws IOException
     */
    @Test
    public void whenNoParameterOrRefererIsSet() throws IOException {
        final CurrentVariantWebInterceptor currentVariantWebInterceptor = new CurrentVariantWebInterceptor();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        currentVariantWebInterceptor.intercept(request, response);

        assertNull(request.getAttribute(VariantAPI.VARIANT_KEY));
    }

    /**
     * Method to test {@link CurrentVariantWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}.
     * When:
     * - The current variantName Query Params is set
     * - the referer is set.
     * - There is exists any session attribute for the current request.
     * Should: Not set any attribute on the request.
     *
     * @throws IOException
     */
    @Test
    public void whenParameterIsSet() throws IOException {
        final String variantFromParameter = "variantFromParameter";
        final CurrentVariantWebInterceptor currentVariantWebInterceptor = new CurrentVariantWebInterceptor();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(VariantAPI.VARIANT_KEY)).thenReturn(variantFromParameter);
        when(request.getHeader("referer")).thenReturn("http://localhost:8080/blog?variantName=variantFromReferer");

        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        final CurrentVariantSessionItem currentVariantSessionItem = new CurrentVariantSessionItem("variantFromSession");

        mockSession.setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        currentVariantWebInterceptor.intercept(request, response);

        assertNull(request.getAttribute(VariantAPI.VARIANT_KEY));
    }


    /**
     * Method to test {@link CurrentVariantWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}.
     * When:
     * - The current variantName Query Params is not set
     * - the referer is set and had the variantName Parameter.
     * - There is exists any session attribute for the current request.
     * Should: Not set any attribute on the request.
     *
     * @throws IOException
     */
    @Test
    @DataProvider(value = {
            "http://localhost:8080/blog?variantName=variantFromReferer",
            "http://localhost:8080/blog?firstParameter=firstValue&variantName=variantFromReferer",
            "http://localhost:8080/blog?variantName=variantFromReferer&secondParameter=secondValue",
            "http://localhost:8080/blog?firstParameter=firstValue&variantName=variantFromReferer&thirdParameter=thirdValue"
    })
    public void whenRefererIsSet(String refererValue) throws IOException {
        final CurrentVariantWebInterceptor currentVariantWebInterceptor = new CurrentVariantWebInterceptor();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("referer")).thenReturn(refererValue);

        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        final CurrentVariantSessionItem currentVariantSessionItem = new CurrentVariantSessionItem("variantFromSession");

        mockSession.setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        currentVariantWebInterceptor.intercept(request, response);

        verify(request).setAttribute(VariantAPI.VARIANT_KEY, "variantFromReferer");
    }

    /**
     * Method to test {@link CurrentVariantWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}.
     * When:
     * - The current variantName Query Params is not set
     * - the referer is not set.
     * - There is a session attribute for the current request and the LoginMode is equals to BE.
     * Should: Not set any attribute on the request.
     *
     * @throws IOException
     */
    @Test
    public void whenSessionAttributeIsSet() throws IOException {
        final CurrentVariantWebInterceptor currentVariantWebInterceptor = new CurrentVariantWebInterceptor();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        mockSession.setAttribute(WebKeys.LOGIN_MODE_PARAMETER, LoginMode.BE);

        final CurrentVariantSessionItem currentVariantSessionItem = new CurrentVariantSessionItem("variantFromSession");

        mockSession.setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        currentVariantWebInterceptor.intercept(request, response);

        verify(request, times(1)).setAttribute(VariantAPI.VARIANT_KEY, "variantFromSession");
    }

    /**
     * Method to test {@link CurrentVariantWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}.
     * When:
     * - The current variantName Query Params is not set
     * - the referer is not set.
     * - There is a session attribute for the current request and the LoginMode is equals to FE.
     * Should: Not set any attribute on the request.
     *
     * @throws IOException
     */
    @Test
    public void whenSessionAttributeIsSetLoginModeFE() throws IOException {
        final CurrentVariantWebInterceptor currentVariantWebInterceptor = new CurrentVariantWebInterceptor();

        final User user = mock(User.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);
        when(request.getSession(false)).thenReturn(mockSession);

        mockSession.setAttribute(WebKeys.LOGIN_MODE_PARAMETER, LoginMode.FE);

        final CurrentVariantSessionItem currentVariantSessionItem = new CurrentVariantSessionItem("variantFromSession");

        mockSession.setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        currentVariantWebInterceptor.intercept(request, response);

        verify(request, never()).setAttribute(eq(VariantAPI.VARIANT_KEY), any());
    }

    /**
     * Method to test {@link CurrentVariantWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}.
     * When:
     * - The current variantName Query Params is not set
     * - the referer is not set.
     * - There is a session attribute for the current request and the LoginMode is equals to BE
     * but the {@link CurrentVariantSessionItem} is expire.
     * Should: Not set any attribute on the request.
     *
     * @throws IOException
     */
    @Test
    public void whenSessionAttributeIsSetButExpire() throws IOException {
        final CurrentVariantWebInterceptor currentVariantWebInterceptor = new CurrentVariantWebInterceptor();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        final MockSession mockSession = new MockSession(RandomStringUtils.random(10));
        when(request.getSession()).thenReturn(mockSession);
        when(request.getSession(true)).thenReturn(mockSession);

        mockSession.setAttribute(WebKeys.LOGIN_MODE_PARAMETER, LoginMode.BE);

        final CurrentVariantSessionItem currentVariantSessionItem = mock(CurrentVariantSessionItem.class);
        when(currentVariantSessionItem.getVariantName()).thenReturn("variantFromSession");
        when(currentVariantSessionItem.isExpired()).thenReturn(true);

        mockSession.setAttribute(VariantAPI.VARIANT_KEY, currentVariantSessionItem);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        currentVariantWebInterceptor.intercept(request, response);

        verify(request, never()).setAttribute(eq(VariantAPI.VARIANT_KEY), any());
    }

    /**
     * Method to test {@link CurrentVariantWebInterceptor#intercept(HttpServletRequest, HttpServletResponse)}.
     * When:
     * - The current variantName Query Params is not set
     * - the referer is not set.
     * - There is a session created
     * Should: Not set any attribute on the request.
     *
     * @throws IOException
     */
    @Test
    public void whenSessionIsNotCreated() throws IOException {
        final CurrentVariantWebInterceptor currentVariantWebInterceptor = new CurrentVariantWebInterceptor();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getSession()).thenReturn(null);
        when(request.getSession(true)).thenReturn(null);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        currentVariantWebInterceptor.intercept(request, response);

        verify(request, never()).setAttribute(eq(VariantAPI.VARIANT_KEY), any());
    }


}