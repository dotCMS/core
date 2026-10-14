package com.dotcms.rest.api.v1.vtl;

import static com.dotcms.rest.api.v1.vtl.VTLResource.VTL_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.graphql.DotGraphQLContextBuilder;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.js.JsEngine;
import com.dotcms.rendering.js.JsResource;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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
        final User systemUser = APILocator.systemUser();
        final Folder folder = APILocator.getFolderAPI()
                .createFolders(basePath + "/" + folderName, site, systemUser, false);
        final File file = new File(Files.createTempDirectory("scripting-anon").toFile(), fileName);
        Files.writeString(file.toPath(), code, StandardCharsets.UTF_8);

        final Contentlet asset = new FileAssetDataGen(folder, file).nextPersisted();
        APILocator.getContentletAPI().publish(asset, systemUser, false);

        final String anonRoleId = APILocator.getRoleAPI().loadCMSAnonymousRole().getId();
        APILocator.getPermissionAPI().save(new Permission(asset.getPermissionId(), anonRoleId,
                PermissionAPI.PERMISSION_READ), asset, systemUser, false);
        APILocator.getPermissionAPI().save(new Permission(folder.getPermissionId(), anonRoleId,
                PermissionAPI.PERMISSION_READ), folder, systemUser, true);
    }

}
