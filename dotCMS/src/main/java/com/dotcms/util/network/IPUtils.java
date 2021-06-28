package com.dotcms.util.network;

import java.util.Objects;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
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

        if (UtilMethods.isEmpty(ip) ||  UtilMethods.isEmpty(CIDR)) {
            return false;
        }
        
        if (Objects.equals(ip, CIDR)) {
            return true;
        }

        if ("0.0.0.0/0".equals(CIDR)) {
            return true;
        }

        final SubnetUtils utils = new SubnetUtils(CIDR);
        utils.setInclusiveHostCount(true);


        return Try.of(() -> utils.getInfo().isInRange(ip)).getOrElse(false);


    }
}
