package com.dotcms.visitor.filter.characteristics;


import com.dotcms.util.SimplePatternMatcher;
import com.dotmarketing.util.Config;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ParamsCharacter extends AbstractCharacter {


    /**
     * Collect all params by default
     */
    private static final SimplePatternMatcher matcher = new SimplePatternMatcher(
                    Arrays.asList(Config.getStringProperty("VISITOR_WHITELISTED_PARAMS", ".*").toLowerCase().split(",")));

    public ParamsCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);


        final Map<String, Serializable> params = new HashMap<>();
        final String queryString = (request.getQueryString() != null) ? request.getQueryString().replaceAll("&", " ") : null;
        for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
            String nextParamName = e.nextElement().toLowerCase();
            if (matcher.isMatch(nextParamName)) {
                params.put("p." + nextParamName.toLowerCase(), request.getParameter(nextParamName));
            }
        }
        accrueAll(params);
        accrue("queryString", queryString);

    }

}
