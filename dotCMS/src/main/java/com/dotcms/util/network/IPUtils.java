package com.dotcms.util.network;

import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xbill.DNS.Address;

/**
 * Address checks for outbound calls made on behalf of user-supplied URLs. The policy is read
 * once per JVM from the provision-time settings {@code REMOTE_CALL_SUBNET_BLACKLIST},
 * {@code REMOTE_CALL_SUBNET_ALLOWLIST} and {@code REMOTE_CALL_ALLOW_IPV6}; see
 * {@link AddressPolicy} for how they combine.
 */
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
     *         returns {@code false}. Cross-family comparisons never match, except that
     *         {@code "0.0.0.0/0"} keeps its long-standing meaning of "every address" for the
     *         callers that use it as a catch-all.
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

        // Cross-family comparisons never match: an IPv4 range says nothing about an IPv6
        // address and the other way round, and SubnetUtils would refuse the IPv6 literal.
        final boolean ipIsV6 = ip.indexOf(':') >= 0;
        final boolean rangeIsV6 = CIDR.indexOf(':') >= 0;
        if (ipIsV6 != rangeIsV6) {
            return false;
        }

        // SubnetUtils only understands IPv4, so IPv6 ranges are evaluated separately.
        if (rangeIsV6) {
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

    /** Ranges refused in addition to the built-in checks. Comma-separated CIDR list. */
    static final String REMOTE_CALL_SUBNET_BLACKLIST = "REMOTE_CALL_SUBNET_BLACKLIST";

    /** Ranges permitted although the built-in checks would refuse them. Comma-separated CIDR list. */
    static final String REMOTE_CALL_SUBNET_ALLOWLIST = "REMOTE_CALL_SUBNET_ALLOWLIST";

    /** Whether IPv6 addresses may be reached at all. Defaults to {@code false}. */
    static final String REMOTE_CALL_ALLOW_IPV6 = "REMOTE_CALL_ALLOW_IPV6";

    /**
     * Ranges refused by default, on top of the address categories the JDK classifies
     * (loopback, unspecified, link-local, site-local, multicast) and the unique-local IPv6 range.
     * The allow list may open these; it may never open {@link #NEVER_PERMITTED_RANGES}.
     */
    private static final List<String> BUILT_IN_RANGES = List.of(
            "127.0.0.1/32",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "169.254.169.254/32",
            "100.64.0.0/10",    // shared address space
            "192.0.0.0/24",     // IETF protocol assignments
            "198.18.0.0/15",    // benchmarking
            "240.0.0.0/4",      // reserved
            "64:ff9b::/96"      // IPv4/IPv6 translation
    );

    /**
     * Ranges that stay refused whatever the lists say: loopback, the unspecified address and
     * "this network", the link-local ranges (which hold the instance metadata and credential
     * endpoints) and the IPv6 instance metadata address. Only switching the checks off as a
     * whole ({@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS}) reaches them.
     */
    private static final List<String> NEVER_PERMITTED_RANGES = List.of(
            "127.0.0.0/8",
            "::1/128",
            "0.0.0.0/8",
            "::/128",
            "169.254.0.0/16",
            "fe80::/10",
            "fd00:ec2::254/128"
    );

    /**
     * The provision-time address policy. A resolved address is refused when it is in
     * {@link #NEVER_PERMITTED_RANGES} or in a denied range; otherwise it is permitted when an
     * allowed range covers it; otherwise it is refused when the built-in checks cover it. So an
     * explicit deny entry always wins, and the allow list only opens built-in ranges. IPv6
     * addresses are refused outright unless {@code ipv6Allowed} is set, in which case they go
     * through the same steps.
     *
     * <p>Entries that are not an IP address or a CIDR range are dropped when the policy is
     * built, with one warning each, so a typo neither widens nor narrows the policy.</p>
     *
     * @param denied      ranges refused in addition to the built-in checks
     * @param allowed     ranges permitted although the built-in checks would refuse them
     * @param ipv6Allowed whether IPv6 addresses may be reached
     */
    public record AddressPolicy(List<String> denied, List<String> allowed, boolean ipv6Allowed) {

        /**
         * Keeps the lists non-null and unmodifiable, whichever constructor built the policy.
         */
        public AddressPolicy {
            denied = denied == null ? List.of() : List.copyOf(denied);
            allowed = allowed == null ? List.of() : List.copyOf(allowed);
        }

        /**
         * Builds a policy from the raw configured lists, keeping the valid entries only.
         *
         * @param denied      the configured deny list, or {@code null} when unset
         * @param allowed     the configured allow list, or {@code null} when unset
         * @param ipv6Allowed whether IPv6 addresses may be reached
         * @return the policy
         */
        public static AddressPolicy of(final String[] denied, final String[] allowed, final boolean ipv6Allowed) {
            return new AddressPolicy(validRanges(REMOTE_CALL_SUBNET_BLACKLIST, denied),
                    validRanges(REMOTE_CALL_SUBNET_ALLOWLIST, allowed), ipv6Allowed);
        }
    }

    /**
     * The policy read from configuration, once per JVM. These settings are provision-time
     * settings: they are read from the environment or the properties file only.
     */
    static final Lazy<AddressPolicy> configuredPolicy = Lazy.of(() -> {

        final String[] denied = Try.of(() -> Config.getStringArrayProperty(REMOTE_CALL_SUBNET_BLACKLIST, null))
                .getOrNull();
        final String[] allowed = Try.of(() -> Config.getStringArrayProperty(REMOTE_CALL_SUBNET_ALLOWLIST, null))
                .getOrNull();
        final boolean ipv6Allowed = Try.of(() -> Config.getBooleanProperty(REMOTE_CALL_ALLOW_IPV6, false))
                .getOrElse(false);

        if (isSet(denied)) {
            Logger.warn(IPUtils.class, REMOTE_CALL_SUBNET_BLACKLIST + " is set: the listed ranges are refused "
                    + "in addition to the built-in address checks. Use " + REMOTE_CALL_SUBNET_ALLOWLIST
                    + " to permit specific ranges.");
        }
        if (isSet(allowed)) {
            Logger.info(IPUtils.class, REMOTE_CALL_SUBNET_ALLOWLIST + " permits the ranges: "
                    + String.join(",", allowed));
        }
        if (!ipv6Allowed) {
            Logger.info(IPUtils.class, "Remote calls use IPv4 only. Set " + REMOTE_CALL_ALLOW_IPV6
                    + "=true to reach IPv6 addresses.");
        }

        return AddressPolicy.of(denied, allowed, ipv6Allowed);
    });

    private static boolean isSet(final String[] subnets) {
        return subnets != null && subnets.length > 0;
    }

    /**
     * Keeps the entries that are an IP address or a CIDR range, logging and dropping the rest.
     */
    private static List<String> validRanges(final String key, final String[] entries) {

        if (!isSet(entries)) {
            return List.of();
        }
        final List<String> ranges = new ArrayList<>(entries.length);
        for (final String entry : entries) {
            final String range = asRange(entry);
            if (range == null) {
                Logger.warn(IPUtils.class, key + ": ignoring entry '" + entry
                        + "', it is not an IP address or a CIDR range");
            } else {
                ranges.add(range);
            }
        }
        return List.copyOf(ranges);
    }

    /**
     * Returns the entry as a CIDR range: a bare address becomes a single-address range. Returns
     * {@code null} when the entry is neither an address nor a range.
     */
    private static String asRange(final String entry) {

        if (UtilMethods.isEmpty(entry)) {
            return null;
        }
        final String trimmed = entry.trim();
        final int slash = trimmed.indexOf('/');
        if (slash < 0) {
            if (Address.toByteArray(trimmed, Address.IPv4) != null) {
                return trimmed + "/32";
            }
            return Address.toByteArray(trimmed, Address.IPv6) != null ? trimmed + "/128" : null;
        }
        final String network = trimmed.substring(0, slash);
        final int prefixLength = Try.of(() -> Integer.parseInt(trimmed.substring(slash + 1).trim()))
                .getOrElse(-1);
        if (Address.toByteArray(network, Address.IPv4) != null) {
            if (prefixLength == 0) {
                // a zero-length prefix covers every IPv4 address, whatever network it names
                return "0.0.0.0/0";
            }
            return prefixLength > 0 && prefixLength <= 32 ? trimmed : null;
        }
        if (Address.toByteArray(network, Address.IPv6) != null) {
            return prefixLength >= 0 && prefixLength <= 128 ? trimmed : null;
        }
        return null;
    }

    /**
     * It is important when we allow calling to remote endpoints that we verify that the remote
     * endpoint is not in our corporate or private network. This method checks if the ip or hostname
     * passed in is on the private network, which can be blocked if needed.
     *
     * <p>The host is resolved and every address it resolves to goes through the configured
     * {@link AddressPolicy}. A host is treated as private when none of its addresses may be
     * reached: an address the policy refuses makes the whole host private, while IPv6
     * addresses are left out of the decision when IPv6 is not allowed, so a host with
     * addresses in both families keeps working over IPv4.</p>
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
            return permittedAddresses(resolvedAddresses, configuredPolicy.get()).length == 0;
        } catch (Exception e) {
            Logger.warn(IPUtils.class, "unable to resolve hostname, assuming the worst:" + ipOrHostName + " "+ e.getMessage());
            return true;
        }
    }

    /**
     * Whether a single already-resolved address belongs to a range that must not be reached
     * from a user-supplied URL, under the same policy as {@link #isIpPrivateSubnet(String)}.
     * Meant for callers that resolve the host themselves, such as the HTTP client's resolver,
     * so the address that is connected to is the address that was checked.
     *
     * @param address the resolved address
     * @return {@code true} when the address is internal, or when the address is missing
     */
    public static boolean isInternalAddress(final InetAddress address) {

        if (disabledIpPrivateSubnet.get()) {
            return false;
        }
        return isInternalAddress(address, configuredPolicy.get());
    }

    /**
     * The addresses of a resolved host that the configured policy permits connecting to. An
     * empty result means the host must not be reached at all.
     *
     * @param resolved every address the host resolved to
     * @return the addresses that may be connected to, in their original order
     */
    public static InetAddress[] permittedAddresses(final InetAddress[] resolved) {

        if (disabledIpPrivateSubnet.get()) {
            return resolved == null ? new InetAddress[0] : resolved;
        }
        return permittedAddresses(resolved, configuredPolicy.get());
    }

    /**
     * Applies a policy to a single address. See {@link AddressPolicy} for the order of the
     * checks.
     *
     * @param address the resolved address
     * @param policy  the policy to apply
     * @return {@code true} when the address must not be reached, or when it is missing
     */
    public static boolean isInternalAddress(final InetAddress address, final AddressPolicy policy) {

        if (null == address) {
            return true;
        }
        if (address instanceof Inet6Address && !policy.ipv6Allowed()) {
            return true;
        }
        final String hostAddress = stripScopeId(address.getHostAddress());
        if (isNeverPermitted(address, hostAddress) || matchesAny(hostAddress, policy.denied())) {
            return true;
        }
        if (matchesAny(hostAddress, policy.allowed())) {
            return false;
        }
        return isInternalByCategory(address) || matchesAny(hostAddress, BUILT_IN_RANGES);
    }

    /**
     * Applies a policy to every address a host resolved to. IPv6 addresses are left out when
     * the policy does not allow them; of the rest, one refused address refuses the host.
     *
     * @param resolved every address the host resolved to
     * @param policy   the policy to apply
     * @return the addresses that may be connected to, or an empty array when the host must not
     *         be reached
     */
    public static InetAddress[] permittedAddresses(final InetAddress[] resolved, final AddressPolicy policy) {

        final InetAddress[] none = new InetAddress[0];
        final List<InetAddress> candidates = candidateAddresses(resolved, policy);
        for (final InetAddress address : candidates) {
            if (isInternalAddress(address, policy)) {
                return none;
            }
        }
        return candidates.toArray(none);
    }

    /**
     * The addresses of a host that a client may consider under the configured policy, before
     * any of them is checked: IPv6 addresses are left out while {@code REMOTE_CALL_ALLOW_IPV6}
     * is off, so a host with addresses in both families is reached over IPv4. A caller that
     * checks each address itself, such as a socket factory, starts from this list so that it
     * applies the same family rule as {@link #permittedAddresses(InetAddress[])}.
     *
     * @param resolved every address the host resolved to
     * @return the addresses to consider, in the resolved order; empty when there is none, when
     *         an entry is missing, or when the input is missing
     */
    public static List<InetAddress> candidateAddresses(final InetAddress[] resolved) {

        if (disabledIpPrivateSubnet.get()) {
            return resolved == null ? List.of() : Arrays.stream(resolved).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return candidateAddresses(resolved, configuredPolicy.get());
    }

    /**
     * Same as {@link #candidateAddresses(InetAddress[])} under the given policy.
     *
     * @param resolved every address the host resolved to
     * @param policy   the policy to apply
     * @return the addresses to consider, in the resolved order
     */
    public static List<InetAddress> candidateAddresses(final InetAddress[] resolved, final AddressPolicy policy) {

        if (resolved == null || resolved.length == 0) {
            return List.of();
        }
        final List<InetAddress> candidates = new ArrayList<>(resolved.length);
        for (final InetAddress address : resolved) {
            if (address == null) {
                return List.of();
            }
            if (address instanceof Inet6Address && !policy.ipv6Allowed()) {
                continue;
            }
            candidates.add(address);
        }
        return candidates;
    }

    /**
     * Whether the address is one the lists can never open.
     */
    private static boolean isNeverPermitted(final InetAddress address, final String hostAddress) {
        return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isLinkLocalAddress()
                || matchesAny(hostAddress, NEVER_PERMITTED_RANGES);
    }

    /**
     * Built-in classification of the ranges that are internal by definition rather than by
     * configuration.
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
     * Whether the address falls in any of the given CIDR ranges. Ranges of the other address
     * family are skipped, so an IPv4 catch-all such as {@code 0.0.0.0/0} covers IPv4 addresses
     * only within the policy.
     */
    private static boolean matchesAny(final String hostAddress, final List<String> subnets) {
        final boolean addressIsV6 = hostAddress.indexOf(':') >= 0;
        for (final String subnet : subnets) {
            if ((subnet.indexOf(':') >= 0) == addressIsV6 && isIpInCIDR(hostAddress, subnet)) {
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

    /**
     * Switches every address check off or back on. Meant for tests that run against a local
     * server on the loopback interface.
     *
     * @param disabledIpPrivateSubnet {@code true} to permit every address
     */
    public static void disabledIpPrivateSubnet(final boolean disabledIpPrivateSubnet) {
        IPUtils.disabledIpPrivateSubnet.set(disabledIpPrivateSubnet);
    }
}
