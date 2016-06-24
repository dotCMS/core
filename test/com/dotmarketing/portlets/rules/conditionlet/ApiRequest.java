package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.servlets.test.ServletTestRunner;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * Created by freddy on 27/01/16.
 */
class ApiRequest {

    private String baseUrl;
    private String jSessionIdCookie;
    private String[] requestPropertiesKey;

    public ApiRequest ( HttpServletRequest request, String... requestPropertiesKey ) {
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String jSessionId = request.getSession().getId();
        baseUrl = String.format("http://%s:%s/", serverName, serverPort);
        jSessionIdCookie = "JSESSIONID=" + jSessionId;
        this.requestPropertiesKey = requestPropertiesKey;
    }

    public URLConnection makeRequest (String path, String... propertiesValues ) throws IOException {
        return makeRequest(new URL(baseUrl + path), propertiesValues);
    }

    public URLConnection makeRequest (String path, String[] propertiesValues, String... cookies  ) throws IOException {
        return makeRequest(new URL(baseUrl + path), propertiesValues, cookies);
    }

    public URLConnection makeRequest ( URL url, String[] propertiesValues, String... cookies ) throws IOException {

        URLConnection con = url.openConnection();

        for (int i = 0; i < requestPropertiesKey.length; i++) {
            String propertyKey = requestPropertiesKey[i];
            con.setRequestProperty(propertyKey, propertiesValues[i]);
        }

        StringBuilder cookiesSB = new StringBuilder();

        if ( jSessionIdCookie != null ) {
            con.setRequestProperty("Cookie", jSessionIdCookie);
            cookiesSB.append(jSessionIdCookie).append("; ");
        }

        if ( cookies != null ) {
            for ( String cookie : cookies ) {
                cookiesSB.append(cookie).append("; ");
            }
        }

        if ( cookiesSB.length() > 0 ) {
            con.setRequestProperty("Cookie", cookiesSB.toString());
        }

        con.connect();
        con.getInputStream();
        return con;
    }

}
