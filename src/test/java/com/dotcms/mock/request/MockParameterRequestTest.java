package com.dotcms.mock.request;

import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MockParameterRequestTest {



    HttpServletRequest getMockRequest() {
        return Mockito.mock(HttpServletRequest.class);
    }
    @Test
    void getParameter_returnsCorrectValue() {

        HttpServletRequest mockRequest = getMockRequest();

        Mockito.when(mockRequest.getQueryString()).thenReturn("param2=value2");

        MockParameterRequest mockParameterRequest = new MockParameterRequest(mockRequest);

        assertEquals("value2", mockParameterRequest.getParameter("param2"));
    }

    @Test
    void getParameter_returnsNullForNonExistentParameter() {
        HttpServletRequest mockRequest = getMockRequest();
        Mockito.when(mockRequest.getQueryString()).thenReturn("");

        MockParameterRequest mockParameterRequest = new MockParameterRequest(mockRequest);

        assertNull(mockParameterRequest.getParameter("nonExistentParam"));
    }

    @Test
    void getParameterNames_returnsAllParameterNames() {
        HttpServletRequest mockRequest = getMockRequest();

        Mockito.when(mockRequest.getQueryString()).thenReturn("param1=value1");

        MockParameterRequest mockParameterRequest = new MockParameterRequest(mockRequest);

        Enumeration<String> parameterNames = mockParameterRequest.getParameterNames();
        assertEquals(true, parameterNames.hasMoreElements());
        assertEquals("param1", parameterNames.nextElement());
        assertEquals(false, parameterNames.hasMoreElements());
    }

    @Test
    void getParameterMap_returnsCorrectParameterMap() {
        HttpServletRequest mockRequest = getMockRequest();
        Mockito.when(mockRequest.getParameterNames()).thenReturn(Collections.enumeration(Collections.singleton("param1")));
        Mockito.when(mockRequest.getParameter("param1")).thenReturn("value1");
        Mockito.when(mockRequest.getQueryString()).thenReturn("param1=value1");

        MockParameterRequest mockParameterRequest = new MockParameterRequest(mockRequest);

        Map<String, String[]> parameterMap = mockParameterRequest.getParameterMap();
        assertEquals(1, parameterMap.size());
        assertEquals("value1", parameterMap.get("param1")[0]);
    }

    @Test
    void getParameterMap_handlesEmptyQueryString() {
        HttpServletRequest mockRequest = getMockRequest();
        Mockito.when(mockRequest.getQueryString()).thenReturn("");

        MockParameterRequest mockParameterRequest = new MockParameterRequest(mockRequest);

        Map<String, String[]> parameterMap = mockParameterRequest.getParameterMap();
        assertEquals(0, parameterMap.size());
    }



    @Test
    void test_when_null_paramter_is_passed_in() {

        HttpServletRequest mockRequest = getMockRequest();

        Mockito.when(mockRequest.getQueryString()).thenReturn("param2=value2");
        Map<String,String> badMap = new HashMap<>();

        badMap.put("badParam", null);
        MockParameterRequest mockParameterRequest = new MockParameterRequest(mockRequest, badMap);

        assertEquals("value2", mockParameterRequest.getParameter("param2"));
        assertNull(mockParameterRequest.getParameter("badParam"));
    }



}
