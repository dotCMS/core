package com.dotcms.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.dotcms.UnitTestBase;
import com.liferay.util.StringPool;
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
        compare(originalURI, expectedNormalizedURI);

        // A ".." segment is removed only if it is preceded by a non-".." segment  (query strings are not part of URI)
        originalURI = "/test/../folder/folder1/forward_jsp.jsp?FORWARD_URL=http://google.com";
        expectedNormalizedURI = "/folder/folder1/forward_jsp.jsp";
        compare(originalURI, expectedNormalizedURI);

        // A ".." segment is removed only if it is preceded by a non-".." segment
        originalURI = "/test/../folder/important/../secret_file.dat";
        expectedNormalizedURI = "/folder/secret_file.dat";
        compare(originalURI, expectedNormalizedURI);

        // Each "." segment is simply removed
        originalURI = "./folder/folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI);

        // Each "." segment is simply removed
        originalURI = "./folder/./folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI);

        // multiple ../../
        originalURI = "../../../folder/./folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI);
        
        // testing double dots
        originalURI = "..";
        expectedNormalizedURI = "/";
        compare(originalURI, expectedNormalizedURI);
        
        // testing double dots, slash
        originalURI = "../";
        expectedNormalizedURI = "/";
        compare(originalURI, expectedNormalizedURI);
        
        // testing double dots, slash,double dots 
        originalURI = "../..";
        expectedNormalizedURI = "/";
        compare(originalURI, expectedNormalizedURI);
        
        
        // testing double dots, slash,double dots , slash
        originalURI = "../../";
        expectedNormalizedURI = "/";
        compare(originalURI, expectedNormalizedURI);
        
    }

    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * valid URIs
     */
    @Test
    public void test_uri_normalization_valid_URI() throws IOException, ServletException {

        String originalURI = "/folder/important/secret_file.dat";
        compare(originalURI, originalURI);

        // (query strings are not part of URI)
        originalURI = "/folder/folder1/forward_jsp.jsp?FORWARD_URL=http://google.com";
        compare(originalURI, originalURI.substring(0,originalURI.indexOf("?")));

        originalURI = "folder/folder1/file.dat";
        compare(originalURI, StringPool.SLASH + originalURI);

        // remove all .. segments
        originalURI = "../folder/folder1/file.dat";
        compare(originalURI, originalURI.replace("..", ""));
    }

    private void compare(final String originalURI, final String expectedNormalizedURI)
            throws IOException, ServletException {

        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn(originalURI);

        //Calling the normalization filter
        normalizationFilter.doFilter(request, response, chain);
        //Verify the getRequestURI is working after passing throw the filter
        final String normalizedValueByFilter = capturedRequest.getValue().getRequestURI();

        assertNotNull(normalizedValueByFilter);
        assertEquals(expectedNormalizedURI, normalizedValueByFilter);
    }
    
    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * invalids URIs
     */
    @Test
    public void test_uri_normalizer_fixes_double_slashes() throws IOException, ServletException {

        
        // remove all //, replace with /
        String originalURI = "//folder/important/secret_file.dat";
        String expectedNormalizedURI = "/folder/important/secret_file.dat";
        compare(originalURI, expectedNormalizedURI);

        // testing ///
        originalURI = "///folder/important/secret_file.dat";
        compare(originalURI, expectedNormalizedURI);

        // testing ////
        originalURI = "////folder/important/secret_file.dat";
        compare(originalURI, expectedNormalizedURI);
        

        
        // testing multiple ////
        originalURI = "////folder/important///secret_file.dat";
        compare(originalURI, expectedNormalizedURI);
        
        // testing // not at the root (query strings are not part of URI)
        originalURI = "/test//folder/folder1//forward_jsp.jsp?FORWARD_URL=http://google.com";
        expectedNormalizedURI = "/test/folder/folder1/forward_jsp.jsp";
        compare(originalURI, expectedNormalizedURI);

        // A ".." segment is removed only if it is preceded by a non-".." segment
        originalURI = "///test/../folder/important//../secret_file.dat";
        expectedNormalizedURI = "/folder/secret_file.dat";
        compare(originalURI, expectedNormalizedURI);

        // Each "." segment is simply removed
        originalURI = "./f/older//folder1//file.dat";
        expectedNormalizedURI = "/f/older/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI);

        // Each "." segment is simply removed
        originalURI = "./folder/./folder1/file.dat//..//";
        expectedNormalizedURI = "/folder/folder1/";
        compare(originalURI, expectedNormalizedURI);

        // starts with ..//
        originalURI = "..//folder/./folder1//file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        compare(originalURI, expectedNormalizedURI);
    }
    
    
    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * invalids URIs
     */
    @Test
    public void test_uri_normalizer_invalid_uris() throws IOException, ServletException {
        // testing escaped slashes - Filter barfs and returns /
        String originalURI = "/\\///folder//important////secret_file.dat";
        compare(originalURI, "/");
        
        
    }
    

}