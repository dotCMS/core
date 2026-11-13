package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the configuration defaults of {@link RestrictedViewToolAccess}.
 */
public class RestrictedViewToolAccessDefaultsTest {

    private MockedStatic<Config> mockedConfig;

    @BeforeEach
    public void setUp() {
        mockedConfig = mockStatic(Config.class);
        mockedConfig.when(() -> Config.getStringArrayProperty(
                eq(RestrictedViewToolAccess.RESTRICTED_TOOLS_PROPERTY), any(String[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    public void tearDown() {
        mockedConfig.close();
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#restrictedToolKeys()}
     * When: nothing is configured
     * Should: list the tools that work outside of the caller's permissions, and not the
     * configuration tool, which limits what it returns on its own
     */
    @Test
    public void restrictedToolKeys_defaultList() {
        assertEquals(List.of("dotsecrets", "filetool", "macro", "mailer", "sql", "xslttool"),
                List.copyOf(RestrictedViewToolAccess.restrictedToolKeys()));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#restrictedToolKeys()}
     * When: nothing is configured
     * Should: cover every view tool that decides for itself whether its caller may run, because
     * each of those asks the Velocity template name who the author is, and the author chooses
     * that string through {@code #parse}. Whoever renders the content knows which content it is
     * and must take the decision instead, by withholding the tool.
     */
    @Test
    public void restrictedToolKeys_coverTheToolsThatGateThemselves() {
        assertTrue(RestrictedViewToolAccess.restrictedToolKeys()
                .containsAll(Set.of("xslttool", "sql", "macro", "dotsecrets")));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#restrictedToolKeys()}
     * When: {@link RestrictedViewToolAccess#RESTRICTED_TOOLS_PROPERTY} is set
     * Should: use it, trimmed and without blanks
     */
    @Test
    public void restrictedToolKeys_configuredList() {
        mockedConfig.when(() -> Config.getStringArrayProperty(
                eq(RestrictedViewToolAccess.RESTRICTED_TOOLS_PROPERTY), any(String[].class)))
                .thenReturn(new String[]{" macro ", "", "mailer"});

        final Set<String> keys = RestrictedViewToolAccess.restrictedToolKeys();
        assertEquals(Set.of("macro", "mailer"), keys);
        assertTrue(keys.stream().noneMatch(String::isBlank));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#announceRenderingAuthor(HttpServletRequest, String)}
     * and {@link RestrictedViewToolAccess#restoreRenderingAuthor(HttpServletRequest, Object)}
     * When: a render announces an author, and an inner render announces another
     * Should: the inner announcement is what a tool reads while the inner code runs, and the
     * outer author is back once the inner render finishes. Renders nest — a page renders a
     * container which renders a field — so an inner render must not leave its author behind.
     */
    @Test
    public void announceRenderingAuthor_nestsAndRestores() {
        final HttpServletRequest request = requestWithAttributes();

        final Object noneBefore = RestrictedViewToolAccess.announceRenderingAuthor(request, "outer-author");
        final Object outerAnnouncement = request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID);
        assertNotNull(outerAnnouncement, "The outer render announced an author");

        final Object outer = RestrictedViewToolAccess.announceRenderingAuthor(request, "inner-author");
        assertNotEquals(outerAnnouncement,
                request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID),
                "While the inner code runs, the inner author is what a tool reads");

        RestrictedViewToolAccess.restoreRenderingAuthor(request, outer);
        assertEquals(outerAnnouncement,
                request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID),
                "Once the inner render is done, the outer author is back");

        RestrictedViewToolAccess.restoreRenderingAuthor(request, noneBefore);
        assertNull(request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID),
                "Nothing was announced before the outer render, so nothing may be left behind");
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#announceRenderingAuthor(HttpServletRequest, String)}
     * When: an announcement is made
     * Should: not store the plain user ID. A Velocity template reaches {@code $request} and can
     * call {@code setAttribute} on it, so an announcement a template could reproduce would let
     * the very code being gated name the user it is checked against.
     */
    @Test
    public void announceRenderingAuthor_doesNotStoreTheBareUserId() {
        final HttpServletRequest request = requestWithAttributes();

        RestrictedViewToolAccess.announceRenderingAuthor(request, "an-author");

        assertNotEquals("an-author",
                request.getAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID),
                "A template writing this attribute must not be able to forge an announcement");
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)}
     * When: the code being rendered writes the ID of a privileged user into the attribute itself,
     * which {@code $request.setAttribute(...)} lets any template do
     * Should: refuse, because that value was not put there by a renderer
     */
    @Test
    public void renderingAuthorHasAccess_authorWrittenByTheTemplate_isDenied() {
        final HttpServletRequest request = requestWithAttributes();
        request.setAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID, UserAPI.SYSTEM_USER_ID);

        assertFalse(RestrictedViewToolAccess.renderingAuthorHasAccess(request));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)}
     * When: no render announced an author
     * Should: refuse. An unknown author is not evidence that the code may run, and a render path
     * that does not announce must fail closed rather than open.
     */
    @Test
    public void renderingAuthorHasAccess_noAuthorAnnounced_isDenied() {
        assertFalse(RestrictedViewToolAccess.renderingAuthorHasAccess(requestWithAttributes()));
        assertFalse(RestrictedViewToolAccess.renderingAuthorHasAccess(null));
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)}
     * When: the announced author is a CMS Administrator who was never given the Scripting
     * Developer role
     * Should: refuse. These tools run for whoever visits the page, so what unlocks them is the
     * role that was granted on purpose for writing such code, not administrative power in
     * general. Granting it to any administrator would let a secret be read by an anonymous
     * visitor of a page an administrator happened to edit last.
     */
    @Test
    public void renderingAuthorHasAccess_administratorWithoutTheScriptingRole_isDenied() throws Exception {
        final HttpServletRequest request = requestWithAttributes();
        RestrictedViewToolAccess.announceRenderingAuthor(request, "an-admin");

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            stubAuthor(apiLocator, "an-admin", true, false);
            assertFalse(RestrictedViewToolAccess.renderingAuthorHasAccess(request));
        }
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)}
     * When: the announced author holds the Scripting Developer role and nothing else
     * Should: allow, which is what makes the refusal above a decision about the role rather than
     * the tool being broken for everyone
     */
    @Test
    public void renderingAuthorHasAccess_scriptingDeveloper_isAllowed() throws Exception {
        final HttpServletRequest request = requestWithAttributes();
        RestrictedViewToolAccess.announceRenderingAuthor(request, "a-scripter");

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            stubAuthor(apiLocator, "a-scripter", false, true);
            assertTrue(RestrictedViewToolAccess.renderingAuthorHasAccess(request));
        }
    }

    /**
     * Stubs the APIs behind an announced author.
     *
     * @param apiLocator          the static mock to stub
     * @param userId              ID the author was announced with
     * @param administrator       whether that user is a CMS Administrator
     * @param scriptingDeveloper  whether that user holds the Scripting Developer role
     */
    private static void stubAuthor(final MockedStatic<APILocator> apiLocator, final String userId,
            final boolean administrator, final boolean scriptingDeveloper) throws Exception {
        final User author = mock(User.class);
        when(author.getUserId()).thenReturn(userId);
        when(author.isAdmin()).thenReturn(administrator);

        final UserAPI userAPI = mock(UserAPI.class);
        when(userAPI.loadUserById(eq(userId), any(), any(Boolean.class))).thenReturn(author);

        final Role scripting = mock(Role.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scripting);
        when(roleAPI.doesUserHaveRole(author, scripting)).thenReturn(scriptingDeveloper);

        apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
        apiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)}
     * When: a script file is bound to the request, an announcement of nobody is in force -- what
     * an evaluated string runs under -- and the evaluated code overwrites the announcement with
     * a value of its own
     * Should: refuse. Overwriting must not drop the check back to the script file's editor,
     * which is the standing the announcement of nobody exists to withhold.
     */
    @Test
    public void renderingAuthorHasAccess_announcementClobberedByTheTemplate_isDenied() throws Exception {
        final HttpServletRequest request = requestWithAttributes();
        final Contentlet scriptFile = mock(Contentlet.class);
        when(scriptFile.getModUser()).thenReturn("the-file-editor");
        when(scriptFile.getHost()).thenReturn("a-site");
        ScriptingFileAttribute.set(request, scriptFile);
        RestrictedViewToolAccess.announceRenderingAuthor(request, null);

        request.setAttribute(RestrictedViewToolAccess.RENDERING_AUTHOR_ID, "the-file-editor");

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            stubAuthor(apiLocator, "the-file-editor", false, true);
            assertFalse(RestrictedViewToolAccess.renderingAuthorHasAccess(request));
        }
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#renderingAuthorHasAccess(HttpServletRequest)}
     * When: nothing was announced and a script file is bound to the request, which is how the
     * file-based scripting endpoints run
     * Should: use that file's editor, so those endpoints keep working
     */
    @Test
    public void renderingAuthorHasAccess_noAnnouncement_usesTheScriptFileEditor() throws Exception {
        final HttpServletRequest request = requestWithAttributes();
        final Contentlet scriptFile = mock(Contentlet.class);
        when(scriptFile.getModUser()).thenReturn("the-file-editor");
        when(scriptFile.getHost()).thenReturn("a-site");
        ScriptingFileAttribute.set(request, scriptFile);

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {
            stubAuthor(apiLocator, "the-file-editor", false, true);
            assertTrue(RestrictedViewToolAccess.renderingAuthorHasAccess(request));
        }
    }

    /**
     * @return a request whose attributes behave like a real one, which a plain mock does not do
     */
    private static HttpServletRequest requestWithAttributes() {
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.getArgument(0)));
        doAnswer(call -> attributes.put(call.getArgument(0), call.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(call -> attributes.remove(call.getArgument(0)))
                .when(request).removeAttribute(anyString());
        return request;
    }

    /**
     * Method to test: {@link RestrictedViewToolAccess#hasAccess(User)}
     * When: the user is the system user, which is an administrator
     * Should: not grant access, because content it saved does not tell who wrote the code
     */
    @Test
    public void hasAccess_systemUser_isDenied() {
        final User system = mock(User.class);
        when(system.getUserId()).thenReturn(UserAPI.SYSTEM_USER_ID);
        when(system.isAdmin()).thenReturn(true);

        assertFalse(RestrictedViewToolAccess.hasAccess(system));
    }
}
