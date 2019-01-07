package com.dotcms.util;

import com.dotcms.config.DotInitializationService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.SystemProperties;
import org.apache.struts.Globals;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.ModuleConfigFactory;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.fail;

/**
 * Sets up the web environment needed to execute integration tests without a server application
 * Created by nollymar on 9/29/16.
 */
public class IntegrationTestInitService {
    private static IntegrationTestInitService service = new IntegrationTestInitService();

    private static AtomicBoolean initCompleted;

    static { SystemProperties.getProperties(); }
    
    private IntegrationTestInitService() {
        initCompleted = new AtomicBoolean(false);
    }

    public static IntegrationTestInitService getInstance() {
        return service;
    }

    public void init() throws Exception {
        if (!initCompleted.get()) {
            TestingJndiDatasource.init();
            ConfigTestHelper._setupFakeTestingContext();

            CacheLocator.init();
    		FactoryLocator.init();
    		APILocator.init();

    		//For these tests fire the reindex immediately
            Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", false);
            Config.setProperty("ASYNC_COMMIT_LISTENERS", false);

            Config.setProperty("NETWORK_CACHE_FLUSH_DELAY", (long) 0);
            // Init other dotCMS services.
            DotInitializationService.getInstance().initialize();

            if (Config.getBooleanProperty("dotcms.integrationtest.run.upgradetask", true)) {
                this.tryUpgradeTask();
            }

            initCompleted.set(true);
        }
    }

    private void tryUpgradeTask () {

        final StartupAPI startupAPI = APILocator.getStartupAPI();

        final List<Class<?>> alwaysTaskClasses = startupAPI.getStartupRunAlwaysTaskClasses();
        final List<Class<?>> onceTaskClasses   = startupAPI.getStartupRunOnceTaskClasses();

        Logger.info(this, ()->"Running the always upgrade tasks");
        for (final Class<?> alwaysTaskClass : alwaysTaskClasses) {

            try {

                Logger.info(this, ()->"Running the upgrade task: " + alwaysTaskClass.getCanonicalName());
                startupAPI.runStartup(alwaysTaskClass);
            } catch (DotDataException e) {
                Logger.info(this, ()->"Error on running the upgrade task: " +
                        alwaysTaskClass.getCanonicalName() + ", msg: " + e.getMessage());

                if (Config.getBooleanProperty("dotcms.integrationtest.run.upgradetask.stoponerror", true)) {
                    fail(e.getMessage());
                }
            }
        }
        Logger.info(this, ()->"Ran the always upgrade tasks");

        Logger.info(this, ()->"Running the once upgrade tasks");

        for (final Class<?> onceTaskClass : onceTaskClasses) {

            try {

                Logger.info(this, ()->"Running the upgrade task: " + onceTaskClass.getCanonicalName());
                startupAPI.runStartup(onceTaskClass);
                Logger.info(this, ()->"Ran the upgrade task: " + onceTaskClass.getCanonicalName());
            } catch (Exception e) {

                Logger.info(this, ()->"Error on running the upgrade task: " +
                        onceTaskClass.getCanonicalName() + ", msg: " + e.getMessage());

                if (Config.getBooleanProperty("dotcms.integrationtest.run.upgradetask.stoponerror", true)) {
                    fail(e.getMessage());
                }
            }
        }
        Logger.info(this, ()->"Ran the once upgrade tasks");
    }

    public void mockStrutsActionModule() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();
        ModuleConfig config = factoryObject.createModuleConfig("");
        Mockito.when(Config.CONTEXT.getAttribute(Globals.MODULE_KEY)).thenReturn(config);
    }
}
