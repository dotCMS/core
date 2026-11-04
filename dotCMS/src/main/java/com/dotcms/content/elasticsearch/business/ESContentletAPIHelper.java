/**
 *
 */
package com.dotcms.content.elasticsearch.business;

import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A helper for {@link ESContentletAPIImpl}
 *
 * @author jsanca
 * @since 1.5
 *
 */
public class ESContentletAPIHelper implements Serializable {


    public final static ESContentletAPIHelper INSTANCE =
            new ESContentletAPIHelper();

    private ESContentletAPIHelper() {}

    /**
     * Generate the Notification for the can not delete scenario on the {@link ESContentletAPIImpl}
     * @param notificationAPI
     * @param userLocale
     * @param userId
     * @param iNode
     */
    public final void generateNotificationCanNotDelete (final NotificationAPI notificationAPI,
                                                  final Locale userLocale,
                                                  final String userId,
                                                  final String iNode) {

        final Locale locale = (null != userLocale)?
                userLocale: Locale.getDefault();
        I18NMessage titleMsg = null;
        I18NMessage errorMsg = null;
        String i18nErrorMsg  = null;

        try {

            titleMsg = new I18NMessage(
                    "notification.escontentelet.cannotdelete.info.title");

             errorMsg = new I18NMessage(
                    "notification.escontentelet.cannotdelete.info.message", null,
                    iNode); // message = Contentlet with Inode {0} cannot be deleted because it's not archived. Please archive it first before deleting it.

            i18nErrorMsg = LanguageUtil.get(locale,
                    errorMsg.getKey(),
                    errorMsg.getArguments());

            Logger.error(this, i18nErrorMsg);

            notificationAPI.generateNotification(
                    titleMsg, // title = Contentlet Notification
                    errorMsg,
                    null, // no actions
                    NotificationLevel.ERROR,
                    NotificationType.GENERIC,
                    userId,
                    locale
            );
        } catch (LanguageException | DotDataException e) {

            Logger.error(this, e.getMessage(), e);
        }

        throw new DotStateException(i18nErrorMsg);
    } // generateNotificationCanNotDelete.

    /**
     * Prepares the replacement of a contentlet's individual permissions with the requested ones.
     * <p>
     * A user who may edit the contentlet's permissions (see
     * {@link #canEditPermissions(PermissionAPI, Contentlet, User)}) gets a task that removes the
     * current individual permissions and saves the requested ones. A user who may not edit them
     * gets nothing to do when the requested list matches the current permissions, and a
     * {@link DotSecurityException} otherwise. The decision is taken here, before anything is
     * removed, so the returned task does not check permissions again.
     *
     * @param permissionAPI the permission API used to read and write the permissions
     * @param contentlet    the saved contentlet whose permissions are replaced
     * @param requested     the permissions requested for the contentlet
     * @param user          the user asking for the change, may be {@code null}
     * @param systemUser    the user the returned task saves the permissions with
     * @return the replacement task, or empty when there is nothing to change
     * @throws DotDataException     if the current permissions cannot be read
     * @throws DotSecurityException if the user may not edit the permissions and the list differs
     */
    public final Optional<Runnable> permissionReplacement(final PermissionAPI permissionAPI,
                                                          final Contentlet contentlet,
                                                          final List<Permission> requested,
                                                          final User user,
                                                          final User systemUser)
            throws DotDataException, DotSecurityException {

        if (this.canEditPermissions(permissionAPI, contentlet, user)) {

            return Optional.of(() -> this.replacePermissions(permissionAPI, contentlet, requested, systemUser));
        }

        this.requireNoChange(permissionAPI, contentlet, requested, user);
        return Optional.empty();
    } // permissionReplacement.

    /**
     * Lets a user who may not edit a contentlet's permissions go on only when the requested list
     * matches the contentlet's current permissions. Otherwise the refusal is written to the
     * security log and a {@link DotSecurityException} is thrown; nothing is removed or saved.
     *
     * @param permissionAPI the permission API used to read the current permissions
     * @param contentlet    the contentlet whose permissions would change
     * @param requested     the permissions requested for the contentlet
     * @param user          the user asking for the change, may be {@code null}
     * @throws DotDataException     if the current permissions cannot be read
     * @throws DotSecurityException if the requested list differs from the current permissions
     */
    private void requireNoChange(final PermissionAPI permissionAPI,
                                 final Contentlet contentlet,
                                 final List<Permission> requested,
                                 final User user) throws DotDataException, DotSecurityException {

        if (this.hasSamePermissions(requested, permissionAPI.getPermissions(contentlet))) {

            return;
        }

        final String userId = null != user ? user.getUserId() : "N/A";
        final String message = "User id: " + userId
                + " does not have permission to alter permissions on asset " + contentlet.getPermissionId();
        SecurityLogger.logInfo(ESContentletAPIHelper.class, message);
        throw new DotSecurityException(message);
    } // requireNoChange.

    /**
     * Removes the contentlet's individual permissions and saves the requested ones against the
     * contentlet's permission id, as the system user. The caller has already decided that the
     * change is allowed. A failure is logged and not rethrown.
     *
     * @param permissionAPI the permission API used to remove and save the permissions
     * @param contentlet    the contentlet whose permissions are replaced
     * @param requested     the permissions requested for the contentlet
     * @param systemUser    the user the permissions are saved with
     */
    private void replacePermissions(final PermissionAPI permissionAPI,
                                    final Contentlet contentlet,
                                    final List<Permission> requested,
                                    final User systemUser) {

        try {

            Logger.debug(ESContentletAPIHelper.class,
                    () -> "Removing the permissions for: " + contentlet.getTitle() +
                            ", id: " + contentlet.getIdentifier());
            permissionAPI.removePermissions(contentlet);
            Logger.debug(ESContentletAPIHelper.class,
                    () -> "Saving the permissions for: " + contentlet.getTitle() +
                            ", id: " + contentlet.getIdentifier());
            permissionAPI.save(requested.stream()
                            .map(permission -> new Permission(contentlet.getPermissionId(),
                                    permission.getRoleId(), permission.getPermission()))
                            .collect(Collectors.toList()), contentlet, systemUser, false);
        } catch (Exception e) {

            Logger.error(ESContentletAPIHelper.class,
                    "Could not save the permissions for: " + contentlet.getTitle() +
                            ", id: " + contentlet.getIdentifier() + ", msg: "
                            + e.getMessage(), e);
        }
    } // replacePermissions.

    /**
     * Tells whether a user may change the permissions of a contentlet: the contentlet is being
     * copied, or the user has EDIT_PERMISSIONS on the contentlet (front-end roles count) or on its
     * content type. This is the same rule {@link PermissionAPI#save(Collection,
     * com.dotmarketing.business.Permissionable, User, boolean)} applies.
     *
     * @param permissionAPI the permission API used for the check
     * @param contentlet    the contentlet whose permissions would change
     * @param user          the user asking for the change, may be {@code null}
     * @return {@code true} if the user may change the contentlet's permissions
     * @throws DotDataException if the permissions cannot be read
     */
    public final boolean canEditPermissions(final PermissionAPI permissionAPI,
                                            final Contentlet contentlet,
                                            final User user) throws DotDataException {

        if (contentlet.getBoolProperty(Contentlet.IS_COPY)) {

            return true;
        }

        if (null == user) {

            return false;
        }

        return permissionAPI.doesUserHavePermission(contentlet,
                        PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user)
                || permissionAPI.doesUserHavePermission(contentlet.getContentType(),
                        PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
    } // canEditPermissions.

    /**
     * Tells whether two permission lists grant the same bits to the same roles. Order, repeated
     * entries and how a role's bits are split across entries do not matter, and a role with no
     * bits counts as absent. A {@code null} list counts as empty.
     *
     * @param requested the requested permissions
     * @param current   the current permissions
     * @return {@code true} if both lists grant the same bits to the same roles
     */
    public final boolean hasSamePermissions(final Collection<Permission> requested,
                                            final Collection<Permission> current) {

        return this.bitsByRole(requested).equals(this.bitsByRole(current));
    } // hasSamePermissions.

    /**
     * Collects the bits each role is granted in a permission list, leaving out roles with no bits.
     *
     * @param permissions the permission list, may be {@code null}
     * @return the bits granted to each role
     */
    private Map<String, Integer> bitsByRole(final Collection<Permission> permissions) {

        final Map<String, Integer> bitsByRole = new HashMap<>();
        if (null != permissions) {

            for (final Permission permission : permissions) {

                if (null != permission) {

                    bitsByRole.merge(permission.getRoleId(), permission.getPermission(),
                            (left, right) -> left | right);
                }
            }
        }

        bitsByRole.values().removeIf(bits -> bits == 0);
        return bitsByRole;
    } // bitsByRole.

} // E:O:F:ESContentletAPIHelper.
