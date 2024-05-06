package com.dotcms.util.network;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xbill.DNS.Address;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class IPUtils {
    private static final AtomicBoolean disabledIpPrivateSubnet = new AtomicBoolean(false);
    
    private IPUtils() {
        throw new IllegalStateException("static Utility class");
    }
    
    
    
    /**
     * Determines whether an IP address is in a specific CIDR.  IPv4 & IPv6 supported
     *
     * @param ip
     *            - The IP address to validate.
     * @param CIDR
     *            - The CIDR-notation CIDR - i.e. ({@code "192.168.1.2/24"}
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

        try{
            final SubnetUtils utils = new SubnetUtils(CIDR);
            utils.setInclusiveHostCount(true);
            return utils.getInfo().isInRange(ip);
        }catch(Exception e){
            Logger.warnAndDebug(IPUtils.class,"subnet:" + CIDR  + ", ip:" + ip + ", error:" + e.getMessage(), e);
        }
        return false;












    }
    
    private static final String[] REMOTE_CALL_SUBNET_BLACKLIST_DEFAULT = {"127.0.0.1/32","10.0.0.0/8","172.16.0.0/12", "192.168.0.0/16", "169.254.169.254/32"};
    
    
    static final Lazy<String[]> disallowedSubnets = Lazy.of(() -> 
                    Try.of(() -> Config.getStringArrayProperty("REMOTE_CALL_SUBNET_BLACKLIST", REMOTE_CALL_SUBNET_BLACKLIST_DEFAULT))
                    .getOrElse(REMOTE_CALL_SUBNET_BLACKLIST_DEFAULT));

    
    
    /**
     * It is important when we allow calling to remote endpoints that we verify
     * that the remote endpoint is not in our corprate or private network.
     * This method checks if the ip or hostname passed in is on the private network
     * which can be blocked if needed.
     * @param ipOrHostName
     * @return
     */
    public static boolean isIpPrivateSubnet(final String ipOrHostName) {

        
        if (disabledIpPrivateSubnet.get()) {
            return false;
        }

        if (ipOrHostName == null) {
            return true;
        }

        try {

            final String ip = "localhost".equals(ipOrHostName) ? "127.0.0.1" : Address.getByName(ipOrHostName).getHostAddress();


            for (String subnet : disallowedSubnets.get()) {
                if (isIpInCIDR(ip, subnet)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Logger.warn(IPUtils.class, "unable to resolve hostname, assuming the worst:" + ipOrHostName + " "+ e.getMessage());
            return true;
        }
        return false;



    }



    public static void disabledIpPrivateSubnet(final boolean disabledIpPrivateSubnet) {
        IPUtils.disabledIpPrivateSubnet.set(disabledIpPrivateSubnet);
    }
}
