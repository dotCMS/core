package com.dotcms.util.network;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.xbill.DNS.Address;

public class IPUtils {
    private static final AtomicBoolean disabledIpPrivateSubnet = new AtomicBoolean(false);

    private IPUtils() {
        throw new IllegalStateException("static Utility class");
    }

    /**
     * Determines whether an IP address is in a specific CIDR. IPv4 and IPv6 are both supported.
     *
     * @param ip   The IP address to validate.
     * @param CIDR The CIDR-notation range, e.g. {@code "192.168.1.2/24"} or {@code "fc00::/7"}.
     * @return If the IP address matches the given CIDR, returns {@code true}. Otherwise,
     *         returns {@code false}. Cross-family comparisons never match.
     */
    public static boolean isIpInCIDR(final String ip, final String CIDR) {

        if (UtilMethods.isEmpty(ip) || UtilMethods.isEmpty(CIDR)) {
            return false;
        }

        if (Objects.equals(ip, CIDR)) {
            return true;
        }

        if ("0.0.0.0/0".equals(CIDR)) {
            return true;
        }

        // SubnetUtils only understands IPv4, so IPv6 ranges are evaluated separately.
        if (CIDR.indexOf(':') >= 0) {
            return isIpInCidrIpV6(ip, CIDR);
        }

        try {
            final SubnetUtils utils = new SubnetUtils(CIDR);
            utils.setInclusiveHostCount(true);
            return utils.getInfo().isInRange(ip);
        } catch (Exception e) {
            Logger.warnAndDebug(IPUtils.class, "subnet:" + CIDR + ", ip:" + ip + ", error:" + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Prefix match for IPv6, comparing the first {@code prefixLength} bits of both addresses.
     * Parsing is literal-only (no name resolution), so this never triggers a DNS lookup.
     */
    private static boolean isIpInCidrIpV6(final String ip, final String cidr) {
        final int slash = cidr.indexOf('/');
        if (slash < 0) {
            return false;
        }
        try {
            final byte[] address = Address.toByteArray(ip, Address.IPv6);
            final byte[] network = Address.toByteArray(cidr.substring(0, slash), Address.IPv6);
            if (address == null || network == null) {
                // one side is not a valid IPv6 literal, so this is a cross-family comparison
                return false;
            }
            final int prefixLength = Integer.parseInt(cidr.substring(slash + 1).trim());
            if (prefixLength < 0 || prefixLength > 128) {
                return false;
            }
            final int wholeBytes = prefixLength / 8;
            for (int i = 0; i < wholeBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            final int remainingBits = prefixLength % 8;
            if (remainingBits == 0) {
                return true;
            }
            final int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (address[wholeBytes] & mask) == (network[wholeBytes] & mask);
        } catch (Exception e) {
            Logger.warnAndDebug(IPUtils.class, "subnet:" + cidr + ", ip:" + ip + ", error:" + e.getMessage(), e);
            return false;
        }
    }

    private static final String REMOTE_CALL_SUBNET_BLACKLIST = "REMOTE_CALL_SUBNET_BLACKLIST";

    private static final String[] REMOTE_CALL_SUBNET_BLACKLIST_DEFAULT = {"127.0.0.1/32","10.0.0.0/8","172.16.0.0/12", "192.168.0.0/16", "169.254.169.254/32"};

    /**
     * The subnets from {@code REMOTE_CALL_SUBNET_BLACKLIST}, or {@code null} when the property is
     * not set. The distinction matters: an explicitly configured list is treated as the whole
     * policy, so a deployment can keep permitting specific internal ranges by narrowing it.
     */
    static final Lazy<String[]> configuredSubnets = Lazy.of(() -> {

        final String[] configured =
                Try.of(() -> Config.getStringArrayProperty(REMOTE_CALL_SUBNET_BLACKLIST, null))
                        .getOrNull();

        if (isSet(configured) && Arrays.stream(configured).noneMatch(subnet -> subnet.indexOf(':') >= 0)) {
            Logger.warn(IPUtils.class, REMOTE_CALL_SUBNET_BLACKLIST + " is set and lists IPv4 ranges only. "
                    + "An explicit list replaces the built-in address checks, so IPv6 ranges are not "
                    + "covered unless listed. Add the IPv6 ranges in use, for example ::1/128, "
                    + "fc00::/7 and fe80::/10.");
        }

        return configured;
    });

    private static boolean isSet(final String[] subnets) {
        return subnets != null && subnets.length > 0;
    }

    /**
     * It is important when we allow calling to remote endpoints that we verify that the remote
     * endpoint is not in our corporate or private network. This method checks if the ip or hostname
     * passed in is on the private network, which can be blocked if needed.
     *
     * <p>Every address the host resolves to is checked, across both address families, including
     * hosts that resolve to IPv6 addresses only. A host that resolves to any internal address is
     * treated as private.</p>
     *
     * <p>When {@code REMOTE_CALL_SUBNET_BLACKLIST} is set it defines the policy on its own and the
     * built-in address checks are not applied, so a deployment can permit specific internal ranges
     * by narrowing the list. When it is not set, the built-in checks apply.</p>
     *
     * <p>Resolution failures fail closed and return {@code true}.</p>
     *
     * <p>This is evaluated at validation time against the addresses the host resolves to at that
     * moment. Callers that need the guarantee to hold for the connection itself should pin the
     * validated address rather than resolving the host a second time.</p>
     *
     * @param ipOrHostName the IP literal or hostname to check
     * @return {@code true} when the host is internal, unresolvable, or empty
     */
    public static boolean isIpPrivateSubnet(final String ipOrHostName) {

        if (disabledIpPrivateSubnet.get()) {
            return false;
        }

        if (!UtilMethods.isSet(ipOrHostName)) {
            return true;
        }

        try {
            // getAllByName resolves both A and AAAA records, accepts bracketed IPv6 literals as
            // URL.getHost() reports them, and normalises IPv4-mapped forms to Inet4Address. It is
            // also the resolver the outbound HTTP client uses, so validation and connection agree.
            final InetAddress[] resolvedAddresses = InetAddress.getAllByName(ipOrHostName.trim());

            if (resolvedAddresses == null || resolvedAddresses.length == 0) {
                return true;
            }

            for (final InetAddress address : resolvedAddresses) {
                if (isInternalAddress(address, configuredSubnets.get())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Logger.warn(IPUtils.class, "unable to resolve hostname, assuming the worst:" + ipOrHostName + " "+ e.getMessage());
            return true;
        }
        return false;
    }

    /**
     * Whether a single resolved address belongs to a range that must not be reachable from a
     * user-supplied URL.
     *
     * @param address    the resolved address to classify
     * @param configured the explicitly configured subnets, or {@code null} when the property is
     *                   unset. When present it is the whole policy and replaces the built-in
     *                   checks, which is what keeps a narrowed list able to permit internal ranges.
     */
    static boolean isInternalAddress(final InetAddress address, final String[] configured) {

        final String hostAddress = stripScopeId(address.getHostAddress());

        if (isSet(configured)) {
            return matchesAny(hostAddress, configured);
        }

        return isInternalByCategory(address) || matchesAny(hostAddress, REMOTE_CALL_SUBNET_BLACKLIST_DEFAULT);
    }

    /**
     * Built-in classification of the ranges that are internal by definition rather than by
     * configuration. Applied only when {@code REMOTE_CALL_SUBNET_BLACKLIST} is not set.
     */
    private static boolean isInternalByCategory(final InetAddress address) {

        if (address.isAnyLocalAddress()      // 0.0.0.0 and ::
                || address.isLoopbackAddress()   // 127.0.0.0/8 and ::1
                || address.isLinkLocalAddress()  // 169.254.0.0/16 (incl. metadata) and fe80::/10
                || address.isSiteLocalAddress()  // 10/8, 172.16/12, 192.168/16 and fec0::/10
                || address.isMulticastAddress()) {
            return true;
        }

        // Unique-local addresses, fc00::/7. These are the IPv6 counterpart of the RFC1918 ranges
        // and the JDK exposes no predicate for them, so they are matched here. This range also
        // covers IPv6 instance-metadata addresses.
        return address instanceof Inet6Address && (address.getAddress()[0] & 0xFE) == 0xFC;
    }

    /**
     * Whether the address falls in any of the given CIDR ranges. Accepts IPv4 and IPv6 CIDRs.
     */
    private static boolean matchesAny(final String hostAddress, final String[] subnets) {
        for (final String subnet : subnets) {
            if (isIpInCIDR(hostAddress, subnet)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the interface suffix that {@link Inet6Address#getHostAddress()} appends for scoped
     * addresses, e.g. {@code fe80::1%lo0}, so the value can be parsed as a plain literal.
     */
    private static String stripScopeId(final String hostAddress) {
        final int scopeSeparator = hostAddress.indexOf('%');
        return scopeSeparator < 0 ? hostAddress : hostAddress.substring(0, scopeSeparator);
    }

    public static void disabledIpPrivateSubnet(final boolean disabledIpPrivateSubnet) {
        IPUtils.disabledIpPrivateSubnet.set(disabledIpPrivateSubnet);
    }
}
