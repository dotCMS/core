package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.liferay.portal.model.Company;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task04375UpdateColorsTest extends BaseWorkflowIntegrationTest {


    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade_newColors_Success() throws DotDataException {
            final Task04375UpdateColors updateColors = new Task04375UpdateColors();
            updateColors.executeUpgrade();
            CacheLocator.getCacheAdministrator().flushGroup("companypool");
            final Company company = APILocator.getCompanyAPI().getDefaultCompany();
            Assert.assertEquals(updateColors.PRIMARY_COLOR.toLowerCase(),company.getType().toLowerCase());
            Assert.assertEquals(updateColors.SECONDARY_COLOR.toLowerCase(),company.getStreet().toLowerCase());
    }

}
