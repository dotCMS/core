package com.dotmarketing.startup.runonce;

import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppsAPIImpl;
import com.dotcms.security.apps.AppsCache;
import com.dotcms.security.apps.SecretsStoreKeyStoreImpl;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import java.io.File;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task05390LoadAppsSecretsTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    private void destroySecretsStore(){
        final String secretStorePath = SecretsStoreKeyStoreImpl.getSecretStorePath();
        new File(secretStorePath).delete();
        final AppsCache appsCache = CacheLocator.getAppsCache();
        appsCache.clearCache();
    }

    private void createSecretsAndExportThem(){
        final AppsAPI appsAPI = APILocator.getAppsAPI();
        
    }

    @Test
    public void Test_UpgradeTask() throws DotDataException {

        final AppsAPI appsAPI = APILocator.getAppsAPI();


        final Task05390LoadAppsSecrets task = new Task05390LoadAppsSecrets();
        Assert.assertTrue(task.forceRun());
        task.executeUpgrade();
    }

}
