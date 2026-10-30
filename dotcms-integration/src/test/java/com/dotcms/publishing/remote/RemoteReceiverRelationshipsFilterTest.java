package com.dotcms.publishing.remote;

import static com.dotcms.publisher.business.PublisherTestUtil.createEndpoint;
import static com.dotcms.publisher.business.PublisherTestUtil.createEnvironment;
import static com.dotcms.publisher.business.PublisherTestUtil.generateBundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.FilterDescriptorDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.business.PublisherTestUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.receiver.BundlePublisher;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies how the receiver handles the relationships (the {@code tree} table) of pushed content
 * when the bundle was generated with a Push Publishing Filter that sets {@code relationships:
 * false}, such as "Only Selected Items". Sender and receiver are simulated in the same instance:
 * the bundle is generated, the receiver-side state is prepared, and the bundle is processed.
 */
public class RemoteReceiverRelationshipsFilterTest extends IntegrationTestBase {

    private static Host host;
    private static User adminUser;

    // Test data to remove once all tests ran. Content Types are removed with their contentlets
    private static final List<ContentType> contentTypes = new ArrayList<>();
    private static final List<String> filterKeys = new ArrayList<>();
    private static final List<PreparedBundle> bundles = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        //If I don't set this we'll end-up getting a ClassCasException at LanguageVariablesHandler.java:75
        when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                .thenReturn(new MultiMessageResources(MultiMessageResourcesFactory.createFactory(), ""));

        LicenseTestUtil.getLicense();

        host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        adminUser = TestUserUtils.getAdminUser();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        for (final PreparedBundle bundle : bundles) {
            PublisherTestUtil.cleanBundleEndpointEnv(bundle.bundle, bundle.endpoint, bundle.environment);
            Files.deleteIfExists(bundle.file.toPath());
        }

        // The test filters only live in memory, so drop them from the list without reloading it from
        // disk, which would also drop the in-memory filters added by other tests in the suite. Only
        // an admin gets the complete list back, which is required to rebuild it
        if (adminUser.isAdmin()) {
            final List<FilterDescriptor> otherFilters = new ArrayList<>(
                    APILocator.getPublisherAPI().getFiltersDescriptorsByRole(adminUser));
            otherFilters.removeIf(filter -> filterKeys.contains(filter.getKey()));
            APILocator.getPublisherAPI().clearFilterDescriptorList();
            otherFilters.forEach(APILocator.getPublisherAPI()::addFilterDescriptor);
        }

        // The Article types hold the Relationships Fields, so they go first
        for (int i = contentTypes.size() - 1; i >= 0; i--) {
            ContentTypeDataGen.remove(contentTypes.get(i));
        }
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: An Article related to an Author is pushed with a filter that sets {@code
     * relationships: false}. The Author already exists in the receiver, but the Article is
     * brand-new there.
     * Expected result: The new Article is created in the receiver related to the existing Author,
     * matched by Identifier.
     */
    @Test
    public void test_newParent_pushedWithoutRelationshipsFilter_isRelatedToExistingChild() throws Exception {
        final TestData data = createRelatedContent();
        final FilterDescriptor filter = createFilterWithoutRelationships();
        final PreparedBundle bundle = generateBundleFor(data.article, filter);

        // The Article doesn't exist in the receiver yet, the Author does
        LocalTransaction.wrap(() -> {
            data.article.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(data.article, adminUser, false);
        });
        assertTrue("The Article must not have relationships before the push, otherwise this test "
                + "would pass for the wrong reason", findRelatedIds(data.article, data.relationship).isEmpty());

        processBundle(bundle);

        assertNotNull("The Article must have been created from the bundle", APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(data.article.getIdentifier()));
        assertEquals("The new Article must be related to the Author that already exists in the receiver",
                List.of(data.author.getIdentifier()), findRelatedIds(data.article, data.relationship));
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: An Article related to an Author is pushed with a filter that sets {@code
     * relationships: false}. Neither the Article nor the Author exist in the receiver.
     * Expected result: The new Article is created in the receiver with a blank relationship, since
     * the related Author can't be found there by Identifier.
     */
    @Test
    public void test_newParent_pushedWithoutRelationshipsFilter_skipsMissingChild() throws Exception {
        final TestData data = createRelatedContent();
        final FilterDescriptor filter = createFilterWithoutRelationships();
        final PreparedBundle bundle = generateBundleFor(data.article, filter);

        // Neither the Article nor the Author exist in the receiver
        LocalTransaction.wrap(() -> {
            data.article.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(data.article, adminUser, false);
            data.author.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(data.author, adminUser, false);
        });

        processBundle(bundle);

        assertNotNull("The Article must have been created from the bundle", APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(data.article.getIdentifier()));
        assertTrue("The new Article must not be related to content that doesn't exist in the receiver",
                findRelatedIds(data.article, data.relationship).isEmpty());
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: A related Article and Author are both selected and pushed together with a
     * filter that sets {@code relationships: false}. Neither of them exist in the receiver.
     * Expected result: Both are created and related to each other, regardless of the order in
     * which the receiver processes them.
     */
    @Test
    public void test_newParentAndChild_pushedTogetherWithoutRelationshipsFilter_areRelated() throws Exception {
        final TestData data = createRelatedContent();
        final FilterDescriptor filter = createFilterWithoutRelationships();
        final PreparedBundle bundle = generateBundleFor(List.of(data.article, data.author), filter);

        LocalTransaction.wrap(() -> {
            data.article.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(data.article, adminUser, false);
            data.author.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(data.author, adminUser, false);
        });

        processBundle(bundle);

        assertEquals("The new Article must be related to the new Author pushed with it",
                List.of(data.author.getIdentifier()), findRelatedIds(data.article, data.relationship));
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: An Author related to an Article is pushed with a filter that sets {@code
     * relationships: false}. The Article already exists in the receiver, but the Author is
     * brand-new there.
     * Expected result: The new Author is created in the receiver and related to the existing
     * Article, matched by Identifier, the same as a new parent is related to existing children.
     */
    @Test
    public void test_newChild_pushedWithoutRelationshipsFilter_isRelatedToExistingParent()
            throws Exception {
        final TestData data = createRelatedContent();
        final FilterDescriptor filter = createFilterWithoutRelationships();
        final PreparedBundle bundle = generateBundleFor(data.author, filter);

        // The Author doesn't exist in the receiver yet, the Article does
        LocalTransaction.wrap(() -> {
            data.author.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(data.author, adminUser, false);
        });
        assertTrue("The Article must not have relationships before the push, otherwise this test "
                + "would pass for the wrong reason", findRelatedIds(data.article, data.relationship).isEmpty());

        processBundle(bundle);

        assertNotNull("The Author must have been created from the bundle", APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(data.author.getIdentifier()));
        assertEquals("The existing Article must be related to the new Author",
                List.of(data.author.getIdentifier()), findRelatedIds(data.article, data.relationship));
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: An Article that already exists in the receiver is pushed with a filter that
     * sets {@code relationships: false}, without {@code syncRelationships}. In the receiver, the
     * Article is related to a different Author than the one in the bundle.
     * Expected result: The receiver keeps its own relationships for the existing Article, as
     * required by <a href="https://github.com/dotCMS/core/issues/30998">#30998</a>.
     */
    @Test
    public void test_existingParent_pushedWithoutRelationshipsFilter_keepsReceiverRelationships()
            throws Exception {
        final TestData data = createRelatedContent();
        final Contentlet receiverOnlyAuthor = new ContentletDataGen(data.authorType).host(host)
                .setProperty("title", "receiver-only-" + UUIDGenerator.shorty()).nextPersisted();
        final PreparedBundle bundle = generateBundleFor(data.article, createFilterWithoutRelationships());
        moveRelationship("child", data.author, receiverOnlyAuthor, data.article, data.relationship);

        processBundle(bundle);

        assertEquals("The relationships of existing content in the receiver must not be modified",
                List.of(receiverOnlyAuthor.getIdentifier()), findRelatedIds(data.article, data.relationship));
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: An Author that already exists in the receiver is pushed with a filter that
     * sets {@code relationships: false}, without {@code syncRelationships}. In the receiver, a
     * second Article that only exists there is also related to the Author.
     * Expected result: The Author keeps both Articles, as required by
     * <a href="https://github.com/dotCMS/core/issues/30998">#30998</a>.
     */
    @Test
    public void test_existingChild_pushedWithoutRelationshipsFilter_keepsReceiverParents() throws Exception {
        final TestData data = createRelatedContent();
        final PreparedBundle bundle = generateBundleFor(data.author, createFilterWithoutRelationships());
        final Contentlet receiverOnlyArticle = new ContentletDataGen(data.article.getContentTypeId()).host(host)
                .setProperty("title", "receiver-only-" + UUIDGenerator.shorty()).nextPersisted();
        relate(receiverOnlyArticle, data.author, data.relationship);

        processBundle(bundle);

        assertEquals("The pushed Author must keep the Articles it has in the receiver",
                Set.of(data.article.getIdentifier(), receiverOnlyArticle.getIdentifier()),
                Set.copyOf(findRelatedIds(data.author, data.relationship)));
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: An Article that already exists in the receiver is pushed with a filter that
     * sets {@code relationships: false} and {@code syncRelationships: true}. In the receiver, the
     * Article is related to Author A, and
     * in the sender its Author was changed to Author B, which also exists in the receiver.
     * Expected result: The Article's relationship in the receiver is replaced with the one in the
     * sender, matched by Identifier, so it's related to Author B only.
     */
    @Test
    public void test_existingParent_pushedWithSyncRelationshipsFilter_matchesSenderRelationships()
            throws Exception {
        final TestData data = createRelatedContent();
        final Contentlet receiverOnlyAuthor = new ContentletDataGen(data.authorType).host(host)
                .setProperty("title", "receiver-only-" + UUIDGenerator.shorty()).nextPersisted();
        final FilterDescriptor filter = createFilterWithoutRelationships(true);
        final PreparedBundle bundle = generateBundleFor(data.article, filter);

        // In the receiver, the Article is related to a different Author. The relationship row can
        // have the Article on either side, depending on the cardinality
        for (final String column : List.of("parent", "child")) {
            final String otherColumn = "parent".equals(column) ? "child" : "parent";
            new DotConnect().setSQL(String.format("update tree set %s = ? where %s = ? and %s = ? and relation_type = ?",
                            column, column, otherColumn))
                    .addParam(receiverOnlyAuthor.getIdentifier())
                    .addParam(data.author.getIdentifier())
                    .addParam(data.article.getIdentifier())
                    .addParam(data.relationship.getRelationTypeValue())
                    .loadResult();
        }
        assertEquals("The receiver-only relationship was not set up; the scenario under test is not in place",
                List.of(receiverOnlyAuthor.getIdentifier()), findRelatedIds(data.article, data.relationship));

        processBundle(bundle);

        assertEquals("The pushed Article must be related to the Author it has in the sender",
                List.of(data.author.getIdentifier()), findRelatedIds(data.article, data.relationship));
        assertTrue("The Author the Article had in the receiver must no longer be related to it",
                findRelatedIds(receiverOnlyAuthor, data.relationship).isEmpty());
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: An Author that already exists in the receiver is pushed with a filter that
     * sets {@code relationships: false} and {@code syncRelationships: true}. In the receiver, a
     * second Article is also related to the Author, but it isn't in the sender.
     * Expected result: The Author's relationships in the receiver are replaced with the ones in the
     * sender, matched by Identifier, so it's related to the first Article only.
     */
    @Test
    public void test_existingChild_pushedWithSyncRelationshipsFilter_matchesSenderParents() throws Exception {
        final TestData data = createRelatedContent();
        final PreparedBundle bundle = generateBundleFor(data.author, createFilterWithoutRelationships(true));

        // In the receiver, a second Article is related to the Author
        final Contentlet receiverOnlyArticle = new ContentletDataGen(data.article.getContentTypeId()).host(host)
                .setProperty("title", "receiver-only-" + UUIDGenerator.shorty()).nextPersisted();
        relate(receiverOnlyArticle, data.author, data.relationship);

        processBundle(bundle);

        assertEquals("The pushed Author must be related to the Articles it has in the sender only",
                List.of(data.article.getIdentifier()), findRelatedIds(data.author, data.relationship));
        assertTrue("The Article that isn't related to the Author in the sender must not be related to it",
                findRelatedIds(receiverOnlyArticle, data.relationship).isEmpty());
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: With a many-to-one relationship (one Author per Article), the receiver has an
     * Article related to Author Jane. In the sender, the Article's Author is changed to a new
     * Author, Maria, and only Maria is pushed with a filter that sets {@code relationships: false}.
     * Expected result: Maria is created in the receiver, but isn't related to the Article, which
     * keeps Jane as its only Author.
     */
    @Test
    public void test_newChild_pushedWithoutRelationshipsFilter_isNotRelatedToParentWithItsOnlyChild()
            throws Exception {
        final TestData data = createRelatedContent();
        final Contentlet maria = new ContentletDataGen(data.authorType).host(host)
                .setProperty("title", "maria-" + UUIDGenerator.shorty()).nextPersisted();
        // In the sender, the Article's only Author is now Maria
        moveRelationship("child", data.author, maria, data.article, data.relationship);
        final PreparedBundle bundle = generateBundleFor(maria, createFilterWithoutRelationships());

        // In the receiver, Maria doesn't exist yet, and the Article's only Author is still Jane
        LocalTransaction.wrap(() -> {
            maria.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(maria, adminUser, false);
        });
        relate(data.article, data.author, data.relationship);

        processBundle(bundle);

        assertNotNull("Maria must have been created from the bundle", APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(maria.getIdentifier()));
        assertEquals("The Article allows one Author only, so it must keep the one it has in the receiver",
                List.of(data.author.getIdentifier()), findRelatedIds(data.article, data.relationship));
    }

    /**
     * Method to test: {@link com.dotcms.enterprise.publishing.remote.handler.ContentHandler#handle(File)}
     * Given scenario: With a one-to-many relationship (one Article per Author), the receiver has an
     * Article related to Author Jane. In the sender, Jane is related to a new Article instead, and
     * only the new Article is pushed with a filter that sets {@code relationships: false}.
     * Expected result: The new Article is created in the receiver without Jane, who stays related
     * to her only Article there.
     */
    @Test
    public void test_newParent_pushedWithoutRelationshipsFilter_isNotRelatedToChildWithItsOnlyParent()
            throws Exception {
        final TestData data = createRelatedContent(RELATIONSHIP_CARDINALITY.ONE_TO_MANY);
        final Contentlet newArticle = new ContentletDataGen(data.article.getContentTypeId()).host(host)
                .setProperty("title", "new-article-" + UUIDGenerator.shorty()).nextPersisted();
        // In the sender, Jane's only Article is now the new one
        moveRelationship("parent", data.article, newArticle, data.author, data.relationship);
        final PreparedBundle bundle = generateBundleFor(newArticle, createFilterWithoutRelationships());

        // In the receiver, the new Article doesn't exist yet, and Jane's only Article is the old one
        LocalTransaction.wrap(() -> {
            newArticle.setIndexPolicy(IndexPolicy.FORCE);
            APILocator.getContentletAPI().destroy(newArticle, adminUser, false);
        });
        relate(data.article, data.author, data.relationship);

        processBundle(bundle);

        assertNotNull("The new Article must have been created from the bundle", APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(newArticle.getIdentifier()));
        assertTrue("Jane allows one Article only, so the new Article must not be related to her",
                findRelatedIds(newArticle, data.relationship).isEmpty());
        assertEquals("Jane must keep the Article she has in the receiver",
                List.of(data.article.getIdentifier()), findRelatedIds(data.author, data.relationship));
    }

    /**
     * Moves the relationship between {@code related} and {@code from} to {@code to}, where {@code
     * column} is the {@code tree} column {@code from} is in.
     */
    private static void moveRelationship(final String column, final Contentlet from, final Contentlet to,
            final Contentlet related, final Relationship relationship) throws Exception {
        final String otherColumn = "parent".equals(column) ? "child" : "parent";
        new DotConnect().setSQL(String.format("update tree set %s = ? where %s = ? and %s = ? and relation_type = ?",
                        column, column, otherColumn))
                .addParam(to.getIdentifier())
                .addParam(from.getIdentifier())
                .addParam(related.getIdentifier())
                .addParam(relationship.getRelationTypeValue())
                .loadResult();
    }

    /**
     * Relates the specified parent and child through the given relationship.
     */
    private static void relate(final Contentlet parent, final Contentlet child, final Relationship relationship)
            throws Exception {
        new DotConnect().setSQL("insert into tree (child, parent, relation_type, tree_order) values (?, ?, ?, 0)")
                .addParam(child.getIdentifier())
                .addParam(parent.getIdentifier())
                .addParam(relationship.getRelationTypeValue())
                .loadResult();
    }

    /**
     * Creates an Article type with a many-to-one Relationships Field pointing to an Author type --
     * the setup of the reported use case -- and an Article related to an Author.
     */
    private TestData createRelatedContent() {
        return createRelatedContent(RELATIONSHIP_CARDINALITY.MANY_TO_ONE);
    }

    /**
     * Creates an Article type with a Relationships Field of the given cardinality pointing to an
     * Author type, and an Article related to an Author.
     */
    private TestData createRelatedContent(final RELATIONSHIP_CARDINALITY cardinality) {
        final ContentType authorType = new ContentTypeDataGen().host(host)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .nextPersisted();
        ContentType articleType = new ContentTypeDataGen().host(host)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .nextPersisted();
        final Relationship relationship = new FieldRelationshipDataGen()
                .parent(articleType)
                .child(authorType)
                .cardinality(cardinality)
                .nextPersisted();
        articleType = findContentType(articleType);
        contentTypes.add(authorType);
        contentTypes.add(articleType);
        final String relationshipFieldVar = articleType.fields(RelationshipField.class).get(0).variable();

        final Contentlet author = new ContentletDataGen(authorType).host(host)
                .setProperty("title", "author-" + UUIDGenerator.shorty()).nextPersisted();
        final Contentlet article = new ContentletDataGen(articleType).host(host)
                .setProperty("title", "article-" + UUIDGenerator.shorty())
                .setProperty(relationshipFieldVar, List.of(author))
                .nextPersisted();
        assertEquals("The Article was not related to the Author; the scenario under test is not in place",
                List.of(author.getIdentifier()), findRelatedIds(article, relationship));
        return new TestData(authorType, relationship, article, author);
    }

    /**
     * Reloads the specified Content Type so that it includes the Fields added after it was created.
     */
    private static ContentType findContentType(final ContentType contentType) {
        try {
            return APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentType.variable());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a filter like "Only Selected Items" that the test bundles reference by key. It must not
     * become the default filter or replace the existing ones, as other tests in the same suite
     * push with the default filter and expect dependencies to be included.
     */
    private FilterDescriptor createFilterWithoutRelationships() {
        return createFilterWithoutRelationships(false);
    }

    /**
     * Adds a filter like "Only Selected Items" that, if {@code syncRelationships} is {@code true},
     * also makes the receiver replace the relationships of the pushed content with the ones in the
     * sender.
     */
    private FilterDescriptor createFilterWithoutRelationships(final boolean syncRelationships) {
        final FilterDescriptor filter = new FilterDescriptorDataGen()
                .syncRelationships(syncRelationships)
                .key("onlySelectedItems_" + System.currentTimeMillis() + ".yml")
                .dependencies(false)
                .relationships(false)
                .defaultFilter(false)
                .clearFilterList(false)
                .nextPersisted();
        filterKeys.add(filter.getKey());
        return filter;
    }

    /**
     * Generates a bundle that pushes only the specified Contentlet with the given filter, the same
     * as selecting it in the UI and pushing it with that filter.
     */
    private PreparedBundle generateBundleFor(final Contentlet contentlet, final FilterDescriptor filter)
            throws Exception {
        return generateBundleFor(List.of(contentlet), filter);
    }

    /**
     * Generates a bundle that pushes only the specified Contentlets with the given filter.
     */
    private PreparedBundle generateBundleFor(final List<Contentlet> contentlets, final FilterDescriptor filter)
            throws Exception {
        final Environment environment = createEnvironment(adminUser);
        final PublishingEndPoint endpoint = createEndpoint(environment);

        final Bundle bundle = new Bundle("relationshipsFilterBundle", null, null, adminUser.getUserId(),
                false, filter.getKey());
        APILocator.getBundleAPI().saveBundle(bundle);
        PublisherAPI.getInstance().saveBundleAssets(contentlets.stream().map(Contentlet::getIdentifier)
                .collect(Collectors.toList()), bundle.getId(), adminUser);

        final Map<String, Object> bundleData = generateBundle(bundle.getId(), Operation.PUBLISH);
        final File file = File.class.cast(bundleData.get(PublisherTestUtil.FILE));
        assertNotNull(file);
        final PreparedBundle preparedBundle = new PreparedBundle(file, bundle, endpoint, environment);
        bundles.add(preparedBundle);
        return preparedBundle;
    }

    /**
     * Simulates the receiver: registers the bundle in the publish audit table, using the bundle file
     * name without its {@code .tar.gz} extension as the bundle folder, and runs
     * {@link BundlePublisher} over the generated file.
     */
    private void processBundle(final PreparedBundle bundle) throws Exception {
        final String fileName = bundle.file.getName();
        final String bundleFolder = fileName.substring(0, fileName.indexOf(".tar.gz"));
        final PublishAuditStatus status = PublishAuditAPI.getInstance().updateAuditTable(
                bundle.endpoint.getId(), bundle.endpoint.getGroupId(), bundleFolder, true);

        final PublisherConfig publisherConfig = new PublisherConfig();
        publisherConfig.setId(fileName);
        publisherConfig.setEndpoint(bundle.endpoint.getId());
        publisherConfig.setGroupId(bundle.endpoint.getGroupId());
        publisherConfig.setPublishAuditStatus(status);

        final BundlePublisher bundlePublisher = new BundlePublisher();
        bundlePublisher.init(publisherConfig);
        bundlePublisher.process(null);
    }

    /**
     * Returns the Identifiers of the contentlets related to the specified one through the given
     * relationship, regardless of the side of the {@code tree} row it's on.
     */
    private static List<String> findRelatedIds(final Contentlet contentlet, final Relationship relationship) {
        try {
            return new DotConnect().setSQL("select case when parent = ? then child else parent end as related "
                            + "from tree where (parent = ? or child = ?) and relation_type = ?")
                    .addParam(contentlet.getIdentifier())
                    .addParam(contentlet.getIdentifier())
                    .addParam(contentlet.getIdentifier())
                    .addParam(relationship.getRelationTypeValue())
                    .loadObjectResults().stream()
                    .map(row -> row.get("related").toString())
                    .collect(Collectors.toList());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The Author Type, the Relationship and the related Article and Author created for a test.
     */
    private static final class TestData {

        final ContentType authorType;
        final Relationship relationship;
        final Contentlet article;
        final Contentlet author;

        TestData(final ContentType authorType, final Relationship relationship, final Contentlet article,
                final Contentlet author) {
            this.authorType = authorType;
            this.relationship = relationship;
            this.article = article;
            this.author = author;
        }

    }

    /**
     * A generated bundle file with the Bundle, Endpoint and Environment it was generated for, which
     * are removed after the tests run.
     */
    private static final class PreparedBundle {

        final File file;
        final Bundle bundle;
        final PublishingEndPoint endpoint;
        final Environment environment;

        PreparedBundle(final File file, final Bundle bundle, final PublishingEndPoint endpoint,
                final Environment environment) {
            this.file = file;
            this.bundle = bundle;
            this.endpoint = endpoint;
            this.environment = environment;
        }

    }

}
