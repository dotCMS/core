package com.dotcms.visitor.filter.characteristics;

import com.dotcms.clickhouse.util.SimplePatternMatcher;
import com.dotmarketing.util.Config;
import java.util.Arrays;
import javax.servlet.http.Cookie;

public class CookieCharacter extends AbstractCharacter {


    private static final SimplePatternMatcher matcher = new SimplePatternMatcher(Arrays.asList(Config.getStringProperty(
                    "VISITOR_WHITELISTED_COOKIES",
                    "_ga,__atuvc,_gid,opvc,sitevisitscookie,__utmz,hubspotutk,gwcc,__utmz,__hstc,__utma,__utmc,")
                    .toLowerCase().split(",")));


    public CookieCharacter(AbstractCharacter incomingCharacter) {
        super(incomingCharacter);


        for (Cookie cookie : request.getCookies()) {
            String cookieName = cookie.getName().toLowerCase();
            if (matcher.isMatch(cookieName)) {
                accrue("c." + cookieName, cookie.getValue());
            }
        }

    }

}
