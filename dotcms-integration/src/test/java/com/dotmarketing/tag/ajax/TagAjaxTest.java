package com.dotmarketing.tag.ajax;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the permission checks in {@link TagAjax}.
 */
public class TagAjaxTest extends IntegrationTestBase {

    private static final String TAGS_FIELD = "tags";

    private static TagAPI tagAPI;
    private static User systemUser;

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

    private static User newLimitedUser() {
        return new UserDataGen().roles(new RoleDataGen().nextPersisted()).nextPersisted();
    }

    private static Contentlet newContentlet() {
        return TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
    }
}
