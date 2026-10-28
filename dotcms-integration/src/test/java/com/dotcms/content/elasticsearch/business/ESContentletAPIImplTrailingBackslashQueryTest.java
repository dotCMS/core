package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers what a search returns when the caller's Lucene query ends in a backslash.
 * <p>
 * The clauses that narrow a search to what the caller is allowed to read are appended to that query
 * as text, separated from it by a single space, so a query ending in an unbalanced escape used to
 * consume the separator and the clauses no longer parsed as clauses of their own. The fixture holds
 * two published contentlets of the same type, each readable by a different role, and a user who
 * holds only one of those roles: whatever that user gets for a query has to be the same with and
 * without a trailing backslash.
 */
public class ESContentletAPIImplTrailingBackslashQueryTest extends IntegrationTestBase {

    private static User systemUser;
    private static ContentType contentType;
    private static User readerUser;
    private static User otherUser;
    private static Contentlet readable;
    private static Contentlet hidden;
    private static String query;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();

        final Host site = new SiteDataGen().nextPersisted();
        contentType = new ContentTypeDataGen().host(site).nextPersisted();

        final Role readerRole = new RoleDataGen().nextPersisted();
        final Role otherRole = new RoleDataGen().nextPersisted();
        readerUser = new UserDataGen().roles(readerRole).nextPersisted();
        otherUser = new UserDataGen().roles(otherRole).nextPersisted();

        readable = publishedContent();
        hidden = publishedContent();
        permitReadOnly(readable, List.of(readerRole));
        permitReadOnly(hidden, List.of(otherRole));

        query = "+contentType:" + contentType.variable() + " +live:true +deleted:false";
    }

    @AfterClass
    public static void cleanUp() {
        ContentletDataGen.destroy(hidden, true);
        ContentletDataGen.destroy(readable, true);
        ContentTypeDataGen.remove(contentType, true);
    }

    /**
     * Given: two published contentlets of one type, readable by two different roles, and a user who
     * holds one of them.
     * When: that user runs the same query twice, once ending in a backslash.
     * Then: both runs return the one contentlet the user can read.
     */
    @Test
    public void test_queryEndingInBackslash_returnsOnlyWhatTheUserCanRead() throws Exception {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        final List<String> control = identifiersOf(
                contentletAPI.search(query, 100, 0, null, readerUser, false));
        final List<String> trailingBackslash = identifiersOf(
                contentletAPI.search(query + " x\\", 100, 0, null, readerUser, false));

        assertEquals("the fixture is only meaningful if the user reads exactly one of the two",
                List.of(readable.getIdentifier()), control);
        assertEquals("a query ending in a backslash has to match what the same query matches",
                control, trailingBackslash);
    }

    /**
     * The mirror image of the test above, run by the user holding the other role. It is what keeps
     * the pair honest: each user sees their own contentlet and only that one, so neither result can
     * be explained by content missing from the index. An administrator could not play that part:
     * no clause is appended for them, so their query reaches the index exactly as written, and one
     * ending in a lone backslash is rejected as unparseable and comes back empty.
     */
    @Test
    public void test_queryEndingInBackslash_showsEachUserTheirOwnContent() throws Exception {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        final List<String> control = identifiersOf(
                contentletAPI.search(query, 100, 0, null, otherUser, false));
        final List<String> trailingBackslash = identifiersOf(
                contentletAPI.search(query + " x\\", 100, 0, null, otherUser, false));

        assertEquals("the other contentlet is in the index, readable by the other role",
                List.of(hidden.getIdentifier()), control);
        assertEquals("a query ending in a backslash has to match what the same query matches",
                control, trailingBackslash);
    }

    /**
     * The count built for the same query goes through the same appended clauses as the search, so it
     * has to agree with it.
     */
    @Test
    public void test_countOfQueryEndingInBackslash_matchesWhatTheUserCanRead() throws Exception {
        assertEquals(1L,
                APILocator.getContentletAPI().indexCount(query + " x\\", readerUser, false));
    }

    /** The identifiers of the given results, sorted, so two result sets can be compared directly. */
    private static List<String> identifiersOf(final List<Contentlet> contentlets) {
        return contentlets.stream().map(Contentlet::getIdentifier).sorted()
                .collect(Collectors.toList());
    }

    /** A contentlet of the test's content type, published and waited for in the index. */
    private static Contentlet publishedContent() {
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.WAIT_FOR)
                .nextPersisted();
        return ContentletDataGen.publish(contentlet);
    }

    /**
     * Replaces the permissions the contentlet inherits with READ for exactly the given roles, and
     * reindexes it so a search filters on the new grant.
     *
     * <p>Breaking the inheritance copies every inherited grant onto the contentlet, and saving only
     * adds to what is there, so each copied grant for a role outside the given ones is revoked
     * explicitly: an individual permission saved with no bits is deleted. Roles that cannot be
     * edited are left alone.
     */
    private static void permitReadOnly(final Contentlet contentlet, final List<Role> roles)
            throws Exception {
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.permissionIndividually(
                permissionAPI.findParentPermissionable(contentlet), contentlet, systemUser);

        final Set<String> keep = roles.stream().map(Role::getId).collect(Collectors.toSet());
        for (final Permission copied : permissionAPI.getPermissions(contentlet, true, true)) {
            final Role role = APILocator.getRoleAPI().loadRoleById(copied.getRoleId());
            if (!keep.contains(role.getId()) && role.isEditPermissions()) {
                permissionAPI.save(new Permission(copied.getType(), contentlet.getPermissionId(),
                        role.getId(), 0, true), contentlet, systemUser, false);
            }
        }

        permissionAPI.save(roles.stream()
                        .map(role -> new Permission(contentlet.getPermissionId(), role.getId(),
                                PermissionAPI.PERMISSION_READ, true))
                        .collect(Collectors.toList()),
                contentlet, systemUser, false);
        contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().refresh(contentlet);
    }
}
