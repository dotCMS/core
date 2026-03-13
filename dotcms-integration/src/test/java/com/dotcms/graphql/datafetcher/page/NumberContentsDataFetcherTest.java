package com.dotcms.graphql.datafetcher.page;

import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for {@link NumberContentsDataFetcher}.
 */
public class NumberContentsDataFetcherTest {

    private static User user;
    private static Language defaultLanguage;
    private static Host defaultHost;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        defaultHost = APILocator.getHostAPI().findDefaultHost(user, false);
    }

    /**
     * MethodToTest: {@link NumberContentsDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Page with no contentlets placed in any container.
     * Expected Result: Returns 0.
     */
    @Test
    public void testGet_WithNoContents() throws Exception {
        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), "1")
                .nextPersisted();
        final Folder folder = new FolderDataGen().site(defaultHost).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final var fetcher = new NumberContentsDataFetcher();
        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        final DotGraphQLContext context = DotGraphQLContext.createServletContext()
                .with(user)
                .build();
        context.addParam("pageMode", PageMode.PREVIEW_MODE.name());
        context.addParam("languageId", String.valueOf(defaultLanguage.getId()));

        Mockito.when(environment.getContext()).thenReturn(context);
        Mockito.when(environment.getSource()).thenReturn(page);

        final Integer result = fetcher.get(environment);
        assertEquals(Integer.valueOf(0), result);
    }

    /**
     * MethodToTest: {@link NumberContentsDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Page with 3 contentlets spread across 2 containers (2 in container 1,
     * 1 in container 2).
     * Expected Result: Returns 3.
     */
    @Test
    public void testGet_WithContents() throws Exception {
        final ContentType contentType = APILocator.getContentTypeAPI(user)
                .findByType(BaseContentType.CONTENT)
                .stream()
                .filter(ct -> !ct.system())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No non-system CONTENT type found"));

        final Container container1 = new ContainerDataGen()
                .withContentType(contentType, "")
                .nextPersisted();
        final Container container2 = new ContainerDataGen()
                .withContentType(contentType, "")
                .nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container1.getIdentifier(), "1")
                .withContainer(container2.getIdentifier(), "2")
                .nextPersisted();
        final Folder folder = new FolderDataGen().site(defaultHost).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet c1 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();
        final Contentlet c2 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();
        final Contentlet c3 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();

        // container1 gets 2, container2 gets 1
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container1.getIdentifier(), c1.getIdentifier(), "1", 0));
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container1.getIdentifier(), c2.getIdentifier(), "1", 1));
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container2.getIdentifier(), c3.getIdentifier(), "2", 0));

        final var fetcher = new NumberContentsDataFetcher();
        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        final DotGraphQLContext context = DotGraphQLContext.createServletContext()
                .with(user)
                .build();
        context.addParam("pageMode", PageMode.PREVIEW_MODE.name());
        context.addParam("languageId", String.valueOf(defaultLanguage.getId()));

        Mockito.when(environment.getContext()).thenReturn(context);
        Mockito.when(environment.getSource()).thenReturn(page);

        final Integer result = fetcher.get(environment);
        assertEquals(Integer.valueOf(3), result);
    }

    /**
     * MethodToTest: {@link NumberContentsDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: A {@link PageRenderUtil} is already cached in the context (as populated by
     * {@link ContainersDataFetcher}). The {@code pageMode} and {@code languageId} params are
     * intentionally absent — if the fetcher tried to build a new {@link PageRenderUtil} it would
     * throw a NullPointerException, proving the cached instance is used.
     * Expected Result: Returns the correct count from the pre-built PageRenderUtil.
     */
    @Test
    public void testGet_WithCachedPageRenderUtil() throws Exception {
        final ContentType contentType = APILocator.getContentTypeAPI(user)
                .findByType(BaseContentType.CONTENT)
                .stream()
                .filter(ct -> !ct.system())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No non-system CONTENT type found"));

        final Container container = new ContainerDataGen()
                .withContentType(contentType, "")
                .nextPersisted();
        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier(), "1")
                .nextPersisted();
        final Folder folder = new FolderDataGen().site(defaultHost).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet c1 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();
        final Contentlet c2 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();

        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c1.getIdentifier(), "1", 0));
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c2.getIdentifier(), "1", 1));

        // Pre-build the PageRenderUtil and cache it in the context
        final PageRenderUtil pageRenderUtil = new PageRenderUtil(
                page, user, PageMode.PREVIEW_MODE, defaultLanguage.getId(), defaultHost);

        final var fetcher = new NumberContentsDataFetcher();
        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        final DotGraphQLContext context = DotGraphQLContext.createServletContext()
                .with(user)
                .build();
        // Intentionally omit "pageMode" and "languageId" — a NPE would occur if cache were bypassed
        context.addParam("pageRenderUtil", pageRenderUtil);

        Mockito.when(environment.getContext()).thenReturn(context);
        Mockito.when(environment.getSource()).thenReturn(page);

        final Integer result = fetcher.get(environment);
        assertEquals(Integer.valueOf(2), result);
    }
}
