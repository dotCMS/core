package com.dotcms.visitor.filter.characteristics;

import com.dotcms.clickhouse.util.SimplePatternMatcher;
import com.dotmarketing.util.Config;
import java.util.Arrays;
import java.util.Enumeration;

public class HeaderCharacter extends AbstractCharacter {



    private static final SimplePatternMatcher matcher = new SimplePatternMatcher(Arrays.asList(
                    Config.getStringProperty("VISITOR_WHITELISTED_HEADERS", "User-Agent,Accept-Languag,Host,DNT").toLowerCase().split(",")));

    
    
    
    public HeaderCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);


        for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
            String nextHeaderName = e.nextElement().toLowerCase();
            if (matcher.isMatch(nextHeaderName)) {
                accrue("h." + nextHeaderName, request.getHeader(nextHeaderName));
            }
        }

    }

}
