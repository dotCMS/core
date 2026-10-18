package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.CMSUrlUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;

/**
 * Decides whether Velocity code may use the view tools and tool methods that expose server
 * configuration or the file system, or that work outside of the caller's content permissions.
 * <p>
 * Access is granted when either of these users is a CMS Administrator or has the
 * {@link Role#SCRIPTING_DEVELOPER} role:
 * <ol>
 *     <li>The user that last modified the content, container or template whose code is being
 *     rendered (resolved from the current Velocity template name, which code being rendered
 *     cannot change).</li>
 *     <li>The user making the HTTP request.</li>
 * </ol>
 */
public final class RestrictedViewToolAccess {

    /**
     * Comma-separated list of the toolbox keys that are removed from the Velocity context when
     * content fields are rendered on behalf of users without access (see
     * {@link #restrictedToolKeys()}).
     */
    public static final String RESTRICTED_TOOLS_PROPERTY = "VELOCITY_FIELD_RENDER_RESTRICTED_VIEWTOOLS";

    private static final String[] DEFAULT_RESTRICTED_TOOLS = {"config", "filetool", "macro", "mailer"};

    private RestrictedViewToolAccess() {
    }

    /**
     * @return the toolbox keys that are not available when content fields are rendered on
     * behalf of a content author and a caller that both lack access.
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
     * Checks the author of the code currently being rendered and then the user of the request.
     *
     * @param context The Velocity context being rendered; may be {@code null}.
     * @param request The current request; may be {@code null}.
     *
     * @return {@code true} if either user has access.
     */
    public static boolean hasAccess(final Context context, final HttpServletRequest request) {
        return currentTemplateAuthorHasAccess(context) || requestUserHasAccess(request);
    }

    /**
     * @return {@code true} if the user logged in to the given request has access.
     */
    public static boolean requestUserHasAccess(final HttpServletRequest request) {
        if (null == request) {
            return false;
        }
        try {
            return hasAccess(WebAPILocator.getUserWebAPI().getUser(request));
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> "Unable to resolve the request user: " + ExceptionUtil.getErrorMessage(e));
            return false;
        }
    }

    /**
     * @return {@code true} if the user with the given ID has access.
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
     * @return {@code true} if the user is a CMS Administrator or has the Scripting Developer role.
     */
    public static boolean hasAccess(final User user) {
        if (null == user || user.isAnonymousUser()) {
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

    /**
     * Resolves the content, container or template being rendered from the current Velocity
     * template name and checks the user that last modified it.
     */
    static boolean currentTemplateAuthorHasAccess(final Context context) {
        if (null == context) {
            return false;
        }
        final String resourcePath = new InternalContextAdapterImpl(context).getCurrentTemplateName();
        if (!UtilMethods.isSet(resourcePath)) {
            return false;
        }
        try {
            final String inode = CMSUrlUtil.getInstance().getInodeFromUrlPath(resourcePath);
            if (!UtilMethods.isSet(inode)) {
                return false;
            }
            Versionable versionable = APILocator.getContentletAPI()
                    .find(inode, APILocator.systemUser(), true);
            if (null == versionable) {
                versionable = APILocator.getContainerAPI()
                        .getLiveContainerById(inode, APILocator.systemUser(), true);
            }
            if (null == versionable) {
                versionable = APILocator.getTemplateAPI()
                        .findLiveTemplate(inode, APILocator.systemUser(), true);
            }
            return null != versionable && userIdHasAccess(versionable.getModUser());
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> String.format("Unable to resolve the author of '%s': %s", resourcePath,
                            ExceptionUtil.getErrorMessage(e)));
            return false;
        }
    }

}
