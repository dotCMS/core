package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Task240131UpdateLanguageVariableContentTypeTest {
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, SQLException, DotSecurityException {
        try {
            APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        }catch (NotFoundInDbException e){
            //Create LanguageVariable Content Type in case doesn't exist
            Task04210CreateDefaultLanguageVariable upgradeTaskCreateLanguageVariable = new Task04210CreateDefaultLanguageVariable();
            upgradeTaskCreateLanguageVariable.executeUpgrade();
        }
        //Update system to true in case it's false
        final DotConnect dc = new DotConnect();
        dc.setSQL("update structure set system = true where velocity_var_name = ?");
        dc.addParam(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME);
        dc.loadResult();
        //Flush cache to get latest changes
        CacheLocator.getContentTypeCache2().clearCache();
        //System must be true
        assertTrue(APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME).system());
        //Run UT
        Task240131UpdateLanguageVariableContentType upgradeTask = new Task240131UpdateLanguageVariableContentType();
        upgradeTask.executeUpgrade();
        //Flush cache to get latest changes
        CacheLocator.getContentTypeCache2().clearCache();
        //System must be false
        assertFalse(APILocator.getContentTypeAPI(APILocator.systemUser()).find(LanguageVariableAPI.LANGUAGEVARIABLE_VAR_NAME).system());
    }
}
