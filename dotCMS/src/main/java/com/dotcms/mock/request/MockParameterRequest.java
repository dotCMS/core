package com.dotcms.mock.request;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;

/**
 * Mock Request Parameter using a Request Wrapper. Part of the work to be
 * able to run the integration tests without the web app container i.e. Tomcat.
 */
public class MockParameterRequest extends HttpServletRequestWrapper implements MockRequest {
    final Map<String, String> params;

    public MockParameterRequest(HttpServletRequest request) {
        this(request, ImmutableMap.of());
    }

    public MockParameterRequest(HttpServletRequest request, Map<String, String> setMe) {
        super(request);
        HashMap<String, String> mutable = new HashMap<String, String>();
        
        List<NameValuePair> additional = URLEncodedUtils.parse(request.getQueryString(), Charset.forName("UTF-8"));
        for(NameValuePair nvp : additional) {
            mutable.put(nvp.getName(),nvp.getValue());
        }
        
        
        Enumeration<String> parameters = request.getParameterNames();
        while (parameters != null && parameters.hasMoreElements()) {
            String key = parameters.nextElement();
            mutable.put(key, request.getParameter(key));
        }
        mutable.putAll(setMe);

        params = ImmutableMap.copyOf(mutable);
    }

    public HttpServletRequest request() {
        return this;
    }

    @Override
    public String getParameter(String name) {
        return params.get(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new Vector<String>(params.keySet()).elements();
    }
    @Override
    public Map<String,String[]> getParameterMap() {
        return params.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                                  e -> new String[]{e.getValue()}));
        

    }
}
