package com.dotcms.mock.request;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;



/**
 * Mocks a full featured request to a specific host / resource.
 * Now supports queryparams
 * <pre>
 * {@code
 * HttpServletRequest requestProxy = new MockHttpRequest(host.getHostname(), uri).request();
 * }
 * </pre>
 *
 */
public class MockHttpRequest implements MockRequest {

    private final HttpServletRequest request;
    private final Map<String,String[]> paramMap;
    public MockHttpRequest(final String incomingHostname, final String incomingUri) {
        
        final String uri = UtilMethods.isSet(incomingUri) ? incomingUri : StringPool.FORWARD_SLASH;
        final String hostname = UtilMethods.isSet(incomingHostname) ? incomingHostname : "localhost";
        DotCMSMockRequest mockReq = new DotCMSMockRequest();
        mockReq.setRequestURI(uri);
        mockReq.setRequestURL(new StringBuffer("http://" + hostname + uri));
        mockReq.setServerName(hostname);
        mockReq.setRemoteAddr("127.0.0.1");
        mockReq.setRemoteHost("127.0.0.1");

        paramMap = new HashMap<>();
        if(uri.contains("?")) {
            final String queryString = uri.substring(uri.indexOf("?") + 1, uri.length());

            List<NameValuePair> additional = URLEncodedUtils.parse(queryString, Charset.forName("UTF-8"));
            for(NameValuePair nvp : additional) {
                paramMap.compute(nvp.getName(), (k, v) -> (v == null) ? new String[] {nvp.getValue()} : new String[]{nvp.getValue(),v[0]});
            }

            mockReq.setQueryString(queryString);
            mockReq.setParameterMap(paramMap);
        }
        mockReq.addHeader("Host", hostname);
        
        request = new MockHeaderRequest(new MockSessionRequest(new MockAttributeRequest(mockReq)));
    }

    @Override
    public HttpServletRequest request() {

        return request;
    }

}
