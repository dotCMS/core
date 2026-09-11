package com.dotcms.publisher.business;

import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * {@code false} here reproduces what the portlet's old 3-arg
     * {@code doesUserHavePermission(p, PERMISSION_PUBLISH, user)} call did, even though that
     * overload passes {@code respectFrontendRoles = true}. The two APIs do not interpret the flag
     * the same way:
     * <ul>
     *   <li>The scalar path ({@code PermissionBitAPIImpl.filterUserRoles}) seeds the role set from
     *       {@code loadRolesForUser} and, when the flag is {@code false}, only <b>removes</b> the
     *       anonymous and Logged-In-Site roles. It never adds a role the user does not hold.</li>
     *   <li>The batched path <b>adds</b> both of those roles unconditionally when the flag is
     *       {@code true}.</li>
     * </ul>
     * So passing {@code true} here would <em>widen</em> access: a backend-only user would inherit
     * any grant made to Logged In Site User and could see - and delete queue entries for - bundles
     * that were previously invisible to them. Passing {@code false} leaves the role set as
     * "exactly the roles this user holds", which is what the scalar call effectively resolved to.
     */
    private static final boolean RESPECT_FRONTEND_ROLES = false;

    /** What {@link PermissionableProxy#setType(String)} maps {@code "folder"} to. */
    private static final String FOLDER_PERMISSION_TYPE =
            PermissionAPI.PermissionableType.FOLDERS.getCanonicalName();

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

        // Folders carry no PUBLISH bit - the scalar doesUserHavePermission substitutes EDIT for
        // them ("Folders do not have PUBLISH, use EDIT instead", PermissionBitAPIImpl:196-201).
        // filterCollection takes one permission type for the whole collection and never inspects
        // the permissionable's type, so the collection is split and each half asked for the bit
        // that actually applies. Still batched: two queries at most, not one per bundle.
        final List<PermissionableProxy> folderProxies = new ArrayList<>();
        final List<PermissionableProxy> otherProxies = new ArrayList<>();

        for (final PermissionableProxy proxy : proxiesByPermissionId.values()) {
            if (FOLDER_PERMISSION_TYPE.equals(proxy.getPermissionType())) {
                folderProxies.add(proxy);
            } else {
                otherProxies.add(proxy);
            }
        }

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final List<PermissionableProxy> allowed = new ArrayList<>();

        if (!otherProxies.isEmpty()) {
            allowed.addAll(permissionAPI.filterCollection(
                    otherProxies,
                    PermissionAPI.PERMISSION_PUBLISH,
                    user,
                    RESPECT_FRONTEND_ROLES));
        }

        if (!folderProxies.isEmpty()) {
            allowed.addAll(permissionAPI.filterCollection(
                    folderProxies,
                    PermissionAPI.PERMISSION_EDIT,
                    user,
                    RESPECT_FRONTEND_ROLES));
        }

        for (final PermissionableProxy proxy : allowed) {
            permitted.addAll(
                    bundlesByPermissionId.getOrDefault(proxy.getPermissionId(), Set.of()));
        }

        return permitted;
    }
}
