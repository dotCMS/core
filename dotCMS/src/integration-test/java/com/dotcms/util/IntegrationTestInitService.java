package com.dotcms.util;

import com.dotcms.config.DotInitializationService;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfigFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.SystemProperties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mockito.Mockito;

import javax.sql.DataSource;

/**
 * Sets up the web environment needed to execute integration tests without a server application
 * Created by nollymar on 9/29/16.
 */
public class IntegrationTestInitService {

    private static final IntegrationTestInitService service = new IntegrationTestInitService();

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
        try {
            if (initCompleted.compareAndSet(false, true)) {
                ConfigTestHelper._setupFakeTestingContext();


                //For these tests fire the reindex immediately
                Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", false);
                Config.setProperty("ASYNC_COMMIT_LISTENERS", false);
                Config.setProperty("GRAPHQL_SCHEMA_DEBOUNCE_DELAY_MILLIS", 0);
                Config.setProperty("NETWORK_CACHE_FLUSH_DELAY", (long) 0);

                // Fail early with exception for db connection configuration issues
                DataSource datasource = DbConnectionFactory.getDataSource();
                if (datasource != null) {
                    Logger.debug(this, "got datasource for integration tests");
                }

                CacheLocator.init();
                FactoryLocator.init();
                APILocator.init();

                //Running the always run startup tasks
                StartupTasksUtil.getInstance().init();


                // Init other dotCMS services.
                DotInitializationService.getInstance().initialize();
            }
        } catch(Exception e) {
            throw new RuntimeException("Fatal error initializing Integration Test Init Service",e);
        }
    }

    public void mockStrutsActionModule() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();
        ModuleConfig config = factoryObject.createModuleConfig("");
        Mockito.when(Config.CONTEXT.getAttribute(Globals.MODULE_KEY)).thenReturn(config);
    }
}
