package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Deleting a user must leave no cached Site, Contentlet or version-info object carrying the
 * deleted user's id, without a manual cache flush and without waiting for the asynchronous
 * reindex (issue #37116). Before the fix, Sites stayed stale in {@code HostCache} indefinitely
 * and Contentlets were only evicted when an index query happened to return them.
 */
public class UserDeletionCacheInvalidationTest {

    private static User systemUser;
    private static UserAPI userAPI;
    private static HostAPI hostAPI;
    private static ContentletAPI contentletAPI;
    private static VersionableAPI versionableAPI;

    private static Host site;
    private static ContentType contentType;
    private static Contentlet contentlet;
    private static User replacementUser;

    /**
     * Boots the integration test environment and resolves the user, site, content and
     * versionable APIs the test drives.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        userAPI = APILocator.getUserAPI();
        hostAPI = APILocator.getHostAPI();
        contentletAPI = APILocator.getContentletAPI();
        versionableAPI = APILocator.getVersionableAPI();
    }

    /**
     * Removes the content, type, site and replacement user the test created. Every step is
     * best effort so one failure does not hide the others.
     */
    @AfterClass
    public static void cleanup() {
        if (null != contentlet) {
            ContentletDataGen.destroy(contentlet, true);
        }
        if (null != contentType) {
            ContentTypeDataGen.remove(contentType, true);
        }
        if (null != site) {
            try {
                hostAPI.archive(site, systemUser, false);
                hostAPI.delete(site, systemUser, false);
            } catch (final Exception e) {
                Logger.error(UserDeletionCacheInvalidationTest.class, "Unable to remove test site", e);
            }
        }
        if (null != replacementUser) {
            UserDataGen.remove(replacementUser, true);
        }
    }

    /**
     * Given a Site and a Contentlet last modified, and the Contentlet locked, by the user being
     * deleted, all three already warm in their caches, when the user is deleted with a
     * replacement, then the very next reads, served from cache with no flush, report the
     * replacement user, and GraphQL hydration of the Site resolves the replacement's name.
     */
    @Test
    public void delete_evictsStaleUserReferencesFromSiteContentletAndLockCaches()
            throws Exception {
        final Role adminRole = APILocator.getRoleAPI().loadCMSAdminRole();
        final User userToDelete = new UserDataGen().roles(adminRole).nextPersisted();
        replacementUser = new UserDataGen().nextPersisted();

        site = hostAPI.save(new SiteDataGen().next(), userToDelete, false);
        contentType = new ContentTypeDataGen().host(site).nextPersisted();
        // Check in explicitly as userToDelete: the data generator always checks in as system
        final Contentlet draft = new ContentletDataGen(contentType).host(site).next();
        draft.setIndexPolicy(IndexPolicy.FORCE);
        draft.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        contentlet = contentletAPI.checkin(draft, userToDelete, false);
        contentletAPI.lock(contentlet, userToDelete, false);

        // Warm every cache with the soon-to-be-deleted user's id
        assertEquals(userToDelete.getUserId(),
                hostAPI.find(site.getIdentifier(), systemUser, false).getModUser());
        assertEquals(userToDelete.getUserId(),
                contentletAPI.find(contentlet.getInode(), systemUser, false).getModUser());
        assertEquals(Optional.of(userToDelete.getUserId()), versionableAPI.getLockedBy(contentlet));

        userAPI.delete(userToDelete, replacementUser, systemUser, false);

        // No cache flush and no wait for the asynchronous reindex: the caches must already
        // be clean when delete() returns
        assertEquals("Cached Site still carries the deleted user", replacementUser.getUserId(),
                hostAPI.find(site.getIdentifier(), systemUser, false).getModUser());
        assertEquals("Cached Contentlet still carries the deleted user", replacementUser.getUserId(),
                contentletAPI.find(contentlet.getInode(), systemUser, false).getModUser());
        assertEquals("Cached lock still names the deleted user",
                Optional.of(replacementUser.getUserId()), versionableAPI.getLockedBy(contentlet));

        final Contentlet hydratedSite = new DotTransformerBuilder().graphQLDataFetchOptions()
                .content(hostAPI.find(site.getIdentifier(), systemUser, false)).build()
                .hydrate().get(0);
        assertEquals(replacementUser.getFullName(), hydratedSite.get(Contentlet.MOD_USER_NAME_KEY));
    }
}
