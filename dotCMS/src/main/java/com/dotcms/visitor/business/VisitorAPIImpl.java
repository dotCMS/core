package com.dotcms.visitor.business;

import com.dotcms.util.DotPreconditions;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.logging.log4j.util.Strings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class VisitorAPIImpl implements VisitorAPI {

    private final LanguageWebAPI languageWebAPI;

    private final PersonaAPI personaAPI ;
    
    
    public VisitorAPIImpl() {
      this(WebAPILocator.getLanguageWebAPI(),APILocator.getPersonaAPI());
    }
    
    
    public VisitorAPIImpl(LanguageWebAPI languageWebAPI, PersonaAPI personaAPI) {
      super();
      this.languageWebAPI = languageWebAPI;
      this.personaAPI = personaAPI;
    }


    @Override
    public Optional<Visitor> getVisitor(HttpServletRequest request) {
        return getVisitor(request, Config.getBooleanProperty("CREATE_VISITOR_OBJECT_IN_SESSION", true));
    }

    @Override
    public Optional<Visitor> getVisitor(HttpServletRequest request, boolean forceSession) {

        DotPreconditions.checkNotNull(request, IllegalArgumentException.class, "Null Request");
        final HttpSession session = request.getSession(forceSession);
        
        final Visitor visitor = session !=null && session.getAttribute(WebKeys.VISITOR)!=null
                        ? (Visitor) session.getAttribute(WebKeys.VISITOR)
                        : request.getAttribute(WebKeys.VISITOR)!=null
                            ?  (Visitor) request.getAttribute(WebKeys.VISITOR)
                            :  createVisitor(request);

        if(forceSession) {
            session.setAttribute(WebKeys.VISITOR, visitor);
        }
        request.setAttribute(WebKeys.VISITOR, visitor);

        if (Objects.nonNull(request.getParameter(WebKeys.CMS_PERSONA_PARAMETER))) {


            final PageMode pageMode = PageMode.get(request);
            try {

                final User user = com.liferay.portal.util.PortalUtil.getUser(request);
                final Persona persona = pageMode.showLive
                                ? personaAPI.findLive(request.getParameter(WebKeys.CMS_PERSONA_PARAMETER), user, true)
                                : personaAPI.find(request.getParameter(WebKeys.CMS_PERSONA_PARAMETER), user, true);
                visitor.setPersona(persona);
            } catch (DotContentletStateException e) {
                // This is meant to catch the "Can't find contentlet" error.
                Logger.debug(this, e.getMessage(), e);
                visitor.setPersona(null);
            } catch (Exception e) {
                // Anything else will be reported here.
                Logger.error(this, e);
                visitor.setPersona(null);
            }
        }
        

        return Optional.of(visitor);
    }

    public void removeVisitor(final HttpServletRequest request){
        DotPreconditions.checkNotNull(request, IllegalArgumentException.class, "Null Request");
        final HttpSession session = request.getSession(false);
        session.removeAttribute(WebKeys.VISITOR);
    }

    private Visitor createVisitor(final HttpServletRequest request) {

        final Visitor visitor = new Visitor();
        
        final InetAddress ipAddress = lookupIPAddress(request);

        if(Objects.isNull(ipAddress)){
            //Exception was thrown so we return an empty, not-null visitor
            return visitor;
        }

        final Language selectedLanguage = languageWebAPI.getLanguage(request);
        final Locale locale = new Locale(selectedLanguage.getLanguageCode(), selectedLanguage.getCountryCode());
        final UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
        final UUID dmid = lookupDMID(request);
        final boolean isNewVisitor = isNewVisitor(request);
        final URI initialReferrer = lookupReferrer(request);

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
