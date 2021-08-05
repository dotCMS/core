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
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.CategoryCache;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
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
    public void testBundlerAndHandler_success_newCategories()
            throws DotBundleException, DotDataException, DotSecurityException {

        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final CategoryCache categoryCache = CacheLocator.getCategoryCache();
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

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config, tempDir);

            //Creating temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            //Generating bundle
            BundlerStatus status = new BundlerStatus(CategoryBundler.class.getName());
            categoryBundler.generate(directoryBundleOutput, status);
            assertEquals("We should have 3 categories.xml", 3, status.getCount());

            // Now let's delete the categories to make sure they are installed by the Handler.
            cleanCategories(categoriesToDelete);
            categoryCache.clearCache();
            assertNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNull(categoryAPI.find(childCategory.getInode(), user, false));
            assertNull(categoryAPI.find(grandchildCategory.getInode(), user, false));

            // Run the bundler that will create the categories again.
            CategoryHandler categoryHandler = new CategoryHandler(config);
            categoryHandler.handle(tempDir);

            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(childCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(grandchildCategory.getInode(), user, false));

            assertEquals(1,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").size());
            assertEquals(childCategory,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").get(0));

            assertEquals(1,
                    categoryAPI.findChildren(user, childCategory.getInode(), false, "").size());
            assertEquals(grandchildCategory,
                    categoryAPI.findChildren(user, childCategory.getInode(), false, "").get(0));

            assertEquals(0, categoryAPI.findChildren(user, grandchildCategory.getInode(), false, "")
                    .size());

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            tempDir.delete();
            cleanCategories(categoriesToDelete);
        }
    }

    /**
     * This Test used to be called `testBundlerAndHandler_success_differentInodeSameKey`
     * That was before the introduction of deterministic identifiers.
     * From now on when restoring a category on a receiver node it is expected to have the exact same inode
     * @throws DotBundleException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testBundlerAndHandler_success_SameKey_SameInodeOnReceiver()
            throws DotBundleException, DotDataException, DotSecurityException {

        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final CategoryCache categoryCache = CacheLocator.getCategoryCache();
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

            //Creating categories hierarchy.
            Set<String> categorySet = Sets.newHashSet();
            categorySet.add(childCategory.getInode());

            CategoryBundler categoryBundler = new CategoryBundler();

            //Mocking Push Publish configuration
            PushPublisherConfig config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getCategories()).thenReturn(categorySet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(PublisherConfig.Operation.PUBLISH);
            categoryBundler.setConfig(config);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config, tempDir);

            //Creating temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            //Generating bundle
            BundlerStatus status = new BundlerStatus(CategoryBundler.class.getName());
            categoryBundler.generate(directoryBundleOutput, status);
            assertEquals("We should have 2 categories.xml", 2, status.getCount());

            // Now let's delete the categories to make sure they are installed by the Handler.
            cleanCategories(categoriesToDelete);
            categoriesToDelete.clear();
            categoryCache.clearCache();
            //Categories no longer exist for the given inode we're using
            assertNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNull(categoryAPI.find(childCategory.getInode(), user, false));

            // But recreate same categories so the key match with different inodes.
            //Create Receiver Parent Category.
            Category parentCategoryReceiver = new Category();
            parentCategoryReceiver.setCategoryName("Category A");
            parentCategoryReceiver.setKey("categoryA");
            parentCategoryReceiver.setCategoryVelocityVarName("categoryA");
            parentCategoryReceiver.setSortOrder((String) null);
            parentCategoryReceiver.setKeywords(null);

            categoryAPI.save(null, parentCategoryReceiver, user, false);
            categoriesToDelete.add(parentCategoryReceiver);

            //Create Receiver Child Category.
            Category childCategoryReceiver = new Category();
            childCategoryReceiver.setCategoryName("Category B");
            childCategoryReceiver.setKey("categoryB");
            childCategoryReceiver.setCategoryVelocityVarName("categoryB");
            childCategoryReceiver.setSortOrder(1);
            childCategoryReceiver.setKeywords(null);

            categoryAPI.save(parentCategoryReceiver, childCategoryReceiver, user, false);
            categoriesToDelete.add(childCategoryReceiver);

            // Run the bundler that will create the categories again.
            CategoryHandler categoryHandler = new CategoryHandler(config);
            categoryHandler.handle(tempDir);

            //These are the categories originally created on the sender.
            // But the inode are the same again since now they were created  using deterministic identifiers
            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(childCategory.getInode(), user, false));

            assertNotNull(categoryAPI.find(parentCategoryReceiver.getInode(), user, false));
            assertNotNull(categoryAPI.find(childCategoryReceiver.getInode(), user, false));

            assertEquals(1,
                    categoryAPI.findChildren(user, parentCategoryReceiver.getInode(), false, "")
                            .size());
            assertEquals(childCategoryReceiver,
                    categoryAPI.findChildren(user, parentCategoryReceiver.getInode(), false, "")
                            .get(0));

            assertEquals(0,
                    categoryAPI.findChildren(user, childCategoryReceiver.getInode(), false, "")
                            .size());

            // inodes being different is no longer the case since the introduction of deterministic ids
            //Whenever the inode gets restored the id should be the same as it was.
            //Also the key reminds the same as well.
            assertEquals(parentCategory.getInode(), parentCategoryReceiver.getInode());
            assertEquals(childCategory.getInode(), childCategoryReceiver.getInode());

            assertEquals(parentCategory.getKey(), parentCategoryReceiver.getKey());
            assertEquals(childCategory.getKey(), childCategoryReceiver.getKey());

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            tempDir.delete();
            cleanCategories(categoriesToDelete);
        }
    }

    @Test
    public void testBundlerAndHandler_success_sameInodeSameKey()
            throws DotBundleException, DotDataException, DotSecurityException {

        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final CategoryCache categoryCache = CacheLocator.getCategoryCache();
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

            //Creating categories hierarchy.
            Set<String> categorySet = Sets.newHashSet();
            categorySet.add(childCategory.getInode());

            CategoryBundler categoryBundler = new CategoryBundler();

            //Mocking Push Publish configuration
            PushPublisherConfig config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getCategories()).thenReturn(categorySet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(PublisherConfig.Operation.PUBLISH);
            categoryBundler.setConfig(config);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config, tempDir);

            //Creating temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            //Generating bundle
            BundlerStatus status = new BundlerStatus(CategoryBundler.class.getName());
            categoryBundler.generate(directoryBundleOutput, status);
            assertEquals("We should have 2 categories.xml", 2, status.getCount());

            // We don't want to delete the categories, just clear cache.
            categoryCache.clearCache();

            // Run the bundler that will create the categories again.
            CategoryHandler categoryHandler = new CategoryHandler(config);
            categoryHandler.handle(tempDir);

            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));

            assertEquals(1,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").size());
            assertEquals(childCategory,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").get(0));

            assertEquals(0,
                    categoryAPI.findChildren(user, childCategory.getInode(), false, "").size());

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            tempDir.delete();
            cleanCategories(categoriesToDelete);
        }
    }

    @Test
    public void testBundlerAndHandler_success_sameInodeBlankKey()
            throws DotBundleException, DotDataException, DotSecurityException {

        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final CategoryCache categoryCache = CacheLocator.getCategoryCache();
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final File tempDir = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());

        List<Category> categoriesToDelete = Lists.newArrayList();

        try {
            //Create Parent Category.
            Category parentCategory = new Category();
            parentCategory.setCategoryName("Category A");
            parentCategory.setKey(null);
            parentCategory.setCategoryVelocityVarName("categoryA");
            parentCategory.setSortOrder((String) null);
            parentCategory.setKeywords(null);

            // We need to use saveRemote so we don't run into unique-key validation.
            categoryAPI.saveRemote(null, parentCategory, user, false);
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

            //Creating categories hierarchy.
            Set<String> categorySet = Sets.newHashSet();
            categorySet.add(childCategory.getInode());

            CategoryBundler categoryBundler = new CategoryBundler();

            //Mocking Push Publish configuration
            PushPublisherConfig config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getCategories()).thenReturn(categorySet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(PublisherConfig.Operation.PUBLISH);
            categoryBundler.setConfig(config);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config, tempDir);

            //Creating temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            //Generating bundle
            BundlerStatus status = new BundlerStatus(CategoryBundler.class.getName());
            categoryBundler.generate(directoryBundleOutput, status);
            assertEquals("We should have 2 categories.xml", 2, status.getCount());

            // We don't want to delete the categories.
            parentCategory.setKey("now-with-key");
            categoryAPI.save(null, parentCategory, user, false);

            categoryCache.clearCache();

            // Run the bundler that will create the categories again.
            CategoryHandler categoryHandler = new CategoryHandler(config);
            categoryHandler.handle(tempDir);

            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));

            assertEquals(1,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").size());
            assertEquals(childCategory,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").get(0));

            assertEquals(0,
                    categoryAPI.findChildren(user, childCategory.getInode(), false, "").size());

            assertTrue(!UtilMethods
                    .isSet(categoryAPI.find(parentCategory.getInode(), user, false).getKey()));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            tempDir.delete();
            cleanCategories(categoriesToDelete);
        }
    }

    @Test
    public void testBundlerAndHandler_success_sameInodeDifferentKey()
            throws DotBundleException, DotDataException, DotSecurityException {

        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final CategoryCache categoryCache = CacheLocator.getCategoryCache();
        final String assetRealPath = Config.getStringProperty("ASSET_REAL_PATH", "test-resources");
        final File tempDir = new File(assetRealPath + "/bundles/" + System.currentTimeMillis());

        List<Category> categoriesToDelete = Lists.newArrayList();

        try {
            final String NEW_KEY = "new-key";
            final String OLD_KEY = "old-key";

            //Create Parent Category.
            Category parentCategory = new Category();
            parentCategory.setCategoryName("Category A");
            parentCategory.setKey(NEW_KEY);
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

            //Creating categories hierarchy.
            Set<String> categorySet = Sets.newHashSet();
            categorySet.add(childCategory.getInode());

            CategoryBundler categoryBundler = new CategoryBundler();

            //Mocking Push Publish configuration
            PushPublisherConfig config = Mockito.mock(PushPublisherConfig.class);
            Mockito.when(config.getCategories()).thenReturn(categorySet);
            Mockito.when(config.isDownloading()).thenReturn(true);
            Mockito.when(config.getOperation()).thenReturn(PublisherConfig.Operation.PUBLISH);
            categoryBundler.setConfig(config);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config, tempDir);

            //Creating temp bundle dir
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            //Generating bundle
            BundlerStatus status = new BundlerStatus(CategoryBundler.class.getName());
            categoryBundler.generate(directoryBundleOutput, status);
            assertEquals("We should have 2 categories.xml", 2, status.getCount());

            // We don't want to delete the categories.
            parentCategory.setKey(OLD_KEY);
            categoryAPI.save(null, parentCategory, user, false);
            assertEquals(OLD_KEY,
                    categoryAPI.find(parentCategory.getInode(), user, false).getKey());

            categoryCache.clearCache();

            // Run the bundler that will create the categories again.
            CategoryHandler categoryHandler = new CategoryHandler(config);
            categoryHandler.handle(tempDir);

            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));
            assertNotNull(categoryAPI.find(parentCategory.getInode(), user, false));

            assertEquals(1,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").size());
            assertEquals(childCategory,
                    categoryAPI.findChildren(user, parentCategory.getInode(), false, "").get(0));

            assertEquals(0,
                    categoryAPI.findChildren(user, childCategory.getInode(), false, "").size());

            assertEquals(NEW_KEY,
                    categoryAPI.find(parentCategory.getInode(), user, false).getKey());

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            tempDir.delete();
            cleanCategories(categoriesToDelete);
        }
    }

}
