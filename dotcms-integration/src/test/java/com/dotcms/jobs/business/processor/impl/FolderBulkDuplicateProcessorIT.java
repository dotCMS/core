package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dotcms.Junit5WeldBaseTest;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import java.util.ArrayList;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.beans.Permission;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.datagen.UserDataGen;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
        return jobFor(paths, admin);
    }

    private Job jobFor(final List<String> paths, final User submitter) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", submitter.getUserId());
        parameters.put("assetPaths", paths);

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName(FolderBulkDuplicateHelper.QUEUE_NAME)
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(new DefaultProgressTracker())
                .build();
    }

    /** Runs the processor over the given paths as the given user and answers its metadata. */
    private Map<String, Object> duplicatePaths(final List<String> paths, final User submitter) {
        final Job job = jobFor(paths, submitter);
        final FolderBulkDuplicateProcessor processor = new FolderBulkDuplicateProcessor();
        processor.process(job);
        return processor.getResultMetadata(job);
    }

    /** The one outcome record for a submitted path. */
    private BatchItemResult resultFor(final Map<String, Object> metadata, final String key) {
        return resultsOf(metadata).stream().filter(result -> result.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no result recorded for " + key));
    }

    /** Grants a role a permission bitmask on a permissionable, as an admin. */
    private void grant(final Permissionable permissionable, final String roleId,
            final int permissions) throws Exception {
        APILocator.getPermissionAPI().save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                permissionable.getPermissionId(), roleId, permissions, true), permissionable, admin,
                false);
    }

    /** A new user with no rights of their own, and their personal role's id. */
    private User limitedUser() {
        return new UserDataGen().nextPersisted();
    }

    private String roleOf(final User user) throws Exception {
        return APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId();
    }

    /**
     * A parent folder the user may read and add to, and a child in it the user may read and
     * duplicate: the folder that succeeds alongside each refusal below.
     * <p>
     * The child is granted READ itself: an individual permission on the parent does not reach the
     * folders under it, so without it the child would be unreadable.
     */
    private Folder duplicableBy(final User user) throws Exception {
        final Folder parent = folder();
        grant(parent, roleOf(user), PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN);
        return readableChildOf(parent, user);
    }

    /** A new folder under the parent, which the user is granted READ on directly. */
    private Folder readableChildOf(final Folder parent, final User user) throws Exception {
        final Folder child = new FolderDataGen().parent(parent).nextPersisted();
        grant(child, roleOf(user), PermissionAPI.PERMISSION_READ);
        return child;
    }

    private void assertFailedWith(final Map<String, Object> metadata, final String path,
            final BatchFailureReason reason) {
        final BatchItemResult result = resultFor(metadata, path);
        assertEquals(BatchItemStatus.FAILED, result.status(), path);
        assertEquals(reason, result.reason().orElseThrow(), path);
    }

    private void assertSucceeded(final Map<String, Object> metadata, final Folder folder)
            throws Exception {
        assertEquals(BatchItemStatus.SUCCESS, resultFor(metadata, pathOf(folder)).status());
        duplicateOf(folder);
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

    /**
     * Whether an asset's name is derived from its id rather than chosen: generic content is named
     * {@code content.<uuid>} and a link by its identifier, so a copy always gets a new one.
     */
    private static boolean isIdNamed(final String assetName) {
        return assetName.startsWith("content.") || assetName.matches("[0-9a-f-]{32,36}");
    }

    /** The assets directly under a folder whose names are chosen, and so carried to a copy. */
    private Map<String, String> namedAssets(final Folder folder) throws Exception {
        return assetsUnder(folder).entrySet().stream()
                .filter(entry -> !isIdNamed(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** How many assets of each type sit directly under a folder, id-named ones included. */
    private Map<String, Long> typeCounts(final Folder folder) throws Exception {
        return assetsUnder(folder).values().stream()
                .collect(Collectors.groupingBy(type -> type, Collectors.counting()));
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
     * Given Scenario: The same folder duplicated three times, in three runs
     * ExpectedResult: Three distinct duplicates carrying one, two and three _copy suffixes, each
     * holding the source's file; the source and each earlier duplicate keep their name and content,
     * so nothing was renamed or overwritten (US2)
     */
    @Test
    public void test_process_repeatedDuplication_neverCollides() throws Exception {
        final Folder source = folder();
        new FileAssetDataGen(source, "a file").nextPersisted();
        final Map<String, String> sourceAssets = assetsUnder(source);

        duplicate(source);
        duplicate(source);
        duplicate(source);

        final String parentPath = source.getPath()
                .substring(0, source.getPath().length() - source.getName().length() - 1);
        for (final String suffix : List.of("_copy", "_copy_copy", "_copy_copy_copy")) {
            final Folder copy = folderAPI.findFolderByPath(
                    parentPath + source.getName() + suffix + "/", site, admin, false);
            assertNotNull(copy.getInode(), "missing " + source.getName() + suffix);
            assertEquals(sourceAssets, assetsUnder(copy), "wrong content in " + copy.getName());
        }
        assertEquals(sourceAssets, assetsUnder(source));
        assertEquals(source.getName(),
                folderAPI.find(source.getInode(), admin, false).getName());
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
        assertEquals(namedAssets(source), namedAssets(copy));
        assertEquals(typeCounts(source), typeCounts(copy));
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
     * ExpectedResult: Pins the shipped copy semantics (research R-12): the many-to-many copy always
     * points at the ORIGINAL child. When the child is copied in the same run it may also point at
     * the child's copy, since copying the child links its copy to every parent the child already
     * has; that is what the shipped copy does and is accepted. The one-to-many relationship is not
     * carried.
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

        assertTrue(APILocator.getContentletAPI().getRelatedContent(manyParentCopy, manyToMany,
                        true, admin, false).stream().map(Contentlet::getIdentifier).toList()
                .contains(child.getIdentifier()));
        assertTrue(APILocator.getContentletAPI().getRelatedContent(oneParentCopy, oneToMany, true,
                admin, false).isEmpty());
    }


    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A folder the submitter cannot read, beside one they can duplicate
     * ExpectedResult: The unreadable one fails with PERMISSION_DENIED and is not duplicated; the
     * other is still duplicated (US3)
     */
    @Test
    public void test_process_unreadableFolder_permissionDenied_restStillRuns() throws Exception {
        final User user = limitedUser();
        final Folder duplicable = duplicableBy(user);
        final Folder unreadable = folder();

        final Map<String, Object> metadata =
                duplicatePaths(List.of(pathOf(unreadable), pathOf(duplicable)), user);

        assertFailedWith(metadata, pathOf(unreadable), BatchFailureReason.PERMISSION_DENIED);
        assertFalse(assetsUnderParentOf(unreadable).containsKey(unreadable.getName() + "_copy"));
        assertSucceeded(metadata, duplicable);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A folder the submitter can read, whose parent they cannot add to, beside one
     * they can duplicate
     * ExpectedResult: PARENT_PERMISSION_DENIED, told apart from no rights on the folder itself,
     * and the other folder is still duplicated (US3)
     */
    @Test
    public void test_process_parentNotWritable_parentPermissionDenied_restStillRuns()
            throws Exception {
        final User user = limitedUser();
        final Folder duplicable = duplicableBy(user);
        final Folder readOnlyParent = folder();
        grant(readOnlyParent, roleOf(user), PermissionAPI.PERMISSION_READ);
        final Folder readable = readableChildOf(readOnlyParent, user);

        final Map<String, Object> metadata =
                duplicatePaths(List.of(pathOf(readable), pathOf(duplicable)), user);

        assertFailedWith(metadata, pathOf(readable), BatchFailureReason.PARENT_PERMISSION_DENIED);
        assertSucceeded(metadata, duplicable);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A path that resolves to nothing, beside a folder that can be duplicated
     * ExpectedResult: PATH_NOT_FOUND, and the other folder is still duplicated (US3)
     */
    @Test
    public void test_process_unresolvablePath_pathNotFound_restStillRuns() throws Exception {
        final Folder duplicable = folder();
        final String gone = "//" + site.getHostname() + "/does-not-exist-" + UUID.randomUUID()
                + "/";

        final Map<String, Object> metadata =
                duplicatePaths(List.of(gone, pathOf(duplicable)), admin);

        assertFailedWith(metadata, gone, BatchFailureReason.PATH_NOT_FOUND);
        assertSucceeded(metadata, duplicable);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A path the server cannot even parse (a space breaks the URI), beside a folder
     * that can be duplicated
     * ExpectedResult: PATH_NOT_FOUND, as the contract says for a gone, file or malformed path, not
     * UNCLASSIFIED; and the other folder is still duplicated (FR-013)
     */
    @Test
    public void test_process_malformedPath_pathNotFound_restStillRuns() throws Exception {
        final Folder duplicable = folder();
        final String malformed = "//" + site.getHostname() + "/has a space/";

        final Map<String, Object> metadata =
                duplicatePaths(List.of(malformed, pathOf(duplicable)), admin);

        assertFailedWith(metadata, malformed, BatchFailureReason.PATH_NOT_FOUND);
        assertSucceeded(metadata, duplicable);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: Paths with no site in front of them, a bare word and a folder path, beside a
     * folder that can be duplicated
     * ExpectedResult: PATH_NOT_FOUND for both, not PROTECTED_FOLDER: having one segment does not
     * make a path a site root; and the other folder is still duplicated (FR-013)
     */
    @Test
    public void test_process_pathWithoutSite_pathNotFound_restStillRuns() throws Exception {
        final Folder duplicable = folder();
        final String bareWord = "not a path/";
        final String withoutSite = "/" + duplicable.getName() + "/";

        final Map<String, Object> metadata =
                duplicatePaths(List.of(bareWord, withoutSite, pathOf(duplicable)), admin);

        assertFailedWith(metadata, bareWord, BatchFailureReason.PATH_NOT_FOUND);
        assertFailedWith(metadata, withoutSite, BatchFailureReason.PATH_NOT_FOUND);
        assertSucceeded(metadata, duplicable);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: The path of a file rather than a folder, spelled with and without the
     * trailing slash the submission adds, beside a folder that can be duplicated
     * ExpectedResult: PATH_NOT_FOUND for both, and the file's folder is not duplicated in its
     * place; the other folder is still duplicated (FR-013)
     */
    @Test
    public void test_process_pathNamesAFile_pathNotFound_restStillRuns() throws Exception {
        final Folder duplicable = folder();
        final Folder holder = folder();
        final Contentlet file = new FileAssetDataGen(holder, "a file").nextPersisted();
        final String filePath = pathOf(holder)
                + APILocator.getIdentifierAPI().find(file.getIdentifier()).getAssetName();
        final String filePathWithSlash = filePath + "/";

        final Map<String, Object> metadata = duplicatePaths(
                List.of(filePath, filePathWithSlash, pathOf(duplicable)), admin);

        assertFailedWith(metadata, filePath, BatchFailureReason.PATH_NOT_FOUND);
        assertFailedWith(metadata, filePathWithSlash, BatchFailureReason.PATH_NOT_FOUND);
        assertFalse(assetsUnderParentOf(holder).containsKey(holder.getName() + "_copy"));
        assertSucceeded(metadata, duplicable);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: The site root, which is the system folder in disguise, beside a folder that
     * can be duplicated
     * ExpectedResult: PROTECTED_FOLDER, never attempted, and the other folder is still duplicated
     * (US3)
     */
    @Test
    public void test_process_siteRoot_protectedFolder_restStillRuns() throws Exception {
        final Folder duplicable = folder();
        final String siteRoot = "//" + site.getHostname() + "/";

        final Map<String, Object> metadata =
                duplicatePaths(List.of(siteRoot, pathOf(duplicable)), admin);

        assertFailedWith(metadata, siteRoot, BatchFailureReason.PROTECTED_FOLDER);
        assertSucceeded(metadata, duplicable);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A selection where every folder is refused
     * ExpectedResult: The run still finishes normally, with successCount 0 and every folder named
     * with its own reason, rather than failing the job (US3)
     */
    @Test
    public void test_process_everyFolderRefused_runFinishesNamingEachOne() throws Exception {
        final String gone = "//" + site.getHostname() + "/does-not-exist-" + UUID.randomUUID()
                + "/";
        final String siteRoot = "//" + site.getHostname() + "/";

        final Map<String, Object> metadata = duplicatePaths(List.of(gone, siteRoot), admin);

        assertEquals(0, metadata.get("successCount"));
        assertEquals(2, metadata.get("failedCount"));
        assertFailedWith(metadata, gone, BatchFailureReason.PATH_NOT_FOUND);
        assertFailedWith(metadata, siteRoot, BatchFailureReason.PROTECTED_FOLDER);
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: A folder and its own child both selected, in both orders
     * ExpectedResult: Only the parent is duplicated; the child is SKIPPED with COVERED_BY_PARENT,
     * no duplicate of the child appears inside the original parent, and the parent's duplicate
     * carries the child. The same whichever was submitted first (US3)
     */
    @Test
    public void test_process_folderAndItsChild_childSkippedInEitherOrder() throws Exception {
        for (final boolean childFirst : List.of(true, false)) {
            final Folder parent = folder();
            final Folder child = new FolderDataGen().parent(parent).name("child").nextPersisted();
            final List<String> paths = childFirst
                    ? List.of(pathOf(child), pathOf(parent))
                    : List.of(pathOf(parent), pathOf(child));

            final Map<String, Object> metadata = duplicatePaths(paths, admin);

            final BatchItemResult childResult = resultFor(metadata, pathOf(child));
            assertEquals(BatchItemStatus.SKIPPED, childResult.status(), "childFirst=" + childFirst);
            assertEquals(BatchFailureReason.COVERED_BY_PARENT, childResult.reason().orElseThrow());
            assertEquals(BatchItemStatus.SUCCESS, resultFor(metadata, pathOf(parent)).status());
            assertFalse(assetsUnder(parent).containsKey("child_copy"), "childFirst=" + childFirst);
            assertEquals("folder", assetsUnder(duplicateOf(parent)).get("child"));
        }
    }

    /**
     * Method to test: {@link FolderDuplicator#duplicate(Folder, User)}
     * Given Scenario: A folder holding a file (which the shipped walk copies) and a custom-type
     * contentlet (which the second pass copies), with the second pass made to fail
     * ExpectedResult: No duplicate of the folder is left at all, not even the walk's part of it, so
     * the walk and the second pass share one transaction (US4, FR-026)
     */
    @Test
    public void test_duplicate_failureInTheSecondPass_leavesNoDuplicate() throws Exception {
        final Folder source = folder();
        new FileAssetDataGen(source, "a file").nextPersisted();
        final ContentType type = new ContentTypeDataGen().host(site).nextPersisted();
        new ContentletDataGen(type.id()).host(site).folder(source).nextPersisted();

        assertThrows(Exception.class,
                () -> new FolderDuplicator(failingCopy()).duplicate(source, admin));

        assertFalse(assetsUnderParentOf(source).containsKey(source.getName() + "_copy"),
                "the walk's part of the duplicate must roll back with the failed second pass");
    }

    /**
     * Method to test: {@link FolderDuplicator#duplicate(Folder, User)}
     * Given Scenario: The second pass fails on a contentlet in a child folder, deep in the tree,
     * after the top folder's own content was already copied
     * ExpectedResult: No duplicate of the top folder is left, so the whole subtree shares the one
     * transaction, not only the top folder (US4, FR-026)
     */
    @Test
    public void test_duplicate_failureInAChildFolder_leavesNoDuplicate() throws Exception {
        final Folder source = folder();
        new FileAssetDataGen(source, "a file").nextPersisted();
        final Folder child = new FolderDataGen().parent(source).name("child").nextPersisted();
        final ContentType type = new ContentTypeDataGen().host(site).nextPersisted();
        new ContentletDataGen(type.id()).host(site).folder(child).nextPersisted();

        assertThrows(Exception.class,
                () -> new FolderDuplicator(failingCopy()).duplicate(source, admin));

        assertFalse(assetsUnderParentOf(source).containsKey(source.getName() + "_copy"),
                "a failure deep in the tree must roll back the whole duplicate");
    }

    /** A contentlet API whose copy always fails, and which otherwise behaves as the real one. */
    private static ContentletAPI failingCopy() {
        final ContentletAPI real = APILocator.getContentletAPI();
        return (ContentletAPI) Proxy.newProxyInstance(ContentletAPI.class.getClassLoader(),
                new Class<?>[]{ContentletAPI.class}, (proxy, method, args) -> {
                    if (method.getName().equals("copyContentlet")) {
                        throw new DotDataException("injected failure in the second pass");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}
     * Given Scenario: The batch permission check itself fails, say on a database error, for a
     * selection the submitter could otherwise duplicate
     * ExpectedResult: Each folder is FAILED with UNCLASSIFIED, carrying the error as its
     * diagnostic, never PERMISSION_DENIED: a check that could not run is not a refusal, and
     * reporting it as one would hide the real failure (PR review finding)
     */
    @Test
    public void test_process_permissionCheckFails_unclassifiedNotPermissionDenied()
            throws Exception {
        final Folder first = folder();
        final Folder second = folder();
        final PermissionAPI real = APILocator.getPermissionAPI();
        final PermissionAPI failingCheck = (PermissionAPI) Proxy.newProxyInstance(
                PermissionAPI.class.getClassLoader(), new Class<?>[]{PermissionAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("filterCollection")) {
                        throw new DotDataException("injected failure in the permission check");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        final Job job = jobFor(List.of(pathOf(first), pathOf(second)));
        final FolderBulkDuplicateProcessor processor =
                new FolderBulkDuplicateProcessor(failingCheck);

        processor.process(job);

        final Map<String, Object> metadata = processor.getResultMetadata(job);
        assertFailedWith(metadata, pathOf(first), BatchFailureReason.UNCLASSIFIED);
        assertFailedWith(metadata, pathOf(second), BatchFailureReason.UNCLASSIFIED);
        assertTrue(resultFor(metadata, pathOf(first)).message().orElseThrow()
                .contains("injected failure"));
    }

    /** Everything directly under a folder's parent, the place its duplicate would land. */
    private Map<String, String> assetsUnderParentOf(final Folder folder) throws Exception {
        final String parentPath = folder.getPath()
                .substring(0, folder.getPath().length() - folder.getName().length() - 1);
        return new DotConnect()
                .setSQL("select asset_name, asset_type from identifier "
                        + "where host_inode = ? and parent_path = ?")
                .addParam(site.getIdentifier())
                .addParam(parentPath)
                .loadObjectResults().stream().collect(Collectors.toMap(
                        row -> String.valueOf(row.get("asset_name")),
                        row -> String.valueOf(row.get("asset_type"))));
    }
}
