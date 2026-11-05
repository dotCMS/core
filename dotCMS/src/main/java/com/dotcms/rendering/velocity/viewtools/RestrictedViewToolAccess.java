package com.dotcms.rendering.velocity.viewtools;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import javax.servlet.http.HttpServletRequest;
import com.liferay.portal.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
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
     * Request attribute carrying the user that last modified the content whose code is being
     * rendered.
     *
     * <p>It is set by whoever renders that code, which is the only party that knows what content
     * it is. A view tool must take the author from here and never from the Velocity resource
     * name: {@code #parse} pushes the template author's own literal argument as the current
     * template name, so a tool that reads that name is asking the caller to vouch for itself.
     *
     * <p>Templates do reach {@code $request} and can call {@code setAttribute} on it, so the
     * value stored here is a private wrapper type only this class creates: anything else found
     * under this attribute is not an announcement and never counts. Compare
     * {@link ScriptingFileAttribute}, which guards the file-based scripting endpoints the same
     * way.
     */
    public static final String RENDERING_AUTHOR_ID = "DOT_RENDERING_AUTHOR_ID";

    /**
     * The announcement itself. Private so that only {@link #announceRenderingAuthor} can make
     * one; a {@code String} a template pushes under {@link #RENDERING_AUTHOR_ID} is ignored.
     *
     * @param userId ID of the user that last modified the content being rendered, {@code null}
     *               when the renderer knows the content but not its author
     */
    private record RenderingAuthor(String userId) { }

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
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> "Unable to resolve the author of '" + velocityResourcePath + "': "
                            + ExceptionUtil.getErrorMessage(e));
        }
        return authorOfFileAsset(velocityResourcePath);
    }

    /**
     * Resolves who last saved the file asset a resource path points at.
     *
     * <p>{@code #dotParse} hands the renderer the asset's own location on disk, such as
     * {@code /data/shared/assets/c/e/ce83.../fileAsset/header.vtl}, which carries the file's
     * inode but is not one of the mode-prefixed resource names. The VTL inside that file is
     * code its editor wrote, so without this every {@code #dotParse}d file -- themes, headers,
     * footers, anything under {@code /application/vtl} -- would run with no author at all.
     *
     * @param velocityResourcePath the resource path the renderer resolved
     *
     * @return the ID of the user that last saved that file, or {@code null} when the path holds
     * no inode or nothing is stored under it
     */
    private static String authorOfFileAsset(final String velocityResourcePath) {
        try {
            final Optional<String> inode = UUIDUtil.findInode(velocityResourcePath);
            if (inode.isEmpty()) {
                return null;
            }
            final Contentlet file = APILocator.getContentletAPI()
                    .find(inode.get(), APILocator.systemUser(), false);
            return null != file ? file.getModUser() : null;
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> "Unable to resolve the file asset author of '" + velocityResourcePath
                            + "': " + ExceptionUtil.getErrorMessage(e));
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
        request.setAttribute(RENDERING_AUTHOR_ID, new RenderingAuthor(authorId));
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
     * <p>When no renderer announced anything, the file-based scripting endpoints
     * ({@code /api/vtl}, {@code /api/js}) are asked instead: they bind the script file's editor
     * through {@link ScriptingFileAttribute}, which answers the same question for code that is
     * run straight from a file rather than rendered from a content item. An explicit
     * announcement always wins over that, so a container parsed from inside such a file is
     * judged by the container's own author.
     *
     * @param request the request the tool was initialised with
     *
     * @return {@code true} when the render named an author who has access
     */
    public static boolean renderingAuthorHasAccess(final HttpServletRequest request) {
        if (null == request) {
            return false;
        }
        final Object announced = request.getAttribute(RENDERING_AUTHOR_ID);
        if (null != announced && !(announced instanceof RenderingAuthor)) {
            // only this class makes announcements, so somebody else wrote here: the code being
            // rendered can, and overwriting must not send the decision anywhere else
            Logger.warn(RestrictedViewToolAccess.class,
                    "A view tool was refused: the announcement of the author was overwritten by "
                            + "something that is not a renderer, so nothing on this request says "
                            + "who wrote the code being run.");
            return false;
        }
        final String authorId = null != announced
                ? ((RenderingAuthor) announced).userId()
                : ScriptingFileAttribute.get(request);
        if (!UtilMethods.isSet(authorId)) {
            Logger.warn(RestrictedViewToolAccess.class,
                    "A view tool was called while no author was announced for the code being "
                            + "rendered, so it is refused. This is what a render path that does "
                            + "not announce its author looks like.");
            return false;
        }
        final boolean allowed = isScriptingDeveloper(authorId);
        if (!allowed) {
            Logger.warn(RestrictedViewToolAccess.class, String.format(
                    "A view tool was refused: the code being rendered was last saved by user "
                            + "'%s', who does not have the %s role",
                    authorId, Role.SCRIPTING_DEVELOPER));
        }
        return allowed;
    }

    /**
     * Whether a user was given the {@link Role#SCRIPTING_DEVELOPER} role.
     *
     * <p>This is deliberately narrower than {@link #hasAccess(User)}: that one decides whether a
     * tool is worth putting in the toolbox at all, while this one decides whether the tool runs,
     * and the tools have always asked for this one role. Being a CMS Administrator does not
     * stand in for it, because the code these tools run is served to whoever visits the page —
     * admitting any administrator would let an anonymous visitor read a secret through a page an
     * administrator merely happened to edit last.
     *
     * @param userId ID of the user to check, as the renderer announced it
     *
     * @return {@code true} when that user holds the role
     */
    private static boolean isScriptingDeveloper(final String userId) {
        try {
            final User author = APILocator.getUserAPI()
                    .loadUserById(userId, APILocator.systemUser(), true);
            if (null == author || author.isAnonymousUser()
                    || UserAPI.SYSTEM_USER_ID.equals(author.getUserId())) {
                return false;
            }
            final Role scripting = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
            return null != scripting && APILocator.getRoleAPI().doesUserHaveRole(author, scripting);
        } catch (final Exception e) {
            Logger.debug(RestrictedViewToolAccess.class.getName(),
                    () -> "Unable to check the " + Role.SCRIPTING_DEVELOPER + " role of user '"
                            + userId + "': " + ExceptionUtil.getErrorMessage(e));
            return false;
        }
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
