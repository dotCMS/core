package com.dotcms.rest.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    final List<String[]> corsHeaders;
    
    final static String CORS_PREFIX="api.cors.headers"; 
    
    public CorsFilter() {
        List<String[]> _headers = new ArrayList<>();
        Iterator<String> keys = Config.subset(CORS_PREFIX);
        while(keys.hasNext()){
            final String key = keys.next();
            String value = Config.getStringProperty(CORS_PREFIX + "." + key, "");
            final String headerName = fixCase(key.replace(CORS_PREFIX, ""));
            _headers.add(new String[]{headerName, value});
        }
        this.corsHeaders=ImmutableList.copyOf(_headers);
        

    }
    
    
    
    
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        for(String[] corsHeader: corsHeaders) {
            if(!headers.containsKey(corsHeader[0])) {
                headers.add(corsHeader[0], corsHeader[1]);
            }
        }

    }
    
    
    private final String fixCase(final String propertyName) {
        
        StringWriter sw = new StringWriter();
        boolean upperCaseNextChar = true;
        for(char c : propertyName.toCharArray()) {
            if(c=='-') {
                sw.append(c);
                upperCaseNextChar=true;
            }else {
                sw.append(upperCaseNextChar ? Character.toUpperCase(c) :  Character.toLowerCase(c));
                upperCaseNextChar=false;
            }
        }
        return sw.toString();
        
        
        
    }
    
    
    
    
}
 
