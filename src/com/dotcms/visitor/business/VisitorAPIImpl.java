package com.dotcms.visitor.business;

import com.dotcms.repackage.org.apache.logging.log4j.util.Strings;
import com.dotcms.rest.validation.Preconditions;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import eu.bitwalker.useragentutils.UserAgent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class VisitorAPIImpl implements VisitorAPI {

    private LanguageWebAPI languageWebAPI = WebAPILocator.getLanguageWebAPI();

    @Override
    public void setLanguageWebAPI(LanguageWebAPI languageWebAPI) {
        this.languageWebAPI = languageWebAPI;
    }

    @Override
    public Optional<Visitor> getVisitor(HttpServletRequest request) {
        return getVisitor(request, Config.getBooleanProperty("CREATE_VISITOR_OBJECT_IN_SESSION", true));
    }

    @Override
    public Optional<Visitor> getVisitor(HttpServletRequest request, boolean create) {

        Preconditions.checkNotNull(request, IllegalArgumentException.class, "Null Request");

        Optional<Visitor> visitorOpt;

        if(!create) {
            HttpSession session = request.getSession(false);

            if(Objects.isNull(session)) {
                visitorOpt = Optional.empty();
            } else {
                visitorOpt = Optional.ofNullable((Visitor) session.getAttribute(WebKeys.VISITOR));
            }

        } else {
            // lets create a session if not already created
            HttpSession session = request.getSession();
            Visitor visitor = (Visitor) session.getAttribute(WebKeys.VISITOR);

            if(Objects.isNull(visitor)) {
                visitor = createVisitor(request);
                session.setAttribute(WebKeys.VISITOR, visitor);
            }

            visitorOpt = Optional.of(visitor);
        }

        return visitorOpt;
    }

    private Visitor createVisitor(HttpServletRequest request) {

        InetAddress ipAddress = lookupIPAddress(request);

        Language selectedLanguage = languageWebAPI.getLanguage(request);

        Locale locale = new Locale(selectedLanguage.getLanguageCode(), selectedLanguage.getCountryCode());

        UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));

        UUID dmid = lookupDMID(request);

        boolean isNewVisitor = isNewVisitor(request);

        LocalDateTime now = LocalDateTime.now();

        URI initialReferrer = lookupReferrer(request);

        Visitor visitor = new Visitor();
        visitor.setIpAddress(ipAddress);
        visitor.setSelectedLanguage(selectedLanguage);
        visitor.setLocale(locale);
        visitor.setUserAgent(userAgent);
        visitor.setDmid(dmid);
        visitor.setNewVisitor(isNewVisitor);
        visitor.setReferrer(initialReferrer);

        return  visitor;

    }

    private boolean isNewVisitor(HttpServletRequest request) {
        String dmid = UtilMethods.getCookieValue(request.getCookies(),
                com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

        return Strings.isEmpty(dmid);
    }

    private InetAddress lookupIPAddress(HttpServletRequest request) {

        InetAddress address = null;
        try {
            address = HttpRequestDataUtil.getIpAddress(request);
        } catch(UnknownHostException e) {
            Logger.error(VisitorAPIImpl.class, "Could not get the IP Address from the request", e);
        }

        return address;
    }

    private UUID lookupDMID(HttpServletRequest request) {
        UUID dmid = null;
        String dmidStr = UtilMethods.getCookieValue(request.getCookies(), WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

        if(Strings.isBlank(dmidStr))
            return null;

        try {
            dmid = UUID.fromString(dmidStr);
        } catch(IllegalArgumentException e) {
            Logger.error(VisitorAPIImpl.class, "Invalid dmid cookie value", e);
        }

        return dmid;
    }

    private URI lookupReferrer(HttpServletRequest request) {
        URI referrer = null;
        try {
            String referrerStr = request.getHeader("Referer");
            if(Strings.isNotBlank(referrerStr)) {
                referrer = new URI(referrerStr);
            }
        } catch(URISyntaxException e) {
            Logger.error(VisitorAPIImpl.class, "Invalid Referrer sent in request", e);
        }

        return referrer;
    }


}
