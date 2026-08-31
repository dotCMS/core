package com.dotmarketing.webdav;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.property.PropertyAuthoriser;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

/**
 * Decides whether a WebDAV request may read or write a resource's properties.
 *
 * <p>The decision is the resource's, exactly as in the library default: ask
 * {@link Resource#authorise(Request, Method, com.bradmcevoy.http.Auth)} and report a refusal for
 * each property the request named. What differs is an empty request.
 *
 * <p>The library builds its answer by iterating the property names, so a request that names none
 * produces an empty set of refusals. The caller reads that set as the list of problems found and
 * treats empty as "no problems", which means the resource's answer decides nothing at all for such
 * a request: it is refused and handled anyway. A PROPPATCH whose body sets no property is the case
 * that reaches this, and the property implementations dotCMS registers make that body a no-op, but
 * a permission check whose result depends on how much the request asked for is not one worth
 * relying on. This reports the refusal either way, naming the resource itself when the request
 * named nothing.
 */
public class WebdavPropertyAuthoriser implements PropertyAuthoriser {

    /**
     * Stands in for the property list when a request named none, so that a refusal is never
     * reported as an empty set. The name is the DAV element that would have held them.
     */
    private static final QName NO_PROPERTY_NAMED = new QName("DAV:", "prop");

    @Override
    public Set<CheckResult> checkPermissions(final Request request, final Method method,
            final PropertyPermission permission, final Set<QName> fields, final Resource resource) {

        if (resource.authorise(request, method, request.getAuthorization())) {
            // Null rather than an empty set, matching what the caller expects of a clean check.
            return null;
        }

        final Set<QName> refused = (fields == null || fields.isEmpty())
                ? Set.of(NO_PROPERTY_NAMED)
                : fields;

        return refused.stream()
                .map(field -> new CheckResult(field, Status.SC_UNAUTHORIZED, "Not authorised", resource))
                .collect(Collectors.toSet());
    }
}
