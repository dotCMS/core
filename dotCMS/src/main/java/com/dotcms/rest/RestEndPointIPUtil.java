package com.dotcms.rest;

import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.util.UtilMethods;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.Optional;

/**
 * Provide util methods to get remote or local IP from {@link HttpServletRequest}
 */
public class RestEndPointIPUtil {
    /**
     * Tries to get the local address plus the port in a "host:port" format
     * @param request http servlet request
     * @return a string representing the address plus the port
     */
    public static String getFullLocalIp(@Context final HttpServletRequest request) {
        final String localIp = request.getLocalName();
        final Optional<String> port = HttpRequestDataUtil.getServerPort();
        return (!UtilMethods.isSet(localIp) ? localIp : request.getLocalName())
                + ':' + port.orElse(String.valueOf(request.getLocalPort()));
    }

    /**
     * Resolves remote IP address from request.
     * @param request {@link HttpServletRequest}
     * @return a String representing the remote IP address (or hostname)
     */
    public static String resolveRemoteIp(final HttpServletRequest request) {
        final String remoteIP = request.getRemoteHost();
        return !UtilMethods.isSet(remoteIP) ? remoteIP : request.getRemoteAddr();
    }
}
