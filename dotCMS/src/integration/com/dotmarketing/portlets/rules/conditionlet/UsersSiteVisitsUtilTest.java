package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.util.CookieTestUtil;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by freddyrodriguez on 15/3/16.
 */
class UsersSiteVisitsUtilTest {

    private List<HttpCookie> historyCookiesAsString;
    private HttpServletRequest request;

    UsersSiteVisitsUtilTest(HttpServletRequest request){

        this.request = request;
    }

    public URLConnection makeNewSessionRequest(String url) throws IOException {
        return this.makeRequest(url, true);
    }

    public URLConnection makeRequest(String url) throws IOException {
        return this.makeRequest(url, false);
    }

    private URLConnection makeRequest(String url, boolean deleteOncePerVisitCookie) throws IOException {

        if ( deleteOncePerVisitCookie && historyCookiesAsString != null){
            historyCookiesAsString = deleteOncePerVisitCookie( historyCookiesAsString );
        }

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest(url, null, CookieTestUtil.getCookiesAsString(historyCookiesAsString));
        List<HttpCookie> cookies = CookieTestUtil.getCookies(conn);

        if ( historyCookiesAsString != null && cookies != null) {
            historyCookiesAsString = joinCookies(cookies);
        }else if (cookies != null){
            historyCookiesAsString = cookies;
        }

        return conn;
    }

    private List<HttpCookie> joinCookies(List<HttpCookie> cookies) {

        List<HttpCookie> aux = new ArrayList<>();

        for (HttpCookie historyCookie : historyCookiesAsString) {

            HttpCookie toAdd = historyCookie;
            boolean remove = false;

            for (HttpCookie newCookie : cookies) {
                if (historyCookie.getName().equals( newCookie.getName() )){
                    toAdd = newCookie;
                    remove = true;
                    break;
                }
            }

            aux.add( toAdd );

            if (remove){
                try {
                    cookies.remove(toAdd);
                }catch(Exception e){
                    System.out.println();
                }
            }
        }

        aux.addAll(cookies);
        return aux;
    }

    private List<HttpCookie> deleteOncePerVisitCookie(List<HttpCookie> cookies) {

        List<HttpCookie> result = new ArrayList<>();

        for (HttpCookie httpCookie : cookies) {
            if (!httpCookie.getName().equals( WebKeys.ONCE_PER_VISIT_COOKIE )){
                result.add( httpCookie );
            }
        }


        return result;
    }

    public void clean() {
        historyCookiesAsString = null;
    }
}
