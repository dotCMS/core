package com.dotcms.vanityurl.filters;

import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.filters.Constants.CMS_FILTER_QUERY_STRING_OVERRIDE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;

/**
 * The VanityUrlOverrideRequest merges the parameters set in the original request and merges them
 * with the parameters that are being set in the vanityUrl. In all cases, if there are parameters
 * set in the VanityURL Query String, they will override the ones being sent in by original visitors
 * request
 *
 */
public class VanityUrlRequestWrapper extends HttpServletRequestWrapper {
    
    private final Map<String, String[]> queryParamMap;
    private final String newQueryString;
    private final int responseCode;

    public VanityUrlRequestWrapper(final HttpServletRequest request, final VanityUrlResult vanityUrlResult) {
        super(request);

        final boolean vanityHasQueryString = UtilMethods.isSet(vanityUrlResult.getQueryString());

        final StringBuilder params = new StringBuilder();
        params.append(UtilMethods.isSet(request.getQueryString()) ? request.getQueryString() : StringPool.BLANK);
        final Map<String, String> vanityParams = convertURLParamsStringToMap(vanityUrlResult.getQueryString());
        final Map<String, String> requestParams = convertURLParamsStringToMap(request.getQueryString());
        if(vanityHasQueryString){
            for (final Map.Entry<String,String> entry : vanityParams.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                //add to the request.getQueryString() the vanity parameters that are not already present, the key and value must not be the same
                if(!requestParams.containsKey(key) || !requestParams.get(key).equals(value)){
                    params.append(StringPool.AMPERSAND).append(key).append(StringPool.EQUAL).append(value);
                }
            }
        }
        this.newQueryString = params.toString();
        // we create a new map here because it merges the
        final Map<String,String[]> tempMap = new HashMap<>(request.getParameterMap());
        if(vanityHasQueryString) {
            final List<NameValuePair> additional = URLEncodedUtils.parse(newQueryString, StandardCharsets.UTF_8);
            for (final NameValuePair nvp : additional) {
                tempMap.compute(nvp.getName(), (k, v) -> (v == null) ? new String[] {nvp.getValue()} : new String[]{nvp.getValue(),v[0]});
            }
        }
        this.queryParamMap = ImmutableMap.copyOf(tempMap);

        this.responseCode = vanityUrlResult.getResponseCode();
        request.setAttribute(CMS_FILTER_URI_OVERRIDE, vanityUrlResult.getRewrite());
        request.setAttribute(CMS_FILTER_QUERY_STRING_OVERRIDE, this.newQueryString);
        this.setAttribute(CMS_FILTER_URI_OVERRIDE, vanityUrlResult.getRewrite());
        this.setAttribute(CMS_FILTER_QUERY_STRING_OVERRIDE, this.newQueryString);
    }

    /**
     * Converts a URL parameters string to a map of key-value pairs
     * @param input URL parameters string
     * @return Map of key-value pairs
     */
    private Map<String, String> convertURLParamsStringToMap(final String input) {
        final Map<String, String> map = new HashMap<>();

        if(UtilMethods.isSet(input)) {
            // Split the input string by '&' to get key-value pairs
            final String[] pairs = input.split("&");

            for (final String pair : pairs) {
                // Split each pair by '=' to get the key and value
                final String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    map.put(keyValue[0], keyValue[1]);
                } else if (keyValue.length == 1) {
                    map.put(keyValue[0], ""); // Handle case where there is a key with no value
                }
            }
        }

        return map;
    }

    @Override
    public String getQueryString() {
        return this.newQueryString;
    }

    @Override
    public String getParameter(final String name) {
        String[] val = this.queryParamMap.get(name);
        return val != null && val.length > 0 ? val[0] : null;
    }


    /**
     * needs to use a hashmap so the entries get de-duped. ImmutableMap throws an error if two entrys
     * with the same key are put into it.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return this.queryParamMap;
    }
    


    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(this.queryParamMap.keySet());

    }

    @Override
    public String[] getParameterValues(String name) {
        return  this.queryParamMap.get(name);
       
    }

    /**
     * response code used to build the Vanity URL.
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }
}
