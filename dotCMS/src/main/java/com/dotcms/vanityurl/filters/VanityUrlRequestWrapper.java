package com.dotcms.vanityurl.filters;

import static com.dotmarketing.filters.Constants.CMS_FILTER_QUERY_STRING_OVERRIDE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import com.dotcms.vanityurl.model.VanityUrlResult;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;


/**
 * The VanityUrlOverrideRequest merges the parameters set in the original request and merges them
 * with the parameters that are being set in the vanityUrl. In all cases, if there are parameters
 * set in the VanityURL Query String, they will override the ones being sent in by original visitors
 * request
 *
 */
public class VanityUrlRequestWrapper extends HttpServletRequestWrapper {
    
    final Map<String, String[]> queryParamMap;
    final String newQueryString;
    final boolean vanityHasQueryString;


    public VanityUrlRequestWrapper(HttpServletRequest request, VanityUrlResult vanityUrlResult) {
        super(request);

        
        this.vanityHasQueryString = UtilMethods.isSet(vanityUrlResult.getQueryString());
        
        this.newQueryString = vanityHasQueryString && UtilMethods.isSet(request.getQueryString())
                        ? request.getQueryString() + "&" + vanityUrlResult.getQueryString()
                        : vanityHasQueryString 
                            ? vanityUrlResult.getQueryString()
                            : request.getQueryString();


        // we create a new map here because it merges the 
        Map<String,String[]> tempMap = new HashMap<>(request.getParameterMap());
        if(vanityHasQueryString) {
            List<NameValuePair> additional = URLEncodedUtils.parse(newQueryString, Charset.forName("UTF-8"));
            for(NameValuePair nvp : additional) {
                tempMap.compute(nvp.getName(), (k, v) -> (v == null) ? new String[] {nvp.getValue()} : new String[]{nvp.getValue(),v[0]});
            }
        }
        

        this.queryParamMap = ImmutableMap.copyOf(tempMap);



        this.setAttribute(CMS_FILTER_URI_OVERRIDE, vanityUrlResult.getRewrite());
        this.setAttribute(CMS_FILTER_QUERY_STRING_OVERRIDE, this.newQueryString);

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

}
