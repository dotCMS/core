package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.PermissionUtilTest;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rendering.velocity.services.ContentletLoader;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.ContentsWebAPI;
import com.dotcms.rendering.velocity.viewtools.JSONTool;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.transform.TransformerLocator;
import com.dotcms.variant.model.Variant;
import com.dotcms.workflow.form.FireActionForm;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.apache.velocity.context.Context;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the data a reader gets for the Contentlets referenced in a Story Block
 * field, depending on the reader's READ permission on each referenced Contentlet.
 * <p>
 * One carrier Contentlet references two published Contentlets: one readable only by
 * administrators and one readable by anyone. The carrier is read through
 * {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#find(String, User, boolean)}
 * with the reader set in the current request, which is how the REST and GraphQL read paths reach
 * the Story Block hydration.
 */
public class StoryBlockReferenceAccessTest extends IntegrationTestBase {

    private static final String BASE_STORY_BLOCK = "{\"type\":\"doc\",\"content\":["
            + "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"intro\"}]}]}";

    private static ContentType carrierType;
    private static Field storyBlockField;
    private static Contentlet carrier;
    private static Contentlet restrictedTarget;
    private static Contentlet publicTarget;
    private static User readerWithRead;
    private static User readerWithoutRead;
    private static Role restrictedReadersRole;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final long timestamp = System.currentTimeMillis();
        final User systemUser = APILocator.systemUser();
        final long language = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        // Referenced contentlets: a published generic content each, with individual permissions
        final ContentType targetType = APILocator.getContentTypeAPI(systemUser).find("webPageContent");
        restrictedTarget = ContentletDataGen.publish(new ContentletDataGen(targetType)
                .languageId(language)
                .setProperty("title", "restricted " + timestamp)
                .setProperty("body", "restricted body")
                .nextPersisted());
        publicTarget = ContentletDataGen.publish(new ContentletDataGen(targetType)
                .languageId(language)
                .setProperty("title", "public " + timestamp)
                .setProperty("body", "public body")
                .nextPersisted());
        restrictedReadersRole = new RoleDataGen().name("storyBlockReaders" + timestamp).nextPersisted();
        grantReadOnlyTo(restrictedTarget, restrictedReadersRole);
        PermissionUtilTest.addAnonymousUser(publicTarget);

        // Readers: back-end users with and without READ on the restricted contentlet
        readerWithRead = new UserDataGen().active(true).roles(TestUserUtils.getBackendRole()).nextPersisted();
        readerWithoutRead = new UserDataGen().active(true).roles(TestUserUtils.getBackendRole()).nextPersisted();
        PermissionUtilTest.addPermission(restrictedTarget, readerWithRead,
                PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        // Back-end readers are read without front-end roles, so the public target needs an explicit grant too
        for (final User reader : List.of(readerWithRead, readerWithoutRead)) {
            PermissionUtilTest.addPermission(publicTarget, reader,
                    PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        }

        // Carrier: a content type with a Story Block field referencing both targets, readable by anyone
        carrierType = new ContentTypeDataGen()
                .name("storyBlockCarrier" + timestamp)
                .velocityVarName("storyBlockCarrier" + timestamp)
                .nextPersisted();
        storyBlockField = new FieldDataGen()
                .type(StoryBlockField.class)
                .defaultValue(null)
                .contentTypeId(carrierType.id())
                .nextPersisted();
        final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();
        final Object withRestricted = storyBlockAPI.addContentlet(BASE_STORY_BLOCK, restrictedTarget);
        final Object withBoth = storyBlockAPI.addContentlet(withRestricted, publicTarget);
        carrier = ContentletDataGen.publish(new ContentletDataGen(carrierType.id())
                .languageId(language)
                .setProperty(storyBlockField.variable(), withBoth.toString())
                .nextPersisted());
        PermissionUtilTest.addAnonymousUser(carrier);
        for (final User reader : List.of(readerWithRead, readerWithoutRead)) {
            PermissionUtilTest.addPermission(carrier, reader,
                    PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        for (final Contentlet contentlet : Arrays.asList(carrier, restrictedTarget, publicTarget)) {
            if (null != contentlet) {
                ContentletDataGen.destroy(contentlet);
            }
        }
        if (null != carrierType) {
            ContentTypeDataGen.remove(carrierType);
        }
        for (final User user : Arrays.asList(readerWithRead, readerWithoutRead)) {
            if (null != user) {
                UserDataGen.remove(user);
            }
        }
        if (null != restrictedReadersRole) {
            APILocator.getRoleAPI().delete(restrictedReadersRole);
        }
    }

    /**
     * Method to test: {@link StoryBlockAPI#refreshReferences(Contentlet)} through
     * {@code ContentletAPI#find}.
     * Given scenario: an anonymous reader opens the carrier.
     * Expected result: the reference the reader cannot read carries no data, while the reference
     * everyone can read is hydrated.
     */
    @Test
    public void anonymousReader_getsNoDataForTheRestrictedReference() throws Exception {
        final Map<String, Map<String, Object>> references = readCarrierAs(anonymous(), true, false);

        assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
        assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
    }

    /**
     * Given scenario: an anonymous reader opens the carrier when it is not cached, so the
     * hydration runs on the database read path (the one the content search uses).
     * Expected result: same as a cached read.
     */
    @Test
    public void anonymousReader_coldCache_getsNoDataForTheRestrictedReference() throws Exception {
        final Map<String, Map<String, Object>> references = readCarrierAs(anonymous(), true, true);

        assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
        assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
    }

    /**
     * Given scenario: a back-end user with READ on the restricted contentlet opens the carrier.
     * Expected result: both references are hydrated.
     */
    @Test
    public void backendUserWithRead_getsBothReferencesHydrated() throws Exception {
        final Map<String, Map<String, Object>> references = readCarrierAs(readerWithRead, false, false);

        assertEquals(restrictedTarget.getTitle(), references.get(restrictedTarget.getIdentifier()).get("title"));
        assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
    }

    /**
     * Given scenario: a back-end user without READ on the restricted contentlet opens the carrier.
     * Expected result: the restricted reference carries no data; the public one is hydrated.
     */
    @Test
    public void backendUserWithoutRead_getsNoDataForTheRestrictedReference() throws Exception {
        final Map<String, Map<String, Object>> references = readCarrierAs(readerWithoutRead, false, false);

        assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
        assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
    }

    /**
     * Given scenario: an administrator opens the carrier first (which hydrates every reference and
     * leaves the carrier in the cache), then an anonymous reader opens it.
     * Expected result: the previous reader's hydration is not served to the anonymous reader.
     */
    @Test
    public void anonymousReader_afterAnAdminRead_stillGetsNoDataForTheRestrictedReference() throws Exception {
        final Map<String, Map<String, Object>> adminView = readCarrierAs(TestUserUtils.getAdminUser(), false, true);
        assertEquals("precondition: the admin sees the restricted reference hydrated",
                restrictedTarget.getTitle(), adminView.get(restrictedTarget.getIdentifier()).get("title"));

        final Map<String, Map<String, Object>> references = readCarrierAs(anonymous(), true, false);

        assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
    }

    /**
     * Given scenario: an anonymous reader opens the carrier first (which leaves the restricted
     * reference reduced in the cache), then a back-end user with READ on it opens the carrier.
     * Expected result: the reference is hydrated again for the reader who can read it.
     */
    @Test
    public void backendUserWithRead_afterAnAnonymousRead_getsTheRestrictedReferenceHydrated() throws Exception {
        assertOnlyReferenceKeys(readCarrierAs(anonymous(), true, true).get(restrictedTarget.getIdentifier()));

        final Map<String, Map<String, Object>> references = readCarrierAs(readerWithRead, false, false);

        assertEquals(restrictedTarget.getTitle(), references.get(restrictedTarget.getIdentifier()).get("title"));
    }

    /**
     * Given scenario: the carrier is not cached; an anonymous reader opens it (which loads it from
     * the database), then opens it again (served from the cache).
     * Expected result: what each reader gets is computed for that reader only; the shared cache
     * keeps the carrier's stored Story Block, so the cached restricted reference is never the
     * reduced one an anonymous reader received.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void anonymousReads_doNotChangeTheCachedCarrier() throws Exception {
        CacheLocator.getContentletCache().remove(carrier.getInode());

        assertOnlyReferenceKeys(readCarrierAs(anonymous(), true, false).get(restrictedTarget.getIdentifier()));
        assertCachedRestrictedReferenceIsNotReduced();

        assertOnlyReferenceKeys(readCarrierAs(anonymous(), true, false).get(restrictedTarget.getIdentifier()));
        assertCachedRestrictedReferenceIsNotReduced();
    }

    /**
     * Given scenario: an anonymous reader opens the carrier.
     * Expected result: the raw companion of the Story Block field ({@code <field>_raw}), which
     * holds the document as stored, carries the same restricted reference reduced to its keys.
     */
    @Test
    public void anonymousReader_rawCompanionField_getsNoDataForTheRestrictedReference() throws Exception {
        final Contentlet found = findCarrierAs(anonymous(), true, true);

        final Object raw = found.get(storyBlockField.variable() + "_raw");
        assertNotNull("precondition: the carrier is loaded with the raw companion field", raw);
        assertOnlyReferenceKeys(referencesIn(raw).get(restrictedTarget.getIdentifier()));
    }

    /**
     * Given scenario: references are not refreshed on cached reads
     * ({@code REFRESH_BLOCK_EDITOR_REFERENCES=false}). A second carrier references the restricted
     * contentlet and a public one whose title changed after the carrier was saved. The carrier is
     * cached, then an anonymous reader opens it.
     * Expected result: the restricted reference is reduced to its keys, and the public one keeps
     * the data stored with it (its old title), which shows that it was not refreshed.
     */
    @Test
    public void anonymousReader_withoutRefreshOnCachedReads_getsStoredDataOnlyForReadableReferences()
            throws Exception {
        final long language = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final long timestamp = System.currentTimeMillis();
        final String storedTitle = "stored title " + timestamp;
        final Contentlet changingTarget = ContentletDataGen.publish(new ContentletDataGen(
                APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent"))
                .languageId(language)
                .setProperty("title", storedTitle)
                .setProperty("body", "body " + timestamp)
                .nextPersisted());
        PermissionUtilTest.addAnonymousUser(changingTarget);
        final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();
        final Contentlet secondCarrier = ContentletDataGen.publish(new ContentletDataGen(carrierType.id())
                .languageId(language)
                .setProperty(storyBlockField.variable(), storyBlockAPI.addContentlet(
                        storyBlockAPI.addContentlet(BASE_STORY_BLOCK, restrictedTarget), changingTarget).toString())
                .nextPersisted());
        PermissionUtilTest.addAnonymousUser(secondCarrier);
        final Contentlet checkout = ContentletDataGen.checkout(changingTarget);
        checkout.setProperty("title", "changed title " + timestamp);
        ContentletDataGen.publish(ContentletDataGen.checkin(checkout));

        final boolean previous = Config.getBooleanProperty("REFRESH_BLOCK_EDITOR_REFERENCES", true);
        try {
            CacheLocator.getContentletCache().remove(secondCarrier.getInode());
            asReader(anonymous(), () -> APILocator.getContentletAPI().find(secondCarrier.getInode(), anonymous(), true));
            Config.setProperty("REFRESH_BLOCK_EDITOR_REFERENCES", false);

            final Contentlet found = asReader(anonymous(), () -> APILocator.getContentletAPI()
                    .find(secondCarrier.getInode(), anonymous(), true));
            final Map<String, Map<String, Object>> references = referencesIn(found.get(storyBlockField.variable()));

            assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
            assertEquals(storedTitle, references.get(changingTarget.getIdentifier()).get("title"));
        } finally {
            Config.setProperty("REFRESH_BLOCK_EDITOR_REFERENCES", previous);
            ContentletDataGen.destroy(secondCarrier);
            ContentletDataGen.destroy(changingTarget);
        }
    }

    /**
     * Given scenario: the carrier is not cached; an anonymous reader lists the carrier's content
     * type, which loads the carrier from the database.
     * Expected result: the shared cache keeps the carrier's stored Story Block.
     */
    @Test
    public void listingTheContentTypeAsAnonymous_doesNotChangeTheCachedCarrier() throws Exception {
        CacheLocator.getContentletCache().remove(carrier.getInode());

        final List<Contentlet> listed = asReader(anonymous(), () -> APILocator.getContentletAPI()
                .findByStructure(carrierType.inode(), anonymous(), true, 0, 0));

        final Contentlet listedCarrier = listed.stream()
                .filter(contentlet -> carrier.getInode().equals(contentlet.getInode())).findFirst()
                .orElseThrow(() -> new AssertionError("the carrier must be listed"));
        final Map<String, Map<String, Object>> references = referencesIn(listedCarrier.get(storyBlockField.variable()));
        assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
        assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
        assertCachedRestrictedReferenceIsNotReduced();
    }

    /**
     * Given scenario: the carrier is not cached; an anonymous reader loads it by inode through
     * {@code ContentletAPI#findContentlets}, which loads it from the database.
     * Expected result: the reader gets the restricted reference reduced to its keys and the public
     * one hydrated, and the shared cache keeps the carrier's stored Story Block.
     */
    @Test
    public void loadingTheCarrierByInodeAsAnonymous_getsNoDataForTheRestrictedReference() throws Exception {
        CacheLocator.getContentletCache().remove(carrier.getInode());

        final List<Contentlet> found = asReader(anonymous(), () -> APILocator.getContentletAPI()
                .findContentlets(List.of(carrier.getInode())));

        assertEquals(1, found.size());
        final Map<String, Map<String, Object>> references = referencesIn(found.get(0).get(storyBlockField.variable()));
        assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
        assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
        assertCachedRestrictedReferenceIsNotReduced();
    }

    /**
     * Given scenario: the carrier's database row is turned into a Contentlet by the
     * {@code ContentletTransformer} while an anonymous reader is in the current request.
     * Expected result: the restricted reference is reduced to its keys and the public one is
     * hydrated.
     */
    @Test
    public void transformingTheCarrierRowAsAnonymous_getsNoDataForTheRestrictedReference() throws Exception {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select contentlet.*, inode.owner from contentlet, inode "
                        + "where contentlet.inode = ? and contentlet.inode = inode.inode")
                .addParam(carrier.getInode())
                .loadObjectResults();

        final List<Contentlet> transformed = asReader(anonymous(),
                () -> TransformerLocator.createContentletTransformer(rows).asList());

        assertEquals(1, transformed.size());
        final Map<String, Map<String, Object>> references =
                referencesIn(transformed.get(0).get(storyBlockField.variable()));
        assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
        assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
    }

    /**
     * Given scenario: a carrier references a published contentlet everyone can read (a hub). The
     * hub is related to the restricted and the public contentlets, and has its own Story Block
     * referencing both of them.
     * Expected result: an anonymous reader gets the hub hydrated, with the public related content
     * and the public nested reference, but nothing of the restricted contentlet beyond its keys. A
     * back-end user with READ on the restricted contentlet gets it in both places.
     */
    @Test
    public void readableReference_withRestrictedRelatedAndNestedContent_showsOnlyWhatTheReaderCanRead()
            throws Exception {
        final User systemUser = APILocator.systemUser();
        final long language = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final long timestamp = System.currentTimeMillis();
        ContentType hubType = new ContentTypeDataGen()
                .name("storyBlockHub" + timestamp)
                .velocityVarName("storyBlockHub" + timestamp)
                .nextPersisted();
        Contentlet hub = null;
        Contentlet hubCarrier = null;
        try {
            final Field nestedBlockField = new FieldDataGen()
                    .type(StoryBlockField.class)
                    .defaultValue(null)
                    .contentTypeId(hubType.id())
                    .nextPersisted();
            final Field relatedField = APILocator.getContentTypeFieldAPI().save(
                    FieldBuilder.builder(RelationshipField.class)
                            .name("related")
                            .contentTypeId(hubType.id())
                            .values(String.valueOf(com.dotmarketing.util.WebKeys.Relationship
                                    .RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                            .relationType(restrictedTarget.getContentType().variable())
                            .build(), systemUser);
            hubType = APILocator.getContentTypeAPI(systemUser).find(hubType.id());

            final StoryBlockAPI storyBlockAPI = APILocator.getStoryBlockAPI();
            final Contentlet hubCheckout = ContentletDataGen.checkout(new ContentletDataGen(hubType)
                    .languageId(language)
                    .setProperty(nestedBlockField.variable(), storyBlockAPI.addContentlet(
                            storyBlockAPI.addContentlet(BASE_STORY_BLOCK, restrictedTarget), publicTarget).toString())
                    .nextPersisted());
            hub = ContentletDataGen.publish(APILocator.getContentletAPI().checkin(hubCheckout,
                    relate(relatedField, hubCheckout, restrictedTarget, publicTarget), null, null, systemUser, false));
            final Contentlet hubForCarrier = hub;
            hubCarrier = ContentletDataGen.publish(new ContentletDataGen(carrierType.id())
                    .languageId(language)
                    .setProperty(storyBlockField.variable(),
                            storyBlockAPI.addContentlet(BASE_STORY_BLOCK, hubForCarrier).toString())
                    .nextPersisted());
            for (final Contentlet readable : List.of(hub, hubCarrier)) {
                PermissionUtilTest.addAnonymousUser(readable);
                PermissionUtilTest.addPermission(readable, readerWithRead,
                        PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
            }

            final Contentlet carrierForReaders = hubCarrier;
            final Map<String, Object> anonymousHub = referenceDataIn(asReader(anonymous(), () -> APILocator
                    .getContentletAPI().find(carrierForReaders.getInode(), anonymous(), true))
                    .get(storyBlockField.variable()));
            assertEquals("the hub is readable by anyone, so it is hydrated", hub.getInode(),
                    anonymousHub.get(Contentlet.INODE_KEY));
            final List<Map<String, Object>> anonymousRelated = relatedItems(anonymousHub.get(relatedField.variable()));
            assertNoDataOf(restrictedTarget, anonymousRelated);
            assertEquals(publicTarget.getTitle(), itemOf(publicTarget, anonymousRelated).get("title"));
            final List<Map<String, Object>> anonymousNested =
                    referencesInOrder(anonymousHub.get(nestedBlockField.variable()));
            assertOnlyReferenceKeys(anonymousNested.get(0));
            assertEquals(publicTarget.getTitle(), anonymousNested.get(1).get("title"));

            final Map<String, Object> readerHub = referenceDataIn(asReader(readerWithRead, () -> APILocator
                    .getContentletAPI().find(carrierForReaders.getInode(), readerWithRead, false))
                    .get(storyBlockField.variable()));
            assertEquals(restrictedTarget.getTitle(), itemOf(restrictedTarget,
                    relatedItems(readerHub.get(relatedField.variable()))).get("title"));
            assertEquals(restrictedTarget.getTitle(),
                    referencesInOrder(readerHub.get(nestedBlockField.variable())).get(0).get("title"));
        } finally {
            for (final Contentlet contentlet : Arrays.asList(hubCarrier, hub)) {
                if (null != contentlet) {
                    ContentletDataGen.destroy(contentlet);
                }
            }
            ContentTypeDataGen.remove(hubType);
        }
    }

    /**
     * Given scenario: a carrier references a contentlet that has a working version and no live
     * one, readable by anyone.
     * Expected result: an anonymous reader (live mode) gets the reference reduced to its keys,
     * since there is nothing live to read; a back-end user in preview mode gets the working
     * version.
     */
    @Test
    public void referenceWithoutALiveVersion_isOnlyHydratedInPreviewMode() throws Exception {
        final long language = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final long timestamp = System.currentTimeMillis();
        final Contentlet workingOnly = new ContentletDataGen(restrictedTarget.getContentType())
                .languageId(language)
                .setProperty("title", "working only " + timestamp)
                .setProperty("body", "working only body")
                .nextPersisted();
        Contentlet workingCarrier = null;
        try {
            PermissionUtilTest.addAnonymousUser(workingOnly);
            PermissionUtilTest.addPermission(workingOnly, readerWithRead,
                    PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
            workingCarrier = ContentletDataGen.publish(new ContentletDataGen(carrierType.id())
                    .languageId(language)
                    .setProperty(storyBlockField.variable(),
                            APILocator.getStoryBlockAPI().addContentlet(BASE_STORY_BLOCK, workingOnly).toString())
                    .nextPersisted());
            PermissionUtilTest.addAnonymousUser(workingCarrier);
            PermissionUtilTest.addPermission(workingCarrier, readerWithRead,
                    PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
            final String carrierInode = workingCarrier.getInode();

            final Map<String, Object> liveData = referenceDataIn(asReader(anonymous(), () -> APILocator
                    .getContentletAPI().find(carrierInode, anonymous(), true)).get(storyBlockField.variable()));
            assertOnlyReferenceKeys(liveData, workingOnly);

            final Map<String, Object> previewData = referenceDataIn(asReader(readerWithRead,
                    Map.of(com.dotmarketing.util.WebKeys.PAGE_MODE_PARAMETER, PageMode.PREVIEW_MODE),
                    () -> APILocator.getContentletAPI().find(carrierInode, readerWithRead, false))
                    .get(storyBlockField.variable()));
            assertEquals(workingOnly.getTitle(), previewData.get("title"));
        } finally {
            if (null != workingCarrier) {
                ContentletDataGen.destroy(workingCarrier);
            }
            ContentletDataGen.destroy(workingOnly);
        }
    }

    /**
     * Given scenario: a carrier saved in a variant other than the default one is read, from the
     * database and then from the cache.
     * Expected result: both reads report the carrier's variant.
     */
    @Test
    public void carrierInAVariant_keepsItsVariantWhenRead() throws Exception {
        final Variant variant = new VariantDataGen().nextPersisted();
        final Contentlet variantCarrier = new ContentletDataGen(carrierType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .variant(variant)
                .setProperty(storyBlockField.variable(), carrier.get(storyBlockField.variable()))
                .nextPersisted();
        try {
            CacheLocator.getContentletCache().remove(variantCarrier.getInode());
            final Contentlet fromDatabase = asReader(anonymous(), () -> APILocator.getContentletAPI()
                    .find(variantCarrier.getInode(), APILocator.systemUser(), false));
            final Contentlet fromCache = asReader(anonymous(), () -> APILocator.getContentletAPI()
                    .find(variantCarrier.getInode(), APILocator.systemUser(), false));

            assertEquals(variant.name(), fromDatabase.getVariantId());
            assertEquals(variant.name(), fromCache.getVariantId());
        } finally {
            ContentletDataGen.destroy(variantCarrier);
        }
    }

    /**
     * Method to test: {@link ContentletLoader#buildVelocity(Contentlet, PageMode, String)}.
     * Given scenario: the Velocity code of the carrier, which is cached and shared by every user
     * who renders a page with it, is built for a reader who can read the restricted reference
     * and for an anonymous reader, in both orders.
     * Expected result: the code is the same whoever builds it, and the Story Block variable it
     * sets carries every reference with only the keys that identify it.
     */
    @Test
    public void carrierPageCode_isTheSameForEveryReader_andCarriesOnlyReferenceKeys() throws Exception {
        final String readerFirst = buildCarrierCodeAs(readerWithRead);
        final String anonymousSecond = buildCarrierCodeAs(anonymous());
        final String anonymousFirst = buildCarrierCodeAs(anonymous());
        final String readerSecond = buildCarrierCodeAs(readerWithRead);

        assertEquals(readerFirst, anonymousSecond);
        assertEquals(anonymousFirst, readerSecond);
        assertEquals(readerFirst, anonymousFirst);
        assertFalse("the shared code must not carry the restricted reference's data",
                readerFirst.contains(restrictedTarget.getTitle()));
        assertFalse("the shared code must not carry the public reference's data either",
                readerFirst.contains(publicTarget.getTitle()));
    }

    /**
     * Method to test: the Story Block variable set by the code of
     * {@link ContentletLoader#buildVelocity(Contentlet, PageMode, String)}.
     * Given scenario: the code is built once, by one reader, and then rendered inside a container
     * (which loads the carrier for the request as {@code $dotContentMap}) by a reader who can read
     * the restricted reference and by an anonymous reader, in both orders.
     * Expected result: each reader gets the variable resolved for them: the reader with READ gets
     * both references hydrated, the anonymous reader only the public one.
     */
    @Test
    public void carrierRenderedInAContainer_givesEachReaderTheirOwnStoryBlock() throws Exception {
        for (final List<User> order : List.of(List.of(readerWithRead, anonymous()),
                List.of(anonymous(), readerWithRead))) {
            final String code = buildCarrierCodeAs(order.get(0));
            for (final User reader : order) {
                final Map<String, Map<String, Object>> references = referencesIn(renderStoryBlockVariableAs(reader, code));

                assertEquals(publicTarget.getTitle(), references.get(publicTarget.getIdentifier()).get("title"));
                if (reader.isAnonymousUser()) {
                    assertOnlyReferenceKeys(references.get(restrictedTarget.getIdentifier()));
                } else {
                    assertEquals(restrictedTarget.getTitle(),
                            references.get(restrictedTarget.getIdentifier()).get("title"));
                }
            }
        }
    }

    /**
     * Method to test: {@code ESContentFactoryImpl#find(String, boolean)} with
     * {@code ignoreStoryBlock = true}.
     * Given scenario: the carrier is loaded from the database, which caches it, and then found
     * again.
     * Expected result: the second call returns the cached instance instead of loading the carrier
     * from the database again.
     */
    @Test
    public void findIgnoringStoryBlock_onACachedCarrier_returnsTheCachedInstance() throws Exception {
        final ESContentFactoryImpl factory = (ESContentFactoryImpl) FactoryLocator.getContentletFactory();
        CacheLocator.getContentletCache().remove(carrier.getInode());

        final Contentlet loaded = factory.find(carrier.getInode(), true);
        final Contentlet found = factory.find(carrier.getInode(), true);

        assertSame(loaded, found);
        assertSame(CacheLocator.getContentletCache().get(carrier.getInode()), found);
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#checkin}
     * with {@link StoryBlockAPI#restoreReferenceData(Object, Object)}.
     * Given scenario: a back-end user without READ on the restricted contentlet opens a carrier
     * (getting that reference with only its keys) and saves it back as it got it.
     * Expected result: the saved carrier keeps the data stored with the restricted reference, so
     * a reader with READ still gets it when references are not refreshed
     * ({@code REFRESH_BLOCK_EDITOR_REFERENCES=false}).
     */
    @Test
    public void savingTheStoryBlockAsReceivedByAReaderWithoutRead_keepsTheStoredReferenceData() throws Exception {
        final Contentlet editedCarrier = ContentletDataGen.publish(new ContentletDataGen(carrierType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty(storyBlockField.variable(), carrier.get(storyBlockField.variable()))
                .nextPersisted());
        for (final User reader : List.of(readerWithRead, readerWithoutRead)) {
            PermissionUtilTest.addPermission(editedCarrier, reader,
                    PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        }
        final boolean previous = Config.getBooleanProperty("REFRESH_BLOCK_EDITOR_REFERENCES", true);
        try {
            final Object asReceived = asReader(readerWithoutRead, () -> APILocator.getContentletAPI()
                    .find(editedCarrier.getInode(), readerWithoutRead, false)).get(storyBlockField.variable());
            assertOnlyReferenceKeys(referencesIn(asReceived).get(restrictedTarget.getIdentifier()));

            final Contentlet checkout = ContentletDataGen.checkout(editedCarrier);
            checkout.setProperty(storyBlockField.variable(), asReceived);
            final Contentlet saved = ContentletDataGen.checkin(checkout);

            final Contentlet stored = FactoryLocator.getContentletFactory().findInDb(saved.getInode(), true)
                    .orElseThrow(() -> new AssertionError("the saved carrier must be in the database"));
            final Map<String, Map<String, Object>> storedReferences = referencesIn(stored.get(storyBlockField.variable()));
            assertEquals(restrictedTarget.getTitle(), storedReferences.get(restrictedTarget.getIdentifier()).get("title"));
            assertEquals(publicTarget.getTitle(), storedReferences.get(publicTarget.getIdentifier()).get("title"));

            Config.setProperty("REFRESH_BLOCK_EDITOR_REFERENCES", false);
            final Contentlet adminView = asReader(readerWithRead, () -> APILocator.getContentletAPI()
                    .find(saved.getInode(), readerWithRead, false));
            assertEquals(restrictedTarget.getTitle(), referencesIn(adminView.get(storyBlockField.variable()))
                    .get(restrictedTarget.getIdentifier()).get("title"));
        } finally {
            Config.setProperty("REFRESH_BLOCK_EDITOR_REFERENCES", previous);
            ContentletDataGen.destroy(editedCarrier);
        }
    }

    /**
     * Method to test: {@link WorkflowResource#fireActionDefaultSinglePart}.
     * Given scenario: a back-end user who can edit a carrier but cannot read the restricted
     * contentlet saves the Story Block as they received it (that reference with only its keys).
     * Expected result: the carrier is saved with the restricted reference's stored data, and the
     * contentlet returned to the user carries that reference with only its keys.
     */
    @Test
    public void savingTheStoryBlockAsReceived_returnsToTheEditorOnlyTheReferencesTheyCanRead() throws Exception {
        final String password = "editor-" + System.currentTimeMillis();
        final User editor = new UserDataGen().active(true).password(password)
                .roles(TestUserUtils.getBackendRole()).nextPersisted();
        final Contentlet editedCarrier = ContentletDataGen.publish(new ContentletDataGen(carrierType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty(storyBlockField.variable(), carrier.get(storyBlockField.variable()))
                .nextPersisted());
        final int edit = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH;
        final String editorRole = APILocator.getRoleAPI().getUserRole(editor).getId();
        APILocator.getPermissionAPI().save(List.of(new Permission(carrierType.getPermissionId(), editorRole, edit, true)),
                carrierType, APILocator.systemUser(), false);
        PermissionUtilTest.addPermission(editedCarrier, editor, PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, edit);
        PermissionUtilTest.addPermission(publicTarget, editor,
                PermissionAPI.INDIVIDUAL_PERMISSION_TYPE, PermissionAPI.PERMISSION_READ);
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        try {
            final Object asReceived = asReader(editor, () -> APILocator.getContentletAPI()
                    .find(editedCarrier.getInode(), editor, false)).get(storyBlockField.variable());
            assertOnlyReferenceKeys(referencesIn(asReceived).get(restrictedTarget.getIdentifier()));

            final HttpServletRequest request = new MockHeaderRequest(new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                            .request()).request());
            ((MockHeaderRequest) request).setHeader("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString((editor.getEmailAddress() + ":" + password).getBytes(StandardCharsets.UTF_8)));
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
            final FireActionForm form = new FireActionForm(new FireActionForm.Builder().contentlet(Map.of(
                    "identifier", editedCarrier.getIdentifier(),
                    "languageId", editedCarrier.getLanguageId(),
                    storyBlockField.variable(), asReceived)));
            final Response response = new WorkflowResource().fireActionDefaultSinglePart(request,
                    new EmptyHttpResponse(), null, editedCarrier.getIdentifier(), "WAIT_FOR",
                    String.valueOf(editedCarrier.getLanguageId()), "DEFAULT", SystemAction.EDIT, form);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            @SuppressWarnings("unchecked")
            final Map<String, Object> returned = (Map<String, Object>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
            final Map<String, Map<String, Object>> returnedReferences = referencesIn(returned.get(storyBlockField.variable()));
            assertOnlyReferenceKeys(returnedReferences.get(restrictedTarget.getIdentifier()));
            assertEquals(publicTarget.getTitle(), returnedReferences.get(publicTarget.getIdentifier()).get("title"));

            final Contentlet stored = FactoryLocator.getContentletFactory()
                    .findInDb(String.valueOf(returned.get("inode")), true)
                    .orElseThrow(() -> new AssertionError("the saved carrier must be in the database"));
            assertEquals(restrictedTarget.getTitle(), referencesIn(stored.get(storyBlockField.variable()))
                    .get(restrictedTarget.getIdentifier()).get("title"));

        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
            ContentletDataGen.destroy(editedCarrier);
            UserDataGen.remove(editor);
        }
    }

    // ----------------------------------------------------------------------------------- helpers

    /** The carrier in the shared cache still holds the restricted reference's stored data. */
    @SuppressWarnings("unchecked")
    private static void assertCachedRestrictedReferenceIsNotReduced() throws Exception {
        final Contentlet cached = CacheLocator.getContentletCache().get(carrier.getInode());
        assertNotNull("the carrier must be in the cache after a read", cached);
        final List<Map<String, Object>> content = (List<Map<String, Object>>) APILocator.getStoryBlockAPI()
                .toMap(cached.get(storyBlockField.variable())).get(StoryBlockAPI.CONTENT_KEY);
        for (final Map<String, Object> node : content) {
            final Map<String, Object> attrs = (Map<String, Object>) node.get(StoryBlockAPI.ATTRS_KEY);
            final Object data = null != attrs ? attrs.get(StoryBlockAPI.DATA_KEY) : null;
            if (data instanceof Map && restrictedTarget.getIdentifier()
                    .equals(((Map<String, Object>) data).get(StoryBlockAPI.IDENTIFIER_KEY))) {
                assertTrue("the cached carrier must not hold a reader-specific reduced reference",
                        ((Map<String, Object>) data).size() > 2);
                return;
            }
        }
        throw new AssertionError("the restricted reference is missing from the cached carrier");
    }

    /** A reference the reader cannot read keeps only the keys that identify it, and nothing else. */
    private static void assertOnlyReferenceKeys(final Map<String, Object> data) {
        assertOnlyReferenceKeys(data, restrictedTarget);
    }

    /** Same as {@link #assertOnlyReferenceKeys(Map)} for a reference to the given contentlet. */
    private static void assertOnlyReferenceKeys(final Map<String, Object> data, final Contentlet referenced) {
        assertNotNull("the reference node must still be in the document", data);
        assertEquals("only the keys identifying the reference may remain",
                java.util.Set.of(StoryBlockAPI.IDENTIFIER_KEY, StoryBlockAPI.LANGUAGE_ID_KEY), data.keySet());
        assertEquals(referenced.getIdentifier(), data.get(StoryBlockAPI.IDENTIFIER_KEY));
    }

    /**
     * Leaves the given contentlet with individual permissions where only the given role (besides
     * administrators) can read it, overriding whatever it inherited.
     */
    private static void grantReadOnlyTo(final Contentlet contentlet, final Role role) throws Exception {
        final Permission roleOnlyRead = new Permission(
                contentlet.getPermissionId(),
                role.getId(),
                PermissionAPI.PERMISSION_READ,
                true);
        APILocator.getPermissionAPI().save(roleOnlyRead, contentlet, APILocator.systemUser(), false);
    }

    private static User anonymous() throws Exception {
        return APILocator.getUserAPI().getAnonymousUser();
    }

    /**
     * Builds the cached Velocity code of the carrier (LIVE mode) with the given user set in the
     * current request, from the carrier that user gets.
     */
    private static String buildCarrierCodeAs(final User reader) throws Exception {
        final Contentlet found = findCarrierAs(reader, reader.isAnonymousUser(), true);
        return asReader(reader, () -> {
            try (final InputStream code = new ContentletLoader()
                    .buildVelocity(found, PageMode.LIVE, "storyBlockCarrier.content")) {
                return new String(code.readAllBytes(), StandardCharsets.UTF_8);
            }
        });
    }

    /**
     * Renders the given carrier code the way a container does, with the carrier loaded for the
     * given reader as {@code $dotContentMap}, and returns the Story Block variable it sets.
     */
    private static String renderStoryBlockVariableAs(final User reader, final String code) throws Exception {
        final Contentlet found = findCarrierAs(reader, reader.isAnonymousUser(), false);
        return asReader(reader, () -> {
            final Context context = VelocityUtil.getBasicContext();
            context.put("json", new JSONTool());
            context.put("contents", new ContentsWebAPI());
            context.put("dotContentMap", new ContentMap(found, reader, PageMode.LIVE,
                    APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false), context));
            return VelocityUtil.eval(code + "$" + storyBlockField.variable(), context).trim();
        });
    }

    /**
     * Reads the carrier as the given user, with that user set in the current request, and returns
     * the {@code attrs.data} map of every content reference in the Story Block field, keyed by the
     * referenced identifier as stored in the carrier.
     *
     * @param reader               The user reading the carrier; the anonymous user leaves the
     *                             request without a user.
     * @param respectFrontendRoles Passed to {@code ContentletAPI#find}.
     * @param evictFirst           When {@code true}, the carrier is removed from the cache first so
     *                             the read goes to the database.
     */
    private static Map<String, Map<String, Object>> readCarrierAs(final User reader,
                                                                  final boolean respectFrontendRoles,
                                                                  final boolean evictFirst) throws Exception {
        return referencesIn(findCarrierAs(reader, respectFrontendRoles, evictFirst)
                .get(storyBlockField.variable()));
    }

    /**
     * Reads the carrier as the given user, with that user set in the current request, and returns
     * the Contentlet that user gets. See {@link #readCarrierAs(User, boolean, boolean)}.
     */
    private static Contentlet findCarrierAs(final User reader, final boolean respectFrontendRoles,
                                            final boolean evictFirst) throws Exception {
        return asReader(reader, () -> {
            if (evictFirst) {
                CacheLocator.getContentletCache().remove(carrier.getInode());
            }
            final Contentlet found = APILocator.getContentletAPI().find(carrier.getInode(), reader, respectFrontendRoles);
            assertNotNull("the carrier itself must be readable by the reader", found);
            return found;
        });
    }

    /**
     * Runs {@code read} with the given user set in the current request (no user for anonymous),
     * restoring the previous request and response afterwards.
     */
    private static <T> T asReader(final User reader, final java.util.concurrent.Callable<T> read) throws Exception {
        return asReader(reader, Map.of(), read);
    }

    /**
     * Same as {@link #asReader(User, java.util.concurrent.Callable)}, with extra attributes (such
     * as the page mode) set in the request.
     */
    private static <T> T asReader(final User reader, final Map<String, Object> attributes,
                                  final java.util.concurrent.Callable<T> read) throws Exception {
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse previousResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        try {
            final HttpServletRequest request = new MockAttributeRequest(mock(HttpServletRequest.class));
            if (!reader.isAnonymousUser()) {
                request.setAttribute(WebKeys.USER, reader);
                request.setAttribute(WebKeys.USER_ID, reader.getUserId());
            }
            attributes.forEach(request::setAttribute);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(mock(HttpServletResponse.class));
            return read.call();
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(previousResponse);
        }
    }

    /**
     * Returns the {@code attrs.data} map of every content reference in a Story Block value of the
     * carrier, keyed by the referenced identifier as stored in the carrier.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> referencesIn(final Object storyBlockValue) throws Exception {
        final List<Map<String, Object>> content = (List<Map<String, Object>>) APILocator.getStoryBlockAPI()
                .toMap(storyBlockValue).get(StoryBlockAPI.CONTENT_KEY);
        final Map<String, Map<String, Object>> byIdentifier = new java.util.HashMap<>();
        final List<String> expected = List.of(restrictedTarget.getIdentifier(), publicTarget.getIdentifier());
        int references = 0;
        for (final Map<String, Object> node : content) {
            if (!"dotContent".equals(node.get(StoryBlockAPI.TYPE_KEY))) {
                continue;
            }
            final Map<String, Object> data = (Map<String, Object>)
                    ((Map<String, Object>) node.get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY);
            // A cleared reference has no identifier left: match it by position in the document
            final String identifier = Optional.ofNullable(data.get(StoryBlockAPI.IDENTIFIER_KEY))
                    .map(Object::toString).orElse(expected.get(references));
            byIdentifier.put(identifier, data);
            references++;
        }
        assertTrue("both references must still be present in the document", references == 2);
        return byIdentifier;
    }

    /** The {@code attrs.data} map of every content reference in a Story Block value, in document order. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> referencesInOrder(final Object storyBlockValue) throws Exception {
        assertNotNull("the Story Block value must be present", storyBlockValue);
        final List<Map<String, Object>> content = (List<Map<String, Object>>) APILocator.getStoryBlockAPI()
                .toMap(storyBlockValue).get(StoryBlockAPI.CONTENT_KEY);
        final List<Map<String, Object>> references = new java.util.ArrayList<>();
        for (final Map<String, Object> node : content) {
            if ("dotContent".equals(node.get(StoryBlockAPI.TYPE_KEY))) {
                references.add((Map<String, Object>)
                        ((Map<String, Object>) node.get(StoryBlockAPI.ATTRS_KEY)).get(StoryBlockAPI.DATA_KEY));
            }
        }
        return references;
    }

    /** The {@code attrs.data} map of the only content reference in a Story Block value. */
    private static Map<String, Object> referenceDataIn(final Object storyBlockValue) throws Exception {
        final List<Map<String, Object>> references = referencesInOrder(storyBlockValue);
        assertEquals("the document must hold exactly one reference", 1, references.size());
        return references.get(0);
    }

    /** The related content loaded in a relationship field, whether one item or a list of them. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> relatedItems(final Object related) {
        assertNotNull("the related content must be loaded", related);
        return related instanceof Map ? List.of((Map<String, Object>) related) : (List<Map<String, Object>>) related;
    }

    /** The related item for the given contentlet; fails if it is missing. */
    private static Map<String, Object> itemOf(final Contentlet contentlet, final List<Map<String, Object>> items) {
        return items.stream()
                .filter(item -> contentlet.getIdentifier().equals(item.get(StoryBlockAPI.IDENTIFIER_KEY)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing related item " + contentlet.getIdentifier()));
    }

    /** The given contentlet is either absent from the related items or present with its keys only. */
    private static void assertNoDataOf(final Contentlet contentlet, final List<Map<String, Object>> items) {
        items.stream()
                .filter(item -> contentlet.getIdentifier().equals(item.get(StoryBlockAPI.IDENTIFIER_KEY)))
                .forEach(item -> assertTrue("a related item the reader cannot read keeps only its keys, was: "
                                + item.keySet(),
                        java.util.Set.of(StoryBlockAPI.IDENTIFIER_KEY, StoryBlockAPI.LANGUAGE_ID_KEY)
                                .containsAll(item.keySet())));
    }

    /** Relationship records relating {@code parent} to {@code related} through {@code relationshipField}. */
    private static ContentletRelationships relate(final Field relationshipField, final Contentlet parent,
                                                  final Contentlet... related) throws Exception {
        final Relationship relationship = APILocator.getRelationshipAPI()
                .getRelationshipFromField(relationshipField, APILocator.systemUser());
        final ContentletRelationships relationships = new ContentletRelationships(parent);
        final ContentletRelationships.ContentletRelationshipRecords records =
                relationships.new ContentletRelationshipRecords(relationship, true);
        records.setRecords(Arrays.asList(related));
        relationships.getRelationshipsRecords().add(records);
        return relationships;
    }
}
