package com.dotcms.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.dotcms.UnitTestBase;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class NormalizationFilterTest extends UnitTestBase {

    private final static NormalizationFilter normalizationFilter = new NormalizationFilter();
    private static HttpServletResponse response;
    private static FilterChain chain;
    private static ArgumentCaptor<HttpServletRequest> capturedRequest;

    @BeforeClass
    public static void prepare() throws IOException, ServletException {

        //Response
        response = mock(HttpServletResponse.class);

        //Chain
        chain = mock(FilterChain.class);
        capturedRequest = ArgumentCaptor
                .forClass(HttpServletRequest.class);
        //Capturing the request when is passed down to the chain
        doNothing().when(chain).doFilter(capturedRequest.capture(), any(HttpServletResponse.class));
    }

    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * invalids URIs
     */
    @Test
    public void test_uri_normalization_invalid_URI() throws IOException, ServletException {

        // A ".." segment is removed only if it is preceded by a non-".." segment
        String originalURI = "/test/../folder/important/secret_file.dat";
        String expectedNormalizedURI = "/folder/important/secret_file.dat";
        compare(originalURI, expectedNormalizedURI, Boolean.FALSE);

        // A ".." segment is removed only if it is preceded by a non-".." segment
        originalURI = "/test/../folder/folder1/forward_jsp.jsp?FORWARD_URL=http://google.com";
        expectedNormalizedURI = "/folder/folder1/forward_jsp.jsp?FORWARD_URL=http://google.com";
        compare(originalURI, expectedNormalizedURI, Boolean.FALSE);

        // A ".." segment is removed only if it is preceded by a non-".." segment
        originalURI = "/test/../folder/important/../secret_file.dat";
        expectedNormalizedURI = "/folder/secret_file.dat";
        compare(originalURI, expectedNormalizedURI, Boolean.FALSE);

        // Each "." segment is simply removed
        originalURI = "./folder/folder1/file.dat";
        expectedNormalizedURI = "folder/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI, Boolean.FALSE);

        // Each "." segment is simply removed
        originalURI = "./folder/./folder1/file.dat";
        expectedNormalizedURI = "folder/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI, Boolean.FALSE);

        // Each "." segment is simply removed
        originalURI = "/folder/./folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI, Boolean.FALSE);
    }

    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * valid URIs
     */
    @Test
    public void test_uri_normalization_valid_URI() throws IOException, ServletException {

        String originalURI = "/folder/important/secret_file.dat";
        compare(originalURI, originalURI, Boolean.TRUE);

        originalURI = "/folder/folder1/forward_jsp.jsp?FORWARD_URL=http://google.com";
        compare(originalURI, originalURI, Boolean.TRUE);

        originalURI = "folder/folder1/file.dat";
        compare(originalURI, originalURI, Boolean.TRUE);

        // A ".." segment is removed only if it is preceded by a non-".." segment
        originalURI = "../folder/folder1/file.dat";
        compare(originalURI, originalURI, Boolean.TRUE);
    }

    private void compare(final String originalURI, final String expectedNormalizedURI,
            boolean equals)
            throws IOException, ServletException {

        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn(originalURI);

        //Calling the normalization filter
        normalizationFilter.doFilter(request, response, chain);
        //Verify the getRequestURI is working after passing throw the filter
        final String normalizedValueByFilter = capturedRequest.getValue().getRequestURI();

        assertNotNull(normalizedValueByFilter);
        if (equals) {
            assertEquals(originalURI, normalizedValueByFilter);
        } else {
            assertNotEquals(originalURI, normalizedValueByFilter);
        }
        assertEquals(expectedNormalizedURI, normalizedValueByFilter);
    }

}