package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import javax.servlet.http.HttpServletRequest;
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

    /**
     * Request attribute carrying the ID of the user that last modified the content whose code is
     * being rendered.
     *
     * <p>It is set by whoever renders that code, which is the only party that knows what content
     * it is. A view tool must take the author from here and never from the Velocity resource
     * name: {@code #parse} pushes the template author's own literal argument as the current
     * template name, so a tool that reads that name is asking the caller to vouch for itself.
     * Velocity templates cannot write request attributes, so this value is not theirs to choose.
     */
    public static final String RENDERING_AUTHOR_ID = "DOT_RENDERING_AUTHOR_ID";

    /**
     * Kept in alphabetical order. Besides the tools that work outside of the viewer's
     * permissions, this covers every tool that used to decide for itself whether its caller may
     * run by reading the Velocity resource name, which the template author chooses.
     */
    private static final String[] DEFAULT_RESTRICTED_TOOLS =
            {"dotsecrets", "filetool", "macro", "mailer", "sql", "xslttool"};

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
     * Resolves who wrote the code held by a Velocity resource that is about to be rendered.
     *
     * <p>Containers and templates carry Velocity of their own, written by whoever last saved
     * them, and that is what this answers. It reads the identifier out of the resource path and
     * loads the object, so the answer describes the code that will actually run; a path naming
     * something else resolves to that something else, or to nothing, and an author that cannot
     * be resolved denies access rather than granting it.
     *
     * @param velocityResourcePath the resource path the renderer resolved, for example
     *                             {@code /LIVE/{containerId}/1.container}
     *
     * @return the ID of the user that last saved that container or template, or {@code null}
     * when the resource is of another kind or cannot be loaded
     */
    public static String authorOfRenderedResource(final String velocityResourcePath) {
        if (!UtilMethods.isSet(velocityResourcePath)) {
            return null;
        }
        try {
            final VelocityResourceKey key = new VelocityResourceKey(velocityResourcePath);
            final User system = APILocator.systemUser();
            if (VelocityType.CONTAINER == key.type) {
                final Container container = key.mode.showLive
                        ? APILocator.getContainerAPI().getLiveContainerById(key.id1, system, false)
                        : APILocator.getContainerAPI().getWorkingContainerById(key.id1, system, false);
                return null != container ? container.getModUser() : null;
            }
            if (VelocityType.TEMPLATE == key.type) {
                final Template template = key.mode.showLive
                        ? APILocator.getTemplateAPI().findLiveTemplate(key.id1, system, false)
                        : APILocator.getTemplateAPI().findWorkingTemplate(key.id1, system, false);
                return null != template ? template.getModUser() : null;
            }
            return null;
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> "Unable to resolve the author of '" + velocityResourcePath + "': "
                            + ExceptionUtil.getErrorMessage(e));
            return null;
        }
    }

    /**
     * Records on the request who wrote the code that is about to be rendered.
     *
     * <p>Only the render knows what content it is rendering, so only the render can answer this;
     * a view tool asking the Velocity resource name is asking the caller to vouch for itself.
     * Renders nest — a page renders a container which renders a field — so the previous value is
     * handed back and must be put back with {@link #restoreRenderingAuthor} once the inner render
     * is done.
     *
     * @param request  the request the render is running under; {@code null} is ignored
     * @param authorId ID of the user that last modified the content being rendered
     *
     * @return whatever was recorded before, to restore afterwards
     */
    public static Object announceRenderingAuthor(final HttpServletRequest request,
            final String authorId) {
        if (null == request) {
            return null;
        }
        final Object previous = request.getAttribute(RENDERING_AUTHOR_ID);
        request.setAttribute(RENDERING_AUTHOR_ID, authorId);
        return previous;
    }

    /**
     * Puts back the author that {@link #announceRenderingAuthor} replaced.
     *
     * @param request  the request the render is running under; {@code null} is ignored
     * @param previous the value that call handed back
     */
    public static void restoreRenderingAuthor(final HttpServletRequest request,
            final Object previous) {
        if (null == request) {
            return;
        }
        if (null == previous) {
            request.removeAttribute(RENDERING_AUTHOR_ID);
        } else {
            request.setAttribute(RENDERING_AUTHOR_ID, previous);
        }
    }

    /**
     * Reads the author of the content being rendered from the request and checks their access.
     *
     * <p>The value is put there by whoever renders that content, under
     * {@link #RENDERING_AUTHOR_ID}. A view tool must use this and never the Velocity resource
     * name: {@code #parse} pushes the template author's own argument as the current template
     * name, so a tool that reads that name lets the caller name the user it will be checked
     * against. A request whose author is missing is denied, because an unknown author is not
     * evidence of anything.
     *
     * @param request the request the tool was initialised with
     *
     * @return {@code true} when the render named an author who has access
     */
    public static boolean renderingAuthorHasAccess(final HttpServletRequest request) {
        if (null == request) {
            return false;
        }
        final Object authorId = request.getAttribute(RENDERING_AUTHOR_ID);
        if (!(authorId instanceof String) || !UtilMethods.isSet((String) authorId)) {
            Logger.warn(RestrictedViewToolAccess.class,
                    "A view tool was called while no author was announced for the code being "
                            + "rendered, so it is refused. This is what a render path that does "
                            + "not announce its author looks like.");
            return false;
        }
        final boolean allowed = userIdHasAccess((String) authorId);
        if (!allowed) {
            Logger.warn(RestrictedViewToolAccess.class, String.format(
                    "A view tool was refused: the code being rendered was last saved by user "
                            + "'%s', who is neither a CMS Administrator nor a %s",
                    authorId, Role.SCRIPTING_DEVELOPER));
        }
        return allowed;
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
