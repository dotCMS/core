package com.dotcms.enterprise.publishing.staticpublishing;

import static com.dotcms.contenttype.model.type.VanityUrlContentType.URI_FIELD_VAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.VanityUrlDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.startup.runonce.Task260408CreateS3VanityAliasTable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class S3VanityStaticPublishingIntegrationTest {

    private static final String BUCKET_NAME = "static-vanity-test";
    private static final String BUCKET_REGION = "us-east-1";
    private static final String BUCKET_PREFIX = "root";

    private S3VanityAliasRepository repository;
    private S3VanityAliasService service;
    private AWSS3EndPointPublisher endpointPublisher;
    private String endpointId;
    private List<String> pushedPaths;
    private List<String> deletedPaths;
    private Map<String, String> pushedContentByPath;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        new Task260408CreateS3VanityAliasTable().executeUpgrade();
    }

    @Before
    public void prepareTest() throws Exception {
        repository = new S3VanityAliasRepository();
        service = new S3VanityAliasService();
        endpointPublisher = mock(AWSS3EndPointPublisher.class);
        endpointId = "s3-vanity-it-" + UUID.randomUUID();
        pushedPaths = new ArrayList<>();
        deletedPaths = new ArrayList<>();
        pushedContentByPath = new LinkedHashMap<>();
        recordEndpointCalls();
    }

    /**
     * Method to Test: {@link S3VanityAliasService#publishAliasForVanityUrl(S3VanityAliasPublishContext, Contentlet)}
     * Given Scenario: A live Vanity URL points to a live dotCMS page
     * ExpectedResult: The service should push the vanity clone and persist the mapping row.
     */
    @Test
    public void publishAliasForVanityUrlShouldPublishCloneAndPersistMapping() throws Exception {
        final PageFixture source = createLivePage("source", "published source content");
        final Contentlet vanity = createLiveVanity(source, "/alias", source.path);

        service.publishAliasForVanityUrl(context(source), vanity);

        verify(endpointPublisher).pushFileToEndpoint(eq(BUCKET_NAME), eq(BUCKET_REGION),
                eq(BUCKET_PREFIX), eq("/alias"), any(File.class));
        assertEquals(List.of("/alias"), pushedPaths);
        assertTrue(pushedContentByPath.get("/alias").contains("published source content"));

        final List<S3VanityAlias> aliases = repository.findByVanityUrlId(endpointId,
                source.language.getId(), vanity.getIdentifier());
        assertEquals(1, aliases.size());
        assertAlias(aliases.get(0), source, source.path, "/alias", vanity.getIdentifier());
    }

    /**
     * Method to Test: {@link S3VanityAliasService#publishAliasForVanityUrl(S3VanityAliasPublishContext, Contentlet)}
     * Given Scenario: An already materialized Vanity URL changes its URI
     * ExpectedResult: The old S3 clone should be deleted and the mapping should point to the new URI.
     */
    @Test
    public void publishAliasForVanityUrlShouldDeleteOldCloneWhenVanityPathChanges() throws Exception {
        final PageFixture source = createLivePage("source-change", "changed source content");
        Contentlet vanity = createLiveVanity(source, "/old-alias", source.path);
        service.publishAliasForVanityUrl(context(source), vanity);

        vanity = updateVanityUri(vanity, "/new-alias");
        service.publishAliasForVanityUrl(context(source), vanity);

        verify(endpointPublisher).deleteFilesFromEndpoint(eq(BUCKET_NAME), eq(BUCKET_PREFIX),
                eq("/old-alias"));
        verify(endpointPublisher).pushFileToEndpoint(eq(BUCKET_NAME), eq(BUCKET_REGION),
                eq(BUCKET_PREFIX), eq("/new-alias"), any(File.class));
        assertTrue(deletedPaths.contains("/old-alias"));
        assertTrue(pushedPaths.contains("/new-alias"));

        final List<S3VanityAlias> aliases = repository.findByVanityUrlId(endpointId,
                source.language.getId(), vanity.getIdentifier());
        assertEquals(1, aliases.size());
        assertAlias(aliases.get(0), source, source.path, "/new-alias", vanity.getIdentifier());
    }

    /**
     * Method to Test: {@link S3VanityAliasService#unpublishAliasesByVanityUrl(S3VanityAliasCleanupContext, long, String)}
     * Given Scenario: A materialized Vanity URL is removed and its URI does not hide a live resource
     * ExpectedResult: The clone should be deleted and the mapping row should be removed.
     */
    @Test
    public void unpublishAliasesByVanityUrlShouldDeleteCloneAndMappingWhenNoResourceIsShadowed()
            throws Exception {
        final PageFixture source = createLivePage("source-delete", "source to delete");
        final Contentlet vanity = createLiveVanity(source, "/orphan-alias", source.path);
        service.publishAliasForVanityUrl(context(source), vanity);

        service.unpublishAliasesByVanityUrl(cleanupContext(), source.language.getId(), vanity.getIdentifier());

        verify(endpointPublisher).deleteFilesFromEndpoint(eq(BUCKET_NAME), eq(BUCKET_PREFIX),
                eq("/orphan-alias"));
        assertTrue(deletedPaths.contains("/orphan-alias"));
        assertTrue(repository.findByVanityUrlId(endpointId, source.language.getId(),
                vanity.getIdentifier()).isEmpty());
    }

    /**
     * Method to Test: {@link S3VanityAliasService#unpublishAliasesByVanityUrl(S3VanityAliasCleanupContext, long, String)}
     * Given Scenario: A materialized Vanity URL is removed and its URI shadows a live dotCMS page
     * ExpectedResult: The live page should be restored on the same S3 key and the mapping should be removed.
     */
    @Test
    public void unpublishAliasesByVanityUrlShouldRestoreShadowedLiveResourceAndRemoveMapping()
            throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final PageFixture source = createLivePage(host, language, "source-shadow",
                "vanity source content");
        final PageFixture shadowed = createLivePage(host, language, "shadowed-alias",
                "shadowed original content");
        final Contentlet vanity = createLiveVanity(source, shadowed.path, source.path);
        service.publishAliasForVanityUrl(context(source), vanity);
        pushedPaths.clear();
        pushedContentByPath.clear();
        clearInvocations(endpointPublisher);

        service.unpublishAliasesByVanityUrl(cleanupContext(), source.language.getId(), vanity.getIdentifier());

        verify(endpointPublisher, never()).deleteFilesFromEndpoint(eq(BUCKET_NAME), eq(BUCKET_PREFIX),
                eq(shadowed.path));
        verify(endpointPublisher).pushFileToEndpoint(eq(BUCKET_NAME), eq(BUCKET_REGION),
                eq(BUCKET_PREFIX), eq(shadowed.path), any(File.class));
        assertEquals(List.of(shadowed.path), pushedPaths);
        assertTrue(pushedContentByPath.get(shadowed.path).contains("shadowed original content"));
        assertTrue(repository.findByVanityUrlId(endpointId, source.language.getId(),
                vanity.getIdentifier()).isEmpty());
    }

    /**
     * Method to Test: {@link S3VanityAliasService#publishAliases(S3VanityAliasContext)}
     * Given Scenario: A canonical page with an existing materialized Vanity URL is republished
     * ExpectedResult: The existing vanity clone should be refreshed without creating new mappings.
     */
    @Test
    public void publishAliasesShouldRefreshExistingCloneWhenCanonicalContentIsRepublished()
            throws Exception {
        final PageFixture source = createLivePage("source-republish", "initial source content");
        final Contentlet vanity = createLiveVanity(source, "/republished-alias", source.path);
        service.publishAliasForVanityUrl(context(source), vanity);
        pushedPaths.clear();
        pushedContentByPath.clear();

        final File republishedFile = temporaryStaticFile("updated static content");
        service.publishAliases(aliasContext(source, republishedFile));

        verify(endpointPublisher).pushFileToEndpoint(eq(BUCKET_NAME), eq(BUCKET_REGION),
                eq(BUCKET_PREFIX), eq("/republished-alias"), eq(republishedFile));
        assertEquals(List.of("/republished-alias"), pushedPaths);
        assertEquals("updated static content", pushedContentByPath.get("/republished-alias"));
        assertEquals(1, repository.findByLookup(lookup(source)).size());
    }

    /**
     * Method to Test: {@link S3VanityAliasService#unpublishAliases(S3VanityAliasContext)}
     * Given Scenario: A canonical page with an existing materialized Vanity URL is unpublished
     * ExpectedResult: The vanity clone and its persisted mapping should be removed.
     */
    @Test
    public void unpublishAliasesShouldDeleteCloneAndMappingWhenCanonicalContentIsRemoved()
            throws Exception {
        final PageFixture source = createLivePage("source-unpublish", "source to unpublish");
        final Contentlet vanity = createLiveVanity(source, "/canonical-removed-alias", source.path);
        service.publishAliasForVanityUrl(context(source), vanity);

        service.unpublishAliases(aliasContext(source, temporaryStaticFile("unused")));

        verify(endpointPublisher).deleteFilesFromEndpoint(eq(BUCKET_NAME), eq(BUCKET_PREFIX),
                eq("/canonical-removed-alias"));
        assertTrue(deletedPaths.contains("/canonical-removed-alias"));
        assertTrue(repository.findByLookup(lookup(source)).isEmpty());
    }

    private void recordEndpointCalls() throws Exception {
        doAnswer(invocation -> {
            final String path = invocation.getArgument(3);
            final File file = invocation.getArgument(4);
            pushedPaths.add(path);
            pushedContentByPath.put(path, Files.readString(file.toPath(), StandardCharsets.UTF_8));
            return null;
        }).when(endpointPublisher).pushFileToEndpoint(anyString(), anyString(), any(), anyString(),
                any(File.class));

        doAnswer(invocation -> {
            deletedPaths.add(invocation.getArgument(2));
            return null;
        }).when(endpointPublisher).deleteFilesFromEndpoint(anyString(), any(), anyString());
    }

    private PageFixture createLivePage(final String path, final String content) throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        return createLivePage(host, language, path, content);
    }

    private PageFixture createLivePage(final Host host, final Language language, final String path,
                                       final String content) throws Exception {
        final Field textField = new FieldDataGen().type(TextField.class).next();
        final ContentType contentType = new ContentTypeDataGen().field(textField).nextPersisted();
        final Container container = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "$!{" + textField.variable() + "}")
                .nextPersisted();
        final Template template = new TemplateDataGen()
                .site(host)
                .withContainer(container.getIdentifier())
                .nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(host)
                .languageId(language.getId())
                .setProperty(textField.variable(), content)
                .nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template)
                .host(host)
                .languageId(language.getId())
                .pageURL(path)
                .title(path)
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(page)
                .setContentlet(contentlet)
                .nextPersisted();

        ContentletDataGen.publish(contentlet);
        ContainerDataGen.publish(container);
        TemplateDataGen.publish(template);
        HTMLPageDataGen.publish(page);

        return new PageFixture(host, language, page, "/" + path);
    }

    private Contentlet createLiveVanity(final PageFixture source, final String uri,
                                        final String forwardTo) {
        final Contentlet vanity = new VanityUrlDataGen()
                .title("Vanity " + System.currentTimeMillis())
                .uri(uri)
                .forwardTo(forwardTo)
                .action(200)
                .order(0)
                .languageId(source.language.getId())
                .host(source.host)
                .nextPersisted();
        return ContentletDataGen.publish(vanity);
    }

    private Contentlet updateVanityUri(final Contentlet vanity, final String uri) {
        final Contentlet checkedOut = ContentletDataGen.checkout(vanity);
        checkedOut.setProperty(URI_FIELD_VAR, uri);
        return ContentletDataGen.publish(ContentletDataGen.checkin(checkedOut));
    }

    private S3VanityAliasPublishContext context(final PageFixture source) {
        return new S3VanityAliasPublishContext(endpointId, BUCKET_NAME, BUCKET_REGION,
                BUCKET_PREFIX, source.host, source.language, endpointPublisher);
    }

    private S3VanityAliasCleanupContext cleanupContext() {
        return new S3VanityAliasCleanupContext(endpointId, endpointPublisher);
    }

    private S3VanityAliasContext aliasContext(final PageFixture source, final File file) {
        return new S3VanityAliasContext(lookup(source), BUCKET_NAME, BUCKET_REGION,
                BUCKET_PREFIX, source.host, source.language, file, endpointPublisher);
    }

    private S3VanityAliasLookup lookup(final PageFixture source) {
        return new S3VanityAliasLookup(endpointId, source.host.getIdentifier(),
                source.language.getId(), source.path);
    }

    private File temporaryStaticFile(final String content) throws Exception {
        final File file = File.createTempFile("s3-vanity-it-", ".html");
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private void assertAlias(final S3VanityAlias alias, final PageFixture source,
                             final String canonicalPath, final String vanityPath,
                             final String vanityUrlId) {
        assertEquals(endpointId, alias.endpointId);
        assertEquals(source.host.getIdentifier(), alias.hostId);
        assertEquals(source.language.getId(), alias.languageId);
        assertEquals(canonicalPath, alias.canonicalPath);
        assertEquals(vanityPath, alias.vanityPath);
        assertEquals(vanityUrlId, alias.vanityUrlId);
        assertEquals(BUCKET_NAME, alias.bucketName);
        assertEquals(BUCKET_REGION, alias.bucketRegion);
        assertEquals(BUCKET_PREFIX, alias.bucketPrefix);
    }

    private static class PageFixture {

        private final Host host;
        private final Language language;
        private final HTMLPageAsset page;
        private final String path;

        private PageFixture(final Host host, final Language language,
                            final HTMLPageAsset page, final String path) {
            this.host = host;
            this.language = language;
            this.page = page;
            this.path = path;
        }
    }
}
