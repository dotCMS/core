package com.dotcms.osgi.tika;

import com.dotcms.tika.TikaServiceBuilder;
import com.dotmarketing.osgi.GenericBundleActivator;
import java.util.Hashtable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends GenericBundleActivator {
    private static Logger LOG = LoggerFactory.getLogger(Activator.class);

    private ServiceRegistration dotTikaService;

    @SuppressWarnings("unchecked")
    public void start(BundleContext context) throws Exception {

        LOG.debug("Starting Tika Plugin");
        //Create an instance of our TikaBuilder
        TikaBuilderImpl tikaBuilder = new TikaBuilderImpl();

        //Register the TikaServiceBuilder as a OSGI service
        this.dotTikaService = context
                .registerService(TikaServiceBuilder.class.getName(), tikaBuilder,
                        new Hashtable<>());
    }

    public void stop(BundleContext context) throws Exception {

        //Unregister the registered services
        this.dotTikaService.unregister();
    }

}
