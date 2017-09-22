package com.dotcms.util;

import com.dotcms.config.DotInitializationService;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfigFactory;
import com.liferay.util.SystemProperties;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.util.Config;

import java.util.concurrent.atomic.AtomicBoolean;

import org.mockito.Mockito;

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
            Config.setProperty("REINDEX_ON_SAVE_IN_SEPARATE_THREAD", false);

            // Init other dotCMS services.
            DotInitializationService.getInstance().initialize();

            initCompleted.set(true);
        }
    }
    
    public void mockStrutsActionModule() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();
        ModuleConfig config = factoryObject.createModuleConfig("");
        Mockito.when(Config.CONTEXT.getAttribute(Globals.MODULE_KEY)).thenReturn(config);
    }
}
