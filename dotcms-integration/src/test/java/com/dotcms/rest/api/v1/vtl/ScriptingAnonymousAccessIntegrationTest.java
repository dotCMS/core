package com.dotcms.rest.api.v1.vtl;

import static com.dotcms.rest.api.v1.vtl.VTLResource.VTL_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.graphql.DotGraphQLContextBuilder;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.js.JsEngine;
import com.dotcms.rendering.js.JsResource;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppConfigThreadLocal;
import com.dotcms.rendering.velocity.viewtools.secrets.DotVelocitySecretAppKeys;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks how SCRIPTING_APIS_ALLOW_ANONYMOUS and CONTENT_APIS_ALLOW_ANONYMOUS combine for anonymous
 * callers: the file-based /api/vtl and /api/js endpoints accept them when either key allows it, while
 * the /dynamic endpoints and the built-in content, ES and GraphQL APIs follow only the global key.
 * Authentication runs for real against a request that carries no credentials.
 */
public class ScriptingAnonymousAccessIntegrationTest {

    private static final String SECRET_VALUE = "js-secret-value";
    private static final String SECRET_DENIED = "denied";

    private static Host site;
    private static String folderName;

    private String originalGlobal;
    private String originalScripting;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        // JsEngine.addModules throws a NullPointerException when the servlet context returns a null real
        // path for its modules folder, which the mocked test context does. Point that one path at an
        // empty folder so JsEngine can evaluate. Nothing in MainSuite1b reassigns Config.CONTEXT, so the
        // stub stays on the shared mock and is harmless to other tests.
        when(Config.CONTEXT.getRealPath(File.separator + JsEngine.WEB_INF + File.separator + "javascript"
                + File.separator + "modules" + File.separator))
                .thenReturn(Files.createTempDirectory("js-modules").toString());
        site = new SiteDataGen().nextPersisted();
        folderName = "anon" + System.currentTimeMillis();
        createScript(VTL_PATH, "get.vtl", "$dotJSON.put(\"ok\", \"vtl\")");
        createScript(JsResource.JS_PATH, "get.js", "(function get(context) { return 'js'; })");
    }

    @Before
    public void saveConfig() {
        originalGlobal = Config.getStringProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, null);
        originalScripting = Config.getStringProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, null);
    }

    @After
    public void restoreConfig() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, originalGlobal);
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, originalScripting);
    }

    /**
     * Given the scripting key on and the global key NONE, anonymous requests to the file-based
     * scripting endpoints succeed, while the content, ES and GraphQL entry points still reject them.
     */
    @Test
    public void scriptingKeyOn_globalNone_onlyFileScriptingEndpointsAllowAnonymous() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "NONE");
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, true);

        final Response vtl = callVtl(anonymousRequest());
        assertEquals(200, vtl.getStatus());
        assertEquals("vtl", ((Map<?, ?>) vtl.getEntity()).get("ok"));

        final Response js = callJs(anonymousRequest());
        assertEquals(200, js.getStatus());
        assertEquals("js", js.getEntity());

        // /api/v1/content read, as ContentResource initializes it
        assertEquals(401, assertThrows(SecurityException.class, () -> new WebResource.InitBuilder()
                .requestAndResponse(anonymousRequest(), response())
                .requiredAnonAccess(AnonymousAccess.READ)
                .init()).getResponse().getStatus());
        // /api/es, as ESContentResourcePortlet initializes it
        assertEquals(401, assertThrows(SecurityException.class,
                () -> new WebResource().init(null, true, anonymousRequest(), false, null))
                .getResponse().getStatus());
        // /api/v1/graphql
        assertEquals(401, assertThrows(SecurityException.class,
                () -> new DotGraphQLContextBuilder().build(anonymousRequest(), response()))
                .getResponse().getStatus());
    }

    /**
     * Given the scripting key on and the global key NONE, the /dynamic endpoint still follows the
     * global key and rejects anonymous callers.
     */
    @Test
    public void scriptingKeyOn_globalNone_dynamicEndpointRejectsAnonymous() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "NONE");
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, true);

        final Response response = new VTLResource().dynamicGet(anonymousRequest(), response(), uriInfo(),
                null, "{\"velocity\":\"$dotJSON.put('ok', 'dynamic')\"}");
        assertEquals(401, response.getStatus());
    }

    /**
     * Given the scripting key on and the global key NONE, the /api/js/dynamic endpoint still follows
     * the global key and rejects anonymous callers. JsResource spots the dynamic endpoints by their
     * missing folder name rather than a flag, so it gets its own check.
     */
    @Test
    public void scriptingKeyOn_globalNone_jsDynamicEndpointRejectsAnonymous() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "NONE");
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, true);

        final Response response = new JsResource().dynamicGet(anonymousRequest(), response(), uriInfo(),
                null, "{\"javascript\":\"(function get(context) { return 'dynamic'; })\"}");
        assertEquals(401, response.getStatus());
    }

    /**
     * Given an anonymous GET to a file-based /api/js endpoint whose get.js reads a Velocity Secret,
     * and the file was last edited by a user with the Scripting Developer role, the secret value is
     * returned. This path relies on Velocity reporting no template name for the JS context.
     */
    @Test
    public void jsGet_anonymousReadsSecret_whenFileLastEditedByScriptingDeveloper() throws Exception {
        final User scriptingDeveloper = new UserDataGen().roles(
                APILocator.getRoleAPI().loadCMSAdminRole(),
                APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER)).nextPersisted();

        assertEquals(SECRET_VALUE, getJsSecretAnonymously(scriptingDeveloper));
    }

    /**
     * Given the same anonymous /api/js request, but the file's last editor lacks the Scripting
     * Developer role, the secret is refused.
     */
    @Test
    public void jsGet_anonymousCannotReadSecret_whenFileEditorLacksScriptingRole() throws Exception {
        final User editor = new UserDataGen().roles(APILocator.getRoleAPI().loadCMSAdminRole()).nextPersisted();

        assertEquals(SECRET_DENIED, getJsSecretAnonymously(editor));
    }

    /**
     * Given the scripting key off and the global key READ, anonymous requests to the file-based
     * scripting endpoints still succeed, as they did before the scripting key existed, and an
     * authenticated user gets through too.
     */
    @Test
    public void scriptingKeyOff_globalRead_fileScriptingEndpointsAllowAnonymousAndUsers() throws Exception {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, false);

        assertEquals(200, callVtl(anonymousRequest()).getStatus());
        assertEquals(200, callJs(anonymousRequest()).getStatus());

        final User admin = TestUserUtils.getAdminUser();
        final HttpServletRequest vtlRequest = anonymousRequest();
        vtlRequest.setAttribute(WebKeys.USER, admin);
        assertEquals(200, callVtl(vtlRequest).getStatus());
        final HttpServletRequest jsRequest = anonymousRequest();
        jsRequest.setAttribute(WebKeys.USER, admin);
        assertEquals(200, callJs(jsRequest).getStatus());
    }

    /**
     * Given the scripting key off and the global key NONE, anonymous requests to the file-based
     * scripting endpoints get 401.
     */
    @Test
    public void scriptingKeyOff_globalNone_fileScriptingEndpointsRejectAnonymous() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "NONE");
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, false);

        assertEquals(401, callVtl(anonymousRequest()).getStatus());
        assertEquals(401, callJs(anonymousRequest()).getStatus());
    }

    /**
     * Seeds a dotVelocitySecretApp secret on the test site, publishes a get.js that returns it as the
     * given editor, and calls the endpoint as the anonymous user.
     *
     * @param editor user who checks in and publishes the get.js file
     * @return what the script returned: the secret, or {@code "denied"} when reading it was refused
     */
    private static String getJsSecretAnonymously(final User editor) throws Exception {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "NONE");
        Config.setProperty(AnonymousAccess.SCRIPTING_APIS_ALLOW_ANONYMOUS, true);
        DotVelocitySecretAppConfigThreadLocal.INSTANCE.clearConfig();

        final User systemUser = APILocator.systemUser();
        final String secretsFolder = "secrets" + System.currentTimeMillis();
        try {
            APILocator.getAppsAPI().saveSecrets(new AppSecrets.Builder()
                    .withKey(DotVelocitySecretAppKeys.APP_KEY)
                    .withSecret(DotVelocitySecretAppKeys.TITLE.key, "JS secrets test")
                    .withHiddenSecret("mySecret", SECRET_VALUE)
                    .build(), site, systemUser);
            createScript(JsResource.JS_PATH, secretsFolder, "get.js",
                    "(function get(context) { try { return String(dotsecrets.get('mySecret')); }"
                            + " catch (e) { return '" + SECRET_DENIED + "'; } })", editor);

            final Response response = new JsResource().get(anonymousRequest(), response(), uriInfo(),
                    secretsFolder, Map.of());
            assertEquals(200, response.getStatus());
            return response.getEntity().toString();
        } finally {
            DotVelocitySecretAppConfigThreadLocal.INSTANCE.clearConfig();
            APILocator.getAppsAPI().deleteSecrets(DotVelocitySecretAppKeys.APP_KEY, site, systemUser);
        }
    }

    private static Response callVtl(final HttpServletRequest request) {
        return new VTLResource().get(request, response(), uriInfo(), folderName, Map.of());
    }

    private static Response callJs(final HttpServletRequest request) {
        return new JsResource().get(request, response(), uriInfo(), folderName, Map.of());
    }

    private static HttpServletRequest anonymousRequest() {
        return new MockAttributeRequest(
                new MockHttpRequestIntegrationTest(site.getHostname(), "/api/test").request()).request();
    }

    private static HttpServletResponse response() {
        return new MockHttpResponse().response();
    }

    private static UriInfo uriInfo() {
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        return uriInfo;
    }

    /**
     * Publishes {@code fileName} with {@code code} under {@code basePath/folderName} on the test site
     * and lets the anonymous role read both the file and its folder.
     */
    private static void createScript(final String basePath, final String fileName, final String code)
            throws Exception {
        createScript(basePath, folderName, fileName, code, APILocator.systemUser());
    }

    /**
     * Publishes {@code fileName} with {@code code} under {@code basePath/endpointFolder} on the test
     * site as {@code editor}, so that {@code editor} is its last editor, and lets the anonymous role
     * read both the file and its folder.
     */
    private static void createScript(final String basePath, final String endpointFolder,
            final String fileName, final String code, final User editor) throws Exception {
        final User systemUser = APILocator.systemUser();
        final Folder folder = APILocator.getFolderAPI()
                .createFolders(basePath + "/" + endpointFolder, site, systemUser, false);
        final File file = new File(Files.createTempDirectory("scripting-anon").toFile(), fileName);
        Files.writeString(file.toPath(), code, StandardCharsets.UTF_8);

        final Contentlet newAsset = new FileAssetDataGen(folder, file).next();
        newAsset.setIndexPolicy(IndexPolicy.FORCE);
        newAsset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        final Contentlet asset = APILocator.getContentletAPI().checkin(newAsset, editor, false);
        asset.setIndexPolicy(IndexPolicy.FORCE);
        asset.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().publish(asset, editor, false);

        final String anonRoleId = APILocator.getRoleAPI().loadCMSAnonymousRole().getId();
        APILocator.getPermissionAPI().save(new Permission(asset.getPermissionId(), anonRoleId,
                PermissionAPI.PERMISSION_READ), asset, systemUser, false);
        APILocator.getPermissionAPI().save(new Permission(folder.getPermissionId(), anonRoleId,
                PermissionAPI.PERMISSION_READ), folder, systemUser, true);
    }

}
