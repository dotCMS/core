package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfigBuilder;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.org.directwebremoting.guice.ApplicationScoped;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.FireActionForm;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionCache;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.reindex.ReindexQueueAPI;
import com.dotmarketing.common.reindex.ReindexQueueAPIImpl;
import com.dotmarketing.common.reindex.ReindexThread;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI.SystemAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import com.dotcms.test.util.ApiTokenTestUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import org.junit.runner.RunWith;

/**
 * Test for the {@link ContentTypeInitializer}
 * @author jsanca
 */
public class ContentTypeInitializerTest extends IntegrationTestBase {

    @ClassRule
    public static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#search(String, int, int, String, User, boolean)}
     * Given Scenario: Creates a few favorite pages
     * ExpectedResult: On search, since favorite pages are system content, they should not be returned
     *
     */
    @Test
    public void test_system_field() throws Exception {

        final ContentType contentType = Try.of(()-> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();
        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();
        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();

        final StringBuffer luceneQuery = new StringBuffer();
        luceneQuery.append("+contentType:"+contentType.variable() + " ");
        luceneQuery.append("+" + ESMappingConstants.SYSTEM_TYPE + ":false ");
        final List<Contentlet> contentlets = APILocator.getContentletAPI().search(luceneQuery.toString(),
                0, 10, null, APILocator.systemUser(), false);

        Assert.assertNotNull("The contentlets shouldn't be null", contentlets);
        Assert.assertTrue("Should not return any contentlet since they are system contentlets",contentlets.isEmpty());
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: If the content type exists is being deleted, and try the initializer to see if works
     * ExpectedResult: The content type should be created
     *
     */
    @Test
    public void test_content_type_init() throws Exception {
        // make sure its cached. see https://github.com/dotCMS/dotCMS/issues/2465
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        ContentType contentType = Try.of(()-> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        if (null != contentType) {
            contentTypeAPI.delete(contentType);
        }

        new ContentTypeInitializer().init();

        contentType = Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        Assert.assertNotNull("The content type dotFavoritePage shouldn't be null", contentType);

        Assert.assertTrue("CT : "+contentType, contentType.fieldMap().get("order").indexed());
        //we make sure the url field isn't unique
        Assert.assertFalse("CT : "+contentType,contentType.fieldMap().get("url").unique());

        //we make sure there is no content type using the legacy name
        Assert.assertNull(Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.LEGACY_FAVORITE_PAGE_VAR_NAME)).getOrNull());

        checkPermissionsOnContentlet(contentType);
    }

    /** USE, EDIT, PUBLISH and EDIT_PERMISSIONS: the bits the initializer gives CMS Owner. */
    private static final int OWNER_BITS = PermissionAPI.PERMISSION_USE | PermissionAPI.PERMISSION_EDIT
            | PermissionAPI.PERMISSION_PUBLISH | PermissionAPI.PERMISSION_EDIT_PERMISSIONS;

    /**
     * Method to test: {@link ContentTypeInitializer#init()} and saving a favorite page through
     * {@link WorkflowResource#fireActionDefaultSinglePart}, the call the Favorite Page dialog makes.
     * Given Scenario: The favorite page content type is in the state the starter ships: an
     * individual CMS Owner row and no row its contentlets inherit. A back-end user saves one
     * favorite before the initializer runs and one after, without a permission list.
     * ExpectedResult: After the initializer runs, both favorites can be read by their owner only:
     * not anonymously, not by a front-end user and not by another back-end user.
     */
    @Test
    public void test_favorite_pages_are_readable_only_by_their_owner() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.save(new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                contentType, APILocator.systemUser(), false);

        final User owner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User otherBackEndUser = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User frontEndUser = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();
        final User anonymous = APILocator.getUserAPI().getAnonymousUser();
        final List<Contentlet> favorites = new ArrayList<>();
        try {

            final Contentlet savedBefore = saveFavoriteAs(owner);
            favorites.add(savedBefore);
            Assert.assertTrue("In the starter state a favorite inherits anonymous READ from the System Host,"
                            + " which is the state the initializer has to repair",
                    permissionAPI.doesUserHavePermission(savedBefore, PermissionAPI.PERMISSION_READ, anonymous, true));

            // The check above cached its answer; the initializer must drop it when it repairs.
            new ContentTypeInitializer().init();

            final Contentlet savedAfter = saveFavoriteAs(owner);
            favorites.add(savedAfter);
            for (final Contentlet favorite : favorites) {

                Assert.assertTrue("The owner must read the favorite",
                        permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ, owner, true));
                for (final User reader : List.of(anonymous, frontEndUser, otherBackEndUser)) {

                    if (permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ, reader, true)) {

                        Assert.fail(reader.getUserId() + " must not read a favorite owned by someone else: "
                                + describePermissions(favorite, favorite == savedBefore ? "saved before init" : "saved after init"));
                    }
                }
            }

            final String query = "+contentType:" + ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME
                    + " +identifier:" + savedAfter.getIdentifier();
            Assert.assertEquals("The owner must find the favorite in a content search", 1,
                    APILocator.getContentletAPI().search(query, 10, 0, null, owner, true).size());
            for (final User reader : List.of(anonymous, frontEndUser, otherBackEndUser)) {

                Assert.assertTrue(reader.getUserId() + " must not find a favorite owned by someone else",
                        APILocator.getContentletAPI().search(query, 10, 0, null, reader, true).isEmpty());
            }

            contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
            final String cmsOwnerId = cmsOwner.getId();
            Assert.assertTrue("The favorite page content type must pass CMS Owner to its contentlets",
                    permissionAPI.getInheritablePermissions(contentType, true).stream().anyMatch(permission ->
                            Contentlet.class.getCanonicalName().equals(permission.getType())
                                    && cmsOwnerId.equals(permission.getRoleId())
                                    && (permission.getPermission() & OWNER_BITS) == OWNER_BITS));
        } finally {

            favorites.forEach(ContentletDataGen::destroy);
            UserDataGen.remove(owner);
            UserDataGen.remove(otherBackEndUser);
            UserDataGen.remove(frontEndUser);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorite page content type already has both CMS Owner rows, plus a READ
     * row for CMS Anonymous that its contentlets inherit.
     * ExpectedResult: The initializer replaces the type's rows, so the anonymous row is gone and a
     * favorite saved afterwards cannot be read anonymously.
     */
    @Test
    public void test_init_removes_rows_of_other_roles_from_favorite_page_type() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        anonymousRole.getId(), PermissionAPI.PERMISSION_READ, true)),
                contentType, APILocator.systemUser(), false);

        new ContentTypeInitializer().init();

        final String anonymousRoleId = anonymousRole.getId();
        Assert.assertTrue("The favorite page content type must not pass CMS Anonymous to its contentlets",
                permissionAPI.getInheritablePermissions(contentType, true).stream()
                        .noneMatch(permission -> anonymousRoleId.equals(permission.getRoleId())));

        final User owner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        Contentlet favorite = null;
        try {

            favorite = saveFavoriteAs(owner);
            Assert.assertFalse("A favorite must not be readable anonymously",
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ,
                            APILocator.getUserAPI().getAnonymousUser(), true));
        } finally {

            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            UserDataGen.remove(owner);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorite page content type already has both CMS Owner rows, so the
     * favorites' own rows are not reset, plus a READ row for CMS Anonymous its contentlets inherit.
     * A favorite on the System Host is indexed while that row is there, so an anonymous search
     * finds it.
     * ExpectedResult: The repair still queues the favorite for reindex, so once the queue drains
     * an anonymous search no longer finds it.
     */
    @Test
    public void test_init_reindexes_favorites_when_removing_rows_of_other_roles() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ, true)),
                contentType, APILocator.systemUser(), false);

        final Contentlet favorite = new ContentletDataGen(contentType).host(APILocator.systemHost())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersistedAndPublish();
        try {

            final User anonymous = APILocator.getUserAPI().getAnonymousUser();
            final String query = "+contentType:" + ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME
                    + " +identifier:" + favorite.getIdentifier();
            Assert.assertEquals("Before the repair the favorite must be indexed as anonymously readable",
                    1, APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).size());

            new ContentTypeInitializer().init();

            final long deadline = System.currentTimeMillis() + 60_000;
            while (!APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).isEmpty()
                    && System.currentTimeMillis() < deadline) {
                DateUtil.sleep(1000);
            }
            Assert.assertTrue("After the repair an anonymous search must not find the favorite; reindex queue holds "
                            + APILocator.getReindexQueueAPI().recordsInQueue() + " record(s): "
                            + describePermissions(favorite, "favorite"),
                    APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).isEmpty());
        } finally {

            ContentletDataGen.destroy(favorite);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: As in the test above, a favorite is indexed while the type passes a CMS
     * Anonymous READ row to its contentlets. The reindex thread is held while the repair runs, and
     * right after it the favorite's old permissions are put back in the permission cache, as a
     * lookup that read them before the repair committed and cached them after the cache was
     * cleared would. The reindex then runs and indexes those old permissions.
     * ExpectedResult: The repair clears the cache and queues the favorites for reindex again a
     * short time later, so an anonymous search stops finding the favorite.
     */
    @Test
    public void test_init_reindexes_favorites_again_after_old_permissions_are_cached() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ, true)),
                contentType, APILocator.systemUser(), false);

        final Contentlet favorite = new ContentletDataGen(contentType).host(APILocator.systemHost())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersistedAndPublish();
        try {

            final User anonymous = APILocator.getUserAPI().getAnonymousUser();
            final String query = "+contentType:" + ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME
                    + " +identifier:" + favorite.getIdentifier();
            Assert.assertEquals("Before the repair the favorite must be indexed as anonymously readable",
                    1, APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).size());
            permissionAPI.getPermissions(favorite);
            final Method getCached = PermissionCache.class.getDeclaredMethod("getPermissionsFromCache", String.class);
            final Method putCached = PermissionCache.class.getDeclaredMethod("addToPermissionCache", String.class, List.class);
            getCached.setAccessible(true);
            putCached.setAccessible(true);
            final Object oldPermissions = getCached.invoke(CacheLocator.getPermissionCache(), favorite.getPermissionId());
            Assert.assertNotNull("Reading the favorite's permissions must cache them", oldPermissions);

            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            ReindexThread.pause();
            try {

                new ContentTypeInitializer().init();
                // the cache put is not public API, so a lookup's late put is made through reflection
                putCached.invoke(CacheLocator.getPermissionCache(), favorite.getPermissionId(), oldPermissions);
            } finally {

                ReindexThread.unpause();
                Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            }

            final long deadline = System.currentTimeMillis() + 90_000;
            while (!APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).isEmpty()
                    && System.currentTimeMillis() < deadline) {
                DateUtil.sleep(1000);
            }
            Assert.assertTrue("After the repair an anonymous search must not find the favorite; reindex queue holds "
                            + APILocator.getReindexQueueAPI().recordsInQueue() + " record(s): "
                            + describePermissions(favorite, "favorite"),
                    APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).isEmpty());
        } finally {

            ContentletDataGen.destroy(favorite);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: A favorite is indexed while the type passes a CMS Anonymous READ row to its
     * contentlets. That row is then deleted in the database without a reindex, so the type's rows
     * are correct and the index is stale, as when a repair committed and its node stopped before
     * the delayed cache clear and reindex ran. The pending cleanup key that repair wrote is still
     * there.
     * ExpectedResult: The next start finds the type correct and repairs nothing, but finishes the
     * pending cleanup: an anonymous search stops finding the favorite, and the key is deleted once
     * the delayed step has run.
     */
    @Test
    public void test_init_finishes_cleanup_left_pending_by_a_stopped_node() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        // a delayed step left by an earlier test would reindex the favorite on its own; wait for
        // the last one to delete the key first
        final long idleDeadline = System.currentTimeMillis() + 60_000;
        while (pendingCleanupKeyExists() && System.currentTimeMillis() < idleDeadline) {
            DateUtil.sleep(1000);
        }
        APILocator.getSystemAPI().getSystemTable()
                .set(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_RESET_KEY, "true");
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        anonymousRole.getId(), PermissionAPI.PERMISSION_READ, true)),
                contentType, APILocator.systemUser(), false);

        final Contentlet favorite = new ContentletDataGen(contentType).host(APILocator.systemHost())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersistedAndPublish();
        try {

            final User anonymous = APILocator.getUserAPI().getAnonymousUser();
            final String query = "+contentType:" + ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME
                    + " +identifier:" + favorite.getIdentifier();
            Assert.assertEquals("Before the cleanup the favorite must be indexed as anonymously readable",
                    1, APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).size());

            new DotConnect().executeUpdate("delete from permission where inode_id = ? and roleid = ?",
                    contentType.getPermissionId(), anonymousRole.getId());
            CacheLocator.getPermissionCache().clearCache();
            APILocator.getSystemAPI().getSystemTable()
                    .set(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_CLEANUP_PENDING_KEY, "true");
            Assert.assertEquals("The stale index must still let an anonymous search find the favorite",
                    1, APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).size());

            new ContentTypeInitializer().init();

            final long deadline = System.currentTimeMillis() + 90_000;
            while ((!APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).isEmpty()
                    || pendingCleanupKeyExists()) && System.currentTimeMillis() < deadline) {
                DateUtil.sleep(1000);
            }
            Assert.assertTrue("After the start an anonymous search must not find the favorite; reindex queue holds "
                            + APILocator.getReindexQueueAPI().recordsInQueue() + " record(s): "
                            + describePermissions(favorite, "favorite"),
                    APILocator.getContentletAPI().search(query, 10, 0, null, anonymous, true).isEmpty());
            Assert.assertFalse("The pending cleanup key must be deleted once the delayed step has run",
                    pendingCleanupKeyExists());
        } finally {

            ContentletDataGen.destroy(favorite);
        }
    }

    private static boolean pendingCleanupKeyExists() throws DotDataException {

        return new DotConnect().setSQL("select count(*) as total from system_table where key = ?")
                .addParam(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_CLEANUP_PENDING_KEY)
                .getInt("total") > 0;
    }

    private static String pendingCleanupValue() throws DotDataException {

        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select value from system_table where key = ?")
                .addParam(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_CLEANUP_PENDING_KEY)
                .loadObjectResults();
        return rows.isEmpty() ? null : String.valueOf(rows.get(0).get("value"));
    }

    /**
     * Method to test: {@link ContentTypeInitializer#finishPermissionCleanup(ContentType, String)}
     * Given Scenario: A repair left the pending cleanup key, and the delayed step runs while
     * adding the favorites to the reindex queue fails.
     * ExpectedResult: The key stays, so the next start runs the step again. Once queuing works,
     * the step deletes it.
     */
    @Test
    public void test_delayed_cleanup_keeps_the_pending_key_when_the_favorites_cannot_be_queued() throws Exception {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        final String cleanupId = UUID.randomUUID().toString();
        try {

            APILocator.getSystemAPI().getSystemTable()
                    .set(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_CLEANUP_PENDING_KEY, cleanupId);
            final ReindexQueueAPI failingQueue = new ReindexQueueAPIImpl() {
                @Override
                public int addIdentifierReindex(final Collection<String> ids) throws DotDataException {
                    throw new DotDataException("queue unavailable");
                }
            };

            new ContentTypeInitializer(failingQueue).finishPermissionCleanup(contentType, cleanupId);
            Assert.assertEquals("The key must stay when the favorites could not be queued",
                    cleanupId, pendingCleanupValue());

            new ContentTypeInitializer().finishPermissionCleanup(contentType, cleanupId);
            Assert.assertNull("The key must be deleted once the favorites are queued", pendingCleanupValue());
        } finally {

            new DotConnect().executeUpdate("delete from system_table where key = ? and value = ?",
                    ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_CLEANUP_PENDING_KEY, cleanupId);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#finishPermissionCleanup(ContentType, String)}
     * Given Scenario: The delayed step of an older repair runs after a newer repair has written
     * its own value to the pending cleanup key.
     * ExpectedResult: The older step leaves the newer value, and the newer repair's own step
     * deletes it.
     */
    @Test
    public void test_delayed_cleanup_of_an_older_repair_keeps_a_newer_pending_key() throws Exception {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        final String olderId = UUID.randomUUID().toString();
        final String newerId = UUID.randomUUID().toString();
        try {

            APILocator.getSystemAPI().getSystemTable()
                    .set(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_CLEANUP_PENDING_KEY, newerId);

            new ContentTypeInitializer().finishPermissionCleanup(contentType, olderId);
            Assert.assertEquals("An older repair's step must leave a newer repair's key",
                    newerId, pendingCleanupValue());

            new ContentTypeInitializer().finishPermissionCleanup(contentType, newerId);
            Assert.assertNull("The newer repair's step must delete its own key", pendingCleanupValue());
        } finally {

            new DotConnect().executeUpdate("delete from system_table where key = ? and value = ?",
                    ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_CLEANUP_PENDING_KEY, newerId);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorite page content type already has its CMS Owner rows. A favorite is
     * given its own rows, sharing it with another back-end user, and the initializer runs again,
     * as on a later start.
     * ExpectedResult: The favorite keeps its own rows and the other user can still read it.
     */
    @Test
    public void test_init_keeps_own_rows_of_a_favorite_when_type_is_correct() throws Exception {

        new ContentTypeInitializer().init();
        assertSharedFavoriteSurvivesInit();
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorite page content type has both CMS Owner rows plus a READ row for
     * CMS Anonymous its contentlets inherit, and a favorite has its own rows sharing it with
     * another back-end user.
     * ExpectedResult: The initializer removes the anonymous row from the type but leaves the
     * favorite's own rows, so the other user can still read it.
     */
    @Test
    public void test_init_keeps_own_rows_of_a_favorite_when_repairing_type_rows() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        anonymousRole.getId(), PermissionAPI.PERMISSION_READ, true)),
                contentType, APILocator.systemUser(), false);

        assertSharedFavoriteSurvivesInit();

        final String anonymousRoleId = anonymousRole.getId();
        Assert.assertTrue("The favorite page content type must not pass CMS Anonymous to its contentlets",
                permissionAPI.getInheritablePermissions(contentType, true).stream()
                        .noneMatch(permission -> anonymousRoleId.equals(permission.getRoleId())));
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorite page content type is in the state the starter ships, with no
     * row its contentlets inherit, the favorites have not been reset yet, and an existing favorite
     * has its own rows granting CMS Anonymous READ, as favorites saved before the repair may have.
     * ExpectedResult: The first repair removes the favorite's own rows, so it inherits from the
     * type and cannot be read anonymously.
     */
    @Test
    public void test_first_init_resets_own_rows_of_existing_favorites() throws Exception {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        assertFirstInitResetsOwnRowsOfFavorite(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true)));
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorite page content type already has both CMS Owner rows, as the
     * previous initializer wrote them, but the favorites have not been reset yet, and an existing
     * favorite has its own rows granting CMS Anonymous READ.
     * ExpectedResult: The first repair still removes the favorite's own rows, so it cannot be read
     * anonymously.
     */
    @Test
    public void test_first_init_resets_own_rows_of_existing_favorites_when_type_rows_are_correct() throws Exception {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        assertFirstInitResetsOwnRowsOfFavorite(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true)));
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorites have not been reset yet, and a favorite saved on a site
     * through the API, not the dialog, has its own rows granting CMS Owner only. The site passes
     * CMS Anonymous READ to its contentlets, and a favorite on a site inherits from the site
     * rather than from the content type.
     * ExpectedResult: The first repair leaves the favorite with its own CMS Owner rows, so it does
     * not start inheriting from the site and cannot be read anonymously.
     */
    @Test
    public void test_first_init_keeps_a_favorite_on_a_site_readable_only_by_its_owner() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true)),
                contentType, APILocator.systemUser(), false);
        APILocator.getSystemAPI().getSystemTable().delete(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_RESET_KEY);

        final Host site = new SiteDataGen().nextPersisted();
        Contentlet favorite = null;
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                            APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                            PermissionAPI.PERMISSION_READ, true)),
                    site, APILocator.systemUser(), false);
            favorite = moveToSite(new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                    .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                    .nextPersisted(), site);
            permissionAPI.assignPermissions(List.of(
                    new Permission(favorite.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true)),
                    favorite, APILocator.systemUser(), false);

            new ContentTypeInitializer().init();

            final String cmsOwnerId = cmsOwner.getId();
            Assert.assertTrue("A favorite on a site must keep its own CMS Owner rows: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.getPermissions(favorite, true, true).stream()
                            .anyMatch(permission -> cmsOwnerId.equals(permission.getRoleId())));
            Assert.assertFalse("A favorite on a site must not be readable anonymously: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ,
                            APILocator.getUserAPI().getAnonymousUser(), true));
        } finally {

            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorites have not been reset yet, a site gives a locked role READ on
     * its content, and a favorite is saved on that site.
     * ExpectedResult: The first repair gives the favorite its own CMS Owner rows and no row of the
     * locked role, so that role cannot read it.
     */
    @Test
    public void test_first_init_gives_a_favorite_on_a_site_no_rows_of_a_locked_role() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final ContentType contentType = prepareFirstInit();
        final Role lockedRole = new RoleDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        Contentlet favorite = null;
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                            lockedRole.getId(), PermissionAPI.PERMISSION_READ, true)),
                    site, APILocator.systemUser(), false);
            APILocator.getRoleAPI().lock(lockedRole);
            final Contentlet saved = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                    .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                    .nextPersisted();

            // moving the favorite queues it for reindex, and a reindex that reads its permissions
            // while it inherits from the site writes a reference to the site; one landing after
            // the repair deleted the references is covered by
            // test_permission_cleanup_deletes_a_late_reference_of_a_favorite_to_its_site, so keep
            // reindexing paused until the repair has committed
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            ReindexThread.pause();
            try {

                favorite = moveToSite(saved, site);
                new ContentTypeInitializer().init();
            } finally {

                ReindexThread.unpause();
                Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            }

            assertOnlyCMSOwnerRows(favorite);
            Assert.assertFalse("The locked role must not read the favorite: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.doesRoleHavePermission(favorite, PermissionAPI.PERMISSION_READ, lockedRole));
        } finally {

            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            APILocator.getRoleAPI().unLock(lockedRole);
            RoleDataGen.remove(lockedRole, true);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#finishPermissionCleanup(ContentType, String)}
     * Given Scenario: A site gives a locked role READ on its content and the first repair gave a
     * favorite on that site its own CMS Owner rows. A lookup that read the favorite while it still
     * inherited from the site then writes its reference to the site after the repair, as one that
     * started before the repair committed can.
     * ExpectedResult: The cleanup step deletes that reference, so the favorite has only its own
     * rows and the locked role cannot read it.
     */
    @Test
    public void test_permission_cleanup_deletes_a_late_reference_of_a_favorite_to_its_site() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final ContentType contentType = prepareFirstInit();
        final Role lockedRole = new RoleDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        Contentlet favorite = null;
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                            lockedRole.getId(), PermissionAPI.PERMISSION_READ, true)),
                    site, APILocator.systemUser(), false);
            APILocator.getRoleAPI().lock(lockedRole);
            final Contentlet saved = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                    .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                    .nextPersisted();
            // paused so the only reference to the site is the one written below
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            ReindexThread.pause();
            try {

                favorite = moveToSite(saved, site);
                new ContentTypeInitializer().init();
            } finally {

                ReindexThread.unpause();
                Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            }

            // what the late lookup leaves behind
            new DotConnect().executeUpdate("insert into permission_reference (id, asset_id, reference_id, permission_type)"
                            + " values (nextval('permission_reference_seq'), ?, ?, ?)",
                    favorite.getPermissionId(), site.getPermissionId(), Contentlet.class.getCanonicalName());
            CacheLocator.getPermissionCache().clearCache();
            Assert.assertTrue("The planted reference must give the locked role READ before the cleanup",
                    permissionAPI.doesRoleHavePermission(favorite, PermissionAPI.PERMISSION_READ, lockedRole));

            new ContentTypeInitializer().finishPermissionCleanup(contentType, "not-the-pending-value");

            Assert.assertEquals("The favorite must not reference its site: "
                            + describePermissions(favorite, "favorite"), 0,
                    new DotConnect().setSQL("select count(*) as total from permission_reference where asset_id = ?")
                            .addParam(favorite.getPermissionId()).getInt("total"));
            assertOnlyCMSOwnerRows(favorite);
            Assert.assertFalse("The locked role must not read the favorite: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.doesRoleHavePermission(favorite, PermissionAPI.PERMISSION_READ, lockedRole));
        } finally {

            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            APILocator.getRoleAPI().unLock(lockedRole);
            RoleDataGen.remove(lockedRole, true);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorites have not been reset yet, and a favorite saved on a site has
     * versions only in a variant other than DEFAULT.
     * ExpectedResult: The first repair still gives the favorite its own CMS Owner rows.
     */
    @Test
    public void test_first_init_gives_owner_rows_to_a_favorite_only_in_another_variant() throws Exception {

        final ContentType contentType = prepareFirstInit();
        final Variant variant = new VariantDataGen().nextPersisted();
        final Host site = new SiteDataGen().nextPersisted();
        Contentlet favorite = null;
        try {

            favorite = moveToSite(new ContentletDataGen(contentType).variant(variant)
                    .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                    .nextPersisted(), site);

            new ContentTypeInitializer().init();

            assertOnlyCMSOwnerRows(favorite);
        } finally {

            if (null != favorite) {
                final Contentlet saved = favorite;
                Try.run(() -> ContentletDataGen.destroy(saved));
            }
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorites have not been reset yet, a site gives CMS Anonymous READ on its
     * content, a favorite is saved on that site, permission references are written in the
     * background as by default, and the worker that writes them is busy while the repair runs,
     * so anything the repair queues there is written only after it commits.
     * ExpectedResult: Once that worker has caught up, the favorite has no permission reference,
     * only its own CMS Owner rows, and cannot be read anonymously.
     */
    @Test
    public void test_first_init_leaves_a_favorite_on_a_site_no_reference_to_the_site() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final ContentType contentType = prepareFirstInit();
        final Host site = new SiteDataGen().nextPersisted();
        // the same name and settings PermissionBitFactoryImpl uses, so this is its submitter
        final DotSubmitter referenceWriter = DotConcurrentFactory.getInstance().getSubmitter("permissionreferences",
                new SubmitterConfigBuilder().poolSize(1).maxPoolSize(5).queueCapacity(10000).build());
        final CountDownLatch writerBusy = new CountDownLatch(1);
        final CountDownLatch releaseWriter = new CountDownLatch(1);
        // the test configuration writes references inline; the default is in the background
        final boolean referencesAsync = Config.getBooleanProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", true);
        Contentlet favorite = null;
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                            APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                            PermissionAPI.PERMISSION_READ, true)),
                    site, APILocator.systemUser(), false);
            favorite = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                    .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                    .nextPersisted();

            // moving the favorite queues it for reindex, and a reindex reads its permissions; a
            // read while it inherits from the site queues a reference to the site behind the
            // blocked writer, which is not something the repair does, so keep reindexing paused
            // until the repair has committed
            Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", true);
            ReindexThread.pause();
            try {

                favorite = moveToSite(favorite, site);

                Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", true);
                referenceWriter.submit(() -> {
                    writerBusy.countDown();
                    Try.run(() -> releaseWriter.await(2, TimeUnit.MINUTES));
                });
                Assert.assertTrue("The permission reference writer did not start the blocking task",
                        writerBusy.await(1, TimeUnit.MINUTES));

                new ContentTypeInitializer().init();
            } finally {

                ReindexThread.unpause();
                Config.setProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false);
            }

            releaseWriter.countDown();
            referenceWriter.submit(() -> { }).get(1, TimeUnit.MINUTES);
            CacheLocator.getPermissionCache().clearCache();

            Assert.assertEquals("The favorite must not reference a parent for its permissions: "
                            + describePermissions(favorite, "favorite"), 0,
                    new DotConnect().setSQL("select count(*) as total from permission_reference where asset_id = ?")
                            .addParam(favorite.getPermissionId()).getInt("total"));
            assertOnlyCMSOwnerRows(favorite);
            Assert.assertFalse("A favorite on a site must not be readable anonymously: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ,
                            APILocator.getUserAPI().getAnonymousUser(), true));
        } finally {

            releaseWriter.countDown();
            Config.setProperty("PERMISSION_REFERENCES_UPDATE_ASYNC", referencesAsync);
            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: saving a favorite page through the contentlet API.
     * Given Scenario: The first repair has already run, a site gives CMS Anonymous READ on its
     * content, and a new favorite owned by a back-end user is saved on that site.
     * ExpectedResult: The favorite is kept on the System Host, so it inherits from its content
     * type: the owner can read and edit it, while another back-end user, a front-end user and an
     * anonymous reader can neither read it nor find it in a content search.
     */
    @Test
    public void test_favorite_saved_on_a_site_after_the_reset_is_readable_only_by_its_owner() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Host site = new SiteDataGen().nextPersisted();
        final User owner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User otherBackEndUser = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User frontEndUser = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadFrontEndUserRole()).nextPersisted();
        Contentlet favorite = null;
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                            APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                            PermissionAPI.PERMISSION_READ, true)),
                    site, APILocator.systemUser(), false);
            favorite = new ContentletDataGen(contentType).host(site).setPolicy(IndexPolicy.WAIT_FOR)
                    .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                    .setProperty(Contentlet.OWNER_KEY, owner.getUserId())
                    .nextPersisted();

            Assert.assertTrue("The owner must read the favorite: " + describePermissions(favorite, "favorite"),
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ, owner, true));
            Assert.assertTrue("The owner must edit the favorite: " + describePermissions(favorite, "favorite"),
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_EDIT, owner, true));
            final String query = "+contentType:" + ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME
                    + " +identifier:" + favorite.getIdentifier();
            for (final User reader : List.of(APILocator.getUserAPI().getAnonymousUser(), frontEndUser, otherBackEndUser)) {

                Assert.assertFalse(reader.getUserId() + " must not read a favorite owned by someone else: "
                                + describePermissions(favorite, "favorite"),
                        permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ, reader, true));
                Assert.assertTrue(reader.getUserId() + " must not find a favorite owned by someone else",
                        APILocator.getContentletAPI().search(query, 10, 0, null, reader, true).isEmpty());
            }
        } finally {

            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            UserDataGen.remove(owner);
            UserDataGen.remove(otherBackEndUser);
            UserDataGen.remove(frontEndUser);
        }
    }

    /**
     * Method to test: moving a favorite page through the contentlet API.
     * Given Scenario: The first repair has already run, a site gives CMS Anonymous READ on its
     * content, and a favorite saved on the System Host is moved to that site.
     * ExpectedResult: The move is refused, the favorite stays on the System Host and it cannot be
     * read anonymously.
     */
    @Test
    public void test_favorite_cannot_be_moved_off_the_system_host() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Host site = new SiteDataGen().nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersisted();
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                            APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                            PermissionAPI.PERMISSION_READ, true)),
                    site, APILocator.systemUser(), false);

            Assert.assertThrows(IllegalArgumentException.class, () -> APILocator.getContentletAPI()
                    .move(favorite, APILocator.systemUser(), "//" + site.getHostname() + "/", false));

            CacheLocator.getPermissionCache().clearCache();
            Assert.assertEquals("The favorite must stay on the System Host", Host.SYSTEM_HOST,
                    APILocator.getIdentifierAPI().loadFromDb(favorite.getIdentifier()).getHostId());
            Assert.assertFalse("A favorite must not be readable anonymously: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ,
                            APILocator.getUserAPI().getAnonymousUser(), true));
        } finally {

            ContentletDataGen.destroy(favorite);
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: moving a favorite page by path through the contentlet API.
     * Given Scenario: A favorite saved at the System Host root is moved to a folder path on the
     * System Host that does not exist yet.
     * ExpectedResult: The move is refused before the folder is created, so no folder is left
     * behind and the favorite stays at the System Host root.
     */
    @Test
    public void test_favorite_cannot_be_moved_to_a_new_folder_on_the_system_host() throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Host systemHost = APILocator.systemHost();
        final String folderPath = "/favorite-folder-" + System.nanoTime() + "/";
        final Contentlet favorite = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersisted();
        try {

            Assert.assertThrows(IllegalArgumentException.class, () -> APILocator.getContentletAPI()
                    .move(favorite, APILocator.systemUser(), systemHost, folderPath, false));

            final Folder folder = Try.of(() -> APILocator.getFolderAPI().findFolderByPath(
                    folderPath, systemHost, APILocator.systemUser(), false)).getOrNull();
            Assert.assertTrue("The refused move must not create the folder " + folderPath,
                    null == folder || !UtilMethods.isSet(folder.getInode()));
            Assert.assertEquals("The favorite must stay at the System Host root", "/",
                    APILocator.getIdentifierAPI().loadFromDb(favorite.getIdentifier()).getParentPath());
        } finally {

            ContentletDataGen.destroy(favorite);
        }
    }

    /**
     * Method to test: moving a favorite page into a folder through the contentlet API.
     * Given Scenario: A folder on the System Host gives CMS Anonymous READ on its content, and a
     * favorite saved at the System Host root is moved into it.
     * ExpectedResult: The move is refused, the favorite stays at the System Host root and it
     * cannot be read anonymously.
     */
    @Test
    public void test_favorite_cannot_be_moved_to_a_folder_on_the_system_host() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Host systemHost = APILocator.systemHost();
        final Folder folder = new FolderDataGen().site(systemHost).nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersisted();
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), folder.getPermissionId(),
                            APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                            PermissionAPI.PERMISSION_READ, true)),
                    folder, APILocator.systemUser(), false);

            Assert.assertThrows(IllegalArgumentException.class, () -> APILocator.getContentletAPI()
                    .move(favorite, APILocator.systemUser(), systemHost, folder, false));

            CacheLocator.getPermissionCache().clearCache();
            Assert.assertEquals("The favorite must stay at the System Host root", "/",
                    APILocator.getIdentifierAPI().loadFromDb(favorite.getIdentifier()).getParentPath());
            Assert.assertFalse("A favorite must not be readable anonymously: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ,
                            APILocator.getUserAPI().getAnonymousUser(), true));
        } finally {

            ContentletDataGen.destroy(favorite);
            FolderDataGen.remove(folder);
        }
    }

    /**
     * Method to test: saving a favorite page through the contentlet API as a back-end user.
     * Given Scenario: A back-end user with no permission on a site saves a new favorite that names
     * that site.
     * ExpectedResult: The save succeeds and the favorite lands at the System Host root, because
     * the site it names is never where it is saved, so it is not checked as a destination.
     */
    @Test
    public void test_back_end_user_saves_a_favorite_naming_a_site_they_cannot_add_to() throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Host site = new SiteDataGen().nextPersisted();
        final User owner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        Contentlet favorite = null;
        try {

            final Contentlet unsaved = new ContentletDataGen(contentType).host(site)
                    .setPolicy(IndexPolicy.WAIT_FOR).setProperty("title", "favorite")
                    .setProperty("url", "/favorite-" + System.nanoTime()).next();
            favorite = APILocator.getContentletAPI().checkin(unsaved, owner, false);

            Assert.assertEquals("The favorite must be saved on the System Host", Host.SYSTEM_HOST,
                    APILocator.getIdentifierAPI().loadFromDb(favorite.getIdentifier()).getHostId());
            Assert.assertEquals("The favorite must be owned by the user who saved it",
                    owner.getUserId(), favorite.getOwner());
        } finally {

            if (null != favorite) {
                ContentletDataGen.destroy(favorite);
            }
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            UserDataGen.remove(owner);
        }
    }

    /**
     * Method to test: saving an existing favorite page through the contentlet API.
     * Given Scenario: A favorite has versions only in a variant other than DEFAULT, and its owner
     * saves an edit to it in that variant.
     * ExpectedResult: The save succeeds, because the stored favorite is found in that variant and
     * its owner has WRITE on it.
     */
    @Test
    public void test_owner_can_edit_a_favorite_only_in_another_variant() throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Variant variant = new VariantDataGen().nextPersisted();
        final User owner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType).variant(variant)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .setProperty(Contentlet.OWNER_KEY, owner.getUserId())
                .nextPersisted();
        try {

            final Contentlet edit = APILocator.getContentletAPI()
                    .checkout(favorite.getInode(), APILocator.systemUser(), false);
            edit.setVariantId(variant.name());
            edit.setProperty("title", "edited");
            // checkout locked it for the system user; unlock so only the permission check can refuse
            APILocator.getContentletAPI().unlock(favorite, APILocator.systemUser(), false);

            final Contentlet saved = APILocator.getContentletAPI().checkin(edit, owner, false);

            Assert.assertEquals("The owner's edit must be saved", "edited",
                    saved.getStringProperty("title"));
        } finally {

            Try.run(() -> ContentletDataGen.destroy(favorite));
            UserDataGen.remove(owner);
        }
    }

    /**
     * Method to test: saving an existing favorite page through the contentlet API.
     * Given Scenario: A favorite's DEFAULT version is owned by one user and its version in another
     * variant by a second user, and the second user saves an edit to the version they own.
     * ExpectedResult: The save succeeds, because the edited version is authorized against its own
     * stored version and owner, not against the DEFAULT version's owner.
     */
    @Test
    public void test_variant_owner_can_edit_their_version_of_a_favorite() throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Variant variant = new VariantDataGen().nextPersisted();
        final User defaultOwner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User variantOwner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .setProperty(Contentlet.OWNER_KEY, defaultOwner.getUserId())
                .nextPersisted();
        try {

            final Contentlet variantVersion = ContentletDataGen.createNewVersion(favorite, variant,
                    Map.of(Contentlet.OWNER_KEY, variantOwner.getUserId()));
            Assert.assertEquals("The variant version must be owned by the second user",
                    variantOwner.getUserId(), variantVersion.getOwner());

            final Contentlet edit = APILocator.getContentletAPI()
                    .checkout(variantVersion.getInode(), APILocator.systemUser(), false);
            edit.setVariantId(variant.name());
            edit.setProperty("title", "edited");
            // the checkouts locked it for the system user, and checkin reads the lock from the
            // DEFAULT version; unlock both so only the permission check can refuse
            APILocator.getContentletAPI().unlock(variantVersion, APILocator.systemUser(), false);
            APILocator.getContentletAPI().unlock(favorite, APILocator.systemUser(), false);

            final Contentlet saved = APILocator.getContentletAPI().checkin(edit, variantOwner, false);

            Assert.assertEquals("The variant owner's edit must be saved", "edited",
                    saved.getStringProperty("title"));
        } finally {

            Try.run(() -> ContentletDataGen.destroy(favorite));
            UserDataGen.remove(defaultOwner);
            UserDataGen.remove(variantOwner);
        }
    }

    /**
     * Method to test: saving an existing favorite page through the contentlet API.
     * Given Scenario: A favorite's only DEFAULT-variant version is in a second language and owned
     * by one user; its default-language version exists only in another variant and is owned by a
     * second user. The second user saves an edit to their version with no language set, which
     * checkin stores as the default-language version.
     * ExpectedResult: The save succeeds, because it is authorized against the default-language
     * version it will update, not against a version in another language found as a fallback.
     */
    @Test
    public void test_default_language_owner_can_edit_a_favorite_saved_without_a_language()
            throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Language secondLanguage = new LanguageDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();
        final User secondLanguageOwner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User defaultLanguageOwner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType)
                .languageId(secondLanguage.getId())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .setProperty(Contentlet.OWNER_KEY, secondLanguageOwner.getUserId())
                .nextPersisted();
        try {

            final Contentlet defaultLanguageVersion = ContentletDataGen.createNewVersion(favorite,
                    variant, APILocator.getLanguageAPI().getDefaultLanguage(),
                    Map.of(Contentlet.OWNER_KEY, defaultLanguageOwner.getUserId()));
            Assert.assertEquals("The default-language version must be owned by the second user",
                    defaultLanguageOwner.getUserId(), defaultLanguageVersion.getOwner());

            final Contentlet edit = APILocator.getContentletAPI()
                    .checkout(defaultLanguageVersion.getInode(), APILocator.systemUser(), false);
            edit.setVariantId(variant.name());
            edit.setLanguageId(0);
            edit.setProperty("title", "edited");
            // the checkouts locked both versions for the system user; unlock them so only the
            // permission check can refuse
            APILocator.getContentletAPI().unlock(defaultLanguageVersion, APILocator.systemUser(), false);
            APILocator.getContentletAPI().unlock(favorite, APILocator.systemUser(), false);

            final Contentlet saved = APILocator.getContentletAPI()
                    .checkin(edit, defaultLanguageOwner, false);

            Assert.assertEquals("The edit must be saved to the default language",
                    APILocator.getLanguageAPI().getDefaultLanguage().getId(), saved.getLanguageId());
            Assert.assertEquals("The default-language owner's edit must be saved", "edited",
                    saved.getStringProperty("title"));
        } finally {

            Try.run(() -> ContentletDataGen.destroy(favorite));
            UserDataGen.remove(secondLanguageOwner);
            UserDataGen.remove(defaultLanguageOwner);
            Try.run(() -> LanguageDataGen.remove(secondLanguage));
        }
    }

    /**
     * Method to test: saving an existing favorite page through the contentlet API.
     * Given Scenario: Another back-end user may edit all content of a site but has no permission on
     * a favorite at the System Host root. With no cached permissions and no permission reference
     * for the favorite, that user saves it naming the site.
     * ExpectedResult: The save is refused, because the favorite is authorized where it is stored,
     * not where the save names, and the stored favorite is left as it was.
     */
    @Test
    public void test_other_user_cannot_edit_a_favorite_by_naming_a_site_they_can_edit() throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Host site = new SiteDataGen().nextPersisted();
        final Role siteEditors = new RoleDataGen().nextPersisted();
        final User owner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User editor = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), siteEditors).nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .setProperty(Contentlet.OWNER_KEY, owner.getUserId())
                .nextPersisted();
        try {

            APILocator.getPermissionAPI().assignPermissions(List.of(
                    new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                            siteEditors.getId(),
                            PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE, true)),
                    site, APILocator.systemUser(), false);

            final Contentlet edit = APILocator.getContentletAPI()
                    .checkout(favorite.getInode(), APILocator.systemUser(), false);
            edit.setHost(site.getIdentifier());
            edit.setProperty("title", "edited");
            // checkout locked it for the system user; unlock so only the permission check can refuse
            APILocator.getContentletAPI().unlock(favorite, APILocator.systemUser(), false);
            APILocator.getPermissionAPI().resetPermissionReferences(favorite);
            CacheLocator.getPermissionCache().clearCache();

            final DotSecurityException refused = Assert.assertThrows(DotSecurityException.class,
                    () -> APILocator.getContentletAPI().checkin(edit, editor, false));
            Assert.assertTrue("The save must be refused for lack of WRITE on the favorite: "
                            + refused.getMessage(),
                    refused.getMessage().contains("doesn't have write permissions"));

            final Contentlet stored = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(favorite.getIdentifier());
            Assert.assertEquals("The stored favorite must keep its title", "favorite",
                    stored.getStringProperty("title"));
            Assert.assertEquals("The favorite must stay on the System Host", Host.SYSTEM_HOST,
                    APILocator.getIdentifierAPI().loadFromDb(favorite.getIdentifier()).getHostId());
        } finally {

            ContentletDataGen.destroy(favorite);
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            UserDataGen.remove(owner);
            UserDataGen.remove(editor);
        }
    }

    /**
     * Method to test: saving an existing favorite page in place, without a new version.
     * Given Scenario: Two back-end users each own a favorite. The second user saves their own
     * favorite in place, then saves it in place again carrying the inode of the first user's
     * favorite.
     * ExpectedResult: The first save succeeds; the second is refused, because an in-place save
     * overwrites the version with the given inode and is authorized against that version, and the
     * first user's favorite is left as it was.
     */
    @Test
    public void test_in_place_favorite_save_is_authorized_against_the_inode_it_updates()
            throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final User owner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User other = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final Contentlet ownersFavorite = new ContentletDataGen(contentType)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .setProperty(Contentlet.OWNER_KEY, owner.getUserId())
                .nextPersisted();
        final Contentlet othersFavorite = new ContentletDataGen(contentType)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "other").setProperty("url", "/favorite-" + System.nanoTime())
                .setProperty(Contentlet.OWNER_KEY, other.getUserId())
                .nextPersisted();
        try {

            final Contentlet ownEdit = APILocator.getContentletAPI()
                    .checkout(othersFavorite.getInode(), APILocator.systemUser(), false);
            ownEdit.setInode(othersFavorite.getInode());
            ownEdit.setProperty("title", "own edit");
            // checkout locked it for the system user; unlock so only the permission check can refuse
            APILocator.getContentletAPI().unlock(othersFavorite, APILocator.systemUser(), false);
            final Contentlet saved = APILocator.getContentletAPI().checkinWithoutVersioning(
                    ownEdit, (ContentletRelationships) null, null, null, other, false);
            Assert.assertEquals("The owner's in-place save must be applied", "own edit",
                    saved.getStringProperty("title"));

            final Contentlet edit = APILocator.getContentletAPI()
                    .checkout(othersFavorite.getInode(), APILocator.systemUser(), false);
            edit.setInode(ownersFavorite.getInode());
            edit.setProperty("title", "edited");
            APILocator.getContentletAPI().unlock(othersFavorite, APILocator.systemUser(), false);

            final DotSecurityException refused = Assert.assertThrows(DotSecurityException.class,
                    () -> APILocator.getContentletAPI().checkinWithoutVersioning(
                            edit, (ContentletRelationships) null, null, null, other, false));
            Assert.assertTrue("The save must be refused for lack of WRITE on the favorite: "
                            + refused.getMessage(),
                    refused.getMessage().contains("doesn't have write permissions"));

            final Contentlet stored = APILocator.getContentletAPI()
                    .find(ownersFavorite.getInode(), APILocator.systemUser(), false);
            Assert.assertEquals("The first user's favorite must keep its title", "favorite",
                    stored.getStringProperty("title"));
            Assert.assertEquals("The first user's favorite must keep its identifier",
                    ownersFavorite.getIdentifier(), stored.getIdentifier());
        } finally {

            Try.run(() -> ContentletDataGen.destroy(ownersFavorite));
            Try.run(() -> ContentletDataGen.destroy(othersFavorite));
            UserDataGen.remove(owner);
            UserDataGen.remove(other);
        }
    }

    /**
     * Method to test: saving an existing favorite page in place, without a new version.
     * Given Scenario: A favorite's DEFAULT version is owned by one user and its version in another
     * variant by a second user. The second user saves their version in place while naming the
     * DEFAULT variant.
     * ExpectedResult: The save is refused, because an in-place save rewrites the row's variant and
     * makes it the working version there, so it must keep the stored version's variant; the
     * DEFAULT working version stays the first user's.
     */
    @Test
    public void test_in_place_favorite_save_cannot_move_a_version_to_another_variant()
            throws Exception {

        final ContentType contentType = prepareFirstInit();
        new ContentTypeInitializer().init();

        final Variant variant = new VariantDataGen().nextPersisted();
        final User defaultOwner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final User variantOwner = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .setProperty(Contentlet.OWNER_KEY, defaultOwner.getUserId())
                .nextPersisted();
        try {

            final Contentlet variantVersion = ContentletDataGen.createNewVersion(favorite, variant,
                    Map.of(Contentlet.OWNER_KEY, variantOwner.getUserId()));

            final Contentlet edit = APILocator.getContentletAPI()
                    .checkout(variantVersion.getInode(), APILocator.systemUser(), false);
            edit.setInode(variantVersion.getInode());
            edit.setVariantId(VariantAPI.DEFAULT_VARIANT.name());
            edit.setProperty("title", "edited");
            // the checkouts locked both versions for the system user; unlock them so only the
            // permission check can refuse
            APILocator.getContentletAPI().unlock(variantVersion, APILocator.systemUser(), false);
            APILocator.getContentletAPI().unlock(favorite, APILocator.systemUser(), false);

            Assert.assertThrows(DotSecurityException.class,
                    () -> APILocator.getContentletAPI().checkinWithoutVersioning(
                            edit, (ContentletRelationships) null, null, null, variantOwner, false));

            final ContentletVersionInfo defaultInfo = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(favorite.getIdentifier(), favorite.getLanguageId(),
                            VariantAPI.DEFAULT_VARIANT.name()).orElseThrow();
            Assert.assertEquals("The DEFAULT working version must stay the first user's",
                    favorite.getInode(), defaultInfo.getWorkingInode());
            final Contentlet stored = APILocator.getContentletAPI()
                    .find(variantVersion.getInode(), APILocator.systemUser(), false);
            Assert.assertEquals("The variant version must keep its variant", variant.name(),
                    stored.getVariantId());
        } finally {

            Try.run(() -> ContentletDataGen.destroy(favorite));
            UserDataGen.remove(defaultOwner);
            UserDataGen.remove(variantOwner);
        }
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: The favorites were already reset by an earlier repair, the row the favorite
     * page contentlets inherit is missing one CMS Owner bit, and a favorite has its own rows
     * sharing it with another back-end user.
     * ExpectedResult: The initializer repairs the type rows but leaves the favorite's own rows, so
     * the other user can still read it.
     */
    @Test
    public void test_init_keeps_own_rows_of_a_favorite_when_an_owner_bit_is_missing() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS & ~PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true)),
                contentType, APILocator.systemUser(), false);

        assertSharedFavoriteSurvivesInit();

        final String cmsOwnerId = cmsOwner.getId();
        Assert.assertTrue("The favorite page content type must pass every CMS Owner bit to its contentlets",
                permissionAPI.getInheritablePermissions(contentType, true).stream().anyMatch(permission ->
                        Contentlet.class.getCanonicalName().equals(permission.getType())
                                && cmsOwnerId.equals(permission.getRoleId())
                                && (permission.getPermission() & OWNER_BITS) == OWNER_BITS));
    }

    /**
     * Gives the favorite page content type its correct CMS Owner rows and marks the favorites as
     * not reset yet, as on the first start after upgrading, and returns the type.
     */
    private static ContentType prepareFirstInit() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(List.of(
                new Permission(contentType.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                        cmsOwner.getId(), OWNER_BITS, true)),
                contentType, APILocator.systemUser(), false);
        APILocator.getSystemAPI().getSystemTable().delete(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_RESET_KEY);
        return contentType;
    }

    /**
     * Checks in the database that a favorite has its own CMS Owner row and no row of another role.
     */
    private static void assertOnlyCMSOwnerRows(final Contentlet favorite) throws DotDataException {

        final String cmsOwnerId = APILocator.getRoleAPI().loadCMSOwnerRole().getId();
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("select roleid from permission where inode_id = ?")
                .addParam(favorite.getPermissionId()).loadObjectResults();
        Assert.assertFalse("The favorite must have its own CMS Owner rows: "
                + describePermissions(favorite, "favorite"), rows.isEmpty());
        Assert.assertTrue("The favorite must have no row of another role: "
                        + describePermissions(favorite, "favorite"),
                rows.stream().allMatch(row -> cmsOwnerId.equals(row.get("roleid"))));
    }

    /**
     * Puts a favorite on a site the way favorites could be saved before they were kept on the
     * System Host: its identifier is moved to the site in the database, its cached copies are
     * dropped and its permission reference is reset, so it inherits from the site. Returns the
     * favorite as loaded again.
     */
    private static Contentlet moveToSite(final Contentlet favorite, final Host site) throws Exception {

        new DotConnect().executeUpdate("update identifier set host_inode = ? where id = ?",
                site.getIdentifier(), favorite.getIdentifier());
        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(favorite.getIdentifier());
        CacheLocator.getContentletCache().remove(favorite.getInode());
        final Contentlet moved = APILocator.getContentletAPI().find(favorite.getInode(), APILocator.systemUser(), false);
        Assert.assertEquals("The favorite must now be on the site", site.getIdentifier(), moved.getHost());
        APILocator.getPermissionAPI().resetPermissionReferences(moved);
        CacheLocator.getPermissionCache().clearCache();
        return moved;
    }

    /**
     * Gives the favorite page content type the given rows, marks the favorites as not reset yet,
     * saves a favorite on the System Host with its own rows granting CMS Anonymous READ, runs the
     * initializer and checks the favorite's own rows are gone and it cannot be read anonymously.
     */
    private static void assertFirstInitResetsOwnRowsOfFavorite(final List<Permission> typeRows) throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        permissionAPI.removePermissions(contentType);
        permissionAPI.assignPermissions(typeRows, contentType, APILocator.systemUser(), false);
        APILocator.getSystemAPI().getSystemTable().delete(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_RESET_KEY);

        // on the System Host, where the dialog saves favorites, so it inherits from the type;
        // on a site it would inherit from the site instead
        final Contentlet favorite = new ContentletDataGen(contentType).host(APILocator.systemHost())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersisted();
        try {

            permissionAPI.assignPermissions(List.of(
                    new Permission(favorite.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                    new Permission(favorite.getPermissionId(),
                            APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                            PermissionAPI.PERMISSION_READ, true)),
                    favorite, APILocator.systemUser(), false);

            new ContentTypeInitializer().init();

            Assert.assertTrue("The first repair must remove the favorite's own rows: "
                            + describePermissions(favorite, "favorite"),
                    permissionAPI.getPermissions(favorite, true, true).isEmpty());
            Assert.assertFalse("A favorite must not be readable anonymously",
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ,
                            APILocator.getUserAPI().getAnonymousUser(), true));
            Assert.assertTrue("The repair must record that the favorites were reset",
                    APILocator.getSystemAPI().getSystemTable()
                            .get(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_RESET_KEY).isPresent());
        } finally {

            ContentletDataGen.destroy(favorite);
        }
    }

    /**
     * Marks the favorites as already reset, as on any start after the first repair, saves a
     * favorite, gives it its own rows sharing it with another back-end user, runs the initializer
     * and checks the rows and the shared read survive.
     */
    private static void assertSharedFavoriteSurvivesInit() throws Exception {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final Role cmsOwner = APILocator.getRoleAPI().loadCMSOwnerRole();
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME);
        APILocator.getSystemAPI().getSystemTable().set(ContentTypeInitializer.FAVORITE_PAGE_PERMISSIONS_RESET_KEY, "true");
        final User sharedWith = new UserDataGen().active(true)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
        final Contentlet favorite = new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "favorite").setProperty("url", "/favorite-" + System.nanoTime())
                .nextPersisted();
        try {

            final String sharedRoleId = APILocator.getRoleAPI().getUserRole(sharedWith).getId();
            permissionAPI.assignPermissions(List.of(
                    new Permission(favorite.getPermissionId(), cmsOwner.getId(), OWNER_BITS, true),
                    new Permission(favorite.getPermissionId(), sharedRoleId, PermissionAPI.PERMISSION_READ, true)),
                    favorite, APILocator.systemUser(), false);

            new ContentTypeInitializer().init();

            Assert.assertTrue("The favorite must keep the row sharing it: " + describePermissions(favorite, "favorite"),
                    permissionAPI.getPermissions(favorite, true, true).stream()
                            .anyMatch(permission -> sharedRoleId.equals(permission.getRoleId())));
            Assert.assertTrue("The user the favorite is shared with must read it",
                    permissionAPI.doesUserHavePermission(favorite, PermissionAPI.PERMISSION_READ, sharedWith, true));
        } finally {

            ContentletDataGen.destroy(favorite);
            UserDataGen.remove(sharedWith);
        }
    }

    /**
     * Describes where a favorite's permissions come from, for a failure message: the favorite, the
     * permissions it resolves to, its own rows and its permission reference in the database.
     */
    private static String describePermissions(final Contentlet favorite, final String label)
            throws DotDataException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        return label + " " + favorite.getIdentifier()
                + ", resolved=" + permissionAPI.getPermissions(favorite)
                + ", own rows=" + new DotConnect().setSQL("select roleid, permission, permission_type from permission"
                        + " where inode_id = ?").addParam(favorite.getPermissionId()).loadObjectResults()
                + ", reference=" + new DotConnect().setSQL("select reference_id, permission_type from"
                        + " permission_reference where asset_id = ?").addParam(favorite.getPermissionId())
                        .loadObjectResults()
                + ", parent=" + Try.of(() -> favorite.getParentPermissionable().getPermissionId()).getOrNull();
    }

    /**
     * Publishes a new favorite page through {@link WorkflowResource#fireActionDefaultSinglePart}
     * with an API token for the given user and the fields the Favorite Page dialog sends, without
     * a permission list, and returns the saved contentlet.
     */
    @SuppressWarnings("unchecked")
    private static Contentlet saveFavoriteAs(final User user) throws Exception {

        final MockHeaderRequest request = new MockHeaderRequest(new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request()).request());
        request.setHeader("Authorization", TOKEN_UTILS.bearerHeader(user));
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        try {

            final FireActionForm form = new FireActionForm(new FireActionForm.Builder().contentlet(Map.of(
                    "contentType", ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME,
                    "title", "favorite",
                    "url", "/favorite-" + System.nanoTime() + "?language_id=1",
                    "order", 1)));
            final Response response = new WorkflowResource().fireActionDefaultSinglePart(request,
                    new EmptyHttpResponse(), null, null, "WAIT_FOR",
                    String.valueOf(APILocator.getLanguageAPI().getDefaultLanguage().getId()),
                    "DEFAULT", SystemAction.PUBLISH, form);
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            final Map<String, Object> saved =
                    (Map<String, Object>) ((ResponseEntityView<?>) response.getEntity()).getEntity();
            return APILocator.getContentletAPI().find(String.valueOf(saved.get("inode")),
                    APILocator.systemUser(), false);
        } finally {

            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

    private static void checkPermissionsOnContentlet(final ContentType contentType) throws DotDataException {
        final Role backendRole = APILocator.getRoleAPI().loadCMSOwnerRole();
        final List<Permission> permissions = APILocator.getPermissionAPI().getPermissions(contentType);
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_USE && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_EDIT  && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_PUBLISH  && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_EDIT_PERMISSIONS && p.getRoleId().equals(backendRole.getId())));
    }
}
