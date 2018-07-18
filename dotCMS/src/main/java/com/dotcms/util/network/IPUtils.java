package com.dotcms.util.network;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.net.UnknownHostException;

public class IPUtils {
    /**
     * Determines whether an IP address is in a specific CIDR.  IPv4 & IPv6 supported
     *
     * @param ip
     *            - The IP address to validate.
     * @param CIDR
     *            - The CIDR-notation CIDR - i.e. ({@code "192.168.1.2/24"}, {@code "0:0:0:0:0:0:0:1/128"}).
     * @return If the IP address matches the given CIDR, returns {@code true}
     *         . Otherwise, returns {@code false}.
     */
    public static boolean isIpInCIDR(final String ip, final String CIDR) {
        boolean isMatching = false;
        if (UtilMethods.isSet(ip) && UtilMethods.isSet(CIDR)) {
            final String[] netmaskParts = CIDR.split("/");
            if (netmaskParts != null && netmaskParts.length == 2) {
                try{
                    final CIDRUtils cidr = new CIDRUtils(CIDR);
                    isMatching = cidr.isInRange(ip);
                }
                catch (UnknownHostException e) {
                    Logger.error(IPUtils.class, "UnknownHostException resolving:"  + CIDR, e);
                }
            }
            else {
                isMatching = CIDR.equals(ip);
            }
        }
        Logger.debug(IPUtils.class, "ip=" + ip + "; CIDR=" + CIDR + "; isMatching=" + isMatching);
        return isMatching;
    }
}
