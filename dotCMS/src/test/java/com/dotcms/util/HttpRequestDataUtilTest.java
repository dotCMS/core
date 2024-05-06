package com.dotcms.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.*;

import org.junit.Assert;
import org.junit.Test;

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
                Collections.enumeration(List.of(testCase.providedParam)));

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
                Collections.enumeration(List.of(testCase.providedParam)));

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

    @Test
    public void getQueryParamsFromUrlWithOneQueryParams(){

        final Map<String, String> queryParams = HttpRequestDataUtil.getQueryParams(
                "http://localhost:8080/test?testParam=testValue");

        Assert.assertEquals(1, queryParams.size());

        Assert.assertEquals("testValue", queryParams.get("testParam"));
    }

    @Test
    public void getQueryParamsFromUrlWithMultiQueryParams(){

        final Map<String, String> queryParams = HttpRequestDataUtil.getQueryParams(
                "http://localhost:8080/test?testParam1=testValue1&testParam2=testValue2");


        Assert.assertEquals(2, queryParams.size());

        Assert.assertEquals("testValue1", queryParams.get("testParam1"));
        Assert.assertEquals("testValue2", queryParams.get("testParam2"));
    }

    @Test
    public void getQueryParamsFromUrlWithNoneQueryParams(){
        final Map<String, String> queryParams = HttpRequestDataUtil.getQueryParams(
                "http://localhost:8080/test");

        Assert.assertEquals(0, queryParams.size());
    }
}
