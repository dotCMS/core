package com.dotmarketing.util;


import javax.servlet.http.Cookie;
import java.net.HttpCookie;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * Created by freddyrodriguez on 10/3/16.
 */
public abstract class CookieTestUtil {

    private static final String RESPONSE_COOKIES_HEADER = "Set-Cookie";
    private static final String REQUEST_COOKIES_HEADER = "Cookie";

    public static String[] getCookiesAsString(URLConnection conn){

        Map<String, List<String>> headerFields = conn.getHeaderFields();
        List<String> cookiesHeader = headerFields.get(RESPONSE_COOKIES_HEADER);

        if (cookiesHeader != null) {
            String[] result = new String[cookiesHeader.size()];

            return cookiesHeader.toArray(result);
        }else{
            return null;
        }
    }

    public static List<HttpCookie> getCookies(URLConnection conn){

        List<HttpCookie> result = null;

        Map<String, List<String>> headerFields = conn.getHeaderFields();
        List<String> cookiesHeader = headerFields.get(RESPONSE_COOKIES_HEADER);

        if(cookiesHeader != null)
        {
            java.net.CookieManager msCookieManager = new java.net.CookieManager();

            for (String cookie : cookiesHeader)
            {
                msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
            }

            result = msCookieManager.getCookieStore().getCookies();
        }

        return result;
    }

    public static void addCookies(URLConnection conn, List<HttpCookie> cookies ){

        conn.setRequestProperty(REQUEST_COOKIES_HEADER, join(cookies));
    }

    public static String[] getCookiesAsString(List<HttpCookie> cookies) {
        String[] result = null;

        if (cookies != null) {
            result = new String[cookies.size()];

            for (int i = 0; i < cookies.size(); i++) {
                HttpCookie cookie = cookies.get(i);
                result[i] = cookie.toString();
            }
        }

        return result;
    }

    private static String join(List<HttpCookie> cookies) {

        StringBuffer buffer = new StringBuffer();

        for (HttpCookie cookie : cookies) {

            if (buffer.length() > 0){
                buffer.append( ";" );
            }

            buffer.append( cookie.toString() );
        }

        return buffer.toString();
    }
}
