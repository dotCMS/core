package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Optional;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

public class SecretToolTest {

    private SecretTool secretTool;
    private Context velocityContext;
    private HttpServletRequest request;
    private ViewContext viewContext;
    private Contentlet scriptingFile;

    private MockedStatic<APILocator> mockedApiLocator;
    private MockedStatic<WebAPILocator> mockedWebApiLocator;
    private MockedStatic<Config> mockedConfig;

    private RoleAPI roleAPI;
    private UserAPI userAPI;
    private UserWebAPI userWebAPI;

    @BeforeEach
    public void setUp() {
        secretTool = new SecretTool();
        velocityContext = mock(Context.class);
        request = mock(HttpServletRequest.class);
        viewContext = mock(ViewContext.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);

        mockedApiLocator = mockStatic(APILocator.class);
        mockedWebApiLocator = mockStatic(WebAPILocator.class);
        mockedConfig = mockStatic(Config.class);

        roleAPI = mock(RoleAPI.class);
        userAPI = mock(UserAPI.class);
        userWebAPI = mock(UserWebAPI.class);

        mockedApiLocator.when(APILocator::getRoleAPI).thenReturn(roleAPI);
        mockedApiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
        mockedWebApiLocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);

        mockedConfig.when(() -> Config.getBooleanProperty(eq("secrets.scripting.enabled"), any(Boolean.class))).thenReturn(true); // Enabled by default

        secretTool.init(viewContext);
    }

    @AfterEach
    public void tearDown() {
        mockedApiLocator.close();
        mockedWebApiLocator.close();
        mockedConfig.close();
    }

    @Test
    public void canUserEvaluate_ScriptingDisabled_ThrowsException() {
        mockedConfig.when(() -> Config.getBooleanProperty(eq("secrets.scripting.enabled"), any(Boolean.class))).thenReturn(false);

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
    }

    @Test
    public void canUserEvaluate_UserInRequestHasRole_Success() throws Exception {
        User user = mock(User.class);
        Role scriptingRole = mock(Role.class);

        when(userWebAPI.getUser(request)).thenReturn(user);
        when(user.isAnonymousUser()).thenReturn(false);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);
        when(roleAPI.doesUserHaveRole(user, scriptingRole)).thenReturn(true);

        assertDoesNotThrow(() -> secretTool.canUserEvaluate());
    }

    @Test
    public void canUserEvaluate_AnonymousRequestUserAndContextUserHasRole_ThrowsException() throws Exception {
        User anonymousUser = mock(User.class);
        when(anonymousUser.isAnonymousUser()).thenReturn(true);
        when(userWebAPI.getUser(request)).thenReturn(anonymousUser);
        final User contextUser = givenContextUserWithScriptingRole();

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
        verify(roleAPI).doesUserHaveRole(same(anonymousUser), any(Role.class));
        verify(roleAPI, never()).doesUserHaveRole(same(contextUser), any(Role.class));
    }

    @Test
    public void canUserEvaluate_NoRequestUserAndContextUserHasRole_ThrowsException() throws Exception {
        when(userWebAPI.getUser(request)).thenReturn(null);
        final User contextUser = givenContextUserWithScriptingRole();

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
        verify(roleAPI, never()).doesUserHaveRole(same(contextUser), any(Role.class));
    }

    @Test
    public void canUserEvaluate_RequestUserWithoutRoleAndContextUserHasRole_ThrowsException() throws Exception {
        User requestUser = mock(User.class);
        when(requestUser.isAnonymousUser()).thenReturn(false);
        when(userWebAPI.getUser(request)).thenReturn(requestUser);
        final User contextUser = givenContextUserWithScriptingRole();
        when(roleAPI.doesUserHaveRole(same(requestUser), any(Role.class))).thenReturn(false);

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
        verify(roleAPI).doesUserHaveRole(same(requestUser), any(Role.class));
        verify(roleAPI, never()).doesUserHaveRole(same(contextUser), any(Role.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("secretReads")
    public void secretReads_AnonymousRequestUserAndContextUserHasRole_ThrowsException(
            final String method, final SecretRead secretRead) throws Exception {
        User anonymousUser = mock(User.class);
        when(anonymousUser.isAnonymousUser()).thenReturn(true);
        when(userWebAPI.getUser(request)).thenReturn(anonymousUser);
        final User contextUser = givenContextUserWithScriptingRole();

        assertThrows(SecurityException.class, () -> secretRead.read(secretTool),
                method + " must check the Scripting Developer role of the request user");
        verify(roleAPI).doesUserHaveRole(same(anonymousUser), any(Role.class));
        verify(roleAPI, never()).doesUserHaveRole(same(contextUser), any(Role.class));
    }

    @FunctionalInterface
    interface SecretRead {
        Object read(SecretTool secretTool);
    }

    static Stream<Arguments> secretReads() {
        return Stream.of(
                Arguments.of("get(key)", (SecretRead) tool -> tool.get("key")),
                Arguments.of("getSystemSecret(key)", (SecretRead) tool -> tool.getSystemSecret("key")),
                Arguments.of("getSystemSecret(key, defaultValue)",
                        (SecretRead) tool -> tool.getSystemSecret("key", "default")),
                Arguments.of("getCharArray(key)", (SecretRead) tool -> tool.getCharArray("key")),
                Arguments.of("getCharArraySystemSecret(key)",
                        (SecretRead) tool -> tool.getCharArraySystemSecret("key")),
                Arguments.of("getCharArraySystemSecret(key, defaultValue)",
                        (SecretRead) tool -> tool.getCharArraySystemSecret("key", new char[]{'d'})));
    }

    /** Puts in the Velocity context a {@code user} entry that holds the Scripting Developer role, and returns it. */
    private User givenContextUserWithScriptingRole() throws Exception {
        final User contextUser = mock(User.class);
        final Role scriptingRole = mock(Role.class);
        when(velocityContext.get("user")).thenReturn(contextUser);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);
        when(roleAPI.doesUserHaveRole(contextUser, scriptingRole)).thenReturn(true);
        return contextUser;
    }

    @Test
    public void canUserEvaluate_NoUserAnywhere_ThrowsException() throws Exception {
        when(userWebAPI.getUser(request)).thenReturn(null);
        when(velocityContext.get("user")).thenReturn(null);
        
        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
    }

    @Test
    public void canUserEvaluate_UserHasNoRole_ThrowsException() throws Exception {
        User user = mock(User.class);
        Role scriptingRole = mock(Role.class);

        when(userWebAPI.getUser(request)).thenReturn(user);
        when(user.isAnonymousUser()).thenReturn(false);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);
        when(roleAPI.doesUserHaveRole(user, scriptingRole)).thenReturn(false);

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
    }

    /**
     * An anonymous request to a file-based scripting endpoint passes when the file's last editor
     * has the Scripting Developer role.
     */
    @Test
    public void canUserEvaluate_AnonymousRequestAndScriptingFileEditorHasRole_Success() throws Exception {
        final Role scriptingRole = anonymousRequestWithScriptingFileEditedBy("scripter");
        final User scripter = mock(User.class);
        when(userAPI.loadUserById(eq("scripter"), any(), eq(true))).thenReturn(scripter);
        when(roleAPI.doesUserHaveRole(scripter, scriptingRole)).thenReturn(true);

        assertDoesNotThrow(() -> secretTool.canUserEvaluate());
    }

    /**
     * An anonymous request to a file-based scripting endpoint fails when the file's last editor
     * lacks the Scripting Developer role.
     */
    @Test
    public void canUserEvaluate_AnonymousRequestAndScriptingFileEditorHasNoRole_ThrowsException() throws Exception {
        final Role scriptingRole = anonymousRequestWithScriptingFileEditedBy("editor");
        final User editor = mock(User.class);
        when(userAPI.loadUserById(eq("editor"), any(), eq(true))).thenReturn(editor);
        when(roleAPI.doesUserHaveRole(editor, scriptingRole)).thenReturn(false);

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
    }

    /**
     * Mutating a loaded file after Java binds it must not let a non-scripting editor borrow a
     * Scripting Developer's authority.
     */
    @Test
    public void canUserEvaluate_MutatedScriptingFileEditor_DoesNotGainRole() throws Exception {
        final Role scriptingRole = anonymousRequestWithScriptingFileEditedBy("editor");
        final User scripter = mock(User.class);
        when(userAPI.loadUserById(eq("scripter"), any(), eq(true))).thenReturn(scripter);
        when(roleAPI.doesUserHaveRole(scripter, scriptingRole)).thenReturn(true);
        when(scriptingFile.getModUser()).thenReturn("scripter");

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
        verify(userAPI, never()).loadUserById(eq("scripter"), any(), any(Boolean.class));
    }

    /**
     * Without a scripting file recorded by Java code the request user still decides, even when a
     * script has placed a raw Contentlet under the attribute name.
     */
    @Test
    public void canUserEvaluate_AnonymousRequestWithoutScriptingFile_ThrowsException() throws Exception {
        final User anonymousUser = mock(User.class);
        when(anonymousUser.isAnonymousUser()).thenReturn(true);
        when(userWebAPI.getUser(request)).thenReturn(anonymousUser);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(mock(Role.class));
        final Contentlet spoofed = mock(Contentlet.class);
        when(spoofed.getModUser()).thenReturn("scripter");
        when(request.getAttribute(ScriptingFileAttribute.ATTRIBUTE)).thenReturn(spoofed);

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
        verify(userAPI, never()).loadUserById(any(), any(), any(Boolean.class));
    }

    /**
     * When the template name resolves to stored content, that content's last editor decides even
     * if a scripting file was also recorded for the request.
     */
    @Test
    public void canUserEvaluate_TemplateNameResolves_IgnoresScriptingFile() throws Exception {
        final Role scriptingRole = anonymousRequestWithScriptingFileEditedBy("scripter");
        final User scripter = mock(User.class);
        when(userAPI.loadUserById(eq("scripter"), any(), eq(true))).thenReturn(scripter);
        when(roleAPI.doesUserHaveRole(scripter, scriptingRole)).thenReturn(true);

        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        mockedApiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        final Contentlet pageContent = mock(Contentlet.class);
        when(pageContent.getModUser()).thenReturn("editor");
        when(contentletAPI.find(eq("content-inode"), any(), eq(true))).thenReturn(pageContent);
        final User editor = mock(User.class);
        when(userAPI.loadUserById(eq("editor"), any(), eq(true))).thenReturn(editor);
        when(roleAPI.doesUserHaveRole(editor, scriptingRole)).thenReturn(false);
        initWithTemplateName("/LIVE/content-inode/1");

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
        verify(userAPI, never()).loadUserById(eq("scripter"), any(), any(Boolean.class));
    }

    /**
     * A nested evaluation such as {@code $render.eval} inside a scripting file has its own
     * template name, so it does not inherit the file's last editor.
     */
    @Test
    public void canUserEvaluate_NestedEvalInScriptingFile_IgnoresScriptingFile() throws Exception {
        anonymousRequestWithScriptingFileEditedBy("scripter");
        // the log tag RenderTool passes to Velocity for $render.eval
        initWithTemplateName("RenderTool.eval()");

        assertThrows(SecurityException.class, () -> secretTool.canUserEvaluate());
        verify(userAPI, never()).loadUserById(eq("scripter"), any(), any(Boolean.class));
    }

    /** Caller-supplied host parameters and later file mutations cannot change a script's secret site. */
    @Test
    public void secretReads_ScriptingFileSiteCannotBeOverridden() throws Exception {
        final Role scriptingRole = anonymousRequestWithScriptingFileEditedBy("scripter");
        final User scripter = mock(User.class);
        when(userAPI.loadUserById(eq("scripter"), any(), eq(true))).thenReturn(scripter);
        when(roleAPI.doesUserHaveRole(scripter, scriptingRole)).thenReturn(true);
        when(request.getParameter("host_id")).thenReturn("other-site");
        when(request.getParameter("host")).thenReturn("other-site");
        when(scriptingFile.getHost()).thenReturn("other-site");

        final Host scriptSite = mock(Host.class);
        final HostAPI hostAPI = mock(HostAPI.class);
        mockedApiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        when(hostAPI.find(eq("script-site"), any(), eq(false))).thenReturn(scriptSite);
        final DotVelocitySecretAppConfig scriptConfig = mock(DotVelocitySecretAppConfig.class);
        final DotVelocitySecretAppConfig callerConfig = mock(DotVelocitySecretAppConfig.class);
        when(scriptConfig.getStringOrNull("key")).thenReturn("script-secret");
        when(scriptConfig.getCharArrayOrNull("key")).thenReturn("script-secret".toCharArray());
        when(callerConfig.getStringOrNull("key")).thenReturn("other-site-secret");
        try (MockedStatic<DotVelocitySecretAppConfig> configs = mockStatic(DotVelocitySecretAppConfig.class)) {
            configs.when(() -> DotVelocitySecretAppConfig.config(request)).thenReturn(Optional.of(callerConfig));
            configs.when(() -> DotVelocitySecretAppConfig.config(scriptSite)).thenReturn(Optional.of(scriptConfig));

            assertEquals("script-secret", secretTool.get("key"));
            assertArrayEquals("script-secret".toCharArray(), secretTool.getCharArray("key"));
            when(hostAPI.find(eq("script-site"), any(), eq(false))).thenReturn(null);
            assertThrows(SecurityException.class, () -> secretTool.get("key"));
            configs.verify(() -> DotVelocitySecretAppConfig.config(request), never());
        }
    }

    /** Missing file-site provenance must fail closed, not fall back to a caller-selected site. */
    @Test
    public void scriptingFileWithoutSiteCannotSelectRequestSite() {
        final Contentlet file = mock(Contentlet.class);
        ScriptingFileAttribute.set(request, file);
        final ArgumentCaptor<Object> attribute = ArgumentCaptor.forClass(Object.class);
        verify(request).setAttribute(eq(ScriptingFileAttribute.ATTRIBUTE), attribute.capture());
        when(request.getAttribute(ScriptingFileAttribute.ATTRIBUTE)).thenReturn(attribute.getValue());

        assertThrows(SecurityException.class, () -> ScriptingFileAttribute.getSiteId(request));
    }

    /** Ordinary page and workflow contexts retain their existing request-scoped site lookup. */
    @Test
    public void secretReads_WithoutScriptingFileStillUseRequestSite() throws Exception {
        final User user = mock(User.class);
        final Role scriptingRole = mock(Role.class);
        when(userWebAPI.getUser(request)).thenReturn(user);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);
        when(roleAPI.doesUserHaveRole(user, scriptingRole)).thenReturn(true);
        final DotVelocitySecretAppConfig config = mock(DotVelocitySecretAppConfig.class);
        when(config.getStringOrNull("key")).thenReturn("request-secret");
        try (MockedStatic<DotVelocitySecretAppConfig> configs = mockStatic(DotVelocitySecretAppConfig.class)) {
            configs.when(() -> DotVelocitySecretAppConfig.config(request)).thenReturn(Optional.of(config));
            assertEquals("request-secret", secretTool.get("key"));
        }
    }

    /**
     * Re-initialises the tool with a real Velocity context whose current template name is the
     * given one, as Velocity sets it while rendering.
     *
     * @param templateName name to push onto the context's template name stack
     */
    private void initWithTemplateName(final String templateName) {
        final VelocityContext context = new VelocityContext();
        context.pushCurrentTemplateName(templateName);
        when(viewContext.getVelocityContext()).thenReturn(context);
        secretTool.init(viewContext);
    }

    /**
     * Sets up an anonymous request whose scripting file, recorded through
     * {@link ScriptingFileAttribute#set}, was last edited by the given user id.
     *
     * @param modUserId id of the file's last editor
     * @return the mocked Scripting Developer role
     */
    private Role anonymousRequestWithScriptingFileEditedBy(final String modUserId) throws Exception {
        final User anonymousUser = mock(User.class);
        when(anonymousUser.isAnonymousUser()).thenReturn(true);
        when(userWebAPI.getUser(request)).thenReturn(anonymousUser);
        final Role scriptingRole = mock(Role.class);
        when(roleAPI.loadRoleByKey(Role.SCRIPTING_DEVELOPER)).thenReturn(scriptingRole);

        final Contentlet file = mock(Contentlet.class);
        scriptingFile = file;
        when(file.getModUser()).thenReturn(modUserId);
        when(file.getHost()).thenReturn("script-site");
        ScriptingFileAttribute.set(request, file);
        final ArgumentCaptor<Object> attribute = ArgumentCaptor.forClass(Object.class);
        verify(request).setAttribute(eq(ScriptingFileAttribute.ATTRIBUTE), attribute.capture());
        when(request.getAttribute(ScriptingFileAttribute.ATTRIBUTE)).thenReturn(attribute.getValue());
        return scriptingRole;
    }
}
