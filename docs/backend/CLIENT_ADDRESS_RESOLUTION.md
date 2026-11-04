# Client Address Resolution

API token network limits (`allowNetwork`), security and access log lines, the location-based
rules conditionlets and geolocation take the client's address from `request.getRemoteAddr()`.
New code that needs the client address must do the same and must
not read `X-Forwarded-For`, `X-Real-IP`, `Forwarded` or similar headers itself; a client can put
any value in them.

Some existing code does read the raw header. The request header conditionlet
(`RequestHeaderConditionlet`) can compare a rule against `X-Forwarded-For` as sent, the temporary
file request fingerprint (`TempFileAPI.getRequestFingerprint`) includes it, and
`RequestUtil.createStatelessRequest` copies it onto the request copy. The valve settings below
do not change what those paths see, so a value read there is whatever the client or the proxies
put in the header, not a resolved client address.

Behind a load balancer the connecting address is the load balancer's, so Tomcat's
`RemoteIpValve` (declared in `dotCMS/src/main/resources/container/tomcat9/conf/server.xml`)
replaces it with the client address carried in `X-Forwarded-For`, but only when the connection
comes from an address the valve trusts as a proxy.

## How the valve picks the address

The valve reads `X-Forwarded-For` from right to left. An entry that matches
`internalProxies` or `trustedProxies` is treated as a proxy and skipped; the first entry that
matches neither becomes `getRemoteAddr()`. If the connecting address itself matches neither
list, the header is ignored and the connecting address is used.

A load balancer that appends the client address to `X-Forwarded-For` keeps any value the
client sent to the left of the appended address. The valve stops at the appended address, and
never reaches the client's values, only when the appended address matches neither
`internalProxies` nor `trustedProxies`. If the client itself connects from an address that
matches one of the lists, for example a user on an internal network covered by the default
private ranges, the valve skips the appended address as if it were a proxy and takes the next
entry, which is a value the client sent. Keep both lists limited to the proxies you actually run,
or have the edge proxy replace the header rather than append to it.

A load balancer or network path that passes the header through unchanged, without appending,
leaves the client's value as the only entry, and the valve uses it if the connecting address is
trusted.

## Settings

Both are environment variables read by `server.xml`. The Docker image's `setenv.sh` exports
the `CMS_REMOTEIP_INTERNALPROXIES` default, so set the variable in the container environment
rather than editing either file.

| Variable | Default | Meaning |
|---|---|---|
| `CMS_REMOTEIP_INTERNALPROXIES` | Every private range: `10/8`, `127/8`, `172.16/12`, `192.168/16`, `169.254/16`, `100.64/10`, and `::1` | Regular expression for the connecting addresses whose `X-Forwarded-For` is believed. |
| `CMS_REMOTEIP_TRUSTEDPROXIES` | Empty | Regular expression for further proxy addresses, usually public ones such as a CDN, whose entries in `X-Forwarded-For` are skipped. |
| `CMS_REMOTEIP_REMOTEIPHEADER` | `x-forwarded-for` | Header that carries the client address. |

The patterns are Java regular expressions matched against the whole address. Write repeat counts
as `\d\d?\d?` rather than `\d{1,3}` inside `server.xml`, because Tomcat's `${...}` resolver
treats the braces as the end of the variable. In an environment variable either form works.

## Setting it behind a load balancer

The default trusts any peer on a private network, and any trusted peer can choose the address
dotCMS records and checks API token network limits against, simply by sending
`X-Forwarded-For`. That suits a deployment where the only thing that can reach dotCMS from a
private address is its own load balancer or ingress, and the default is kept so that such
deployments resolve the client address without extra configuration. Where other
clients can connect from a private address (users on the same internal network, other workloads
in the cluster, a Kubernetes service or layer-4 load balancer that rewrites the source address
without adding `X-Forwarded-For`), narrow the list to the proxies you run that either replace
`X-Forwarded-For` or append the address they received the connection from. Leave out a proxy
that only rewrites the source address and passes the header through unchanged, even one you run:
it cannot tell dotCMS the original client address, and trusting it lets the client's own header
value through.

```bash
# Only the ingress controller pods' subnet is a proxy
export CMS_REMOTEIP_INTERNALPROXIES='10\.42\.7\.\d+'
```

With a CDN in front of the load balancer, list the CDN's address ranges as trusted proxies so
the client address, not the CDN's, is used:

```bash
export CMS_REMOTEIP_TRUSTEDPROXIES='203\.0\.113\.\d+|198\.51\.100\.\d+'
```

To check the result, compare the address in the access log
(`%{org.apache.catalina.AccessLog.RemoteAddr}r` in the default pattern, which is the resolved
address) with the client's real address.

## Tests

`dotCMS/src/test/java/com/dotcms/util/network/ClientAddressResolutionTest.java` runs the valve
with the attributes read from the shipped `server.xml` and checks that the `setenv.sh` default
matches it. Update both files and the test together.
