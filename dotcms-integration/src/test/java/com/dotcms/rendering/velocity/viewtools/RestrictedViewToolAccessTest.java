package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the view tools available when content fields are rendered through
 * {@link RenderFieldStrategy} (e.g. GraphQL {@code _map(render:true)}).
 */
public class RestrictedViewToolAccessTest {

    private static final String ALLOWED_PROPERTY = "VELOCITY_PUBLIC_RENDER_TEST_VALUE";
    private static final String ALLOWED_VALUE = "allowed-" + System.currentTimeMillis();
    private static final String OTHER_PROPERTY = "DOT_RENDER_TEST_OTHER";
    private static final String OTHER_VALUE = "other-" + System.currentTimeMillis();

    /**
     * A: allowed property through {@code $config}; B: other property through {@code $config};
     * C and D: the same two through {@code $webapi.getConfigVar}; E: the WEB-INF folder;
     * F: whether {@code $macro} is available at all; G: a default-list property.
     */
    private static final String FIELD_CODE = "[A:$!config.getStringProperty(\"" + ALLOWED_PROPERTY + "\")]"
            + "[B:$!config.getStringProperty(\"" + OTHER_PROPERTY + "\")]"
            + "[C:$!webapi.getConfigVar(\"" + ALLOWED_PROPERTY + "\")]"
            + "[D:$!webapi.getConfigVar(\"" + OTHER_PROPERTY + "\")]"
            + "[E:#foreach($f in $macro.getFileSystemFolder(\"/WEB-INF\"))$f.name#end]"
            + "[F:#if($macro)macro#end]"
            + "[G:$!config.getStringProperty(\"DEFAULT_TEMPLATE_NAME\", \"default\")]";

    private static User admin;
    private static User limitedUser;
    private static final List<User> users = new ArrayList<>();
    private static final List<Role> roles = new ArrayList<>();
    private static final List<ContentType> contentTypes = new ArrayList<>();
    private static final List<Container> containers = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        Config.setProperty(ALLOWED_PROPERTY, ALLOWED_VALUE);
        Config.setProperty(OTHER_PROPERTY, OTHER_VALUE);
        admin = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com",
                APILocator.systemUser(), false);
        limitedUser = user(new UserDataGen().nextPersisted());
    }

    @AfterClass
    public static void cleanUp() {
        Config.setProperty(ALLOWED_PROPERTY, null);
        Config.setProperty(OTHER_PROPERTY, null);
        contentTypes.forEach(contentType -> ContentTypeDataGen.remove(contentType, true));
        users.forEach(user -> UserDataGen.remove(user, true));
        roles.forEach(role -> RoleDataGen.remove(role, true));
        containers.forEach(ContainerDataGen::remove);
    }

    private static User user(final User user) {
        users.add(user);
        return user;
    }

    private static Role role(final Role role) {
        roles.add(role);
        return role;
    }

    private static HttpServletRequest request(final User user) {
        final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
        if (null != user) {
            request.setAttribute(WebKeys.USER, user);
        }
        return request;
    }

    private static String render(final HttpServletRequest request, final Contentlet contentlet) {
        final HttpServletResponse response = new MockHttpResponse().response();
        return String.valueOf(RenderFieldStrategy.renderFieldValue(request, response, FIELD_CODE,
                contentlet, "code"));
    }

    private static Contentlet notPersistedContent(final User author) {
        final Contentlet contentlet = new Contentlet();
        contentlet.setInode(UUIDGenerator.generateUuid());
        contentlet.setModUser(author.getUserId());
        return contentlet;
    }

    /**
     * Method to test: {@link RenderFieldStrategy#renderFieldValue}
     * When: a content field's Velocity runs
     * Should: find on the request the user that last modified that content, and leave nothing
     * behind once the render is over.
     *
     * <p>The restricted view tools read that value to decide whether they may run, so the whole
     * control rests on this strategy putting it there. The field's own code reads it back, which
     * is the only way to see what was on the request while that code ran.
     */
    @Test
    public void renderFieldValue_announcesTheContentAuthorAndThenClearsIt() {
        final User author = user(new UserDataGen().nextPersisted());
        final HttpServletRequest request = request(null);

        final String rendered = String.valueOf(RenderFieldStrategy.renderFieldValue(request,
                new MockHttpResponse().response(),
                "$request.getAttribute('" + RestrictedViewToolAccess.RENDERING_AUTHOR_ID + "')",
                notPersistedContent(author), "code"));

        assertTrue("The field's code did not see its author on the request, so the view tools "
                + "cannot either. Rendered: " + rendered, rendered.contains(author.getUserId()));
        assertNull("The announcement outlived the render, so the next thing rendered on this "
                        + "request would be judged on the wrong author",
                request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#authorOfRenderedResource(String)}
     * When: the renderer is about to run a container's own Velocity
     * Should: answer with the user that last saved that container, and with nothing at all when
     * the path names something that does not resolve.
     *
     * <p>This is what {@code DotDirective} announces while a container or template renders, so a
     * wrong answer here would judge that code on the wrong author.
     */
    @Test
    public void authorOfRenderedResource_resolvesTheContainerAuthor() {
        // The API records the saving user as the modifier, and saving a container needs
        // permissions, so the admin stands in as the author here. Which user it is does not
        // matter: what is under test is that the author is read off the container the path
        // resolves to, not that this particular user may use the tools.
        final Container container = new ContainerDataGen().owner(admin).modUser(admin)
                .nextPersisted();
        containers.add(container);

        assertEquals(admin.getUserId(), RestrictedViewToolAccess.authorOfRenderedResource(
                "/WORKING/" + container.getIdentifier() + "/1.container"));
        assertNull("A path that resolves to nothing must not name an author",
                RestrictedViewToolAccess.authorOfRenderedResource(
                        "/WORKING/" + UUIDGenerator.generateUuid() + "/1.container"));
    }

    /**
     * The parts of the output that do not depend on who wrote or who reads the content.
     */
    private static void assertCommonRules(final String result) {
        assertTrue(result, result.contains("[A:" + ALLOWED_VALUE + "]"));
        assertTrue(result, result.contains("[C:" + ALLOWED_VALUE + "]"));
        assertFalse(result, result.contains(OTHER_VALUE));
        assertTrue(result, result.contains("[B:]"));
        assertTrue(result, result.contains("[D:]"));
        assertTrue(result, result.contains("[E:]"));
        assertFalse(result, result.contains("[G:]"));
    }

    /**
     * Method to test: {@link RenderFieldStrategy#renderFieldValue}
     * When: content written by a user without the Scripting Developer role is rendered for an
     * anonymous visitor
     * Should: expose only the allowed configuration properties and not make {@code $macro}
     * available
     */
    @Test
    public void limitedAuthor_anonymousCaller_cannotUseRestrictedTools() {
        final String result = render(request(null), notPersistedContent(limitedUser));

        assertCommonRules(result);
        assertTrue(result, result.contains("[F:]"));
    }

    /**
     * Method to test: {@link RenderFieldStrategy#renderFieldValue}
     * When: the same content is rendered for a CMS Administrator
     * Should: behave exactly as for the anonymous visitor; the reader does not change what the
     * author's code can do
     */
    @Test
    public void adminCaller_limitedAuthor_rendersAsAnonymous() {
        final String result = render(request(admin), notPersistedContent(limitedUser));

        assertCommonRules(result);
        assertTrue(result, result.contains("[F:]"));
    }

    /**
     * Method to test: {@link RenderFieldStrategy#renderFieldValue}
     * When: content written by a CMS Administrator is rendered for an anonymous visitor
     * Should: keep {@code $macro} available while still exposing only the allowed configuration
     * properties
     */
    @Test
    public void adminAuthor_anonymousCaller_keepsTools() throws Exception {
        final ContentType contentType = new ContentTypeDataGen()
                .fields(List.of(new FieldDataGen().type(TextAreaField.class)
                        .velocityVarName("code").next()))
                .nextPersisted();
        contentTypes.add(contentType);
        final Contentlet draft = new ContentletDataGen(contentType.id())
                .setProperty("code", "text")
                .next();
        draft.setIndexPolicy(IndexPolicy.WAIT_FOR);
        final Contentlet contentlet = APILocator.getContentletAPI().checkin(draft, admin, false);
        assertEquals(admin.getUserId(), contentlet.getModUser());

        final String result = render(request(null), contentlet);

        assertCommonRules(result);
        assertTrue(result, result.contains("[F:macro]"));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#hasAccess(User)}
     * When: the user is anonymous, has no special role, has the Scripting Developer role or is a
     * CMS Administrator
     * Should: only grant access to the last two
     */
    @Test
    public void hasAccess_checksRoles() throws Exception {
        assertFalse(RestrictedViewToolAccess.hasAccess((User) null));
        assertFalse(RestrictedViewToolAccess.hasAccess(APILocator.getUserAPI().getAnonymousUser()));
        assertFalse(RestrictedViewToolAccess.hasAccess(limitedUser));
        assertFalse(RestrictedViewToolAccess.hasAccess(APILocator.systemUser()));
        assertTrue(RestrictedViewToolAccess.hasAccess(admin));

        final Role scripting = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
        final User developer = user(new UserDataGen().roles(scripting).nextPersisted());
        assertTrue(RestrictedViewToolAccess.hasAccess(developer));
        assertTrue(RestrictedViewToolAccess.userIdHasAccess(developer.getUserId()));
        assertFalse(RestrictedViewToolAccess.userIdHasAccess(limitedUser.getUserId()));
        assertFalse(RestrictedViewToolAccess.hasAccess(user(new UserDataGen()
                .roles(role(new RoleDataGen().nextPersisted())).nextPersisted())));
    }

    /**
     * Method to test: {@link DotCMSMacroWebAPI#getFileSystemFolder(String)}
     * When: the folder is WEB-INF, META-INF or outside the web application
     * Should: return an empty list
     */
    @Test
    public void macro_getFileSystemFolder_rejectsProtectedFolders() {
        final DotCMSMacroWebAPI macro = new DotCMSMacroWebAPI();
        for (final String folder : List.of("/WEB-INF", "/web-inf", "WEB-INF/classes", "/META-INF",
                "/..", "/html/../WEB-INF", "/html/../../", "..\\WEB-INF")) {
            assertTrue(folder, macro.getFileSystemFolder(folder).isEmpty());
        }
        assertEquals(null, DotCMSMacroWebAPI.resolveWebAppFolder("/../.."));
    }
}
