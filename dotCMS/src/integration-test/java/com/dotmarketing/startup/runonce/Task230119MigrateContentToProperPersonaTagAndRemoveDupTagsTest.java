package com.dotmarketing.startup.runonce;

import com.dotcms.datagen.TagDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.tag.model.Tag;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task230119MigrateContentToProperPersonaTagAndRemoveDupTagsTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link Task230119MigrateContentToProperPersonaTagAndRemoveDupTags#executeUpgrade()}
     * Given scenario: duplicated persona tags, "hipster" and "hipster:persona" and related content to the latter
     * Expected behavior: UT should
     *  <ul>
     *     <li>Insert new relationships between the correct tag and the content related to the wrong duplicate tag
     *     <li>Delete bad relationships between content and the wrong duplicate tag
     *     <li>Delete the duplicate tags
     * </ul>
     */
    @Test
    public void testUT() throws DotDataException {
        final String realHipsterName = "hipster";
        final String fakeHipsterName = "hipster:persona";
        // Create dup tag and relate content to it
        final Tag realHipster = new TagDataGen().name(realHipsterName).nextPersisted();
        final Tag fakeHipster = new TagDataGen().name(fakeHipsterName).nextPersisted();
        final Contentlet bannerWithTag = TestDataUtils.getBannerLikeContent(true, 1);
        APILocator.getTagAPI().addContentletTagInode(fakeHipster,
                bannerWithTag.getInode(), "tags");

        // let's now create a second content but add the good and bad tag to it. The upgrade task shouldn't fail
        final Contentlet anotherContentWithTag = TestDataUtils.getBannerLikeContent(true, 1);
        APILocator.getTagAPI().addContentletTagInode(realHipster,
                anotherContentWithTag.getInode(), "tags");
        APILocator.getTagAPI().addContentletTagInode(fakeHipster,
                anotherContentWithTag.getInode(), "tags");

        List<Map<String, Object>> resultsBeforeUT = new DotConnect().setSQL("SELECT tagname\n"
                + "FROM tag_inode JOIN tag t ON tag_inode.tag_id = t.tag_id\n"
                + "WHERE inode = '" + bannerWithTag.getInode() + "' ORDER BY tagname DESC").loadObjectResults();

        // get(1) since get(0) is a test tag created by the TestDataUtils
        Assert.assertEquals(fakeHipsterName, resultsBeforeUT.get(1).get("tagname"));

        new Task230119MigrateContentToProperPersonaTagAndRemoveDupTags().executeUpgrade();

        List<Map<String, Object>> resultsAfterUT = new DotConnect().setSQL("SELECT tagname\n"
                + "FROM tag_inode JOIN tag t ON tag_inode.tag_id = t.tag_id\n"
                + "WHERE inode = '" + bannerWithTag.getInode() + "' ORDER BY tagname DESC").loadObjectResults();

        Assert.assertEquals(realHipsterName, resultsAfterUT.get(1).get("tagname"));

        // no related content with the wrong tag should be present
        Assert.assertTrue(new DotConnect().setSQL("SELECT tagname\n"
                + "FROM tag_inode JOIN tag t ON tag_inode.tag_id = t.tag_id\n"
                + "WHERE tagname = '"+fakeHipsterName+"'").loadObjectResults().isEmpty());

        // wrong tag should not be present anymore
        Assert.assertTrue(new DotConnect()
                .setSQL("SELECT * FROM tag WHERE tagname = '"+fakeHipsterName+"'")
                .loadObjectResults()
                .isEmpty());

    }
}
