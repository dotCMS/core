package com.dotcms.mock.request;

import com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletRequest;
import java.util.Enumeration;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.nio.charset.Charset;
import java.util.Collections;
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

    private class DotCMSMockRequest extends FakeHttpServletRequest {

        private String uri;
        private StringBuffer requestURL;
        private String serverName;
        private String remoteAddr;
        private String remoteHost;
        private String queryString;
        private Map<String, String[]> paramMap;
        private Map<String, String> headers = new HashMap<>();

        @Override
        public String getRequestURI() {
            return uri;
        }

        @Override
        public StringBuffer getRequestURL() {
            return requestURL;
        }

        @Override
        public String getServerName() {
            return serverName;
        }

        @Override
        public String getRemoteAddr() {
            return remoteAddr;
        }

        @Override
        public String getRemoteHost() {
            return remoteHost;
        }

        @Override
        public String getQueryString() {
            return queryString;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return paramMap;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(paramMap.keySet());
        }

        @Override
        public String getParameter(final String name) {
            final String[] answer = paramMap.get(name);
            return answer!=null && answer.length > 0 ? answer[0] : null;
        }

        @Override
        public String getHeader(final String name) {
            return headers.get(name);
        }

        public void addHeader(final String name, final String value) {
            this.headers.put(name, value);
        }

        public void setRequestURI(final String uri) {
            this.uri = uri;
        }

        public void setRequestURL(final StringBuffer requestURL) {
            this.requestURL = requestURL;
        }

        public void setServerName(final String serverName) {
            this.serverName = serverName;
        }

        public void setRemoteAddr(final String remoteAddr) {
            this.remoteAddr = remoteAddr;
        }

        public void setRemoteHost(final String remoteHost) {
            this.remoteHost = remoteHost;
        }

        public void setQueryString(final String queryString) {
            this.queryString = queryString;
        }

        public void setParameterMap(final Map<String, String[]> paramMap) {
            this.paramMap = paramMap;
        }
    }

}
