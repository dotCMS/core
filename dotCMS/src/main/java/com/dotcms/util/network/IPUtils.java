package com.dotcms.util.network;

import java.net.InetAddress;
import java.util.Objects;
import org.xbill.DNS.Address;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.exception.DotRuntimeException;
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
    
    final static private String[] privateSubnets = {"10.0.0.0/8","172.16.0.0/12", "192.168.0.0/16"};
    
    
    
    
    public static boolean isIpPrivateSubnet(final String ipOrHostName) {

        if (ipOrHostName == null) {
            return true;
        }

        try {
            InetAddress addr = Address.getByName(ipOrHostName);

            final String ip = addr.getHostAddress();

            if ("127.0.0.1".equals(ip)) {
                return true;
            }

            if ("localhost".equals(ip)) {
                return true;
            }
            
            for (String subnet : privateSubnets) {
                if (isIpInCIDR(ip, subnet)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Logger.warn(IPUtils.class, "unable to resolve hostname");
            throw new DotRuntimeException(e);
        }
        return false;



    }

    
    
}
