package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides whether the code of a content item may use the view tools that list the file system or
 * that work outside of the viewer's content permissions.
 * <p>
 * Access depends on who wrote the code, not on who reads the page: it is granted when the user
 * that last modified the content is a CMS Administrator or has the
 * {@link Role#SCRIPTING_DEVELOPER} role, and denied when that user is unknown. Note that "last
 * modified" is what the content records: a privileged user who edits or publishes content written
 * by someone else becomes its author for this purpose. Tools that check the viewer's permissions
 * themselves, like {@code $filetool}, do not need this class.
 */
public final class RestrictedViewToolAccess {

    /**
     * Comma-separated list of the toolbox keys that are removed from the Velocity context when
     * content fields are rendered and their author lacks access (see
     * {@link #restrictedToolKeys()}).
     */
    public static final String RESTRICTED_TOOLS_PROPERTY = "VELOCITY_FIELD_RENDER_RESTRICTED_VIEWTOOLS";

    private static final String[] DEFAULT_RESTRICTED_TOOLS = {"filetool", "macro", "mailer"};

    private RestrictedViewToolAccess() {
    }

    /**
     * @return the toolbox keys that are not available when content fields are rendered and the
     * content's author lacks access.
     */
    public static Set<String> restrictedToolKeys() {
        final String[] keys = Config.getStringArrayProperty(RESTRICTED_TOOLS_PROPERTY,
                DEFAULT_RESTRICTED_TOOLS);
        if (null == keys) {
            return Collections.emptySet();
        }
        return Arrays.stream(keys).filter(UtilMethods::isSet).map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * @param userId the ID of the user that last modified the content
     *
     * @return {@code true} if that user has access.
     */
    public static boolean userIdHasAccess(final String userId) {
        if (!UtilMethods.isSet(userId)) {
            return false;
        }
        try {
            return hasAccess(APILocator.getUserAPI()
                    .loadUserById(userId, APILocator.systemUser(), true));
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> "Unable to load user '" + userId + "': " + ExceptionUtil.getErrorMessage(e));
            return false;
        }
    }

    /**
     * @param user the {@link User} to check
     *
     * @return {@code true} if the user is a CMS Administrator or has the Scripting Developer role.
     * The system user does not count: content saved by it does not
     * tell who wrote the code.
     */
    public static boolean hasAccess(final User user) {
        if (null == user || user.isAnonymousUser() || UserAPI.SYSTEM_USER_ID.equals(user.getUserId())) {
            return false;
        }
        try {
            if (user.isAdmin()) {
                return true;
            }
            final Role scripting = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
            return null != scripting && APILocator.getRoleAPI().doesUserHaveRole(user, scripting);
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> "Unable to check roles of user '" + user.getUserId() + "': "
                            + ExceptionUtil.getErrorMessage(e));
            return false;
        }
    }

}
