package com.dotcms.analytics.track;

import com.dotcms.analytics.track.matchers.FilesRequestMatcher;
import com.dotcms.analytics.track.matchers.PagesAndUrlMapsRequestMatcher;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.analytics.track.matchers.RulesRedirectsRequestMatcher;
import com.dotcms.analytics.track.matchers.VanitiesRequestMatcher;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.DotCMSMockResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotcms.visitor.filter.characteristics.Character;
import com.dotcms.visitor.filter.characteristics.CharacterWebAPI;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.filters.Constants;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for RequestMatcher
 */
@Ignore("Data Collectors have been disabled in favor of creating events via REST")
public class RequestMatcherTest {

    @BeforeClass
    public static void beforeClass() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Will use a default implementation of the method (without implementing anything)
     * ExpectedResult: Any of the matches wont work, so the method will return false
     */
    @Test
    public void test_default_implementation() throws Exception {

        final RequestMatcher requestMatcher = new RequestMatcher() {
            // empty implementation
        };
        final FakeHttpRequest request = new FakeHttpRequest("localhost", "/test");
        final boolean result = requestMatcher.match(request.request(), new MockHttpResponse().response());
        assertFalse(result);
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Creates a matcher for the exact uri path and the GET method
     * ExpectedResult: Since both criteria are met, the method will return true
     */
    @Test
    public void test_get_method_with_valid_exact_match() throws Exception {

        final String url = "/test";
        final RequestMatcher requestMatcher = new RequestMatcher() {

            @Override
            public boolean runBeforeRequest() {
                return true;
            }

            @Override
            public Set<String> getMatcherPatterns() {
                return Set.of(url);
            }

            @Override
            public Set<String> getAllowedMethods() {
                return Set.of(HttpMethod.GET);
            }
        };

        final DotCMSMockRequest mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/api/v1/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost" + url));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.GET);
        final boolean result = requestMatcher.match(mockReq, new MockHttpResponse().response());
        assertTrue(result);
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Creates a matcher for the wildcard and the GET/post method
     * ExpectedResult: Since both criteria are met, the method will return true
     */
    @Test
    public void test_get_and_post_method_with_wildcard_match() throws Exception {

        final String url = "/test*";
        final RequestMatcher requestMatcher = new RequestMatcher() {

            @Override
            public boolean runBeforeRequest() {
                return true;
            }

            @Override
            public Set<String> getMatcherPatterns() {
                return Set.of(url);
            }

            @Override
            public Set<String> getAllowedMethods() {
                return Set.of(HttpMethod.GET, HttpMethod.POST);
            }
        };

        DotCMSMockRequest mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/api/v1/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost/api/v1/test"));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.GET);
        boolean result = requestMatcher.match(mockReq, new MockHttpResponse().response());
        assertTrue(result);

        mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/dA/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost/dA/test"));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.POST);
        result = requestMatcher.match(mockReq, new MockHttpResponse().response());
        assertTrue(result);
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Creates a matcher to identify pages
     * ExpectedResult: Since sending a page will return true
     */
    @Test
    public void test_get_method_with_pages_matcher() throws Exception {

        final CharacterWebAPI characterWebAPI = new CharacterWebAPI() {
            @Override
            public Character getOrCreateCharacter(HttpServletRequest request, HttpServletResponse response) {


                return new Character() {
                    @Override
                    public Map<String, Serializable> getMap() {
                        return Map.of("iAm", CMSFilter.IAm.PAGE);
                    }

                    @Override
                    public void clearMap() {

                    }
                };
            }

            @Override
            public Optional<Character> getCharacterIfExist(HttpServletRequest request, HttpServletResponse response) {
                return Optional.empty();
            }
        };

        final RequestMatcher requestMatcher = new PagesAndUrlMapsRequestMatcher(characterWebAPI);

        final DotCMSMockRequest mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/site/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost/site/test"));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.GET);
        final boolean result = requestMatcher.match(mockReq, new MockHttpResponse().response());
        assertTrue(result);
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Creates a matcher to identify files
     * ExpectedResult: Since sending a file will return true
     */
    @Test
    public void test_get_method_with_file_matcher() throws Exception {

        final CharacterWebAPI characterWebAPI = new CharacterWebAPI() {
            @Override
            public Character getOrCreateCharacter(HttpServletRequest request, HttpServletResponse response) {


                return new Character() {
                    @Override
                    public Map<String, Serializable> getMap() {
                        return Map.of("iAm", CMSFilter.IAm.FILE);
                    }

                    @Override
                    public void clearMap() {

                    }
                };
            }

            @Override
            public Optional<Character> getCharacterIfExist(HttpServletRequest request, HttpServletResponse response) {
                return Optional.empty();
            }
        };

        final RequestMatcher requestMatcher = new FilesRequestMatcher(characterWebAPI);

        final DotCMSMockRequest mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/dA/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost/dA/test"));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.GET);
        final boolean result = requestMatcher.match(mockReq, new MockHttpResponse().response());
        assertTrue(result);
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Creates a matcher to identify a rules redirection
     * ExpectedResult: Since sending a rules redirection will return true
     */
    @Test
    public void test_get_method_with_rules_matcher() throws Exception {

        final RequestMatcher requestMatcher = new RulesRedirectsRequestMatcher();

        final DotCMSMockRequest mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/dA/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost/dA/test"));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.GET);


        final DotCMSMockResponse response = new DotCMSMockResponse();

        response.addHeader("X-DOT-SendRedirectRuleAction", "true");
        final boolean result = requestMatcher.match(mockReq, response);
        assertTrue(result);
    }

    /**
     * Method to test: RequestMatcher.match(HttpServletRequest request)
     * Given Scenario: Creates a matcher to identify a vanities redirection
     * ExpectedResult: Since sending a vanities redirection will return true
     */
    @Test
    public void test_get_method_with_vanities_matcher() throws Exception {

        final RequestMatcher requestMatcher = new VanitiesRequestMatcher();

        final DotCMSMockRequest mockReq = new DotCMSMockRequest();

        mockReq.setRequestURI("/dA/test");
        mockReq.setRequestURL(new StringBuffer("http://localhost/dA/test"));
        mockReq.setServerName("http://localhost");
        mockReq.setMethod(HttpMethod.GET);
        final DefaultVanityUrl defaultVanityUrl = new DefaultVanityUrl();
        defaultVanityUrl.setForwardTo("/test");
        mockReq.setAttribute(Constants.VANITY_URL_OBJECT, defaultVanityUrl);


        final DotCMSMockResponse response = new DotCMSMockResponse();

        final boolean result = requestMatcher.match(mockReq, response);
        assertTrue(result);
    }

}
