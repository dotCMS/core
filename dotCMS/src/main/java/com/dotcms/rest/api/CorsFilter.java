package com.dotcms.rest.api;

import io.vavr.Lazy;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import javax.ws.rs.ext.Provider;


/**
 * @author Geoff M. Granum
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {


    final public static String CORS_PREFIX = "api.cors";
    final public static String CORS_DEFAULT = "default";
    final private Lazy<Map<String, List<String[]>>> headerMap = Lazy.of(this::loadHeaders);

    Map<String, List<String[]>> loadHeaders() {
        Map<String, List<String[]>> loadingMap  = new HashMap<>();
        final List<String> props = Config.subsetContainsAsList(CORS_PREFIX);
        props.forEach(key -> {
            final String convertedKeyToEnvKey = Config.envKey(key);
            final String[] splitter = convertedKeyToEnvKey.split("_", 5);
            final String mapping = splitter[3].toLowerCase();
            final String header = fixHeaderCase(splitter[4]);
            List<String[]> keys = loadingMap.getOrDefault(mapping,  new ArrayList<>());

            keys.add(new String[] {header, Config.getStringProperty(key, "")});

            loadingMap.put(mapping, keys);

        });
        return ImmutableMap.copyOf(loadingMap);
    }






    
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        String resource = "unknown";
        try{
            resource = requestContext.getUriInfo().getMatchedResources().get(0).getClass().getSimpleName().toLowerCase();
        }catch(Exception e) {
            Logger.debug(this.getClass(), () -> e.getMessage());
        }
        getHeaders(resource)
            .stream()
            .forEach(entry-> {
                List<Object> vals = new ArrayList<>();
                vals.add(entry[1]);
                headers.putIfAbsent(entry[0], vals);
            });
            

    }


    protected List<String[]> getHeaders(final String mapping) {
        List<String[]> corsHeaders = headerMap.get().containsKey(mapping) ? headerMap.get().get(mapping) : headerMap.get().get(CORS_DEFAULT);
        return corsHeaders != null ? corsHeaders : ImmutableList.of() ;

    }


    protected final String fixHeaderCase(String propertyName) {

        propertyName = propertyName.toLowerCase().replace("_","-");
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
