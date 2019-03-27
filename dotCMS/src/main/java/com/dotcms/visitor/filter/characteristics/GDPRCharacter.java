package com.dotcms.visitor.filter.characteristics;

import com.dotmarketing.util.Config;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.model.User;

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



            getMap().put("ip", visitor.getIpAddress().getHostAddress());
            final Map<String, String> allCookies =
                    (request.getCookies() != null)
                            ? Arrays.asList(request.getCookies()).stream().collect(
                                    Collectors.toMap(from -> from.getName().toLowerCase(), from -> from.getValue()))
                            : new HashMap<>();

            for (String oldKey : allCookies.keySet()) {
                String val = allCookies.get(oldKey);
                if (WHITELISTED_COOKIES.contains(oldKey)) {
                    getMap().put("c." + oldKey, val);
                }
            }
            getMap().put("userId", user);
        }
    }



    private boolean hasGdprConsent(final HttpServletRequest request) {

        return (GDPR_CONSENT
                && (request.getAttribute(GDPR_CONSENT_PROPERTY) == null || (boolean) request.getAttribute(GDPR_CONSENT_PROPERTY))
                && (request.getSession().getAttribute(GDPR_CONSENT_PROPERTY) == null
                || (boolean) request.getSession().getAttribute(GDPR_CONSENT_PROPERTY)));



    }
}
