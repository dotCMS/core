package com.dotcms.publisher.business;

import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resolves which publishing-queue bundles a user may publish, in a single batched permission
 * query.
 * <p>
 * The Publishing Queue portlet previously asked {@code doesUserHavePermission} once per bundle -
 * an N+1 pattern that ADR-0020 identifies as a design defect. This collapses that into one
 * {@link PermissionAPI#filterCollection(Collection, int, User, boolean)} call.
 * <p>
 * Two behaviours are preserved deliberately, because changing either would alter who can see which
 * bundles:
 * <ul>
 *   <li>A bundle's visibility is judged from its <b>first</b> queue element alone - the loop this
 *       replaces broke after one element.</li>
 *   <li>{@code respectFrontendRoles} is {@code true}, matching the three-argument
 *       {@code doesUserHavePermission} overload the portlet used to call.</li>
 * </ul>
 * Extracted from the JSP so this rule is unit-testable; the portlet holds no permission logic of
 * its own.
 *
 * @see <a href="https://github.com/dotCMS/core/issues/36861">Issue #36861</a>
 */
public final class PublishQueuePermissionFilter {

    /** Matches the 3-arg {@code doesUserHavePermission} overload the portlet used to call. */
    private static final boolean RESPECT_FRONTEND_ROLES = true;

    private PublishQueuePermissionFilter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns the ids of the bundles the user may publish.
     *
     * @param firstElementByBundle bundle id to that bundle's first queue element. A bundle with no
     *                             element cannot be judged and is simply absent from the result,
     *                             which callers must treat as "not permitted" rather than as an
     *                             error - see issue #36861, where a missing map entry NPE'd the
     *                             whole page render.
     * @param user                 the user whose access is being checked
     * @return the subset of bundle ids the user holds PUBLISH permission on; never null
     */
    public static Set<String> permittedBundleIds(
            final Map<String, PublishQueueElement> firstElementByBundle,
            final User user) throws DotDataException, DotSecurityException {

        final Set<String> permitted = new HashSet<>();

        if (firstElementByBundle == null || firstElementByBundle.isEmpty()) {
            return permitted;
        }

        // De-duplicate by permission id: the same asset can head more than one bundle (exactly the
        // #36861 scenario), and filterCollection decides per permission id, so sending duplicates
        // would only repeat work.
        final Map<String, PermissionableProxy> proxiesByPermissionId = new LinkedHashMap<>();
        final Map<String, Set<String>> bundlesByPermissionId = new HashMap<>();

        for (final Map.Entry<String, PublishQueueElement> entry : firstElementByBundle.entrySet()) {
            final PublishQueueElement element = entry.getValue();

            if (element == null || !UtilMethods.isSet(element.getAsset())) {
                continue;
            }

            final PermissionableProxy proxy = new PermissionableProxy();
            proxy.setIdentifier(element.getAsset());
            proxy.setType(element.getType());
            proxy.setInode(element.getAsset());

            final String permissionId = proxy.getPermissionId();
            proxiesByPermissionId.putIfAbsent(permissionId, proxy);
            bundlesByPermissionId
                    .computeIfAbsent(permissionId, key -> new HashSet<>())
                    .add(entry.getKey());
        }

        if (proxiesByPermissionId.isEmpty()) {
            return permitted;
        }

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Collection<PermissionableProxy> allowed = permissionAPI.filterCollection(
                proxiesByPermissionId.values(),
                PermissionAPI.PERMISSION_PUBLISH,
                user,
                RESPECT_FRONTEND_ROLES);

        for (final PermissionableProxy proxy : allowed) {
            permitted.addAll(
                    bundlesByPermissionId.getOrDefault(proxy.getPermissionId(), Set.of()));
        }

        return permitted;
    }
}
