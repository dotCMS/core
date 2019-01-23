package com.dotcms.rest.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dotcms.repackage.javax.ws.rs.container.ContainerRequestContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseFilter;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableList;


/**
 * @author Geoff M. Granum
 */
public class CorsFilter implements ContainerResponseFilter {


    final private static String CORS_PREFIX = "api.cors";
    final private static String CORS_DEFAULT = "default";
    final private static Map<String, List<String[]>> headerMap = new ConcurrentHashMap<>();


    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();
        final String resource = requestContext.getUriInfo().getMatchedResources().get(0).getClass().getSimpleName().toLowerCase();
        getHeaders(resource).forEach(header-> responseHeaders.add(header[0], header[1]));
        
    }


    protected List<String[]> getHeaders(final String mapping) {
        List<String[]> corsHeaders = headerMap.get(mapping);
        if (null == corsHeaders) {
            synchronized (this.getClass()) {
                corsHeaders = headerMap.get(mapping);
                if (null == corsHeaders) {
                    final List<String[]> newHeaders = new ArrayList<>();
                    Config.subset(CORS_PREFIX + "." + mapping).forEachRemaining(key -> {
                        newHeaders.add(new String[] {
                                fixHeaderCase(key.replace(CORS_PREFIX, "")), 
                                Config.getStringProperty(CORS_PREFIX + "." + mapping + "." + key, null)
                            });
                    });
                    corsHeaders = ImmutableList.copyOf(newHeaders);
                    headerMap.put(mapping, corsHeaders);
                }
            }
        }
        if (corsHeaders.isEmpty() && !CORS_DEFAULT.equals(mapping)) {
            return getHeaders(CORS_DEFAULT);
        }
        return corsHeaders;

    }


    protected final String fixHeaderCase(final String propertyName) {

        final StringWriter sw = new StringWriter();
        boolean upperCaseNextChar = true;
        for (final char c : propertyName.toCharArray()) {
            if(c==' ') {
                upperCaseNextChar = true;
            }
            else if (c == '-') {
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

