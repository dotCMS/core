package com.dotcms.util.network;

import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

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
                return Try.of(()->new SubnetUtils(CIDR).getInfo().isInRange(ip)).getOrElse(false);
            }
            else {
                isMatching = CIDR.equals(ip);
            }
        }
        Logger.debug(IPUtils.class, "ip=" + ip + "; CIDR=" + CIDR + "; isMatching=" + isMatching);
        return isMatching;
    }
}
