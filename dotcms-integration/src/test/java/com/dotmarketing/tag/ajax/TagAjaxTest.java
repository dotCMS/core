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
 * Integration tests for {@link TagAjax#deleteTag(User, String)}.
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

    private static User newLimitedUser() {
        return new UserDataGen().roles(new RoleDataGen().nextPersisted()).nextPersisted();
    }

    private static Contentlet newContentlet() {
        return TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
    }
}
