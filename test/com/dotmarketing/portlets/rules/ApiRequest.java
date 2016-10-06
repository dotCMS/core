package com.dotmarketing.portlets.rules;

import com.dotmarketing.servlets.test.ServletTestRunner;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.http.HttpServletRequest;

import org.junit.Ignore;

/**
 * @author Geoff M. Granum
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class ApiRequest {

    private final String robotsTxtUrl;
    private final String jSessionIdCookie;

    public ApiRequest() {
        HttpServletRequest request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String jSessionId = request.getSession().getId();
        robotsTxtUrl = String.format("http://%s:%s/robots.txt?t=", serverName, serverPort);
        jSessionIdCookie = "JSESSIONID=" + jSessionId;
    }

    public URLConnection makeRequest() throws IOException {
        return makeRequest(robotsTxtUrl);
    }

    public URLConnection makeRequest(String urlStr, String...cookies) throws IOException {
        URL url = new URL(urlStr + System.currentTimeMillis());
        URLConnection con = url.openConnection();

        StringBuilder cookiesSB = new StringBuilder();

        if(jSessionIdCookie != null) {
            con.setRequestProperty("Cookie", jSessionIdCookie);
            cookiesSB.append(jSessionIdCookie).append("; ");
        }

        if(cookies != null) {
            for (String cookie:cookies) {
                cookiesSB.append(cookie).append("; ");
            }
        }

        if(cookiesSB.length()>0) {
            con.setRequestProperty("Cookie", cookiesSB.toString());
        }

        con.connect();
        con.getInputStream();
        return con;
    }
}
 
