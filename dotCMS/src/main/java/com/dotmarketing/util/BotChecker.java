package com.dotmarketing.util;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.config.ConfigLoader;

/**
 * Determines if a request is actually a bot or spider.
 *
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody</a>
 */
public class BotChecker {
    public static boolean isBot(HttpServletRequest request) {
        List agents = ConfigLoader.getInstance().getConfig().getBotAgents();
        List hosts = ConfigLoader.getInstance().getConfig().getBotHosts();

        if (request.getRequestURI().indexOf("robots.txt") != -1) {
            // there is a specific request for the robots.txt file, so we assume
            // it must be a robot (only robots request robots.txt)
            return true;
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            for (Iterator iterator = agents.iterator(); iterator.hasNext();) {
                String agent = (String) iterator.next();
                if (userAgent.indexOf(agent) != -1) {
                    return true;
                }
            }
        }

        String remoteHost = request.getRemoteHost(); // requires a DNS lookup
        if (remoteHost != null && remoteHost.length() > 0 && remoteHost.charAt(remoteHost.length() - 1) > 64) {
            for (Iterator iterator = hosts.iterator(); iterator.hasNext();) {
                String host = (String) iterator.next();
                if (remoteHost.indexOf(host) != -1) {
                    return true;
                }
            }
        }

        return false;
    }
}