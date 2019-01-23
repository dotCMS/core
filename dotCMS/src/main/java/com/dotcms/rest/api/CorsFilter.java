package com.dotcms.rest.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import com.dotcms.repackage.javax.ws.rs.container.ContainerRequestContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseFilter;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;
import com.dotmarketing.util.Config;


/**
 * @author Geoff M. Granum
 */
public class CorsFilter implements ContainerResponseFilter {


    final private static String CORS_PREFIX = "api.cors";
    final private static String CORS_DEFAULT = "default";
    final private static Map<String, List<String[]>> headerMap = new ConcurrentHashMap();


    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        final String resource = requestContext.getUriInfo().getMatchedResources().get(0).getClass().getSimpleName().toLowerCase();
        final List<String[]> corsHeaders = getHeaders(resource);
        for (final String[] corsHeader : corsHeaders) {
            if (!headers.containsKey(corsHeader[0])) {
                headers.add(corsHeader[0], corsHeader[1]);
            }
        }
    }


    private List<String[]> getHeaders(final String mapping) {
        List<String[]> corsHeaders = headerMap.get(mapping);
        if (null == corsHeaders) {
            synchronized (this.getClass()) {
                corsHeaders = headerMap.get(mapping);
                if (null == corsHeaders) {
                    corsHeaders = new ArrayList<>();
                    final Iterator<String> keys = Config.subset(CORS_PREFIX + "." + mapping);
                    while (keys.hasNext()) {
                        final String key = keys.next();
                        final String value = Config.getStringProperty(CORS_PREFIX + "." + mapping + "." + key, "");
                        final String headerName = fixHeaderCase(key.replace(CORS_PREFIX, ""));
                        corsHeaders.add(new String[] {headerName, value});
                    }
                    headerMap.put(mapping, corsHeaders);
                }
            }
        }
        if (corsHeaders.isEmpty() && !CORS_DEFAULT.equals(mapping)) {
            return getHeaders(CORS_DEFAULT);
        }
        return corsHeaders;

    }


    private final String fixHeaderCase(final String propertyName) {

        StringWriter sw = new StringWriter();
        boolean upperCaseNextChar = true;
        for (char c : propertyName.toCharArray()) {
            if (c == '-') {
                sw.append(c);
                upperCaseNextChar = true;
            } else {
                sw.append(upperCaseNextChar ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upperCaseNextChar = false;
            }
        }
        return sw.toString();

    }


}

