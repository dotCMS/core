package com.dotmarketing.startup.runonce;


import static com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl.DEFAULT_LANGUAGE_CODE;
import static com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryImpl.DEFAULT_LANGUAGE_COUNTRY_CODE;
import static com.dotmarketing.util.ConfigUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.business.UniqueLanguageDataGen;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple2;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task211012AddCompanyDefaultLanguageTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void dropColumn(final DotConnect dotConnect) throws DotDataException{

        Logger.debug(this, "Prepping for testing `add` column.");
        try {
            final Task211012AddCompanyDefaultLanguage task = new Task211012AddCompanyDefaultLanguage();
            if(task.companyHasDefaultLanguageColumn()) {
                DbConnectionFactory.getConnection().setAutoCommit(true);
                dotConnect.setSQL("UPDATE company SET default_language_id = NULL").loadResult();
                final String dropColumnSQL = "ALTER TABLE company DROP COLUMN default_language_id";
                dotConnect.executeStatement(dropColumnSQL);
            }
        } catch (SQLException e) {
            Logger.warn(Task211012AddCompanyDefaultLanguageTest.class, e.getMessage(), e);
        }
    }

    /**
     * Given scenario: We drop the column if it already exists. Run the upgrade task
     * Expected Results: The column must be there
     * @throws DotDataException
     */
    @Test
    public void Test_UpgradeTask_Success() throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        dropColumn(dotConnect);
        final Task211012AddCompanyDefaultLanguage task = new Task211012AddCompanyDefaultLanguage();
        assertTrue(task.forceRun());
        task.executeUpgrade();

        dotConnect.setSQL("SELECT default_language_id FROM company WHERE companyId = ?");
        final List<Map<String, Object>> maps = dotConnect.addParam(
                Task211012AddCompanyDefaultLanguage.DEFAULT_COMPANY_ID).loadObjectResults();
        assertEquals("There can only be 1.", 1, maps.size());
        final Map<String, Object> map = maps.get(0);
        Assert.assertNotNull(map.get("default_language_id"));
    }

    /**
     * Given scenario: Test upgrade task adding an existing lang
     * Expected Results: Success.
     * @throws DotDataException
     */
    @Test
    public void Test_UpgradeTask_Added_New_Language_Success()
            throws DotDataException, DotSecurityException {
        final LanguageAPI languageAPI = APILocator.getLanguageAPI();
        final Language defaultLang = languageAPI.getDefaultLanguage();
        final Language language = new UniqueLanguageDataGen().nextPersisted();
        final Task211012AddCompanyDefaultLanguage task = new Task211012AddCompanyDefaultLanguage();
        final Tuple2<String, String> defaultLanguageDeclaration = getDeclaredDefaultLanguage();

        try {
            //Test a brand new lang
            Config.setProperty(DEFAULT_LANGUAGE_CODE, language.getLanguageCode());
            Config.setProperty(DEFAULT_LANGUAGE_COUNTRY_CODE, language.getCountryCode());

            final DotConnect dotConnect = new DotConnect();
            dropColumn(dotConnect);
            task.executeUpgrade();
            dotConnect.setSQL("SELECT default_language_id FROM company WHERE companyId = ?");
            final List<Map<String, Object>> maps = dotConnect.addParam(
                    Task211012AddCompanyDefaultLanguage.DEFAULT_COMPANY_ID).loadObjectResults();
            assertEquals("There can only be 1.", 1, maps.size());
            final Map<String, Object> map = maps.get(0);
            Assert.assertEquals(language.getId(),((Number)map.get("default_language_id")).longValue());

        }finally {
            Config.setProperty(DEFAULT_LANGUAGE_CODE, defaultLanguageDeclaration._1());
            Config.setProperty(DEFAULT_LANGUAGE_COUNTRY_CODE, defaultLanguageDeclaration._2());
            languageAPI.makeDefault(defaultLang.getId(), APILocator.systemUser());
        }
    }

    /**
     * Given scenario: Test upgrade task adding a non-existing lang
     * Expected Results: Failure
     * @throws DotDataException
     */
    @Test(expected = DotDataException.class)
    public void Test_UpgradeTask_None_Existing_Language_Property() throws DotDataException {

        final DotConnect dotConnect = new DotConnect();
        dropColumn(dotConnect);

        final Task211012AddCompanyDefaultLanguage task = new Task211012AddCompanyDefaultLanguage();
        final Tuple2<String, String> defaultLanguageDeclaration = getDeclaredDefaultLanguage();

        try {
            //Test a non exising language
            Config.setProperty(DEFAULT_LANGUAGE_CODE, "Any");
            Config.setProperty(DEFAULT_LANGUAGE_COUNTRY_CODE, "Any");
            task.executeUpgrade();
        }finally {
            Config.setProperty(DEFAULT_LANGUAGE_CODE, defaultLanguageDeclaration._1());
            Config.setProperty(DEFAULT_LANGUAGE_COUNTRY_CODE, defaultLanguageDeclaration._2());
        }
    }

}
