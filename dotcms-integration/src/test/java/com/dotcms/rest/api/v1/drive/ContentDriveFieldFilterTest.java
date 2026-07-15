package com.dotcms.rest.api.v1.drive;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.IntegrationTestBase;
import com.dotcms.browser.BrowserAPIImpl.PaginatedContents;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RelationshipField;
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
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private static ContentType relatedType;
    private static String relationshipVar;
    private static Contentlet childNews;
    private static Contentlet childPress;

    private static final String TEXT_VAR = "topic";
    private static final String TAG_VAR = "labels";
    private static final String DATE_VAR = "postingDate";
    private static final String MULTI_VAR = "sections";
    private static final String BOOL_VAR = "featured";
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
        new FieldDataGen().type(MultiSelectField.class).name(MULTI_VAR).velocityVarName(MULTI_VAR)
                .values("news|news\r\npress|press\r\nopinion|opinion").defaultValue("")
                .contentTypeId(typeWithFields.id()).searchable(true).indexed(true).nextPersisted();
        new FieldDataGen().type(CheckboxField.class).name(BOOL_VAR).velocityVarName(BOOL_VAR)
                .values("true|true\r\nfalse|false").defaultValue("")
                .contentTypeId(typeWithFields.id()).searchable(true).indexed(true).nextPersisted();
        // Not user-searchable — filtering on it must be rejected.
        new FieldDataGen().type(TextField.class).name(NON_SEARCHABLE_VAR).velocityVarName(NON_SEARCHABLE_VAR)
                .contentTypeId(typeWithFields.id()).searchable(false).indexed(true).nextPersisted();

        angularWithTags = new ContentletDataGen(typeWithFields.id())
                .setProperty("title", "Angular with tags " + uniqueId)
                .setProperty(TEXT_VAR, "angular")
                .setProperty(TAG_VAR, "angular,cms")
                .setProperty(DATE_VAR, date(2024))
                .setProperty(MULTI_VAR, "news")
                .setProperty(BOOL_VAR, "true")
                .folder(testFolder)
                .nextPersisted();

        reactWithVue = new ContentletDataGen(typeWithFields.id())
                .setProperty("title", "React with vue " + uniqueId)
                .setProperty(TEXT_VAR, "react")
                .setProperty(TAG_VAR, "vue")
                .setProperty(DATE_VAR, date(2020))
                .setProperty(MULTI_VAR, "press")
                .setProperty(BOOL_VAR, "false")
                .folder(testFolder)
                .nextPersisted();

        angularNoTags = new ContentletDataGen(typeWithFields.id())
                .setProperty("title", "Angular no tags " + uniqueId)
                .setProperty(TEXT_VAR, "angular")
                .setProperty(DATE_VAR, date(2026))
                .setProperty(BOOL_VAR, "false")
                .folder(testFolder)
                .nextPersisted();

        // Relationship setup: typeWithFields (parent) references relatedType (child) via a field.
        relatedType = new ContentTypeDataGen()
                .name("DriveFfRelated_" + uniqueId)
                .velocityVarName("driveFfRelated_" + uniqueId)
                .baseContentType(BaseContentType.CONTENT)
                .host(testSite)
                .nextPersisted();

        relationshipVar = "related";
        final Field relationshipField = FieldBuilder.builder(RelationshipField.class)
                .name(relationshipVar)
                .variable(relationshipVar)
                .contentTypeId(typeWithFields.id())
                .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(relatedType.variable())
                .required(false)
                .indexed(true)
                .searchable(true)
                .build();
        final Field savedRelationshipField =
                APILocator.getContentTypeFieldAPI().save(relationshipField, systemUser);
        final Relationship relationship =
                APILocator.getRelationshipAPI().getRelationshipFromField(savedRelationshipField, systemUser);

        childNews = new ContentletDataGen(relatedType.id())
                .setProperty("title", "Child news " + uniqueId).folder(testFolder).nextPersisted();
        childPress = new ContentletDataGen(relatedType.id())
                .setProperty("title", "Child press " + uniqueId).folder(testFolder).nextPersisted();

        // Re-checkin creates a new working version (new inode); reassign so inode-based assertions
        // keep pointing at the current version.
        angularWithTags = relate(angularWithTags, relationship, childNews);
        reactWithVue = relate(reactWithVue, relationship, childPress);

        Logger.info(ContentDriveFieldFilterTest.class,
                "Field-filter test data ready under " + testAssetPath);
    }

    /**
     * Relates a child under the given relationship on a (re-checked-out) parent contentlet and
     * returns the resulting working version.
     */
    private static Contentlet relate(final Contentlet parent, final Relationship relationship,
            final Contentlet child) {
        final Contentlet checkedOut = ContentletDataGen.checkout(parent);
        checkedOut.setProperty(relationship.getChildRelationName(), List.of(child));
        return ContentletDataGen.checkin(checkedOut);
    }

    private static Date date(final int year) {
        // year-01-01T00:00:00Z — explicit UTC so the range assertions don't depend on the CI
        // agent's default timezone.
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
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
        // ISO-8601 with milliseconds + Z, exactly as the FE sends it (the format that previously
        // matched nothing until the bound normalization was added).
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(DATE_VAR,
                        Map.of("from", "2023-01-01T00:00:00.000Z", "to", "2025-01-01T00:00:00.000Z")))
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
     * Multi-Select "in list" is OR (match any): filtering by [news, press] returns both items,
     * filtering by [news] returns only the news item. Consistent with Tag/Category OR-in-list.
     */
    @Test
    public void testMultiSelectOrsValues() throws DotDataException, DotSecurityException {
        final Set<String> either = driveInodes(baseRequest()
                .userSearchable(Map.of(MULTI_VAR, List.of("news", "press")))
                .build());
        assertTrue("news item must match [news, press]", either.contains(angularWithTags.getInode()));
        assertTrue("press item must match [news, press]", either.contains(reactWithVue.getInode()));

        final Set<String> onlyNews = driveInodes(baseRequest()
                .userSearchable(Map.of(MULTI_VAR, List.of("news")))
                .build());
        assertTrue("news item must match [news]", onlyNews.contains(angularWithTags.getInode()));
        assertFalse("press item must not match [news]", onlyNews.contains(reactWithVue.getInode()));
    }

    /**
     * Boolean (checkbox) equals: filtering by {@code featured=true} returns only the true item.
     */
    @Test
    public void testBooleanCheckboxFilter() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(BOOL_VAR, true))
                .build());
        assertTrue("featured=true item must match", inodes.contains(angularWithTags.getInode()));
        assertFalse("featured=false item must not match", inodes.contains(reactWithVue.getInode()));
        assertFalse("featured=false item must not match", inodes.contains(angularNoTags.getInode()));
    }

    /**
     * Open-ended range (only {@code from}) matches everything at or after the bound.
     */
    @Test
    public void testOpenEndedDateRange() throws DotDataException, DotSecurityException {
        final Map<String, Object> onlyFrom = new java.util.HashMap<>();
        onlyFrom.put("from", "2023-01-01");
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(DATE_VAR, onlyFrom))
                .build());
        assertTrue("2024 item is after 2023", inodes.contains(angularWithTags.getInode()));
        assertTrue("2026 item is after 2023", inodes.contains(angularNoTags.getInode()));
        assertFalse("2020 item is before 2023", inodes.contains(reactWithVue.getInode()));
    }

    /**
     * Relationship field routes to the DB ({@code tree}): filtering the parent by a child identifier
     * returns only the parent that references it (read-your-writes, no index involved).
     */
    @Test
    public void testRelationshipFiltersViaDatabase() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(relationshipVar, List.of(childNews.getIdentifier())))
                .build());
        assertTrue("parent related to 'news' child must match",
                inodes.contains(angularWithTags.getInode()));
        assertFalse("parent related to 'press' child must not match",
                inodes.contains(reactWithVue.getInode()));
        assertFalse("unrelated parent must not match",
                inodes.contains(angularNoTags.getInode()));
    }

    /**
     * The relationship filter also accepts a single identifier sent as a scalar string (not an
     * array), e.g. a one-to-one relationship — treated as a one-element list.
     */
    @Test
    public void testRelationshipAcceptsScalarIdentifier() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(relationshipVar, childNews.getIdentifier()))
                .build());
        assertTrue(inodes.contains(angularWithTags.getInode()));
        assertFalse(inodes.contains(reactWithVue.getInode()));
    }

    /**
     * Multiple related identifiers combine with OR (related to any).
     */
    @Test
    public void testRelationshipOrsValues() throws DotDataException, DotSecurityException {
        final Set<String> inodes = driveInodes(baseRequest()
                .userSearchable(Map.of(relationshipVar,
                        List.of(childNews.getIdentifier(), childPress.getIdentifier())))
                .build());
        assertTrue(inodes.contains(angularWithTags.getInode()));
        assertTrue(inodes.contains(reactWithVue.getInode()));
        assertFalse(inodes.contains(angularNoTags.getInode()));
    }

    /**
     * A range value on a text field (kind/type mismatch) is rejected.
     */
    @Test
    public void testKindMismatchReturns400() {
        assert400(baseRequest()
                .userSearchable(Map.of(TEXT_VAR, Map.of("from", "1", "to", "2")))
                .build());
    }

    private void assert400(final DriveRequestForm request) {
        try {
            driveInodes(request);
            fail("Expected a BadRequestException");
        } catch (final BadRequestException e) {
            // expected
        } catch (final Exception e) {
            fail("Expected a BadRequestException, got: " + e.getClass().getName());
        }
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
