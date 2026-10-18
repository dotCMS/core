package com.dotcms.rendering.velocity.viewtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.type.ContentType;
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
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.strategy.RenderFieldStrategy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.util.WebKeys;
import com.liferay.portal.model.User;
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

    private static final String PROPERTY_NAME = "DOT_RENDER_TEST_PASSWORD";
    private static final String PROPERTY_VALUE = "value-" + System.currentTimeMillis();
    private static final String FIELD_CODE = "[$!config.getStringProperty(\"" + PROPERTY_NAME + "\")]"
            + "[#foreach($f in $macro.getFileSystemFolder(\"/WEB-INF\"))$f.name#end]"
            + "[$!config.getStringProperty(\"DEFAULT_TEMPLATE_NAME\", \"default\")]";

    private static User admin;
    private static User limitedUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        Config.setProperty(PROPERTY_NAME, PROPERTY_VALUE);
        admin = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com",
                APILocator.systemUser(), false);
        limitedUser = new UserDataGen().nextPersisted();
    }

    @AfterClass
    public static void cleanUp() {
        Config.setProperty(PROPERTY_NAME, null);
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
     * When: content written by a user without the Scripting Developer role is rendered for an
     * anonymous visitor
     * Should: not expose configuration values nor list folders of the web application
     */
    @Test
    public void limitedAuthor_anonymousCaller_cannotUseRestrictedTools() {
        final String result = render(request(null), notPersistedContent(limitedUser));

        assertFalse(result, result.contains(PROPERTY_VALUE));
        assertFalse(result, result.contains("web.xml"));
    }

    /**
     * Method to test: {@link RenderFieldStrategy#renderFieldValue}
     * When: the caller is a CMS Administrator
     * Should: keep the configuration tool and return the configured value, but still never list
     * the WEB-INF folder
     */
    @Test
    public void adminCaller_canReadConfiguration() {
        final String result = render(request(admin), notPersistedContent(limitedUser));

        assertTrue(result, result.contains("[" + PROPERTY_VALUE + "]"));
        assertTrue(result, result.contains("[]"));
        assertFalse(result, result.contains("web.xml"));
    }

    /**
     * Method to test: {@link RenderFieldStrategy#renderFieldValue}
     * When: content written by a CMS Administrator is rendered for an anonymous visitor
     * Should: keep the configuration tool and return the configured value
     */
    @Test
    public void adminAuthor_anonymousCaller_canReadConfiguration() {
        final ContentType contentType = new ContentTypeDataGen()
                .fields(List.of(new FieldDataGen().type(TextAreaField.class)
                        .velocityVarName("code").next()))
                .nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .user(admin)
                .setProperty("code", "text")
                .nextPersisted();
        assertEquals(admin.getUserId(), contentlet.getModUser());

        final String result = render(request(null), contentlet);

        assertTrue(result, result.contains("[" + PROPERTY_VALUE + "]"));
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
        assertTrue(RestrictedViewToolAccess.hasAccess(admin));

        final Role scripting = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
        final User developer = new UserDataGen().roles(scripting).nextPersisted();
        assertTrue(RestrictedViewToolAccess.hasAccess(developer));
        assertTrue(RestrictedViewToolAccess.userIdHasAccess(developer.getUserId()));
        assertFalse(RestrictedViewToolAccess.userIdHasAccess(limitedUser.getUserId()));
        assertFalse(RestrictedViewToolAccess.hasAccess(new UserDataGen()
                .roles(new RoleDataGen().nextPersisted()).nextPersisted()));
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
