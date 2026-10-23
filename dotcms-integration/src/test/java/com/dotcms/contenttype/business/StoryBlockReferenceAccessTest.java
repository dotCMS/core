package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.PermissionUtilTest;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        assertNotNull("the reference node must still be in the document", data);
        assertEquals("only the keys identifying the reference may remain",
                java.util.Set.of(StoryBlockAPI.IDENTIFIER_KEY, StoryBlockAPI.LANGUAGE_ID_KEY), data.keySet());
        assertEquals(restrictedTarget.getIdentifier(), data.get(StoryBlockAPI.IDENTIFIER_KEY));
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
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> readCarrierAs(final User reader,
                                                                  final boolean respectFrontendRoles,
                                                                  final boolean evictFirst) throws Exception {
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse previousResponse = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        try {
            final HttpServletRequest request = new MockAttributeRequest(mock(HttpServletRequest.class));
            if (!reader.isAnonymousUser()) {
                request.setAttribute(WebKeys.USER, reader);
                request.setAttribute(WebKeys.USER_ID, reader.getUserId());
            }
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(mock(HttpServletResponse.class));
            if (evictFirst) {
                CacheLocator.getContentletCache().remove(carrier.getInode());
            }

            final Contentlet found = APILocator.getContentletAPI().find(carrier.getInode(), reader, respectFrontendRoles);
            assertNotNull("the carrier itself must be readable by the reader", found);
            final Object value = found.get(storyBlockField.variable());
            final List<Map<String, Object>> content =
                    (List<Map<String, Object>>) APILocator.getStoryBlockAPI().toMap(value).get(StoryBlockAPI.CONTENT_KEY);

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
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
            HttpServletResponseThreadLocal.INSTANCE.setResponse(previousResponse);
        }
    }
}
