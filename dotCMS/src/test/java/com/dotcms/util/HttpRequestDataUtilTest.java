package com.dotcms.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Vector;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by nollymar on 3/8/17.
 */
@RunWith(DataProviderRunner.class)
public class HttpRequestDataUtilTest {

    @Test
    public void test_getIpAddress_XForwardedForWithMultipleIPs_ReturnFirstHost() throws UnknownHostException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("www.dotcms.com,www.google.com,www.github.com");

        InetAddress ip = HttpRequestDataUtil.getIpAddress(request);
        Assert.assertNotNull(ip);
        Assert.assertEquals(ip.getHostName(), "www.dotcms.com");
        Assert.assertNotNull(ip.getHostAddress());

    }

    @DataProvider
    public static Object[] getParamCaseInsensitiveTestCases() {
        return new ParamTestCase[] {
                new ParamTestCase("dotcachettl", "DoTcAcHeTtL"),
                new ParamTestCase("dotcachettl", "DOTCACHETTL"),
                new ParamTestCase("dotcachettl", "dotcachettl")
        };
    }

    @Test
    @UseDataProvider("getParamCaseInsensitiveTestCases")
    public void test_getParamCaseInsensitive(final ParamTestCase testCase) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterNames()).thenReturn(
                Collections.enumeration(Collections.singletonList(testCase.providedParam)));

        when(request.getParameter(testCase.providedParam)).thenReturn("60");

        Optional<String> param = HttpRequestDataUtil
                .getParamCaseInsensitive(request, testCase.paramToLooFor);
        assertTrue(param.isPresent());
        assertEquals("60", param.get());
    }

    @Test
    @UseDataProvider("getParamCaseInsensitiveTestCases")
    public void test_getHeaderCaseInsensitive(final ParamTestCase testCase) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeaderNames()).thenReturn(
                Collections.enumeration(Collections.singletonList(testCase.providedParam)));

        when(request.getHeader(testCase.providedParam)).thenReturn("60");

        Optional<String> header = HttpRequestDataUtil
                .getHeaderCaseInsensitive(request, testCase.paramToLooFor);
        assertTrue(header.isPresent());
        assertEquals("60", header.get());
    }

    private static class ParamTestCase {
        String paramToLooFor;
        String providedParam;

        public ParamTestCase(String paramToLooFor, String providedParam) {
            this.paramToLooFor = paramToLooFor;
            this.providedParam = providedParam;
        }
    }

}
