package com.dotcms.graphql.datafetcher.page;

import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
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
     * Given Scenario: pageMode is not EDIT_MODE (e.g. LIVE).
     * Expected Result: Returns null — numberContents is only computed in edit mode.
     */
    @Test
    public void testGet_ReturnsNullWhenNotEditMode() throws Exception {
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
        context.addParam("pageMode", PageMode.LIVE.name());
        context.addParam("languageId", String.valueOf(defaultLanguage.getId()));

        Mockito.when(environment.getContext()).thenReturn(context);
        Mockito.when(environment.getSource()).thenReturn(page);

        final Integer result = fetcher.get(environment);
        assertEquals(null, result);
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
        context.addParam("pageMode", PageMode.EDIT_MODE.name());
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
        context.addParam("pageMode", PageMode.EDIT_MODE.name());
        context.addParam("languageId", String.valueOf(defaultLanguage.getId()));

        Mockito.when(environment.getContext()).thenReturn(context);
        Mockito.when(environment.getSource()).thenReturn(page);

        final Integer result = fetcher.get(environment);
        assertEquals(Integer.valueOf(3), result);
    }

    /**
     * MethodToTest: {@link NumberContentsDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Page has 3 contentlets placed, all exist only in the default language.
     * The request asks for a second language that has no content.
     * Expected Result: Returns 0 — none of the placements have a version in the requested language.
     */
    @Test
    public void testGet_ReturnsZero_WhenContentNotInRequestedLanguage() throws Exception {
        final Language secondLanguage = new LanguageDataGen().nextPersisted();

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
        final Contentlet c3 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();

        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c1.getIdentifier(), "1", 0));
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c2.getIdentifier(), "1", 1));
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c3.getIdentifier(), "1", 2));

        final var fetcher = new NumberContentsDataFetcher();
        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        final DotGraphQLContext context = DotGraphQLContext.createServletContext()
                .with(user)
                .build();
        context.addParam("pageMode", PageMode.EDIT_MODE.name());
        context.addParam("languageId", String.valueOf(secondLanguage.getId()));

        Mockito.when(environment.getContext()).thenReturn(context);
        Mockito.when(environment.getSource()).thenReturn(page);

        final Integer result = fetcher.get(environment);
        assertEquals(Integer.valueOf(0), result);
    }

    /**
     * MethodToTest: {@link NumberContentsDataFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Page has 3 contentlets placed. 2 are translated into a second language,
     * 1 exists only in the default language. Requesting the second language should count only
     * the 2 translated contentlets; requesting the default language should count all 3.
     * Expected Result: 2 for the second language, 3 for the default language.
     */
    @Test
    public void testGet_CountsOnlyContentInRequestedLanguage() throws Exception {
        final Language secondLanguage = new LanguageDataGen().nextPersisted();

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

        // c1 and c2 exist in both languages (translated); c3 exists only in the default language
        final Contentlet c1 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();
        final Contentlet c2 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();
        final Contentlet c3 = new ContentletDataGen(contentType)
                .host(defaultHost).languageId(defaultLanguage.getId()).nextPersisted();

        // Translate c1 and c2 into the second language (same identifier, new inode)
        final Contentlet c1Translated = APILocator.getContentletAPI().checkout(c1.getInode(), user, false);
        c1Translated.setLanguageId(secondLanguage.getId());
        APILocator.getContentletAPI().checkin(c1Translated, user, false);

        final Contentlet c2Translated = APILocator.getContentletAPI().checkout(c2.getInode(), user, false);
        c2Translated.setLanguageId(secondLanguage.getId());
        APILocator.getContentletAPI().checkin(c2Translated, user, false);

        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c1.getIdentifier(), "1", 0));
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c2.getIdentifier(), "1", 1));
        APILocator.getMultiTreeAPI().saveMultiTree(
                new MultiTree(page.getIdentifier(), container.getIdentifier(), c3.getIdentifier(), "1", 2));

        final var fetcher = new NumberContentsDataFetcher();
        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        // Default language: all 3 placements have a version → 3
        final DotGraphQLContext defaultLangContext = DotGraphQLContext.createServletContext()
                .with(user)
                .build();
        defaultLangContext.addParam("pageMode", PageMode.EDIT_MODE.name());
        defaultLangContext.addParam("languageId", String.valueOf(defaultLanguage.getId()));

        Mockito.when(environment.getContext()).thenReturn(defaultLangContext);
        Mockito.when(environment.getSource()).thenReturn(page);

        assertEquals(Integer.valueOf(3), fetcher.get(environment));

        // Second language: only c1 and c2 are translated → 2
        final DotGraphQLContext secondLangContext = DotGraphQLContext.createServletContext()
                .with(user)
                .build();
        secondLangContext.addParam("pageMode", PageMode.EDIT_MODE.name());
        secondLangContext.addParam("languageId", String.valueOf(secondLanguage.getId()));

        Mockito.when(environment.getContext()).thenReturn(secondLangContext);

        assertEquals(Integer.valueOf(2), fetcher.get(environment));
    }
}
