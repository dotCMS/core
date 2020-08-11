package com.dotcms.translate;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.dotmarketing.util.Config;
import java.util.Collections;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GoogleTranslationServiceIntegrationTest {

    private static User admin;
    private static GoogleTranslationService translationService;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        admin = TestUserUtils.getAdminUser();
        translationService = new GoogleTranslationService();
    }

    @Test
    public void test_setServiceParameters_SecretExistAtHost_returnAPIKey()
            throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().name("GoogleTranslateSecretAtHost").nextPersisted();
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,host);
        final String secretValue = "Secret At Site";

        //Create app secret
        final AppSecrets appSecrets = new AppSecrets.Builder()
                .withKey(GoogleTranslationService.GOOGLE_TRANSLATE_APP_CONFIG_KEY)
                .withSecret(GoogleTranslationService.API_KEY_VAR,secretValue)
                .build();
        APILocator.getAppsAPI().saveSecrets(appSecrets,host,admin);

        //Call setServiceParameters to set value to apiKey
        translationService.setServiceParameters(Collections
                .singletonList(new ServiceParameter(GoogleTranslationService.API_KEY_VAR, "Service API Key",
                        StringPool.BLANK)),contentlet.getHost());

        final String apiKey = translationService.getApiKey();
        Assert.assertNotNull(apiKey);
        Assert.assertEquals(secretValue,apiKey);

        APILocator.getAppsAPI().deleteSecrets(GoogleTranslationService.GOOGLE_TRANSLATE_APP_CONFIG_KEY,host,admin);
    }

    @Test
    public void test_setServiceParameters_SecretExistAtSystemHost_returnAPIKey()
            throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().name("GoogleTranslateSecretAtSystemHost").nextPersisted();
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,host);
        final String secretValue = "Secret At System Host";

        //Create app secret
        final AppSecrets appSecrets = new AppSecrets.Builder()
                .withKey(GoogleTranslationService.GOOGLE_TRANSLATE_APP_CONFIG_KEY)
                .withSecret(GoogleTranslationService.API_KEY_VAR,secretValue)
                .build();
        APILocator.getAppsAPI().saveSecrets(appSecrets,APILocator.systemHost(),admin);

        //Call setServiceParameters to set value to apiKey
        translationService.setServiceParameters(Collections
                .singletonList(new ServiceParameter(GoogleTranslationService.API_KEY_VAR, "Service API Key",
                        StringPool.BLANK)),contentlet.getHost());

        final String apiKey = translationService.getApiKey();
        Assert.assertNotNull(apiKey);
        Assert.assertEquals(secretValue,apiKey);

        APILocator.getAppsAPI().deleteSecrets(GoogleTranslationService.GOOGLE_TRANSLATE_APP_CONFIG_KEY,APILocator.systemHost(),admin);
    }

    @Test
    public void test_setServiceParameters_APIKeySetPropertyFile_returnAPIKey(){
        final Host host = new SiteDataGen().name("GoogleTranslateKeyAtPropertyFile").nextPersisted();
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,1,host);
        final String apiKeyValue = "apiKeyValue at Property file";

        Config.setProperty(GoogleTranslationService.GOOGLE_TRANSLATE_SERVICE_API_KEY_PROPERTY,apiKeyValue);

        //Call setServiceParameters to set value to apiKey
        translationService.setServiceParameters(Collections
                .singletonList(new ServiceParameter(GoogleTranslationService.API_KEY_VAR, "Service API Key",
                        StringPool.BLANK)),contentlet.getIdentifier());

        final String apiKey = translationService.getApiKey();
        Assert.assertNotNull(apiKey);
        Assert.assertEquals(apiKeyValue,apiKey);
    }

}
