package com.dotcms.enterprise.publishing.remote;

import static org.junit.Assert.*;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.enterprise.publishing.remote.bundler.CategoryBundler;
import com.dotcms.enterprise.publishing.remote.handler.CategoryHandler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Config;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.liferay.portal.model.User;
import java.io.File;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * This class will work as a place to run integration tests related to bundler->handler process as a
 * whole.
 */
public class CategoryBundlerHandlerTest extends IntegrationTestBase {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

        user = APILocator.getUserAPI().getSystemUser();
    }

    @Test
    public void testBundlerAndHandler_success_when_categoriesAsDependencies()
            throws DotBundleException, DotDataException, DotSecurityException {

        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final File tempDir = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());

        List<Category> categoriesToDelete = Lists.newArrayList();

        try {
            //Create Parent Category.
            Category parentCategory = new Category();
            parentCategory.setCategoryName("Category A");
            parentCategory.setKey("categoryA");
            parentCategory.setCategoryVelocityVarName("categoryA");
            parentCategory.setSortOrder((String) null);
            parentCategory.setKeywords(null);

            categoryAPI.save(null, parentCategory, user, false);
            categoriesToDelete.add(parentCategory);

            //Create First Child Category.
            Category childCategory = new Category();
            childCategory.setCategoryName("Category B");
            childCategory.setKey("categoryB");
            childCategory.setCategoryVelocityVarName("categoryB");
            childCategory.setSortOrder(1);
            childCategory.setKeywords(null);

            categoryAPI.save(parentCategory, childCategory, user, false);
            categoriesToDelete.add(childCategory);

            //Create Grand-Child Category.
            Category grandchildCategory = new Category();
            grandchildCategory.setCategoryName("Category C");
            grandchildCategory.setKey("categoryC");
            grandchildCategory.setCategoryVelocityVarName("categoryC");
            grandchildCategory.setSortOrder(2);
            grandchildCategory.setKeywords(null);

            categoryAPI.save(childCategory, grandchildCategory, user, false);
            categoriesToDelete.add(grandchildCategory);

            //Creating categories hierarchy.
            Set<String> categorySet = Sets.newHashSet();
            categorySet.add(grandchildCategory.getInode());

            CategoryBundler categoryBundler = new CategoryBundler();

            //Mocking Push Publish configuration
            PushPublisherConfig config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getCategories()).thenReturn(categorySet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(PublisherConfig.Operation.PUBLISH);
            categoryBundler.setConfig(config);

            //Creating temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            //Generating bundle
            BundlerStatus status = new BundlerStatus(CategoryBundler.class.getName());
            categoryBundler.generate(tempDir, status);
            assertEquals("We should have 3 categories.xml", 3, status.getCount());

            // Now let's delete the categories to make sure they are installed by the Handler.
            cleanCategories(categoriesToDelete);
            assertNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNull(categoryAPI.find(childCategory.getInode(), user, false));
            assertNull(categoryAPI.find(grandchildCategory.getInode(), user, false));

            // Run the bundler that will create the categories again.
            CategoryHandler categoryHandler = new CategoryHandler(config);
            categoryHandler.handle(tempDir);

            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(childCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(grandchildCategory.getInode(), user, false));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            tempDir.delete();
            cleanCategories(categoriesToDelete);
        }
    }

}
