package com.dotcms.rest.config;

import javax.inject.Singleton;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.internal.DefaultClassAnalyzer;
import org.jvnet.hk2.internal.DynamicConfigurationImpl;
import org.jvnet.hk2.internal.DynamicConfigurationServiceImpl;
import org.jvnet.hk2.internal.InstantiationServiceImpl;
import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.jvnet.hk2.internal.ServiceLocatorRuntimeImpl;
import org.jvnet.hk2.internal.Utilities;
/**
 * This class provides a workaround for a known issue in Jersey where a service locator,
 * once marked as inactive, throws an exception that renders Jersey unusable.
 *
 * <p>When this exception is thrown directly from the service locator during dependency
 * injection, Jersey enters a non-functional state. The solution implemented here involves
 * overriding the service locator using JavaServiceProviderLocator. By leveraging the
 * class loader, we replace the problematic service instances with safe-to-dispose null
 * classes.</p>
 *
 * <p>The replacement is configured in the <code>META-INF/services</code> folder via Service Provider Interface (SPI),
 * where the necessary class overrides are specified. This ensures that when an exception
 * is encountered during disposal, it is intercepted, and a safe null instance is returned,
 * preventing additional failures.</p>
 *
 * <p>It is important to note that the root cause of this issue is a known Github isse,
 * and this workaround should be removed once the migration to Tomcat 10 is complete.</p>
 * See <a href="https://github.com/dotCMS/core/issues/31185">31185</a> for more information.
 */
public class DotServiceLocatorGeneratorImpl implements ServiceLocatorGenerator {

    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
        ServiceLocatorImpl sli = new DotServiceLocatorImpl(name, (ServiceLocatorImpl) parent);

        DynamicConfigurationImpl dci = new DynamicConfigurationImpl(sli);

        // The service locator itself
        dci.bind(Utilities.getLocatorDescriptor(sli));

        // The injection resolver for three thirty
        dci.addActiveDescriptor(Utilities.getThreeThirtyDescriptor(sli));

        // The dynamic configuration utility
        dci.bind(BuilderHelper.link(DynamicConfigurationServiceImpl.class, false).
                to(DynamicConfigurationService.class).
                in(Singleton.class.getName()).
                localOnly().
                build());

        dci.bind(BuilderHelper.createConstantDescriptor(
                new DefaultClassAnalyzer(sli)));

        dci.bind(BuilderHelper.createDescriptorFromClass(ServiceLocatorRuntimeImpl.class));

        dci.bind(BuilderHelper.createConstantDescriptor(
                new InstantiationServiceImpl()));

        dci.commit();

        return sli;
    }
}
