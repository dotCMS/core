package com.dotmarketing.filters;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.google.common.collect.ImmutableMap;

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

        final Map<String, String> formParameters = ImmutableMap.of("param1", FORM, "param2", FORM);



        final HttpServletRequest baseRequest = new MockParameterRequest(
                        new MockHttpRequest("testing", "/test?param1=" + URL + "&param2=" + URL).request(), formParameters)
                                        .request();



        final VanityUrlResult vanityUrlResult = new VanityUrlResult("/newUrl", "param2=" + VANITY + "&param3=" + VANITY, false);


        final HttpServletRequest request = new VanityUrlRequestWrapper(baseRequest, vanityUrlResult);



        // we have 3 objects in our param map
        assert (request.getParameterMap().size() == 3);


        assert (request.getParameter("param1").equals(URL));

        // param2 have been overridden by the vanity url
        assert (request.getParameter("param2").equals(VANITY));

        assert (request.getParameter("param3").equals(VANITY));

        // param2 also has the original value from the url
        assert (request.getParameterValues("param2").length == 2);
        assert (request.getParameterValues("param2")[0].equals(VANITY));
        assert (request.getParameterValues("param2")[1].equals(URL));



    }



}
