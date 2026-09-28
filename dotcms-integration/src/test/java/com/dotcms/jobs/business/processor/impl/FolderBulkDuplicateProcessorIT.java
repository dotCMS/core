package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.rest.api.v1.asset.bulkduplicate.FolderBulkDuplicateHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link FolderBulkDuplicateProcessor}: the happy path and what a duplicate
 * holds (#37062, spec US1).
 * <p>
 * Drives the processor directly with a hand-built {@link Job}, the pattern
 * {@code FolderBulkDeleteProcessorIT} uses. What a folder holds is read from the {@code identifier}
 * table rather than the search index, so an item is counted once however many versions or
 * languages it has, and archived items are counted too.
 */
@EnableWeld
public class FolderBulkDuplicateProcessorIT extends Junit5WeldBaseTest {

    private static User admin;
    private static Host site;
    private static FolderAPI folderAPI;
    private static Template template;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        folderAPI = APILocator.getFolderAPI();
        template = new TemplateDataGen().site(site).nextPersisted();
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return "//" + site.getHostname() + folder.getPath();
    }

    private Job jobFor(final List<String> paths) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", admin.getUserId());
        parameters.put("assetPaths", paths);

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName(FolderBulkDuplicateHelper.QUEUE_NAME)
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(new DefaultProgressTracker())
                .build();
    }

    /** Runs the processor over the given folders and answers its result metadata. */
    private Map<String, Object> duplicate(final Folder... folders) {
        final Job job = jobFor(List.of(folders).stream().map(this::pathOf).toList());
        final FolderBulkDuplicateProcessor processor = new FolderBulkDuplicateProcessor();
        processor.process(job);
        return processor.getResultMetadata(job);
    }

    /** The duplicate of a folder: its name plus {@code _copy}, in the same parent. */
    private Folder duplicateOf(final Folder source) throws Exception {
        final String parentPath = source.getPath()
                .substring(0, source.getPath().length() - source.getName().length() - 1);
        final Folder duplicate = folderAPI.findFolderByPath(
                parentPath + source.getName() + "_copy/", site, admin, false);
        assertNotNull(duplicate, "no duplicate found beside " + source.getPath());
        assertNotNull(duplicate.getInode(), "no duplicate found beside " + source.getPath());
        return duplicate;
    }

    /**
     * Every asset directly under a folder, as {@code asset_name → asset_type}, read from the
     * identifier table: one row per item, whatever its versions, languages or archived state.
     */
    private Map<String, String> assetsUnder(final Folder folder) throws Exception {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select asset_name, asset_type from identifier "
                        + "where host_inode = ? and parent_path = ?")
                .addParam(site.getIdentifier())
                .addParam(folder.getPath())
                .loadObjectResults();
        return rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row.get("asset_name")),
                row -> String.valueOf(row.get("asset_type"))));
    }

    /** Identifiers of the contentlets of one type directly under a folder. */
    private List<String> contentletsOfType(final Folder folder, final ContentType type)
            throws Exception {
        return new DotConnect()
                .setSQL("select id from identifier where host_inode = ? and parent_path = ? "
                        + "and asset_subtype = ?")
                .addParam(site.getIdentifier())
                .addParam(folder.getPath())
                .addParam(type.variable())
                .loadObjectResults().stream()
                .map(row -> String.valueOf(row.get("id")))
                .toList();
    }

    /** The identifier of the asset with this name directly under a folder. */
    private String identifierOf(final Folder folder, final String assetName) throws Exception {
        return String.valueOf(new DotConnect()
                .setSQL("select id from identifier where host_inode = ? and parent_path = ? "
                        + "and asset_name = ?")
                .addParam(site.getIdentifier())
                .addParam(folder.getPath())
                .addParam(assetName)
                .loadObjectResults().getFirst().get("id"));
    }

    private boolean isLive(final String identifier) throws Exception {
        return APILocator.getVersionableAPI()
                .getContentletVersionInfo(identifier, APILocator.getLanguageAPI()
                        .getDefaultLanguage().getId())
                .map(info -> info.getLiveInode() != null)
                .orElse(false);
    }

    private boolean isArchived(final String identifier) throws Exception {
        return APILocator.getVersionableAPI()
                .getContentletVersionInfo(identifier, APILocator.getLanguageAPI()
                        .getDefaultLanguage().getId())
                .map(info -> info.isDeleted())
                .orElse(false);
    }

    @SuppressWarnings("unchecked")
    private List<BatchItemResult> resultsOf(final Map<String, Object> metadata) {
        return (List<BatchItemResult>) metadata.get("results");
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: Two folders, one at the site root and one nested, each with a child folder
     * ExpectedResult: Each source is untouched, each duplicate sits in its source's own parent
     * under the source name plus _copy, child folders keep their names, and the run reports one
     * SUCCESS per folder keyed by the submitted path
     */
    @Test
    public void test_process_rootAndNestedFolders_duplicatedBesideTheirSources()
            throws Exception {
        final Folder atRoot = folder();
        new FolderDataGen().parent(atRoot).name("child").nextPersisted();
        final Folder parent = folder();
        final Folder nested = new FolderDataGen().parent(parent).nextPersisted();
        new FolderDataGen().parent(nested).name("inner").nextPersisted();

        final Map<String, Object> metadata = duplicate(atRoot, nested);

        assertNotNull(folderAPI.find(atRoot.getInode(), admin, false).getInode());
        assertNotNull(folderAPI.find(nested.getInode(), admin, false).getInode());
        assertEquals("folder", assetsUnder(duplicateOf(atRoot)).get("child"));
        assertEquals("folder", assetsUnder(duplicateOf(nested)).get("inner"));
        assertTrue(duplicateOf(nested).getPath().startsWith(parent.getPath()));

        assertEquals(2, metadata.get("total"));
        assertEquals(2, metadata.get("successCount"));
        assertEquals(List.of(pathOf(atRoot), pathOf(nested)),
                resultsOf(metadata).stream().map(BatchItemResult::key).toList());
        assertTrue(resultsOf(metadata).stream()
                .allMatch(result -> result.status() == BatchItemStatus.SUCCESS));
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A folder holding a file asset, a published page, an unpublished page, a
     * link, a child folder with its own file, a contentlet of a custom type, and an archived file
     * asset
     * ExpectedResult: The duplicate holds every one of them exactly once, and its child folder
     * holds the child's file. The shipped folder copy leaves generic contentlets and archived items
     * behind; this run closes that gap.
     */
    @Test
    public void test_process_duplicateHoldsEverything() throws Exception {
        final Folder source = folder();
        new FileAssetDataGen(source, "a file").nextPersisted();
        ContentletDataGen.publish(new HTMLPageDataGen(source, template).nextPersisted());
        new HTMLPageDataGen(source, template).nextPersisted();
        new LinkDataGen(source).hostId(site.getIdentifier()).nextPersisted();
        final Folder child = new FolderDataGen().parent(source).name("child").nextPersisted();
        new FileAssetDataGen(child, "a child file").nextPersisted();
        final ContentType blog = new ContentTypeDataGen().host(site).nextPersisted();
        new ContentletDataGen(blog.id()).host(site).folder(source).nextPersisted();
        ContentletDataGen.archive(new FileAssetDataGen(source, "archived").nextPersisted());

        duplicate(source);

        final Folder copy = duplicateOf(source);
        assertEquals(assetsUnder(source), assetsUnder(copy));
        assertEquals(1, contentletsOfType(copy, blog).size());
        assertEquals(assetsUnder(child).keySet(),
                assetsUnder(folderAPI.findFolderByPath(copy.getPath() + "child/", site, admin,
                        false)).keySet());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A folder with a published page, an unpublished page and an archived file
     * ExpectedResult: Each copy keeps its source's state: the published page's copy is live, the
     * unpublished page's copy is not, and the archived file's copy is archived
     */
    @Test
    public void test_process_duplicatePreservesState() throws Exception {
        final Folder source = folder();
        final Contentlet published = new HTMLPageDataGen(source, template).nextPersisted();
        ContentletDataGen.publish(published);
        final Contentlet unpublished = new HTMLPageDataGen(source, template).nextPersisted();
        final Contentlet archived = new FileAssetDataGen(source, "archived").nextPersisted();
        ContentletDataGen.archive(archived);
        final Map<String, String> sourceAssets = assetsUnder(source);
        final String publishedName = nameOf(published.getIdentifier());
        final String unpublishedName = nameOf(unpublished.getIdentifier());
        final String archivedName = nameOf(archived.getIdentifier());
        assertEquals(3, sourceAssets.size());

        duplicate(source);

        final Folder copy = duplicateOf(source);
        assertTrue(isLive(identifierOf(copy, publishedName)));
        assertFalse(isLive(identifierOf(copy, unpublishedName)));
        assertTrue(isArchived(identifierOf(copy, archivedName)));
    }

    private String nameOf(final String identifier) throws Exception {
        return String.valueOf(new DotConnect()
                .setSQL("select asset_name from identifier where id = ?")
                .addParam(identifier)
                .loadObjectResults().getFirst().get("asset_name"));
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A folder holding two file assets and two pages, which the shipped folder
     * copy already carries
     * ExpectedResult: The duplicate holds four items, not eight: the complement pass leaves out
     * everything the walk carried
     */
    @Test
    public void test_process_nothingCopiedTwice() throws Exception {
        final Folder source = folder();
        new FileAssetDataGen(source, "one").nextPersisted();
        new FileAssetDataGen(source, "two").nextPersisted();
        new HTMLPageDataGen(source, template).nextPersisted();
        new HTMLPageDataGen(source, template).nextPersisted();

        duplicate(source);

        final long rows = new DotConnect()
                .setSQL("select count(*) as n from identifier where host_inode = ? "
                        + "and parent_path = ?")
                .addParam(site.getIdentifier())
                .addParam(duplicateOf(source).getPath())
                .getInt("n");
        assertEquals(4, rows);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: In one folder, a parent item related many-to-many to a child item, and
     * another parent item related one-to-many to another child
     * ExpectedResult: Pins the shipped copy semantics (research R-12): the many-to-many copy points
     * at the ORIGINAL child, not the child's own copy, and the one-to-many relationship is not
     * carried
     */
    @Test
    public void test_process_relationshipsPointAtOriginals() throws Exception {
        final Folder source = folder();
        final ContentType childType = new ContentTypeDataGen().host(site).nextPersisted();
        final ContentType manyType = new ContentTypeDataGen().host(site).nextPersisted();
        final Relationship manyToMany = new FieldRelationshipDataGen()
                .parent(manyType).child(childType)
                .cardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY)
                .nextPersisted();
        final ContentType oneType = new ContentTypeDataGen().host(site).nextPersisted();
        final Relationship oneToMany = new FieldRelationshipDataGen()
                .parent(oneType).child(childType)
                .cardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY)
                .nextPersisted();

        final Contentlet child = new ContentletDataGen(childType.id()).host(site).folder(source)
                .nextPersisted();
        final Contentlet other = new ContentletDataGen(childType.id()).host(site).folder(source)
                .nextPersisted();
        final Contentlet manyParent = new ContentletDataGen(manyType.id()).host(site)
                .folder(source).nextPersisted();
        final Contentlet oneParent = new ContentletDataGen(oneType.id()).host(site)
                .folder(source).nextPersisted();
        APILocator.getContentletAPI().relateContent(manyParent, manyToMany, List.of(child),
                admin, false);
        APILocator.getContentletAPI().relateContent(oneParent, oneToMany, List.of(other),
                admin, false);

        duplicate(source);

        final Folder copy = duplicateOf(source);
        final Contentlet manyParentCopy = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(
                contentletsOfType(copy, manyType).getFirst());
        final Contentlet oneParentCopy = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(
                contentletsOfType(copy, oneType).getFirst());

        assertEquals(List.of(child.getIdentifier()),
                APILocator.getContentletAPI().getRelatedContent(manyParentCopy, manyToMany, true,
                        admin, false).stream().map(Contentlet::getIdentifier).toList());
        assertTrue(APILocator.getContentletAPI().getRelatedContent(oneParentCopy, oneToMany, true,
                admin, false).isEmpty());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: Two folders duplicated in one run
     * ExpectedResult: COPY_FOLDER is announced once per duplicate, naming the duplicate as its
     * target. It is pushed on commit, so a listener that reloads on it finds the content in place.
     */
    @Test
    public void test_process_announcesCopyFolderOncePerDuplicate() throws Exception {
        final Folder first = folder();
        final Folder second = folder();
        final long before = System.currentTimeMillis();

        duplicate(first, second);

        final String firstCopy = duplicateOf(first).getIdentifier();
        final String secondCopy = duplicateOf(second).getIdentifier();
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            final List<SystemEvent> copies = APILocator.getSystemEventsAPI()
                    .getEventsSince(before).stream()
                    .filter(event -> event.getEventType() == SystemEventType.COPY_FOLDER)
                    .toList();
            assertEquals(1, copies.stream()
                    .filter(event -> String.valueOf(event.getPayload().getData())
                            .contains(firstCopy)).count());
            assertEquals(1, copies.stream()
                    .filter(event -> String.valueOf(event.getPayload().getData())
                            .contains(secondCopy)).count());
        });
    }
}
