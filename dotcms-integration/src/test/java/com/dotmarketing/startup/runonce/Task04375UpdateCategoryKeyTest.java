package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.CategoryFactory;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import java.util.Date;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.liferay.portal.model.User;

public class Task04375UpdateCategoryKeyTest extends BaseWorkflowIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade_updateEmptyCategoryKey_Success() throws Exception {

        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final CategoryFactory categoryFactory = FactoryLocator.getCategoryFactory();
        final User systemUser = APILocator.systemUser();
        Category category = null;
        long currentTime = System.currentTimeMillis();
        String categoryInode = null;
        try {
            category = new Category();
            category.setSortOrder(0);
            category.setCategoryName("testCategory" + currentTime);
            category.setCategoryVelocityVarName("testCategory" + currentTime);
            category.setKeywords(null);
            category.setModDate(new Date());
            category.setActive(true);

            categoryFactory.save(category);

            categoryInode = category.getInode();
            category = categoryAPI.find(categoryInode, systemUser, false);

            Assert.assertNull(category.getKey());

            final Task04385UpdateCategoryKey updateCategoryKey = new Task04385UpdateCategoryKey();
            updateCategoryKey.executeUpgrade();

            CacheLocator.getCategoryCache().clearCache();
            HibernateUtil.evict(category);

            category = categoryAPI.find(categoryInode, systemUser, false);
            Assert.assertNotNull(category.getKey());

        } finally {
            if (categoryInode != null) {
                category = categoryAPI.find(categoryInode, systemUser, false);
                categoryAPI.delete(category, systemUser, false);
            }
        }
    }



}
