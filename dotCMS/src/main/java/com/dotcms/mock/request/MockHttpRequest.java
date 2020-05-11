package com.dotcms.mock.request;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
        HttpServletRequest mockReq = new BaseRequest().request();
        Mockito.when(mockReq.getRequestURI()).thenReturn(uri);
        Mockito.when(mockReq.getRequestURL()).thenReturn(new StringBuffer("http://" + hostname + uri));
        Mockito.when(mockReq.getServerName()).thenReturn(hostname);
        Mockito.when(mockReq.getRemoteAddr()).thenReturn("127.0.0.1");
        Mockito.when(mockReq.getRemoteHost()).thenReturn("127.0.0.1");
        paramMap = new HashMap<>();
        if(uri.contains("?")) {
            final String queryString = uri.substring(uri.indexOf("?") + 1, uri.length());
            Mockito.when(mockReq.getQueryString()).thenReturn(queryString);
            List<NameValuePair> additional = URLEncodedUtils.parse(queryString, Charset.forName("UTF-8"));
            for(NameValuePair nvp : additional) {
                paramMap.compute(nvp.getName(), (k, v) -> (v == null) ? new String[] {nvp.getValue()} : new String[]{nvp.getValue(),v[0]});
            }
            Mockito.when(mockReq.getQueryString()).thenReturn(queryString);
            Mockito.when(mockReq.getParameterMap()).thenReturn(paramMap);
            Mockito.when(mockReq.getParameterNames()).thenReturn(Collections.enumeration(paramMap.keySet()));
            Mockito.when(mockReq.getParameter(Mockito.anyString())).thenAnswer(new Answer<String>() {
                public String answer(InvocationOnMock invocation) {
                    Object[] args = invocation.getArguments();
                    Object mock = invocation.getMock();
                    String[] answer = paramMap.get((String)args[0]);
                    return answer!=null && answer.length>0 ? answer[0] : null;
                    }
                });
        }
        Mockito.when(mockReq.getHeader("Host")).thenReturn(hostname);
        
        request = new MockHeaderRequest(new MockSessionRequest(new MockAttributeRequest(mockReq)));
    }

    @Override
    public HttpServletRequest request() {

        return request;
    }



}
