package com.dotcms.visitor.filter.characteristics;

import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class GDPRCharacter extends AbstractCharacter {

    private final String GDPR_CONSENT_PROPERTY = "GDPR_CONSENT";
    private final String GDPR_CONSENT_DEFAULT_PROPERTY = "GDPR_CONSENT_DEFAULT_PROPERTY";
    private final boolean GDPR_CONSENT;
    private final static Set<String> WHITELISTED_COOKIES =
            new HashSet<>(Arrays.asList(
                    Config.getStringProperty("WHITELISTED_COOKIES", "").toLowerCase().split(",")));

    public GDPRCharacter(AbstractCharacter incomingCharacter) {

        super(incomingCharacter);
        this.GDPR_CONSENT = Config.getBooleanProperty(GDPR_CONSENT_DEFAULT_PROPERTY, false);
        if (hasGdprConsent(request)) {

            String user = (request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_USER) != null)
                    ? ((User) request.getSession().getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).getUserId()
                    : null;



            getMap().put("ip", request.getRemoteAddr());
            
            
            
            
            final Map<String, String> allCookies =new HashMap<>();
            for(Cookie c : request.getCookies()) {
                if (WHITELISTED_COOKIES.contains(c.getName())) {
                    String cookieName = (c.getDomain()!=null)?c.getDomain() + ":" : "";
                    cookieName+= (c.getPath()!=null)?c.getPath() + ":" : "";
                    cookieName+= c.getName();
                    allCookies.put(cookieName, c.getValue());
                }
            }
            

            accrue("userId", user);
        }
    }



    private boolean hasGdprConsent(final HttpServletRequest request) {

        return (GDPR_CONSENT
                && (request.getAttribute(GDPR_CONSENT_PROPERTY) == null || (boolean) request.getAttribute(GDPR_CONSENT_PROPERTY))
                && (request.getSession().getAttribute(GDPR_CONSENT_PROPERTY) == null
                || (boolean) request.getSession().getAttribute(GDPR_CONSENT_PROPERTY)));



    }
}
