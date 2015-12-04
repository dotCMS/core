package com.dotmarketing.portlets.rules;

import com.dotmarketing.servlets.test.ServletTestRunner;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Geoff M. Granum
 */
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
        URL url = new URL(robotsTxtUrl + System.currentTimeMillis());
        URLConnection con = url.openConnection();

        if(jSessionIdCookie != null) {
            con.setRequestProperty("Cookie", jSessionIdCookie);
        }

        con.connect();
        con.getInputStream();
        return con;
    }
}
 
