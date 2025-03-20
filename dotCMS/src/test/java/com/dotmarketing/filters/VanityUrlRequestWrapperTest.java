package com.dotmarketing.filters;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.Test;
import com.dotcms.mock.request.MockHttpRequestUnitTest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.vanityurl.filters.VanityUrlRequestWrapper;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;

public class VanityUrlRequestWrapperTest {

    final String URL = "URL";
    final String FORM = "FORM";
    final String VANITY = "VANITY";


    /**
     * This tests if you have a vanity URL that has query parameters, that they will be merged into to
     * the request and if their keys match an existing parameter, the vanity url parameter will
     * overwrite any parameters that were in the original request
     * 
     * see: https://github.com/dotCMS/core/issues/18325
     */
    @Test
    public void test_that_vanity_urls_with_query_params_override_incoming_requests() {

        // three form params
        final Map<String, String> formParameters = ImmutableMap.of("param0", FORM, "param1", FORM, "param2", FORM);


        // two url params (2 overlap)
        final HttpServletRequest baseRequest = new MockParameterRequest(
                        new MockHttpRequestUnitTest("testing", "/test?param1=" + URL + "&param2=" + URL).request(), formParameters)
                                        .request();

        // two vanity params (1 overlaps)
        final VanityUrlResult vanityUrlResult = new VanityUrlResult("/newUrl", "param2=" + VANITY + "&param3=" + VANITY, false);


        final HttpServletRequest request = new VanityUrlRequestWrapper(baseRequest, vanityUrlResult);



        // we have 3 objects in our param map
        assert (request.getParameterMap().size() == 4);

        assert (request.getParameter("param0").equals(FORM));
        assert (request.getParameter("param1").equals(URL));

        // param2 have been overridden by the vanity url
        assert (request.getParameter("param2").equals(VANITY));

        assert (request.getParameter("param3").equals(VANITY));

        // param2 also has the original value from the url
        assert (request.getParameterValues("param2").length == 2);
        assert (request.getParameterValues("param2")[0].equals(VANITY));
        assert (request.getParameterValues("param2")[1].equals(URL));



    }

    /**
     * This tests to insure that the new query string has both the old and the new query parameters
     * 
     * see: https://github.com/dotCMS/core/issues/18325
     */
    @Test
    public void test_that_query_string_has_all_the_parameters() {


        final HttpServletRequest baseRequest = new MockHttpRequestUnitTest("testing", "/test?param1=" + URL + "&param2=" + URL).request();

        final VanityUrlResult vanityUrlResult = new VanityUrlResult("/newUrl", "param2=" + VANITY + "&param3=" + VANITY, false);

        final HttpServletRequest request = new VanityUrlRequestWrapper(baseRequest, vanityUrlResult);

        final String queryString= request.getQueryString();
        assert(queryString!=null);
        assert(!queryString.startsWith("&"));
        assert(!queryString.endsWith("&"));
        assert(queryString.contains("param1=" + URL));
        assert(queryString.contains("param2=" + URL));
        assert(queryString.contains("param2=" + VANITY));
        assert(queryString.contains("param3=" + VANITY));
        List<NameValuePair> queryParams = URLEncodedUtils.parse(queryString, Charset.forName("UTF-8"));
        assert(queryParams.size()==4);
        

    
        
    }


    @Test
    public void test_that_vanityUrlParams_requestParams_Are_Same_Should_Not_Be_Duped() {


        final HttpServletRequest baseRequest = new MockHttpRequestUnitTest("testing", "/test?param1=" + URL + "&param2=" + URL).request();

        final VanityUrlResult vanityUrlResult = new VanityUrlResult("/newUrl", "param1=" + URL + "&param2=" + URL, false);

        final HttpServletRequest request = new VanityUrlRequestWrapper(baseRequest, vanityUrlResult);

        final String queryString= request.getQueryString();
        assert(queryString!=null);
        assert(!queryString.startsWith("&"));
        assert(!queryString.endsWith("&"));
        assert(queryString.contains("param1=" + URL));
        assert(queryString.contains("param2=" + URL));
        List<NameValuePair> queryParams = URLEncodedUtils.parse(queryString, Charset.forName("UTF-8"));
        assertEquals("Should be only 2 params since all are the same. Params: " + queryParams,2,queryParams.size());




    }
}
