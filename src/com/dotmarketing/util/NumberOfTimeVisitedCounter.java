package com.dotmarketing.util;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by freddyrodriguez on 14/3/16.
 */
public abstract class NumberOfTimeVisitedCounter {

    public static void maybeCount(HttpServletRequest request, HttpServletResponse response){
        String _oncePerVisitCookie = UtilMethods.getCookieValue(request.getCookies(), WebKeys.ONCE_PER_VISIT_COOKIE);

        if (!UtilMethods.isSet(_oncePerVisitCookie)) {
            Cookie cookie = CookieUtil.createOncePerVisitCookie();
            response.addCookie(cookie);
            reallyCountNewVisit(request, response);
        }
    }

    private static void reallyCountNewVisit(HttpServletRequest request, HttpServletResponse response){
        String _siteVisitsCookie = UtilMethods.getCookieValue(request.getCookies(), com.dotmarketing.util.WebKeys.SITE_VISITS_COOKIE);

        if(!UtilMethods.isSet(_siteVisitsCookie)){
            Cookie cookie = CookieUtil.createSiteVisitsCookie();
            response.addCookie(cookie);
        } else{
            int visits = Integer.parseInt(_siteVisitsCookie);
            visits++;
            Cookie siteVisitsCookie = UtilMethods.getCookie(request.getCookies(), com.dotmarketing.util.WebKeys.SITE_VISITS_COOKIE);
            siteVisitsCookie.setValue(Integer.toString(visits));
            siteVisitsCookie.setMaxAge(60 * 60 * 24 * 356 * 5);
            siteVisitsCookie.setPath("/");
            response.addCookie(siteVisitsCookie);
        }
    }

    public static int getNumberSiteVisits( HttpServletRequest request ){
        String cookieValue = UtilMethods.getCookieValue(request.getCookies(), WebKeys.SITE_VISITS_COOKIE);
        return (UtilMethods.isSet(cookieValue)) ?  Integer.parseInt(cookieValue) : 0;
    }
}
