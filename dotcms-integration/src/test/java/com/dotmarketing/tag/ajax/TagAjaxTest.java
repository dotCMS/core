package com.dotmarketing.tag.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.ajax.DwrWebContextRule;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.business.TagTestFixtures;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the permission checks in {@link TagAjax}.
 */
public class TagAjaxTest extends IntegrationTestBase {

    private static final String TAGS_FIELD = "tags";

    @ClassRule
    public static final DwrWebContextRule dwrContext = new DwrWebContextRule();

    private static final TagTestFixtures fixtures = new TagTestFixtures();

    private static TagAPI tagAPI;
    private static User systemUser;

    @AfterClass
    public static void cleanUpTagPermissionFixtures() throws Exception {
        fixtures.cleanUp();
    }

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        tagAPI = APILocator.getTagAPI();
        systemUser = APILocator.systemUser();
    }

    /**
     * Method to test: {@link TagAjax#deleteTag(User, String)}
     * Given: a tag on a contentlet the user can't edit
     * Expected: a {@link DotSecurityException} is thrown and the tag is kept
     */
    @Test
    public void test_deleteTag_userCannotEditTaggedContent_shouldThrow() throws Exception {
        final User limitedUser = newLimitedUser();
        final Contentlet contentlet = newContentlet();
        final Tag tag = tagAPI.getTagAndCreate("ajax-delete-" + UUIDGenerator.shorty(),
                systemUser.getUserId(), Host.SYSTEM_HOST);

        try {
            tagAPI.addContentletTagInode(tag, contentlet.getInode(), TAGS_FIELD);

            try {
                TagAjax.deleteTag(limitedUser, tag.getTagId());
                fail("Deleting a tag on content the user can't edit should not be allowed");
            } catch (final DotSecurityException e) {
                // expected
            }
            assertNotNull("The tag should be kept", tagAPI.getTagByTagId(tag.getTagId()));
        } finally {
            tagAPI.deleteTag(tag.getTagId());
            UserDataGen.remove(limitedUser);
        }
    }

    /**
     * Method to test: {@link TagAjax#deleteTag(User, String)}
     * Given: a tag on a contentlet and an admin user
     * Expected: the tag is deleted
     */
    @Test
    public void test_deleteTag_adminUser_shouldDelete() throws Exception {
        final Contentlet contentlet = newContentlet();
        final Tag tag = tagAPI.getTagAndCreate("ajax-delete-admin-" + UUIDGenerator.shorty(),
                systemUser.getUserId(), Host.SYSTEM_HOST);
        tagAPI.addContentletTagInode(tag, contentlet.getInode(), TAGS_FIELD);

        TagAjax.deleteTag(systemUser, tag.getTagId());

        assertNull("The tag should be deleted", tagAPI.getTagByTagId(tag.getTagId()));
    }

    /**
     * Method to test: {@link TagAjax#checkCanEditUserTags(User, String)}
     * Given: a user without admin rights or the Users portlet changes another user's tags
     * Expected: a {@link DotSecurityException} is thrown
     */
    @Test(expected = DotSecurityException.class)
    public void test_checkCanEditUserTags_otherUser_shouldThrow() throws Exception {
        final User limitedUser = newLimitedUser();
        final User otherUser = newLimitedUser();
        try {
            TagAjax.checkCanEditUserTags(limitedUser, otherUser.getUserId());
        } finally {
            UserDataGen.remove(limitedUser);
            UserDataGen.remove(otherUser);
        }
    }

    /**
     * Method to test: {@link TagAjax#checkCanEditUserTags(User, String)}
     * Given: a user without admin rights changes their own tags
     * Expected: no exception
     */
    @Test
    public void test_checkCanEditUserTags_ownTags_shouldPass() throws Exception {
        final User limitedUser = newLimitedUser();
        try {
            TagAjax.checkCanEditUserTags(limitedUser, limitedUser.getUserId());
        } finally {
            UserDataGen.remove(limitedUser);
        }
    }

    /**
     * Method to test: {@link TagAjax#checkCanEditUserTags(User, String)}
     * Given: an admin changes another user's tags
     * Expected: no exception
     */
    @Test
    public void test_checkCanEditUserTags_adminUser_shouldPass() throws Exception {
        final User otherUser = newLimitedUser();
        try {
            TagAjax.checkCanEditUserTags(APILocator.getUserAPI().loadByUserByEmail(
                    "admin@dotcms.com", systemUser, false), otherUser.getUserId());
        } finally {
            UserDataGen.remove(otherUser);
        }
    }

    /**
     * Method to test: {@link TagAjax#addTag(String, String, String)}
     * Given: a back-end user without a Users portlet tags another user
     * Expected: a {@link DotSecurityException} is thrown and the other user gets no tag
     */
    @Test
    public void test_addTag_otherUserWithoutUsersPortlet_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final User other = fixtures.backEndUser();
        final String tagName = "ajax-add-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            assertThrows(DotSecurityException.class,
                    () -> TagAjax.addTag(tagName, other.getUserId(), ""));
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertFalse("The other user should get no tag", userTagNames(other).contains(tagName));
    }

    /**
     * Method to test: {@link TagAjax#addTag(String, String, String)}
     * Given: a user with the Tags portlet tags themselves
     * Expected: the tag is added to the user
     */
    @Test
    public void test_addTag_ownUserWithTagsPortlet_shouldBindTag() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final String tagName = "ajax-own-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            TagAjax.addTag(tagName, user.getUserId(), "");
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertTrue(userTagNames(user).contains(tagName));
    }

    /**
     * Method to test: {@link TagAjax#addTag(String, String, String)}
     * Given: a back-end user without Users or Tags portlets tags themselves
     * Expected: a {@link DotSecurityException} is thrown and no tag is created
     */
    @Test
    public void test_addTag_ownUserWithoutPortlets_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final String tagName = "ajax-own-denied-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            assertThrows(DotSecurityException.class,
                    () -> TagAjax.addTag(tagName, user.getUserId(), ""));
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertNull(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
    }

    /**
     * Method to test: {@link TagAjax#addTag(String, String, String)}
     * Given: a user with only the legacy Users portlet tags another user
     * Expected: the tag is added to the other user
     */
    @Test
    public void test_addTag_usersLegacyPortlet_shouldBindTag() throws Exception {
        final User user = fixtures.backEndUser(PortletID.USERS_LEGACY.toString());
        final User other = fixtures.backEndUser();
        final String tagName = "ajax-legacy-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            TagAjax.addTag(tagName, other.getUserId(), "");
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertTrue(userTagNames(other).contains(tagName));
    }

    /**
     * Method to test: {@link TagAjax#addTag(String, String, String)}
     * Given: a user with only the legacy Tags portlet creates a tag with no owner
     * Expected: the tag is created
     */
    @Test
    public void test_addTag_noOwnerWithTagsLegacyPortlet_shouldCreateTag() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS_LEGACY.toString());
        final String tagName = "ajax-noowner-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            TagAjax.addTag(tagName, "", Host.SYSTEM_HOST);
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertNotNull(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
    }

    /**
     * Method to test: {@link TagAjax#addTag(String, String, String)}
     * Given: an admin without any Tags portlet creates a tag with no owner
     * Expected: the tag is created
     */
    @Test
    public void test_addTag_noOwnerAsAdmin_shouldCreateTag() throws Exception {
        final String tagName = "ajax-noowner-admin-" + UUIDGenerator.shorty();
        dwrContext.loginAs(TestUserUtils.getAdminUser());

        try {
            TagAjax.addTag(tagName, "", Host.SYSTEM_HOST);
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertNotNull(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
    }

    /**
     * Method to test: {@link TagAjax#addTag(String, String, String)}
     * Given: a back-end user without a Tags portlet creates a tag with no owner
     * Expected: a {@link DotSecurityException} is thrown and no tag is created
     */
    @Test
    public void test_addTag_noOwnerWithoutTagsPortlet_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final String tagName = "ajax-noowner-denied-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            assertThrows(DotSecurityException.class,
                    () -> TagAjax.addTag(tagName, "", Host.SYSTEM_HOST));
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertNull(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
    }

    /**
     * Method to test: {@link TagAjax#getTagsByUser(String)}
     * Given: a back-end user without a Users portlet reads another user's tags
     * Expected: a {@link DotSecurityException} is thrown
     */
    @Test
    public void test_getTagsByUser_otherUser_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final User other = fixtures.backEndUser();
        tagAPI.addUserTagInode(fixtures.tag("ajax-read"), other.getUserId());
        dwrContext.loginAs(user);

        assertThrows(DotSecurityException.class,
                () -> new TagAjax().getTagsByUser(other.getUserId()));
    }

    /**
     * Method to test: {@link TagAjax#getTagByUser(String)}
     * Given: a back-end user without a Users portlet reads another user's tags
     * Expected: a {@link DotSecurityException} is thrown
     */
    @Test
    public void test_getTagByUser_otherUser_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final User other = fixtures.backEndUser();
        tagAPI.addUserTagInode(fixtures.tag("ajax-read"), other.getUserId());
        dwrContext.loginAs(user);

        assertThrows(DotSecurityException.class,
                () -> new TagAjax().getTagByUser(other.getUserId()));
    }

    /**
     * Method to test: {@link TagAjax#getTagsByUser(String)}
     * Given: a user with the Users portlet reads another user's tags
     * Expected: the other user's tags are returned
     */
    @Test
    public void test_getTagsByUser_usersPortlet_shouldReturnTags() throws Exception {
        final User user = fixtures.backEndUser(PortletID.USERS.toString());
        final User other = fixtures.backEndUser();
        final Tag tag = fixtures.tag("ajax-read");
        tagAPI.addUserTagInode(tag, other.getUserId());
        dwrContext.loginAs(user);

        final Map<String, List<Tag>> result = new TagAjax().getTagsByUser(other.getUserId());

        assertTrue(result.get("tags").stream().anyMatch(t -> t.getTagId().equals(tag.getTagId())));
    }

    /**
     * Method to test: {@link TagAjax#checkCanEditUserTags(User, String)}
     * Given: a user with only the legacy Users portlet changes another user's tags
     * Expected: no exception
     */
    @Test
    public void test_checkCanEditUserTags_usersLegacyPortlet_shouldPass() throws Exception {
        final User user = fixtures.backEndUser(PortletID.USERS_LEGACY.toString());
        final User other = fixtures.backEndUser();

        TagAjax.checkCanEditUserTags(user, other.getUserId());
    }

    /**
     * Method to test: {@link TagAjax#updateTag(String, String, String)}
     * Given: a user with the Tags portlet renames a tag on content they can't edit
     * Expected: a {@link DotSecurityException} is thrown and the name is kept
     */
    @Test
    public void test_updateTag_tagOnContentWithoutEdit_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final Contentlet contentlet = fixtures.contentWithPermission(user, PermissionAPI.PERMISSION_READ);
        final Tag tag = fixtures.tag("ajax-rename");
        tagAPI.addContentletTagInode(tag, contentlet.getInode(), TAGS_FIELD);
        dwrContext.loginAs(user);

        assertThrows(DotSecurityException.class, () -> TagAjax.updateTag(tag.getTagId(),
                tag.getTagName() + "-renamed", Host.SYSTEM_HOST));
        assertEquals(tag.getTagName(), tagAPI.getTagByTagId(tag.getTagId()).getTagName());
    }

    /**
     * Method to test: {@link TagAjax#updateTag(String, String, String)}
     * Given: a user with the Tags portlet renames a tag nothing uses
     * Expected: the tag is renamed
     */
    @Test
    public void test_updateTag_unusedTagWithTagsPortlet_shouldRename() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final Tag tag = fixtures.tag("ajax-rename");
        final String newName = tag.getTagName() + "-renamed";
        dwrContext.loginAs(user);

        TagAjax.updateTag(tag.getTagId(), newName, Host.SYSTEM_HOST);

        assertEquals(newName, tagAPI.getTagByTagId(tag.getTagId()).getTagName());
    }

    /**
     * Method to test: {@link TagAjax#updateTag(String, String, String)}
     * Given: a back-end user without a Tags portlet renames a tag nothing uses
     * Expected: a {@link DotSecurityException} is thrown and the name is kept
     */
    @Test
    public void test_updateTag_withoutTagsPortlet_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final Tag tag = fixtures.tag("ajax-rename");
        dwrContext.loginAs(user);

        assertThrows(DotSecurityException.class, () -> TagAjax.updateTag(tag.getTagId(),
                tag.getTagName() + "-renamed", Host.SYSTEM_HOST));
        assertEquals(tag.getTagName(), tagAPI.getTagByTagId(tag.getTagId()).getTagName());
    }

    /**
     * Method to test: {@link TagAjax#getTagInodeByInode(String)}
     * Given: a contentlet the user can't read
     * Expected: a {@link DotSecurityException} is thrown
     */
    @Test
    public void test_getTagInodeByInode_contentWithoutRead_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final Contentlet contentlet = fixtures.contentWithPermission(user, 0);
        tagAPI.addContentletTagInode(fixtures.tag("ajax-inode"), contentlet.getInode(), TAGS_FIELD);
        dwrContext.loginAs(user);

        assertThrows(DotSecurityException.class,
                () -> TagAjax.getTagInodeByInode(contentlet.getInode()));
    }

    /**
     * Method to test: {@link TagAjax#deleteTagInode(String, String)}
     * Given: a contentlet the user can only read
     * Expected: a {@link DotSecurityException} is thrown
     */
    @Test
    public void test_deleteTagInode_contentWithoutEdit_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final Contentlet contentlet = fixtures.contentWithPermission(user, PermissionAPI.PERMISSION_READ);
        final Tag tag = fixtures.tag("ajax-inode");
        tagAPI.addContentletTagInode(tag, contentlet.getInode(), TAGS_FIELD);
        dwrContext.loginAs(user);

        assertThrows(DotSecurityException.class,
                () -> new TagAjax().deleteTagInode(tag.getTagName(), contentlet.getInode()));
    }

    /**
     * Method to test: {@link TagAjax#importTags(byte[])}
     * Given: a back-end user without a Tags portlet imports a tag
     * Expected: a {@link DotSecurityException} is thrown and no tag is created
     */
    @Test
    public void test_importTags_withoutTagsPortlet_shouldThrow() throws Exception {
        final User user = fixtures.backEndUser();
        final String tagName = "ajax-import-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            assertThrows(DotSecurityException.class, () -> TagAjax.importTags(csv(tagName)));
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertNull(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
    }

    /**
     * Method to test: {@link TagAjax#importTags(byte[])}
     * Given: a user with only the legacy Tags portlet imports a tag
     * Expected: the tag is created
     */
    @Test
    public void test_importTags_tagsLegacyPortlet_shouldCreateTags() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS_LEGACY.toString());
        final String tagName = "ajax-import-" + UUIDGenerator.shorty();
        dwrContext.loginAs(user);

        try {
            TagAjax.importTags(csv(tagName));
        } finally {
            fixtures.track(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        assertNotNull(tagAPI.getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
    }

    private static List<String> userTagNames(final User user) throws Exception {
        return tagAPI.getTagsForUserByUserId(user.getUserId()).stream()
                .map(Tag::getTagName).collect(Collectors.toList());
    }

    private static byte[] csv(final String tagName) {
        return String.format("%s,%s%n", tagName, Host.SYSTEM_HOST).getBytes(StandardCharsets.UTF_8);
    }

    private static User newLimitedUser() {
        return new UserDataGen().roles(new RoleDataGen().nextPersisted()).nextPersisted();
    }

    private static Contentlet newContentlet() {
        return TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
    }
}
