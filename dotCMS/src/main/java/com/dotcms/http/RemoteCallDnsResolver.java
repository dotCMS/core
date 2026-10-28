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
     * Resolves the host and refuses the result when any of its addresses is internal.
     *
     * @param host the host name or address literal to resolve
     * @return the addresses the client may connect to
     * @throws UnknownHostException when the host cannot be resolved or resolves to an internal
     *                              address
     */
    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {

        final InetAddress[] addresses = super.resolve(host);
        if (allowAccessToPrivateSubnets.get()) {
            return addresses;
        }
        for (final InetAddress address : addresses) {
            if (IPUtils.isInternalAddress(address)) {
                SecurityLogger.logInfo(this.getClass(), "Refused to connect to host '" + host
                        + "': it resolves to a private or internal address");
                throw new UnknownHostException("Host '" + host
                        + "' refused: it resolves to a private or internal address");
            }
        }
        return addresses;
    }
}
