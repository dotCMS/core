package com.dotcms.filters;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import com.dotcms.UnitTestBase;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.MockHttpStatusResponse;

public class NormalizationFilterTest extends UnitTestBase {

    private static final NormalizationFilter normalizationFilter = new NormalizationFilter();
    private static HttpServletResponse mockResponse;
    private static HttpServletRequest mockRequest;
    private static FilterChain chain;
    private static ArgumentCaptor<HttpServletRequest> capturedRequest;

    @BeforeClass
    public static void prepare() throws IOException, ServletException {

        //Response
        mockRequest = mock(HttpServletRequest.class);


        //Response
        mockResponse = mock(HttpServletResponse.class);

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
        String expectedNormalizedURI = "/testfolder/important/secret_file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);

        // A ".." segment is removed only if it is preceded by a non-".." segment  (query strings are not part of URI)
        originalURI = "/test/../folder/folder1/forward_jsp.jsp?FORWARD_URL=http://google.com";
        expectedNormalizedURI = "/testfolder/folder1/forward_jsp.jsp";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);

        // A ".." segment is removed only if it is preceded by a non-".." segment
        originalURI = "/test/../folder/important/../secret_file.dat";
        expectedNormalizedURI = "/testfolder/importantsecret_file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);

        // Each "." segment is simply removed
        originalURI = "/./folder/folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);

        // Each "." segment is simply removed
        originalURI = "./folder/./folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);

        // multiple ../../
        originalURI = "../../../folder/./folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);
        
        // testing double dots
        originalURI = "..";
        expectedNormalizedURI = "/";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);
        
        // testing double dots, slash
        originalURI = "../";
        shouldFail(originalURI);

        
        // testing double dots, slash,double dots 
        originalURI = "../..";
        shouldFail(originalURI);

        
        // testing double dots, slash,double dots , slash
        originalURI = "../../";
        shouldFail(originalURI);
        

        
    }

    
    /**
     * Test that urls with double slashes are blocked.
     */
    @Test
    public void test_double_slashes_are_blocked() throws IOException, ServletException {
        // testing double backslashes
        String originalURI = "//html/portlet/ext/files/edit_text_inc.jsp";
        shouldFail(originalURI);
    
    
        originalURI = "/html/portlet/ext/files//edit_text_inc.jsp";
        shouldFail(originalURI);
        
        
        
    }
    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * valid URIs
     */
    @Test
    public void test_uri_normalization_valid_URI() throws IOException, ServletException {

        String originalURI = "/folder/important/secret_file.dat";
        shouldWork(originalURI,originalURI);
        // (query strings are not part of URI)
        originalURI = "/folder/folder1/forward_jsp.jsp?FORWARD_URL=http://google.com";
        shouldWork(originalURI,"/folder/folder1/forward_jsp.jsp");

        originalURI = "folder/folder1/file.dat";
        shouldWork(originalURI,"/folder/folder1/forward_jsp.jsp");

        // remove all .. segments
        originalURI = "../folder/folder1/file.dat";
        shouldFail(originalURI);
    }

    private void shouldFail(final String originalURI)
            throws IOException, ServletException {

        HttpServletRequest request = new FakeHttpRequest("localhost", originalURI.contains("?") ? originalURI.substring(0, originalURI.indexOf("?")) : originalURI).request();

        MockHttpStatusResponse response = new MockHttpStatusResponse(mockResponse);
        //Calling the normalization filter
        new NormalizationFilter().doFilter(request, response, chain);




        assert(response.getStatus()==404);



    }

    private void shouldWork(final String originalURI, final String expectedURI)
                    throws IOException, ServletException {

        HttpServletRequest request = new FakeHttpRequest("localhost",
                        originalURI.contains("?") ? originalURI.substring(0, originalURI.indexOf("?")) : originalURI).request();

        MockHttpStatusResponse response = new MockHttpStatusResponse(mockResponse);
        // Calling the normalization filter



        try {
            normalizationFilter.doFilter(request, response, chain);
        }catch(Exception e) {
            assertTrue("got error:" + originalURI + ":"+ e.getMessage(), false);
        }
        assertTrue("should be 200:" + originalURI + " vs. "+ expectedURI, response.getStatus()==200);




    }

    /**
     * tests that any URL with bad characters in it throw a 404 response
     */
    private void checkEvilChars(final String originalURI, final String... shouldNotContain) throws IOException, ServletException {

        HttpServletRequest request = new FakeHttpRequest("localhost", originalURI).request();

        MockHttpStatusResponse response = new MockHttpStatusResponse(mockResponse);


        normalizationFilter.doFilter(request, response, chain);


        assertTrue("expecting failure:"+originalURI , response.getStatus()==404);
    }



    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * invalids URIs
     */
    @Test
    public void test_uri_normalizer_fixes_semicolons() throws IOException, ServletException {

        // remove all ;
        String originalURI = "//folder;/important/secret_file.dat";
        String expectedNormalizedURI = "/folder/important/secret_file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);



        // testing ///
        originalURI = "///folder/important/secret_file.dat;jsessionId=0";
        expectedNormalizedURI = "/folder/important/secret_file.datjsessionId0";

        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);


        // testing ////
        originalURI = "////;jsessionId=0;folder/important/secret_file.dat";
        expectedNormalizedURI = "/jsessionId0folder/important/secret_file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);





    }
    
    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * invalids URIs
     */
    @Test
    public void test_uri_normalizer_dot_and_double_dots() throws IOException, ServletException {
        // A ".." segment is removed only if it is preceded by a non-".." segment
        String originalURI = "///test/../folder/important//../secret_file.dat";
        String expectedNormalizedURI = "/testfolder/important/secret_file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);

        // Each "." segment is simply removed
        originalURI = "./folder/./folder1/file.dat/../";
        expectedNormalizedURI = "/folder/folder1/file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);

        // starts with ../
        originalURI = "../folder/./folder1/file.dat";
        expectedNormalizedURI = "/folder/folder1file.dat";
        shouldFail(originalURI);
        shouldWork(expectedNormalizedURI,expectedNormalizedURI);
    }
    
    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * invalids URIs
     */
    @Test
    public void test_uri_normalizer_invalid_uris() throws IOException, ServletException {
        // testing escaped slashes - Filter barfs and returns /
        String originalURI = "/\\///folder//important////secret_file.dat";
        shouldFail(originalURI);
        
        
    }
    


    /**
     * Test to verify the {@link NormalizationFilter} is applying properly the normalization on
     * invalids URIs
     */
    @Test
    public void test_stripping_bad_chars() throws IOException, ServletException {
        // testing escaped slashes - Filter barfs and returns /
        final String originalURI = "/folder/important/secret_file.dat";

        for(String bad : new NormalizationFilter().DISALLOWED_URI_DEFAULT) {

            final String badUrl = originalURI.replace("port", "po" + bad + "rt");

            checkEvilChars(badUrl,bad);
        }
    }


    /**
     * Test to verify the {@link NormalizationFilter} works against urls with spaces and plus signs
     */
    @Test
    public void test_allow_spaces_in_uri() throws IOException, ServletException {
        // testing escaped slashes - Filter barfs and returns /

        final String expectedURI = "/folder/important/secret%20file.dat";


        String originalURI = "/folder/important/secret+file.dat";
        shouldWork(originalURI,expectedURI);

        originalURI = "/folder/important/secret file.dat";
        shouldWork(originalURI,expectedURI);

        originalURI = "/folder/important/secret%20file.dat";
        shouldWork(originalURI,expectedURI);

    }


    @Test
    public void test_allow_utf8_in_uri() throws IOException, ServletException {
        // This should show an asset with the url of /Hellö Wörld@Java
        final String encodedUrl = "/Hell%C3%B6%20W%C3%B6rld%40Java";

        shouldWork(encodedUrl,encodedUrl);
    }





}