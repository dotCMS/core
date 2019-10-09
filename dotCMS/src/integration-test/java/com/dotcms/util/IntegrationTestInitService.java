package com.dotcms.util;

import com.dotcms.config.DotInitializationService;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfigFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.util.Config;
import com.liferay.util.SystemProperties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.Mockito;

/**
 * Sets up the web environment needed to execute integration tests without a server application
 * Created by nollymar on 9/29/16.
 */
public class IntegrationTestInitService {

    private static IntegrationTestInitService service = new IntegrationTestInitService();

    private static final AtomicBoolean initCompleted = new AtomicBoolean(false);

    static {
        SystemProperties.getProperties();
    }

    private IntegrationTestInitService() {
    }

    public static IntegrationTestInitService getInstance() {
        return service;
    }

    public void init() throws Exception {
        if (initCompleted.compareAndSet(false, true)) {
            TestingJndiDatasource.init();
            ConfigTestHelper._setupFakeTestingContext();

            CacheLocator.init();
            FactoryLocator.init();
            APILocator.init();

            //Running the always run startup tasks
            StartupTasksUtil.getInstance().init();

            //For these tests fire the reindex immediately
            Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", false);
            Config.setProperty("ASYNC_COMMIT_LISTENERS", false);

            Config.setProperty("NETWORK_CACHE_FLUSH_DELAY", (long) 0);
            // Init other dotCMS services.
            DotInitializationService.getInstance().initialize();
        }
    }

    public void mockStrutsActionModule() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();
        ModuleConfig config = factoryObject.createModuleConfig("");
        Mockito.when(Config.CONTEXT.getAttribute(Globals.MODULE_KEY)).thenReturn(config);
    }
}
