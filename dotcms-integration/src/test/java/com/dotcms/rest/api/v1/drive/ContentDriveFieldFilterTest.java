package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration test for Content Drive field-based filtering ({@code userSearchable}), issue #36384
 * (PR 2 — resolver + routing).
 *
 * <p>Exercises {@link ContentDriveHelper#driveSearch} with the per-field {@code userSearchable} map
 * and asserts the ADR-0018 routing contract implemented in
 * {@link com.dotcms.browser.BrowserAPIImpl}:</p>
 *
 * <ul>
 *   <li><b>Text field → index</b>: {@code contains} semantics via the reused field strategies.</li>
 *   <li><b>Tag field → database</b>: resolved DB-side ({@code tag}/{@code tag_inode} join), so a
 *   freshly saved item is filterable immediately (read-your-writes), without waiting for the
 *   index.</li>
 *   <li><b>Date-Time range → index</b>: {@code from/to} range.</li>
 *   <li><b>AND composition</b>: an index-routed and a DB-routed criterion combine.</li>
 *   <li><b>Validation</b>: unknown key, non-searchable key and more than one content type all
 *   yield a {@link BadRequestException} (HTTP 400).</li>
 * </ul>
 */
@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ContentDriveFieldFilterTest extends IntegrationTestBase {

    private static final ContentDriveHelper contentDriveHelper = new ContentDriveHelper();
    private static User systemUser;

    private static Host testSite;
    private static Folder testFolder;
    private static String testAssetPath;

    private static ContentType typeWithFields;
    private static ContentType otherType;

    private static final String TEXT_VAR = "topic";
    private static final String TAG_VAR = "labels";
    private static final String DATE_VAR = "postingDate";
    private static final String NON_SEARCHABLE_VAR = "notSearchable";

    // angular text + tags [angular, cms] + 2024
    private static Contentlet angularWithTags;
    // react text + tag [vue] + 2020
    private static Contentlet reactWithVue;
    // angular text + no tags + 2026
    private static Contentlet angularNoTags;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();

        final String uniqueId = System.currentTimeMillis() + "";

        testSite = new SiteDataGen().name("drive-ff-" + uniqueId + ".local").nextPersisted();
        testFolder = new FolderDataGen().name("driveFfFolder_" + uniqueId).site(testSite).nextPersisted();
        testAssetPath = "//" + testSite.getHostname() + testFolder.getPath();

        typeWithFields = new ContentTypeDataGen()
                .name("DriveFfType_" + uniqueId)
                .velocityVarName("driveFfType_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();

        // A second content type to exercise the "exactly one content type" validation.
        otherType = new ContentTypeDataGen()
                .name("DriveFfOther_" + uniqueId)
                .velocityVarName("driveFfOther_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();

        new FieldDataGen().type(TextField.class).name(TEXT_VAR).velocityVarName(TEXT_VAR)
                .contentTypeId(typeWithFields.id()).searchable(true).indexed(true).nextPersisted();
        new FieldDataGen().type(TagField.class).name(TAG_VAR).velocityVarName(TAG_VAR)
                .contentTypeId(typeWithFields.id()).searchable(true).indexed(true).nextPersisted();
        new FieldDataGen().type(DateTimeField.class).name(DATE_VAR).velocityVarName(DATE_VAR)
                .values("").defaultValue("")
                .contentTypeId(typeWithFields.id()).searchable(true).indexed(true).nextPersisted();
        // Not user-searchable — filtering on it must be rejected.
        new FieldDataGen().type(TextField.class).name(NON_SEARCHABLE_VAR).velocityVarName(NON_SEARCHABLE_VAR)
                .contentTypeId(typeWithFields.id()).searchable(false).indexed(true).nextPersisted();

        angularWithTags = new ContentletDataGen(typeWithFields.id())
                .setProperty("title", "Angular with tags " + uniqueId)
                .setProperty(TEXT_VAR, "angular")
                .setProperty(TAG_VAR, "angular,cms")
                .setProperty(DATE_VAR, date(2024))
                .folder(testFolder)
                .nextPersisted();

        reactWithVue = new ContentletDataGen(typeWithFields.id())
                .setProperty("title", "React with vue " + uniqueId)
                .setProperty(TEXT_VAR, "react")
                .setProperty(TAG_VAR, "vue")
                .setProperty(DATE_VAR, date(2020))
                .folder(testFolder)
                .nextPersisted();

        angularNoTags = new ContentletDataGen(typeWithFields.id())
                .setProperty("title", "Angular no tags " + uniqueId)
                .setProperty(TEXT_VAR, "angular")
                .setProperty(DATE_VAR, date(2026))
                .folder(testFolder)
                .nextPersisted();

        Logger.info(ContentDriveFieldFilterTest.class,
                "Field-filter test data ready under " + testAssetPath);
    }

    private static Date date(final int year) {
        // year-01-01 00:00:00 UTC-ish; only the year matters for the range assertions.
        return new Date(year - 1900, 0, 1);
    }

    private Set<String> driveInodes(final DriveRequestForm request)
            throws DotDataException, DotSecurityException {
        final PaginatedContents results = contentDriveHelper.driveSearch(request, systemUser);
        return results.list.stream()
                .map(item -> (String) ((Map<String, Object>) item).get("inode"))
                .collect(Collectors.toSet());
    }

    private DriveRequestForm.Builder baseRequest() {
        return DriveRequestForm.builder()
                .assetPath(testAssetPath)
                .contentTypes(List.of(typeWithFields.variable()))
                .live(false)
                .archived(false)
                .showFolders(false)
                .offset(0)
                .maxResults(100);
    }

    /**
     * Text field routes to the index and applies {@code contains} semantics: both "angular" items
     * match, the "react" item does not.
     */
    @Test
    public void testTextFieldFiltersViaIndex() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(TEXT_VAR, "angular"))
                .build());

        assertTrue("angular item (with tags) must match the text filter",
                inodes.contains(angularWithTags.getInode()));
        assertTrue("angular item (no tags) must match the text filter",
                inodes.contains(angularNoTags.getInode()));
        assertFalse("react item must not match the 'angular' text filter",
                inodes.contains(reactWithVue.getInode()));
    }

    /**
     * Tag field routes to the database, so the tagged item is filterable immediately after being
     * saved (read-your-writes). Only the item carrying the "cms" tag matches.
     */
    @Test
    public void testTagFieldFiltersViaDatabase() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(TAG_VAR, List.of("cms")))
                .build());

        assertTrue("item tagged 'cms' must match the tag filter",
                inodes.contains(angularWithTags.getInode()));
        assertFalse("item tagged only 'vue' must not match",
                inodes.contains(reactWithVue.getInode()));
        assertFalse("untagged item must not match",
                inodes.contains(angularNoTags.getInode()));
    }

    /**
     * Tag names within a single field combine with OR: filtering by [cms, vue] returns both tagged
     * items.
     */
    @Test
    public void testTagFieldOrsValuesWithinField() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(TAG_VAR, List.of("cms", "vue")))
                .build());

        assertTrue(inodes.contains(angularWithTags.getInode()));
        assertTrue(inodes.contains(reactWithVue.getInode()));
        assertFalse(inodes.contains(angularNoTags.getInode()));
    }

    /**
     * Date-Time field routes to the index and applies a from/to range: only the 2024 item falls
     * within 2023–2025.
     */
    @Test
    public void testDateRangeFiltersViaIndex() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(DATE_VAR,
                        Map.of("from", "2023-01-01", "to", "2025-01-01")))
                .build());

        assertTrue("2024 item must fall within the 2023-2025 range",
                inodes.contains(angularWithTags.getInode()));
        assertFalse("2020 item must be outside the range",
                inodes.contains(reactWithVue.getInode()));
        assertFalse("2026 item must be outside the range",
                inodes.contains(angularNoTags.getInode()));
    }

    /**
     * An index-routed (text) and a DB-routed (tag) criterion combine with AND: only the item that
     * is both "angular" and tagged "cms" matches.
     */
    @Test
    public void testIndexAndDbCriteriaCombineWithAnd() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(
                        TEXT_VAR, "angular",
                        TAG_VAR, List.of("cms")))
                .build());

        assertTrue("only the angular + cms item must match",
                inodes.contains(angularWithTags.getInode()));
        assertFalse(inodes.contains(reactWithVue.getInode()));
        assertFalse("angular but untagged item must be excluded by the tag criterion",
                inodes.contains(angularNoTags.getInode()));
    }

    /**
     * A key that is not a field of the content type must be rejected with a 400.
     */
    @Test
    public void testUnknownFieldReturns400() {
        try {
            driveInodes(baseRequest()
                    .userSearchable(Map.of("thisFieldDoesNotExist", "x"))
                    .build());
            fail("Expected a BadRequestException for an unknown field key");
        } catch (final BadRequestException e) {
            // expected
        } catch (final Exception e) {
            fail("Expected a BadRequestException, got: " + e.getClass().getName());
        }
    }

    /**
     * A field that is not user-searchable must be rejected with a 400.
     */
    @Test
    public void testNonSearchableFieldReturns400() {
        try {
            driveInodes(baseRequest()
                    .userSearchable(Map.of(NON_SEARCHABLE_VAR, "x"))
                    .build());
            fail("Expected a BadRequestException for a non-searchable field");
        } catch (final BadRequestException e) {
            // expected
        } catch (final Exception e) {
            fail("Expected a BadRequestException, got: " + e.getClass().getName());
        }
    }

    /**
     * Field filtering requires exactly one content type to resolve field definitions; more than one
     * must be rejected with a 400.
     */
    @Test
    public void testMultipleContentTypesReturns400() {
        try {
            driveInodes(DriveRequestForm.builder()
                    .assetPath(testAssetPath)
                    .contentTypes(List.of(typeWithFields.variable(), otherType.variable()))
                    .showFolders(false)
                    .userSearchable(Map.of(TEXT_VAR, "angular"))
                    .build());
            fail("Expected a BadRequestException when more than one content type is provided");
        } catch (final BadRequestException e) {
            // expected
        } catch (final Exception e) {
            fail("Expected a BadRequestException, got: " + e.getClass().getName());
        }
    }
}
