package com.dotcms.rest.api.v1.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@code GET /api/v1/page/_render-sources}.
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class PageRenderSourcesResourceTest {

    private static User adminUser;

    private PageResource pageResource;
    private Host host;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        adminUser = APILocator.getUserAPI()
                .loadByUserByEmail("admin@dotcms.com",
                        APILocator.getUserAPI().getSystemUser(), false);
    }

    @Before
    public void init() throws DotDataException, DotSecurityException {
        host = new SiteDataGen().nextPersisted();
        APILocator.getVersionableAPI().setWorking(host);
        APILocator.getVersionableAPI().setLive(host);

        final Map<String, Object> sessionAttributes = new ConcurrentHashMap<>();
        session  = mock(HttpSession.class);
        request  = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.getRequestURI()).thenReturn("/");
        when(session.getAttribute(any())).thenAnswer(
                (InvocationOnMock inv) -> sessionAttributes.get(inv.getArgument(0)));
        org.mockito.Mockito.doAnswer(inv -> {
            sessionAttributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(session).setAttribute(any(), any());

        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(adminUser);

        final WebResource webResource = mock(WebResource.class);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initData);

        final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
        when(hostWebAPI.getCurrentHost(any(HttpServletRequest.class), any(User.class)))
                .thenReturn(host);
        when(hostWebAPI.getCurrentHostNoThrow(any(HttpServletRequest.class))).thenReturn(host);
        when(hostWebAPI.findDefaultHost(any(User.class), anyBoolean())).thenReturn(host);

        final HTMLPageAssetRenderedAPI renderedAPI = new HTMLPageAssetRenderedAPIImpl();

        pageResource = new PageResource(
                PageResourceHelper.getInstance(),
                webResource,
                renderedAPI,
                APILocator.getContentletAPI(),
                hostWebAPI);
    }

    // -----------------------------------------------------------------------
    // Test 1: 404 when uri segment is null/empty (no page at root "/")
    // Previously a 400, now a 404 because the path is a JAX-RS path segment;
    // calling the method with null uri normalizes to "/" which finds no page.
    // -----------------------------------------------------------------------
    @Test(expected = NotFoundException.class)
    public void test_missing_uri_returns_404() throws Exception {
        pageResource.getRenderSources(request, response,
                null, null, null, null, null, null);
    }

    // -----------------------------------------------------------------------
    // Test 2: 404 unknown path
    // -----------------------------------------------------------------------
    @Test(expected = NotFoundException.class)
    public void test_unknown_path_returns_404() throws Exception {
        // Use host_id to point to our test host, plain unknown path
        pageResource.getRenderSources(request, response,
                "/this-page-does-not-exist-" + System.currentTimeMillis(),
                host.getIdentifier(), null, null, null, null);
    }

    // -----------------------------------------------------------------------
    // Test 3: 403 when user has no READ permission
    // -----------------------------------------------------------------------
    @Test(expected = DotSecurityException.class)
    public void test_no_permission_returns_403() throws Exception {
        // Create a page
        final Template template = new TemplateDataGen().nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        // Revoke READ on the page for the CMS_ANONYMOUS role
        final com.dotmarketing.business.Role anonRole = APILocator.getRoleAPI()
                .loadCMSAnonymousRole();
        final Permission noReadPerm = new Permission(
                page.getPermissionId(), anonRole.getId(), 0);
        APILocator.getPermissionAPI().save(noReadPerm, page, adminUser, false);

        // Plain user with no specific role grants
        final User limitedUser = new com.dotcms.datagen.UserDataGen().nextPersisted();

        // Call the helper directly — it should throw DotSecurityException
        PageResourceHelper.getInstance().getRenderSources(
                page.getURI(), host.getIdentifier(), null, null, null, null,
                limitedUser, new HTMLPageAssetRenderedAPIImpl());
    }

    // -----------------------------------------------------------------------
    // Test 4: DB container shape
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_db_container_shape() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

        // Create content type + container
        final com.dotcms.contenttype.model.type.ContentType ct =
                new ContentTypeDataGen().nextPersisted();
        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(ct.id());
        cs.setCode("$!{body}");
        final Container container = APILocator.getContainerAPI()
                .save(new ContainerDataGen().nextPersisted(), List.of(cs), host,
                        APILocator.systemUser(), false);
        APILocator.getVersionableAPI().setWorking(container);
        APILocator.getVersionableAPI().setLive(container);

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        // Add a contentlet of that type to the page (makes ct appear in onPage keys)
        final Contentlet content = new ContentletDataGen(ct.id())
                .languageId(defaultLang.getId())
                .host(host)
                .nextPersisted();
        content.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(content, adminUser, false);

        final MultiTree mt = new MultiTree(page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), UUIDGenerator.generateUuid(), 1);
        APILocator.getMultiTreeAPI().saveMultiTree(mt);

        final Response resp = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, null, null);

        assertEquals(200, resp.getStatus());
        final ResponseEntityView<PageRenderSourcesView> entity =
                (ResponseEntityView<PageRenderSourcesView>) resp.getEntity();
        final PageRenderSourcesView view = entity.getEntity();

        assertNotNull(view);
        assertNotNull(view.getPage());
        assertEquals(page.getIdentifier(), view.getPage().getIdentifier());
        // uri must be host-qualified
        assertNotNull("page.uri must not be null", view.getPage().getUri());
        assertTrue("page.uri must start with //", view.getPage().getUri().startsWith("//"));
        assertTrue("page.uri must contain the page path",
                view.getPage().getUri().endsWith(page.getURI()));
        assertFalse("Expected at least one container", view.getContainers().isEmpty());

        // containers is now a Map keyed by container identifier
        final ContainerSourceView containerView =
                view.getContainers().get(container.getIdentifier());

        assertNotNull("Should find the DB container keyed by its identifier", containerView);
        assertEquals(Source.DB.name(), containerView.getSource());

        // contentTypes must be non-null and contain the placed content type
        assertNotNull(containerView.getContentTypes());
        assertFalse("contentTypes should not be empty", containerView.getContentTypes().isEmpty());

        final ContentTypeEntryView ctEntry = containerView.getContentTypes().stream()
                .filter(e -> ct.variable().equals(e.getContentTypeVar()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find the content type in the container", ctEntry);
        // DB entries have no path or identifier
        assertNull("DB content type entry should have no path", ctEntry.getPath());
        assertNull("DB content type entry should have no identifier", ctEntry.getIdentifier());
    }

    // -----------------------------------------------------------------------
    // Test 5: FILE container shape
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_file_container_shape() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

        final com.dotcms.contenttype.model.type.ContentType ct =
                new ContentTypeDataGen().nextPersisted();

        final FileAssetContainer fac = new ContainerAsFileDataGen()
                .host(host)
                .contentType(ct, "$!{body}")
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .withContainer(fac.getIdentifier())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        // Place a contentlet so the FILE container's ct appears in onPageKeys
        final Contentlet content = new ContentletDataGen(ct.id())
                .languageId(defaultLang.getId())
                .host(host)
                .nextPersisted();
        content.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(content, adminUser, false);
        final MultiTree mt = new MultiTree(page.getIdentifier(), fac.getIdentifier(),
                content.getIdentifier(), UUIDGenerator.generateUuid(), 1);
        APILocator.getMultiTreeAPI().saveMultiTree(mt);

        final Response resp = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, null, null);

        assertEquals(200, resp.getStatus());
        final ResponseEntityView<PageRenderSourcesView> entity =
                (ResponseEntityView<PageRenderSourcesView>) resp.getEntity();
        final PageRenderSourcesView view = entity.getEntity();

        assertNotNull(view);

        // FILE container key is the host-qualified path (starts with //)
        final Map.Entry<String, ContainerSourceView> fileEntry = view.getContainers().entrySet()
                .stream()
                .filter(e -> Source.FILE.name().equals(e.getValue().getSource()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find a FILE container in the response", fileEntry);
        assertTrue("Map key for FILE container should start with //",
                fileEntry.getKey().startsWith("//"));

        final ContainerSourceView fileContainerView = fileEntry.getValue();
        assertEquals(Source.FILE.name(), fileContainerView.getSource());
        assertNotNull(fileContainerView.getContentTypes());
        assertFalse("contentTypes should not be empty for FILE container with placed content",
                fileContainerView.getContentTypes().isEmpty());

        final ContentTypeEntryView ctEntry = fileContainerView.getContentTypes().stream()
                .filter(e -> ct.variable().equals(e.getContentTypeVar()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find the FILE content type entry", ctEntry);
        // FILE entries carry path and identifier
        assertNotNull("FILE content type entry should have a path", ctEntry.getPath());
        assertNotNull("FILE content type entry should have an identifier", ctEntry.getIdentifier());
    }

    // -----------------------------------------------------------------------
    // Test 6: persona filtering — type placed under persona appears, absent without
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_persona_filtering() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final com.dotcms.contenttype.model.type.ContentType ct =
                new ContentTypeDataGen().nextPersisted();

        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(ct.id());
        cs.setCode("$!{body}");
        final Container container = APILocator.getContainerAPI()
                .save(new ContainerDataGen().nextPersisted(), List.of(cs), host,
                        APILocator.systemUser(), false);
        APILocator.getVersionableAPI().setWorking(container);
        APILocator.getVersionableAPI().setLive(container);

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        final Persona persona = new PersonaDataGen()
                .keyTag("persona" + System.currentTimeMillis())
                .hostFolder(host.getIdentifier())
                .nextPersisted();
        persona.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(persona, adminUser, false);

        final String personalization = Persona.DOT_PERSONA_PREFIX_SCHEME
                + StringPool.COLON + persona.getKeyTag();

        final Contentlet content = new ContentletDataGen(ct.id())
                .languageId(defaultLang.getId())
                .host(host)
                .nextPersisted();
        content.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(content, adminUser, false);

        // Save multi-tree ONLY under persona personalization (not default)
        final MultiTree mt = new MultiTree(page.getIdentifier(), container.getIdentifier(),
                content.getIdentifier(), UUIDGenerator.generateUuid(), 1, personalization);
        APILocator.getMultiTreeAPI().saveMultiTree(mt);

        // ------ Call WITH persona: ct should be present ------
        final Response respPersona = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null,
                persona.getIdentifier(), null, null);
        assertEquals(200, respPersona.getStatus());
        final PageRenderSourcesView viewPersona =
                ((ResponseEntityView<PageRenderSourcesView>) respPersona.getEntity()).getEntity();

        final ContainerSourceView cvPersona =
                viewPersona.getContainers().get(container.getIdentifier());
        assertNotNull("Container should be in response under persona", cvPersona);
        final boolean ctPresentUnderPersona = cvPersona.getContentTypes() != null
                && cvPersona.getContentTypes().stream()
                        .anyMatch(e -> ct.variable().equals(e.getContentTypeVar()));
        assertTrue("Content type should be present under persona", ctPresentUnderPersona);

        // ------ Call WITHOUT persona: ct should be absent ------
        final Response respDefault = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, null, null);
        assertEquals(200, respDefault.getStatus());
        final PageRenderSourcesView viewDefault =
                ((ResponseEntityView<PageRenderSourcesView>) respDefault.getEntity()).getEntity();

        final ContainerSourceView cvDefault =
                viewDefault.getContainers().get(container.getIdentifier());
        // Either container absent or contentTypes empty / ct not present
        final boolean ctPresentUnderDefault = cvDefault != null
                && cvDefault.getContentTypes() != null
                && cvDefault.getContentTypes().stream()
                        .anyMatch(e -> ct.variable().equals(e.getContentTypeVar()));
        assertFalse("Content type should NOT be present under default persona", ctPresentUnderDefault);
    }

    // -----------------------------------------------------------------------
    // Test 7: variant filtering — type placed under variant appears
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_variant_filtering() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final com.dotcms.contenttype.model.type.ContentType ct =
                new ContentTypeDataGen().nextPersisted();

        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(ct.id());
        cs.setCode("$!{body}");
        final Container container = APILocator.getContainerAPI()
                .save(new ContainerDataGen().nextPersisted(), List.of(cs), host,
                        APILocator.systemUser(), false);
        APILocator.getVersionableAPI().setWorking(container);
        APILocator.getVersionableAPI().setLive(container);

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        final Variant variant = new VariantDataGen().nextPersisted();

        final Contentlet content = new ContentletDataGen(ct.id())
                .languageId(defaultLang.getId())
                .host(host)
                .nextPersisted();
        content.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(content, adminUser, false);

        // Save multi-tree ONLY under variant
        final MultiTree mt = new MultiTree()
                .setHtmlPage(page.getIdentifier())
                .setContainer(container.getIdentifier())
                .setContentlet(content.getIdentifier())
                .setInstanceId(UUIDGenerator.generateUuid())
                .setTreeOrder(1)
                .setVariantId(variant.name());
        APILocator.getMultiTreeAPI().saveMultiTree(mt);

        final Response resp = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, variant.name(), null);

        assertEquals(200, resp.getStatus());
        final PageRenderSourcesView view =
                ((ResponseEntityView<PageRenderSourcesView>) resp.getEntity()).getEntity();

        final ContainerSourceView containerView =
                view.getContainers().get(container.getIdentifier());
        assertNotNull("Container should appear under variant", containerView);
        final ContentTypeEntryView ctEntry = containerView.getContentTypes() == null ? null
                : containerView.getContentTypes().stream()
                        .filter(e -> ct.variable().equals(e.getContentTypeVar()))
                        .findFirst().orElse(null);
        assertNotNull("Content type should be present under variant", ctEntry);
    }

    // -----------------------------------------------------------------------
    // Test 8: Host resolution via host_id query param targeting a non-default site.
    //
    // This is the discriminating case for host resolution: the page lives on a
    // fresh SiteDataGen site (not the default dotCMS host).  Passing host_id causes
    // resolveHostForRenderSources to call hostAPI.find(hostId) and return that exact
    // site.  Without host_id the fallback is findDefaultHost, which would be a
    // different site and would NOT find the page.
    //
    // The //host/uri form is intentionally NOT tested here — dotCMS's
    // NormalizationFilter rejects any URI containing "//" before it reaches the
    // resource, so that form cannot work in this stack.
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_host_resolution_via_host_id_non_default_site() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final Template template = new TemplateDataGen().nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        // `host` is a fresh SiteDataGen site (non-default) created in @Before.
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .pageURL("index" + System.currentTimeMillis())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        // Confirm `host` is not the default host — wrong host_id would 404.
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(adminUser, false);
        assertFalse("Test requires a non-default host so wrong resolution is detectable",
                host.getIdentifier().equals(defaultHost.getIdentifier()));

        // Pass host_id = non-default site identifier.  No //hostname in the path.
        final Response resp = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, null, null);

        assertEquals("host_id targeting non-default site should return 200", 200, resp.getStatus());
        final ResponseEntityView<PageRenderSourcesView> entity =
                (ResponseEntityView<PageRenderSourcesView>) resp.getEntity();
        assertNotNull(entity.getEntity());
        assertEquals("page.identifier must match the page on the non-default site",
                page.getIdentifier(), entity.getEntity().getPage().getIdentifier());
        assertNotNull("page.uri must not be null", entity.getEntity().getPage().getUri());
        assertTrue("page.uri must be host-qualified (starts with //)",
                entity.getEntity().getPage().getUri().startsWith("//"));
        assertTrue("page.uri must reference the correct non-default host",
                entity.getEntity().getPage().getUri().contains(host.getHostname()));
    }

    // -----------------------------------------------------------------------
    // Test 9: Theme block
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_theme_block() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

        // Create a theme folder and a .vtl file inside it
        final Folder themesParent = APILocator.getFolderAPI()
                .createFolders("/application/themes/", host,
                        APILocator.systemUser(), false);
        final String themeName = "testtheme" + System.currentTimeMillis();
        final Folder themeFolder = new FolderDataGen()
                .site(host)
                .parent(themesParent)
                .name(themeName)
                .nextPersisted();

        // Create a test VTL file in the theme folder
        final File tempVtl = File.createTempFile("header", ".vtl");
        Files.writeString(tempVtl.toPath(), "#* test vtl *#");
        final Contentlet vtlContentlet = new FileAssetDataGen(themeFolder, tempVtl)
                .languageId(defaultLang.getId())
                .nextPersisted();
        vtlContentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(vtlContentlet, adminUser, false);

        // Create a template that uses this theme
        final Template template = new TemplateDataGen()
                .theme("//" + host.getHostname() + themeFolder.getPath())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        final Response resp = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, null, null);

        assertEquals(200, resp.getStatus());
        final ResponseEntityView<PageRenderSourcesView> entity =
                (ResponseEntityView<PageRenderSourcesView>) resp.getEntity();
        final PageRenderSourcesView view = entity.getEntity();

        // The template references a real theme folder created above, so it must resolve.
        assertNotNull("Theme should resolve for a template with a theme folder",
                view.getTheme());
        assertNotNull(view.getTheme().getId());
        assertNotNull(view.getTheme().getFolderPath());
        assertTrue("folderPath should start with //",
                view.getTheme().getFolderPath().startsWith("//"));
    }

    // -----------------------------------------------------------------------
    // Test 10: CODE widget — inline widgetCode, no file reference
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_code_widget_shape() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

        // Create a Widget content type with no file fields (inline widgetCode style)
        final String widgetVar = "TestCodeWidget" + System.currentTimeMillis();
        final com.dotcms.contenttype.model.type.ContentType widgetCT =
                new ContentTypeDataGen()
                        .baseContentType(BaseContentType.WIDGET)
                        .velocityVarName(widgetVar)
                        .nextPersisted();

        // Create a container that accepts the widget type
        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(widgetCT.id());
        cs.setCode("$!{widgetCode}");
        final Container container = APILocator.getContainerAPI()
                .save(new ContainerDataGen().nextPersisted(), List.of(cs), host,
                        APILocator.systemUser(), false);
        APILocator.getVersionableAPI().setWorking(container);
        APILocator.getVersionableAPI().setLive(container);

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        // Place a widget contentlet on the page
        final Contentlet widgetContent = new ContentletDataGen(widgetCT.id())
                .languageId(defaultLang.getId())
                .host(host)
                .setProperty("code", "#set($x=1)")
                .nextPersisted();
        widgetContent.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(widgetContent, adminUser, false);

        final MultiTree mt = new MultiTree(page.getIdentifier(), container.getIdentifier(),
                widgetContent.getIdentifier(), UUIDGenerator.generateUuid(), 1);
        APILocator.getMultiTreeAPI().saveMultiTree(mt);

        final Response resp = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, null, null);

        assertEquals(200, resp.getStatus());
        final PageRenderSourcesView view =
                ((ResponseEntityView<PageRenderSourcesView>) resp.getEntity()).getEntity();

        assertNotNull("widgets list must not be null", view.getWidgets());
        final WidgetSourceView widgetView = view.getWidgets().stream()
                .filter(w -> widgetCT.variable().equals(w.getContentTypeVar()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find the CODE widget in the response", widgetView);

        // contentletId must always be present and stable
        assertNotNull("contentletId must be set", widgetView.getContentletId());
        assertEquals("contentletId must match the placed contentlet",
                widgetContent.getIdentifier(), widgetView.getContentletId());

        // contentletInode must be set and match the working version (PREVIEW_MODE → working)
        assertNotNull("contentletInode must be set", widgetView.getContentletInode());
        assertEquals("contentletInode must match the placed contentlet's working inode",
                widgetContent.getInode(), widgetView.getContentletInode());

        // source must be CODE; no path/identifier for inline widget
        assertEquals("source must be CODE for inline widget",
                WidgetSourceView.Source.CODE.name(), widgetView.getSource());
        assertNull("path must be null for CODE widget", widgetView.getPath());
        assertNull("identifier must be null for CODE widget", widgetView.getIdentifier());
    }

    // -----------------------------------------------------------------------
    // Test 11: FILE widget — file-backed VTL, path + identifier populated
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_file_widget_shape() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

        // Create a VTL file asset that will back the widget
        final Folder widgetFolder = new FolderDataGen().site(host).nextPersisted();
        final File tempVtl = File.createTempFile("widget", ".vtl");
        Files.writeString(tempVtl.toPath(), "#set($x='hello')");
        final Contentlet vtlContentlet = new FileAssetDataGen(widgetFolder, tempVtl)
                .languageId(defaultLang.getId())
                .nextPersisted();
        vtlContentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(vtlContentlet, adminUser, false);

        // Widget content type with a FileField pointing to the VTL
        final String widgetVar = "TestFileWidget" + System.currentTimeMillis();
        final com.dotcms.contenttype.model.field.Field fileField =
                new FieldDataGen()
                        .name("VtlFile")
                        .velocityVarName("vtlFile")
                        .type(com.dotcms.contenttype.model.field.FileField.class)
                        .next();
        final com.dotcms.contenttype.model.type.ContentType widgetCT =
                new ContentTypeDataGen()
                        .baseContentType(BaseContentType.WIDGET)
                        .velocityVarName(widgetVar)
                        .field(fileField)
                        .nextPersisted();

        // Container
        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(widgetCT.id());
        cs.setCode("$!{code}");
        final Container container = APILocator.getContainerAPI()
                .save(new ContainerDataGen().nextPersisted(), List.of(cs), host,
                        APILocator.systemUser(), false);
        APILocator.getVersionableAPI().setWorking(container);
        APILocator.getVersionableAPI().setLive(container);

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(page);
        APILocator.getVersionableAPI().setLive(page);

        // Widget contentlet with the file field pointing to the VTL asset
        final Contentlet widgetContent = new ContentletDataGen(widgetCT.id())
                .languageId(defaultLang.getId())
                .host(host)
                .setProperty("vtlFile", vtlContentlet.getIdentifier())
                .nextPersisted();
        widgetContent.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(widgetContent, adminUser, false);

        final MultiTree mt = new MultiTree(page.getIdentifier(), container.getIdentifier(),
                widgetContent.getIdentifier(), UUIDGenerator.generateUuid(), 1);
        APILocator.getMultiTreeAPI().saveMultiTree(mt);

        final Response resp = pageResource.getRenderSources(request, response,
                page.getURI(), host.getIdentifier(), null, null, null, null);

        assertEquals(200, resp.getStatus());
        final PageRenderSourcesView view =
                ((ResponseEntityView<PageRenderSourcesView>) resp.getEntity()).getEntity();

        assertNotNull("widgets list must not be null", view.getWidgets());
        final WidgetSourceView widgetView = view.getWidgets().stream()
                .filter(w -> widgetCT.variable().equals(w.getContentTypeVar()))
                .findFirst()
                .orElse(null);
        assertNotNull("Should find the FILE widget in the response", widgetView);

        // contentletId must always be present and stable
        assertNotNull("contentletId must be set", widgetView.getContentletId());
        assertEquals("contentletId must match the placed contentlet",
                widgetContent.getIdentifier(), widgetView.getContentletId());

        // contentletInode must be set and match the working version (PREVIEW_MODE → working)
        assertNotNull("contentletInode must be set", widgetView.getContentletInode());
        assertEquals("contentletInode must match the placed contentlet's working inode",
                widgetContent.getInode(), widgetView.getContentletInode());

        // source must be FILE and path/identifier must be populated
        assertEquals("source must be FILE for file-backed widget",
                WidgetSourceView.Source.FILE.name(), widgetView.getSource());
        assertNotNull("path must be set for FILE widget", widgetView.getPath());
        assertTrue("path should start with //", widgetView.getPath().startsWith("//"));
        assertNotNull("identifier must be set for FILE widget", widgetView.getIdentifier());
    }

    // -----------------------------------------------------------------------
    // Test 12: URL-mapped content resolves to detail page; urlContentMap populated.
    //
    // HOST RESOLUTION PATH COVERAGE
    // -----------------------------------------------------------------------
    // PageResourceHelper.getRenderSources builds a FakeHttpRequest mock chain and
    // calls renderedAPI.getHtmlPageAsset(context, mockReq).  Internally that calls
    // HostWebAPIImpl.getCurrentHostFromRequest which resolves the host in priority:
    //   1. getParameter("host_id") when user.isBackendUser()  ← PRIMARY (our fix)
    //   2. getParameter/getAttribute(Host.HOST_VELOCITY_VAR_NAME)
    //   3. getAttribute(WebKeys.CURRENT_HOST)
    //   4. resolveHostName(serverName)                        ← fragile fallback
    //
    // The mapped contentlet lives on a FRESH SiteDataGen site (not the default
    // dotCMS host).  If host resolution falls through to option-4 it would return
    // the default host, URLMapAPIImpl.processURLMap would find nothing on it, and
    // the request would produce a 404 instead of the expected urlContentMap.
    //
    // Our fix embeds "?host_id=<identifier>" in the FakeHttpRequest URI so that
    // option-1 fires for backend users and returns the exact resolved host
    // deterministically.  Options 2 and 3 are set as belt-and-suspenders attributes.
    // -----------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    public void test_urlmap_resolves_detail_page_and_populates_urlContentMap() throws Exception {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

        // `host` is a fresh SiteDataGen site (set up in @Before) — it is NOT the
        // default dotCMS host.  This is deliberate: if mock-request host resolution
        // falls back to option-4 (resolveHostName), it would return the default host
        // instead of `host`, causing URLMapAPIImpl.processURLMap to query the wrong
        // site and produce a 404.
        final Host defaultHost = APILocator.getHostAPI()
                .findDefaultHost(adminUser, false);
        assertFalse("Test requires a non-default host so that wrong host resolution is detectable",
                host.getIdentifier().equals(defaultHost.getIdentifier()));

        // Build a detail page on `host` that will be the landing page for URL-mapped content
        final Template template = new TemplateDataGen().nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);

        final Folder detailFolder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset detailPage = new HTMLPageDataGen(detailFolder, template)
                .languageId(defaultLang.getId())
                .nextPersisted();
        APILocator.getVersionableAPI().setWorking(detailPage);
        APILocator.getVersionableAPI().setLive(detailPage);

        // Create a content type with a URL map pattern pointing to the detail page
        final String urlPattern = "/testblog" + System.currentTimeMillis() + "/{urlTitle}";
        final com.dotcms.contenttype.model.type.ContentType mappedCT =
                new ContentTypeDataGen()
                        .urlMapPattern(urlPattern)
                        .detailPage(detailPage.getIdentifier())
                        .nextPersisted();

        // Create the mapped contentlet on `host` (the non-default site).
        // processURLMap must be called with the correct host or it finds nothing.
        final Contentlet mappedContent = new ContentletDataGen(mappedCT.id())
                .languageId(defaultLang.getId())
                .host(host)
                .setProperty("urlTitle", "my-test-post")
                .nextPersisted();
        mappedContent.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(mappedContent, adminUser, false);

        // Build the URL-mapped URI (matches the pattern)
        final String mappedUri = urlPattern.replace("{urlTitle}", "my-test-post");

        // getRenderSources is called with host.getIdentifier() so that the helper
        // resolves the correct host in step-1 and then embeds it as "?host_id=<id>"
        // in the FakeHttpRequest URI (option-1 of getCurrentHostFromRequest).
        // If the host_id param were absent from the mock request, getCurrentHost would
        // fall through to the fragile serverName fallback and resolve the DEFAULT host
        // instead of `host`, causing processURLMap to find no matching contentlet → 404.
        final Response resp = pageResource.getRenderSources(request, response,
                mappedUri, host.getIdentifier(), null, null, null, null);

        assertEquals("URL-mapped request on non-default host should return 200 — "
                + "failure here means mock-request host resolution picked the wrong host",
                200, resp.getStatus());
        final ResponseEntityView<PageRenderSourcesView> entity =
                (ResponseEntityView<PageRenderSourcesView>) resp.getEntity();
        final PageRenderSourcesView view = entity.getEntity();

        assertNotNull("Response entity must not be null", view);
        assertNotNull("page block must be present", view.getPage());

        // page.identifier must be the DETAIL page, not the mapped contentlet
        assertEquals("page.identifier must be the detail page identifier",
                detailPage.getIdentifier(), view.getPage().getIdentifier());

        // page.uri must be host-qualified and contain the ORIGINAL requested URI
        assertNotNull("page.uri must not be null", view.getPage().getUri());
        assertTrue("page.uri must be host-qualified (start with //)",
                view.getPage().getUri().startsWith("//"));
        assertTrue("page.uri must contain the originally requested mapped URI",
                view.getPage().getUri().contains(mappedUri));

        // urlContentMap must be populated for URL-mapped requests
        assertNotNull("urlContentMap must be present for URL-mapped page", view.getUrlContentMap());
        assertEquals("urlContentMap.contentTypeVar must match the mapped content type",
                mappedCT.variable(), view.getUrlContentMap().getContentTypeVar());
        assertEquals("urlContentMap.contentletId must match the mapped contentlet identifier — "
                + "wrong host resolution would return a different contentlet or null",
                mappedContent.getIdentifier(), view.getUrlContentMap().getContentletId());
        assertNotNull("urlContentMap.contentletInode must be set",
                view.getUrlContentMap().getContentletInode());
        assertNotNull("urlContentMap.title must be set", view.getUrlContentMap().getTitle());

        // For regular (non-mapped) pages, urlContentMap must be null
        final Response regularResp = pageResource.getRenderSources(request, response,
                detailPage.getURI(), host.getIdentifier(), null, null, null, null);
        assertEquals(200, regularResp.getStatus());
        final PageRenderSourcesView regularView =
                ((ResponseEntityView<PageRenderSourcesView>) regularResp.getEntity()).getEntity();
        assertNull("urlContentMap must be absent for regular page requests",
                regularView.getUrlContentMap());
    }
}
