package com.dotcms.http;

import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import io.vavr.Lazy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;

/**
 * Name resolution for outbound calls made on behalf of a user-supplied URL. The host is
 * checked before the call and again at each redirect, but those checks resolve the name on
 * their own and the client resolves it once more to connect, so a name whose answer changes
 * in between could still reach an internal address. This resolver is the one the client
 * connects with: every address it returns has passed the check, and a name that resolves to
 * an internal address is reported as unknown unless {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS}
 * opens those ranges.
 */
public class RemoteCallDnsResolver extends SystemDefaultDnsResolver {

    private static final Lazy<Boolean> allowAccessToPrivateSubnets =
            Lazy.of(() -> Config.getBooleanProperty("ALLOW_ACCESS_TO_PRIVATE_SUBNETS", false));

    /**
     * Resolves the host and hands the client the addresses the address policy permits. The
     * policy decides which address families may be used, so the client only ever connects to
     * an address that passed the check.
     *
     * @param host the host name or address literal to resolve
     * @return the addresses the client may connect to
     * @throws UnknownHostException when the host cannot be resolved or none of its addresses
     *                              may be connected to
     */
    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {

        final InetAddress[] addresses = lookup(host);
        if (allowAccessToPrivateSubnets.get()) {
            return addresses;
        }
        final InetAddress[] permitted = permitted(addresses);
        if (permitted.length == 0) {
            SecurityLogger.logInfo(this.getClass(), "Refused to connect to host '" + host
                    + "': it resolves to a private or internal address");
            throw new UnknownHostException("Host '" + host
                    + "' refused: it resolves to a private or internal address");
        }
        return permitted;
    }

    /**
     * The system lookup of the host, before the policy is applied. Kept apart from
     * {@link #resolve(String)} so a test can supply the addresses a host resolves to.
     *
     * @param host the host name or address literal to resolve
     * @return every address the host resolves to
     * @throws UnknownHostException when the host cannot be resolved
     */
    protected InetAddress[] lookup(final String host) throws UnknownHostException {
        return super.resolve(host);
    }

    /**
     * The addresses of a resolved host the configured address policy permits. Kept apart from
     * {@link #resolve(String)} so a test can supply a fixed policy instead of the configured one.
     *
     * @param addresses every address the host resolved to
     * @return the addresses the client may connect to, empty when none may
     */
    protected InetAddress[] permitted(final InetAddress[] addresses) {
        return IPUtils.permittedAddresses(addresses);
    }
}
