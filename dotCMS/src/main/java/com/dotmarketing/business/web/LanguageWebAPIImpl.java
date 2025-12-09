package com.dotmarketing.business.web;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Locale;

/**
 * Implement LanguageWebAPI methods to manage language cache and language struts
 *
 * @author Oswaldo
 * @author David H Torres
 * @version 1.9
 *
 */
public class LanguageWebAPIImpl implements LanguageWebAPI {

    private final LanguageAPI langAPI;

    public LanguageWebAPIImpl() {
        langAPI = APILocator.getLanguageAPI();
    }
    private static final String HTMLPAGE_CURRENT_LANGUAGE = WebKeys.HTMLPAGE_LANGUAGE + ".current";
    /**
     * Clear the language cache and struts cache
     *
     * @throws DotRuntimeException
     */
    public void clearCache() throws DotRuntimeException {

        langAPI.clearCache();
        MultiMessageResources messages = (MultiMessageResources) MultiMessageResourcesFactory.getResources();
        messages.reload();

    }



    // only try internal session and attributes
    private Language currentLanguage(HttpServletRequest httpRequest) {
        HttpSession sessionOpt = httpRequest.getSession(false);

        try{
            if(sessionOpt !=null){
                if(sessionOpt.getAttribute("tm_lang")!=null){
                    return langAPI.getLanguage((String) sessionOpt.getAttribute("tm_lang"));
                }else{
                    return langAPI.getLanguage((String) sessionOpt.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE));
                }
            }
            else if(UtilMethods.isSet(httpRequest.getAttribute(HTMLPAGE_CURRENT_LANGUAGE))){
                return langAPI.getLanguage((String) httpRequest.getAttribute(HTMLPAGE_CURRENT_LANGUAGE));
            }
        }
        catch(Exception e){
            // no log
        }

        return langAPI.getDefaultLanguage();
    }


    @Override
    public void checkSessionLocale(HttpServletRequest httpRequest) {
        getLanguage(httpRequest);

    }

    private Language futureLanguage(HttpServletRequest httpRequest,Language currentLang) {
        Language futureLang = currentLang;
        // update page language
        String tryId=null;
        if (UtilMethods.isSet(httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE))){
            tryId = httpRequest.getParameter(WebKeys.HTMLPAGE_LANGUAGE);
        }else if(UtilMethods.isSet(httpRequest.getParameter("language_id"))) {
            tryId = httpRequest.getParameter("language_id");
        } else if(UtilMethods.isSet(httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE))) {
            tryId = (String)httpRequest.getAttribute(WebKeys.HTMLPAGE_LANGUAGE);
        }
        if(tryId!=null){
            try{
                futureLang = langAPI.getLanguage(Long.parseLong(tryId));
                if(futureLang==null) throw new DotStateException("lang cannot be null");
            } catch (Exception e){
                Logger.debug(this.getClass(), "invalid language passed in:" + tryId);
                futureLang=currentLang;
            }
        }

        return futureLang;
    }

    /**
     * Here is the order in which langauges should be checked:
     * first,      is there a parameter passed, if so use it
     * second,     is there an attribute set, if so use it
     * third,      is there a language in session, if so use it
     * finally     use the default language
     */
    @CloseDBIfOpened
    @Override
    public Language getLanguage(HttpServletRequest httpRequest) {
        final Language current = currentLanguage(httpRequest);
        final Language future = futureLanguage(httpRequest,current);
        final Locale locale = null != future.getCountryCode()? new Locale(future.getLanguageCode(), future.getCountryCode()): new Locale(future.getLanguageCode());
        HttpSession sessionOpt = httpRequest.getSession(false);
        httpRequest.setAttribute(HTMLPAGE_CURRENT_LANGUAGE, String.valueOf(future.getId()));

        httpRequest.setAttribute(WebKeys.LOCALE, locale);

        // if someone is changing langauges, we need a session
        if(!current.equals(future)){
            sessionOpt = httpRequest.getSession(true);
        }
        if(sessionOpt!=null){
            //only set in session if we are not in a timemachine
            if(sessionOpt.getAttribute("tm_lang")==null){
                sessionOpt.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(future.getId()));
                boolean ADMIN_MODE =   PageMode.get(httpRequest).isAdmin;

                if (ADMIN_MODE == false || httpRequest.getParameter("leftMenu") == null) {
                    sessionOpt.setAttribute(WebKeys.Globals_FRONTEND_LOCALE_KEY, locale);
                    httpRequest.setAttribute(WebKeys.Globals_FRONTEND_LOCALE_KEY, locale);
                }
                sessionOpt.setAttribute(WebKeys.LOCALE, locale);
            }
        }
        return future;

    }

    /**
     * Return the back end session language
     * @return
     */
    public Language getBackendLanguage() {
        return this.getBackendLanguage(HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }

    /**
     * Return the back end session language
     * @return
     */
    public Language getBackendLanguage(final HttpServletRequest request) {
        Locale locale = null;

        if(request != null) {
            locale = this.getGlocalLocale(request);

            if (locale == null) {
                locale = (Locale) request.getAttribute(Globals.LOCALE_KEY);
            }

            if (locale == null) {
                locale = getLocaleFromSession(request);
            }
        }

        return getLanguage(locale);
    }

    private Locale getLocaleFromSession(HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        return session != null ? (Locale) session.getAttribute(Globals.LOCALE_KEY) :  null;
    }

    private Language getLanguage(final Locale locale) {
        Language language = null;

        if (locale != null) {
            language = APILocator.getLanguageAPI().getLanguage(locale.getLanguage(), locale.getCountry());
        }

        return language != null ? language : APILocator.getLanguageAPI().getDefaultLanguage();
    }

    private Locale getGlocalLocale(final HttpServletRequest request){
        final String parameter = request.getParameter(WebKeys.BACKEND_LANGUAGE_PARAMETER_NAME);

        if (parameter != null) {
            final String[] parameterSplit = parameter.split("-|_");
            return parameterSplit.length < 2 ? null : new Locale(parameterSplit[0], parameterSplit[1]);
        } else {
            return null;
        }
    }

}
