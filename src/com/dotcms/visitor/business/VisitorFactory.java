package com.dotcms.visitor.business;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.logging.log4j.util.Strings;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import eu.bitwalker.useragentutils.UserAgent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public class VisitorFactory {

    private static final VisitorFactory INSTANCE = new VisitorFactory();

    private VisitorFactory() {}

    public static VisitorFactory getInstance() {
        return INSTANCE;
    }

    private LanguageAPI languageAPI = APILocator.getLanguageAPI();

    public void setLanguageAPI(LanguageAPI languageAPI) {
        this.languageAPI = languageAPI;
    }

    public Visitor createVisitor(HttpServletRequest request, UUID dmid, boolean newVisitor) {

        // if session hasn't been created, create it
        HttpSession session = request.getSession();

        InetAddress ipAddress = lookupIPAddress(request);

        long selectedLanguageId = lookupSelectedLanguage(session);

        Locale locale = lookupLocale(selectedLanguageId);

        UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));

        LocalDateTime now = LocalDateTime.now();

        URI initialReferrer = lookupReferrer(request);

        return null;
    }

    private Visitor createVisitor() {

        return null;
    }

    private long lookupSelectedLanguage(HttpSession session) {
        long selectedLanguage = languageAPI.getDefaultLanguage().getId();
        String sessionLanguageStr = (String)session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);

        if(Strings.isNotBlank((sessionLanguageStr))) {
            selectedLanguage = Long.parseLong(sessionLanguageStr);
        }

        return selectedLanguage;
    }

    private InetAddress lookupIPAddress(HttpServletRequest request) {

        InetAddress address = null;
        try {
            address = HttpRequestDataUtil.getIpAddress(request);
        } catch(UnknownHostException e) {
            Logger.error(VisitorFactory.class, "Could not get the IP Address from the request", e);
        }

        return address;
    }

    private Locale lookupLocale(long selectedLanguageId) {
        Language currentLang = languageAPI.getLanguage(selectedLanguageId);
        return new Locale(currentLang.getLanguageCode(), currentLang.getCountryCode());
    }

    private URI lookupReferrer(HttpServletRequest request) {
        URI referrer = null;
        try {
            String referrerStr = request.getHeader("Referer");
            if(Strings.isNotBlank(referrerStr)) {
                referrer = new URI(referrerStr);
            }
        } catch(URISyntaxException e) {
            Logger.error(VisitorFactory.class, "Invalid Referrer sent in request", e);
        }

        return referrer;
    }


}
