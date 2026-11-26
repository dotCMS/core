package com.dotcms.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;

import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.WorkflowDataGen;
import com.dotcms.rest.api.v1.drive.BrowseScope;
import com.dotcms.rest.api.v1.drive.ContentDriveHelper;
import com.dotcms.rest.api.v1.drive.DriveRequestForm;
import com.dotcms.rest.api.v1.drive.QueryFilters;
import com.dotcms.rest.api.v1.drive.WorkflowFilterForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link BrowserAPI#getContentForExport(BrowserQuery, int)}: the rows a
 * Content Drive export writes (research R1, FR-005, FR-011, FR-017, FR-017a).
 * <p>
 * Every case builds the query the way the export does — through
 * {@link ContentDriveHelper#toBrowserQueryBuilder} with folders and links off and working versions
 * on — and compares the rows with what the listing shows for the same request, paged to the end
 * through {@code nextContentCursor}. The listing is always read before any budget is lowered, so it
 * is the full, unbudgeted answer.
 */
public class BrowserAPIExportTest extends IntegrationTestBase {

    private static final int NO_LIMIT = 100_000;
    private static final int MAX_PAGES = 200;

    private static User systemUser;
    private static ContentDriveHelper helper;
    private static WorkflowAPI workflowAPI;
    private static ContentletAPI contentletAPI;

    private static Host site;
    private static Folder folder;
    private static Language defaultLanguage;
    private static Language secondLanguage;
    private static WorkflowScheme scheme;
    private static WorkflowStep stepOne;
    private static Category categoryOne;

    /** Type with every state the listing filters on. */
    private static ContentType mixedType;
    /** Type with 40 contentlets, for the scan-cap case. */
    private static ContentType bulkType;
    /** Type with 6 contentlets, for the row limit. */
    private static ContentType sixType;
    private static ContentType formType;

    private static Contentlet live;
    private static Contentlet liveSecondLanguage;
    private static Contentlet draft;
    private static Contentlet archived;
    private static Contentlet locked;
    private static Contentlet publishedThenEdited;
    private static Contentlet atSiteRoot;
    private static Contentlet inSystemHost;
    private static Contentlet formEntry;
    private static final List<Contentlet> bulk = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        helper = ContentDriveHelper.newInstance();
        workflowAPI = APILocator.getWorkflowAPI();
        contentletAPI = APILocator.getContentletAPI();

        final String unique = String.valueOf(System.currentTimeMillis());
        site = new SiteDataGen().name("browser-export-" + unique + ".local").nextPersisted();
        folder = new FolderDataGen().site(site).name("export-" + unique).nextPersisted();
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        secondLanguage = new LanguageDataGen().nextPersisted();

        scheme = new WorkflowDataGen().name("BrowserExportScheme_" + unique)
                .nextPersistedWithDefaultStepsAndActions();
        final List<WorkflowStep> steps = workflowAPI.findSteps(scheme);
        stepOne = steps.get(0);
        final WorkflowStep stepTwo = steps.get(1);

        final Category parent = new CategoryDataGen().setCategoryName("exportParent" + unique)
                .setKey("exportParent" + unique).setCategoryVelocityVarName("exportParent" + unique)
                .nextPersisted();
        categoryOne = new CategoryDataGen().setCategoryName("exportOne" + unique)
                .setKey("exportOne" + unique).setCategoryVelocityVarName("exportOne" + unique)
                .parent(parent).nextPersisted();
        final Category categoryTwo = new CategoryDataGen().setCategoryName("exportTwo" + unique)
                .setKey("exportTwo" + unique).setCategoryVelocityVarName("exportTwo" + unique)
                .parent(parent).nextPersisted();

        // FieldDataGen gives every field a text default value, which would leak into the rows
        // the filters see; every field here clears it.
        mixedType = new ContentTypeDataGen().host(site)
                .name("BrowserExportMixed " + unique).velocityVarName("browserExportMixed" + unique)
                .workflowId(scheme.getId())
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .field(new FieldDataGen().defaultValue(null).type(TagField.class).name("labels")
                        .velocityVarName("labels").next())
                .field(new FieldDataGen().defaultValue(null).type(CategoryField.class).name("cats")
                        .velocityVarName("cats").values(parent.getInode()).next())
                .nextPersisted();

        live = ContentletDataGen.publish(mixed(folder, "alpha needle", "red", categoryOne));
        moveToStep(live, stepOne);

        final Contentlet secondLanguageDraft = ContentletDataGen.checkout(live);
        secondLanguageDraft.setLanguageId(secondLanguage.getId());
        liveSecondLanguage = ContentletDataGen.checkin(secondLanguageDraft);

        draft = mixed(folder, "beta", "blue", categoryTwo);
        moveToStep(draft, stepTwo);

        archived = ContentletDataGen.publish(mixed(folder, "gamma needle", "red", categoryTwo));
        ContentletDataGen.archive(archived);

        locked = ContentletDataGen.publish(mixed(folder, "delta", "blue", categoryOne));
        contentletAPI.lock(locked, systemUser, false);

        final Contentlet published = ContentletDataGen.publish(mixed(folder, "epsilon", "red",
                categoryOne));
        final Contentlet checkout = contentletAPI.checkout(published.getInode(), systemUser, false);
        checkout.setProperty("title", "epsilon edited");
        checkout.setIndexPolicy(IndexPolicy.FORCE);
        publishedThenEdited = contentletAPI.checkin(checkout, systemUser, false);

        atSiteRoot = mixed(null, "root needle", "blue", categoryTwo);
        inSystemHost = new ContentletDataGen(mixedType).host(APILocator.systemHost())
                .languageId(defaultLanguage.getId()).setProperty("title", "system needle")
                .nextPersisted();

        bulkType = simpleType("BrowserExportBulk", unique);
        for (int i = 0; i < 40; i++) {
            bulk.add(new ContentletDataGen(bulkType).host(site).folder(folder)
                    .setProperty("title", "bulk " + i).nextPersisted());
        }

        sixType = simpleType("BrowserExportSix", unique);
        for (int i = 0; i < 6; i++) {
            new ContentletDataGen(sixType).host(site).folder(folder)
                    .setProperty("title", "six " + i).nextPersisted();
        }

        formType = new ContentTypeDataGen().baseContentType(BaseContentType.FORM).host(site)
                .name("BrowserExportForm" + unique).velocityVarName("browserExportForm" + unique)
                .nextPersisted();
        formEntry = new ContentletDataGen(formType).host(site).nextPersistedWithSampleTextValues();
    }

    private static Contentlet mixed(final Folder target, final String title, final String tag,
            final Category category) {
        final ContentletDataGen dataGen = new ContentletDataGen(mixedType).host(site)
                .languageId(defaultLanguage.getId())
                .setProperty("title", title).setProperty("labels", tag).addCategory(category);
        if (target != null) {
            dataGen.folder(target);
        }
        return dataGen.nextPersisted();
    }

    private static ContentType simpleType(final String name, final String unique) {
        return new ContentTypeDataGen().host(site).name(name + " " + unique)
                .velocityVarName(name + unique)
                .field(new FieldDataGen().defaultValue(null).type(TextField.class).name("title")
                        .velocityVarName("title").next())
                .nextPersisted();
    }

    private static void moveToStep(final Contentlet contentlet, final WorkflowStep step)
            throws Exception {
        workflowAPI.deleteWorkflowTaskByContentletIdAnyLanguage(contentlet, systemUser);
        final WorkflowTask task = workflowAPI.createWorkflowTask(contentlet, systemUser, step,
                "browser-export-test", "browser-export-test");
        workflowAPI.saveWorkflowTask(task);
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    /** The site at any depth, both languages, the mixed type. */
    private static DriveRequestForm.Builder siteRequest(final ContentType type) {
        return DriveRequestForm.builder()
                .assetPath("//" + site.getHostname() + "/")
                .browseScope(BrowseScope.ALL)
                .includeSystemHost(true)
                .language(List.of(String.valueOf(defaultLanguage.getId()),
                        String.valueOf(secondLanguage.getId())))
                .contentTypes(List.of(type.variable()))
                .showFolders(false)
                .live(false)
                .archived(false)
                .maxResults(10);
    }

    /** The working inodes the listing shows for a request, every page, read as the system user. */
    private static Set<String> listingInodes(final DriveRequestForm request) throws Exception {
        final Set<String> inodes = new HashSet<>();
        int cursor = 0;
        for (int page = 1; page <= MAX_PAGES; page++) {
            final PaginatedContents result = helper.driveSearch(
                    DriveRequestForm.builder().from(request).contentCursor(cursor).build(),
                    systemUser);
            result.list.forEach(item -> inodes.add((String) item.get("inode")));
            if (!result.hasMoreContent) {
                return inodes;
            }
            cursor = result.nextContentCursor;
        }
        throw new AssertionError("The listing did not end after " + MAX_PAGES + " pages");
    }

    /** The query the export runs for a request: the listing's, without folders and links. */
    private static BrowserQuery exportQuery(final DriveRequestForm request) throws Exception {
        return helper.toBrowserQueryBuilder(request, systemUser)
                .showFolders(false).showLinks(false).showWorking(true).build();
    }

    private static ExportCandidates export(final BrowserAPI browserAPI,
            final DriveRequestForm request, final int maxRows) throws Exception {
        return browserAPI.getContentForExport(exportQuery(request), maxRows);
    }

    private static Set<String> inodes(final ExportCandidates candidates) {
        return candidates.rows().stream().map(ExportRow::inode).collect(Collectors.toSet());
    }

    private static Set<String> inodesOf(final List<Contentlet> contentlets) {
        return contentlets.stream().map(Contentlet::getInode).collect(Collectors.toSet());
    }

    /**
     * Asserts the export returns exactly the listing's rows for a request, and that the listing is
     * not empty, so the comparison cannot pass vacuously.
     */
    private static void assertSameRowsAsListing(final String label, final DriveRequestForm request)
            throws Exception {
        final Set<String> listing = listingInodes(request);
        assertFalse(label + ": the listing must show rows, or the comparison proves nothing",
                listing.isEmpty());
        final ExportCandidates candidates = export(APILocator.getBrowserAPI(), request, NO_LIMIT);
        assertFalse(label, candidates.overLimit());
        assertEquals(label, listing, inodes(candidates));
        assertEquals(label + ": one row per identifier and language",
                candidates.rows().size(), candidates.rows().stream().map(ExportRow::key)
                        .collect(Collectors.toSet()).size());
    }

    /**
     * Runs an action with browser settings changed, restoring each to its default afterwards.
     */
    private static void withSettings(final Map<String, Object> settings, final Action action)
            throws Exception {
        settings.forEach(Config::setProperty);
        try {
            action.run();
        } finally {
            Config.setProperty(BrowserAPIImpl.BROWSER_DB_MAX_SCAN_ROWS_KEY,
                    BrowserAPIImpl.BROWSER_DB_MAX_SCAN_ROWS_DEFAULT);
            Config.setProperty(BrowserAPIImpl.BROWSER_CONTENT_CHUNK_SIZE_KEY,
                    BrowserAPIImpl.BROWSER_CONTENT_CHUNK_SIZE_DEFAULT);
            Config.setProperty(BrowserAPIImpl.BROWSER_DB_MAX_SCAN_TIME_MILLIS_KEY,
                    BrowserAPIImpl.BROWSER_DB_MAX_SCAN_TIME_MILLIS_DEFAULT);
            Config.setProperty(BrowserAPIImpl.BROWSER_SINGLE_PASS_CHUNK_SIZE_KEY,
                    BrowserAPIImpl.BROWSER_SINGLE_PASS_CHUNK_SIZE_DEFAULT);
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    // ---------------------------------------------------------------------------------------------
    // tests
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (a) No text filter, 40 contentlets, and {@code BROWSER_DB_MAX_SCAN_ROWS}
     *                 lowered to 15, far below them
     * ExpectedResult: All 40 rows: the listing's scan cap does not apply to an export (FR-011)
     */
    @Test
    public void test_noTextFilter_ignoresTheScanCap() throws Exception {
        final Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(BrowserAPIImpl.BROWSER_DB_MAX_SCAN_ROWS_KEY, 15);
        settings.put(BrowserAPIImpl.BROWSER_CONTENT_CHUNK_SIZE_KEY, 7);
        withSettings(settings, () -> {
            final ExportCandidates candidates =
                    export(new BrowserAPIImpl(), siteRequest(bulkType).build(), NO_LIMIT);

            assertFalse(candidates.overLimit());
            assertEquals(inodesOf(bulk), inodes(candidates));
        });
    }

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (b) A text filter, with the scan rows and the chunk sizes lowered so the
     *                 candidates span many chunks and exceed the listing's scan cap
     * ExpectedResult: Exactly the rows the listing matches when it runs unbudgeted (US1 #11)
     */
    @Test
    public void test_textFilter_matchesTheListingAcrossChunks() throws Exception {
        final DriveRequestForm request = siteRequest(mixedType)
                .filters(QueryFilters.builder().text("needle").build()).build();
        final Set<String> listing = listingInodes(request);
        assertTrue("fixture: the text must match the site-root item",
                listing.contains(atSiteRoot.getInode()));
        assertFalse("fixture: the text must not match every row", listing.contains(draft.getInode()));

        final Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(BrowserAPIImpl.BROWSER_DB_MAX_SCAN_ROWS_KEY, 2);
        settings.put(BrowserAPIImpl.BROWSER_CONTENT_CHUNK_SIZE_KEY, 2);
        settings.put(BrowserAPIImpl.BROWSER_SINGLE_PASS_CHUNK_SIZE_KEY, 2);
        settings.put(BrowserAPIImpl.BROWSER_DB_MAX_SCAN_TIME_MILLIS_KEY, 1L);
        withSettings(settings, () -> {
            final ExportCandidates candidates = export(new BrowserAPIImpl(), request, NO_LIMIT);
            assertEquals(listing, inodes(candidates));
        });
    }

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (c) A contentlet published and then edited, so it has a live version and a
     *                 newer working one
     * ExpectedResult: One row for it, carrying the working inode (FR-017)
     */
    @Test
    public void test_publishedThenEdited_givesOneWorkingRow() throws Exception {
        final ExportCandidates candidates =
                export(APILocator.getBrowserAPI(), siteRequest(mixedType).build(), NO_LIMIT);

        final List<ExportRow> rows = candidates.rows().stream()
                .filter(row -> row.identifier().equals(publishedThenEdited.getIdentifier()))
                .collect(Collectors.toList());
        assertEquals(1, rows.size());
        assertEquals(publishedThenEdited.getInode(), rows.get(0).inode());
    }

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (d) Each listing filter on its own: language, workflow scheme, workflow step,
     *                 each status, archived, and each System Host mode
     * ExpectedResult: The same rows as the listing every time
     */
    @Test
    public void test_listingFilters_giveTheListingRows() throws Exception {
        assertSameRowsAsListing("language", siteRequest(mixedType)
                .language(List.of(String.valueOf(secondLanguage.getId()))).build());
        assertSameRowsAsListing("workflow scheme", siteRequest(mixedType)
                .workflow(List.of(WorkflowFilterForm.builder().scheme(scheme.getId()).build()))
                .build());
        assertSameRowsAsListing("workflow step", siteRequest(mixedType)
                .workflow(List.of(WorkflowFilterForm.builder().scheme(scheme.getId())
                        .step(stepOne.getId()).build()))
                .build());
        assertSameRowsAsListing("status ARCHIVED",
                siteRequest(mixedType).status(List.of("ARCHIVED")).build());
        assertSameRowsAsListing("status UNPUBLISHED",
                siteRequest(mixedType).status(List.of("UNPUBLISHED")).build());
        assertSameRowsAsListing("status LOCKED",
                siteRequest(mixedType).status(List.of("LOCKED")).build());
        assertSameRowsAsListing("archived", siteRequest(mixedType).archived(true).build());
        assertSameRowsAsListing("System Host INCLUDE",
                siteRequest(mixedType).includeSystemHost(true).build());
        assertSameRowsAsListing("System Host EXCLUDE",
                siteRequest(mixedType).includeSystemHost(false).build());
        assertSameRowsAsListing("System Host ONLY",
                siteRequest(mixedType).browseScope(BrowseScope.SYSTEM_HOST).build());

        assertTrue("fixture: System Host content is listed with INCLUDE",
                listingInodes(siteRequest(mixedType).build()).contains(inSystemHost.getInode()));
        assertTrue("fixture: archived content is listed when asked for",
                listingInodes(siteRequest(mixedType).archived(true).build())
                        .contains(archived.getInode()));
    }

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (e) A Tag field criterion, which the database resolves, and a Category field
     *                 criterion, which the index resolves
     * ExpectedResult: The same rows as the listing for each
     */
    @Test
    public void test_fieldCriteria_giveTheListingRows() throws Exception {
        assertSameRowsAsListing("tag", siteRequest(mixedType)
                .userSearchable(Map.of("labels", List.of("red"))).build());
        assertSameRowsAsListing("category", siteRequest(mixedType)
                .userSearchable(Map.of("cats", List.of(categoryOne.getInode()))).build());
    }

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (f) 6 readable rows, with a limit of 5 and then of 6
     * ExpectedResult: Over the limit at 5; all 6 rows and not over the limit at 6
     */
    @Test
    public void test_limit_flagsOverLimit() throws Exception {
        final DriveRequestForm request = siteRequest(sixType).build();

        assertTrue(export(APILocator.getBrowserAPI(), request, 5).overLimit());

        final ExportCandidates atLimit = export(APILocator.getBrowserAPI(), request, 6);
        assertFalse(atLimit.overLimit());
        assertEquals(6, atLimit.rows().size());
    }

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (g) Rows in two languages and several identifiers
     * ExpectedResult: Ordered by identifier, then language
     */
    @Test
    public void test_rows_areOrderedByIdentifierThenLanguage() throws Exception {
        final ExportCandidates candidates =
                export(APILocator.getBrowserAPI(), siteRequest(mixedType).build(), NO_LIMIT);
        assertTrue("fixture: both languages of one identifier are exported",
                inodes(candidates).containsAll(Set.of(live.getInode(),
                        liveSecondLanguage.getInode())));

        final List<ExportRow> sorted = new ArrayList<>(candidates.rows());
        sorted.sort(Comparator.comparing(ExportRow::identifier)
                .thenComparingLong(ExportRow::languageId));
        assertEquals(sorted, candidates.rows());
    }

    /**
     * Method to test: {@link BrowserAPI#getContentForExport(BrowserQuery, int)}
     * Given Scenario: (h) The whole site, every base type, with a Form entry on it, and the whole of
     *                 System Host, where every site lives
     * ExpectedResult: No Form entry and no Host row is ever returned (FR-007)
     */
    @Test
    public void test_formsAndHosts_areNeverReturned() throws Exception {
        final ContentType hostType = APILocator.getContentTypeAPI(systemUser)
                .find(Host.HOST_VELOCITY_VAR_NAME);
        final DriveRequestForm wholeSite = DriveRequestForm.builder()
                .assetPath("//" + site.getHostname() + "/")
                .browseScope(BrowseScope.ALL).includeSystemHost(true)
                .language(List.of(String.valueOf(defaultLanguage.getId())))
                .showFolders(false).live(false).archived(false).build();

        final ExportCandidates candidates = export(APILocator.getBrowserAPI(), wholeSite, NO_LIMIT);

        assertTrue("fixture: the site's other content is exported",
                inodes(candidates).contains(atSiteRoot.getInode()));
        assertFalse("a Form entry is never exported",
                inodes(candidates).contains(formEntry.getInode()));
        for (final ExportRow row : candidates.rows()) {
            assertFalse("no Host row: " + row, hostType.id().equals(row.contentTypeId()));
            assertFalse("no Form row: " + row, formType.id().equals(row.contentTypeId()));
        }
    }
}
